package com.runninghub.shared.data.repository

import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.domain.session.SessionManager
import com.runninghub.shared.domain.session.SessionState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `refreshTokenIfNeeded refreshes expired access token`() = runBlocking {
        val expiredToken = jwtWithExp(Clock.System.now().epochSeconds - 60)
        val freshToken = jwtWithExp(Clock.System.now().epochSeconds + 3600)
        val settings = FakeSettingsRepository(
            authToken = expiredToken,
            refreshToken = "refresh-token",
        )
        val sessionManager = SessionManager()
        var refreshCalls = 0
        val engine = MockEngine { request ->
            refreshCalls += 1
            assertEquals("/uc/token/refresh", request.url.encodedPath)
            respond(
                content = """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "access_token": "$freshToken",
                        "refresh_token": "new-refresh-token",
                        "expire_in": "3600"
                      }
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = AuthRepositoryImpl(
            api = RunningHubApi(client),
            credentialStore = settings,
            sessionManager = sessionManager,
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = settings,
            ),
        )

        val token = repository.refreshTokenIfNeeded().getOrThrow()

        assertEquals(1, refreshCalls)
        assertEquals(freshToken, token)
        assertEquals(freshToken, settings.authToken)
        assertEquals("new-refresh-token", settings.refreshToken)
        assertEquals(SessionState.Authenticated, sessionManager.state.value)
    }

    @Test
    fun `refreshTokenIfNeeded marks session expired when refresh response fails`() = runBlocking {
        val expiredToken = jwtWithExp(Clock.System.now().epochSeconds - 60)
        val settings = FakeSettingsRepository(
            authToken = expiredToken,
            refreshToken = "refresh-token",
        )
        val sessionManager = SessionManager()
        val engine = MockEngine { request ->
            assertEquals("/uc/token/refresh", request.url.encodedPath)
            respond(
                content = """
                    {
                      "code": 401,
                      "msg": "TOKEN_EXPIRED",
                      "data": null
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = AuthRepositoryImpl(
            api = RunningHubApi(client),
            credentialStore = settings,
            sessionManager = sessionManager,
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = settings,
            ),
        )

        val result = repository.refreshTokenIfNeeded()

        assertTrue(result.isFailure)
        assertTrue(sessionManager.isExpired.value)
        assertEquals(SessionState.Expired, sessionManager.state.value)
    }

    @Test
    fun `login maps network failure through core network mapper`() = runBlocking {
        val settings = FakeSettingsRepository(
            authToken = null,
            refreshToken = null,
        )
        val engine = MockEngine {
            error("Unable to resolve host www.runninghub.cn")
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = AuthRepositoryImpl(
            api = RunningHubApi(client),
            credentialStore = settings,
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = settings,
            ),
        )

        val result = repository.login(phone = "13800000000", password = "password")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AuthError.Network)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun jwtWithExp(exp: Long): String {
        val header = base64Url("""{"alg":"none"}""")
        val payload = base64Url("""{"sub":"user-1","exp":$exp}""")
        return "$header.$payload.signature"
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun base64Url(value: String): String =
        Base64.Default.encode(value.encodeToByteArray())
            .trimEnd('=')
            .replace('+', '-')
            .replace('/', '_')

    private class FakeSettingsRepository(
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
        override suspend fun isLoggedIn(): Boolean = authToken != null
        override suspend fun clearAll() {
            authToken = null
            refreshToken = null
        }
    }
}
