package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateFeePreviewInteractor
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingController
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaResolver
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaUploadCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
        assertEquals(QuickCreateRuntimeUiText.feeConfirming, uiState.value.error)
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
    private class RecordingGenerationRepository : QuickCreationGenerationRepository {
        var imageGenerateCalls = 0
            private set
        var videoGenerateCalls = 0
            private set

        override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> {
            imageGenerateCalls += 1
            return emptyFlow()
        }

        override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> {
            videoGenerateCalls += 1
            return emptyFlow()
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
}
