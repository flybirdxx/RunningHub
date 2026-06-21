package com.runninghub.shared.data.repository

import com.runninghub.core.network.NetworkErrorMapper
import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.storage.BalanceCache
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.PwdLoginRequest
import com.runninghub.shared.data.remote.dto.SmsCodeRequest
import com.runninghub.shared.data.remote.dto.SmsLoginRequest
import com.runninghub.shared.data.remote.dto.toDomain
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.SmsError
import com.runninghub.core.model.User
import com.runninghub.shared.util.md5
import kotlinx.datetime.Clock

/**
 * Auth Repository 的 Data 层实现。
 *
 * 本类负责调用 RunningHub 登录接口、刷新访问令牌，并把认证凭证写入本地存储。
 * 它只向 Domain 暴露稳定错误语义；用户可见文案由 Presentation 层统一映射，
 * 避免网络层返回消息直接泄漏到 UI。
 *
 * 并发约束：
 * - 令牌刷新委托给 [TokenRefresher]，确保 Repository 手动刷新与 401 拦截器复用同一套并发去重规则。
 * - logout 期间清理本地凭证，但当前 401 拦截器尚未读取 isLoggingOut，这是后续会话治理范围。
 *
 * @param api RunningHub 认证接口封装，负责实际网络请求。
 * @param credentialStore 本地凭据存储边界，只用于读写认证令牌、Cookie 和 API Key。
 * @param sessionManager 可注入会话状态管理器，用于把令牌刷新失败通知给应用根层导航。
 * @param tokenRefresher core/network 提供的刷新协调器，负责 refresh token 请求、并发去重和令牌写回。
 * @param balanceCache 可选余额缓存，用于注销后清理首页展示的旧余额；为空时只处理凭据。
 */
