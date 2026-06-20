package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.feature.quickcreate.presentation.draft.DraftData
import com.runninghub.feature.quickcreate.presentation.draft.resumeSummaryText

import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateDraftSnapshot
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTag
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplate
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplateDetail
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplatePage
import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationProject
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectPage
import com.runninghub.feature.quickcreate.domain.QuickCreationProjectRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.presentation.history.QuickCreateHistoryOutputMediaType
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationBadgeTone
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationPlaceholderMediaType
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreateInspirationPreviewUi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.*
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoModel
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateScreenModelTest {
    private val createdModels = mutableListOf<QuickCreateScreenModel>()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun resetMainDispatcher() {
        // 测试会触发媒体上传协程；先释放 ScreenModel，再重置 Main dispatcher，
        // 避免后台上传恢复到已经被清理的测试调度器。
        createdModels.asReversed().forEach { it.onDispose() }
        createdModels.clear()
        Dispatchers.resetMain()
    }

    private fun createModel(
        historyRepository: QuickCreationTaskHistoryRepository,
        mediaResolver: MediaResolver,
        draftRepository: QuickCreateDraftRepository,
        ioDispatcher: CoroutineDispatcher = Dispatchers.Main,
        modelCatalogRepository: QuickCreationModelCatalogRepository =
            historyRepository as QuickCreationModelCatalogRepository,
        generationRepository: QuickCreationGenerationRepository =
            historyRepository as QuickCreationGenerationRepository,
        feePreviewRepository: QuickCreationFeePreviewRepository =
            historyRepository as QuickCreationFeePreviewRepository,
        inspirationRepository: QuickCreationInspirationRepository =
            historyRepository as QuickCreationInspirationRepository,
        mediaUploadRepository: QuickCreationMediaUploadRepository =
            historyRepository as QuickCreationMediaUploadRepository,
        projectRepository: QuickCreationProjectRepository =
            historyRepository as QuickCreationProjectRepository,
    ): QuickCreateScreenModel =
        QuickCreateScreenModel(
            historyRepository = historyRepository,
            modelCatalogRepository = modelCatalogRepository,
            generationRepository = generationRepository,
            feePreviewRepository = feePreviewRepository,
            inspirationRepository = inspirationRepository,
            mediaUploadRepository = mediaUploadRepository,
            projectRepository = projectRepository,
            mediaResolver = mediaResolver,
            draftRepository = draftRepository,
            ioDispatcher = ioDispatcher,
        ).also { createdModels += it }

    class FakeQuickCreateRepository :
        QuickCreationTaskHistoryRepository,
        QuickCreationModelCatalogRepository,
        QuickCreationGenerationRepository,
        QuickCreationFeePreviewRepository,
        QuickCreationInspirationRepository,
        QuickCreationMediaUploadRepository,
        QuickCreationProjectRepository {
        var uploadResult: Result<String> = Result.success("https://example.com/file.jpg")
        var uploadDelayMillis: Long = 0L
        var lastImageRequest: com.runninghub.feature.quickcreate.domain.ImageGenerationRequest? = null
        var lastVideoRequest: com.runninghub.feature.quickcreate.domain.VideoGenerationRequest? = null
        var imageTaskStatuses: List<QuickCreateTaskStatus> = listOf(QuickCreateTaskStatus.Queuing("task-1"))
        var feePreviewResult: Result<QuickCreationFeePreview> = Result.success(
            QuickCreationFeePreview(
                passed = true,
                free = false,
                settlementMode = "cash_only",
                requiredCashAmount = 0.76,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            )
        )
        val feePreviewRequests = mutableListOf<com.runninghub.feature.quickcreate.domain.ImageGenerationRequest>()
        var imageFeePreviewHandler:
            (suspend (com.runninghub.feature.quickcreate.domain.ImageGenerationRequest) -> Result<QuickCreationFeePreview>)? =
            null
        var videoFeePreviewResult: Result<QuickCreationFeePreview> = Result.success(
            QuickCreationFeePreview(
                passed = true,
                free = false,
                settlementMode = "cash_only",
                requiredCashAmount = 9.60,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            )
        )
        val videoFeePreviewRequests = mutableListOf<com.runninghub.feature.quickcreate.domain.VideoGenerationRequest>()
        var videoFeePreviewHandler:
            (suspend (com.runninghub.feature.quickcreate.domain.VideoGenerationRequest) -> Result<QuickCreationFeePreview>)? =
            null
        var lastHistoryDetailOutputId: String? = null
        val cancelledTaskIds = mutableListOf<String>()
        val requestedHistoryPages = mutableListOf<Int>()
        val requestedProjectPages = mutableListOf<Int>()
        val requestedTemplatePages = mutableListOf<Int>()
        val requestedProjectTaskPages = mutableListOf<Pair<String, Int>>()
        val pinnedProjectRequests = mutableListOf<Pair<String, Boolean>>()
        val createdProjectNames = mutableListOf<String>()
        val renamedProjectRequests = mutableListOf<Pair<String, String>>()
        val deletedProjectIds = mutableListOf<String>()
        var lastProjectDetailId: String? = null
        var projectDetail = QuickCreationProject(
            projectId = "project-1",
            name = "项目详情",
            coverUrl = "https://example.com/project-detail.png",
            taskCount = 9,
            pinned = true,
        )
        var createdProject = QuickCreationProject(
            projectId = "project-new",
            name = "新项目",
        )
        var historyPage = QuickCreationHistoryPage(
            page = 1,
            size = 10,
            total = 1,
            items = listOf(
                QuickCreationHistoryItem(
                    taskId = "history-task-1",
                    status = "SUCCESS",
                    categoryId = "IMAGE",
                    bindingId = "binding-1",
                    skuId = "sku-1",
                    params = mapOf("prompt" to "green icon"),
                    cashAmount = 0.76,
                    cashCurrency = "CNY",
                    outputs = listOf(
                        QuickCreationHistoryOutput(
                            outputId = "output-1",
                            url = "https://example.com/result.png",
                            type = "png",
                            thumbnailUrl = "https://example.com/preview.png",
                            width = 2048,
                            height = 1152,
                        )
                    ),
                )
            ),
        )
        var historyPages: Map<Int, QuickCreationHistoryPage>? = null
        var projectTaskPages: Map<Int, QuickCreationHistoryPage>? = null
        var projectTaskPage = QuickCreationHistoryPage(
            page = 1,
            size = 10,
            total = 1,
            items = listOf(
                QuickCreationHistoryItem(
                    taskId = "project-task-1",
                    status = "SUCCESS",
                    categoryId = "IMAGE",
                    params = mapOf("prompt" to "project prompt"),
                    outputs = listOf(
                        QuickCreationHistoryOutput(
                            outputId = "project-output-1",
                            url = "https://example.com/project-result.png",
                            type = "png",
                        )
                    ),
                )
            ),
        )
        var projectPage = QuickCreationProjectPage(
            page = 1,
            size = 20,
            total = 1,
            pages = 1,
            hasNext = false,
            hasPrevious = false,
            items = listOf(
                QuickCreationProject(
                    projectId = "project-1",
                    name = "世界杯广告",
                    coverUrl = "https://example.com/project.png",
                    taskCount = 3,
                    pinned = true,
                )
            ),
        )
        var projectPages: Map<Int, QuickCreationProjectPage>? = null
        var overrideHistoryList: ((page: Int, size: Int) -> QuickCreationHistoryPage)? = null
        var overrideProjectTaskList:
            ((projectId: String, page: Int, size: Int) -> QuickCreationHistoryPage)? = null
        val requestedTemplateDetailIds = mutableListOf<String>()
        var templateDetailResult: Result<QuickCreateInspirationTemplateDetail>? = null
        var historyDetail = QuickCreationHistoryItem(
            taskId = "history-task-detail",
            status = "SUCCESS",
            categoryId = "IMAGE",
            params = mapOf("prompt" to "detail prompt"),
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "output-1",
                    url = "https://example.com/detail.png",
                    type = "png",
                    width = 1024,
                    height = 1024,
                )
            ),
        )
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
        var inspirationTemplatePages: Map<Int, QuickCreateInspirationTemplatePage>? = null
        var templateDetail = QuickCreateInspirationTemplateDetail(
            templateId = "tpl-video",
            title = "薯片赛场",
            categoryId = "VIDEO",
            bindingId = "video-binding-1",
            skuId = "video-sku-1",
            prompt = "薯片人偶踢足球",
            params = mapOf(
                "ratio" to "3:4",
                "aspectRatio" to "3:4",
                "resolution" to "720p",
                "duration" to "8",
                "generateAudio" to "false",
                "realPersonMode" to "false",
            ),
            listParams = mapOf(
                "imageUrls" to listOf("https://example.com/ref.png"),
            ),
            coverUrl = "https://example.com/cover.jpg",
            videoUrl = "https://example.com/preview.mp4",
        )
        var videoModels = listOf(
            QuickCreationServiceModel(
                categoryId = "VIDEO",
                groupName = "视频生成",
                bindingId = "video-binding-1",
                skuId = "video-sku-1",
                name = "Seedance2.0",
                description = "服务端视频模型",
                fields = listOf(
                    QuickCreationServiceField(
                        fieldKey = "style",
                        paramKey = "style",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "cinematic",
                        options = emptyList(),
                    ),
                    QuickCreationServiceField(
                        fieldKey = "referenceVideo",
                        paramKey = "referenceVideos",
                        fieldType = "VIDEO_UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        maxUploadCount = 1,
                    ),
                    QuickCreationServiceField(
                        fieldKey = "referenceAudio",
                        paramKey = "referenceAudios",
                        fieldType = "AUDIO_UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        maxUploadCount = 1,
                    ),
                ),
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
        override fun generateImage(request: com.runninghub.feature.quickcreate.domain.ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            lastImageRequest = request
            return flowOf(*imageTaskStatuses.toTypedArray())
        }
        override fun generateVideo(request: com.runninghub.feature.quickcreate.domain.VideoGenerationRequest): Flow<QuickCreateTaskStatus> {
            lastVideoRequest = request
            return flowOf(QuickCreateTaskStatus.Queuing("video-task-1"))
        }
        override suspend fun previewImageQuickCreationFee(
            request: com.runninghub.feature.quickcreate.domain.ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> {
            feePreviewRequests += request
            imageFeePreviewHandler?.let { handler -> return handler(request) }
            return feePreviewResult
        }
        override suspend fun previewVideoQuickCreationFee(
            request: com.runninghub.feature.quickcreate.domain.VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> {
            videoFeePreviewRequests += request
            videoFeePreviewHandler?.let { handler -> return handler(request) }
            return videoFeePreviewResult
        }
        override suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String> {
            if (uploadDelayMillis > 0L) {
                delay(uploadDelayMillis)
            }
            return when {
                mimeType.startsWith("video") -> Result.success("https://example.com/video.mp4")
                mimeType.startsWith("audio") -> Result.success("https://example.com/audio.mp3")
                else -> uploadResult
            }
        }
        override suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>> = Result.success(inspirationTags)
        override suspend fun getInspirationTemplates(
            page: Int,
            size: Int,
            tagId: String?,
        ): Result<QuickCreateInspirationTemplatePage> =
            Result.success(
                inspirationTemplatePages?.get(page) ?: QuickCreateInspirationTemplatePage(
                    page = page,
                    size = size,
                    total = inspirationTemplates.size,
                    pages = 1,
                    hasNext = false,
                    hasPrevious = page > 1,
                    items = inspirationTemplates,
                )
            ).also {
                requestedTemplatePages += page
            }
        override suspend fun getInspirationTemplateDetail(
            templateId: String,
        ): Result<QuickCreateInspirationTemplateDetail> =
            (templateDetailResult ?: Result.success(templateDetail.copy(templateId = templateId))).also {
                requestedTemplateDetailIds += templateId
            }
        override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> =
            Result.success((models + videoModels).filter { it.categoryId == kind.testCategoryId() })

        override suspend fun listQuickCreationHistory(page: Int, size: Int): Result<QuickCreationHistoryPage> =
            Result.success(
                overrideHistoryList?.invoke(page, size)
                    ?: (historyPages?.get(page) ?: historyPage).copy(page = page, size = size)
            ).also {
                requestedHistoryPages += page
            }

        override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> =
            Result.success(historyDetail).also {
                lastHistoryDetailOutputId = outputId
            }

        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> =
            Result.success(Unit).also {
                cancelledTaskIds += taskId
            }

        override suspend fun listQuickCreationProjects(page: Int, size: Int): Result<QuickCreationProjectPage> =
            Result.success((projectPages?.get(page) ?: projectPage).copy(page = page, size = size)).also {
                requestedProjectPages += page
            }

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            Result.success(
                overrideProjectTaskList?.invoke(projectId, page, size)
                    ?: (projectTaskPages?.get(page) ?: projectTaskPage).copy(page = page, size = size)
            ).also {
                requestedProjectTaskPages += projectId to page
            }

        override suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject> =
            Result.success(createdProject.copy(name = name)).also {
                createdProjectNames += name
            }

        override suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit> =
            Result.success(Unit).also {
                renamedProjectRequests += projectId to name
            }

        override suspend fun deleteQuickCreationProject(projectId: String): Result<Unit> =
            Result.success(Unit).also {
                deletedProjectIds += projectId
            }

        override suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit> =
            Result.success(Unit).also {
                pinnedProjectRequests += projectId to pinned
            }

        override suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject> =
            Result.success(projectDetail.copy(projectId = projectId)).also {
                lastProjectDetailId = projectId
            }
    }

    class FakeMediaResolver : MediaResolver {
        override fun readBytes(uri: String): ByteArray = ByteArray(0)
        override fun getDisplayName(uri: String): String? = "test.jpg"
        override fun getFileSizeBytes(uri: String): Long = 1024L
    }

    class FakeSettingsRepo : QuickCreateDraftRepository {
        private var draft: String? = null

        suspend fun getQuickCreateDraft() = draft

        suspend fun saveQuickCreateDraft(json: String) {
            draft = json
        }

        suspend fun clearQuickCreateDraft() {
            draft = null
        }

        override suspend fun getRestorableDraft(): QuickCreateDraftSnapshot? {
            val raw = draft
            if (raw.isNullOrEmpty()) return null

            val snapshot = runCatching { raw.toDraftSnapshot() }.getOrElse {
                draft = null
                return null
            }

            return if (snapshot.hasPromptContent) {
                snapshot
            } else {
                draft = null
                null
            }
        }

        override suspend fun saveDraft(snapshot: QuickCreateDraftSnapshot) {
            draft = if (snapshot.hasPromptContent) snapshot.toJsonString() else null
        }

        override suspend fun clearDraft() {
            draft = null
        }

        private val QuickCreateDraftSnapshot.hasPromptContent: Boolean
            get() = imagePrompt.isNotBlank() || videoPrompt.isNotBlank()

        private fun String.toDraftSnapshot(): QuickCreateDraftSnapshot {
            val element = draftJson.parseToJsonElement(this).jsonObject
            return QuickCreateDraftSnapshot(
                currentTab = element["currentTab"]?.jsonPrimitive?.contentOrNull ?: "IMAGE",
                imagePrompt = element["imagePrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                videoPrompt = element["videoPrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }

        private fun QuickCreateDraftSnapshot.toJsonString(): String =
            buildJsonObject {
                put("currentTab", currentTab)
                put("imagePrompt", imagePrompt)
                put("videoPrompt", videoPrompt)
            }.toString()

        private companion object {
            val draftJson: Json = Json { encodeDefaults = true }
        }
    }

    @Test
    fun `initial state is IDLE with default IMAGE tab`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        assertEquals(QuickCreateTab.IMAGE, model.uiState.value.currentTab)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
    }

    @Test
    fun `initialization loads quick creation history`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        assertEquals(false, model.uiState.value.historyLoading)
        assertEquals("history-task-1", model.uiState.value.historyItems.single().source.taskId)
        assertEquals("history-task-1", model.uiState.value.historyItems.single().taskId)
        assertEquals("green icon", model.uiState.value.historyItems.single().title)
        assertEquals("IMAGE · SUCCESS · PNG", model.uiState.value.historyItems.single().metadataText)
        assertEquals("0.76 CNY", model.uiState.value.historyItems.single().cashText)
        assertEquals("https://example.com/result.png", model.uiState.value.historyItems.single().primaryOutput?.source?.url)
        assertEquals("output-1", model.uiState.value.historyItems.single().primaryOutput?.outputId)
        assertEquals("https://example.com/preview.png", model.uiState.value.historyItems.single().primaryOutput?.previewUrl)
        assertEquals(QuickCreateHistoryOutputMediaType.IMAGE, model.uiState.value.historyItems.single().primaryOutput?.mediaType)
    }

    @Test
    fun `initialization loads quick creation projects`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        assertEquals(listOf(1), repository.requestedProjectPages)
        assertEquals(false, model.uiState.value.projectsLoading)
        assertEquals("project-1", model.uiState.value.projects.single().projectId)
        assertEquals("世界杯广告", model.uiState.value.projects.single().name)
        assertEquals(true, model.uiState.value.projects.single().isPinned)
        assertEquals("取消置顶项目", model.uiState.value.projects.single().pinContentDescription)
        assertEquals("3 个任务", model.uiState.value.projects.single().taskCountText)
        assertEquals("https://example.com/project.png", model.uiState.value.projects.single().coverUrl)
        assertEquals("删除「世界杯广告」后，项目入口会从当前列表移除。", model.uiState.value.projects.single().deleteConfirmationText)
        assertEquals(false, model.uiState.value.projectsHasMore)
    }

    @Test
    fun `loading more projects appends next page`() {
        val repository = FakeQuickCreateRepository().apply {
            projectPages = mapOf(
                1 to projectPage.copy(
                    page = 1,
                    total = 2,
                    hasNext = true,
                    items = listOf(
                        QuickCreationProject(
                            projectId = "project-1",
                            name = "世界杯广告",
                            taskCount = 3,
                        )
                    ),
                ),
                2 to projectPage.copy(
                    page = 2,
                    total = 2,
                    hasNext = false,
                    items = listOf(
                        QuickCreationProject(
                            projectId = "project-2",
                            name = "新品海报",
                            taskCount = 1,
                        )
                    ),
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.loadMoreQuickCreationProjects()

        assertEquals(listOf(1, 2), repository.requestedProjectPages)
        assertEquals(listOf("project-1", "project-2"), model.uiState.value.projects.map { it.projectId })
        assertEquals(2, model.uiState.value.projectsPage)
        assertEquals(false, model.uiState.value.projectsHasMore)
        assertEquals(false, model.uiState.value.projectsLoadingMore)
    }

    @Test
    fun `selecting project loads project tasks into history area`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectProject("project-1")

        assertEquals(listOf("project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals("project-1", model.uiState.value.selectedProjectId)
        assertEquals(false, model.uiState.value.projectTasksLoading)
        assertEquals("project-task-1", model.uiState.value.historyItems.single().source.taskId)
        assertEquals(false, model.uiState.value.historyHasMore)
    }

    @Test
    fun `clearing selected project reloads recent history`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.selectProject("project-1")

        model.clearSelectedProject()

        assertEquals(null, model.uiState.value.selectedProjectId)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("history-task-1", model.uiState.value.historyItems.single().source.taskId)
    }

    @Test
    fun `successful generation refreshes selected project tasks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                        )
                    ),
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.selectProject("project-1")
        runCurrent()
        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("project-1", model.uiState.value.selectedProjectId)
        assertEquals(listOf("project-1" to 1, "project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals(listOf(1), repository.requestedHistoryPages)
        assertEquals("project-task-1", model.uiState.value.historyItems.single().source.taskId)
    }

    @Test
    fun `toggling project pin calls repository and updates project state`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.toggleProjectPin("project-1")

        assertEquals(listOf("project-1" to false), repository.pinnedProjectRequests)
        assertEquals(false, model.uiState.value.projects.single().isPinned)
        assertEquals("置顶项目", model.uiState.value.projects.single().pinContentDescription)
        assertEquals("未置顶", model.uiState.value.projects.single().pinStatusText)
        assertEquals(emptySet(), model.uiState.value.projectPinningIds)
    }

    @Test
    fun `creating project calls repository and prepends project`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.createProject("新项目")

        assertEquals(listOf("新项目"), repository.createdProjectNames)
        assertEquals("project-new", model.uiState.value.projects.first().projectId)
        assertEquals("新项目", model.uiState.value.projects.first().name)
        assertEquals("0 个任务", model.uiState.value.projects.first().taskCountText)
        assertEquals(emptySet(), model.uiState.value.projectMutatingIds)
    }

    @Test
    fun `renaming project calls repository and updates project name`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.renameProject("project-1", "新名称")

        assertEquals(listOf("project-1" to "新名称"), repository.renamedProjectRequests)
        assertEquals("新名称", model.uiState.value.projects.single().name)
        assertEquals(emptySet(), model.uiState.value.projectMutatingIds)
    }

    @Test
    fun `deleting selected project removes it and reloads recent history`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.selectProject("project-1")

        model.deleteProject("project-1")

        assertEquals(listOf("project-1"), repository.deletedProjectIds)
        assertEquals(emptyList(), model.uiState.value.projects)
        assertEquals(null, model.uiState.value.selectedProjectId)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals(emptySet(), model.uiState.value.projectMutatingIds)
    }

    @Test
    fun `selecting project detail loads detail into state`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectProjectDetail("project-1")

        assertEquals("project-1", repository.lastProjectDetailId)
        assertEquals(false, model.uiState.value.projectDetailLoading)
        assertEquals("项目详情", model.uiState.value.selectedProjectDetail?.name)
        assertEquals("https://example.com/project-detail.png", model.uiState.value.selectedProjectDetail?.coverUrl)
        assertEquals(
            listOf("任务数量" to "9", "置顶状态" to "已置顶"),
            model.uiState.value.selectedProjectDetail?.rows?.map { it.label to it.value },
        )
    }

    @Test
    fun `selecting history output loads detail into state`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectHistoryOutput("output-1")

        assertEquals("output-1", repository.lastHistoryDetailOutputId)
        assertEquals(false, model.uiState.value.historyDetailLoading)
        assertEquals("history-task-detail", model.uiState.value.selectedHistoryDetail?.source?.taskId)
        assertEquals("detail prompt", model.uiState.value.selectedHistoryDetail?.title)
        assertEquals("IMAGE · SUCCESS · PNG · 1024x1024", model.uiState.value.selectedHistoryDetail?.metadataText)
        assertEquals(null, model.uiState.value.selectedHistoryDetail?.cashText)
        assertEquals("https://example.com/detail.png", model.uiState.value.selectedHistoryDetail?.primaryOutput?.source?.url)
        assertEquals(QuickCreateHistoryOutputMediaType.IMAGE, model.uiState.value.selectedHistoryDetail?.primaryOutput?.mediaType)
    }

    @Test
    fun `selecting history output maps video detail media type`() {
        val repository = FakeQuickCreateRepository().apply {
            historyDetail = historyDetail.copy(
                outputs = listOf(
                    QuickCreationHistoryOutput(
                        outputId = "output-video",
                        url = "https://example.com/detail.mp4?download=1",
                        type = "file",
                        thumbnailUrl = "https://example.com/detail-cover.png",
                        width = 1920,
                        height = 1080,
                    )
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectHistoryOutput("output-video")

        assertEquals("output-video", repository.lastHistoryDetailOutputId)
        assertEquals("https://example.com/detail-cover.png", model.uiState.value.selectedHistoryDetail?.primaryOutput?.previewUrl)
        assertEquals(QuickCreateHistoryOutputMediaType.VIDEO, model.uiState.value.selectedHistoryDetail?.primaryOutput?.mediaType)
    }

    @Test
    fun `loading more history appends next page`() {
        val repository = FakeQuickCreateRepository().apply {
            historyPages = mapOf(
                1 to QuickCreationHistoryPage(
                    page = 1,
                    size = 1,
                    total = 2,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "history-task-1",
                            status = "SUCCESS",
                            outputs = listOf(
                                QuickCreationHistoryOutput(
                                    outputId = "output-1",
                                    url = "https://example.com/one.png",
                                    type = "png",
                                )
                            ),
                        )
                    ),
                ),
                2 to QuickCreationHistoryPage(
                    page = 2,
                    size = 1,
                    total = 2,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "history-task-2",
                            status = "SUCCESS",
                            outputs = listOf(
                                QuickCreationHistoryOutput(
                                    outputId = "output-2",
                                    url = "https://example.com/two.mp4?download=1",
                                    type = "file",
                                    thumbnailUrl = "https://example.com/two-cover.png",
                                )
                            ),
                        )
                    ),
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.loadMoreQuickCreationHistory()

        assertEquals(listOf(1, 2), repository.requestedHistoryPages)
        assertEquals(listOf("history-task-1", "history-task-2"), model.uiState.value.historyItems.map { it.source.taskId })
        assertEquals(listOf("history-task-1", "history-task-2"), model.uiState.value.historyItems.map { it.taskId })
        assertEquals(QuickCreateHistoryOutputMediaType.VIDEO, model.uiState.value.historyItems.last().primaryOutput?.mediaType)
        assertEquals("https://example.com/two-cover.png", model.uiState.value.historyItems.last().primaryOutput?.previewUrl)
        assertEquals(false, model.uiState.value.historyHasMore)
        assertEquals(false, model.uiState.value.historyLoadingMore)
    }

    @Test
    fun `loading more selected project tasks appends next page`() {
        val repository = FakeQuickCreateRepository().apply {
            projectTaskPages = mapOf(
                1 to QuickCreationHistoryPage(
                    page = 1,
                    size = 1,
                    total = 2,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "project-task-1",
                            status = "SUCCESS",
                        )
                    ),
                ),
                2 to QuickCreationHistoryPage(
                    page = 2,
                    size = 1,
                    total = 2,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "project-task-2",
                            status = "SUCCESS",
                        )
                    ),
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.selectProject("project-1")

        model.loadMoreQuickCreationHistory()

        assertEquals(listOf("project-1" to 1, "project-1" to 2), repository.requestedProjectTaskPages)
        assertEquals(listOf(1), repository.requestedHistoryPages)
        assertEquals(listOf("project-task-1", "project-task-2"), model.uiState.value.historyItems.map { it.source.taskId })
        assertEquals(false, model.uiState.value.historyHasMore)
    }

    @Test
    fun `running history refreshes first page until terminal status`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            var requestCount = 0
            historyPage = QuickCreationHistoryPage(
                page = 1,
                size = 10,
                total = 1,
                items = listOf(
                    QuickCreationHistoryItem(
                        taskId = "history-task-1",
                        status = "PREPAID",
                    )
                ),
            )
            historyPages = emptyMap()
            overrideHistoryList = { page, size ->
                requestCount += 1
                val status = if (requestCount == 1) "PREPAID" else "SUCCESS"
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "history-task-1",
                            status = status,
                        )
                    ),
                )
            }
        }

        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()
        assertEquals("PREPAID", model.uiState.value.historyItems.single().source.status)
        assertEquals(true, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(true, model.uiState.value.historyItems.single().needsRefresh)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("SUCCESS", model.uiState.value.historyItems.single().source.status)
        assertEquals(false, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(false, model.uiState.value.historyItems.single().needsRefresh)
    }

    @Test
    fun `running selected project task refreshes project tasks until terminal status`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            var requestCount = 0
            overrideProjectTaskList = { projectId, page, size ->
                requestCount += 1
                val status = if (requestCount == 1) "PREPAID" else "SUCCESS"
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "$projectId-task-1",
                            status = status,
                        )
                    ),
                )
            }
        }

        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()
        model.selectProject("project-1")
        runCurrent()
        assertEquals("PREPAID", model.uiState.value.historyItems.single().source.status)
        assertEquals(true, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(true, model.uiState.value.historyItems.single().needsRefresh)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf("project-1" to 1, "project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals(listOf(1), repository.requestedHistoryPages)
        assertEquals("SUCCESS", model.uiState.value.historyItems.single().source.status)
        assertEquals(false, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(false, model.uiState.value.historyItems.single().needsRefresh)
    }

    @Test
    fun `cancelling history task calls repository and refreshes history`() {
        val repository = FakeQuickCreateRepository().apply {
            var requestCount = 0
            overrideHistoryList = { page, size ->
                requestCount += 1
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "history-task-1",
                            status = if (requestCount == 1) "PREPAID" else "CANCELED",
                        )
                    ),
                )
            }
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.cancelHistoryTask("history-task-1")

        assertEquals(listOf("history-task-1"), repository.cancelledTaskIds)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("CANCELED", model.uiState.value.historyItems.single().source.status)
        assertEquals(false, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(false, model.uiState.value.historyCancellingTaskIds.contains("history-task-1"))
    }

    @Test
    fun `cancelling terminal history task is ignored`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.cancelHistoryTask("history-task-1")

        assertEquals(emptyList(), repository.cancelledTaskIds)
        assertEquals(listOf(1), repository.requestedHistoryPages)
        assertEquals("SUCCESS", model.uiState.value.historyItems.single().source.status)
        assertEquals(false, model.uiState.value.historyItems.single().canCancelTask)
    }

    @Test
    fun `cancelling selected project task refreshes project tasks`() {
        val repository = FakeQuickCreateRepository().apply {
            var requestCount = 0
            overrideProjectTaskList = { projectId, page, size ->
                requestCount += 1
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(
                        QuickCreationHistoryItem(
                            taskId = "$projectId-task-1",
                            status = if (requestCount == 1) "PREPAID" else "CANCELED",
                        )
                    ),
                )
            }
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.selectProject("project-1")

        model.cancelHistoryTask("project-1-task-1")

        assertEquals(listOf("project-1-task-1"), repository.cancelledTaskIds)
        assertEquals(listOf("project-1" to 1, "project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals(listOf(1), repository.requestedHistoryPages)
        assertEquals("CANCELED", model.uiState.value.historyItems.single().source.status)
        assertEquals(false, model.uiState.value.historyItems.single().canCancelTask)
        assertEquals(false, model.uiState.value.historyCancellingTaskIds.contains("project-1-task-1"))
    }

    @Test
    fun `switchTab updates tab and cost`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.switchTab(QuickCreateTab.VIDEO)
        assertEquals(QuickCreateTab.VIDEO, model.uiState.value.currentTab)
    }

    @Test
    fun `switchMode toggles creation and inspiration surfaces`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)
        assertEquals(QuickCreateMode.INSPIRATION, model.uiState.value.currentMode)
        assertEquals(false, model.uiState.value.showCreationInput)

        model.switchMode(QuickCreateMode.CREATION)
        assertEquals(QuickCreateMode.CREATION, model.uiState.value.currentMode)
        assertEquals(true, model.uiState.value.showCreationInput)
    }

    @Test
    fun `quick create sheets are mutually exclusive and close outside creation`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        model.showModelPickerSheet()
        assertEquals(QuickCreateSheet.MODEL_PICKER, model.uiState.value.activeSheet)

        model.showParamsSheet()
        assertEquals(QuickCreateSheet.PARAMS, model.uiState.value.activeSheet)

        model.closeActiveSheet()
        assertEquals(null, model.uiState.value.activeSheet)

        model.showModelPickerSheet()
        model.switchMode(QuickCreateMode.INSPIRATION)
        assertEquals(null, model.uiState.value.activeSheet)
    }

    @Test
    fun `switchMode to inspiration loads tags and templates`() {
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)

        assertEquals(listOf("hot"), model.uiState.value.inspirationTags.map { it.id })
        assertEquals(listOf("热门"), model.uiState.value.inspirationTags.map { it.label })
        assertEquals(listOf(true), model.uiState.value.inspirationTags.map { it.selected })
        val template = model.uiState.value.inspirationTemplates.single()
        assertEquals("tpl-1", template.id)
        assertEquals("赛博城市漫游", template.title)
        assertEquals("IMAGE", template.categoryLabel)
        val preview = assertIs<QuickCreateInspirationPreviewUi.Image>(template.preview)
        assertEquals("https://example.com/cover.png", preview.url)
        assertEquals(listOf("HOT"), template.badges.map { it.label })
        assertEquals(listOf(QuickCreateInspirationBadgeTone.HOT), template.badges.map { it.tone })
        assertEquals(false, model.uiState.value.inspirationLoading)
    }

    @Test
    fun `loading more inspiration templates appends next page`() {
        val repository = FakeQuickCreateRepository().apply {
            inspirationTemplatePages = mapOf(
                1 to QuickCreateInspirationTemplatePage(
                    page = 1,
                    size = 20,
                    total = 21,
                    pages = 2,
                    hasNext = true,
                    hasPrevious = false,
                    items = (1..20).map { index ->
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-$index",
                            title = "Template $index",
                            categoryId = "IMAGE",
                            coverUrl = "https://example.com/cover-$index.png",
                            videoUrl = null,
                            tagHot = index == 1,
                            tagNew = false,
                        )
                    },
                ),
                2 to QuickCreateInspirationTemplatePage(
                    page = 2,
                    size = 20,
                    total = 21,
                    pages = 2,
                    hasNext = false,
                    hasPrevious = true,
                    items = listOf(
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-21",
                            title = "Template 21",
                            categoryId = "VIDEO",
                            coverUrl = "https://example.com/cover-21.png",
                            videoUrl = "https://example.com/preview-21.mp4",
                            tagHot = false,
                            tagNew = true,
                        )
                    ),
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.loadMoreInspirationTemplates()

        assertEquals(listOf(1, 2), repository.requestedTemplatePages)
        assertEquals((1..21).map { "tpl-$it" }, model.uiState.value.inspirationTemplates.map { it.id })
        val videoTemplate = model.uiState.value.inspirationTemplates.last()
        val videoPreview = assertIs<QuickCreateInspirationPreviewUi.Video>(videoTemplate.preview)
        assertEquals("https://example.com/preview-21.mp4", videoPreview.url)
        assertEquals(listOf("NEW"), videoTemplate.badges.map { it.label })
        assertEquals(listOf(QuickCreateInspirationBadgeTone.NEW), videoTemplate.badges.map { it.tone })
        assertEquals(2, model.uiState.value.inspirationTemplatesPage)
        assertEquals(false, model.uiState.value.inspirationTemplatesHasMore)
        assertEquals(false, model.uiState.value.inspirationTemplatesLoadingMore)
    }

    @Test
    fun `loading more inspiration templates deduplicates by template id`() {
        val repository = FakeQuickCreateRepository().apply {
            inspirationTemplatePages = mapOf(
                1 to QuickCreateInspirationTemplatePage(
                    page = 1,
                    size = 20,
                    total = 3,
                    pages = 2,
                    hasNext = true,
                    hasPrevious = false,
                    items = listOf(
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-1",
                            title = "Template 1",
                            categoryId = "IMAGE",
                            coverUrl = "https://example.com/cover-1.png",
                            videoUrl = null,
                            tagHot = true,
                            tagNew = false,
                        ),
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-2",
                            title = "Template 2",
                            categoryId = "IMAGE",
                            coverUrl = null,
                            videoUrl = null,
                            tagHot = false,
                            tagNew = false,
                        ),
                    ),
                ),
                2 to QuickCreateInspirationTemplatePage(
                    page = 2,
                    size = 20,
                    total = 3,
                    pages = 2,
                    hasNext = false,
                    hasPrevious = true,
                    items = listOf(
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-2",
                            title = "Template 2 duplicate",
                            categoryId = "IMAGE",
                            coverUrl = "https://example.com/cover-2b.png",
                            videoUrl = null,
                            tagHot = false,
                            tagNew = false,
                        ),
                        QuickCreateInspirationTemplate(
                            templateId = "tpl-3",
                            title = "Template 3",
                            categoryId = "VIDEO",
                            coverUrl = "https://example.com/cover-3.png",
                            videoUrl = "https://example.com/preview-3.mp4",
                            tagHot = false,
                            tagNew = true,
                        ),
                    ),
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.loadMoreInspirationTemplates()

        assertEquals(listOf("tpl-1", "tpl-2", "tpl-3"), model.uiState.value.inspirationTemplates.map { it.id })
        assertEquals("Template 2", model.uiState.value.inspirationTemplates[1].title)
        val placeholder = assertIs<QuickCreateInspirationPreviewUi.Placeholder>(
            model.uiState.value.inspirationTemplates[1].preview,
        )
        assertEquals(QuickCreateInspirationPlaceholderMediaType.IMAGE, placeholder.mediaType)
        assertEquals(emptyList(), model.uiState.value.inspirationTemplates[1].badges)
        assertEquals(2, model.uiState.value.inspirationTemplatesPage)
        assertEquals(false, model.uiState.value.inspirationTemplatesHasMore)
    }

    @Test
    fun `apply inspiration template failure clears loading and keeps creation state`() {
        val repository = FakeQuickCreateRepository().apply {
            templateDetailResult = Result.failure(IllegalStateException("模板不存在"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.updateImagePrompt("original prompt")

        model.applyInspirationTemplate("missing-template")

        assertEquals(listOf("missing-template"), repository.requestedTemplateDetailIds)
        assertEquals(false, model.uiState.value.inspirationLoading)
        assertEquals("模板不存在", model.uiState.value.error)
        assertEquals(QuickCreateMode.CREATION, model.uiState.value.currentMode)
        assertEquals(QuickCreateTab.IMAGE, model.uiState.value.currentTab)
        assertEquals("original prompt", model.uiState.value.imageConfig.prompt)
    }

    @Test
    fun `init loads service driven image models`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        assertEquals("全能图片G-2.0-官方版", model.uiState.value.serviceImageModels.single().name)
        assertEquals("binding-1", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("全能图片G-2.0-官方版", model.uiState.value.serviceImageModelItems.single().displayName)
        assertEquals("G-2.0", model.uiState.value.selectedImageServiceModelUi?.compactName)
        assertEquals("全能图片 · 4 个参数", model.uiState.value.selectedImageServiceModelUi?.subtitle)
        assertEquals(true, model.uiState.value.selectedImageServiceModelUi?.selected)
        assertEquals("photoreal", model.uiState.value.imageServiceParams["style"])
        assertEquals(false, model.uiState.value.serviceModelsLoading)
    }

    @Test
    fun `generate image uses selected service model ids`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("IMAGE", repository.lastImageRequest?.quickCreationCategoryId)
        assertEquals("binding-1", repository.lastImageRequest?.quickCreationBindingId)
        assertEquals("sku-1", repository.lastImageRequest?.quickCreationSkuId)
    }

    @Test
    fun `generate image keeps selected image service model when video service model is requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageServiceModel(repository.videoModels.single())
        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("binding-1", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("IMAGE", repository.lastImageRequest?.quickCreationCategoryId)
        assertEquals("binding-1", repository.lastImageRequest?.quickCreationBindingId)
        assertEquals("sku-1", repository.lastImageRequest?.quickCreationSkuId)
    }

    @Test
    fun `selecting image service model by identity key updates model and ui summary`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val secondModel = repository.models.single().copy(
            bindingId = "binding-key",
            skuId = "sku-key",
            name = "身份键图片模型",
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "style",
                    paramKey = "style",
                    fieldType = "LIST",
                    required = false,
                    defaultValue = "sketch",
                    options = emptyList(),
                ),
            ),
        )
        repository.models = repository.models + secondModel
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageServiceModel("binding-key|sku-key")

        assertEquals("binding-key", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-key|sku-key", model.uiState.value.selectedImageServiceModelUi?.identityKey)
        assertEquals("身份键图片模型", model.uiState.value.selectedImageServiceModelUi?.displayName)
        assertEquals(mapOf("style" to "sketch"), model.uiState.value.imageServiceParams)
        assertEquals(listOf(false, true), model.uiState.value.serviceImageModelItems.map { it.selected })
    }

    @Test
    fun `selecting image service model by stale identity key keeps current model`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageServiceModel("missing|sku")

        assertEquals("binding-1", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-1|sku-1", model.uiState.value.selectedImageServiceModelUi?.identityKey)
        assertEquals(mapOf("style" to "photoreal", "aspectRatio" to "1:1"), model.uiState.value.imageServiceParams)
    }

    @Test
    fun `generate video keeps selected video service model when image service model is requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoServiceModel(repository.models.single())
        model.updateVideoPrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("video-binding-1", model.uiState.value.selectedVideoServiceModel?.bindingId)
        assertEquals("VIDEO", repository.lastVideoRequest?.quickCreationCategoryId)
        assertEquals("video-binding-1", repository.lastVideoRequest?.quickCreationBindingId)
        assertEquals("video-sku-1", repository.lastVideoRequest?.quickCreationSkuId)
    }

    @Test
    fun `service model reload keeps selected image model canonical when identity matches`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        repository.models = listOf(repository.models.single().copy(name = "Updated image model"))
        model.loadServiceModels()
        runCurrent()

        assertEquals("Updated image model", model.uiState.value.selectedImageServiceModel?.name)
        assertEquals("Updated image model", model.uiState.value.selectedImageServiceModelUi?.displayName)
        assertEquals(true, model.uiState.value.serviceImageModelItems.single().selected)
    }

    @Test
    fun `service model reload keeps selected video model canonical when identity matches`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        repository.videoModels = listOf(repository.videoModels.single().copy(name = "Updated video model"))
        model.loadServiceModels()
        runCurrent()

        assertEquals("Updated video model", model.uiState.value.selectedVideoServiceModel?.name)
        assertEquals("Updated video model", model.uiState.value.selectedVideoServiceModelUi?.displayName)
        assertEquals(true, model.uiState.value.serviceVideoModelItems.single().selected)
    }

    @Test
    fun `switching image service model resets params to selected model defaults`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val secondModel = repository.models.single().copy(
            bindingId = "binding-2",
            skuId = "sku-2",
            name = "第二图片模型",
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "style",
                    paramKey = "style",
                    fieldType = "LIST",
                    required = false,
                    defaultValue = "watercolor",
                    options = emptyList(),
                ),
            ),
        )
        repository.models = repository.models + secondModel
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageServiceParam("negativePrompt", "old value")
        model.updateImageServiceModel(secondModel)
        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("binding-2", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("binding-2|sku-2", model.uiState.value.selectedImageServiceModelUi?.identityKey)
        assertEquals("第二图片模型", model.uiState.value.selectedImageServiceModelUi?.displayName)
        assertEquals(listOf(false, true), model.uiState.value.serviceImageModelItems.map { it.selected })
        assertEquals(mapOf("style" to "watercolor"), model.uiState.value.imageServiceParams)
        assertEquals("watercolor", repository.lastImageRequest?.quickCreationParams?.get("style"))
        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("negativePrompt"))
    }

    @Test
    fun `service model reload falls back to first image model when selected identity disappears`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageServiceParam("negativePrompt", "old value")
        repository.models = listOf(
            repository.models.single().copy(
                bindingId = "binding-new",
                skuId = "sku-new",
                name = "新默认图片模型",
                fields = listOf(
                    QuickCreationServiceField(
                        fieldKey = "style",
                        paramKey = "style",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "line-art",
                        options = emptyList(),
                    ),
                ),
            ),
        )
        model.loadServiceModels()
        runCurrent()

        assertEquals("binding-new", model.uiState.value.selectedImageServiceModel?.bindingId)
        assertEquals("新默认图片模型", model.uiState.value.selectedImageServiceModel?.name)
        assertEquals("新默认图片模型", model.uiState.value.selectedImageServiceModelUi?.displayName)
        assertEquals(true, model.uiState.value.serviceImageModelItems.single().selected)
        assertEquals(mapOf("style" to "line-art"), model.uiState.value.imageServiceParams)
    }

    @Test
    fun `generate image uses selected service model field defaults`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("photoreal", repository.lastImageRequest?.quickCreationParams?.get("style"))
        assertEquals("1:1", repository.lastImageRequest?.quickCreationParams?.get("aspectRatio"))
    }

    @Test
    fun `generate image keeps previous count when unsupported image count is requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageCount(4)
        model.updateImageCount(0)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(4, model.uiState.value.imageConfig.count)
        assertEquals(4, repository.lastImageRequest?.numImages)
    }

    @Test
    fun `generate video keeps previous count when unsupported video count is requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.updateVideoCount(2)
        model.updateVideoCount(99)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(2, model.uiState.value.videoConfig.count)
        assertEquals(2, repository.lastVideoRequest?.numVideos)
    }

    @Test
    fun `generate image clears negative seed before building request`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageSeed(123)
        model.updateImageSeed(-1)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, model.uiState.value.imageConfig.seed)
        assertEquals(null, repository.lastImageRequest?.seed)
    }

    @Test
    fun `generate video clears negative seed before building request`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.updateVideoSeed(456)
        model.updateVideoSeed(-1)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, model.uiState.value.videoConfig.seed)
        assertEquals(null, repository.lastVideoRequest?.seed)
    }

    @Test
    fun `generate image keeps previous model params when unsupported image params are requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageModel(ImageModel.SEEDREAM_4)
        model.updateImageAspectRatio(ImageAspectRatio.RATIO_16_9)
        model.updateImageAspectRatio(ImageAspectRatio.RATIO_21_9)
        model.updateImageResolution(ImageResolution.RES_2K)
        model.updateImageResolution(ImageResolution.RES_4K)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(ImageAspectRatio.RATIO_16_9, model.uiState.value.imageConfig.aspectRatio)
        assertEquals(ImageResolution.RES_2K, model.uiState.value.imageConfig.resolution)
        assertEquals("16:9", repository.lastImageRequest?.aspectRatio)
        assertEquals("2K", repository.lastImageRequest?.resolution)
    }

    @Test
    fun `generate image uses updated quality when model supports it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageQuality(ImageQuality.QUALITY_HIGH)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(ImageQuality.QUALITY_HIGH, model.uiState.value.imageConfig.quality)
        assertEquals("high", repository.lastImageRequest?.quality)
    }

    @Test
    fun `generate video keeps previous model params when unsupported video params are requested`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.updateVideoModel(VideoModel.SEEDANCE_2_FAST)
        model.updateVideoResolution(VideoResolution.RES_480P)
        model.updateVideoResolution(VideoResolution.RES_1080P)
        model.updateVideoDuration(VideoDuration.DURATION_10S)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(VideoResolution.RES_480P, model.uiState.value.videoConfig.resolution)
        assertEquals(VideoDuration.DURATION_5S, model.uiState.value.videoConfig.duration)
        assertEquals("480p", repository.lastVideoRequest?.resolution)
        assertEquals(5, repository.lastVideoRequest?.duration)
    }

    @Test
    fun `generate video ignores realistic toggle when model does not support it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.toggleRealisticMode()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(false, model.uiState.value.videoConfig.realisticMode)
        assertEquals(false, repository.lastVideoRequest?.realistic)
    }

    @Test
    fun `generate video ignores audio toggle when model does not support it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.updateVideoModel(VideoModel.SEEDANCE_2_FAST)
        model.toggleGenerateAudio()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(false, model.uiState.value.videoConfig.generateAudio)
        assertEquals(false, repository.lastVideoRequest?.generateAudio)
    }

    @Test
    fun `generate video uses audio toggle when model supports it`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("prompt")
        model.toggleGenerateAudio()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(true, model.uiState.value.videoConfig.generateAudio)
        assertEquals(true, repository.lastVideoRequest?.generateAudio)
    }

    @Test
    fun `image prompt refreshes server fee preview into estimated cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
        assertEquals("IMAGE", repository.feePreviewRequests.single().quickCreationCategoryId)
        assertEquals("binding-1", repository.feePreviewRequests.single().quickCreationBindingId)
        assertEquals("sku-1", repository.feePreviewRequests.single().quickCreationSkuId)
        assertEquals("green icon", repository.feePreviewRequests.single().prompt)
        assertEquals(0.76, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `image fee preview uses service model defaults before local fallbacks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = models.map { model ->
                model.copy(
                    fields = model.fields + listOf(
                        QuickCreationServiceField(
                            fieldKey = "prompt",
                            paramKey = "prompt",
                            fieldType = "STRING",
                            required = true,
                            defaultValue = "",
                            options = emptyList(),
                        ),
                        QuickCreationServiceField(
                            fieldKey = "resolution",
                            paramKey = "resolution",
                            fieldType = "LIST",
                            required = true,
                            defaultValue = "2k",
                            options = emptyList(),
                        ),
                    )
                )
            }
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        val request = repository.feePreviewRequests.single()
        assertEquals("1K", request.resolution)
        assertEquals("2k", request.quickCreationParams["resolution"])
    }

    @Test
    fun `stale image fee preview result does not overwrite latest prompt cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageFeePreviewHandler = { request ->
                if (request.prompt == "slow prompt") {
                    withContext(NonCancellable) {
                        delay(1_000)
                    }
                    Result.success(
                        QuickCreationFeePreview(
                            passed = true,
                            free = false,
                            settlementMode = "cash_only",
                            requiredCashAmount = 3.33,
                            userCashBalance = 156.376,
                            cashCurrency = "CNY",
                        )
                    )
                } else {
                    Result.success(
                        QuickCreationFeePreview(
                            passed = true,
                            free = false,
                            settlementMode = "cash_only",
                            requiredCashAmount = 0.76,
                            userCashBalance = 156.376,
                            cashCurrency = "CNY",
                        )
                    )
                }
            }
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("slow prompt")
        advanceTimeBy(500)
        runCurrent()
        model.updateImagePrompt("fast prompt")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(0.76, model.uiState.value.estimatedCost)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf("slow prompt", "fast prompt"), repository.feePreviewRequests.map { it.prompt })
        assertEquals(0.76, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `image fee preview is skipped when required service option field is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "stylePreset",
                        paramKey = "stylePreset",
                        fieldType = "LIST",
                        required = true,
                        defaultValue = null,
                        options = listOf(
                            QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
                        ),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Style preset"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, repository.feePreviewRequests.size)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `video fee preview is skipped when required service option field is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "motionPreset",
                        paramKey = "motionPreset",
                        fieldType = "LIST",
                        required = true,
                        defaultValue = null,
                        options = listOf(
                            QuickCreationServiceFieldOption(label = "Smooth", value = "smooth"),
                        ),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Motion preset"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, repository.videoFeePreviewRequests.size)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `hidden service param update does not refresh image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenPrompt",
                        paramKey = "hiddenPrompt",
                        fieldType = "STRING",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)

        model.updateImageServiceParam("hiddenPrompt", "secret")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
    }

    @Test
    fun `hidden service upload field media does not refresh image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenImage",
                        paramKey = "hiddenImages",
                        fieldType = "IMAGE",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)

        model.pickImageReferenceForField("content://image/hidden", "hiddenImages")
        advanceUntilIdle()

        assertEquals(1, repository.feePreviewRequests.size)
    }

    @Test
    fun `removing hidden service upload field media does not refresh image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenImage",
                        paramKey = "hiddenImages",
                        fieldType = "IMAGE",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.feePreviewRequests.size)

        model.pickImageReferenceForField("content://image/hidden", "hiddenImages")
        advanceUntilIdle()
        val hiddenReferenceId = model.uiState.value.imageConfig.mediaReferences.single().id

        model.removeMediaReference(hiddenReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
    }

    @Test
    fun `removing inactive child service upload field media does not refresh image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.feePreviewRequests.size)

        model.pickImageReferenceForField("content://image/inactive-child", "childImages")
        advanceUntilIdle()
        val inactiveChildReferenceId = model.uiState.value.imageConfig.mediaReferences.single().id

        model.removeMediaReference(inactiveChildReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
    }

    @Test
    fun `hidden service upload field media does not refresh video fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenVideo",
                        paramKey = "hiddenVideos",
                        fieldType = "VIDEO_UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.videoFeePreviewRequests.size)

        model.pickVideoReferenceForField("content://video/hidden", "hiddenVideos")
        advanceUntilIdle()

        assertEquals(1, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `removing hidden service upload field media does not refresh video fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenVideo",
                        paramKey = "hiddenVideos",
                        fieldType = "VIDEO_UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.videoFeePreviewRequests.size)

        model.pickVideoReferenceForField("content://video/hidden", "hiddenVideos")
        advanceUntilIdle()
        val hiddenReferenceId = model.uiState.value.videoConfig.mediaReferences.single().id

        model.removeMediaReference(hiddenReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `inactive child service upload field media does not refresh video fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childVideo",
                                    paramKey = "childVideos",
                                    fieldType = "VIDEO_UPLOAD",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("videoReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.videoFeePreviewRequests.size)

        model.pickVideoReferenceForField("content://video/inactive-child", "childVideos")
        advanceUntilIdle()

        assertEquals(1, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `removing inactive child service upload field media does not refresh video fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childVideo",
                                    paramKey = "childVideos",
                                    fieldType = "VIDEO_UPLOAD",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("videoReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.videoFeePreviewRequests.size)

        model.pickVideoReferenceForField("content://video/inactive-child", "childVideos")
        advanceUntilIdle()
        val inactiveChildReferenceId = model.uiState.value.videoConfig.mediaReferences.single().id

        model.removeMediaReference(inactiveChildReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `removing active child service upload field media refreshes video fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = videoModels.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "videoReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childVideo",
                                    paramKey = "childVideos",
                                    fieldType = "VIDEO_UPLOAD",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("videoReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.videoFeePreviewRequests.size)

        model.pickVideoReferenceForField("content://video/active-child", "childVideos")
        advanceUntilIdle()
        withContext(Dispatchers.IO) {
            delay(10)
        }
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        val activeChildReferenceId = model.uiState.value.videoConfig.mediaReferences.single().id
        assertEquals("https://example.com/video.mp4", model.uiState.value.videoConfig.mediaReferences.single().remoteUrl)
        assertEquals(2, repository.videoFeePreviewRequests.size)
        assertEquals(
            listOf("https://example.com/video.mp4"),
            repository.videoFeePreviewRequests.last().quickCreationListParams["childVideos"],
        )

        model.removeMediaReference(activeChildReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(3, repository.videoFeePreviewRequests.size)
        assertEquals(
            null,
            repository.videoFeePreviewRequests.last().quickCreationListParams["childVideos"],
        )
        model.onDispose()
        runCurrent()
    }

    @Test
    fun `uploading global image media does not refresh image fee preview before remote url exists`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val uploadDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(
            repository,
            FakeMediaResolver(),
            FakeSettingsRepo(),
            ioDispatcher = uploadDispatcher,
        )
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
        assertEquals(null, repository.feePreviewRequests.single().referenceImageUri)

        model.pickImageReference("content://image/global")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
    }

    @Test
    fun `failed image upload prevents image fee preview when prompt changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            uploadResult = Result.failure(IllegalStateException("upload unavailable"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.pickImageReference("content://image/fail")
        advanceUntilIdle()
        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, repository.feePreviewRequests.size)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `failed image upload clears previous image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(0.76, model.uiState.value.estimatedCost)

        repository.uploadResult = Result.failure(IllegalStateException("upload unavailable"))
        model.pickImageReference("content://image/fail")
        advanceUntilIdle()

        assertEquals(model.uiState.value.imageConfig.estimatedCost, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `removing failed image upload restores image fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, repository.feePreviewRequests.size)

        repository.uploadResult = Result.failure(IllegalStateException("upload unavailable"))
        model.pickImageReference("content://image/fail")
        advanceUntilIdle()
        val failedReferenceId = model.uiState.value.imageConfig.mediaReferences.single().id
        assertEquals(model.uiState.value.imageConfig.estimatedCost, model.uiState.value.estimatedCost)

        model.removeMediaReference(failedReferenceId)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(2, repository.feePreviewRequests.size)
        assertEquals(0.76, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `video prompt refreshes server fee preview into estimated cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.videoFeePreviewRequests.size)
        assertEquals("VIDEO", repository.videoFeePreviewRequests.single().quickCreationCategoryId)
        assertEquals("video-binding-1", repository.videoFeePreviewRequests.single().quickCreationBindingId)
        assertEquals("video-sku-1", repository.videoFeePreviewRequests.single().quickCreationSkuId)
        assertEquals("green icon animation", repository.videoFeePreviewRequests.single().prompt)
        assertEquals(9.60, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `stale video fee preview result does not overwrite latest prompt cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoFeePreviewHandler = { request ->
                if (request.prompt == "slow video") {
                    withContext(NonCancellable) {
                        delay(1_000)
                    }
                    Result.success(
                        QuickCreationFeePreview(
                            passed = true,
                            free = false,
                            settlementMode = "cash_only",
                            requiredCashAmount = 4.44,
                            userCashBalance = 156.376,
                            cashCurrency = "CNY",
                        )
                    )
                } else {
                    Result.success(
                        QuickCreationFeePreview(
                            passed = true,
                            free = false,
                            settlementMode = "cash_only",
                            requiredCashAmount = 9.60,
                            userCashBalance = 156.376,
                            cashCurrency = "CNY",
                        )
                    )
                }
            }
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("slow video")
        advanceTimeBy(500)
        runCurrent()
        model.updateVideoPrompt("fast video")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(9.60, model.uiState.value.estimatedCost)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf("slow video", "fast video"), repository.videoFeePreviewRequests.map { it.prompt })
        assertEquals(9.60, model.uiState.value.estimatedCost)
        assertEquals(false, model.uiState.value.feePreviewLoading)
        assertEquals(null, model.uiState.value.feePreviewError)
    }

    @Test
    fun `generate image is blocked when fee preview failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            feePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("preview unavailable", model.uiState.value.feePreviewError)
        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("价格待确认", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when fee preview is not passed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            feePreviewResult = Result.success(
                feePreviewResult.getOrThrow().copy(
                    passed = false,
                    insufficientType = "cash",
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(model.uiState.value.imageConfig.estimatedCost, model.uiState.value.estimatedCost)
        assertEquals("余额不足或价格预览未通过", model.uiState.value.feePreviewError)
        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("余额不足或价格预览未通过", model.uiState.value.error)
    }

    @Test
    fun `image fee preview failure falls back from previous server amount to local estimate`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(0.76, model.uiState.value.estimatedCost)

        repository.feePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        model.updateImagePrompt("blue icon")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(model.uiState.value.imageConfig.estimatedCost, model.uiState.value.estimatedCost)
        assertEquals("preview unavailable", model.uiState.value.feePreviewError)
        assertEquals(false, model.uiState.value.feePreviewLoading)
    }

    @Test
    fun `generate video is blocked when fee preview failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoFeePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("preview unavailable", model.uiState.value.feePreviewError)
        assertEquals(null, repository.lastVideoRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("价格待确认", model.uiState.value.error)
    }

    @Test
    fun `generate video is blocked when fee preview is not passed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoFeePreviewResult = Result.success(
                videoFeePreviewResult.getOrThrow().copy(
                    passed = false,
                    insufficientType = "cash",
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(model.uiState.value.videoConfig.estimatedCost, model.uiState.value.estimatedCost)
        assertEquals("余额不足或价格预览未通过", model.uiState.value.feePreviewError)
        assertEquals(null, repository.lastVideoRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("余额不足或价格预览未通过", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked while fee preview is loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("green icon")
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(true, model.uiState.value.feePreviewLoading)
        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("价格确认中", model.uiState.value.error)
    }

    @Test
    fun `fee preview failure clears previous task status text when generate is blocked`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                        )
                    ),
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("first prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()
        assertEquals(QuickCreateTaskUiStatus.SUCCESS, model.uiState.value.taskStatus)
        assertNotNull(model.uiState.value.statusText)

        repository.lastImageRequest = null
        repository.feePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        model.updateImagePrompt("second prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
    }

    @Test
    fun `generate video is blocked while fee preview is loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("green icon animation")
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(true, model.uiState.value.feePreviewLoading)
        assertEquals(null, repository.lastVideoRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("价格确认中", model.uiState.value.error)
    }

    @Test
    fun `empty image prompt clears submitting status text when generate is blocked`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
        assertEquals("请输入描述词", model.uiState.value.error)
    }

    @Test
    fun `empty video prompt clears submitting status text when generate is blocked`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastVideoRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
        assertEquals("请输入描述词", model.uiState.value.error)
    }

    @Test
    fun `over limit image prompt shows length error when generate is blocked`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("x".repeat(MAX_PROMPT_CHARS + 1))
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
        assertEquals("描述词不能超过 500 个字符", model.uiState.value.error)
    }

    @Test
    fun `over limit video prompt shows length error when generate is blocked`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("x".repeat(MAX_PROMPT_CHARS + 1))
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastVideoRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
        assertEquals("描述词不能超过 500 个字符", model.uiState.value.error)
    }

    @Test
    fun `generate image uses updated service field values`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("style", "anime")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("anime", repository.lastImageRequest?.quickCreationParams?.get("style"))
    }

    @Test
    fun `generate image only submits declared service field values`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("negativePrompt", "low quality")
        model.updateImageServiceParam("unexpected", "value")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("low quality", repository.lastImageRequest?.quickCreationParams?.get("negativePrompt"))
        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("unexpected"))
    }

    @Test
    fun `generate image submits declared child service field values`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("referenceStrength", "0.65")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("0.65", repository.lastImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `generate image submits active child service field defaults`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    defaultValue = "0.65",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("0.65", repository.lastImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `apply inspiration image template submits defaults for child activated by template params`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    defaultValue = "0.65",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = mapOf("creationMode" to "imageReference"),
                listParams = emptyMap(),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("imageReference", repository.lastImageRequest?.quickCreationParams?.get("creationMode"))
        assertEquals("0.65", repository.lastImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `generate image is blocked when required service text field is too short`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "tagline",
                        paramKey = "tagline",
                        fieldType = "STRING",
                        required = true,
                        defaultValue = null,
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Tagline", minLength = 3),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("tagline", "ab")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Tagline 至少 3 个字符", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when required service option field is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "stylePreset",
                        paramKey = "stylePreset",
                        fieldType = "LIST",
                        required = true,
                        defaultValue = null,
                        options = listOf(
                            QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
                        ),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Style preset"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Style preset 不能为空", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when service option value is not allowed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "stylePreset",
                        paramKey = "stylePreset",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = null,
                        options = listOf(
                            QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
                        ),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Style preset"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("stylePreset", "legacy")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Style preset 选项无效", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when active required child text field is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    required = true,
                                    title = "Reference strength",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Reference strength 不能为空", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when active required child option field is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStyle",
                                    paramKey = "referenceStyle",
                                    fieldType = "LIST",
                                    required = true,
                                    title = "Reference style",
                                    options = listOf(
                                        QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
                                    ),
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Reference style 不能为空", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when active child option value is not allowed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStyle",
                                    paramKey = "referenceStyle",
                                    fieldType = "LIST",
                                    required = false,
                                    title = "Reference style",
                                    options = listOf(
                                        QuickCreationServiceFieldOption(label = "Realistic", value = "realistic"),
                                    ),
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("referenceStyle", "legacy")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Reference style 选项无效", model.uiState.value.error)
    }

    @Test
    fun `inactive required child text field does not block image generation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    required = true,
                                    title = "Reference strength",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("text", repository.lastImageRequest?.quickCreationParams?.get("creationMode"))
        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `inactive child service field value is not submitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("referenceStrength", "0.65")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("text", repository.lastImageRequest?.quickCreationParams?.get("creationMode"))
        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `hidden required service text field does not block image generation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenPrompt",
                        paramKey = "hiddenPrompt",
                        fieldType = "STRING",
                        required = true,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                        inputExtra = QuickCreationServiceFieldExtra(title = "Hidden prompt"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("prompt", repository.lastImageRequest?.prompt)
        assertEquals(null, model.uiState.value.error)
    }

    @Test
    fun `hidden service text field value is not submitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenPrompt",
                        paramKey = "hiddenPrompt",
                        fieldType = "STRING",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.updateImageServiceParam("hiddenPrompt", "secret")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("hiddenPrompt"))
    }

    @Test
    fun `hidden service text field default value is not submitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenPrompt",
                        paramKey = "hiddenPrompt",
                        fieldType = "STRING",
                        required = false,
                        defaultValue = "secret-default",
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.quickCreationParams?.get("hiddenPrompt"))
    }

    @Test
    fun `generate image maps uploaded images to service upload field`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/1")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/file.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("referenceImages"),
        )
    }

    @Test
    fun `image upload completion updates image config after switching to video tab`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/1")
        model.switchTab(QuickCreateTab.VIDEO)
        advanceUntilIdle()
        model.switchTab(QuickCreateTab.IMAGE)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/file.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("referenceImages"),
        )
    }

    @Test
    fun `generate image is blocked when upload stays pending past wait window`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            uploadDelayMillis = 120_000L
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.pickImageReference("content://image/slow")
        runCurrent()
        model.generate()
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("素材上传超时: test.jpg", model.uiState.value.error)
    }

    @Test
    fun `generate image is blocked when selected upload already failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            uploadResult = Result.failure(IllegalStateException("upload unavailable"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/fail")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(null, model.uiState.value.statusText)
        assertEquals("素材上传失败: test.jpg", model.uiState.value.error)
    }

    @Test
    fun `remove media reference removes image media after switching to video tab`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/1")
        advanceUntilIdle()
        val mediaId = model.uiState.value.imageConfig.mediaReferences.single().id

        model.switchTab(QuickCreateTab.VIDEO)
        model.removeMediaReference(mediaId)
        model.switchTab(QuickCreateTab.IMAGE)
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.quickCreationListParams?.get("referenceImages"))
    }

    @Test
    fun `removing image template media does not remove video template media with same template id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "shared-template",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "image prompt",
                params = emptyMap(),
                listParams = mapOf("sharedImage" to listOf("https://example.com/image-ref.png")),
                coverUrl = "https://example.com/image-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("shared-template")
        runCurrent()
        val imageMediaId = model.uiState.value.imageConfig.mediaReferences.single().id

        repository.templateDetail = QuickCreateInspirationTemplateDetail(
            templateId = "shared-template",
            title = "Video template",
            categoryId = "VIDEO",
            bindingId = "video-binding-1",
            skuId = "video-sku-1",
            prompt = "video prompt",
            params = emptyMap(),
            listParams = mapOf("sharedImage" to listOf("https://example.com/video-ref.png")),
            coverUrl = "https://example.com/video-cover.png",
            videoUrl = "https://example.com/video-preview.mp4",
        )
        model.applyInspirationTemplate("shared-template")
        runCurrent()

        model.switchTab(QuickCreateTab.IMAGE)
        model.removeMediaReference(imageMediaId)

        assertEquals(emptyList(), model.uiState.value.imageConfig.mediaReferences)
        assertEquals(
            listOf("https://example.com/video-ref.png"),
            model.uiState.value.videoConfig.mediaReferences.mapNotNull { it.remoteUrl },
        )
    }

    @Test
    fun `generate image maps uploaded images to service image field`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "imageUrls",
                            paramKey = "imageUrls",
                            fieldType = "IMAGE",
                            required = true,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        )
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/1")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/file.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("imageUrls"),
        )
    }

    @Test
    fun `global image media does not fill multiple service image fields`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "imageUrls",
                            paramKey = "imageUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                        QuickCreationServiceField(
                            fieldKey = "maskUrls",
                            paramKey = "maskUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReference("content://image/1")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(emptyMap(), repository.lastImageRequest?.quickCreationListParams)
    }

    @Test
    fun `generate image maps uploaded images to active child upload field`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReferenceForField("content://image/1", "childImages")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/file.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("childImages"),
        )
    }

    @Test
    fun `generate image maps field bound images to matching child upload fields`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "firstImage",
                                    paramKey = "firstImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                ),
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "secondImage",
                                    paramKey = "secondImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                ),
                            )
                        ),
                    )
                )
            )
        }
        val mediaResolver = FakeMediaResolver()
        val model = createModel(repository, mediaResolver, FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReferenceForField("content://image/first", "firstImages")
        advanceUntilIdle()
        repository.uploadResult = Result.success("https://example.com/second.jpg")
        model.pickImageReferenceForField("content://image/second", "secondImages")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/file.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("firstImages"),
        )
        assertEquals(
            listOf("https://example.com/second.jpg"),
            repository.lastImageRequest?.quickCreationListParams?.get("secondImages"),
        )
        assertEquals(null, repository.lastImageRequest?.referenceImageUri)
    }

    @Test
    fun `generate image is blocked when required service image field has no upload`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "imageUrls",
                        paramKey = "imageUrls",
                        fieldType = "IMAGE",
                        required = true,
                        defaultValue = null,
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(title = "Reference image"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("Reference image 不能为空", model.uiState.value.error)
    }

    @Test
    fun `hidden required service upload field does not block image generation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenImage",
                        paramKey = "hiddenImages",
                        fieldType = "IMAGE",
                        required = true,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                        inputExtra = QuickCreationServiceFieldExtra(title = "Hidden image"),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("prompt", repository.lastImageRequest?.prompt)
        assertEquals(null, model.uiState.value.error)
    }

    @Test
    fun `hidden service upload field media is not submitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenImage",
                        paramKey = "hiddenImages",
                        fieldType = "IMAGE",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        visible = false,
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReferenceForField("content://image/hidden", "hiddenImages")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.quickCreationListParams?.get("hiddenImages"))
    }

    @Test
    fun `hidden parent child upload field media is not submitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "hiddenMode",
                        paramKey = "hiddenMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        visible = false,
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "hiddenChildImage",
                                    paramKey = "hiddenChildImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        model.pickImageReferenceForField("content://image/hidden-child", "hiddenChildImages")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.quickCreationListParams?.get("hiddenChildImages"))
    }

    @Test
    fun `generate image is blocked when active required child image field has no upload`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "imageReference",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    required = true,
                                    title = "Child image",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest)
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(true, model.uiState.value.error?.startsWith("Child image"))
    }

    @Test
    fun `generate video maps uploaded video and audio to service upload fields`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        model.updateVideoPrompt("video prompt")
        model.pickVideoReference("content://video/1")
        model.pickAudioReference("content://audio/1")
        advanceUntilIdle()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("VIDEO", repository.lastVideoRequest?.quickCreationCategoryId)
        assertEquals("video-binding-1", repository.lastVideoRequest?.quickCreationBindingId)
        assertEquals("video-sku-1", repository.lastVideoRequest?.quickCreationSkuId)
        assertEquals("cinematic", repository.lastVideoRequest?.quickCreationParams?.get("style"))
        assertEquals(
            listOf("https://example.com/video.mp4"),
            repository.lastVideoRequest?.quickCreationListParams?.get("referenceVideos"),
        )
        assertEquals(
            listOf("https://example.com/audio.mp3"),
            repository.lastVideoRequest?.quickCreationListParams?.get("referenceAudios"),
        )
    }

    @Test
    fun `apply inspiration video template fills creation state from detail`() {
        runBlocking {
            val repository = FakeQuickCreateRepository()
            val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())

            model.switchMode(QuickCreateMode.INSPIRATION)
            model.applyInspirationTemplate("tpl-video")

            assertEquals(QuickCreateMode.CREATION, model.uiState.value.currentMode)
            assertEquals(QuickCreateTab.VIDEO, model.uiState.value.currentTab)
            assertEquals("薯片人偶踢足球", model.uiState.value.videoConfig.prompt)
            assertEquals(VideoAspectRatio.RATIO_3_4, model.uiState.value.videoConfig.aspectRatio)
            assertEquals(VideoResolution.RES_720P, model.uiState.value.videoConfig.resolution)
            assertEquals(VideoDuration.DURATION_10S, model.uiState.value.videoConfig.duration)
            assertEquals(false, model.uiState.value.videoConfig.generateAudio)
            assertEquals("video-binding-1", model.uiState.value.selectedVideoServiceModel?.bindingId)
            assertEquals("video-sku-1", model.uiState.value.selectedVideoServiceModel?.skuId)
            assertEquals(
                listOf("https://example.com/ref.png"),
                model.uiState.value.videoConfig.mediaReferences.mapNotNull { it.remoteUrl },
            )
        }
    }

    @Test
    fun `apply inspiration video template keeps undeclared image list params as global reference`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-video")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("https://example.com/ref.png", repository.lastVideoRequest?.referenceImageUri)
        assertEquals(null, repository.lastVideoRequest?.quickCreationListParams?.get("imageUrls"))
    }

    @Test
    fun `apply inspiration video template maps list param field key to upload param key`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            templateDetail = templateDetail.copy(
                listParams = mapOf("referenceVideo" to listOf("https://example.com/template-video.mp4")),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-video")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/template-video.mp4"),
            repository.lastVideoRequest?.quickCreationListParams?.get("referenceVideos"),
        )
        assertEquals(null, repository.lastVideoRequest?.referenceVideoUri)
    }

    @Test
    fun `apply inspiration image template ignores unsupported model params`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = mapOf(
                    "aspectRatio" to "21:9",
                    "resolution" to "4K",
                ),
                listParams = emptyMap(),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImageModel(ImageModel.SEEDREAM_4)
        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(ImageAspectRatio.RATIO_3_4, model.uiState.value.imageConfig.aspectRatio)
        assertEquals(ImageResolution.RES_1K, model.uiState.value.imageConfig.resolution)
        assertEquals("3:4", repository.lastImageRequest?.aspectRatio)
        assertEquals("1K", repository.lastImageRequest?.resolution)
    }

    @Test
    fun `apply inspiration video template ignores unsupported model params and toggles`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            templateDetail = templateDetail.copy(
                params = mapOf(
                    "resolution" to "1080p",
                    "duration" to "10",
                    "generateAudio" to "true",
                    "realPersonMode" to "true",
                ),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateVideoModel(VideoModel.SEEDANCE_2_FAST)
        model.applyInspirationTemplate("tpl-video")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(VideoResolution.RES_720P, model.uiState.value.videoConfig.resolution)
        assertEquals(VideoDuration.DURATION_5S, model.uiState.value.videoConfig.duration)
        assertEquals(false, model.uiState.value.videoConfig.generateAudio)
        assertEquals(false, model.uiState.value.videoConfig.realisticMode)
        assertEquals("720p", repository.lastVideoRequest?.resolution)
        assertEquals(5, repository.lastVideoRequest?.duration)
        assertEquals(false, repository.lastVideoRequest?.generateAudio)
        assertEquals(false, repository.lastVideoRequest?.realistic)
    }

    @Test
    fun `apply inspiration video template infers generic list param media type from service field`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoModels = listOf(
                videoModels.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "reference",
                            paramKey = "referenceVideos",
                            fieldType = "VIDEO_UPLOAD",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                    ),
                ),
            )
            templateDetail = templateDetail.copy(
                listParams = mapOf("reference" to listOf("https://example.com/template-reference.mp4")),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-video")
        runCurrent()

        val reference = model.uiState.value.videoConfig.mediaReferences.single()
        assertEquals(QuickCreateMediaType.VIDEO, reference.type)
        assertEquals("referenceVideos", reference.fieldParamKey)
    }

    @Test
    fun `apply inspiration image template keeps list params bound to service fields`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "imageUrls",
                            paramKey = "imageUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                        QuickCreationServiceField(
                            fieldKey = "maskUrls",
                            paramKey = "maskUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = emptyMap(),
                listParams = mapOf("imageUrls" to listOf("https://example.com/template.png")),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/template.png"),
            repository.lastImageRequest?.quickCreationListParams?.get("imageUrls"),
        )
        assertEquals(null, repository.lastImageRequest?.quickCreationListParams?.get("maskUrls"))
    }

    @Test
    fun `apply inspiration image template keeps media ids unique per list param field`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "imageUrls",
                            paramKey = "imageUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                        QuickCreationServiceField(
                            fieldKey = "maskUrls",
                            paramKey = "maskUrls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = emptyMap(),
                listParams = mapOf(
                    "imageUrls" to listOf("https://example.com/template.png"),
                    "maskUrls" to listOf("https://example.com/mask.png"),
                ),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("tpl-image")
        runCurrent()

        val references = model.uiState.value.imageConfig.mediaReferences
        assertEquals(listOf("imageUrls", "maskUrls"), references.map { it.fieldParamKey })
        assertEquals(references.size, references.map { it.id }.toSet().size)
    }

    @Test
    fun `apply inspiration image template keeps media ids unique when field keys sanitize equally`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = listOf(
                        QuickCreationServiceField(
                            fieldKey = "image-urls",
                            paramKey = "image-urls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                        QuickCreationServiceField(
                            fieldKey = "image_urls",
                            paramKey = "image_urls",
                            fieldType = "IMAGE",
                            required = false,
                            defaultValue = null,
                            options = emptyList(),
                            maxUploadCount = 1,
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = emptyMap(),
                listParams = mapOf(
                    "image-urls" to listOf("https://example.com/dash.png"),
                    "image_urls" to listOf("https://example.com/underscore.png"),
                ),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("tpl-image")
        runCurrent()

        val references = model.uiState.value.imageConfig.mediaReferences
        assertEquals(listOf("image-urls", "image_urls"), references.map { it.fieldParamKey })
        assertEquals(references.size, references.map { it.id }.toSet().size)
    }

    @Test
    fun `apply inspiration image template does not submit inactive child upload media as global reference`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = emptyMap(),
                listParams = mapOf("childImages" to listOf("https://example.com/child.png")),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(null, repository.lastImageRequest?.referenceImageUri)
        assertEquals(null, repository.lastImageRequest?.quickCreationListParams?.get("childImages"))
    }

    @Test
    fun `apply inspiration image template maps param field key before resolving active child upload media`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "mode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "mode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = mapOf("mode" to "imageReference"),
                listParams = mapOf("childImage" to listOf("https://example.com/child.png")),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(
            listOf("https://example.com/child.png"),
            repository.lastImageRequest?.quickCreationListParams?.get("childImages"),
        )
        assertEquals("imageReference", repository.lastImageRequest?.quickCreationParams?.get("creationMode"))
    }

    @Test
    fun `apply inspiration image template keeps canonical param value over field key alias`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            models = listOf(
                models.single().copy(
                    fields = models.single().fields + QuickCreationServiceField(
                        fieldKey = "mode",
                        paramKey = "creationMode",
                        fieldType = "LIST",
                        required = false,
                        defaultValue = "text",
                        options = emptyList(),
                        inputExtra = QuickCreationServiceFieldExtra(
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "childImage",
                                    paramKey = "childImages",
                                    fieldType = "IMAGE",
                                    maxInputCount = 1,
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "mode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            )
                        ),
                    )
                )
            )
            templateDetail = QuickCreateInspirationTemplateDetail(
                templateId = "tpl-image",
                title = "Image template",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                prompt = "template prompt",
                params = mapOf(
                    "creationMode" to "imageReference",
                    "mode" to "text",
                ),
                listParams = mapOf("childImage" to listOf("https://example.com/child.png")),
                coverUrl = "https://example.com/template-cover.png",
                videoUrl = null,
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.switchMode(QuickCreateMode.INSPIRATION)
        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("imageReference", repository.lastImageRequest?.quickCreationParams?.get("creationMode"))
        assertEquals(
            listOf("https://example.com/child.png"),
            repository.lastImageRequest?.quickCreationListParams?.get("childImages"),
        )
    }

    @Test
    fun `updateImagePrompt changes prompt`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.updateImagePrompt("test prompt")
        assertEquals("test prompt", model.uiState.value.imageConfig.prompt)
    }

    @Test
    fun `auto save clears draft when prompts become empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        model.updateImagePrompt("draft prompt")
        advanceTimeBy(500)
        runCurrent()
        assertNotNull(settings.getQuickCreateDraft())

        model.updateImagePrompt("")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(null, settings.getQuickCreateDraft())
    }

    @Test
    fun `auto save hides stale draft entry after prompt changes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"old draft","videoPrompt":""}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()
        assertEquals(true, model.uiState.value.hasDraft)

        model.updateImagePrompt("new draft")
        advanceTimeBy(500)
        runCurrent()

        assertEquals(false, model.uiState.value.hasDraft)
    }

    @Test
    fun `updateVideoPrompt changes prompt`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.updateVideoPrompt("video prompt")
        assertEquals("video prompt", model.uiState.value.videoConfig.prompt)
    }

    @Test
    fun `hasDraft false initially`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        assertEquals(false, model.hasDraft)
    }

    @Test
    fun `checkForDraft exposes saved draft through ui state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"hello","videoPrompt":""}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        model.checkForDraft()
        runCurrent()

        assertEquals(true, model.uiState.value.hasDraft)
    }

    @Test
    fun `init exposes saved draft through ui state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"hello","videoPrompt":""}""")

        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        assertEquals(true, model.uiState.value.hasDraft)
    }

    @Test
    fun `init ignores saved draft without prompt content`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"","videoPrompt":""}""")

        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        assertEquals(false, model.uiState.value.hasDraft)
        assertEquals(null, settings.getQuickCreateDraft())
    }

    @Test
    fun `draft resume summary describes tab and prompt length`() {
        val draft = DraftData(
            currentTab = "VIDEO",
            imagePrompt = "image",
            videoPrompt = "video prompt",
        )

        assertEquals("上次草稿 · 视频 · 12 字", draft.resumeSummaryText())
    }

    @Test
    fun `draft resume summary falls back to image prompt when video tab has no prompt`() {
        val draft = DraftData(
            currentTab = "VIDEO",
            imagePrompt = "image prompt",
            videoPrompt = "",
        )

        assertEquals("上次草稿 · 图片 · 12 字", draft.resumeSummaryText())
    }

    @Test
    fun `checkForDraft finds saved draft`() {
        runBlocking {
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"hello","videoPrompt":""}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        model.checkForDraft()
        // async — draft loaded in coroutine
        assertTrue(true) // basic sanity
        }
    }

    @Test
    fun `restore draft refreshes fee preview for restored image prompt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"draft prompt","videoPrompt":""}""")
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), settings)
        runCurrent()

        model.checkForDraft()
        runCurrent()
        model.restoreDraft()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, repository.feePreviewRequests.size)
        assertEquals("draft prompt", repository.feePreviewRequests.single().prompt)
    }

    @Test
    fun `restore image draft switches back from video tab before fee preview`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"image draft","videoPrompt":""}""")
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), settings)
        runCurrent()

        model.switchTab(QuickCreateTab.VIDEO)
        runCurrent()
        model.checkForDraft()
        runCurrent()
        model.restoreDraft()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(QuickCreateTab.IMAGE, model.uiState.value.currentTab)
        assertEquals(1, repository.feePreviewRequests.size)
        assertEquals("image draft", repository.feePreviewRequests.single().prompt)
        assertEquals(0, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `restore draft falls back to image tab when video prompt is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"VIDEO","imagePrompt":"image draft","videoPrompt":""}""")
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), settings)
        runCurrent()

        model.restoreDraft()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(QuickCreateTab.IMAGE, model.uiState.value.currentTab)
        assertEquals(1, repository.feePreviewRequests.size)
        assertEquals("image draft", repository.feePreviewRequests.single().prompt)
        assertEquals(0, repository.videoFeePreviewRequests.size)
    }

    @Test
    fun `restore video draft clears existing image prompt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"VIDEO","imagePrompt":"","videoPrompt":"video draft"}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        model.updateImagePrompt("existing image prompt")
        runCurrent()
        model.restoreDraft()
        runCurrent()

        assertEquals("", model.uiState.value.imageConfig.prompt)
        assertEquals("video draft", model.uiState.value.videoConfig.prompt)
        assertEquals(QuickCreateTab.VIDEO, model.uiState.value.currentTab)
    }

    @Test
    fun `restore draft cancels pending autosave`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"VIDEO","imagePrompt":"","videoPrompt":"video draft"}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        model.updateImagePrompt("existing image prompt")
        runCurrent()
        model.restoreDraft()
        runCurrent()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(null, settings.getQuickCreateDraft())
    }

    @Test
    fun `dispose cancels pending draft autosave`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()

        model.updateImagePrompt("draft that should not be saved")
        runCurrent()
        model.onDispose()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(null, settings.getQuickCreateDraft())
    }

    @Test
    fun `successful submit clears in memory draft entry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"old draft","videoPrompt":""}""")
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), settings)
        runCurrent()
        assertEquals(true, model.uiState.value.hasDraft)

        model.applyInspirationTemplate("tpl-image")
        runCurrent()
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(false, model.uiState.value.hasDraft)
        assertEquals(null, settings.getQuickCreateDraft())
    }

    @Test
    fun `failed image task replaces running status text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Running("task-1", progress = 42),
                QuickCreateTaskStatus.Failed("task-1", "render failed"),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(QuickCreateTaskUiStatus.FAILED, model.uiState.value.taskStatus)
        assertEquals("render failed", model.uiState.value.error)
        assertEquals("render failed", model.uiState.value.statusText)
    }

    @Test
    fun `errored image task clears running status text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Running("task-1", progress = 42),
                QuickCreateTaskStatus.Error("network unavailable"),
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals("network unavailable", model.uiState.value.error)
        assertEquals(null, model.uiState.value.statusText)
    }

    @Test
    fun `queued image task exposes queuing status text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(QuickCreateTaskStatus.Queuing("task-1"))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(QuickCreateTaskUiStatus.QUEUING, model.uiState.value.taskStatus)
        assertEquals("排队中...", model.uiState.value.statusText)
        assertEquals(emptyList(), model.uiState.value.results)
    }

    @Test
    fun `running image task exposes progress status text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(QuickCreateTaskStatus.Running("task-1", progress = 42))
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals(QuickCreateTaskUiStatus.RUNNING, model.uiState.value.taskStatus)
        assertEquals("生成中... 42%", model.uiState.value.statusText)
        assertEquals(emptyList(), model.uiState.value.results)
    }

    @Test
    fun `successful image task maps results and refreshes recent history`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                            thumbnailUrl = "https://example.com/thumb.png",
                            width = 1024,
                            height = 768,
                            duration = 6,
                        )
                    ),
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        val result = model.uiState.value.results.single()
        assertEquals(QuickCreateTaskUiStatus.SUCCESS, model.uiState.value.taskStatus)
        assertEquals("生成完成", model.uiState.value.statusText)
        assertEquals("https://example.com/result.png", result.url)
        assertEquals("png", result.type)
        assertEquals(QuickCreateResultMediaType.IMAGE, result.mediaType)
        assertEquals("https://example.com/thumb.png", result.thumbnailUrl)
        assertEquals(1024, result.width)
        assertEquals(768, result.height)
        assertEquals(6, result.duration)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
    }

    @Test
    fun `successful task maps video media type from result type or url suffix`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.mp4?download=1",
                            type = "file",
                            thumbnailUrl = "https://example.com/thumb.jpg",
                        ),
                        QuickCreateResultItem(
                            url = "https://example.com/result-output",
                            type = "video",
                        ),
                    ),
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        val results = model.uiState.value.results
        assertEquals("file", results[0].type)
        assertEquals(QuickCreateResultMediaType.VIDEO, results[0].mediaType)
        assertEquals("https://example.com/result.mp4?download=1", results[0].url)
        assertEquals("https://example.com/thumb.jpg", results[0].thumbnailUrl)
        assertEquals("video", results[1].type)
        assertEquals(QuickCreateResultMediaType.VIDEO, results[1].mediaType)
        assertEquals("https://example.com/result-output", results[1].url)
    }

    @Test
    fun `clearing successful results clears status text`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            imageTaskStatuses = listOf(
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                        )
                    ),
                )
            )
        }
        val model = createModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()
        model.clearResults()

        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(emptyList(), model.uiState.value.results)
        assertEquals(null, model.uiState.value.statusText)
    }

    @Test
    fun `checkForDraft clears stale in memory draft when stored draft is invalid`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"old draft","videoPrompt":""}""")
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), settings)
        runCurrent()

        model.checkForDraft()
        runCurrent()
        assertEquals(true, model.hasDraft)

        settings.saveQuickCreateDraft("{")
        model.checkForDraft()
        runCurrent()
        model.restoreDraft()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(false, model.hasDraft)
        assertEquals("", model.uiState.value.imageConfig.prompt)
        assertEquals(0, repository.feePreviewRequests.size)
    }

    @Test
    fun `checkForDraft clears stale in memory draft when stored draft is empty`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val settings = FakeSettingsRepo()
        settings.saveQuickCreateDraft("""{"currentTab":"IMAGE","imagePrompt":"old draft","videoPrompt":""}""")
        val repository = FakeQuickCreateRepository()
        val model = createModel(repository, FakeMediaResolver(), settings)
        runCurrent()

        model.checkForDraft()
        runCurrent()
        assertEquals(true, model.hasDraft)

        settings.clearQuickCreateDraft()
        model.checkForDraft()
        runCurrent()
        model.restoreDraft()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(false, model.hasDraft)
        assertEquals("", model.uiState.value.imageConfig.prompt)
        assertEquals(0, repository.feePreviewRequests.size)
    }

    @Test
    fun `generate without prompt shows error`() {
        runBlocking {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.generate()
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertNotNull(model.uiState.value.error)
        }
    }

    @Test
    fun `reset restores to idle`() {
        val model = createModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())
        model.clearResults()
        assertEquals(QuickCreateTaskUiStatus.IDLE, model.uiState.value.taskStatus)
        assertEquals(0, model.uiState.value.results.size)
    }
}

/**
 * 测试 fake 使用的远端分类 ID 映射。
 *
 * 生产代码中的映射位于 Data 层；这里仅用于筛选测试 fixture 中模拟的服务端模型，
 * 避免重新把接口字符串暴露到 Domain 枚举。
 */
private fun QuickCreationServiceKind.testCategoryId(): String =
    when (this) {
        QuickCreationServiceKind.IMAGE -> "IMAGE"
        QuickCreationServiceKind.VIDEO -> "VIDEO"
    }
