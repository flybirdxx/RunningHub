package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.model.domain.ApiModelDetail
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

class QuickCreateRepositoryImplModelCatalogTest {
    @Test
    fun `non image model catalog uses server groups for ambiguous capability filters`() = runBlocking {
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = unusedQuickCreateApi(),
            credentialStore = FakeSettingsRepository(),
            authRepository = FakeAuthRepository(),
            modelCatalogRepository = FakeModelCatalogRepository(
                cachedModels = listOf(
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

        val models = repository.getModels(QuickCreationServiceKind.VIDEO).getOrThrow()

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
    fun `model catalog restores grouped standard snapshot without quickcreate model api`() = runBlocking {
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

        val models = repository.getModels(QuickCreationServiceKind.VIDEO).getOrThrow()

        assertEquals(listOf("seedance-text-video", "seedance-motion"), models.map { it.skuId })
        assertEquals(listOf("VIDEO", "VIDEO"), models.map { it.categoryId })
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
    private val cachedGroups: List<ApiModelGroup> = emptyList(),
    private val cachedModelsByGroup: Map<String, List<ApiModelSummary>> = emptyMap(),
) : ModelCatalogRepository {
    override suspend fun listStandardModels(
        search: String,
        page: Int,
        size: Int,
    ): Result<List<ApiModelSummary>> =
        Result.success(emptyList())

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

    override suspend fun getStandardModelDetail(modelId: String): Result<ApiModelDetail> =
        Result.failure(NotImplementedError())

    override suspend fun listLlmModels(): Result<List<LlmModelSummary>> =
        Result.success(emptyList())
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
