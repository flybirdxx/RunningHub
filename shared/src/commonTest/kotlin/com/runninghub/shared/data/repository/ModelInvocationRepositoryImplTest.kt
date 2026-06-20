package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.QuickCreateApi
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
import kotlin.test.assertTrue

class ModelInvocationRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `uploadMedia fails before network when api key is missing`() = runBlocking {
        var networkCalls = 0
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = null),
            response = { _ ->
                networkCalls += 1
                error("Network should not be called without API Key")
            },
        )

        val result = repository.uploadMedia(
            fileBytes = "image".encodeToByteArray(),
            fileName = "image.png",
            contentType = "image/png",
        )

        assertTrue(result.isFailure)
        assertEquals(0, networkCalls)
        assertEquals("请先在设置中绑定 API Key", result.exceptionOrNull()?.message)
    }

    @Test
    fun `uploadMedia injects api key from credential store`() = runBlocking {
        var authorizationHeader: String? = null
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = { auth ->
                authorizationHeader = auth
                """
                    {
                      "code": 0,
                      "message": "success",
                      "data": {
                        "downloadUrl": "https://cdn.example/image.png",
                        "fileName": "image.png"
                      }
                    }
                """.trimIndent()
            },
        )

        val url = repository.uploadMedia(
            fileBytes = "image".encodeToByteArray(),
            fileName = "image.png",
            contentType = "image/png",
        ).getOrThrow()

        assertEquals("Bearer local-api-key", authorizationHeader)
        assertEquals("https://cdn.example/image.png", url)
    }

    private fun repositoryWithMock(
        credentialStore: CredentialStore,
        response: (authorizationHeader: String?) -> String,
    ): ModelInvocationRepositoryImpl {
        val engine = MockEngine { request ->
            respond(
                content = response(request.headers[HttpHeaders.Authorization]),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return ModelInvocationRepositoryImpl(
            client = client,
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = credentialStore,
        )
    }

    private class FakeCredentialStore(
        private val apiKey: String?,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = apiKey
        override suspend fun setApiKey(key: String) {}
        override suspend fun clearApiKey() {}
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) {}
        override suspend fun clearEnterpriseApiKey() {}
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) {}
        override suspend fun clearCookie() {}
        override suspend fun getAuthToken(): String? = null
        override suspend fun setAuthToken(token: String) {}
        override suspend fun clearAuthToken() {}
        override suspend fun getRefreshToken(): String? = null
        override suspend fun setRefreshToken(token: String) {}
        override suspend fun clearRefreshToken() {}
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun clearAll() {}
    }
}
