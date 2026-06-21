package com.runninghub.core.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 创建平台安全凭据存储。
 *
 * Android actual 必须使用 Android Keystore 保护本地密文，iOS actual 必须使用 Keychain；
 * commonMain 只依赖 [CredentialStore] 抽象，避免把平台安全 API 泄漏给业务层。
 *
 * @return 只负责 API Key、Cookie、access token 和 refresh token 的安全存储实现。
 */
expect fun createSecureCredentialStore(): CredentialStore

/**
 * 将旧 Preferences 凭据平滑迁移到平台安全存储的包装器。
 *
 * L1 迁移期间 [PreferencesSettingsStore] 同时承载凭据、余额缓存和快捷创作草稿。
 * 直接把 [CredentialStore] 绑定切换到安全存储会导致老用户会话丢失；直接调用旧实现的
 * [CredentialStore.clearAll] 又会误删非敏感草稿。因此本类在首次读取每个凭据时只迁移并清理
 * 对应旧键，新写入只进入 [primary]，注销时也只清理旧凭据键，不碰余额和草稿。
 *
 * 并发约束：同一进程内多个请求可能同时读取 token。迁移阶段通过 [migrationMutex] 串行化，
 * 防止旧值被重复写入或清理时序交错；正常读写仍由具体平台安全存储保证原子性。
 *
 * @param primary Android Keystore 或 iOS Keychain backed 的目标凭据存储。
 * @param legacy 迁移期旧 Preferences 凭据存储；为 `null` 时表示无需兼容旧数据。
 */
class MigratingCredentialStore(
    private val primary: CredentialStore,
    private val legacy: CredentialStore? = null,
) : CredentialStore {
    private val migrationMutex = Mutex()

    override suspend fun getApiKey(): String? =
        readMigrating(
            readPrimary = primary::getApiKey,
            readLegacy = legacy?.let { it::getApiKey },
            writePrimary = primary::setApiKey,
            clearLegacy = legacy?.let { it::clearApiKey },
        )

    override suspend fun setApiKey(key: String) {
        primary.setApiKey(key)
        legacy?.clearApiKey()
    }

    override suspend fun clearApiKey() {
        primary.clearApiKey()
        legacy?.clearApiKey()
    }

    override suspend fun getEnterpriseApiKey(): String? =
        readMigrating(
            readPrimary = primary::getEnterpriseApiKey,
            readLegacy = legacy?.let { it::getEnterpriseApiKey },
            writePrimary = primary::setEnterpriseApiKey,
            clearLegacy = legacy?.let { it::clearEnterpriseApiKey },
        )

    override suspend fun setEnterpriseApiKey(key: String) {
        primary.setEnterpriseApiKey(key)
        legacy?.clearEnterpriseApiKey()
    }

    override suspend fun clearEnterpriseApiKey() {
        primary.clearEnterpriseApiKey()
        legacy?.clearEnterpriseApiKey()
    }

    override suspend fun getCookie(): String? =
        readMigrating(
            readPrimary = primary::getCookie,
            readLegacy = legacy?.let { it::getCookie },
            writePrimary = primary::setCookie,
            clearLegacy = legacy?.let { it::clearCookie },
        )

    override suspend fun setCookie(cookie: String) {
        primary.setCookie(cookie)
        legacy?.clearCookie()
    }

    override suspend fun clearCookie() {
        primary.clearCookie()
        legacy?.clearCookie()
    }

    override suspend fun getAuthToken(): String? =
        readMigrating(
            readPrimary = primary::getAuthToken,
            readLegacy = legacy?.let { it::getAuthToken },
            writePrimary = primary::setAuthToken,
            clearLegacy = legacy?.let { it::clearAuthToken },
        )

    override suspend fun setAuthToken(token: String) {
        primary.setAuthToken(token)
        legacy?.clearAuthToken()
    }

    override suspend fun clearAuthToken() {
        primary.clearAuthToken()
        legacy?.clearAuthToken()
    }

    override suspend fun getRefreshToken(): String? =
        readMigrating(
            readPrimary = primary::getRefreshToken,
            readLegacy = legacy?.let { it::getRefreshToken },
            writePrimary = primary::setRefreshToken,
            clearLegacy = legacy?.let { it::clearRefreshToken },
        )

    override suspend fun setRefreshToken(token: String) {
        primary.setRefreshToken(token)
        legacy?.clearRefreshToken()
    }

    override suspend fun clearRefreshToken() {
        primary.clearRefreshToken()
        legacy?.clearRefreshToken()
    }

    override suspend fun isLoggedIn(): Boolean =
        !getAuthToken().isNullOrEmpty()

    override suspend fun clearAll() {
        primary.clearAll()
        // 旧实现的 clearAll 会清空草稿和余额；这里只逐项清理敏感凭据，保留非敏感本地数据。
        legacy?.clearApiKey()
        legacy?.clearEnterpriseApiKey()
        legacy?.clearCookie()
        legacy?.clearAuthToken()
        legacy?.clearRefreshToken()
    }

    private suspend fun readMigrating(
        readPrimary: suspend () -> String?,
        readLegacy: (suspend () -> String?)?,
        writePrimary: suspend (String) -> Unit,
        clearLegacy: (suspend () -> Unit)?,
    ): String? {
        readPrimary()?.let { return it }
        if (readLegacy == null || clearLegacy == null) return null

        return migrationMutex.withLock {
            readPrimary()?.let { return@withLock it }

            val legacyValue = readLegacy()
            if (legacyValue != null) {
                // 空字符串不是有效凭据，但仍按真实旧值迁移并清理旧键，避免反复触发迁移分支。
                writePrimary(legacyValue)
                clearLegacy()
            }
            legacyValue
        }
    }
}
