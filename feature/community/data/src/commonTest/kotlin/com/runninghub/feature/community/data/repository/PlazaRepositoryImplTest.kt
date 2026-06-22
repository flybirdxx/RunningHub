package com.runninghub.feature.community.data.repository

import com.runninghub.feature.community.data.remote.api.PlazaApi
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
    }
