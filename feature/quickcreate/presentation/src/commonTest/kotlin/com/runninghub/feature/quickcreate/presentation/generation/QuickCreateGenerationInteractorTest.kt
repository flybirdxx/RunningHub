package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingPreviewUi
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateFeePreviewInteractor
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateFeeRequestKey
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultMediaType
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskIndicator
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingController
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskStatusText
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.result.quickCreateTaskStatusDisplay
import com.runninghub.feature.quickcreate.presentation.result.toQuickCreateTaskPresentationStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaUploadCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateGenerationInteractorTest {

    @Test
    fun `generate blocks while fee preview is loading without submitting repository request`() = runTest {
        val repository = RecordingGenerationRepository()
        val uiState = MutableStateFlow(
            QuickCreateUiState(
                imageConfig = ImageConfig(prompt = "green icon"),
                feePreviewLoading = true,
            )
        )
        val interactor = createInteractor(
            generationRepository = repository,
            uiState = uiState,
            scope = this,
        )

        interactor.generate()
        runCurrent()

        assertEquals(0, repository.imageGenerateCalls)
        assertEquals(0, repository.videoGenerateCalls)
        assertEquals(QuickCreateTaskUiStatus.IDLE, uiState.value.taskStatus)
        assertEquals("", uiState.value.submittedPrompt)
        assertEquals(QuickCreateRuntimeUiText.FeeConfirming.asQuickCreateUiMessage(), uiState.value.error)
    }

    @Test
    fun `generate stores submitted prompt snapshot and clears editable prompt after submit validation passes`() = runTest {
        val repository = RecordingGenerationRepository()
        val initialState = QuickCreateUiState(
            imageConfig = ImageConfig(prompt = "  green icon  "),
        )
        val request = (
            QuickCreateGenerationRequestFactory()
                .buildCurrentGenerationRequest(initialState, validateUploads = true)
                as QuickCreateGenerationRequestBuildResult.ImageReady
            ).request
        val uiState = MutableStateFlow(
            initialState.copy(feePreviewRequestKey = request.quickCreateFeeRequestKey())
        )
        val interactor = createInteractor(
            generationRepository = repository,
            uiState = uiState,
            scope = this,
        )

        interactor.generate()
        runCurrent()

        assertEquals(1, repository.imageGenerateCalls)
        assertEquals("green icon", uiState.value.submittedPrompt)
        assertEquals("", uiState.value.imageConfig.prompt)
        assertEquals(1, uiState.value.conversationItems.size)
        assertEquals("green icon", uiState.value.conversationItems.single().prompt)
        assertEquals(QuickCreateTaskUiStatus.SUBMITTING, uiState.value.conversationItems.single().taskStatus)
    }

    @Test
    fun `generate opens confirmation sheet for paid request before repository submit`() = runTest {
        val repository = RecordingGenerationRepository()
        val initialState = QuickCreateUiState(
            imageConfig = ImageConfig(prompt = "green icon"),
            estimatedCost = 0.76,
            billingPreview = QuickCreateBillingPreviewUi(
                requiredCashAmount = 0.76,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            ),
        )
        val request = (
            QuickCreateGenerationRequestFactory()
                .buildCurrentGenerationRequest(initialState, validateUploads = true)
                as QuickCreateGenerationRequestBuildResult.ImageReady
            ).request
        val uiState = MutableStateFlow(
            initialState.copy(feePreviewRequestKey = request.quickCreateFeeRequestKey())
        )
        val interactor = createInteractor(
            generationRepository = repository,
            uiState = uiState,
            scope = this,
        )

        interactor.generate()
        runCurrent()

        assertEquals(0, repository.imageGenerateCalls)
        assertEquals(QuickCreateSheet.GENERATION_CONFIRM, uiState.value.activeSheet)
        assertEquals(QuickCreateTaskUiStatus.IDLE, uiState.value.taskStatus)
        assertEquals("", uiState.value.submittedPrompt)
    }

    @Test
    fun `confirm generation submits paid request after confirmation sheet`() = runTest {
        val repository = RecordingGenerationRepository()
        val initialState = QuickCreateUiState(
            imageConfig = ImageConfig(prompt = "green icon"),
            estimatedCost = 0.76,
            billingPreview = QuickCreateBillingPreviewUi(
                requiredCashAmount = 0.76,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            ),
        )
        val request = (
            QuickCreateGenerationRequestFactory()
                .buildCurrentGenerationRequest(initialState, validateUploads = true)
                as QuickCreateGenerationRequestBuildResult.ImageReady
            ).request
        val uiState = MutableStateFlow(
            initialState.copy(feePreviewRequestKey = request.quickCreateFeeRequestKey())
        )
        val interactor = createInteractor(
            generationRepository = repository,
            uiState = uiState,
            scope = this,
        )

        interactor.generate()
        interactor.confirmGeneration()
        runCurrent()

        assertEquals(1, repository.imageGenerateCalls)
        assertEquals(null, uiState.value.activeSheet)
        assertEquals("green icon", uiState.value.submittedPrompt)
    }

    @Test
    fun `generate links parameter snapshot request factory polling and result display`() = runTest {
        val initialState = QuickCreateUiState(
            imageConfig = ImageConfig(
                prompt = "  cinematic product shot  ",
                aspectRatio = ImageAspectRatio.RATIO_1_1,
                resolution = ImageResolution.RES_1K,
            ),
            selectedImageServiceModel = serviceModelWithDefaultImageParams(),
        )
        val request = (
            QuickCreateGenerationRequestFactory()
                .buildCurrentGenerationRequest(initialState, validateUploads = true)
                as QuickCreateGenerationRequestBuildResult.ImageReady
            ).request
        val uiState = MutableStateFlow(
            initialState.copy(feePreviewRequestKey = request.quickCreateFeeRequestKey())
        )
        val repository = RecordingGenerationRepository(
            imageStatuses = flowOf(
                QuickCreateTaskStatus.Submitting,
                QuickCreateTaskStatus.Queuing("task-1"),
                QuickCreateTaskStatus.Running("task-1", progress = 64),
                QuickCreateTaskStatus.Success(
                    taskId = "task-1",
                    results = listOf(
                        QuickCreateResultItem(
                            url = "https://example.com/result.png",
                            type = "png",
                            thumbnailUrl = "https://example.com/thumb.png",
                            width = 1024,
                            height = 768,
                        )
                    ),
                ),
            )
        )
        val interactor = createInteractor(
            generationRepository = repository,
            uiState = uiState,
            scope = this,
        )

        interactor.generate()
        runCurrent()

        val submittedRequest = repository.lastImageRequest
        val conversationItem = uiState.value.conversationItems.single()
        val display = quickCreateTaskStatusDisplay(
            status = uiState.value.taskStatus.toQuickCreateTaskPresentationStatus(),
            statusText = uiState.value.statusText,
        )

        assertEquals(1, repository.imageGenerateCalls)
        assertEquals("cinematic product shot", submittedRequest?.prompt)
        assertEquals("16:9", submittedRequest?.quickCreationParams?.get("aspectRatio"))
        assertEquals("2k", submittedRequest?.quickCreationParams?.get("resolution"))
        assertEquals("cinematic product shot", conversationItem.prompt)
        assertEquals("16:9", conversationItem.aspectRatio)
        assertEquals("2K", conversationItem.resolution)
        assertEquals(QuickCreateTaskUiStatus.SUCCESS, conversationItem.taskStatus)
        assertEquals(QuickCreateTaskStatusText.Success, conversationItem.statusText)
        assertEquals(listOf("https://example.com/result.png"), conversationItem.results.map { it.url })
        assertEquals(QuickCreateResultMediaType.IMAGE, uiState.value.results.single().mediaType)
        assertEquals(QuickCreateTaskIndicator.Success, display.indicator)
        assertEquals(QuickCreateTaskStatusText.Success, display.text)
    }

    private fun createInteractor(
        generationRepository: RecordingGenerationRepository,
        uiState: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
    ): QuickCreateGenerationInteractor {
        val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val generationRequestFactory = QuickCreateGenerationRequestFactory()
        val feePreviewInteractor = QuickCreateFeePreviewInteractor(
            feePreviewRepository = StaticFeePreviewRepository(),
            generationRequestFactory = generationRequestFactory,
            scope = scope,
            uiState = uiState,
        )
        val mediaUploadCoordinator = QuickCreateMediaUploadCoordinator(
            mediaUploadRepository = StaticMediaUploadRepository(),
            mediaResolver = StaticMediaResolver(),
            generationRequestFactory = generationRequestFactory,
            scope = scope,
            uiState = uiState,
            ioDispatcher = dispatcher,
            onFeePreviewRequired = {},
        )
        val taskPollingController = QuickCreateTaskPollingController(
            uiState = uiState,
            onTaskQueued = {},
            onTaskSucceeded = {},
        )
        return QuickCreateGenerationInteractor(
            generationRepository = generationRepository,
            generationRequestFactory = generationRequestFactory,
            feePreviewInteractor = feePreviewInteractor,
            mediaUploadCoordinator = mediaUploadCoordinator,
            taskPollingController = taskPollingController,
            scope = scope,
            uiState = uiState,
        )
    }

    /**
     * 生成仓库替身只记录正式提交次数。
     *
     * 本测试覆盖计费预览仍在进行时的同步拦截，因此任何远端生成请求都代表 Interactor 边界失效。
     */
    private class RecordingGenerationRepository(
        private val imageStatuses: Flow<QuickCreateTaskStatus> = emptyFlow(),
        private val videoStatuses: Flow<QuickCreateTaskStatus> = emptyFlow(),
    ) : QuickCreationGenerationRepository {
        var imageGenerateCalls = 0
            private set
        var videoGenerateCalls = 0
            private set
        var lastImageRequest: ImageGenerationRequest? = null
            private set
        var lastVideoRequest: VideoGenerationRequest? = null
            private set

        override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            imageGenerateCalls += 1
            lastImageRequest = request
            return imageStatuses
        }

        override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> {
            videoGenerateCalls += 1
            lastVideoRequest = request
            return videoStatuses
        }
    }

    /**
     * 计费仓库替身提供完整接口实现。
     *
     * 当前测试不会触发计费预览请求；保留成功结果只是为了构造真实 [QuickCreateFeePreviewInteractor]。
     */
    private class StaticFeePreviewRepository : QuickCreationFeePreviewRepository {
        override suspend fun previewImageQuickCreationFee(
            request: ImageGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            Result.success(successPreview())

        override suspend fun previewVideoQuickCreationFee(
            request: VideoGenerationRequest,
        ): Result<QuickCreationFeePreview> =
            Result.success(successPreview())

        private fun successPreview(): QuickCreationFeePreview =
            QuickCreationFeePreview(
                passed = true,
                free = false,
                settlementMode = "cash_only",
                requiredCashAmount = 0.76,
                userCashBalance = 156.376,
                cashCurrency = "CNY",
            )
    }

    private class StaticMediaUploadRepository : QuickCreationMediaUploadRepository {
        override suspend fun uploadMedia(
            fileBytes: ByteArray,
            fileName: String,
            mimeType: String,
        ): Result<String> =
            Result.success("https://example.com/uploaded.png")
    }

    private class StaticMediaResolver : QuickCreateMediaResolver {
        override fun readBytes(uri: String): ByteArray = byteArrayOf(1, 2, 3)

        override fun getDisplayName(uri: String): String = "media.png"

        override fun getFileSizeBytes(uri: String): Long = 3L
    }

    private fun serviceModelWithDefaultImageParams(): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = null,
            bindingId = "image-binding",
            skuId = "image-sku",
            name = "全能图片 G-2.0",
            description = null,
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "aspectRatio",
                    paramKey = "aspectRatio",
                    fieldType = "select",
                    required = true,
                    defaultValue = "16:9",
                    options = emptyList(),
                ),
                QuickCreationServiceField(
                    fieldKey = "resolution",
                    paramKey = "resolution",
                    fieldType = "select",
                    required = true,
                    defaultValue = "2k",
                    options = emptyList(),
                ),
            ),
        )
}
