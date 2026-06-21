package com.runninghub.feature.auth.data.repository

import com.runninghub.core.common.md5
import com.runninghub.core.model.User
import com.runninghub.core.network.NetworkErrorMapper
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.data.remote.dto.LoginTokenDataDto
import com.runninghub.feature.auth.data.remote.dto.PwdLoginRequestDto
import com.runninghub.feature.auth.data.remote.dto.SmsCodeRequestDto
import com.runninghub.feature.auth.data.remote.dto.SmsLoginRequestDto
import com.runninghub.feature.auth.data.remote.dto.toDomain
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.feature.auth.domain.SmsError
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Auth Repository 的 Data 层实现。
 *
 * 本类负责调用用户中心登录接口、持久化令牌、拉取当前用户资料，并把服务端错误收口为
 * Auth Domain 的稳定错误类型。它位于 `feature:auth:data`，不再依赖 shared 的 API/DTO/工具类。
 *
 * 并发约束：
 * - 令牌刷新统一委托给 [TokenRefresher]，与 HTTP 401 拦截器共用同一套并发去重规则。
 * - 登录成功必须在用户资料也恢复成功后才调用 [SessionManager.markAuthenticated]。
 * - logout 远端请求采用 best-effort，失败也必须清理本地敏感凭据。
 */
