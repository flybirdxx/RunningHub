package com.runninghub.core.network.auth

import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenRefresherTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `refreshAfterUnauthorized writes refreshed tokens`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "old-refresh",
        )
        var authorizationHeader: String? = null
        val refresher = refresherWithMock(store) { requestAuth ->
            authorizationHeader = requestAuth
            """
                {
                  "access_token": "new-access",
                  "refresh_token": "new-refresh"
                }
            """.trimIndent()
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertTrue(refreshed)
        assertEquals("Bearer old-refresh", authorizationHeader)
        assertEquals("new-access", store.authToken)
        assertEquals("new-refresh", store.refreshToken)
    }

    @Test
    fun `refreshAfterUnauthorized skips network when token was refreshed by another coroutine`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "new-access",
            refreshToken = "old-refresh",
        )
        var networkCalls = 0
        val refresher = refresherWithMock(store) {
            networkCalls += 1
            error("Network should not be called when token already changed")
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertTrue(refreshed)
        assertEquals(0, networkCalls)
    }

    @Test
    fun `refreshAfterUnauthorized returns false when refresh token is missing`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = null,
        )
        var networkCalls = 0
        val refresher = refresherWithMock(store) {
            networkCalls += 1
            """{}"""
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertFalse(refreshed)
        assertEquals(0, networkCalls)
    }

    private fun refresherWithMock(
        store: FakeCredentialStore,
        responseBody: (authorizationHeader: String?) -> String,
    ): TokenRefresher {
        val engine = MockEngine { request ->
            respond(
                content = responseBody(request.headers[HttpHeaders.Authorization]),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return TokenRefresher(
            refreshClient = client,
            credentialStore = store,
            refreshEndpoint = "https://example.test/token/refresh",
        )
    }

    private class FakeCredentialStore(
        var authToken: String?,
        var refreshToken: String?,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) {}
        override suspend fun clearApiKey() {}
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) {}
        override suspend fun clearEnterpriseApiKey() {}
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) {}
        override suspend fun clearCookie() {}
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
        }
    }
}
