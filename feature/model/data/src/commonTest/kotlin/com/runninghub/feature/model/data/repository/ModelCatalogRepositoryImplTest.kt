package com.runninghub.feature.model.data.repository

import com.runninghub.feature.model.data.remote.api.ModelCatalogApi
import com.runninghub.feature.model.domain.ModelCatalogException
import com.runninghub.feature.model.domain.ModelCatalogIssue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ModelCatalogRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `listStandardModels maps failed response to catalog issue`() = runBlocking {
        val repository = repositoryWithResponses(
            "/api/sku/list" to """{"code":500,"msg":"remote failure","data":null}""",
        )

        val result = repository.listStandardModels()

        assertTrue(result.isFailure)
        val error = assertIs<ModelCatalogException>(result.exceptionOrNull())
        assertEquals(ModelCatalogIssue.StandardListLoadFailed, error.issue)
        assertEquals(500, error.remoteCode)
    }

    @Test
    fun `getStandardModelDetail maps missing data to catalog issue`() = runBlocking {
        val repository = repositoryWithResponses(
            "/api/sku/detail" to """{"code":0,"msg":"success","data":null}""",
        )

        val result = repository.getStandardModelDetail("sku-1")

        assertTrue(result.isFailure)
        val error = assertIs<ModelCatalogException>(result.exceptionOrNull())
        assertEquals(ModelCatalogIssue.StandardDetailMissing, error.issue)
        assertEquals(null, error.remoteCode)
    }

    @Test
    fun `listLlmModels maps failed response to catalog issue`() = runBlocking {
        val repository = repositoryWithResponses(
            "/llm/api/models" to """{"code":403,"msg":"remote failure","data":null}""",
        )

        val result = repository.listLlmModels()

        assertTrue(result.isFailure)
        val error = assertIs<ModelCatalogException>(result.exceptionOrNull())
        assertEquals(ModelCatalogIssue.LlmListLoadFailed, error.issue)
        assertEquals(403, error.remoteCode)
    }

    private fun repositoryWithResponses(vararg responses: Pair<String, String>): ModelCatalogRepositoryImpl {
        val responseByPath = responses.toMap()
        val client = HttpClient(
            MockEngine { request ->
                respond(
                    content = responseByPath.getValue(request.url.encodedPath),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return ModelCatalogRepositoryImpl(
            api = ModelCatalogApi(client),
            json = json,
            endpointRegistry = ModelEndpointRegistry(),
        )
    }
}
