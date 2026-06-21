package com.runninghub.feature.discovery.data.repository

import com.runninghub.feature.discovery.data.remote.api.WebAppCatalogApi
import com.runninghub.feature.discovery.domain.CatalogError
import com.runninghub.feature.discovery.domain.CatalogQuery
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WebAppCatalogRepositoryImpl 的错误映射测试。
 *
 * Discovery Data 迁移后，目录仓库仍必须把服务端业务码和空响应转换为稳定的 Domain 错误类型，
 * Presentation 不应重新解析服务端 msg。
 */
class WebAppCatalogRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * 非零业务码应映射为 CatalogError.Remote。
     */
    @Test
    fun `non zero response maps to remote catalog error`() = runBlocking {
        val repository = repositoryWithResponse("""{"code":500,"msg":"server busy","data":null}""")

        val result = repository.getAppList(testQuery())

        val error = assertIs<CatalogError.Remote>(result.exceptionOrNull())
        assertEquals(500, error.code)
        assertEquals("server busy", error.serverMessage)
    }

    /**
     * 成功业务码但缺少 data 时应映射为空响应错误。
     */
    @Test
    fun `empty successful response maps to empty response error`() = runBlocking {
        val repository = repositoryWithResponse("""{"code":0,"msg":"success","data":null}""")

        val result = repository.getAppList(testQuery())

        val error = assertIs<CatalogError.EmptyResponse>(result.exceptionOrNull())
        assertEquals("getAppList", error.operation)
    }

    /**
     * 正常分页响应应映射为 Domain PageData。
     */
    @Test
    fun `successful page response maps to domain page`() = runBlocking {
        val repository = repositoryWithResponse(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [{"id":"app-1","name":"Demo","statisticsInfo":{"useCount":"7"}}],
                "total": 1,
                "size": 10,
                "current": 1,
                "hasNext": false
              }
            }
            """.trimIndent()
        )

        val result = repository.getAppList(testQuery())

        assertTrue(result.isSuccess)
        assertEquals("app-1", result.getOrThrow().records.single().id)
        assertEquals("7", result.getOrThrow().records.single().useCount)
    }

    private fun repositoryWithResponse(response: String): WebAppCatalogRepositoryImpl {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = response,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return WebAppCatalogRepositoryImpl(WebAppCatalogApi(client))
    }

    private fun testQuery(): CatalogQuery = CatalogQuery(pageNum = 1, pageSize = 10)
}
