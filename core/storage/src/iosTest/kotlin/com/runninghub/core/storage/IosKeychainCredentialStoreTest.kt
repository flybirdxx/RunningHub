package com.runninghub.core.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * iOS Keychain 凭据存储的运行契约。
 *
 * 登录链依赖 access token 写入后立刻可被 Profile、会话恢复和网络拦截器读回；
 * 如果 Keychain query 构造失败，登录会短暂进入主页面但后续页面仍判断为未登录。
 */
class IosKeychainCredentialStoreTest {
    @Test
    fun keychainStorePersistsTokensOrReportsUnavailable() = runTest {
        val store = createSecureCredentialStore()
        store.clearAll()

        try {
            store.setAuthToken("ios-test-auth-token")
        } catch (error: IllegalStateException) {
            assertTrue(
                actual = error.message?.startsWith("KEYCHAIN_WRITE_FAILED_STATUS_") == true,
                message = "Keychain write failures must be explicit, not silently drop credentials.",
            )
            assertNull(store.getAuthToken())
            assertFalse(store.isLoggedIn())
            return@runTest
        }

        store.setRefreshToken("ios-test-refresh-token")

        assertEquals("ios-test-auth-token", store.getAuthToken())
        assertEquals("ios-test-refresh-token", store.getRefreshToken())
        assertTrue(store.isLoggedIn())

        store.clearAll()

        assertNull(store.getAuthToken())
        assertNull(store.getRefreshToken())
        assertFalse(store.isLoggedIn())
    }
}
