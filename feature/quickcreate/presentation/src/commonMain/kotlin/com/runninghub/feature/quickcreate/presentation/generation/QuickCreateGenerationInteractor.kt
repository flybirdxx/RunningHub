package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.QuickCreateRuntimeUiText
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateFeePreviewInteractor
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateFeeRequestKey
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskPollingController
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.upload.QuickCreateMediaUploadCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 协调快捷创作的正式生成提交流程。
 *
 * 该 Interactor 属于 QuickCreate presentation 层，负责生成入口的业务编排：计费预览拦截、
 * 服务字段校验、等待上传素材、构建图片或视频请求，并把任务状态流交给轮询控制器回写页面状态。
 * ScreenModel 只保留公开 Action 入口，避免继续直接管理生成 Job 和任务状态转换。
 *
 * 并发与生命周期约束：
 * - 同一时间只允许一个生成 Job 处于活跃状态，重复提交只提示用户等待当前任务结束。
 * - 页面销毁时必须调用 [dispose]，避免任务状态流在 ScreenModel 失效后继续回写。
 *
 * @param generationRepository 快捷创作生成仓库，只负责提交图片和视频生成请求并返回任务状态流。
 * @param generationRequestFactory 当前页面状态到图片/视频生成请求的构建器，同时提供服务字段校验规则。
 * @param feePreviewInteractor 计费预览协调器，用于把预览错误转换为生成前拦截文案。
 * @param mediaUploadCoordinator 媒体上传协调器，用于生成前等待当前请求相关素材完成上传。
 * @param taskPollingController 任务状态流控制器，负责把远端排队、运行和终态映射为页面状态。
 * @param scope 页面生命周期作用域，生成 Job 挂在该作用域下。
 * @param uiState 页面状态流，Interactor 只读取编辑状态并更新提交前的阻塞、上传和错误状态。
 */
