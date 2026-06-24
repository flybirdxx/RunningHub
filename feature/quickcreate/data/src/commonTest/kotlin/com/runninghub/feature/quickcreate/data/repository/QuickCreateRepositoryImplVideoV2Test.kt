package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.quickcreate.data.remote.api.QuickCreateApi
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskIssueCode
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QuickCreateRepositoryImplVideoV2Test {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `video request with quick creation ids uses v2 prepare commit list flow`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":9.6,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"video-token","ttlSeconds":120,"skuId":"video-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"video-task-1","skuId":"video-sku","taskStatus":"QUEUED","cashAmount":9.6}}
                """
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
                            "taskId": "video-task-1",
                            "taskStatus": "SUCCESS",
                            "outputList": [
                              {
                                "id": "out-1",
                                "outputType": "mp4",
                                "fileUrl": "https://example.com/result.mp4",
                                "filePreviewUrl": "https://example.com/preview.jpg"
                              }
                            ]
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateVideo(
            VideoGenerationRequest(
                prompt = "video prompt",
                model = "seedance2",
                aspectRatio = "3:4",
                duration = 8,
                resolution = "720p",
                quickCreationCategoryId = "VIDEO",
                quickCreationBindingId = "video-binding",
                quickCreationSkuId = "video-sku",
            )
        ).toList()

        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_TASK_LIST,
            ),
            paths,
        )
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val success = assertIs<QuickCreateTaskStatus.Success>(statuses.last())
        assertEquals("https://example.com/result.mp4", success.results.single().url)
    }

    @Test
    fun `image request with quick creation ids uses v2 even when model is service identity`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-1","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}
                """
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
                            "taskId": "image-task-1",
                            "taskStatus": "SUCCESS",
                            "outputList": [
                              {
                                "id": "out-1",
                                "outputType": "png",
                                "fileUrl": "https://example.com/result.png",
                                "filePreviewUrl": "https://example.com/preview.png"
                              }
                            ]
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_TASK_LIST,
            ),
            paths,
        )
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val success = assertIs<QuickCreateTaskStatus.Success>(statuses.last())
        assertEquals("https://example.com/result.png", success.results.single().url)
    }

    @Test
    fun `quick creation polling maps v2 running progress`() = runTest {
        var taskListCalls = 0
        val engine = MockEngine { request ->
            val response = when (request.url.encodedPath) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-progress","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}
                """
                QuickCreateApi.QC_TASK_LIST -> {
                    taskListCalls += 1
                    if (taskListCalls == 1) {
                        """
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "page": 1,
                                "size": 10,
                                "total": 1,
                                "list": [
                                  {
                                    "taskId": "image-task-progress",
                                    "taskStatus": "RUNNING",
                                    "progress": "42%"
                                  }
                                ]
                              }
                            }
                        """
                    } else {
                        """
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "page": 1,
                                "size": 10,
                                "total": 1,
                                "list": [
                                  {
                                    "taskId": "image-task-progress",
                                    "taskStatus": "SUCCESS",
                                    "outputList": [
                                      {
                                        "id": "out-1",
                                        "outputType": "png",
                                        "fileUrl": "https://example.com/result.png"
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                        """
                    }
                }
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        val running = assertIs<QuickCreateTaskStatus.Running>(statuses[2])
        assertEquals(42, running.progress)
        assertIs<QuickCreateTaskStatus.Success>(statuses.last())
    }

    @Test
    fun `quick creation polling filters blank output urls from success results`() = runBlocking {
        val engine = MockEngine { request ->
            val response = when (request.url.encodedPath) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.06,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-empty-url","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.06}}
                """
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
                            "taskId": "image-task-empty-url",
                            "taskStatus": "SUCCESS",
                            "outputList": [
                              {
                                "id": "out-blank",
                                "outputType": "png",
                                "fileUrl": ""
                              },
                              {
                                "id": "out-valid",
                                "outputType": "png",
                                "fileUrl": "https://example.com/result.png"
                              }
                            ]
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "1k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        val success = assertIs<QuickCreateTaskStatus.Success>(statuses.last())
        assertEquals(listOf("https://example.com/result.png"), success.results.map { it.url })
    }

    @Test
    fun `quick creation polling searches following pages before timing out missing first page record`() = runTest {
        val paths = mutableListOf<String>()
        val requestedPages = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.06,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-page-2","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.06}}
                """
                QuickCreateApi.QC_TASK_LIST -> {
                    val page = request.body.toRequestBodyText()
                        .let { body -> Regex("\\\"page\\\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1) }
                        ?: "1"
                    requestedPages += page
                    if (page == "1") {
                        """
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "page": 1,
                                "size": 50,
                                "total": 51,
                                "pages": 2,
                                "list": [
                                  {
                                    "taskId": "another-task",
                                    "taskStatus": "SUCCESS",
                                    "outputList": []
                                  }
                                ]
                              }
                            }
                        """
                    } else {
                        """
                            {
                              "code": 0,
                              "msg": "success",
                              "data": {
                                "page": 2,
                                "size": 50,
                                "total": 51,
                                "pages": 2,
                                "list": [
                                  {
                                    "taskId": "image-task-page-2",
                                    "taskStatus": "SUCCESS",
                                    "outputList": [
                                      {
                                        "id": "out-1",
                                        "outputType": "png",
                                        "fileUrl": "https://example.com/page-2-result.png"
                                      }
                                    ]
                                  }
                                ]
                              }
                            }
                        """
                    }
                }
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "1k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        assertEquals(listOf("1", "2"), requestedPages)
        assertEquals(2, paths.count { it == QuickCreateApi.QC_TASK_LIST })
        val success = assertIs<QuickCreateTaskStatus.Success>(statuses.last())
        assertEquals(listOf("https://example.com/page-2-result.png"), success.results.map { it.url })
    }

    @Test
    fun `image generation re-prepares when commit reports expired prepare token`() = runBlocking {
        val paths = mutableListOf<String>()
        var commitCalls = 0
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token-${paths.count { it == QuickCreateApi.QC_PREPARE }}","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> {
                    commitCalls += 1
                    if (commitCalls == 1) {
                        """{"code":409,"msg":"PREPARE_TOKEN_EXPIRED","data":null}"""
                    } else {
                        """{"code":0,"msg":"success","data":{"taskId":"image-task-1","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}"""
                    }
                }
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
                            "taskId": "image-task-1",
                            "taskStatus": "SUCCESS",
                            "outputList": [
                              {
                                "id": "out-1",
                                "outputType": "png",
                                "fileUrl": "https://example.com/result.png",
                                "filePreviewUrl": "https://example.com/preview.png"
                              }
                            ]
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_TASK_LIST,
            ),
            paths,
        )
        val success = assertIs<QuickCreateTaskStatus.Success>(statuses.last())
        assertEquals("https://example.com/result.png", success.results.single().url)
    }

    @Test
    fun `image generation stops before prepare when fee preview fails balance check`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            respond(
                content = """
                    {"code":0,"msg":"success","data":{"passed":false,"insufficientType":"CASH","requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        assertEquals(listOf(QuickCreateApi.QC_FEE_PREVIEW), paths)
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        val error = assertIs<QuickCreateTaskStatus.Error>(statuses.last())
        assertEquals(QuickCreateTaskIssueCode.FEE_PREVIEW_BLOCKED, error.message)
    }

    @Test
    fun `quick creation polling stops when task is cancelled`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-1","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}
                """
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
                            "taskId": "image-task-1",
                            "taskStatus": "CANCELED",
                            "outputList": []
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_TASK_LIST,
            ),
            paths,
        )
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val cancelled = assertIs<QuickCreateTaskStatus.Cancelled>(statuses.last())
        assertEquals("image-task-1", cancelled.taskId)
    }

    @Test
    fun `quick creation polling stops when task fails`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-1","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}
                """
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
                            "taskId": "image-task-1",
                            "taskStatus": "FAILED",
                            "outputList": []
                          }
                        ]
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        // 失败属于服务端终态；只允许查询一次任务列表，避免失败后继续轮询直到客户端超时。
        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
                QuickCreateApi.QC_TASK_LIST,
            ),
            paths,
        )
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val failed = assertIs<QuickCreateTaskStatus.Failed>(statuses.last())
        assertEquals("image-task-1", failed.taskId)
        assertEquals(QuickCreateTaskIssueCode.TASK_FAILED, failed.errorMessage)
    }

    @Test
    fun `quick creation polling emits timeout after max attempts without terminal record`() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.QC_FEE_PREVIEW -> """
                    {"code":0,"msg":"success","data":{"passed":true,"requiredCashAmount":0.76,"cashCurrency":"CNY"}}
                """
                QuickCreateApi.QC_PREPARE -> """
                    {"code":0,"msg":"success","data":{"prepareToken":"image-token","ttlSeconds":120,"skuId":"image-sku"}}
                """
                QuickCreateApi.QC_COMMIT -> """
                    {"code":0,"msg":"success","data":{"taskId":"image-task-1","skuId":"image-sku","taskStatus":"QUEUED","cashAmount":0.76}}
                """
                QuickCreateApi.QC_TASK_LIST -> """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "page": 1,
                        "size": 10,
                        "total": 0,
                        "list": []
                      }
                    }
                """
                else -> """{"code":404,"msg":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "image-binding:image-sku",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
            )
        ).toList()

        // 未返回任务记录时最多查询固定次数；超时后必须发出稳定错误码并结束 Flow。
        assertEquals(120, paths.count { it == QuickCreateApi.QC_TASK_LIST })
        assertEquals(
            listOf(
                QuickCreateApi.QC_FEE_PREVIEW,
                QuickCreateApi.QC_PREPARE,
                QuickCreateApi.QC_COMMIT,
            ),
            paths.take(3),
        )
        val timeout = assertIs<QuickCreateTaskStatus.Error>(statuses.last())
        assertEquals(QuickCreateTaskIssueCode.TASK_TIMEOUT, timeout.message)
    }

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
        }

    @Test
    fun `legacy openapi polling stops when query task is cancelled`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.IMAGE_X_TEXT -> """
                    {"taskId":"legacy-task-1","status":"QUEUING"}
                """
                QuickCreateApi.TASK_QUERY -> """
                    {"taskId":"legacy-task-1","status":"CANCELLED","progress":0,"results":[]}
                """
                else -> """{"taskId":"unexpected","status":"FAILED","errorMessage":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-x-official",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
            )
        ).toList()

        assertEquals(listOf(QuickCreateApi.IMAGE_X_TEXT, QuickCreateApi.TASK_QUERY), paths)
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val cancelled = assertIs<QuickCreateTaskStatus.Cancelled>(statuses.last())
        assertEquals("legacy-task-1", cancelled.taskId)
    }

    @Test
    fun `legacy openapi polling stops when query task fails`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            paths += path
            val response = when (path) {
                QuickCreateApi.IMAGE_X_TEXT -> """
                    {"taskId":"legacy-task-1","status":"QUEUING"}
                """
                QuickCreateApi.TASK_QUERY -> """
                    {"taskId":"legacy-task-1","status":"FAILED","progress":0,"errorMessage":"render failed","results":[]}
                """
                else -> """{"taskId":"unexpected","status":"FAILED","errorMessage":"unexpected path"}"""
            }.trimIndent()
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
        )

        val statuses = repository.generateImage(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-x-official",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "medium",
            )
        ).toList()

        // 旧 OpenAPI 轮询同样必须在失败终态结束，并把服务端 errorMessage 收口为稳定错误码，
        // 避免远端诊断文本被 Presentation 当作最终展示文案。
        assertEquals(listOf(QuickCreateApi.IMAGE_X_TEXT, QuickCreateApi.TASK_QUERY), paths)
        assertIs<QuickCreateTaskStatus.Submitting>(statuses[0])
        assertIs<QuickCreateTaskStatus.Queuing>(statuses[1])
        val failed = assertIs<QuickCreateTaskStatus.Failed>(statuses.last())
        assertEquals("legacy-task-1", failed.taskId)
        assertEquals(QuickCreateTaskIssueCode.TASK_FAILED, failed.errorMessage)
    }

    private class FakeSettingsRepository : CredentialStore {
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
        override suspend fun clearAll() {}
    }
}