class AuthRepositoryImpl(
    private val api: AuthApi,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager,
    private val tokenRefresher: TokenRefresher,
    private val balanceCache: BalanceCache? = null,
) : AuthRepository {

    /**
     * 使用手机号和密码登录用户中心。
     *
     * 密码会先按旧接口协议计算 MD5 摘要；登录成功后必须持久化令牌、拉取用户资料并写入
     * API Key，随后才把会话标记为已认证。网络异常会映射为 [AuthError.Network]，服务端业务
     * 失败保留为失败结果交给 Presentation 映射展示。
     */
    override suspend fun login(phone: String, password: String): Result<User> = runCatching {
        val response = try {
            api.pwdLogin(PwdLoginRequestDto(mobile = phone, password = md5(password)))
        } catch (e: Exception) {
            if (!NetworkErrorMapper.isNetworkError(e)) throw e
            throw AuthError.Network()
        }
        if (response.code != 0) throw AuthError.Unknown("AUTH_FAILED_CODE_${response.code}")

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        val user = try {
            fetchAndCacheUser(tokenData.accessToken)
        } catch (e: Exception) {
            if (!NetworkErrorMapper.isNetworkError(e)) throw e
            throw AuthError.Network()
        }
        sessionManager.markAuthenticated()
        user
    }

    /**
     * 请求用户中心发送短信验证码。
     *
     * 调用方只需要处理 [SmsError] 语义错误；Data 层在这里把网络异常、频控、验证码过期等
     * 服务端结果统一收口，避免 UI 直接依赖远端错误字符串。
     *
     * @param captchaToken TAC 滑块验证码返回的 `validToken`。
     * 首次发送时通常为空；若服务端返回 [SmsError.CaptchaRequired]，Presentation
     * 层应完成图形验证后携带 token 重试。
     */
    override suspend fun sendSmsCode(phone: String, captchaToken: String?): Result<Unit> = runCatching {
        val response = try {
            api.sendSmsCode(SmsCodeRequestDto(mobile = phone, token = captchaToken))
        } catch (_: Exception) {
            throw SmsError.Network()
        }
        if (response.code != 0) throw mapSmsError(response.msg, response.code)
    }

    /**
     * 使用短信验证码登录用户中心。
     *
     * 成功路径与密码登录保持一致：先持久化令牌，再恢复用户资料和 API Key，最后更新会话状态。
     * 这样可以避免根导航在用户资料尚未准备好时提前进入主界面。
     */
    override suspend fun smsLogin(phone: String, code: String): Result<User> = runCatching {
        val response = try {
            api.smsLogin(SmsLoginRequestDto(mobile = phone, code = code))
        } catch (_: Exception) {
            throw SmsError.Network()
        }
        if (response.code != 0) throw mapSmsError(response.msg, response.code)

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        val user = fetchAndCacheUser(tokenData.accessToken)
        sessionManager.markAuthenticated()
        user
    }

    /**
     * 注销当前会话并清理本地敏感凭据。
     *
     * 远端注销失败不会阻断本地清理，因为用户已经明确要求退出；本方法会清理 access token、
     * refresh token、Cookie、API Key 和余额缓存，并通过 [SessionManager] 释放业务页面状态。
     */
    override suspend fun logout() {
        // 主动登出是用户明确结束会话，先切换状态可立即清空业务页面栈。
        sessionManager.logout()
        try {
            credentialStore.getAuthToken()?.let { api.logout(it) }
        } catch (error: Exception) {
            ignoreRemoteLogoutFailure(error)
        }
        credentialStore.clearAuthToken()
        credentialStore.clearRefreshToken()
        credentialStore.clearCookie()
        credentialStore.clearApiKey()
        balanceCache?.clearLastKnownCoins()
        sessionManager.logout()
    }

    /**
     * 判断本地是否存在 access token。
     *
     * 该判断只代表本地凭据存在，不保证 token 仍然有效；需要有效 token 时应调用
     * [refreshTokenIfNeeded] 触发过期检查和刷新。
     */
    override suspend fun isLoggedIn(): Boolean =
        !credentialStore.getAuthToken().isNullOrEmpty()

    /**
     * 在 token 过期或即将过期时刷新 access token。
     *
     * 刷新逻辑委托给 [TokenRefresher]，从而复用 HTTP 401 拦截器的并发去重和失败失效策略。
     * 刷新失败会触发会话过期，调用方应按失败结果切换到登录态。
     */
    override suspend fun refreshTokenIfNeeded(): Result<String> = runCatching {
        val currentToken = credentialStore.getAuthToken()
        if (!currentToken.isNullOrEmpty() && !currentToken.isExpiredJwt()) {
            sessionManager.markAuthenticated()
            return@runCatching currentToken
        }

        val refreshed = tokenRefresher.refreshAfterUnauthorized(currentToken)
        if (!refreshed) {
            sessionManager.expire()
            throw IllegalStateException("Token refresh failed")
        }

        val refreshedToken = credentialStore.getAuthToken()
        if (refreshedToken.isNullOrEmpty()) {
            sessionManager.expire()
            throw IllegalStateException("No access token received")
        }

        sessionManager.resetExpiration()
        sessionManager.markAuthenticated()
        refreshedToken
    }

    /**
     * 读取当前本地 access token。
     *
     * 该函数不触发刷新，也不校验有效期，适合只需要把当前凭据传给兼容逻辑的场景。
     */
    override suspend fun getCurrentAuthToken(): String? = credentialStore.getAuthToken()

    /**
     * 从当前 JWT 的 `sub` 字段解析用户 ID。
     *
     * 解析失败或本地没有 token 时返回 null，调用方不应把该值作为认证有效性的唯一依据。
     */
    override suspend fun getCurrentUserId(): String? {
        val token = credentialStore.getAuthToken() ?: return null
        return extractUserIdFromJwt(token).ifEmpty { null }
    }

    private suspend fun persistTokens(tokenData: LoginTokenDataDto) {
        credentialStore.setAuthToken(tokenData.accessToken)
        credentialStore.setRefreshToken(tokenData.refreshToken)
    }

    private suspend fun fetchAndCacheUser(accessToken: String): User {
        val userId = extractUserIdFromJwt(accessToken)
        val userResponse = api.getUserInfoWithToken(accessToken, userId)
        if (userResponse.code != 0) throw AuthError.Unknown("AUTH_USER_INFO_FAILED_CODE_${userResponse.code}")

        val user = userResponse.data?.toDomain() ?: throw IllegalStateException("Empty user response")
        user.apiKey?.let { credentialStore.setApiKey(it) }
        return user
    }

    private fun mapSmsError(msg: String, code: Int): SmsError {
        val upper = msg.uppercase()
        return when {
            upper.contains("SMS_CODE_EXPIRED") -> SmsError.CodeExpired()
            upper.contains("SMS_CODE_INVALID") -> SmsError.WrongCode()
            upper.contains("ACCOUNT_NOT_EXIST") -> SmsError.AccountNotFound()
            upper.contains("SMS_SEND_TOO_FREQUENT") -> SmsError.RateLimited()
            upper.contains("SMS_DAILY_LIMIT") -> SmsError.DailyLimit()
            upper.contains("CAPTCHA_VERIFY_ERROR") -> SmsError.CaptchaRequired()
            msg.isNotEmpty() -> SmsError.Unknown("AUTH_FAILED_CODE_$code")
            else -> SmsError.Unknown("AUTH_FAILED_CODE_$code")
        }
    }

    private fun ignoreRemoteLogoutFailure(error: Exception) {
        // 远端注销失败不能阻止本地清理，否则会保留已失效或敏感凭据。
        // 不记录异常文本，避免服务端或网络库把认证请求细节写入生产日志。
        error.message
    }

    private fun extractUserIdFromJwt(jwt: String): String {
        val payload = decodeJwtPayload(jwt) ?: return ""
        return payload["sub"]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    private fun String.isExpiredJwt(): Boolean {
        val payload = decodeJwtPayload(this) ?: return true
        val exp = payload["exp"]?.jsonPrimitive?.longOrNull ?: return true
        return exp <= Clock.System.now().epochSeconds + TOKEN_REFRESH_SKEW_SECONDS
    }

    private fun decodeJwtPayload(jwt: String): JsonObject? {
        return try {
            val payload = jwt.split(".").getOrNull(1) ?: return null
            val decoded = decodeBase64Url(payload)
            // JWT payload 本质是 JSON，必须交给 JSON 解析器处理 unicode escape 和字段类型；
            // 正则截取会把 `\uXXXX` 原样当成用户 ID，并可能误判带空白或字段顺序变化的响应。
            JWT_PAYLOAD_JSON.parseToJsonElement(decoded).jsonObject
        } catch (_: Exception) {
            null
        }
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun decodeBase64Url(input: String): String {
        val padded = input
            .replace('-', '+')
            .replace('_', '/')
            .let {
                val mod = it.length % 4
                if (mod > 0) it + "=".repeat(4 - mod) else it
            }
        return kotlin.io.encoding.Base64.decode(padded.encodeToByteArray()).decodeToString()
    }

    private companion object {
        const val TOKEN_REFRESH_SKEW_SECONDS = 60L
        val JWT_PAYLOAD_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