class QuickCreateGenerationInteractor(
    private val generationRepository: QuickCreationGenerationRepository,
    private val generationRequestFactory: QuickCreateGenerationRequestFactory,
    private val feePreviewInteractor: QuickCreateFeePreviewInteractor,
    private val mediaUploadCoordinator: QuickCreateMediaUploadCoordinator,
    private val taskPollingController: QuickCreateTaskPollingController,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
) {
    private var generationJob: Job? = null

    /**
     * 提交当前快捷创作任务。
     *
     * 方法会先按计费预览状态拦截不可提交场景，再校验动态服务字段。进入协程后会等待当前请求真正相关的上传素材，
     * 并在素材和字段都就绪后根据当前 Tab 调用图片或视频生成接口。
     */
    fun generate() {
        if (generationJob?.isActive == true) {
            blockDuplicateGenerate()
            return
        }
        val submitSnapshot = uiState.value
        if (submitSnapshot.feePreviewLoading) {
            blockGenerate(QuickCreateRuntimeUiText.feeConfirming)
            return
        }
        if (submitSnapshot.feePreviewError != null) {
            blockGenerate(feePreviewInteractor.generateBlockedMessage(submitSnapshot.feePreviewError))
            return
        }
        when (val buildResult = generationRequestFactory.buildCurrentGenerationRequest(
            state = submitSnapshot,
            validateUploads = false,
        )) {
            is QuickCreateGenerationRequestBuildResult.Blocked -> {
                blockGenerate(buildResult.reason.toRuntimeMessage())
                return
            }
            QuickCreateGenerationRequestBuildResult.Unavailable -> return
            is QuickCreateGenerationRequestBuildResult.ImageReady,
            is QuickCreateGenerationRequestBuildResult.VideoReady -> Unit
        }

        generationJob = scope.launch {
            uiState.update {
                it.copy(
                    taskStatus = QuickCreateTaskUiStatus.SUBMITTING,
                    statusText = QuickCreateRuntimeUiText.submittingTask,
                    error = null,
                    results = emptyList(),
                )
            }
            var requestSnapshot = submitSnapshot
            try {
                // 生成提交前只等待当前请求真正会使用的素材，隐藏字段和未激活 child 字段不会阻塞生成。
                // 这里必须使用点击生成时捕获的快照；等待上传期间用户仍可继续编辑页面，
                // 但本次远端任务、计费预览和上传素材应对应同一份参数，不能混入后续输入。
                requestSnapshot = mediaUploadCoordinator.awaitPendingUploads(submitSnapshot)
            } catch (error: IllegalStateException) {
                blockGenerate(error.message)
                return@launch
            }
            when (val buildResult = generationRequestFactory.buildCurrentGenerationRequest(
                state = requestSnapshot,
                validateUploads = true,
            )) {
                is QuickCreateGenerationRequestBuildResult.ImageReady -> {
                    // 最终提交前必须重新按远端请求计算指纹，确保上传完成后的 URL、动态参数和 Prompt
                    // 都已经过同一轮计费预览；否则余额不足或价格变化会被旧预览结果绕过。
                    if (hasMatchingFeePreview(requestSnapshot, buildResult.request.quickCreateFeeRequestKey())) {
                        generateImage(buildResult.request)
                    } else {
                        blockGenerate(QuickCreateRuntimeUiText.feePending)
                    }
                }
                is QuickCreateGenerationRequestBuildResult.VideoReady -> {
                    // 视频请求同样需要绑定最近一次成功预览，尤其是首尾帧、参考视频和音频上传完成后
                    // 会改变正式提交参数，不能复用上传前的价格确认状态。
                    if (hasMatchingFeePreview(requestSnapshot, buildResult.request.quickCreateFeeRequestKey())) {
                        generateVideo(buildResult.request)
                    } else {
                        blockGenerate(QuickCreateRuntimeUiText.feePending)
                    }
                }
                is QuickCreateGenerationRequestBuildResult.Blocked -> blockGenerate(buildResult.reason.toRuntimeMessage())
                QuickCreateGenerationRequestBuildResult.Unavailable -> blockGenerate(null)
            }
        }
    }

    /**
     * 清理当前生成结果和任务状态。
     *
     * UI 在用户关闭结果或准备下一次创作时调用；具体页面状态恢复由任务轮询控制器维护，
     * 保证结果清理和远端任务状态映射保持同一边界。
     */
    fun clearResults() {
        taskPollingController.clearResults()
    }

    /**
     * 取消仍在收集的生成任务状态流。
     *
     * ScreenModel 销毁时调用，避免远程任务状态继续回写已经失效的页面状态。
     */
    fun dispose() {
        generationJob?.cancel()
        generationJob = null
    }

    private suspend fun generateImage(
        request: ImageGenerationRequest,
    ) {
        generationRepository.generateImage(request).let { statuses ->
            taskPollingController.collect(statuses)
        }
    }

    private suspend fun generateVideo(
        request: VideoGenerationRequest,
    ) {
        generationRepository.generateVideo(request).let { statuses ->
            taskPollingController.collect(statuses)
        }
    }

    private fun blockGenerate(error: String?) {
        uiState.update {
            it.copy(
                taskStatus = QuickCreateTaskUiStatus.IDLE,
                statusText = null,
                error = error,
            )
        }
    }

    private fun blockDuplicateGenerate() {
        uiState.update {
            // 当前任务已经进入提交或轮询链路时不能取消后重新提交，否则会产生第二个远端任务且旧任务失去状态归属。
            it.copy(error = QuickCreateRuntimeUiText.duplicateGeneration)
        }
    }

    private fun hasMatchingFeePreview(
        state: QuickCreateUiState,
        requestKey: String,
    ): Boolean =
        state.feePreviewRequestKey == requestKey
}

/**
 * 将生成前阻塞原因映射为当前运行时文案端口。
 *
 * 请求构建器只返回稳定原因，避免纯数据转换层继续拼接最终中文 UI 文案；
 * 这里仍暂时复用 [QuickCreateRuntimeUiText]，后续可把该端口整体迁到 Compose Resources 或注入式 TextProvider。
 */
private fun QuickCreateGenerationBlockReason.toRuntimeMessage(): String =
    when (this) {
        is QuickCreateGenerationBlockReason.CustomMessage -> message
        QuickCreateGenerationBlockReason.PromptRequired -> QuickCreateRuntimeUiText.promptRequired
        is QuickCreateGenerationBlockReason.PromptTooLong -> QuickCreateRuntimeUiText.promptTooLong(maxChars)
    }
