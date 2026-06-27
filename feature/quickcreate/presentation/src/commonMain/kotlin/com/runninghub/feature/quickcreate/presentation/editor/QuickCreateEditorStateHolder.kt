package com.runninghub.feature.quickcreate.presentation.editor

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.draft.QuickCreateDraftRestore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet

private val supportedImageCounts = setOf(1, 2, 4)
private val supportedVideoCounts = setOf(1, 2)
private fun sanitizedSeed(seed: Int?): Int? = seed?.takeIf { it >= 0 }

/**
 * 维护快捷创作编辑区的本地输入状态。
 *
 * 本类位于 Presentation 层，只负责用户在编辑器里直接修改的页面状态：
 * 模式、Tab、弹层、Prompt、本地模型参数、数量、随机种子和视频开关。它不负责服务端模型目录、
 * 素材上传、计费预览请求、草稿持久化或任务提交；这些副作用通过回调交给 ScreenModel 继续协调。
 *
 * @param uiState 页面状态容器；本类只修改编辑区和一次性错误展示相关字段。
 * @param onFeePreviewRequired 影响价格的输入变化后触发，由调用方负责防抖和旧响应隔离。
 * @param onDraftChanged Prompt 或当前 Tab 变化后触发，由调用方负责草稿自动保存和清理。
 */
