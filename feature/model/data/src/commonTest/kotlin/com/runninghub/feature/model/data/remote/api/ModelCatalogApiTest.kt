package com.runninghub.feature.model.data.remote.api

import com.runninghub.feature.model.data.remote.dto.SkuListRequestDto
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

class ModelCatalogApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `model catalog api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<Pair<HttpMethod, String>>()
        val api = ModelCatalogApi(clientWithPathCapture(captured))

        api.listStandardModels(SkuListRequestDto())
        api.getStandardModelDetail("sku-1")
        api.listLlmModels()

        assertEquals(
            listOf(
                HttpMethod.Post to "/api/sku/list",
                HttpMethod.Post to "/api/sku/detail",
                HttpMethod.Get to "/llm/api/models",
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
            "/api/sku/list" -> """{"code":0,"msg":"success","data":{"records":[],"total":0}}"""
            "/api/sku/detail" -> """{"code":0,"msg":"success","data":{"id":"sku-1"}}"""
            "/llm/api/models" -> """{"code":0,"msg":"success","data":[]}"""
            else -> error("Unexpected path: $path")
        }
}
