package com.runninghub.core.network.auth

import com.runninghub.core.storage.CredentialStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthHeaderProviderTest {

    @Test
    fun `provideForRunningHubRequest returns bearer token and cookie when request has no auth headers`() = runBlocking {
        val provider = AuthHeaderProvider(
            FakeCredentialStore(
                authToken = "access-token",
                cookie = "SESSION=abc",
            )
        )

        val headers = provider.provideForRunningHubRequest(
            hasAuthorizationHeader = false,
            hasCookieHeader = false,
        )

        assertEquals("Bearer access-token", headers.authorization)
        assertEquals("SESSION=abc", headers.cookie)
    }

    @Test
    fun `provideForRunningHubRequest skips defaults when authorization was explicitly set`() = runBlocking {
        val provider = AuthHeaderProvider(
            FakeCredentialStore(
                authToken = "access-token",
                cookie = "SESSION=abc",
            )
        )

        val headers = provider.provideForRunningHubRequest(
            hasAuthorizationHeader = true,
            hasCookieHeader = false,
        )

        assertNull(headers.authorization)
        assertNull(headers.cookie)
    }

    @Test
    fun `provideForRunningHubRequest preserves explicit cookie while adding missing bearer token`() = runBlocking {
        val provider = AuthHeaderProvider(
            FakeCredentialStore(
                authToken = "access-token",
                cookie = "SESSION=abc",
            )
        )

        val headers = provider.provideForRunningHubRequest(
            hasAuthorizationHeader = false,
            hasCookieHeader = true,
        )

        assertEquals("Bearer access-token", headers.authorization)
        assertNull(headers.cookie)
    }

    @Test
    fun `provideForRunningHubRequest ignores blank stored credentials`() = runBlocking {
        val provider = AuthHeaderProvider(
            FakeCredentialStore(
                authToken = " ",
                cookie = "",
            )
        )

        val headers = provider.provideForRunningHubRequest(
            hasAuthorizationHeader = false,
            hasCookieHeader = false,
        )

        assertNull(headers.authorization)
        assertNull(headers.cookie)
    }

    private class FakeCredentialStore(
        var authToken: String?,
        var cookie: String?,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) {}
        override suspend fun clearApiKey() {}
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) {}
        override suspend fun clearEnterpriseApiKey() {}
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
        override suspend fun getRefreshToken(): String? = null
        override suspend fun setRefreshToken(token: String) {}
        override suspend fun clearRefreshToken() {}
        override suspend fun isLoggedIn(): Boolean = !authToken.isNullOrEmpty()
        override suspend fun clearAll() {
            authToken = null
            cookie = null
        }
    }
}
