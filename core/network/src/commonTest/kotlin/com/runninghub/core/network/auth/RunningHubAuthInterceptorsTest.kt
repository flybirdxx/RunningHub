package com.runninghub.core.network.auth

import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    fun `lookalike runninghub hosts do not receive stored authorization or cookie headers`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "access-token",
            refreshToken = "refresh-token",
            cookie = "SESSION=abc",
        )
        val capturedHeaders = mutableListOf<Pair<String?, String?>>()
        val client = HttpClient(
            MockEngine { request ->
                capturedHeaders += request.headers[HttpHeaders.Authorization] to request.headers[HttpHeaders.Cookie]
                respond(content = "{}", status = HttpStatusCode.OK)
            }
        ).applyAuthInterceptors(store)

        client.get("https://runninghub.cn.example.com/api/test")
        client.get("https://evilrunninghub.cn/api/test")

        assertEquals(listOf<Pair<String?, String?>>(null to null, null to null), capturedHeaders)
    }

    @Test
    fun `lookalike runninghub host unauthorized response does not refresh token`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = "SESSION=abc",
        )
        var refreshCalls = 0
        var expirationNotifications = 0
        val client = HttpClient(
            MockEngine {
                respond(content = """{"error":"third-party-unauthorized"}""", status = HttpStatusCode.Unauthorized)
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
            onSessionExpired = { expirationNotifications += 1 },
        )

        val body = client.get("https://runninghub.cn.example.com/api/needs-auth").bodyAsText()

        assertEquals("""{"error":"third-party-unauthorized"}""", body)
        assertEquals(0, refreshCalls)
        assertEquals(0, expirationNotifications)
        assertEquals("old-access", store.authToken)
    }

    @Test
    fun `unauthorized response refreshes token and retries original request once`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = "SESSION=abc",
        )
        val authorizationHeaders = mutableListOf<String?>()
        var refreshCalls = 0
        var expirationNotifications = 0
        val client = HttpClient(
            MockEngine { request ->
                val authorization = request.headers[HttpHeaders.Authorization]
                authorizationHeaders += authorization
                if (authorization == "Bearer new-access") {
                    respond(content = """{"ok":true}""", status = HttpStatusCode.OK)
                } else {
                    respond(content = """{"error":"expired"}""", status = HttpStatusCode.Unauthorized)
                }
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
            onSessionExpired = { expirationNotifications += 1 },
        )

        val body = client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText()

        assertEquals("""{"ok":true}""", body)
        assertEquals(listOf<String?>("Bearer old-access", "Bearer new-access"), authorizationHeaders)
        assertEquals(1, refreshCalls)
        assertEquals(0, expirationNotifications)
        assertEquals("new-access", store.authToken)
        assertEquals("new-refresh", store.refreshToken)
    }

    @Test
    fun `post unauthorized response refreshes token but does not retry without replay marker`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = null,
        )
        val authorizationHeaders = mutableListOf<String?>()
        var refreshCalls = 0
        var expirationNotifications = 0
        val client = HttpClient(
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                respond(content = """{"error":"expired"}""", status = HttpStatusCode.Unauthorized)
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
            onSessionExpired = { expirationNotifications += 1 },
        )

        val body = client.post("https://www.runninghub.cn/api/submit") {
            setBody("""{"name":"sample"}""")
        }.bodyAsText()

        assertEquals("""{"error":"expired"}""", body)
        assertEquals(listOf<String?>("Bearer old-access"), authorizationHeaders)
        assertEquals(1, refreshCalls)
        assertEquals(0, expirationNotifications)
        assertEquals("new-access", store.authToken)
    }

    @Test
    fun `post unauthorized response retries once when request marks body replayable`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = null,
        )
        val authorizationHeaders = mutableListOf<String?>()
        var refreshCalls = 0
        val client = HttpClient(
            MockEngine { request ->
                val authorization = request.headers[HttpHeaders.Authorization]
                authorizationHeaders += authorization
                if (authorization == "Bearer new-access") {
                    respond(content = """{"ok":true}""", status = HttpStatusCode.OK)
                } else {
                    respond(content = """{"error":"expired"}""", status = HttpStatusCode.Unauthorized)
                }
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
        )

        val body = client.post("https://www.runninghub.cn/api/submit") {
            setBody("""{"name":"sample"}""")
            markRunningHubAuthRetryAllowed()
        }.bodyAsText()

        assertEquals("""{"ok":true}""", body)
        assertEquals(listOf<String?>("Bearer old-access", "Bearer new-access"), authorizationHeaders)
        assertEquals(1, refreshCalls)
        assertEquals("new-access", store.authToken)
    }

    @Test
    fun `explicit authorization is not replaced or retried after unauthorized response`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "stored-access",
            refreshToken = "refresh-token",
            cookie = "SESSION=abc",
        )
        val authorizationHeaders = mutableListOf<String?>()
        var refreshCalls = 0
        val client = HttpClient(
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                respond(content = """{"error":"custom-auth-expired"}""", status = HttpStatusCode.Unauthorized)
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
        )

        val body = client.get("https://www.runninghub.cn/api/custom-auth") {
            header(HttpHeaders.Authorization, "Bearer custom-token")
        }.bodyAsText()

        assertEquals("""{"error":"custom-auth-expired"}""", body)
        assertEquals(listOf<String?>("Bearer custom-token"), authorizationHeaders)
        assertEquals(0, refreshCalls)
        assertEquals("stored-access", store.authToken)
    }

    @Test
    fun `explicit cookie is preserved when stored authorization is refreshed and retried`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = "SESSION=stored",
        )
        val capturedHeaders = mutableListOf<Pair<String?, String?>>()
        var refreshCalls = 0
        val client = HttpClient(
            MockEngine { request ->
                val authorization = request.headers[HttpHeaders.Authorization]
                val cookie = request.headers[HttpHeaders.Cookie]
                capturedHeaders += authorization to cookie
                if (authorization == "Bearer new-access" && cookie == "SESSION=custom") {
                    respond(content = """{"ok":true}""", status = HttpStatusCode.OK)
                } else {
                    respond(content = """{"error":"expired"}""", status = HttpStatusCode.Unauthorized)
                }
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
        )

        val body = client.get("https://www.runninghub.cn/api/custom-cookie") {
            header(HttpHeaders.Cookie, "SESSION=custom")
        }.bodyAsText()

        assertEquals("""{"ok":true}""", body)
        assertEquals(
            listOf<Pair<String?, String?>>(
                "Bearer old-access" to "SESSION=custom",
                "Bearer new-access" to "SESSION=custom",
            ),
            capturedHeaders,
        )
        assertEquals(1, refreshCalls)
        assertEquals("new-access", store.authToken)
    }

    @Test
    fun `concurrent unauthorized responses share one refresh request`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = null,
        )
        val bothOldRequestsSeen = CompletableDeferred<Unit>()
        val lock = Mutex()
        var oldRequests = 0
        var newRequests = 0
        var refreshCalls = 0
        val client = HttpClient(
            MockEngine { request ->
                when (request.headers[HttpHeaders.Authorization]) {
                    "Bearer old-access" -> {
                        lock.withLock {
                            oldRequests += 1
                            if (oldRequests == 2) bothOldRequestsSeen.complete(Unit)
                        }
                        bothOldRequestsSeen.await()
                        respond(content = """{"error":"expired"}""", status = HttpStatusCode.Unauthorized)
                    }
                    "Bearer new-access" -> {
                        lock.withLock { newRequests += 1 }
                        respond(content = """{"ok":true}""", status = HttpStatusCode.OK)
                    }
                    else -> respond(content = """{"error":"missing"}""", status = HttpStatusCode.Unauthorized)
                }
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
        )

        listOf(
            async { client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText() },
            async { client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText() },
        ).awaitAll()

        assertEquals(2, oldRequests)
        assertEquals(2, newRequests)
        assertEquals(1, refreshCalls)
        assertEquals("new-access", store.authToken)
    }

    @Test
    fun `retry still unauthorized notifies expiration once and does not refresh again`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "refresh-token",
            cookie = null,
        )
        val authorizationHeaders = mutableListOf<String?>()
        var refreshCalls = 0
        var expirationNotifications = 0
        val client = HttpClient(
            MockEngine { request ->
                authorizationHeaders += request.headers[HttpHeaders.Authorization]
                respond(content = """{"error":"still-expired"}""", status = HttpStatusCode.Unauthorized)
            }
        ).applyAuthInterceptors(
            store = store,
            refreshResponseBody = {
                refreshCalls += 1
                """{"access_token":"new-access","refresh_token":"new-refresh"}"""
            },
            onSessionExpired = { expirationNotifications += 1 },
        )

        client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText()

        assertEquals(listOf<String?>("Bearer old-access", "Bearer new-access"), authorizationHeaders)
        assertEquals(1, refreshCalls)
        assertEquals(1, expirationNotifications)
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

    @Test
    fun `concurrent refresh failures notify session expiration once`() = runBlocking {
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

        listOf(
            async { client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText() },
            async { client.get("https://www.runninghub.cn/api/needs-auth").bodyAsText() },
        ).awaitAll()

        assertEquals(1, expirationNotifications)
    }

    private fun HttpClient.applyAuthInterceptors(
        store: FakeCredentialStore,
        refreshResponseBody: (() -> String)? = null,
        onSessionExpired: suspend () -> Unit = {},
    ): HttpClient {
        val refreshClient = HttpClient(
            MockEngine {
                val responseBody = refreshResponseBody?.invoke()
                    ?: error("Refresh client should not be called in this test")
                respond(
                    content = responseBody,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        ) {
            install(ContentNegotiation) {
                json()
            }
        }
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
