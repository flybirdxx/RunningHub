package com.runninghub.feature.quickcreate.presentation.billing

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.asQuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestBuildResult
import com.runninghub.feature.quickcreate.presentation.generation.QuickCreateGenerationRequestFactory
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.toQuickCreateUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 计费预览需要等待用户连续编辑结束后再请求，避免每个输入字符都触发远程计费接口。
private const val FEE_PREVIEW_DEBOUNCE_MS = 500L

// 计费预览与正式生成共用请求构建规则；这里仅把已构建请求按媒体类型分发到不同 Repository 入口。
private sealed interface QuickCreateFeePreviewRequest {
    data class Image(
        val request: ImageGenerationRequest,
    ) : QuickCreateFeePreviewRequest

    data class Video(
        val request: VideoGenerationRequest,
    ) : QuickCreateFeePreviewRequest
}

/**
 * 计算计费预览和正式提交共用的请求指纹。
 *
 * 该指纹只包含会进入远端计费/提交请求的领域参数，不包含 UI 加载态、错误态或本地展示字段。
 * Map 参数按 key 排序后拼接，避免不同遍历顺序造成同一请求被误判为不同快照。
 */
fun ImageGenerationRequest.quickCreateFeeRequestKey(): String =
    listOf(
        "image",
        prompt,
        model,
        aspectRatio,
        resolution,
        quality,
        referenceImageUri.orEmpty(),
        numImages.toString(),
        seed?.toString().orEmpty(),
        negativePrompt.orEmpty(),
        quickCreationCategoryId.orEmpty(),
        quickCreationBindingId.orEmpty(),
        quickCreationSkuId.orEmpty(),
        quickCreationParams.stableScalarParamsKey(),
        quickCreationListParams.stableListParamsKey(),
    ).joinToString(separator = "\u001F")

/**
 * 计算视频计费预览和正式提交共用的请求指纹。
 *
 * 与图片请求保持同一规则：只比较远端请求参数，并对 Map 做稳定排序。
 */
fun VideoGenerationRequest.quickCreateFeeRequestKey(): String =
    listOf(
        "video",
        prompt,
        model,
        aspectRatio,
        duration.toString(),
        resolution,
        referenceImageUri.orEmpty(),
        referenceVideoUri.orEmpty(),
        referenceAudioUri.orEmpty(),
        firstFrameImageUri.orEmpty(),
        lastFrameImageUri.orEmpty(),
        realistic.toString(),
        generateAudio.toString(),
        numVideos.toString(),
        seed?.toString().orEmpty(),
        negativePrompt.orEmpty(),
        style,
        promptExtend.toString(),
        quickCreationCategoryId.orEmpty(),
        quickCreationBindingId.orEmpty(),
        quickCreationSkuId.orEmpty(),
        quickCreationParams.stableScalarParamsKey(),
        quickCreationListParams.stableListParamsKey(),
    ).joinToString(separator = "\u001F")

private fun Map<String, String>.stableScalarParamsKey(): String =
    entries
        .sortedBy { it.key }
        .joinToString(separator = "\u001E") { (key, value) -> "$key=$value" }

private fun Map<String, List<String>>.stableListParamsKey(): String =
    entries
        .sortedBy { it.key }
        .joinToString(separator = "\u001E") { (key, values) -> "$key=${values.joinToString(separator = "\u001D")}" }

/**
 * 协调快捷创作的计费预览流程。
 *
 * 该 Interactor 属于 QuickCreate presentation 层，专门接管原本集中在 ScreenModel 中的计费预览副作用：
 * 根据当前编辑状态构建图片或视频预览请求、防抖调用远程计费接口，并把结果回写到 [QuickCreateUiState]。
 * 它不持有 Compose 或平台类型，便于被 composeApp 壳层组装，并保持计费逻辑在 feature 边界内演进。
 *
 * 并发约束：
 * - 每次 [schedule] 都会取消上一轮尚未发出的预览任务。
 * - 使用递增序号忽略旧请求的晚到响应，避免旧价格覆盖用户最新编辑后的价格。
 * - [dispose] 必须在 ScreenModel 销毁时调用，防止防抖任务或远程响应继续回写失效状态。
 *
 * @param feePreviewRepository 快捷创作计费预览仓库，负责执行图片和视频计费预览请求。
 * @param generationRequestFactory 当前编辑状态到计费请求的构建器，同时复用服务字段和上传校验规则。
 * @param scope 页面生命周期作用域，所有预览任务都挂在该作用域下，随 ScreenModel 一起取消。
 * @param uiState 页面状态流，Interactor 只更新计费预览相关字段和估算价格。
 */