class AuthRepositoryImpl(
    private val api: RunningHubApi,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager,
    private val tokenRefresher: TokenRefresher,
    private val balanceCache: BalanceCache? = null,
) : AuthRepository {

    /**
     * 使用密码登录并持久化服务端返回的访问令牌。
     *
     * 密码在发送前沿用旧接口要求进行 MD5 处理；网络错误在 Data 层降级为稳定异常，
     * Presentation 层负责把异常转换为最终提示。
     */
    override suspend fun login(phone: String, password: String): Result<User> = runCatching {
        val hashedPassword = md5(password)
        val response = try {
            api.pwdLogin(PwdLoginRequest(mobile = phone, password = hashedPassword))
        } catch (e: Exception) {
            if (!NetworkErrorMapper.isNetworkError(e)) throw e
            throw AuthError.Network()
        }
        check(response.code == 0) { response.msg.ifEmpty { "Login failed" } }

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        val user = try {
            fetchAndCacheUser(tokenData.accessToken)
        } catch (e: Exception) {
            if (!NetworkErrorMapper.isNetworkError(e)) throw e
            throw AuthError.Network()
        }
        // 用户资料也成功恢复后才标记已认证，避免登录 Result 失败但根导航提前进入主页面。
        sessionManager.markAuthenticated()
        user
    }

    /**
     * 请求服务端发送短信验证码。
     *
     * 服务端错误码会被映射成 [SmsError]，保留错误语义但不生成最终 UI 文案。
     */
    override suspend fun sendSmsCode(phone: String): Result<Unit> = runCatching {
        val response = try {
            api.sendSmsCode(SmsCodeRequest(mobile = phone))
        } catch (e: Exception) {
            throw SmsError.Network()
        }
        if (response.code != 0) throw mapSmsError(response.msg, response.code)
    }

    /**
     * 使用短信验证码登录。
     *
     * 成功后立即缓存令牌并拉取用户信息；失败时返回稳定 [SmsError]，便于页面执行
     * 清空验证码、停止倒计时等状态转换。
     */
    override suspend fun smsLogin(phone: String, code: String): Result<User> = runCatching {
        val response = try {
            api.smsLogin(SmsLoginRequest(mobile = phone, code = code))
        } catch (e: Exception) {
            throw SmsError.Network()
        }
        if (response.code != 0) throw mapSmsError(response.msg, response.code)

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        val user = fetchAndCacheUser(tokenData.accessToken)
        // 短信登录需要拿到当前用户信息后才算完整成功，此时再驱动根入口进入主页面。
        sessionManager.markAuthenticated()
        user
    }

    private var isLoggingOut = false

    /**
     * 注销当前会话并清理本地认证相关缓存。
     *
     * 远程 logout 采用 best-effort 策略：服务端请求失败也必须清理本地令牌，
     * 否则用户会被困在一个本地看似已登录但远程不可用的状态。
     */
    override suspend fun logout() {
        // 主动登出是用户明确结束会话，不应保留之前 401 触发的失效标记影响下一次登录。
        sessionManager.logout()
        isLoggingOut = true
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
        isLoggingOut = false
    }

    /** 根据本地访问令牌是否存在判断会话恢复入口是否可用。 */
    override suspend fun isLoggedIn(): Boolean = !credentialStore.getAuthToken().isNullOrEmpty()

    /**
     * 刷新即将过期或已经过期的访问令牌。
     *
     * Repository 只负责判断是否需要刷新以及刷新失败后的会话失效标记；真正的网络请求、
     * refresh token 读取、并发去重和令牌写回统一交给 [TokenRefresher]。这样手动会话恢复和
     * HTTP 401 拦截器不会维护两套不同的刷新语义。
     */
    override suspend fun refreshTokenIfNeeded(): Result<String> = runCatching {
        val currentToken = credentialStore.getAuthToken()
        if (!currentToken.isNullOrEmpty() && !currentToken.isExpiredJwt()) {
            sessionManager.markAuthenticated()
            return@runCatching currentToken
        }

        // 这里把 401 发生前的 token 传给 TokenRefresher；若其他协程已经完成刷新，
        // 刷新器会直接返回成功，避免重复请求 refresh 接口。
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

    /** 读取当前访问令牌，调用方不得记录完整值。 */
    override suspend fun getCurrentAuthToken(): String? = credentialStore.getAuthToken()

    /** 从 JWT payload 中读取用户 ID；解析失败时返回 null，避免把坏令牌当作有效身份。 */
    override suspend fun getCurrentUserId(): String? {
        val token = credentialStore.getAuthToken() ?: return null
        return extractUserIdFromJwt(token).ifEmpty { null }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private suspend fun persistTokens(tokenData: com.runninghub.shared.data.remote.dto.LoginTokenData) {
        credentialStore.setAuthToken(tokenData.accessToken)
        credentialStore.setRefreshToken(tokenData.refreshToken)
    }

    private suspend fun fetchAndCacheUser(accessToken: String): User {
        val userId = extractUserIdFromJwt(accessToken)
        val userResponse = api.getUserInfoWithToken(accessToken, userId)
        check(userResponse.code == 0) { userResponse.msg.ifEmpty { "Failed to get user info" } }

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
            msg.isNotEmpty() -> SmsError.Unknown(msg)
            else -> SmsError.Unknown("AUTH_FAILED_CODE_$code")
        }
    }

    private fun ignoreRemoteLogoutFailure(error: Exception) {
        // 远程注销失败不阻止本地清理，避免保留失效或敏感凭证。
        // 不记录异常文本，防止认证请求、Cookie 或 token 细节进入生产日志。
        error.message
    }

    private fun extractUserIdFromJwt(jwt: String): String {
        return try {
            val payload = jwt.split(".").getOrNull(1) ?: return ""
            val decoded = decodeBase64Url(payload)
            val subRegex = """"sub"\s*:\s*"([^"]+)"""".toRegex()
            subRegex.find(decoded)?.groupValues?.get(1) ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun String.isExpiredJwt(): Boolean {
        return try {
            val payload = split(".").getOrNull(1) ?: return true
            val decoded = decodeBase64Url(payload)
            val exp = """"exp"\s*:\s*(\d+)""".toRegex()
                .find(decoded)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return true
            exp <= Clock.System.now().epochSeconds + TOKEN_REFRESH_SKEW_SECONDS
        } catch (_: Exception) {
            true
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
    }
}
