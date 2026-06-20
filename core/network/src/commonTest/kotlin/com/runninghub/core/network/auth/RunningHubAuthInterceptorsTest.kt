package com.runninghub.core.network.auth

import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RunningHubAuthInterceptorsTest {
    @Test
    fun `runninghub requests receive stored authorization and cookie headers`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "access-token",
            refreshToken = "refresh-token",
            cookie = "SESSION=abc",
        )
        var authorizationHeader: String? = null
        var cookieHeader: String? = null
        val client = HttpClient(
            MockEngine { request ->
                authorizationHeader = request.headers[HttpHeaders.Authorization]
                cookieHeader = request.headers[HttpHeaders.Cookie]
                respond(content = "{}", status = HttpStatusCode.OK)
            }
        ).applyAuthInterceptors(store)

        client.get("https://www.runninghub.cn/api/test")

        assertEquals("Bearer access-token", authorizationHeader)
        assertEquals("SESSION=abc", cookieHeader)
    }

    @Test
    fun `unauthorized response notifies session expiration when refresh fails`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "expired-token",
            refreshToken = null,
            cookie = null,
        )
        var expirationNotifications = 0
        val client = HttpClient(
            MockEngine {
                respond(content = "{}", status = HttpStatusCode.Unauthorized)
            }
        ).applyAuthInterceptors(store) {
            expirationNotifications += 1
        }

        client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText()

        assertEquals(1, expirationNotifications)
    }

    private fun HttpClient.applyAuthInterceptors(
        store: FakeCredentialStore,
        onSessionExpired: suspend () -> Unit = {},
    ): HttpClient {
        val refreshClient = HttpClient(
            MockEngine {
                error("Refresh client should not be called in this test")
            }
        )
        installRunningHubAuthInterceptors(
            credentialStore = store,
            tokenRefresher = TokenRefresher(
                refreshClient = refreshClient,
                credentialStore = store,
                refreshEndpoint = "https://example.test/token/refresh",
            ),
            onSessionExpired = onSessionExpired,
        )
        return this
    }

    private class FakeCredentialStore(
        var authToken: String?,
        var refreshToken: String?,
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
        override suspend fun getRefreshToken(): String? = refreshToken
        override suspend fun setRefreshToken(token: String) {
            refreshToken = token
        }
        override suspend fun clearRefreshToken() {
            refreshToken = null
        }
        override suspend fun isLoggedIn(): Boolean = !authToken.isNullOrEmpty()
        override suspend fun clearAll() {
            authToken = null
            refreshToken = null
            cookie = null
        }
    }
}