class QuickCreateFeePreviewInteractor(
    private val feePreviewRepository: QuickCreationFeePreviewRepository,
    private val generationRequestFactory: QuickCreateGenerationRequestFactory,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
) {
    private var feePreviewJob: Job? = null
    private var feePreviewRequestSeq: Long = 0L

    /**
     * 根据最新页面状态安排一次计费预览。
     *
     * 调用方可以在模型、参数、素材或模板变化后直接调用该方法。方法会先取消旧任务，再检查当前状态是否
     * 已具备计费预览条件；如果请求无法构建或素材仍在上传，则立即清理预览 loading、错误和金额，
     * 由 UI 显示运行前确认费用。
     */
    fun schedule() {
        feePreviewJob?.cancel()
        val requestSeq = ++feePreviewRequestSeq
        val request = buildFeePreviewRequest(uiState.value)
        if (request == null) {
            uiState.update {
                it.copy(
                    feePreviewLoading = false,
                    feePreviewError = null,
                    feePreviewRequestKey = null,
                    billingPreview = null,
                    estimatedCost = 0.0,
                )
            }
            return
        }
        uiState.update {
            it.copy(
                estimatedCost = 0.0,
                feePreviewLoading = true,
                feePreviewError = null,
                feePreviewRequestKey = null,
                billingPreview = null,
            )
        }
        feePreviewJob = scope.launch {
            delay(FEE_PREVIEW_DEBOUNCE_MS)
            when (val request = buildFeePreviewRequest(uiState.value)) {
                null -> {
                    if (isCurrentFeePreviewRequest(requestSeq)) {
                        clearFeePreviewState()
                    }
                }
                is QuickCreateFeePreviewRequest.Image -> previewImage(
                    requestSeq = requestSeq,
                    request = request.request,
                    requestKey = request.request.quickCreateFeeRequestKey(),
                )
                is QuickCreateFeePreviewRequest.Video -> previewVideo(
                    requestSeq = requestSeq,
                    request = request.request,
                    requestKey = request.request.quickCreateFeeRequestKey(),
                )
            }
        }
    }

    /**
     * 释放当前计费预览任务。
     *
     * ScreenModel 销毁或测试显式清理时调用。这里不清空 UI 状态，因为调用方可能需要保留最后一次价格或错误展示；
     * 只取消尚未完成的协程，避免生命周期结束后继续发请求或回写状态。
     */
    fun dispose() {
        feePreviewJob?.cancel()
        feePreviewJob = null
    }

    /**
     * 把计费预览错误转换为生成按钮拦截时展示的文案。
     *
     * 余额不足属于明确业务失败，应直接透传；其他网络异常或服务端异常只表示价格无法确认，
     * 生成流程需要提示用户先等待或重新触发预览，而不是暴露底层异常细节。
     */
    fun generateBlockedMessage(error: QuickCreateUiMessage?): QuickCreateUiMessage =
        if (error == QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage()) {
            error
        } else {
            QuickCreateRuntimeUiText.FeePending.asQuickCreateUiMessage()
        }

    private suspend fun previewVideo(
        requestSeq: Long,
        request: VideoGenerationRequest,
        requestKey: String,
    ) {
        feePreviewRepository.previewVideoQuickCreationFee(request).fold(
            onSuccess = { preview ->
                if (isCurrentFeePreviewRequest(requestSeq)) {
                    applyFeePreview(preview, requestKey)
                }
            },
            onFailure = { error ->
                if (isCurrentFeePreviewRequest(requestSeq)) {
                    applyFeePreviewError(error)
                }
            },
        )
    }

    private suspend fun previewImage(
        requestSeq: Long,
        request: ImageGenerationRequest,
        requestKey: String,
    ) {
        feePreviewRepository.previewImageQuickCreationFee(request).fold(
            onSuccess = { preview ->
                if (isCurrentFeePreviewRequest(requestSeq)) {
                    applyFeePreview(preview, requestKey)
                }
            },
            onFailure = { error ->
                if (isCurrentFeePreviewRequest(requestSeq)) {
                    applyFeePreviewError(error)
                }
            },
        )
    }

    private fun isCurrentFeePreviewRequest(requestSeq: Long): Boolean =
        requestSeq == feePreviewRequestSeq

    private fun buildFeePreviewRequest(state: QuickCreateUiState): QuickCreateFeePreviewRequest? {
        if (state.hasUnreadyFeePreviewMediaReferences()) return null
        return when (val result = generationRequestFactory.buildCurrentGenerationRequest(state, validateUploads = true)) {
            is QuickCreateGenerationRequestBuildResult.ImageReady -> QuickCreateFeePreviewRequest.Image(result.request)
            is QuickCreateGenerationRequestBuildResult.VideoReady -> QuickCreateFeePreviewRequest.Video(result.request)
            is QuickCreateGenerationRequestBuildResult.Blocked,
            QuickCreateGenerationRequestBuildResult.Unavailable -> null
        }
    }

    private fun QuickCreateUiState.hasUnreadyFeePreviewMediaReferences(): Boolean =
        generationRequestFactory.currentRelevantMediaReferences(this).any { reference ->
            reference.uploadStatus == UploadStatus.FAILED ||
                reference.uploadStatus == UploadStatus.UPLOADING ||
                reference.uploadStatus == UploadStatus.PROCESSING
        }

    private fun clearFeePreviewState() {
        uiState.update {
            it.copy(
                feePreviewLoading = false,
                feePreviewError = null,
                feePreviewRequestKey = null,
                billingPreview = null,
                estimatedCost = 0.0,
            )
        }
    }

    private fun applyFeePreview(
        preview: QuickCreationFeePreview,
        requestKey: String,
    ) {
        val previewCost = when {
            preview.free -> 0.0
            preview.requiredCashAmount > 0.0 -> preview.requiredCashAmount
            else -> preview.requiredRhAmount
        }
        val previewError = if (!preview.passed || preview.insufficientType != null) {
            QuickCreatePresentationError.FeePreviewNotPassed.asQuickCreateUiMessage()
        } else {
            null
        }
        val billingPreview = preview.toQuickCreateBillingPreviewUi()

        uiState.update {
            it.copy(
                estimatedCost = if (previewError == null) {
                    previewCost
                } else {
                    0.0
                },
                feePreviewLoading = false,
                feePreviewError = previewError,
                feePreviewRequestKey = if (previewError == null) requestKey else null,
                billingPreview = billingPreview,
            )
        }
    }

    private fun applyFeePreviewError(error: Throwable) {
        uiState.update {
            it.copy(
                estimatedCost = 0.0,
                feePreviewLoading = false,
                feePreviewError = error.toQuickCreateUiMessage(QuickCreatePresentationError.FeePreviewFailed),
                feePreviewRequestKey = null,
                billingPreview = null,
            )
        }
    }
}
