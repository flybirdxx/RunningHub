package com.runninghub.feature.community.data.remote.api

import com.runninghub.feature.community.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.feature.community.data.remote.dto.PlazaShortListRequestDto
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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Plaza API 路径测试。
 *
 * 迁移到 `feature:community:data` 后仍需固定真实 endpoint，避免社区页面请求误走旧 shared 路径。
 */
class PlazaApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * Plaza API 应调用预期的服务端路径。
     */
    @Test
    fun `plaza api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = PlazaApi(clientWithPathCapture(captured))

        api.getCreationTags()
        api.listCreations(
            PlazaCreationListRequestDto(
                current = 2,
                size = 30,
                sort = "HOT",
                tags = listOf("1875941016195785390"),
            )
        )
        api.listShortCategories()
        api.listShorts(PlazaShortListRequestDto(page = 3, size = 30, categoryCode = "MV"))

        assertEquals(
            listOf(
                HttpMethod.Post to "/api/portal/tag/tree",
                HttpMethod.Post to "/api/portal/creation/list",
                HttpMethod.Post to "/canvas/community/category/list",
                HttpMethod.Post to "/canvas/community/composition/list",
            ),
            captured.map { it.method to it.path },
        )
        val tagBody = json.parseToJsonElement(captured[0].body).jsonObject
        val creationBody = json.parseToJsonElement(captured[1].body).jsonObject
        val shortBody = json.parseToJsonElement(captured[3].body).jsonObject

        assertEquals("CREATION", tagBody.getValue("rang").jsonPrimitive.content)
        assertEquals("2", creationBody.getValue("current").jsonPrimitive.content)
        assertEquals("30", creationBody.getValue("size").jsonPrimitive.content)
        assertEquals("HOT", creationBody.getValue("sort").jsonPrimitive.content)
        assertEquals(
            listOf("1875941016195785390"),
            creationBody.getValue("tags").jsonArray.map { it.jsonPrimitive.content },
        )
        assertEquals("3", shortBody.getValue("page").jsonPrimitive.content)
        assertEquals("30", shortBody.getValue("size").jsonPrimitive.content)
        assertEquals("MV", shortBody.getValue("categoryCode").jsonPrimitive.content)
    }

    private fun clientWithPathCapture(
        captured: MutableList<CapturedRequest>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += CapturedRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    body = request.body.toRequestBodyText(),
                )
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
            "/api/portal/tag/tree" -> """{"code":0,"msg":"success","data":[]}"""
            "/api/portal/creation/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            "/canvas/community/category/list" -> """{"code":0,"msg":"success","data":[]}"""
            "/canvas/community/composition/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            else -> error("Unexpected path: $path")
        }

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
        }

    private data class CapturedRequest(
        val method: HttpMethod,
        val path: String,
        val body: String,
    )
}
