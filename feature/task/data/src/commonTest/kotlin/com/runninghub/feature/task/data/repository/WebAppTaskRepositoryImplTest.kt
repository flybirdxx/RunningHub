package com.runninghub.feature.task.data.repository

import com.runninghub.core.common.MissingCredential
import com.runninghub.core.common.MissingCredentialException
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.task.data.remote.api.WebAppTaskApi
import com.runninghub.feature.task.domain.WebAppTaskException
import com.runninghub.feature.task.domain.WebAppTaskIssue
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
import kotlin.test.assertIs
import kotlin.test.assertNull
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
        val error = assertIs<MissingCredentialException>(result.exceptionOrNull())
        assertEquals(MissingCredential.ApiKey, error.credential)
        assertEquals("MISSING_API_KEY", error.message)
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

        assertTrue(result.isFailure)
        val error = assertIs<WebAppTaskException>(result.exceptionOrNull())
        assertEquals(WebAppTaskIssue.RunTaskFailed, error.issue)
        assertEquals(503, error.remoteCode)
        assertTrue("raw task server failure" !in error.message.orEmpty())
    }

    /**
     * 成功业务码缺少 data 时应返回结构化响应异常，而不是英文异常 message。
     */
    @Test
    fun `runTask missing data maps to stable task issue`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": null
                    }
                """.trimIndent()
            },
        )

        val result = repository.runTask(
            webappId = 10L,
            nodeInfoList = listOf(inputNode()),
        )

        assertTrue(result.isFailure)
        val error = assertIs<WebAppTaskException>(result.exceptionOrNull())
        assertEquals(WebAppTaskIssue.RunTaskMissing, error.issue)
        assertEquals(null, error.remoteCode)
    }

    /**
     * 不同任务接口失败时应保留各自的稳定语义，避免上层解析 operation 字符串。
     */
    @Test
    fun `task operations map failed responses to stable task issues`() = runBlocking {
        val failures = listOf(
            WebAppTaskIssue.ApiCallDemoFailed to suspend {
                repositoryWithMock().getApiCallDemo("webapp-1")
            },
            WebAppTaskIssue.TaskOutputsFailed to suspend {
                repositoryWithMock().getTaskOutputs(10L)
            },
            WebAppTaskIssue.UploadFileFailed to suspend {
                repositoryWithMock().uploadFile("image/png", "image".encodeToByteArray(), "image.png")
            },
            WebAppTaskIssue.TaskHistoryFailed to suspend {
                repositoryWithMock().getTaskHistory(pageNum = 1, pageSize = 10)
            },
            WebAppTaskIssue.TaskDetailFailed to suspend {
                repositoryWithMock().getTaskDetail("task-1")
            },
        )

        failures.forEach { (issue, call) ->
            val result = call()
            assertTrue(result.isFailure)
            val error = assertIs<WebAppTaskException>(result.exceptionOrNull())
            assertEquals(issue, error.issue)
            assertEquals(409, error.remoteCode)
        }
    }

    /**
     * 控制台任务宽表应把所有任务 records 映射为领域历史条目。
     */
    @Test
    fun `task history maps billing usage records to domain items`() = runBlocking {
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
                            "taskId": "2071208241819508737",
                            "taskStatus": "SUCCESS",
                            "taskName": "AI app task",
                            "webappId": "2046794551444119554",
                            "taskCategoryCode": "WEBAPP_API",
                            "taskCategoryDisplay": "AI应用API",
                            "taskRelation": "PARENT",
                            "coinAmount": 12.0,
                            "coinUsedDuration": "60",
                            "createTime": "2026-06-28 20:25:01"
                          }
                        ],
                        "hasNext": false
                      }
                    }
                """.trimIndent()
            },
        )

        val result = repository.getTaskHistory(pageNum = 1, pageSize = 10).getOrThrow()

        assertEquals("2071208241819508737", result.single().taskId)
        assertEquals(TaskExecutionStatus.Success, result.single().status)
        assertEquals("AI app task", result.single().taskName)
        assertEquals("2046794551444119554", result.single().webappId)
        assertEquals("WEBAPP_API", result.single().taskCategoryCode)
        assertEquals("PARENT", result.single().taskRelation)
        assertEquals(12.0, result.single().coinAmount)
        assertEquals("60", result.single().taskCostTime)
    }

    /**
     * 宽表负责所有任务和运行中状态，旧 output history 负责已完成任务的输出缩略图。
     * AI 应用父任务没有直接输出时，应使用子任务输出补齐父任务卡片。
     */
    @Test
    fun `task history merges legacy output images into billing usage parent task`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = { body ->
                if (body.contains(""""apiKey"""")) {
                    """
                        {
                          "code": 0,
                          "msg": "success",
                          "data": {
                            "records": [
                              {
                                "taskId": "child-task-1",
                                "taskStatus": "SUCCESS",
                                "outputList": [
                                  {
                                    "id": "output-1",
                                    "outputName": "result.png",
                                    "outputType": "png",
                                    "fileUrl": "https://example.com/result.png",
                                    "filePreviewUrl": "https://example.com/result-preview.png",
                                    "expireDays": "7"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                    """.trimIndent()
                } else {
                    """
                        {
                          "code": 0,
                          "msg": "success",
                          "data": {
                            "records": [
                              {
                                "taskId": "parent-task-1",
                                "taskStatus": "SUCCESS",
                                "taskName": "AI app task",
                                "webappId": "2046794551444119554",
                                "taskCategoryCode": "WEBAPP_API",
                                "taskCategoryDisplay": "AI应用API",
                                "taskRelation": "PARENT",
                                "coinAmount": 40.0,
                                "coinUsedDuration": "199"
                              },
                              {
                                "taskId": "child-task-1",
                                "taskStatus": "SUCCESS",
                                "taskName": "Model child task",
                                "taskCategoryCode": "SKU_EXTERNAL_API",
                                "taskCategoryDisplay": "模型API",
                                "taskRelation": "CHILD",
                                "parentTaskId": "parent-task-1",
                                "moneyAmount": 0.1,
                                "currency": "CNY",
                                "moneyDuration": "188"
                              }
                            ],
                            "hasNext": false
                          }
                        }
                    """.trimIndent()
                }
            },
        )

        val result = repository.getTaskHistory(pageNum = 1, pageSize = 10).getOrThrow()
        val parentTask = result.first { it.taskId == "parent-task-1" }
        val childTask = result.first { it.taskId == "child-task-1" }

        assertEquals("https://example.com/result-preview.png", parentTask.outputs.single().filePreviewUrl)
        assertEquals("https://example.com/result-preview.png", childTask.outputs.single().filePreviewUrl)
    }

    /**
     * 控制台任务详情应补齐任务输出，并在暴露请求 JSON 前脱敏 API Key。
     */
    @Test
    fun `task detail maps console outputs and redacts sensitive request info`() = runBlocking {
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
                        "basicInfo": {
                          "apiName": "AI app task",
                          "apiType": "API",
                          "apiKeyType": "1",
                          "taskStatus": "SUCCESS",
                          "taskId": "2071208241819508737",
                          "callTime": "2026-06-28 20:25:01",
                          "duration": "999",
                          "amount": 9.9,
                          "coinNum": "99"
                        },
                        "list": [
                          {
                            "id": "output-1",
                            "outputName": "result.png",
                            "outputType": "png",
                            "fileUrl": "https://example.com/result.png",
                            "filePreviewUrl": "https://example.com/result-preview.png"
                          }
                        ],
                        "costInfo": {
                          "amount": 8.8,
                          "coinNum": "88"
                        },
                        "requestInfo": {
                          "apiRequestParams": "{\"apiKey\":\"secret-api-key\",\"webappId\":\"2046794551444119554\",\"aspectRatio\":\"16:9\",\"quality\":\"high\",\"estimatedCount\":\"20\",\"nodeInfoList\":[{\"fieldName\":\"prompt\",\"fieldValue\":\"city\"}]}"
                        },
                        "responseInfo": {
                          "taskId": "2071208241819508737",
                          "status": "SUCCESS",
                          "usage": {
                            "consumeCoins": "12",
                            "taskCostTime": "60",
                            "thirdPartyConsumeMoney": "0.1"
                          }
                        }
                      }
                    }
                """.trimIndent()
            },
        )

        val detail = repository.getTaskDetail("2071208241819508737").getOrThrow()

        assertEquals("2071208241819508737", detail.taskId)
        assertEquals("AI app task", detail.title)
        assertEquals("SUCCESS", detail.status)
        assertEquals("60", detail.duration)
        assertEquals("12", detail.rhCoins)
        assertEquals("0.1", detail.finalAmount)
        assertEquals("https://example.com/result-preview.png", detail.outputs.single().thumbnailUrl)
        assertTrue(capturedBody.contains(""""taskId":"2071208241819508737""""))
        assertTrue(!capturedBody.contains("apiKey"))
        assertTrue(!detail.requestInfo.orEmpty().contains("secret-api-key"))
        assertTrue(detail.requestInfo.orEmpty().contains(""""apiKey": "******""""))
        assertEquals("city", detail.requestParameters["prompt"])
        assertEquals(null, detail.requestParameters["apiKey"])
        assertEquals(null, detail.requestParameters["aspectRatio"])
        assertEquals(null, detail.requestParameters["quality"])
        assertEquals(null, detail.requestParameters["estimatedCount"])
    }

    @Test
    fun `task detail keeps top level request parameters when node info list is absent`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "basicInfo": {
                          "apiName": "Model API task",
                          "apiType": "MODEL_API",
                          "taskStatus": "SUCCESS",
                          "taskId": "model-task-1"
                        },
                        "requestInfo": {
                          "apiRequestParams": "{\"apiKey\":\"secret-api-key\",\"model\":\"seedream\",\"prompt\":\"city\",\"aspectRatio\":\"16:9\"}"
                        },
                        "responseInfo": {
                          "usage": {
                            "consumeCoins": "12"
                          }
                        }
                      }
                    }
                """.trimIndent()
            },
        )

        val detail = repository.getTaskDetail("model-task-1").getOrThrow()

        assertEquals("seedream", detail.requestParameters["model"])
        assertEquals("city", detail.requestParameters["prompt"])
        assertEquals("16:9", detail.requestParameters["aspectRatio"])
        assertEquals(null, detail.requestParameters["apiKey"])
    }

    @Test
    fun `task detail does not infer usage from legacy cost fields when usage is missing`() = runBlocking {
        val repository = repositoryWithMock(
            credentialStore = FakeCredentialStore(apiKey = "local-api-key"),
            response = {
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "basicInfo": {
                          "apiName": "AI app task",
                          "apiType": "API",
                          "taskStatus": "SUCCESS",
                          "taskId": "2071208241819508737",
                          "duration": "999",
                          "amount": 9.9,
                          "coinNum": "99"
                        },
                        "costInfo": {
                          "amount": 8.8,
                          "coinNum": "88"
                        },
                        "responseInfo": {
                          "taskId": "2071208241819508737",
                          "status": "SUCCESS"
                        }
                      }
                    }
                """.trimIndent()
            },
        )

        val detail = repository.getTaskDetail("2071208241819508737").getOrThrow()

        assertNull(detail.duration)
        assertNull(detail.rhCoins)
        assertNull(detail.finalAmount)
    }

    private fun repositoryWithMock(
        credentialStore: CredentialStore = FakeCredentialStore(apiKey = "local-api-key"),
        response: (String) -> String = {
            """
                {
                  "code": 409,
                  "msg": "raw task server failure",
                  "data": null
                }
            """.trimIndent()
        },
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
