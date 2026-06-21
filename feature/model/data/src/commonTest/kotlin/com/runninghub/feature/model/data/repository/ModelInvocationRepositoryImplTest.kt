package com.runninghub.feature.model.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.model.data.remote.api.ModelCatalogApi
import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldType
import com.runninghub.feature.model.domain.ModelFieldValue
import com.runninghub.feature.model.domain.ModelInvocationIssueCode
import com.runninghub.feature.model.domain.ModelInvocationRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
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
    fun `submitStandardModel resolves endpoint from sku detail by model id`() = runBlocking {
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/sku/detail" -> respond(
                    content = """
                        {
                          "code": 0,
                          "msg": "success",
                          "data": {
                            "id": "sku-1",
                            "name": "Image V2",
                            "rhEndpoint": "/rhart-image/text-to-image",
                            "inputConfigJson": "[]"
                          }
                        }
                    """.trimIndent(),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                "/openapi/v2/rhart-image/text-to-image" -> respond(
                    content = """{"taskId":"task-1","status":"SUBMITTED"}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
                else -> error("Unexpected path: ${request.url.encodedPath}")
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = ModelInvocationRepositoryImpl(
            client = client,
            modelCatalogApi = ModelCatalogApi(client),
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            endpointRegistry = ModelEndpointRegistry(),
        )

        val task = repository.submitStandardModel(
            ModelInvocationRequest(
                modelId = "sku-1",
                fields = listOf(ApiModelField("prompt", "prompt", ApiModelFieldType.STRING, required = true)),
                values = mapOf("prompt" to ModelFieldValue.Text("a cat")),
            )
        ).getOrThrow()

        assertEquals("task-1", task.taskId)
        assertEquals("SUBMITTED", task.status)
        assertEquals(listOf("/api/sku/detail", "/openapi/v2/rhart-image/text-to-image"), requestedPaths)
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
        assertEquals(ModelInvocationIssueCode.API_KEY_MISSING, result.exceptionOrNull()?.message)
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

    @Test
    fun `uploadMedia maps missing remote url to stable issue code`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 0,
                      "message": "success",
                      "data": {}
                    }
                """.trimIndent()
            },
        )

        val result = repository.uploadMedia(
            fileBytes = "image".encodeToByteArray(),
            fileName = "image.png",
            contentType = "image/png",
        )

        assertTrue(result.isFailure)
        assertEquals(ModelInvocationIssueCode.MEDIA_UPLOAD_EMPTY_URL, result.exceptionOrNull()?.message)
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
            modelCatalogApi = ModelCatalogApi(client),
            credentialStore = credentialStore,
            endpointRegistry = ModelEndpointRegistry(),
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
