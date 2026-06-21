package com.runninghub.feature.discovery.data.remote.api

import com.runninghub.feature.discovery.data.remote.dto.CustomMadeWebappRequestDto
import com.runninghub.feature.discovery.data.remote.dto.WebAppListRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WebApp 目录 API 路径测试。
 *
 * 迁移到 `feature:discovery:data` 后仍需固定真实 endpoint，避免首页、搜索和详情页误走旧 shared 绑定。
 */
class WebAppCatalogApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * WebApp 目录 API 应调用预期的服务端路径。
     */
    @Test
    fun `webapp catalog api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<Pair<HttpMethod, String>>()
        val api = WebAppCatalogApi(clientWithPathCapture(captured))

        api.getWebAppList(WebAppListRequestDto(pageSize = 10, pageNum = 1))
        api.getCarefullyChosenList()
        api.getCustomMadeWebappList(CustomMadeWebappRequestDto())
        api.getWebAppUserList(mapOf("userId" to "user-1"))
        api.getTagTree()
        api.getWebAppDetail(mapOf("webappId" to "100"))

        assertEquals(
            listOf(
                HttpMethod.Post to "/api/webapp/list",
                HttpMethod.Post to "/api/webapp/carefullyChosenList",
                HttpMethod.Post to "/api/webapp/customMadeWebappList",
                HttpMethod.Post to "/api/webapp/user/list",
                HttpMethod.Post to "/api/portal/tag/tree",
                HttpMethod.Post to "/api/webapp/detail",
            ),
            captured,
        )
    }

    private fun clientWithPathCapture(
        captured: MutableList<Pair<HttpMethod, String>>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += request.method to request.url.encodedPath
                respond(
                    content = responseFor(request.url.encodedPath),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    private fun responseFor(path: String): String =
        when (path) {
            "/api/webapp/list",
            "/api/webapp/user/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            "/api/webapp/carefullyChosenList",
            "/api/webapp/customMadeWebappList",
            "/api/portal/tag/tree" -> """{"code":0,"msg":"success","data":[]}"""
            "/api/webapp/detail" -> """{"code":0,"msg":"success","data":{"id":"100","name":"Demo"}}"""
            else -> error("Unexpected path: $path")
        }
}
