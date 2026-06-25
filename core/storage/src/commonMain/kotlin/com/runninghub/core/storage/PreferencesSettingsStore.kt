package com.runninghub.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 基于 Preferences DataStore 的非敏感本地设置实现。
 *
 * 本类同时实现 [CredentialStore]、[BalanceCache] 和 [QuickCreateDraftStore]，
 * 是为了兼容 L1 迁移期间已经写入 Preferences 的历史凭据。生产组合根不应再直接把本类绑定为
 * [CredentialStore]；应通过 [MigratingCredentialStore] 把旧凭据迁移到平台安全存储。
 * 余额缓存和草稿属于非敏感数据，继续由本类保存。
 *
 * @param dataStore 应用级 Preferences DataStore，由 [createDataStore] 创建并保证平台文件路径一致。
 */
class PreferencesSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : CredentialStore,
    BalanceCache,
    QuickCreateDraftStore,
    QuickCreateModelSelectionStore,
    ModelCatalogCacheStore,
    AppStartupStore {

    companion object {
        // 这些凭据键只用于读取和清理 L1 迁移前的旧数据；新凭据写入必须进入平台安全存储。
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_ENTERPRISE_API_KEY = stringPreferencesKey("enterprise_api_key")
        private val KEY_COOKIE = stringPreferencesKey("user_cookie")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_LAST_KNOWN_COINS = stringPreferencesKey("last_known_coins")
        private val KEY_QUICK_CREATE_DRAFT = stringPreferencesKey("quick_create_draft")
        private val KEY_QUICK_CREATE_LAST_IMAGE_SERVICE_MODEL =
            stringPreferencesKey("quick_create_last_image_service_model")
        private val KEY_QUICK_CREATE_LAST_VIDEO_SERVICE_MODEL =
            stringPreferencesKey("quick_create_last_video_service_model")
        private val KEY_INITIAL_MODEL_CATALOG_PRELOAD_STARTED =
            stringPreferencesKey("initial_model_catalog_preload_started")
        private const val KEY_MODEL_CATALOG_GROUP_PREFIX = "model_catalog_standard_group_"
        private const val KEY_MODEL_CATALOG_LIST_PREFIX = "model_catalog_standard_list_"
        private const val KEY_MODEL_CATALOG_DETAIL_PREFIX = "model_catalog_standard_detail_"
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

    override suspend fun getLastImageServiceModelIdentityKey(): String? =
        dataStore.data.map { it[KEY_QUICK_CREATE_LAST_IMAGE_SERVICE_MODEL] }.first()

    override suspend fun saveLastImageServiceModelIdentityKey(identityKey: String) {
        dataStore.edit { it[KEY_QUICK_CREATE_LAST_IMAGE_SERVICE_MODEL] = identityKey }
    }

    override suspend fun getLastVideoServiceModelIdentityKey(): String? =
        dataStore.data.map { it[KEY_QUICK_CREATE_LAST_VIDEO_SERVICE_MODEL] }.first()

    override suspend fun saveLastVideoServiceModelIdentityKey(identityKey: String) {
        dataStore.edit { it[KEY_QUICK_CREATE_LAST_VIDEO_SERVICE_MODEL] = identityKey }
    }

    override suspend fun hasStartedInitialModelCatalogPreload(): Boolean =
        dataStore.data.map { it[KEY_INITIAL_MODEL_CATALOG_PRELOAD_STARTED] == "true" }.first()

    override suspend fun markInitialModelCatalogPreloadStarted() {
        dataStore.edit { it[KEY_INITIAL_MODEL_CATALOG_PRELOAD_STARTED] = "true" }
    }

    override suspend fun getStandardModelGroups(cacheKey: String): String? =
        dataStore.data.map { it[stringPreferencesKey(KEY_MODEL_CATALOG_GROUP_PREFIX + cacheKey)] }.first()

    override suspend fun saveStandardModelGroups(cacheKey: String, json: String) {
        dataStore.edit { it[stringPreferencesKey(KEY_MODEL_CATALOG_GROUP_PREFIX + cacheKey)] = json }
    }

    override suspend fun getStandardModelList(cacheKey: String): String? =
        dataStore.data.map { it[stringPreferencesKey(KEY_MODEL_CATALOG_LIST_PREFIX + cacheKey)] }.first()

    override suspend fun saveStandardModelList(cacheKey: String, json: String) {
        dataStore.edit { it[stringPreferencesKey(KEY_MODEL_CATALOG_LIST_PREFIX + cacheKey)] = json }
    }

    override suspend fun getStandardModelDetail(modelId: String): String? =
        dataStore.data.map { it[stringPreferencesKey(KEY_MODEL_CATALOG_DETAIL_PREFIX + modelId)] }.first()

    override suspend fun saveStandardModelDetail(modelId: String, json: String) {
        dataStore.edit { it[stringPreferencesKey(KEY_MODEL_CATALOG_DETAIL_PREFIX + modelId)] = json }
    }

    override suspend fun clearAll() {
        // 该方法只保留给仍直接依赖本类的遗留 shared 组合根；composeApp 的生产组合根已经通过
        // MigratingCredentialStore 逐项清理凭据，避免注销时误删快捷创作草稿。
        dataStore.edit { it.clear() }
    }
}
