package com.runninghub.shared.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private companion object {
        val KEY_API_KEY = stringPreferencesKey("api_key")
        val KEY_ENTERPRISE_API_KEY = stringPreferencesKey("enterprise_api_key")
        val KEY_COOKIE = stringPreferencesKey("cookie")
    }

    override suspend fun saveApiKey(key: String) {
        dataStore.edit { it[KEY_API_KEY] = key }
    }

    override fun getApiKey(): Flow<String> =
        dataStore.data.map { it[KEY_API_KEY] ?: "" }

    override suspend fun clearApiKey() {
        dataStore.edit { it.remove(KEY_API_KEY) }
    }

    override suspend fun saveEnterpriseApiKey(key: String) {
        dataStore.edit { it[KEY_ENTERPRISE_API_KEY] = key }
    }

    override fun getEnterpriseApiKey(): Flow<String> =
        dataStore.data.map { it[KEY_ENTERPRISE_API_KEY] ?: "" }

    override suspend fun clearEnterpriseApiKey() {
        dataStore.edit { it.remove(KEY_ENTERPRISE_API_KEY) }
    }

    override suspend fun saveCookie(cookie: String) {
        dataStore.edit { it[KEY_COOKIE] = cookie }
    }

    override fun getCookie(): Flow<String> =
        dataStore.data.map { it[KEY_COOKIE] ?: "" }

    override suspend fun clearCookie() {
        dataStore.edit { it.remove(KEY_COOKIE) }
    }

    override suspend fun getApiKeySync(): String =
        dataStore.data.map { it[KEY_API_KEY] ?: "" }.first()

    override suspend fun getEnterpriseApiKeySync(): String =
        dataStore.data.map { it[KEY_ENTERPRISE_API_KEY] ?: "" }.first()

    override suspend fun getCookieSync(): String =
        dataStore.data.map { it[KEY_COOKIE] ?: "" }.first()
}
