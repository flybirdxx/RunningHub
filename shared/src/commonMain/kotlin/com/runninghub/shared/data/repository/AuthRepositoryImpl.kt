package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.PwdLoginRequest
import com.runninghub.shared.data.remote.dto.SmsCodeRequest
import com.runninghub.shared.data.remote.dto.SmsLoginRequest
import com.runninghub.shared.data.remote.dto.toDomain
import com.runninghub.shared.di.SessionExpiredHandler
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.feature.auth.domain.SmsError
import com.runninghub.shared.util.md5
import kotlinx.datetime.Clock

class AuthRepositoryImpl(
    private val api: RunningHubApi,
    private val settings: SettingsRepository
) : AuthRepository {

    override suspend fun login(phone: String, password: String): Result<User> = runCatching {
        val hashedPassword = md5(password)
        val response = try {
            api.pwdLogin(PwdLoginRequest(mobile = phone, password = hashedPassword))
        } catch (e: Exception) {
            if (!isNetworkError(e)) throw e
            throw RuntimeException(mapNetworkError(e))
        }
        check(response.code == 0) { response.msg.ifEmpty { "Login failed" } }

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        try {
            fetchAndCacheUser(tokenData.accessToken)
        } catch (e: Exception) {
            if (!isNetworkError(e)) throw e
            throw RuntimeException(mapNetworkError(e))
        }
    }

    override suspend fun sendSmsCode(phone: String): Result<Unit> = runCatching {
        val response = try {
            api.sendSmsCode(SmsCodeRequest(mobile = phone))
        } catch (e: Exception) {
            println("[sendSmsCode] Network error: ${e.message}")
            throw RuntimeException(mapNetworkError(e))
        }
        println("[sendSmsCode] Server response: code=${response.code}, msg=${response.msg}")
        if (response.code != 0) throw mapSmsError(response.msg, response.code)
    }

    override suspend fun smsLogin(phone: String, code: String): Result<User> = runCatching {
        val response = try {
            api.smsLogin(SmsLoginRequest(mobile = phone, code = code))
        } catch (e: Exception) {
            throw SmsError.Network(mapNetworkError(e))
        }
        if (response.code != 0) throw mapSmsError(response.msg, response.code)

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        persistTokens(tokenData)
        fetchAndCacheUser(tokenData.accessToken)
    }

    private var isLoggingOut = false

    override suspend fun logout() {
        isLoggingOut = true  // prevent 401 interceptor from triggering refresh during logout
        try {
            settings.getAuthToken()?.let { api.logout(it) }
        } catch (_: Exception) {
            // best-effort server logout
        }
        settings.clearAuthToken()
        settings.clearRefreshToken()
        settings.clearCookie()
        settings.clearApiKey()
        settings.clearLastKnownCoins()
        isLoggingOut = false
    }

    override suspend fun isLoggedIn(): Boolean = !settings.getAuthToken().isNullOrEmpty()

    override suspend fun refreshTokenIfNeeded(): Result<String> = runCatching {
        val currentToken = settings.getAuthToken()
        if (!currentToken.isNullOrEmpty() && !currentToken.isExpiredJwt()) return@runCatching currentToken

        val refreshToken = settings.getRefreshToken()
            ?: throw IllegalStateException("No refresh token available")

        val response = try {
            api.tokenRefresh(refreshToken)
        } catch (e: Exception) {
            SessionExpiredHandler.expire()
            throw RuntimeException(mapNetworkError(e))
        }
        check(response.code == 0) {
            SessionExpiredHandler.expire()
            response.msg.ifEmpty { "Token refresh failed" }
        }

        val tokenData = response.data ?: run {
            SessionExpiredHandler.expire()
            throw IllegalStateException("Empty refresh response")
        }
        check(tokenData.accessToken.isNotEmpty()) {
            SessionExpiredHandler.expire()
            "No access token received"
        }

        persistTokens(tokenData)
        tokenData.accessToken
    }

    override suspend fun getCurrentAuthToken(): String? = settings.getAuthToken()

    override suspend fun getCurrentUserId(): String? {
        val token = settings.getAuthToken() ?: return null
        return extractUserIdFromJwt(token).ifEmpty { null }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private suspend fun persistTokens(tokenData: com.runninghub.shared.data.remote.dto.LoginTokenData) {
        settings.setAuthToken(tokenData.accessToken)
        settings.setRefreshToken(tokenData.refreshToken)
    }

    private suspend fun fetchAndCacheUser(accessToken: String): User {
        val userId = extractUserIdFromJwt(accessToken)
        val userResponse = api.getUserInfoWithToken(accessToken, userId)
        check(userResponse.code == 0) { userResponse.msg.ifEmpty { "Failed to get user info" } }

        val user = userResponse.data?.toDomain() ?: throw IllegalStateException("Empty user response")
        user.apiKey?.let { settings.setApiKey(it) }
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
            else -> SmsError.Unknown("登录失败 (code=$code)")
        }
    }

    private fun mapNetworkError(e: Throwable): String {
        return when {
            isNetworkError(e) -> "网络连接失败，请检查网络后重试"
            else -> "登录失败，请稍后重试"
        }
    }

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("unable to resolve host")
            || msg.contains("unknownhost")
            || msg.contains("network")
            || msg.contains("connect")
            || msg.contains("timeout")
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
