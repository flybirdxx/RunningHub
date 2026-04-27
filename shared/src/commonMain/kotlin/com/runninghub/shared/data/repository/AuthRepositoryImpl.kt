package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.PwdLoginRequest
import com.runninghub.shared.data.remote.dto.toDomain
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.util.md5

class AuthRepositoryImpl(
    private val api: RunningHubApi,
    private val settings: SettingsRepository
) : AuthRepository {

    override suspend fun login(phone: String, password: String): Result<User> = runCatching {
        val hashedPassword = md5(password)
        val response = api.pwdLogin(PwdLoginRequest(mobile = phone, password = hashedPassword))
        check(response.code == 0) { response.msg.ifEmpty { "Login failed" } }

        val tokenData = response.data ?: throw IllegalStateException("Empty login response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        settings.setAuthToken(tokenData.accessToken)
        settings.setRefreshToken(tokenData.refreshToken)

        val userId = extractUserIdFromJwt(tokenData.accessToken)
        val userResponse = api.getUserInfoWithToken(tokenData.accessToken, userId)
        check(userResponse.code == 0) { userResponse.msg.ifEmpty { "Failed to get user info" } }

        val user = userResponse.data.toDomain()
        user.apiKey?.let { settings.setApiKey(it) }
        user
    }

    override suspend fun logout() {
        try {
            settings.getAuthToken()?.let { api.logout(it) }
        } catch (_: Exception) {
            // best-effort server logout
        }
        settings.clearAuthToken()
        settings.clearRefreshToken()
        settings.clearCookie()
        settings.clearApiKey()
    }

    override suspend fun isLoggedIn(): Boolean = !settings.getAuthToken().isNullOrEmpty()

    override suspend fun refreshTokenIfNeeded(): Result<String> = runCatching {
        val currentToken = settings.getAuthToken()
        if (!currentToken.isNullOrEmpty()) return@runCatching currentToken

        val refreshToken = settings.getRefreshToken()
            ?: throw IllegalStateException("No refresh token available")

        val response = api.tokenRefresh(refreshToken)
        check(response.code == 0) { response.msg.ifEmpty { "Token refresh failed" } }

        val tokenData = response.data ?: throw IllegalStateException("Empty refresh response")
        check(tokenData.accessToken.isNotEmpty()) { "No access token received" }

        settings.setAuthToken(tokenData.accessToken)
        if (tokenData.refreshToken.isNotEmpty()) {
            settings.setRefreshToken(tokenData.refreshToken)
        }
        tokenData.accessToken
    }

    override suspend fun getCurrentAuthToken(): String? = settings.getAuthToken()

    override suspend fun getCurrentUserId(): String? {
        val token = settings.getAuthToken() ?: return null
        return extractUserIdFromJwt(token).ifEmpty { null }
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
}
