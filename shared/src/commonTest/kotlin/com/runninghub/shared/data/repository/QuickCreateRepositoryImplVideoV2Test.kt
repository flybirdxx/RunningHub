package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.VideoGenerationRequest
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
            settingsRepository = FakeSettingsRepository(),
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
