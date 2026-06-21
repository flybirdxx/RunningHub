package com.runninghub.feature.community.data.repository

import com.runninghub.feature.community.data.remote.api.PlazaApi
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
     * 标签树业务失败时不得把远端 msg 写入异常消息。
     */
    @Test
    fun `getTags failure does not expose remote msg as exception message`() = runBlocking {
        val remoteMessage = "REMOTE_PLAZA_TAG_REASON"
        val repository = PlazaRepositoryImpl(
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

        val result = repository.getTags()

        val error = result.exceptionOrNull()
        assertTrue(result.isFailure)
        val message = assertNotNull(error).message
        assertEquals("PLAZA_TAGS_LOAD_FAILED_CODE_456", message)
        assertTrue(message != remoteMessage)
    }
}
