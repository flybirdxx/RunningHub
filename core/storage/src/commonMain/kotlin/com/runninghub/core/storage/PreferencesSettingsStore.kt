package com.runninghub.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 基于 Preferences DataStore 的迁移期本地设置实现。
 *
 * 本类同时实现 [CredentialStore]、[BalanceCache] 和 [QuickCreateDraftStore]，
 * 是为了在 L1 阶段先把启动组合根从 `sharedModule` 解耦。凭据仍暂存于普通
 * DataStore，不满足 L2 安全存储要求；后续 Android Keystore 与 iOS Keychain
 * 接入后，应只替换 [CredentialStore] 的实现，余额缓存和草稿继续保留在非敏感存储。
 *
 * @param dataStore 应用级 Preferences DataStore，由 [createDataStore] 创建并保证平台文件路径一致。
 */
class PreferencesSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : CredentialStore,
    BalanceCache,
    QuickCreateDraftStore {

    companion object {
        // TODO(RH-storage-security): 当前凭据仍存于普通 DataStore，root 设备或备份提取存在泄露风险。
        // Android 接入加密存储、iOS 接入 Keychain 后，应把 API Key、Cookie 和 token 键迁出本实现。
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
        // 迁移期该实现同时承载凭据、余额和草稿；注销时沿用旧行为整体清理。
        // L2 安全存储拆分后，凭据清理不应再删除非敏感草稿。
        dataStore.edit { it.clear() }
    }
}
