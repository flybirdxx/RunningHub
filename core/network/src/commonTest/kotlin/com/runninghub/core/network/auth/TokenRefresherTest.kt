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

    /**
     * 刷新响应必须按 JSON 语义解码，而不是用正则截取原始文本。
     *
     * 服务端或网关可能把 token 中的安全字符写成 JSON unicode escape；正则会把
     * `\u002D` 原样保存，导致后续 Authorization 使用错误 token。
     */
    @Test
    fun `refreshAfterUnauthorized decodes escaped token JSON`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "old-refresh",
        )
        val refresher = refresherWithMock(store) {
            """
                {
                  "access_token": "new\u002Daccess",
                  "refresh_token": "new\u002Drefresh"
                }
            """.trimIndent()
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertTrue(refreshed)
        assertEquals("new-access", store.authToken)
        assertEquals("new-refresh", store.refreshToken)
    }

    /**
     * 兼容用户中心旧响应 envelope，同时保持 JSON escape 解码语义。
     *
     * shared 迁移期仍有调用路径使用 `code/msg/data` 包装格式；刷新组件不能只支持
     * 扁平 token 字段，否则手动会话恢复会误判刷新失败。
     */
    @Test
    fun `refreshAfterUnauthorized decodes token envelope JSON`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "old-refresh",
        )
        val refresher = refresherWithMock(store) {
            """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "access_token": "new\u002Daccess",
                    "refresh_token": "new\u002Drefresh",
                    "expire_in": "3600"
                  }
                }
            """.trimIndent()
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertTrue(refreshed)
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

    @Test
    fun `refreshAfterUnauthorized does not rewrite credentials cleared during logout`() = runBlocking {
        val store = FakeCredentialStore(
            authToken = "old-access",
            refreshToken = "old-refresh",
        )
        val refresher = refresherWithMock(store) {
            // 模拟用户主动 logout 与 401 refresh 并发：远端响应回来前，本地凭据已经被清空。
            store.clearAll()
            """
                {
                  "access_token": "new-access",
                  "refresh_token": "new-refresh"
                }
            """.trimIndent()
        }

        val refreshed = refresher.refreshAfterUnauthorized(tokenBeforeLock = "old-access")

        assertFalse(refreshed)
        assertEquals(null, store.authToken)
        assertEquals(null, store.refreshToken)
    }

    private fun refresherWithMock(
        store: FakeCredentialStore,
        responseBody: suspend (authorizationHeader: String?) -> String,
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
