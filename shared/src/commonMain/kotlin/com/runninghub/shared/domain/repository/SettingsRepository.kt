package com.runninghub.shared.domain.repository

interface SettingsRepository {
    suspend fun getApiKey(): String?
    suspend fun setApiKey(key: String)
    suspend fun clearApiKey()

    suspend fun getEnterpriseApiKey(): String?
    suspend fun setEnterpriseApiKey(key: String)
    suspend fun clearEnterpriseApiKey()

    suspend fun getCookie(): String?
    suspend fun setCookie(cookie: String)
    suspend fun clearCookie()

    suspend fun getAuthToken(): String?
    suspend fun setAuthToken(token: String)
    suspend fun clearAuthToken()

    suspend fun getRefreshToken(): String?
    suspend fun setRefreshToken(token: String)
    suspend fun clearRefreshToken()

    suspend fun isLoggedIn(): Boolean

    suspend fun getLastKnownCoins(): String?
    suspend fun setLastKnownCoins(coins: String)
    suspend fun clearLastKnownCoins()

    suspend fun getQuickCreateDraft(): String?
    suspend fun saveQuickCreateDraft(json: String)
    suspend fun clearQuickCreateDraft()

    suspend fun clearAll()
}
