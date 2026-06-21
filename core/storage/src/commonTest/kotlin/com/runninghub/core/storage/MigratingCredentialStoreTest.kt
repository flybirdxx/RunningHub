package com.runninghub.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * 验证迁移包装器只处理敏感凭据，不误删仍由 Preferences DataStore 承载的非敏感数据。
 */
class MigratingCredentialStoreTest {
    @Test
    fun `getAuthToken migrates legacy token into primary store`() = runTest {
        val primary = InMemoryCredentialStore()
        val legacy = InMemoryCredentialStore(authToken = "legacy-token")
        val store = MigratingCredentialStore(primary = primary, legacy = legacy)

        assertEquals("legacy-token", store.getAuthToken())
        assertEquals("legacy-token", primary.getAuthToken())
        assertNull(legacy.getAuthToken())
        assertTrue(store.isLoggedIn())
    }

    @Test
    fun `setCookie writes primary store and clears legacy value`() = runTest {
        val primary = InMemoryCredentialStore()
        val legacy = InMemoryCredentialStore(cookie = "old-cookie")
        val store = MigratingCredentialStore(primary = primary, legacy = legacy)

        store.setCookie("new-cookie")

        assertEquals("new-cookie", primary.getCookie())
        assertNull(legacy.getCookie())
    }

    @Test
    fun `clearAll clears credentials without invoking legacy destructive clear`() = runTest {
        val primary = InMemoryCredentialStore(
            apiKey = "api",
            enterpriseApiKey = "enterprise",
            cookie = "cookie",
            authToken = "auth",
            refreshToken = "refresh",
        )
        val legacy = InMemoryCredentialStore(
            apiKey = "old-api",
            enterpriseApiKey = "old-enterprise",
            cookie = "old-cookie",
            authToken = "old-auth",
            refreshToken = "old-refresh",
        )
        val store = MigratingCredentialStore(primary = primary, legacy = legacy)

        store.clearAll()

        assertTrue(primary.clearAllCalled)
        assertFalse(legacy.clearAllCalled)
        assertNull(primary.getApiKey())
        assertNull(primary.getEnterpriseApiKey())
        assertNull(primary.getCookie())
        assertNull(primary.getAuthToken())
        assertNull(primary.getRefreshToken())
        assertNull(legacy.getApiKey())
        assertNull(legacy.getEnterpriseApiKey())
        assertNull(legacy.getCookie())
        assertNull(legacy.getAuthToken())
        assertNull(legacy.getRefreshToken())
    }

    private class InMemoryCredentialStore(
        private var apiKey: String? = null,
        private var enterpriseApiKey: String? = null,
        private var cookie: String? = null,
        private var authToken: String? = null,
        private var refreshToken: String? = null,
    ) : CredentialStore {
        var clearAllCalled: Boolean = false
            private set

        override suspend fun getApiKey(): String? = apiKey

        override suspend fun setApiKey(key: String) {
            apiKey = key
        }

        override suspend fun clearApiKey() {
            apiKey = null
        }

        override suspend fun getEnterpriseApiKey(): String? = enterpriseApiKey

        override suspend fun setEnterpriseApiKey(key: String) {
            enterpriseApiKey = key
        }

        override suspend fun clearEnterpriseApiKey() {
            enterpriseApiKey = null
        }

        override suspend fun getCookie(): String? = cookie

        override suspend fun setCookie(cookie: String) {
            this.cookie = cookie
        }

        override suspend fun clearCookie() {
            cookie = null
        }

        override suspend fun getAuthToken(): String? = authToken

        override suspend fun setAuthToken(token: String) {
            authToken = token
        }

        override suspend fun clearAuthToken() {
            authToken = null
        }

        override suspend fun getRefreshToken(): String? = refreshToken

        override suspend fun setRefreshToken(token: String) {
            refreshToken = token
        }

        override suspend fun clearRefreshToken() {
            refreshToken = null
        }

        override suspend fun isLoggedIn(): Boolean =
            !authToken.isNullOrEmpty()

        override suspend fun clearAll() {
            clearAllCalled = true
            apiKey = null
            enterpriseApiKey = null
            cookie = null
            authToken = null
            refreshToken = null
        }
    }
}
