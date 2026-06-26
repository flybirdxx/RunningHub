package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.core.storage.ModelCatalogCacheStore
import com.runninghub.feature.model.domain.ApiModelDetail
import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldOption
import com.runninghub.feature.model.domain.ApiModelFieldType
import com.runninghub.feature.model.domain.ApiModelGroup
import com.runninghub.feature.model.domain.ApiModelSummary
import com.runninghub.feature.model.domain.LlmModelSummary
import com.runninghub.feature.model.domain.ModelCatalogRepository
import com.runninghub.feature.quickcreate.data.remote.api.QuickCreateApi
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
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
import kotlin.test.assertTrue

class QuickCreateRepositoryImplModelCatalogTest {
    @Test
    fun `non image model catalog uses server groups for ambiguous capability filters`() = runBlocking {
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = emptyQuickCreateApi(),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = FakeModelCatalogRepository(
                cachedModels = emptyList(),
                refreshedModels = listOf(
                    standardModel(id = "seedance-text-video", type = "text-to-video", groupName = "Seedance"),
                    standardModel(id = "seedance-motion", type = "motion-control", groupName = "Seedance", name = "即梦/动作模仿2.0"),
                    standardModel(id = "seedance-explicit-image", type = "text-to-image", groupName = "Seedance"),
                    standardModel(id = "suno-music", type = "text-to-music", groupName = "Suno"),
                    standardModel(id = "suno-lyrics", type = "text-to-lyrics", groupName = "Suno", name = "suno-歌词生成"),
                    standardModel(id = "mureka-music", type = "text-to-music", groupName = "Mureka AI Models"),
                    standardModel(id = "mureka-upload", type = "upload-file", groupName = "Mureka AI Models"),
                    standardModel(id = "hitem3d", type = "image-to-3d", groupName = "Hitem3D"),
                    standardModel(id = "unknown-motion", type = "motion-control", groupName = "Unknown"),
                    standardModel(id = "text-image", type = "text-to-image", groupName = "全能图片"),
                ),
            ),
        )

        val models = repository.refreshModels(QuickCreationServiceKind.VIDEO).getOrThrow()

