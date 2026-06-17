package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.domain.repository.SettingsRepository
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

class QuickCreateRepositoryImplHistoryTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `history list maps quick creation tasks and outputs`() = runBlocking {
        val paths = mutableListOf<String>()
        val repository = repositoryWithMock { path ->
            paths += path
            when (path) {
                QuickCreateApi.QC_TASK_LIST -> """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "page": 1,
                        "size": 10,
                        "total": 1,
                        "list": [
                          {
                            "taskId": "task-1",
                            "taskStatus": "SUCCESS",
                            "taskCostTime": "66",
                            "taskType": "FAST_WEBAPP_V2",
                            "skuId": "sku-1",
                            "bindingId": "binding-1",
                            "bindingCategoryId": "IMAGE",
                            "apiRequestParams": "{\"prompt\":\"green icon\",\"aspectRatio\":\"16:9\"}",
                            "prepayRecord": {
                              "settlementMode": "cash_only",
                              "rhAmount": 0,
                              "cashAmount": 0.76,
                              "cashCurrency": "CNY",
                              "isFree": 0,
                              "status": "PREPAID"
                            },
                            "outputList": [
                              {
                                "id": "output-1",
                                "outputName": "result.png",
                                "outputType": "png",
                                "fileUrl": "https://example.com/result.png",
                                "filePreviewUrl": "https://example.com/preview.png",
                                "outputSize": "2048x1152",
                                "expireTime": "2026-07-02 00:27:12",
                                "expireDays": "13"
                              }
                            ]
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }
        }

        val page = repository.listQuickCreationHistory(page = 1, size = 10).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_TASK_LIST), paths)
        assertEquals(1, page.page)
        assertEquals(10, page.size)
        assertEquals(1, page.total)
        val item = page.items.single()
        assertEquals("task-1", item.taskId)
        assertEquals("SUCCESS", item.status)
        assertEquals("IMAGE", item.categoryId)
        assertEquals("binding-1", item.bindingId)
        assertEquals("sku-1", item.skuId)
        assertEquals(0.76, item.cashAmount)
        assertEquals("CNY", item.cashCurrency)
        assertEquals("green icon", item.params["prompt"])
        val output = item.outputs.single()
        assertEquals("output-1", output.outputId)
        assertEquals("https://example.com/result.png", output.url)
        assertEquals("https://example.com/preview.png", output.thumbnailUrl)
        assertEquals(2048, output.width)
        assertEquals(1152, output.height)
        assertTrue(output.isImage)
    }

    @Test
    fun `history detail uses output id and maps output record`() = runBlocking {
        val paths = mutableListOf<String>()
        val repository = repositoryWithMock { path ->
            paths += path
            when (path) {
                QuickCreateApi.QC_TASK_DETAIL -> """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "taskId": "task-2",
                        "taskStatus": "SUCCESS",
                        "bindingCategoryId": "VIDEO",
                        "outputList": [
                          {
                            "id": "output-2",
                            "outputType": "mp4",
                            "fileUrl": "https://example.com/result.mp4",
                            "filePreviewUrl": "https://example.com/preview.jpg",
                            "outputSize": "720x1280",
                            "expireDays": "7"
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }
        }

        val detail = repository.getQuickCreationHistoryDetail(outputId = "output-2").getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_TASK_DETAIL), paths)
        assertEquals("task-2", detail.taskId)
        assertEquals("VIDEO", detail.categoryId)
        val output = detail.outputs.single()
        assertEquals("output-2", output.outputId)
        assertEquals("mp4", output.type)
        assertTrue(output.isVideo)
    }

    @Test
    fun `cancel history task posts encoded task id`() = runBlocking {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val repository = repositoryWithMock(
            responseForPath = { path ->
                paths += path
                when (path) {
                    QuickCreateApi.QC_TASK_CANCEL -> """{"code":0,"msg":"success","data":true}"""
                    else -> """{"code":404,"msg":"unexpected path"}"""
                }
            },
            captureBody = { bodies += it },
        )

        val result = repository.cancelQuickCreationTask("task id/1").getOrThrow()

        assertEquals(Unit, result)
        assertEquals(listOf(QuickCreateApi.QC_TASK_CANCEL), paths)
        assertEquals("""{"taskId":"task%20id%2F1"}""", bodies.single())
    }

    @Test
    fun `project list maps paged project records`() = runBlocking {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val repository = repositoryWithMock(
            responseForPath = { path ->
                paths += path
                when (path) {
                    QuickCreateApi.QC_PROJECT_LIST -> """
                        {
                          "code": 0,
                          "msg": "success",
                          "data": {
                            "records": [
                              {
                                "projectId": "project-1",
                                "name": "世界杯广告",
                                "coverUrl": "https://example.com/project.png",
                                "taskCount": 3,
                                "pin": true,
                                "createdAt": "2026-06-17 10:00:00",
                                "updatedAt": "2026-06-17 11:00:00"
                              }
                            ],
                            "size": "20",
                            "current": "1",
                            "total": "1",
                            "pages": "1",
                            "hasNext": false,
                            "hasPrevious": false,
                            "nextCursor": null
                          }
                        }
                    """
                    else -> """{"code":404,"msg":"unexpected path"}"""
                }
            },
            captureBody = { bodies += it },
        )

        val page = repository.listQuickCreationProjects(page = 1, size = 20).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_PROJECT_LIST), paths)
        assertEquals("""{"page":1,"size":20}""", bodies.single())
        assertEquals(1, page.page)
        assertEquals(20, page.size)
        assertEquals(1, page.total)
        assertEquals(1, page.pages)
        assertEquals(false, page.hasNext)
        val project = page.items.single()
        assertEquals("project-1", project.projectId)
        assertEquals("世界杯广告", project.name)
        assertEquals("https://example.com/project.png", project.coverUrl)
        assertEquals(3, project.taskCount)
        assertEquals(true, project.pinned)
        assertEquals("2026-06-17 10:00:00", project.createdAt)
        assertEquals("2026-06-17 11:00:00", project.updatedAt)
    }

    @Test
    fun `project task list posts project id and maps history page`() = runBlocking {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val repository = repositoryWithMock(
            responseForPath = { path ->
                paths += path
                when (path) {
                    QuickCreateApi.QC_PROJECT_TASKS -> """
                        {
                          "code": 0,
                          "msg": "success",
                          "data": {
                            "page": 1,
                            "size": 10,
                            "total": 1,
                            "list": [
                              {
                                "taskId": "task-1",
                                "taskStatus": "SUCCESS",
                                "bindingCategoryId": "IMAGE",
                                "apiRequestParams": "{\"prompt\":\"project prompt\"}",
                                "outputList": [
                                  {
                                    "id": "output-1",
                                    "outputType": "png",
                                    "fileUrl": "https://example.com/project-result.png",
                                    "filePreviewUrl": "https://example.com/project-preview.png",
                                    "outputSize": "1024x1024"
                                  }
                                ]
                              }
                            ]
                          }
                        }
                    """
                    else -> """{"code":404,"msg":"unexpected path"}"""
                }
            },
            captureBody = { bodies += it },
        )

        val page = repository.listQuickCreationProjectTasks(
            projectId = "project-1",
            page = 1,
            size = 10,
        ).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_PROJECT_TASKS), paths)
        assertEquals("""{"projectId":"project-1","page":1,"size":10}""", bodies.single())
        assertEquals(1, page.total)
        val item = page.items.single()
        assertEquals("task-1", item.taskId)
        assertEquals("project prompt", item.params["prompt"])
        assertEquals("https://example.com/project-result.png", item.outputs.single().url)
    }

    @Test
    fun `pin project posts project id and pin state`() = runBlocking {
        val paths = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val repository = repositoryWithMock(
            responseForPath = { path ->
                paths += path
                when (path) {
                    QuickCreateApi.QC_PROJECT_PIN -> """{"code":0,"msg":"success","data":true}"""
                    else -> """{"code":404,"msg":"unexpected path"}"""
                }
            },
            captureBody = { bodies += it },
        )

        val result = repository.pinQuickCreationProject(projectId = "project-1", pinned = true).getOrThrow()

        assertEquals(Unit, result)
        assertEquals(listOf(QuickCreateApi.QC_PROJECT_PIN), paths)
        assertEquals("""{"projectId":"project-1","pin":true}""", bodies.single())
    }

    private fun repositoryWithMock(responseForPath: (String) -> String): QuickCreateRepositoryImpl {
        return repositoryWithMock(responseForPath = responseForPath, captureBody = {})
    }

    private fun repositoryWithMock(
        responseForPath: (String) -> String,
        captureBody: (String) -> Unit,
    ): QuickCreateRepositoryImpl {
        val engine = MockEngine { request ->
            captureBody(request.body.toRequestBodyText())
            respond(
                content = responseForPath(request.url.encodedPath).trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            settingsRepository = FakeSettingsRepository(),
        )
    }

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
        }

    private class FakeSettingsRepository : SettingsRepository {
        override suspend fun getApiKey(): String? = null
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
        override suspend fun getLastKnownCoins(): String? = null
        override suspend fun setLastKnownCoins(coins: String) {}
        override suspend fun clearLastKnownCoins() {}
        override suspend fun saveQuickCreateDraft(json: String) {}
        override suspend fun getQuickCreateDraft(): String? = null
        override suspend fun clearQuickCreateDraft() {}
        override suspend fun clearAll() {}
    }
}
