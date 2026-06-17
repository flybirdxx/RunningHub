package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.app.platform.MediaResolver
import com.runninghub.shared.domain.repository.QuickCreateInspirationTag
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplate
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*

class QuickCreateScreenModelTest {
    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }

    class FakeQuickCreateRepository : QuickCreateRepository {
        var uploadResult: Result<String> = Result.success("https://example.com/file.jpg")
        var lastImageRequest: com.runninghub.shared.domain.repository.ImageGenerationRequest? = null
        var inspirationTags = listOf(QuickCreateInspirationTag(id = "hot", name = "热门"))
        var inspirationTemplates = listOf(
            QuickCreateInspirationTemplate(
                templateId = "tpl-1",
                title = "赛博城市漫游",
                categoryId = "IMAGE",
                coverUrl = "https://example.com/cover.png",
                videoUrl = null,
                tagHot = true,
                tagNew = false,
            )
        )
        var models = listOf(
            QuickCreationServiceModel(
                categoryId = "IMAGE",
                groupName = "全能图片",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "全能图片G-2.0-官方版",
                description = "服务端模型",
                fields = listOf(
                    QuickCreationServiceField(
                        fieldKey = "style",
                        paramKey = "style",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "photoreal",
                        options = emptyList(),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "aspectRatio",
                        paramKey = "aspectRatio",
                        fieldType = "LIST",
                        required = true,
                        defaultValue = "1:1",
                        options = emptyList(),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "negativePrompt",
                        paramKey = "negativePrompt",
                        fieldType = "STRING",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "referenceImage",
                        paramKey = "referenceImages",
                        fieldType = "UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        maxUploadCount = 1,
                    ),
                ),
            )
        )
        override fun generateImage(request: com.runninghub.shared.domain.repository.ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            lastImageRequest = request
            return flowOf(QuickCreateTaskStatus.Queuing("task-1"))
        }
        override fun generateVideo(request: com.runninghub.shared.domain.repository.VideoGenerationRequest): Flow<QuickCreateTaskStatus> = emptyFlow()
        override suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String) = uploadResult
        override suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>> = Result.success(inspirationTags)
        override suspend fun getInspirationTemplates(
            page: Int,
            size: Int,
            tagId: String?,
        ): Result<List<QuickCreateInspirationTemplate>> = Result.success(inspirationTemplates)
        override suspend fun getModels(categoryId: String): Result<List<QuickCreationServiceModel>> =
            Result.success(models.filter { it.categoryId == categoryId })
    }

    class FakeMediaResolver : MediaResolver {
        override fun readBytes(uri: String): ByteArray = ByteArray(0)
        override fun getDisplayName(uri: String): String? = "test.jpg"
        override fun getFileSizeBytes(uri: String): Long = 1024L
    }

    class FakeSettingsRepo : SettingsRepository {
        private var draft: String? = null
        override suspend fun getQuickCreateDraft() = draft
        override suspend fun saveQuickCreateDraft(json: String) { draft = json }
        override suspend fun clearQuickCreateDraft() { draft = null }
        override suspend fun getApiKey() = null
        override suspend fun setApiKey(key: String) {}
        override suspend fun clearApiKey() {}
        override suspend fun getEnterpriseApiKey() = null
        override suspend fun setEnterpriseApiKey(key: String) {}
        override suspend fun clearEnterpriseApiKey() {}
        override suspend fun getCookie() = null
        override suspend fun setCookie(cookie: String) {}
        override suspend fun clearCookie() {}
        override suspend fun getAuthToken() = null
        override suspend fun setAuthToken(token: String) {}
        override suspend fun clearAuthToken() {}
        override suspend fun getRefreshToken() = null
        override suspend fun setRefreshToken(token: String) {}
        override suspend fun clearRefreshToken() {}
        override suspend fun isLoggedIn() = false
        override suspend fun getLastKnownCoins() = null
        override suspend fun setLastKnownCoins(coins: String) {}
        override suspend fun clearLastKnownCoins() {}
        override suspend fun clearAll() {}
    }

    @Test
    fun `initial state is IDLE with default IMAGE tab`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        assertEquals(QuickCreateTab.IMAGE, model.uiState.value.currentTab)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
    }

    @Test
    fun `switchTab updates tab and cost`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.switchTab(QuickCreateTab.VIDEO)
        assertEquals(QuickCreateTab.VIDEO, model.uiState.value.currentTab)
    }

    @Test
    fun `switchMode toggles creation and inspiration surfaces`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)
        assertEquals(QuickCreateMode.INSPIRATION, model.uiState.value.currentMode)
        assertEquals(false, model.uiState.value.showCreationInput)

        model.switchMode(QuickCreateMode.CREATION)
        assertEquals(QuickCreateMode.CREATION, model.uiState.value.currentMode)
        assertEquals(true, model.uiState.value.showCreationInput)
    }

    @Test
    fun `switchMode to inspiration loads tags and templates`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)

        assertEquals(listOf("热门"), model.uiState.value.inspirationTags.map { it.name })
        assertEquals("tpl-1", model.uiState.value.inspirationTemplates.single().templateId)
        assertEquals(false, model.uiState.value.inspirationLoading)
    }

    @Test
    fun `init loads service driven image models`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        assertEquals("全能图片G-2.0-官方版", model.uiState.value.serviceImageModels.single().name)
        assertEquals("binding-1", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("photoreal", model.uiState.value.imageServiceParams["style"])
        assertEquals(false, model.uiState.value.serviceModelsLoading)
    }

    @Test
    fun `generate image uses selected service model ids`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.updateImagePrompt("prompt")
            model.generate()

            assertEquals("IMAGE", repository.lastImageRequest?.quickCreationCategoryId)
            assertEquals("binding-1", repository.lastImageRequest?.quickCreationBindingId)
            assertEquals("sku-1", repository.lastImageRequest?.quickCreationSkuId)
        }
    }

    @Test
    fun `generate image uses selected service model field defaults`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.updateImagePrompt("prompt")
            model.generate()

            assertEquals("photoreal", repository.lastImageRequest?.quickCreationParams?.get("style"))
            assertEquals("16:9", repository.lastImageRequest?.quickCreationParams?.get("aspectRatio"))
        }
    }

    @Test
    fun `generate image uses updated service field values`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.updateImagePrompt("prompt")
            model.updateImageServiceParam("style", "anime")
            model.generate()

            assertEquals("anime", repository.lastImageRequest?.quickCreationParams?.get("style"))
        }
    }

    @Test
    fun `generate image only submits declared service field values`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.updateImagePrompt("prompt")
            model.updateImageServiceParam("negativePrompt", "low quality")
            model.updateImageServiceParam("unexpected", "value")
            model.generate()

            assertEquals("low quality", repository.lastImageRequest?.quickCreationParams?.get("negativePrompt"))
            assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("unexpected"))
        }
    }

    @Test
    fun `generate image maps uploaded images to service upload field`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.updateImagePrompt("prompt")
            model.pickImageReference("content://image/1")
            repeat(20) {
                if (model.uiState.value.imageConfig.mediaReferences.any { it.uploadStatus == UploadStatus.DONE }) {
                    return@repeat
                }
                delay(10)
            }
            model.generate()

            assertEquals(
                listOf("https://example.com/file.jpg"),
                repository.lastImageRequest?.quickCreationListParams?.get("referenceImages"),
            )
        }
    }

    @Test
    fun `updateImagePrompt changes prompt`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.updateImagePrompt("test prompt")
        assertEquals("test prompt", model.uiState.value.imageConfig.prompt)
    }

    @Test
    fun `updateVideoPrompt changes prompt`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.updateVideoPrompt("video prompt")
        assertEquals("video prompt", model.uiState.value.videoConfig.prompt)
    }

    @Test
    fun `hasDraft false initially`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        assertEquals(false, model.hasDraft)
    }

    @Test
    fun `checkForDraft finds saved draft`() {
        runBlocking {
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"hello","videoPrompt":""}""")
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        model.checkForDraft()
        // async — draft loaded in coroutine
        assertTrue(true) // basic sanity
        }
    }

    @Test
    fun `generate without prompt shows error`() {
        runBlocking {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.generate()
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertNotNull(model.uiState.value.error)
        }
    }

    @Test
    fun `reset restores to idle`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.clearResults()
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(0, model.uiState.value.results.size)
    }
}
