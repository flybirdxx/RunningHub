package com.runninghub.shared.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun saveApiKey(key: String)
    fun getApiKey(): Flow<String>
    suspend fun clearApiKey()

    suspend fun saveEnterpriseApiKey(key: String)
    fun getEnterpriseApiKey(): Flow<String>
    suspend fun clearEnterpriseApiKey()

    suspend fun saveCookie(cookie: String)
    fun getCookie(): Flow<String>
    suspend fun clearCookie()

    suspend fun getApiKeySync(): String
    suspend fun getEnterpriseApiKeySync(): String
    suspend fun getCookieSync(): String
}
