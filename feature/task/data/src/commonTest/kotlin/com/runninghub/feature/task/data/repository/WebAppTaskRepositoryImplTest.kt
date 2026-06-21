package com.runninghub.feature.task.data.repository

import com.runninghub.core.model.InputNode
import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.task.data.remote.api.WebAppTaskApi
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

/**
 * WebAppTaskRepositoryImpl 的凭据注入测试。
 *
 * Task Data 迁移后，API Key 必须继续由 Data 层从 CredentialStore 读取，UI 和 ScreenModel 不应透传敏感凭据。
 */
class WebAppTaskRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * 缺少 API Key 时应在网络请求前失败。
     */
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

    /**
     * 提交任务时应把本地 API Key 注入请求体。
     */
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
        assertEquals(TaskExecutionStatus.Submitted, result.status)
        assertTrue(capturedBody.contains(""""apiKey":"local-api-key""""))
        assertTrue(capturedBody.contains(""""webappId":10"""))
    }

    /**
     * 服务端业务失败时不应把原始 msg 放入异常消息。
     *
     * 该异常可能被上层统一错误处理读取；Data 层只暴露稳定错误码，
     * 避免后端诊断文本被误当作最终 UI 文案。
     */
    @Test
    fun `task business failure does not expose remote msg as exception message`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 503,
                      "msg": "raw task server failure",
                      "data": null
                    }
                """.trimIndent()
            },
        )

        val result = repository.runTask(
            webappId = 10L,
            nodeInfoList = listOf(inputNode()),
        )

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(result.isFailure)
        assertEquals("TASK_RUNTASK_FAILED_CODE_503", message)
        assertTrue("raw task server failure" !in message)
    }

    /**
     * 历史接口应把分页 records 映射为领域历史条目。
     */
    @Test
    fun `task history maps records to domain items`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "records": [
                          {
                            "taskId": "task-1",
                            "taskStatus": "SUCCESS",
                            "taskName": "Demo task",
                            "outputList": [{"id": "out-1", "fileUrl": "https://example.com/out.png"}]
                          }
                        ],
                        "total": 1
                      }
                    }
                """.trimIndent()
            },
        )

        val result = repository.getTaskHistory(pageNum = 1, pageSize = 10).getOrThrow()

        assertEquals("task-1", result.single().taskId)
        assertEquals(TaskExecutionStatus.Success, result.single().status)
        assertEquals("https://example.com/out.png", result.single().outputs.single().fileUrl)
    }

    private fun repositoryWithMock(
        credentialStore: CredentialStore,
        response: (String) -> String,
    ): WebAppTaskRepositoryImpl {
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
        return WebAppTaskRepositoryImpl(
            api = WebAppTaskApi(client),
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
