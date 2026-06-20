package com.runninghub.shared.data.repository

import com.runninghub.core.model.InputNode
import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.RunningHubApi
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

class WebAppRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `runTask fails before network when api key is missing`() = runBlocking {
        var networkCalls = 0
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = null),
            response = { _ ->
                networkCalls += 1
                error("Network should not be called without API Key")
            },
        )

        val result = repository.runTask(
            webappId = 10L,
            nodeInfoList = listOf(inputNode()),
        )

        assertTrue(result.isFailure)
        assertEquals(0, networkCalls)
        assertEquals("请先在设置中绑定 API Key", result.exceptionOrNull()?.message)
    }

    @Test
    fun `runTask injects api key from credential store`() = runBlocking {
        var capturedBody = ""
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = { body ->
                capturedBody = body
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "taskId": 123,
                        "taskStatus": "SUBMITTED"
                      }
                    }
                """.trimIndent()
            },
        )

        val result = repository.runTask(
            webappId = 10L,
            nodeInfoList = listOf(inputNode()),
        ).getOrThrow()

        assertEquals(123L, result.taskId)
        assertTrue(capturedBody.contains(""""apiKey":"local-api-key""""))
        assertTrue(capturedBody.contains(""""webappId":10"""))
    }

    private fun repositoryWithMock(
        credentialStore: CredentialStore,
        response: (String) -> String,
    ): WebAppRepositoryImpl {
        val engine = MockEngine { request ->
            respond(
                content = response(request.body.toRequestBodyText()),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return WebAppRepositoryImpl(
            api = RunningHubApi(client),
            credentialStore = credentialStore,
        )
    }

    private fun inputNode(): InputNode =
        InputNode(
            nodeId = "1",
            nodeName = "Prompt",
            fieldName = "prompt",
            fieldValue = "hello",
            fieldData = null,
            fieldType = "TEXT",
            description = null,
        )

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
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
