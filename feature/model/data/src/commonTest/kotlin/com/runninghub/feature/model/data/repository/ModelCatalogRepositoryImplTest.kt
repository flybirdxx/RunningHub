package com.runninghub.feature.model.data.repository

import com.runninghub.core.storage.ModelCatalogCacheStore
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

    @Test
    fun `listStandardModels ignores legacy list cache without schema version`() = runBlocking {
        val cacheStore = FakeModelCatalogCacheStore(
            standardLists = mutableMapOf(
                "s0_p1_n30" to """{"models":[{"id":"legacy","name":"旧缓存模型"}]}""",
            ),
        )
        val repository = repositoryWithResponses(
            "/api/sku/list" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "records": [
                      {
                        "id": "fresh",
                        "name": "接口模型",
                        "categoryName": "text-to-image",
                        "sourceTypeName": "rh-ai"
                      }
                    ],
                    "total": 1
                  }
                }
            """.trimIndent(),
            cacheStore = cacheStore,
        )

        val models = repository.listStandardModels().getOrThrow()

        assertEquals(listOf("fresh"), models.map { it.id })
        assertEquals("text-to-image", models.single().type)
    }

    @Test
    fun `listStandardModelsByGroup uses server group as model group name`() = runBlocking {
        val repository = repositoryWithResponses(
            "/api/sku/tag/query" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": [
                    { "id": 438, "name": "Seedance", "nameEn": "Seedance Models", "apiCount": 10 },
                    { "id": 999, "name": "Empty", "apiCount": 0 }
                  ]
                }
            """.trimIndent(),
            "/api/sku/list" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "page": {
                      "records": [
                        {
                          "id": "seedance-motion",
                          "name": "即梦/动作模仿2.0",
                          "categoryName": "motion-control",
                          "sourceTypeName": "bytedance"
                        }
                      ]
                    },
                    "total": 1
                  }
                }
            """.trimIndent(),
        )

        val group = repository.listStandardModelGroups().getOrThrow().single()
        val models = repository.listStandardModelsByGroup(group = group, size = 999).getOrThrow()

        assertEquals("438", group.id)
        assertEquals("Seedance", group.name)
        assertEquals(listOf("seedance-motion"), models.map { it.id })
        assertEquals("Seedance", models.single().groupName)
        assertEquals("motion-control", models.single().type)
    }

    @Test
    fun `grouped standard model cache can be restored without network`() = runBlocking {
        val cacheStore = FakeModelCatalogCacheStore()
        val repository = repositoryWithResponses(
            "/api/sku/tag/query" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": [
                    { "id": 438, "name": "Seedance", "nameEn": "Seedance Models", "apiCount": 10 }
                  ]
                }
            """.trimIndent(),
            "/api/sku/list" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "page": {
                      "records": [
                        {
                          "id": "seedance-video",
                          "name": "SEEDANCE-V1.5-PRO-TEXT-TO-VIDEO",
                          "categoryName": "text-to-video",
                          "sourceTypeName": "bytedance"
                        }
                      ]
                    },
                    "total": 1
                  }
                }
            """.trimIndent(),
            cacheStore = cacheStore,
        )

        val group = repository.listStandardModelGroups().getOrThrow().single()
        repository.listStandardModelsByGroup(group = group, size = 999).getOrThrow()

        val restoredRepository = repositoryWithResponses(cacheStore = cacheStore)
        val restoredGroup = restoredRepository.getCachedStandardModelGroups().single()
        val restoredModels = restoredRepository.getCachedStandardModelsByGroup(
            group = restoredGroup,
            size = 999,
        )

        assertEquals("438", restoredGroup.id)
        assertEquals("Seedance", restoredGroup.name)
        assertEquals(listOf("seedance-video"), restoredModels.map { it.id })
        assertEquals("Seedance", restoredModels.single().groupName)
    }

    private fun repositoryWithResponses(
        vararg responses: Pair<String, String>,
        cacheStore: ModelCatalogCacheStore? = null,
    ): ModelCatalogRepositoryImpl {
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
            cacheStore = cacheStore,
        )
    }
}

private class FakeModelCatalogCacheStore(
    private val standardGroups: MutableMap<String, String> = mutableMapOf(),
    private val standardLists: MutableMap<String, String> = mutableMapOf(),
    private val standardDetails: MutableMap<String, String> = mutableMapOf(),
) : ModelCatalogCacheStore {
    override suspend fun getStandardModelGroups(cacheKey: String): String? =
        standardGroups[cacheKey]

    override suspend fun saveStandardModelGroups(cacheKey: String, json: String) {
        standardGroups[cacheKey] = json
    }

    override suspend fun getStandardModelList(cacheKey: String): String? =
        standardLists[cacheKey]

    override suspend fun saveStandardModelList(cacheKey: String, json: String) {
        standardLists[cacheKey] = json
    }

    override suspend fun getStandardModelDetail(modelId: String): String? =
        standardDetails[modelId]

    override suspend fun saveStandardModelDetail(modelId: String, json: String) {
        standardDetails[modelId] = json
    }
}