        assertEquals(
            listOf(
                "seedance-text-video",
                "seedance-motion",
                "suno-music",
                "suno-lyrics",
                "mureka-music",
                "mureka-upload",
                "hitem3d",
                "unknown-motion",
            ),
            models.map { it.skuId },
        )
        assertEquals(
            listOf(
                "VIDEO",
                "VIDEO",
                "AUDIO",
                "AUDIO",
                "AUDIO",
                "AUDIO",
                "OTHER",
                "OTHER",
            ),
            models.map { it.categoryId },
        )
    }

    @Test
    fun `get models does not treat grouped standard snapshot as quickcreate cache`() = runBlocking {
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = unusedQuickCreateApi(),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = FakeModelCatalogRepository(
                cachedModels = emptyList(),
                cachedGroups = listOf(ApiModelGroup(id = "438", name = "Seedance", apiCount = 10)),
                cachedModelsByGroup = mapOf(
                    "438" to listOf(
                        standardModel(id = "seedance-text-video", type = "text-to-video", groupName = "Seedance"),
                        standardModel(id = "seedance-motion", type = "motion-control", groupName = "Seedance"),
                    ),
                ),
            ),
        )

        val result = repository.getModels(QuickCreationServiceKind.VIDEO)

        assertTrue(result.isFailure)
    }

    @Test
    fun `get models restores quickcreate merged snapshot with submit identity`() = runBlocking {
        val cacheStore = FakeQuickCreateModelCatalogCacheStore()
        val firstRepository = QuickCreateRepositoryImpl(
            quickCreateApi = quickCreateCatalogApi(
                categoryId = "IMAGE",
                groupName = "\u5168\u80fd\u56fe\u7247G",
                bindingId = "qc-g2-binding",
                skuId = "g2-sku",
                name = "\u5168\u80fd\u56fe\u7247G-2.0-\u6587\u751f\u56fe-\u5b98\u65b9\u7a33\u5b9a\u7248",
            ),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = FakeModelCatalogRepository(
                cachedModels = emptyList(),
                refreshedModels = listOf(
                    standardModel(
                        id = "g2-sku",
                        type = "text-to-image",
                        groupName = "\u5168\u80fd\u56fe\u7247G",
                    ),
                ),
            ),
            modelCatalogCacheStore = cacheStore,
        )
        val refreshed = firstRepository.refreshModels(QuickCreationServiceKind.IMAGE).getOrThrow()
        val secondRepository = QuickCreateRepositoryImpl(
            quickCreateApi = unusedQuickCreateApi(),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = FakeModelCatalogRepository(
                cachedModels = listOf(
                    standardModel(
                        id = "g2-sku",
                        type = "text-to-image",
                        groupName = "\u5168\u80fd\u56fe\u7247PRO",
                    ),
                ),
            ),
            modelCatalogCacheStore = cacheStore,
        )

        val cached = secondRepository.getModels(QuickCreationServiceKind.IMAGE).getOrThrow()

        assertEquals(listOf("qc-g2-binding|g2-sku"), refreshed.map { "${it.bindingId}|${it.skuId}" })
        assertEquals(refreshed.map { "${it.bindingId}|${it.skuId}" }, cached.map { "${it.bindingId}|${it.skuId}" })
        assertEquals(refreshed.map { it.groupName }, cached.map { it.groupName })
    }

    @Test
    fun `refreshing standard catalog enriches standalone models with detail fields`() = runBlocking {
        val modelCatalogRepository = FakeModelCatalogRepository(
            cachedModels = emptyList(),
            refreshedModels = listOf(
                standardModel(
                    id = "open-source-lora",
                    type = "text-to-image",
                    groupName = "\u81ea\u90e8\u7f72\u5f00\u6e90\u6a21\u578b",
                ),
                standardModel(
                    id = "audio-file",
                    type = "video-to-audio",
                    groupName = "\u81ea\u90e8\u7f72\u5f00\u6e90\u6a21\u578b",
                ),
                standardModel(
                    id = "regular-text-image",
                    type = "text-to-image",
                    groupName = "\u5168\u80fd\u56fe\u7247",
                ),
            ),
            details = mapOf(
                "open-source-lora" to ApiModelDetail(
                    id = "open-source-lora",
                    name = "f-2-dev/text-to-image-lora",
                    type = "text-to-image",
                    groupName = "\u81ea\u90e8\u7f72\u5f00\u6e90\u6a21\u578b",
                    fields = listOf(
                        ApiModelField(
                            fieldKey = "16##lora_name",
                            paramKey = "lora",
                            type = ApiModelFieldType.MODEL,
                            required = false,
                            title = "LoRA",
                            defaultValue = "example.safetensors",
                        ),
                        ApiModelField(
                            fieldKey = "43##file_type",
                            paramKey = "outputFormat",
                            type = ApiModelFieldType.LIST,
                            required = true,
                            defaultValue = "png",
                            options = listOf(ApiModelFieldOption(label = "png", value = "png")),
                        ),
                    ),
                ),
                "audio-file" to ApiModelDetail(
                    id = "audio-file",
                    name = "Separate audio",
                    type = "video-to-audio",
                    groupName = "\u81ea\u90e8\u7f72\u5f00\u6e90\u6a21\u578b",
                    fields = listOf(
                        ApiModelField(
                            fieldKey = "3##file",
                            paramKey = "file",
                            type = ApiModelFieldType.FILE,
                            required = true,
                            title = "Source file",
                            acceptFormats = listOf("mp4", "wav"),
                        ),
                    ),
                ),
                "regular-text-image" to ApiModelDetail(
                    id = "regular-text-image",
                    name = "Regular text image",
                    type = "text-to-image",
                    groupName = "\u5168\u80fd\u56fe\u7247",
                    fields = listOf(
                        ApiModelField(
                            fieldKey = "unused",
                            paramKey = "unused",
                            type = ApiModelFieldType.STRING,
                            required = false,
                        ),
                    ),
                ),
            ),
        )
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = emptyQuickCreateApi(),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = modelCatalogRepository,
        )

        val imageModels = repository.refreshModels(QuickCreationServiceKind.IMAGE).getOrThrow()
        val videoModels = repository.refreshModels(QuickCreationServiceKind.VIDEO).getOrThrow()

        val loraModel = imageModels.single { it.skuId == "open-source-lora" }
        assertEquals(listOf("lora", "outputFormat"), loraModel.fields.map { it.paramKey })
        assertEquals("LoRA", loraModel.fields.first().inputExtra?.title)
        assertEquals("example.safetensors", loraModel.fields.first().defaultValue)

        val fileField = videoModels.single().fields.single()
        assertEquals("file", fileField.paramKey)
        assertEquals("FILE", fileField.fieldType)
        assertEquals(listOf("mp4", "wav"), fileField.inputExtra?.acceptFormats)
        assertEquals(null, fileField.uploadMediaKind)
        assertEquals(
            listOf("open-source-lora", "audio-file"),
            modelCatalogRepository.detailRequests,
        )
    }
}