class QuickCreateEditorStateHolder(
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onFeePreviewRequired: () -> Unit,
    private val onDraftChanged: () -> Unit,
) {

    /**
     * 切换图片 / 视频创作 Tab。
     *
     * Tab 切换会立即把本地估价切到目标配置的估算值，并清理上一 Tab 的远端计费预览状态；
     * 这样页面不会短暂展示另一种创作类型的价格或预览错误。
     */
    fun switchTab(tab: QuickCreateTab) {
        val cost = when (tab) {
            QuickCreateTab.IMAGE -> uiState.value.imageConfig.estimatedCost
            QuickCreateTab.VIDEO -> uiState.value.videoConfig.estimatedCost
        }
        uiState.update {
            it.copy(
                currentTab = tab,
                estimatedCost = cost,
                feePreviewLoading = false,
                feePreviewError = null,
                feePreviewRequestKey = null,
            )
        }
        onFeePreviewRequired()
        onDraftChanged()
    }

    /**
     * 更新图片 Prompt。
     *
     * Prompt 同时影响生成请求、计费预览和草稿恢复内容，因此修改后必须同时触发价格预览与草稿自动保存。
     */
    fun updateImagePrompt(prompt: String) {
        uiState.update { it.copy(imageConfig = it.imageConfig.copy(prompt = prompt)) }
        onFeePreviewRequired()
        onDraftChanged()
    }

    /**
     * 更新视频 Prompt。
     *
     * 与图片 Prompt 保持同一套副作用：刷新价格预览并调度草稿自动保存，避免用户切换 Tab 后丢失输入。
     */
    fun updateVideoPrompt(prompt: String) {
        uiState.update { it.copy(videoConfig = it.videoConfig.copy(prompt = prompt)) }
        onFeePreviewRequired()
        onDraftChanged()
    }

    /**
     * 将可恢复草稿写回编辑区。
     *
     * 草稿恢复属于编辑区状态变更：需要同时恢复当前 Tab、图片 Prompt 和视频 Prompt。
     * 本方法只负责同步页面状态，不触发草稿保存；调用方会在草稿模块清理本地草稿后再统一调度计费预览，
     * 避免恢复旧草稿时又把同一内容重新写回持久化草稿。
     *
     * @param restore 草稿模块解析出的最小恢复结果，包含目标 Tab 和两个编辑区提示词。
     */
    fun applyDraftRestore(restore: QuickCreateDraftRestore) {
        uiState.update {
            it.copy(
                currentTab = restore.tab,
                imageConfig = it.imageConfig.copy(prompt = restore.imagePrompt),
                videoConfig = it.videoConfig.copy(prompt = restore.videoPrompt),
                feePreviewRequestKey = null,
            )
        }
    }

    /**
     * 打开模型选择弹层。
     *
     * 弹层状态使用单个枚举表示，新的弹层会覆盖旧弹层，保证页面不会同时展示模型和参数两个面板。
     */
    fun showModelPickerSheet() {
        uiState.update { it.copy(activeSheet = QuickCreateSheet.MODEL_PICKER) }
    }

    /**
     * 打开参数调节弹层。
     *
     * 与模型弹层互斥，避免同一底部弹层容器出现两个不同内容来源。
     */
    fun showParamsSheet() {
        uiState.update { it.copy(activeSheet = QuickCreateSheet.PARAMS) }
    }

    /** 关闭当前弹层；`null` 表示页面没有任何底部参数或模型选择弹层。 */
    fun closeActiveSheet() {
        uiState.update { it.copy(activeSheet = null) }
    }

    /**
     * 切换图片本地模型。
     *
     * 本地模型变更后必须回到该模型支持的默认比例、分辨率和质量，避免保留旧模型不支持的参数。
     */
    fun updateImageModel(model: ImageModel) {
        uiState.update {
            val newConfig = it.imageConfig.copy(
                model = model,
                aspectRatio = model.defaultAspectRatio,
                resolution = model.defaultResolution,
                quality = model.defaultQuality,
            )
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /**
     * 切换视频本地模型。
     *
     * 模型变更会重置比例、分辨率和时长；对于新模型不支持的真实模式或音频生成开关会自动关闭，
     * 防止生成请求携带与模型能力不匹配的参数。
     */
    fun updateVideoModel(model: VideoModel) {
        uiState.update {
            val newConfig = it.videoConfig.copy(
                model = model,
                aspectRatio = model.defaultAspectRatio,
                resolution = model.defaultResolution,
                duration = model.defaultDuration,
                generateAudio = model.supportsGenerateAudio && it.videoConfig.generateAudio,
                realisticMode = model.supportsRealistic && it.videoConfig.realisticMode,
            )
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /** 更新图片比例；若目标比例不受当前图片模型支持，则保持旧值以避免生成请求非法。 */
    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        if (ratio !in uiState.value.imageConfig.model.supportedRatios) return
        uiState.update { it.copy(imageConfig = it.imageConfig.copy(aspectRatio = ratio)) }
        onFeePreviewRequired()
    }

    /** 更新图片分辨率；非法分辨率会被忽略，并保留当前估价与请求参数。 */
    fun updateImageResolution(resolution: ImageResolution) {
        if (resolution !in uiState.value.imageConfig.model.supportedResolutions) return
        uiState.update {
            val newConfig = it.imageConfig.copy(resolution = resolution)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /** 更新图片质量；只有当前模型声明支持的质量档位才会触发价格预览。 */
    fun updateImageQuality(quality: ImageQuality) {
        if (quality !in uiState.value.imageConfig.model.supportedQualities) return
        uiState.update {
            val newConfig = it.imageConfig.copy(quality = quality)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /** 更新图片生成数量；当前只允许服务端支持的固定数量，非法值会保持上一次有效选择。 */
    fun updateImageCount(count: Int) {
        if (count !in supportedImageCounts) return
        uiState.update {
            val newConfig = it.imageConfig.copy(count = count)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /**
     * 更新图片随机种子。
     *
     * 负数在业务上表示用户想清空种子，因此会被规范化为 `null`，请求构建时不再提交 seed。
     */
    fun updateImageSeed(seed: Int?) {
        uiState.update { it.copy(imageConfig = it.imageConfig.copy(seed = sanitizedSeed(seed))) }
        onFeePreviewRequired()
    }

    /** 更新视频比例；若当前视频模型不支持目标比例，则忽略该次用户输入。 */
    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        if (ratio !in uiState.value.videoConfig.model.supportedRatios) return
        uiState.update { it.copy(videoConfig = it.videoConfig.copy(aspectRatio = ratio)) }
        onFeePreviewRequired()
    }

    /** 更新视频分辨率；非法值会被忽略，避免远端生成接口收到不支持的分辨率。 */
    fun updateVideoResolution(resolution: VideoResolution) {
        if (resolution !in uiState.value.videoConfig.model.supportedResolutions) return
        uiState.update {
            val newConfig = it.videoConfig.copy(resolution = resolution)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /** 更新视频时长；只有当前模型支持的时长才会写入状态并刷新价格。 */
    fun updateVideoDuration(duration: VideoDuration) {
        if (duration !in uiState.value.videoConfig.model.supportedDurations) return
        uiState.update {
            val newConfig = it.videoConfig.copy(duration = duration)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /** 更新视频生成数量；非法数量会被忽略，防止生成请求超过服务端当前能力。 */
    fun updateVideoCount(count: Int) {
        if (count !in supportedVideoCounts) return
        uiState.update {
            val newConfig = it.videoConfig.copy(count = count)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /**
     * 更新视频随机种子。
     *
     * 与图片一致，负数会清空 seed；`null` 表示生成请求交给服务端随机。
     */
    fun updateVideoSeed(seed: Int?) {
        uiState.update { it.copy(videoConfig = it.videoConfig.copy(seed = sanitizedSeed(seed))) }
        onFeePreviewRequired()
    }

    /**
     * 切换视频真实模式。
     *
     * 只有当前模型声明支持真实模式时才允许切换；不支持时保持旧值，避免 UI 开关和请求能力不一致。
     */
    fun toggleRealisticMode() {
        if (!uiState.value.videoConfig.model.supportsRealistic) return
        uiState.update {
            it.copy(videoConfig = it.videoConfig.copy(realisticMode = !it.videoConfig.realisticMode))
        }
        onFeePreviewRequired()
    }

    /**
     * 切换视频生成音频开关。
     *
     * 只有支持音频生成的模型才允许开启；该开关会影响视频估价，因此切换后需要刷新价格预览。
     */
    fun toggleGenerateAudio() {
        if (!uiState.value.videoConfig.model.supportsGenerateAudio) return
        uiState.update {
            val newConfig = it.videoConfig.copy(generateAudio = !it.videoConfig.generateAudio)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        onFeePreviewRequired()
    }

    /**
     * 清除页面当前错误提示。
     *
     * 错误可能来自上传、计费、生成、历史或项目流程；此处只处理用户关闭提示后的展示状态，
     * 不修改产生错误的业务子状态。
     */
    fun dismissError() {
        uiState.update { it.copy(error = null) }
    }
}
