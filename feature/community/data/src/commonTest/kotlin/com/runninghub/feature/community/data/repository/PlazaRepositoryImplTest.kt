package com.runninghub.feature.community.data.repository

import com.runninghub.feature.community.data.remote.api.PlazaApi
import com.runninghub.feature.community.domain.PlazaShortPage
import com.runninghub.feature.community.domain.PlazaRepositoryException
import com.runninghub.feature.community.domain.PlazaRepositoryIssue
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Plaza Repository 的错误收口测试。
 *
 * Data 层只输出稳定错误码，具体用户可见文案由 Presentation 根据页面上下文映射，
 * 避免服务端原始 msg 直接进入 UI。
 */
class PlazaRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Plaza 接口业务失败时输出结构化 issue，不把远端 msg 写入异常消息。
     */
    @Test
    fun `repository failures expose typed issues instead of remote messages`() = runBlocking {
        val remoteMessage = "REMOTE_PLAZA_TAG_REASON"
        val cases = listOf(
            PlazaRepositoryIssue.TagsLoadFailed to repositoryWithResponse(remoteMessage).getTags(),
            PlazaRepositoryIssue.CreationsLoadFailed to repositoryWithResponse(remoteMessage).listCreations(),
            PlazaRepositoryIssue.ShortCategoriesLoadFailed to repositoryWithResponse(remoteMessage).listShortCategories(),
            PlazaRepositoryIssue.ShortListLoadFailed to repositoryWithResponse(remoteMessage).listShorts(),
        )

        cases.forEach { (expectedIssue, result) ->
            assertTrue(result.isFailure)
            val error = assertIs<PlazaRepositoryException>(result.exceptionOrNull())
            assertEquals(expectedIssue, error.issue)
            assertEquals(456, error.remoteCode)
            val message = assertNotNull(error.message)
            assertEquals("${expectedIssue.code}:456", message)
            assertTrue(message != remoteMessage)
        }
    }

    /**
     * Plaza Repository 应按抓包结构映射灵感和短片成功响应。
     */
    @Test
    fun `repository maps captured creation and short responses`() = runBlocking {
        val repository = PlazaRepositoryImpl(
            api = PlazaApi(
                HttpClient(
                    MockEngine { request ->
                        respond(
                            content = responseForSuccessPath(request.url.encodedPath),
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                },
            ),
        )

        val creations = repository.listCreations(page = 1, sort = "HOT", tags = listOf("1875941016195785390"))
            .getOrThrow()
        val shorts = repository.listShorts(page = 1, categoryCode = "NARRATIVE_SHORT").getOrThrow()

        assertEquals(listOf("2067532558576996354"), creations.items.map { it.id })
        assertEquals("https://image.png", creations.items.single().mediaUrl)
        assertEquals(
            PlazaShortPage(
                page = 1,
                total = 274,
                items = listOf(
                    com.runninghub.feature.community.domain.PlazaShortCard(
                        id = "2063090624344010753",
                        name = "不扫兴的父母：不讲大道理，却让人红了眼",
                        videoUrl = "https://video.mp4",
                        thumbnailUrl = "https://thumb.jpg",
                        durationSeconds = 42,
                        categoryName = "叙事短片",
                        authorName = "selene",
                        authorAvatar = "https://avatar.png",
                    ),
                ),
            ),
            shorts,
        )
    }

    private fun repositoryWithResponse(remoteMessage: String): PlazaRepositoryImpl =
        PlazaRepositoryImpl(
            api = PlazaApi(
                HttpClient(
                    MockEngine {
                        respond(
                            content = """{"code":456,"msg":"$remoteMessage","data":null}""",
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                },
            ),
        )

    private fun responseForSuccessPath(path: String): String =
        when (path) {
            "/api/portal/creation/list" -> """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "records": [
                      {
                        "id": "2067532558576996354",
                        "intro": "美女",
                        "owner": { "id": "1970504488576520194", "name": "kitten HZ", "avatar": "https://avatar.png" },
                        "statisticsInfo": { "likeCount": "2", "useCount": "76", "collectCount": "1" },
                        "creationShowreelInfo": {
                          "outputId": "2067532451395362818",
                          "fileUrl": "https://image.png",
                          "fileType": "png",
                          "imageWidth": 1664,
                          "imageHeight": 2496
                        }
                      }
                    ],
                    "current": 1,
                    "total": 64615
                  }
                }
            """.trimIndent()
            "/canvas/community/composition/list" -> """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "records": [
                      {
                        "id": "2063090624344010753",
                        "name": "不扫兴的父母：不讲大道理，却让人红了眼",
                        "compositionUrl": "https://video.mp4",
                        "compositionDuration": 42,
                        "thumbnail": "https://thumb.jpg",
                        "categoryCode": "NARRATIVE_SHORT",
                        "categoryName": "叙事短片",
                        "authorName": "selene",
                        "authorAvatar": "https://avatar.png"
                      }
                    ],
                    "total": 274
                  }
                }
            """.trimIndent()
            else -> """{"code":0,"msg":"success","data":[]}"""
        }
}
