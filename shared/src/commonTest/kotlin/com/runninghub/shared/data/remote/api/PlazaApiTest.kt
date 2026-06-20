package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.shared.data.remote.dto.PlazaShortListRequestDto
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

class PlazaApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `plaza api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<Pair<HttpMethod, String>>()
        val api = PlazaApi(clientWithPathCapture(captured))

        api.getCreationTags()
        api.listCreations(PlazaCreationListRequestDto())
        api.listShortCategories()
        api.listShorts(PlazaShortListRequestDto())

        assertEquals(
            listOf(
                HttpMethod.Post to "/api/portal/tag/tree",
                HttpMethod.Post to "/api/portal/creation/list",
                HttpMethod.Post to "/canvas/community/category/list",
                HttpMethod.Post to "/canvas/community/composition/list",
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
            "/api/portal/tag/tree" -> """{"code":0,"msg":"success","data":[]}"""
            "/api/portal/creation/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            "/canvas/community/category/list" -> """{"code":0,"msg":"success","data":[]}"""
            "/canvas/community/composition/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            else -> error("Unexpected path: $path")
        }
}
