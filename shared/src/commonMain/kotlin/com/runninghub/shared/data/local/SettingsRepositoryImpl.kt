package com.runninghub.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    companion object {
        // TODO: Migrate token/API key storage to EncryptedSharedPreferences (Android) or Keychain (iOS)
        // Currently stored as plaintext in DataStore — vulnerable to rooted device / backup extraction
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_ENTERPRISE_API_KEY = stringPreferencesKey("enterprise_api_key")
        private val KEY_COOKIE = stringPreferencesKey("user_cookie")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_LAST_KNOWN_COINS = stringPreferencesKey("last_known_coins")
        private val KEY_QUICK_CREATE_DRAFT = stringPreferencesKey("quick_create_draft")
    }

    override suspend fun getApiKey(): String? =
        dataStore.data.map { it[KEY_API_KEY] }.first()

    override suspend fun setApiKey(key: String) {
        dataStore.edit { it[KEY_API_KEY] = key }
    }

    override suspend fun clearApiKey() {
        dataStore.edit { it.remove(KEY_API_KEY) }
    }

    override suspend fun getEnterpriseApiKey(): String? =
        dataStore.data.map { it[KEY_ENTERPRISE_API_KEY] }.first()

    override suspend fun setEnterpriseApiKey(key: String) {
        dataStore.edit { it[KEY_ENTERPRISE_API_KEY] = key }
    }

    override suspend fun clearEnterpriseApiKey() {
        dataStore.edit { it.remove(KEY_ENTERPRISE_API_KEY) }
    }

    override suspend fun getCookie(): String? =
        dataStore.data.map { it[KEY_COOKIE] }.first()

    override suspend fun setCookie(cookie: String) {
        dataStore.edit { it[KEY_COOKIE] = cookie }
    }

    override suspend fun clearCookie() {
        dataStore.edit { it.remove(KEY_COOKIE) }
    }

    override suspend fun getAuthToken(): String? =
        dataStore.data.map { it[KEY_AUTH_TOKEN] }.first()

    override suspend fun setAuthToken(token: String) {
        dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    override suspend fun clearAuthToken() {
        dataStore.edit { it.remove(KEY_AUTH_TOKEN) }
    }

    override suspend fun getRefreshToken(): String? =
        dataStore.data.map { it[KEY_REFRESH_TOKEN] }.first()

    override suspend fun setRefreshToken(token: String) {
        dataStore.edit { it[KEY_REFRESH_TOKEN] = token }
    }

    override suspend fun clearRefreshToken() {
        dataStore.edit { it.remove(KEY_REFRESH_TOKEN) }
    }

    override suspend fun isLoggedIn(): Boolean =
        !getAuthToken().isNullOrEmpty()

    override suspend fun getLastKnownCoins(): String? =
        dataStore.data.map { it[KEY_LAST_KNOWN_COINS] }.first()

    override suspend fun setLastKnownCoins(coins: String) {
        dataStore.edit { it[KEY_LAST_KNOWN_COINS] = coins }
    }

    override suspend fun clearLastKnownCoins() {
        dataStore.edit { it.remove(KEY_LAST_KNOWN_COINS) }
    }

    override suspend fun getQuickCreateDraft(): String? =
        dataStore.data.map { it[KEY_QUICK_CREATE_DRAFT] }.first()

    override suspend fun saveQuickCreateDraft(json: String) {
        dataStore.edit { it[KEY_QUICK_CREATE_DRAFT] = json }
    }

    override suspend fun clearQuickCreateDraft() {
        dataStore.edit { it.remove(KEY_QUICK_CREATE_DRAFT) }
    }

    override suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