private fun unusedQuickCreateApi(): QuickCreateApi {
    val engine = MockEngine {
        respond(
            content = """{"code":404,"msg":"unexpected path"}""",
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    val json = Json { ignoreUnknownKeys = true }
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    return QuickCreateApi(client, json)
}

private fun emptyQuickCreateApi(): QuickCreateApi {
    val engine = MockEngine {
        respond(
            content = """{"code":0,"msg":"success","data":{"categoryMeta":[],"categories":{}}}""",
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    val json = Json { ignoreUnknownKeys = true }
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    return QuickCreateApi(client, json)
}

private fun quickCreateCatalogApi(
    categoryId: String,
    groupName: String,
    bindingId: String,
    skuId: String,
    name: String,
): QuickCreateApi {
    val engine = MockEngine {
        respond(
            content = """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "categoryMeta": [],
                    "categories": {
                      "$categoryId": [
                        {
                          "nameCn": "$groupName",
                          "children": [
                            {
                              "categoryId": "$categoryId",
                              "bindingId": "$bindingId",
                              "skuId": "$skuId",
                              "nameCn": "$name",
                              "fields": [
                                {
                                  "fieldKey": "style",
                                  "mappedApiParamKey": "style",
                                  "fieldType": "LIST",
                                  "required": false,
                                  "defaultValue": "photoreal"
                                }
                              ],
                              "pricing": {
                                "flatPrice": 0.1
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
            """.trimIndent(),
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }
    val json = Json { ignoreUnknownKeys = true }
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
    }
    return QuickCreateApi(client, json)
}

private fun standardModel(
    id: String,
    type: String?,
    name: String = id,
    groupName: String? = null,
): ApiModelSummary =
    ApiModelSummary(
        id = id,
        name = name,
        type = type,
        groupName = groupName,
    )

private class FakeModelCatalogRepository(
    private val cachedModels: List<ApiModelSummary>,
    private val refreshedModels: List<ApiModelSummary> = emptyList(),
    private val cachedGroups: List<ApiModelGroup> = emptyList(),
    private val cachedModelsByGroup: Map<String, List<ApiModelSummary>> = emptyMap(),
    private val details: Map<String, ApiModelDetail> = emptyMap(),
) : ModelCatalogRepository {
    val detailRequests = mutableListOf<String>()

    override suspend fun listStandardModels(
        search: String,
        page: Int,
        size: Int,
    ): Result<List<ApiModelSummary>> =
        Result.success(if (page == 1) refreshedModels else emptyList())

    override suspend fun getCachedStandardModels(
        search: String,
        page: Int,
        size: Int,
    ): List<ApiModelSummary> =
        if (page == 1) cachedModels else emptyList()

    override suspend fun getCachedStandardModelGroups(search: String): List<ApiModelGroup> =
        cachedGroups

    override suspend fun getCachedStandardModelsByGroup(
        group: ApiModelGroup,
        search: String,
        page: Int,
        size: Int,
    ): List<ApiModelSummary> =
        if (page == 1) cachedModelsByGroup[group.id].orEmpty() else emptyList()

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> {
        detailRequests += modelId
        return details[modelId]?.let { Result.success(it) } ?: Result.failure(NotImplementedError())
    }

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        Result.success(emptyList())
}

private class FakeQuickCreateModelCatalogCacheStore : ModelCatalogCacheStore {
    private val quickCreateCatalogs = mutableMapOf<String, String>()

    override suspend fun getStandardModelGroups(cacheKey: String): String? = null

    override suspend fun saveStandardModelGroups(cacheKey: String, json: String) {}

    override suspend fun getStandardModelList(cacheKey: String): String? = null

    override suspend fun saveStandardModelList(cacheKey: String, json: String) {}

    override suspend fun getStandardModelDetail(modelId: String): String? = null

    override suspend fun saveStandardModelDetail(modelId: String, json: String) {}

    override suspend fun getQuickCreateModelCatalog(kindKey: String): String? =
        quickCreateCatalogs[kindKey]

    override suspend fun saveQuickCreateModelCatalog(kindKey: String, json: String) {
        quickCreateCatalogs[kindKey] = json
    }
}

private class FakeSettingsRepository : CredentialStore {
    override suspend fun getApiKey(): String? = null
    override suspend fun setApiKey(key: String) {}
    override suspend fun clearApiKey() {}
    override suspend fun getEnterpriseApiKey(): String? = null
    override suspend fun setEnterpriseApiKey(key: String) {}
    override suspend fun clearEnterpriseApiKey() {}
    override suspend fun getCookie(): String? = null
    override suspend fun setCookie(cookie: String) {}
    override suspend fun clearCookie() {}
    override suspend fun getAuthToken(): String? = null
    override suspend fun setAuthToken(token: String) {}
    override suspend fun clearAuthToken() {}
    override suspend fun getRefreshToken(): String? = null
    override suspend fun setRefreshToken(token: String) {}
    override suspend fun clearRefreshToken() {}
    override suspend fun isLoggedIn(): Boolean = false
    override suspend fun clearAll() {}
}
