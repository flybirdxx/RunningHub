package com.runninghub.app.ui.feature.create

import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class CreateScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createScreenModel(
        repository: FakeQuickCreateRepository,
        ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): CreateScreenModel =
        CreateScreenModel(
            historyRepository = repository,
            mediaResolver = FakeMediaResolver(),
            ioDispatcher = ioDispatcher,
            modelCatalogRepository = repository,
            generationRepository = repository,
            feePreviewRepository = repository,
            mediaUploadRepository = repository,
        )

    @Test
    fun `loadModels selects real quick creation model and recent history`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()

        val state = screenModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals("Real Image Model", state.selectedModel?.name)
        assertEquals("16:9", state.fieldValues["aspectRatio"])
        assertEquals("medium", state.fieldValues["quality"])
        assertEquals("history-1", state.recentHistory.single().taskId)
        assertEquals(listOf("IMAGE"), repository.modelCategories)
    }

    @Test
    fun `history load failure exposes retryable error and retry restores real history`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            historyFailureMessage = "history failed"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()

        assertEquals(false, screenModel.uiState.value.historyLoading)
        assertEquals("历史记录加载失败", screenModel.uiState.value.historyError)
        assertEquals(emptyList(), screenModel.uiState.value.recentHistory)

        repository.historyFailureMessage = null
        screenModel.refreshRecentHistory()
        advanceUntilIdle()

        assertEquals(null, screenModel.uiState.value.historyError)
        assertEquals("history-1", screenModel.uiState.value.recentHistory.single().taskId)
        assertEquals(2, repository.historyListCalls)
    }

    @Test
    fun `history filter narrows list by media type and status`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            customHistoryItems = listOf(
                historyItem(taskId = "image-success", categoryId = "IMAGE", status = "SUCCESS", outputType = "png"),
                historyItem(taskId = "video-success", categoryId = "VIDEO", status = "SUCCESS", outputType = "mp4"),
                historyItem(taskId = "image-failed", categoryId = "IMAGE", status = "FAILED", outputType = "png"),
            )
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()

        assertEquals(listOf("image-success", "video-success", "image-failed"), screenModel.uiState.value.filteredHistory.map { it.taskId })

        screenModel.updateHistoryFilter(CreateHistoryFilter.IMAGE)
        assertEquals(listOf("image-success", "image-failed"), screenModel.uiState.value.filteredHistory.map { it.taskId })

        screenModel.updateHistoryFilter(CreateHistoryFilter.VIDEO)
        assertEquals(listOf("video-success"), screenModel.uiState.value.filteredHistory.map { it.taskId })

        screenModel.updateHistoryFilter(CreateHistoryFilter.SUCCESS)
        assertEquals(listOf("image-success", "video-success"), screenModel.uiState.value.filteredHistory.map { it.taskId })
    }

    @Test
    fun `history filter matches running statuses without starting polling`() {
        val items = listOf(
            historyItem(taskId = "queued", status = "QUEUED"),
            historyItem(taskId = "running", status = "RUNNING"),
            historyItem(taskId = "success", status = "SUCCESS"),
        )

        assertEquals(
            listOf("queued", "running"),
            items.filterByCreateHistoryFilter(CreateHistoryFilter.RUNNING).map { it.taskId },
        )
    }

    @Test
    fun `non quick creation category shows explicit unavailable notice`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.updateCategory(CreateCategory.AUDIO)
        advanceUntilIdle()

        val state = screenModel.uiState.value
        assertEquals(CreateCategory.AUDIO, state.selectedCategory)
        assertEquals(emptyList(), state.serviceModels)
        assertEquals("音频暂未接入快捷创作模型", state.catalogNotice)
        assertEquals(emptyList(), repository.modelCategories)
    }

    @Test
    fun `model load token invalid shows login catalog message without fallback models`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            modelFailureMessage = "TOKEN_INVALID Bearer secret-token"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()

        val state = screenModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(emptyList(), state.serviceModels)
        assertEquals(null, state.selectedModel)
        assertEquals("登录后可同步真实快捷创作模型目录", state.error)
    }

    @Test
    fun `submit blocks missing required prompt before generation`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.submitSelectedModel()

        assertEquals(0, repository.generateImageCalls)
        assertEquals("prompt 不能为空", screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `submit blocks when fee preview has not been confirmed`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        screenModel.submitSelectedModel()

        assertEquals(0, repository.generateImageCalls)
        assertEquals("价格待确认，暂不能生成", screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `submit blocks when fee preview fails`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            previewFailureMessage = "fee-preview failed"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        advanceTimeBy(501)
        advanceUntilIdle()
        screenModel.submitSelectedModel()

        assertEquals(0, repository.generateImageCalls)
        assertEquals("fee-preview failed", screenModel.uiState.value.feePreviewError)
        assertEquals("价格待确认，暂不能生成", screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `fee preview token invalid shows login price message without leaking token text`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            previewFailureMessage = "TOKEN_INVALID Bearer secret-token cookie=session"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        advanceTimeBy(501)
        advanceUntilIdle()

        assertEquals("登录后可确认价格", screenModel.uiState.value.feePreviewError)
        assertEquals(false, screenModel.uiState.value.feePreviewError.orEmpty().contains("secret-token"))
        assertEquals(false, screenModel.uiState.value.feePreviewError.orEmpty().contains("session"))
    }

    @Test
    fun `submit blocks when fee preview reports insufficient balance`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            feePreviewPassed = false
            feePreviewInsufficientType = "CASH"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        advanceTimeBy(501)
        advanceUntilIdle()
        screenModel.submitSelectedModel()

        assertEquals(0, repository.generateImageCalls)
        assertEquals("余额不足或价格预览未通过", screenModel.uiState.value.feePreviewError)
        assertEquals("余额不足或价格预览未通过", screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `prompt change refreshes fee preview with selected service model ids`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        screenModel.updateFieldValue("quality", "high")
        advanceTimeBy(501)
        advanceUntilIdle()

        val request = assertNotNull(repository.lastPreviewImageRequest)
        assertEquals("binding-1", request.quickCreationBindingId)
        assertEquals("sku-1", request.quickCreationSkuId)
        assertEquals("IMAGE", request.quickCreationCategoryId)
        assertEquals("binding-1:sku-1", request.model)
        assertEquals("green icon", request.prompt)
        assertEquals("high", request.quickCreationParams["quality"])
        assertEquals(0.76, screenModel.uiState.value.feePreview?.requiredCashAmount)
    }

    @Test
    fun `zero upload limits on scalar fields do not make them upload fields`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            promptMaxInputCount = 0
            scalarMaxUploadCount = 0
            scalarAcceptFormats = listOf("")
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        advanceTimeBy(501)
        advanceUntilIdle()

        assertEquals("green icon", repository.lastPreviewImageRequest?.prompt)
        assertEquals(null, screenModel.uiState.value.feePreviewError)
        assertEquals(null, screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `upload field uploads media and writes remote url into fee preview params`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            includeUploadField = true
        }
        val screenModel = createScreenModel(repository, UnconfinedTestDispatcher(testScheduler))

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        screenModel.pickUploadField("referenceImage", "content://local/image.png")
        advanceUntilIdle()
        advanceTimeBy(501)
        advanceUntilIdle()

        assertEquals(1, repository.uploadMediaCalls)
        assertEquals("image.png", repository.lastUploadFileName)
        assertEquals("image/png", repository.lastUploadMimeType)
        assertEquals("https://example.com/uploaded.png", screenModel.uiState.value.fieldValues["referenceImage"])
        assertEquals(
            listOf("https://example.com/uploaded.png"),
            repository.lastPreviewImageRequest?.quickCreationListParams?.get("imageUrls"),
        )
    }

    @Test
    fun `upload field failure does not expose repository exception message`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            includeUploadField = true
            uploadFailureMessage = "TOKEN_INVALID msg=internal render path Bearer secret-token"
        }
        val screenModel = createScreenModel(repository, UnconfinedTestDispatcher(testScheduler))

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.pickUploadField("referenceImage", "content://local/image.png")
        advanceUntilIdle()

        val uploadState = assertNotNull(screenModel.uiState.value.uploadFieldStates["referenceImage"])
        assertEquals(true, uploadState.isError)
        assertEquals("登录后可上传素材", uploadState.errorMessage)
        assertEquals("登录后可上传素材", screenModel.uiState.value.feePreviewError)
    }

    @Test
    fun `conditional input child is submitted only when visible condition matches`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            includeConditionalChildField = true
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        screenModel.updateFieldValue("creationMode", "text")
        screenModel.updateFieldValue("referenceStrength", "0.85")
        advanceTimeBy(501)
        advanceUntilIdle()

        assertEquals(null, repository.lastPreviewImageRequest?.quickCreationParams?.get("referenceStrength"))

        screenModel.updateFieldValue("creationMode", "imageReference")
        advanceTimeBy(501)
        advanceUntilIdle()

        assertEquals("0.85", repository.lastPreviewImageRequest?.quickCreationParams?.get("referenceStrength"))
    }

    @Test
    fun `submit after fee preview calls generate and refreshes history`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.updateFieldValue("prompt", "green icon")
        screenModel.updateFieldValue("quality", "high")
        advanceTimeBy(501)
        advanceUntilIdle()
        screenModel.submitSelectedModel()
        advanceUntilIdle()

        val request = assertNotNull(repository.lastGenerateImageRequest)
        assertEquals("binding-1", request.quickCreationBindingId)
        assertEquals("sku-1", request.quickCreationSkuId)
        assertEquals("binding-1:sku-1", request.model)
        assertEquals("high", request.quickCreationParams["quality"])
        assertEquals(1, repository.generateImageCalls)
        assertEquals(false, screenModel.uiState.value.isSubmitting)
        assertEquals("生成成功 generated-1", screenModel.uiState.value.submitMessage)
    }

    @Test
    fun `select history output loads quick creation detail`() = runTest {
        val repository = FakeQuickCreateRepository()
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.selectHistoryOutput("output-1")
        advanceUntilIdle()

        assertEquals("output-1", repository.lastDetailOutputId)
        assertEquals("detail-history", screenModel.uiState.value.selectedHistoryDetail?.taskId)

        screenModel.dismissHistoryDetail()

        assertEquals(null, screenModel.uiState.value.selectedHistoryDetail)
        assertEquals(false, screenModel.uiState.value.historyDetailLoading)
    }

    @Test
    fun `history detail failure does not expose repository exception message`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            historyDetailFailureMessage = "DETAIL_CODE_500 msg=remote detail shard internal"
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        advanceUntilIdle()
        screenModel.selectHistoryOutput("output-1")
        advanceUntilIdle()

        assertEquals("历史记录加载失败", screenModel.uiState.value.historyDetailError)
    }

    @Test
    fun `non terminal history refreshes every five seconds until terminal`() = runTest {
        val repository = FakeQuickCreateRepository().apply {
            historyStatuses.clear()
            historyStatuses += listOf("RUNNING", "SUCCESS")
        }
        val screenModel = createScreenModel(repository)

        screenModel.loadModels()
        runCurrent()

        assertEquals(1, repository.historyListCalls)
        assertEquals("RUNNING", screenModel.uiState.value.recentHistory.single().status)

        advanceTimeBy(4_999)
        runCurrent()

        assertEquals(1, repository.historyListCalls)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(2, repository.historyListCalls)
        assertEquals("SUCCESS", screenModel.uiState.value.recentHistory.single().status)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(2, repository.historyListCalls)
    }

    private inner class FakeQuickCreateRepository :
        QuickCreationTaskHistoryRepository,
        QuickCreationModelCatalogRepository,
        QuickCreationGenerationRepository,
        QuickCreationFeePreviewRepository,
        QuickCreationMediaUploadRepository {
        val modelCategories = mutableListOf<String>()
        var lastPreviewImageRequest: ImageGenerationRequest? = null
        var lastGenerateImageRequest: ImageGenerationRequest? = null
        var lastDetailOutputId: String? = null
        var generateImageCalls: Int = 0
        var uploadMediaCalls: Int = 0
        var lastUploadFileName: String? = null
        var lastUploadMimeType: String? = null
        var historyListCalls: Int = 0
        var includeUploadField: Boolean = false
        var includeConditionalChildField: Boolean = false
        var promptMaxInputCount: Int? = null
        var scalarMaxUploadCount: Int? = null
        var scalarAcceptFormats: List<String> = emptyList()
        var modelFailureMessage: String? = null
        var previewFailureMessage: String? = null
        var uploadFailureMessage: String? = null
        var feePreviewPassed: Boolean = true
        var feePreviewInsufficientType: String? = null
        var historyFailureMessage: String? = null
        var historyDetailFailureMessage: String? = null
        var customHistoryItems: List<QuickCreationHistoryItem>? = null
        val historyStatuses = mutableListOf("SUCCESS")

        override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            generateImageCalls += 1
            lastGenerateImageRequest = request
            return flowOf(
                QuickCreateTaskStatus.Submitting,
                QuickCreateTaskStatus.Queuing("generated-1"),
                QuickCreateTaskStatus.Success("generated-1", emptyList()),
            )
        }

        override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> =
            flowOf(QuickCreateTaskStatus.Error("not supported"))

        override suspend fun previewImageQuickCreationFee(request: ImageGenerationRequest): Result<QuickCreationFeePreview> {
            lastPreviewImageRequest = request
            previewFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(
                QuickCreationFeePreview(
                    passed = feePreviewPassed,
                    free = false,
                    requiredCashAmount = 0.76,
                    userCashBalance = 10.0,
                    insufficientType = feePreviewInsufficientType,
                    cashCurrency = "CNY",
                )
            )
        }

        override suspend fun previewVideoQuickCreationFee(request: VideoGenerationRequest): Result<QuickCreationFeePreview> =
            Result.failure(NotImplementedError())

        override suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String> {
            uploadMediaCalls += 1
            lastUploadFileName = fileName
            lastUploadMimeType = mimeType
            uploadFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success("https://example.com/uploaded.png")
        }

        override suspend fun getModels(kind: QuickCreationServiceKind): Result<List<QuickCreationServiceModel>> {
            val categoryId = kind.testCategoryId()
            modelCategories += categoryId
            modelFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(
                listOf(
                    serviceModel(
                        categoryId = categoryId,
                        includeUploadField = includeUploadField,
                        includeConditionalChildField = includeConditionalChildField,
                        promptMaxInputCount = promptMaxInputCount,
                        scalarMaxUploadCount = scalarMaxUploadCount,
                        scalarAcceptFormats = scalarAcceptFormats,
                    )
                )
            )
        }

        override suspend fun listQuickCreationHistory(page: Int, size: Int): Result<QuickCreationHistoryPage> {
            historyFailureMessage?.let {
                historyListCalls += 1
                return Result.failure(IllegalStateException(it))
            }
            val status = historyStatuses.getOrElse(historyListCalls) { historyStatuses.last() }
            historyListCalls += 1
            return Result.success(
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = customHistoryItems?.size ?: 1,
                    items = customHistoryItems ?: listOf(historyItem(taskId = "history-1", status = status)),
                )
            )
        }

        override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> {
            lastDetailOutputId = outputId
            historyDetailFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(
                QuickCreationHistoryItem(
                    taskId = "detail-history",
                    status = "SUCCESS",
                    categoryId = "IMAGE",
                    skuId = "sku-1",
                    taskType = "FAST_WEBAPP_V2",
                    cashAmount = 0.76,
                    cashCurrency = "CNY",
                    params = mapOf("prompt" to "detail prompt"),
                    outputs = listOf(
                        QuickCreationHistoryOutput(
                            outputId = outputId,
                            url = "https://example.com/detail.png",
                            type = "png",
                            width = 1024,
                            height = 1024,
                        )
                    ),
                )
            )
        }

        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            Result.failure(NotImplementedError())
    }

    /**
     * 测试 fake 使用的远端分类 ID 映射。
     *
     * 生产代码中的映射位于 Data 层；测试只需要构造旧创作页依赖的服务模型 fixture，
     * 因此在测试内局部保留字符串，避免反向污染 Domain 枚举。
     */
    private fun QuickCreationServiceKind.testCategoryId(): String =
        when (this) {
            QuickCreationServiceKind.IMAGE -> "IMAGE"
            QuickCreationServiceKind.VIDEO -> "VIDEO"
        }

    private fun historyItem(
        taskId: String,
        categoryId: String = "IMAGE",
        status: String = "SUCCESS",
        outputType: String = "png",
    ): QuickCreationHistoryItem =
        QuickCreationHistoryItem(
            taskId = taskId,
            status = status,
            categoryId = categoryId,
            skuId = "sku-1",
            taskType = "FAST_WEBAPP_V2",
            cashAmount = 0.76,
            cashCurrency = "CNY",
            params = mapOf("prompt" to "green icon"),
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "output-$taskId",
                    url = "https://example.com/$taskId.$outputType",
                    type = outputType,
                    width = 1024,
                    height = 1024,
                )
            ),
        )

    private fun serviceModel(
        categoryId: String,
        includeUploadField: Boolean = false,
        includeConditionalChildField: Boolean = false,
        promptMaxInputCount: Int? = null,
        scalarMaxUploadCount: Int? = null,
        scalarAcceptFormats: List<String> = emptyList(),
    ): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = categoryId,
            groupName = "Real Group",
            bindingId = "binding-1",
            skuId = "sku-1",
            name = "Real Image Model",
            description = "Real service model",
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "prompt",
                    paramKey = "prompt",
                    fieldType = "TEXT",
                    required = true,
                    defaultValue = null,
                    options = emptyList(),
                    inputExtra = QuickCreationServiceFieldExtra(maxInputCount = promptMaxInputCount),
                ),
                QuickCreationServiceField(
                    fieldKey = "aspectRatio",
                    paramKey = "aspectRatio",
                    fieldType = "SELECT",
                    required = true,
                    defaultValue = "16:9",
                    options = listOf(
                        QuickCreationServiceFieldOption("16:9", "16:9"),
                        QuickCreationServiceFieldOption("1:1", "1:1"),
                    ),
                    maxUploadCount = scalarMaxUploadCount,
                    inputExtra = if (scalarAcceptFormats.isEmpty()) {
                        null
                    } else {
                        QuickCreationServiceFieldExtra(acceptFormats = scalarAcceptFormats)
                    },
                ),
                QuickCreationServiceField(
                    fieldKey = "resolution",
                    paramKey = "resolution",
                    fieldType = "SELECT",
                    required = true,
                    defaultValue = "2k",
                    options = listOf(QuickCreationServiceFieldOption("2k", "2k")),
                ),
                QuickCreationServiceField(
                    fieldKey = "quality",
                    paramKey = "quality",
                    fieldType = "SELECT",
                    required = true,
                    defaultValue = null,
                    options = listOf(
                        QuickCreationServiceFieldOption("medium", "medium"),
                        QuickCreationServiceFieldOption("high", "high"),
                    ),
                ),
            ) + if (includeUploadField) {
                listOf(
                    QuickCreationServiceField(
                        fieldKey = "referenceImage",
                        paramKey = "imageUrls",
                        fieldType = "IMAGE_UPLOAD",
                        required = false,
                        defaultValue = null,
                        options = emptyList(),
                        maxUploadCount = 1,
                        maxUploadSize = 10 * 1024 * 1024,
                    )
                )
            } else {
                emptyList()
            } + if (includeConditionalChildField) {
                listOf(
                    QuickCreationServiceField(
                        fieldKey = "creationMode",
                        paramKey = "creationMode",
                        fieldType = "SELECT",
                        required = false,
                        defaultValue = "text",
                        options = listOf(
                            QuickCreationServiceFieldOption("Text", "text"),
                            QuickCreationServiceFieldOption("Image Reference", "imageReference"),
                        ),
                        inputExtra = QuickCreationServiceFieldExtra(
                            title = "Creation mode",
                            inputChildren = listOf(
                                QuickCreationServiceFieldInputChild(
                                    fieldKey = "referenceStrength",
                                    paramKey = "referenceStrength",
                                    fieldType = "NUMBER",
                                    required = false,
                                    defaultValue = "0.65",
                                    title = "Reference strength",
                                    visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                        fieldKey = "creationMode",
                                        values = listOf("imageReference"),
                                    ),
                                )
                            ),
                        ),
                    )
                )
            } else {
                emptyList()
            },
        )

    private class FakeMediaResolver : MediaResolver {
        override fun readBytes(uri: String): ByteArray = byteArrayOf(1, 2, 3)

        override fun getDisplayName(uri: String): String? = "image.png"

        override fun getFileSizeBytes(uri: String): Long = 3
    }
}
