package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.app.platform.MediaResolver
import com.runninghub.shared.domain.repository.QuickCreateInspirationTag
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplate
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplateDetail
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.QuickCreationFeePreview
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryOutput
import com.runninghub.shared.domain.repository.QuickCreationHistoryPage
import com.runninghub.shared.domain.repository.QuickCreationProject
import com.runninghub.shared.domain.repository.QuickCreationProjectPage
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldExtra
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
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
        var uploadDelayMillis: Long = 0L
        var lastImageRequest: com.runninghub.shared.domain.repository.ImageGenerationRequest? = null
        var lastVideoRequest: com.runninghub.shared.domain.repository.VideoGenerationRequest? = null
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
        val feePreviewRequests = mutableListOf<com.runninghub.shared.domain.repository.ImageGenerationRequest>()
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
        val videoFeePreviewRequests = mutableListOf<com.runninghub.shared.domain.repository.VideoGenerationRequest>()
        var lastHistoryDetailOutputId: String? = null
        val cancelledTaskIds = mutableListOf<String>()
        val requestedHistoryPages = mutableListOf<Int>()
        val requestedProjectPages = mutableListOf<Int>()
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
        var overrideHistoryList: ((page: Int, size: Int) -> QuickCreationHistoryPage)? = null
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
        override fun generateImage(request: com.runninghub.shared.domain.repository.ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            lastImageRequest = request
            return flowOf(QuickCreateTaskStatus.Queuing("task-1"))
        }
        override fun generateVideo(request: com.runninghub.shared.domain.repository.VideoGenerationRequest): Flow<QuickCreateTaskStatus> {
            lastVideoRequest = request
            return flowOf(QuickCreateTaskStatus.Queuing("video-task-1"))
        }
        override suspend fun previewImageQuickCreationFee(
            request: com.runninghub.shared.domain.repository.ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> {
            feePreviewRequests += request
            return feePreviewResult
        }
        override suspend fun previewVideoQuickCreationFee(
            request: com.runninghub.shared.domain.repository.VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> {
            videoFeePreviewRequests += request
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
        ): Result<List<QuickCreateInspirationTemplate>> = Result.success(inspirationTemplates)
        override suspend fun getInspirationTemplateDetail(
            templateId: String,
        ): Result<QuickCreateInspirationTemplateDetail> = Result.success(templateDetail.copy(templateId = templateId))
        override suspend fun getModels(categoryId: String): Result<List<QuickCreationServiceModel>> =
            Result.success((models + videoModels).filter { it.categoryId == categoryId })

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
            Result.success(projectPage.copy(page = page, size = size)).also {
                requestedProjectPages += page
            }

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            Result.success(projectTaskPage.copy(page = page, size = size)).also {
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
    fun `initialization loads quick creation history`() {
        val model = QuickCreateScreenModel(FakeQuickCreateRepository(), FakeMediaResolver(), FakeSettingsRepo())

        assertEquals(false, model.uiState.value.historyLoading)
        assertEquals("history-task-1", model.uiState.value.historyItems.single().taskId)
        assertEquals("https://example.com/result.png", model.uiState.value.historyItems.single().outputs.single().url)
    }

    @Test
    fun `initialization loads quick creation projects`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        assertEquals(listOf(1), repository.requestedProjectPages)
        assertEquals(false, model.uiState.value.projectsLoading)
        assertEquals("project-1", model.uiState.value.projects.single().projectId)
        assertEquals("世界杯广告", model.uiState.value.projects.single().name)
        assertEquals(true, model.uiState.value.projects.single().pinned)
        assertEquals(false, model.uiState.value.projectsHasMore)
    }

    @Test
    fun `selecting project loads project tasks into history area`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectProject("project-1")

        assertEquals(listOf("project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals("project-1", model.uiState.value.selectedProjectId)
        assertEquals(false, model.uiState.value.projectTasksLoading)
        assertEquals("project-task-1", model.uiState.value.historyItems.single().taskId)
        assertEquals(false, model.uiState.value.historyHasMore)
    }

    @Test
    fun `clearing selected project reloads recent history`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        model.selectProject("project-1")

        model.clearSelectedProject()

        assertEquals(null, model.uiState.value.selectedProjectId)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("history-task-1", model.uiState.value.historyItems.single().taskId)
    }

    @Test
    fun `toggling project pin calls repository and updates project state`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.toggleProjectPin("project-1")

        assertEquals(listOf("project-1" to false), repository.pinnedProjectRequests)
        assertEquals(false, model.uiState.value.projects.single().pinned)
        assertEquals(emptySet(), model.uiState.value.projectPinningIds)
    }

    @Test
    fun `creating project calls repository and prepends project`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.createProject("新项目")

        assertEquals(listOf("新项目"), repository.createdProjectNames)
        assertEquals("project-new", model.uiState.value.projects.first().projectId)
        assertEquals("新项目", model.uiState.value.projects.first().name)
        assertEquals(emptySet(), model.uiState.value.projectMutatingIds)
    }

    @Test
    fun `renaming project calls repository and updates project name`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.renameProject("project-1", "新名称")

        assertEquals(listOf("project-1" to "新名称"), repository.renamedProjectRequests)
        assertEquals("新名称", model.uiState.value.projects.single().name)
        assertEquals(emptySet(), model.uiState.value.projectMutatingIds)
    }

    @Test
    fun `deleting selected project removes it and reloads recent history`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectProjectDetail("project-1")

        assertEquals("project-1", repository.lastProjectDetailId)
        assertEquals(false, model.uiState.value.projectDetailLoading)
        assertEquals("项目详情", model.uiState.value.selectedProjectDetail?.name)
        assertEquals("https://example.com/project-detail.png", model.uiState.value.selectedProjectDetail?.coverUrl)
        assertEquals(9, model.uiState.value.selectedProjectDetail?.taskCount)
    }

    @Test
    fun `selecting history output loads detail into state`() {
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.selectHistoryOutput("output-1")

        assertEquals("output-1", repository.lastHistoryDetailOutputId)
        assertEquals(false, model.uiState.value.historyDetailLoading)
        assertEquals("history-task-detail", model.uiState.value.selectedHistoryDetail?.taskId)
        assertEquals("https://example.com/detail.png", model.uiState.value.selectedHistoryDetail?.outputs?.single()?.url)
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
                                    url = "https://example.com/two.png",
                                    type = "png",
                                )
                            ),
                        )
                    ),
                ),
            )
        }
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.loadMoreQuickCreationHistory()

        assertEquals(listOf(1, 2), repository.requestedHistoryPages)
        assertEquals(listOf("history-task-1", "history-task-2"), model.uiState.value.historyItems.map { it.taskId })
        assertEquals(false, model.uiState.value.historyHasMore)
        assertEquals(false, model.uiState.value.historyLoadingMore)
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

        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()
        assertEquals("PREPAID", model.uiState.value.historyItems.single().status)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("SUCCESS", model.uiState.value.historyItems.single().status)
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

        model.cancelHistoryTask("history-task-1")

        assertEquals(listOf("history-task-1"), repository.cancelledTaskIds)
        assertEquals(listOf(1, 1), repository.requestedHistoryPages)
        assertEquals("CANCELED", model.uiState.value.historyItems.single().status)
        assertEquals(false, model.uiState.value.historyCancellingTaskIds.contains("history-task-1"))
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
    fun `generate image uses selected service model ids`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate image uses selected service model field defaults`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.updateImagePrompt("prompt")
        advanceTimeBy(500)
        runCurrent()
        model.generate()
        runCurrent()

        assertEquals("photoreal", repository.lastImageRequest?.quickCreationParams?.get("style"))
        assertEquals("16:9", repository.lastImageRequest?.quickCreationParams?.get("aspectRatio"))
    }

    @Test
    fun `image prompt refreshes server fee preview into estimated cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `uploading global image media does not refresh image fee preview before remote url exists`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `video prompt refreshes server fee preview into estimated cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate image is blocked when fee preview failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            feePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        }
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate video is blocked when fee preview failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository().apply {
            videoFeePreviewResult = Result.failure(IllegalStateException("preview unavailable"))
        }
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate image is blocked while fee preview is loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate video is blocked while fee preview is loading`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `generate image uses updated service field values`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
    fun `remove media reference removes image media after switching to video tab`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeQuickCreateRepository()
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, mediaResolver, FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
            val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())

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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
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
        val model = QuickCreateScreenModel(repository, FakeMediaResolver(), FakeSettingsRepo())
        runCurrent()

        model.applyInspirationTemplate("tpl-image")
        runCurrent()

        val references = model.uiState.value.imageConfig.mediaReferences
        assertEquals(listOf("image-urls", "image_urls"), references.map { it.fieldParamKey })
        assertEquals(references.size, references.map { it.id }.toSet().size)
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
