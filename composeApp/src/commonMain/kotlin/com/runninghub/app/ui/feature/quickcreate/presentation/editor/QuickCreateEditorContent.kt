package com.runninghub.app.ui.feature.quickcreate.presentation.editor

import com.runninghub.feature.quickcreate.presentation.editor.quickCreationGlobalMediaReferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.QuickCreateClassicComposer
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.QuickCreateCompactComposer
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

/**
 * 展示快捷创作页面底部的编辑器区域。
 *
 * 该组件属于 editor 子区域，负责提示词输入、全局媒体引用、模型入口、参数入口、
 * 草稿恢复提示和生成按钮的渲染。所有用户行为都通过回调交给 ScreenModel/Coordinator，
 * 组件自身不读写草稿、媒体文件、Repository 或计费数据，保持 UI -> Action 的单向数据流。
 *
 * @param uiState 快捷创作页面状态，读取当前 tab、提示词、媒体、模型、计费预览和任务状态。
 * @param onTabSwitch 用户切换图片/视频创作 tab 时触发。
 * @param onPromptChange 用户输入提示词时触发，参数为最新提示词。
 * @param onLaunchImagePicker 用户请求选择图片素材时触发。
 * @param onLaunchVideoPicker 用户请求选择视频素材时触发。
 * @param onLaunchAudioPicker 用户请求选择音频素材时触发。
 * @param onRemoveMedia 用户移除媒体引用时触发，参数为媒体引用 ID。
 * @param onOpenModelSheet 用户打开模型选择面板时触发。
 * @param onOpenParamsSheet 用户打开参数调优面板时触发。
 * @param onImageRatioChange 用户切换图片比例时触发。
 * @param onImageResChange 用户切换图片尺寸时触发。
 * @param onImageQualityChange 用户切换图片质量时触发。
 * @param onImageCountChange 用户切换图片生成数量时触发。
 * @param onVideoRatioChange 用户切换视频比例时触发。
 * @param onVideoResChange 用户切换视频尺寸时触发。
 * @param onVideoDurationChange 用户切换视频时长时触发。
 * @param onToggleAudio 用户切换视频是否生成音频时触发。
 * @param onRestoreDraft 用户恢复本地草稿时触发。
 * @param onDiscardDraft 用户丢弃本地草稿时触发。
 * @param onGenerate 用户提交生成任务时触发。
 */
@Composable
internal fun QuickCreateEditorPanel(
    uiState: QuickCreateUiState,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onPromptChange: (String) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchVideoPicker: () -> Unit,
    onLaunchAudioPicker: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onImageRatioChange: (ImageAspectRatio) -> Unit,
    onImageResChange: (ImageResolution) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onVideoRatioChange: (VideoAspectRatio) -> Unit,
    onVideoResChange: (VideoResolution) -> Unit,
    onVideoDurationChange: (VideoDuration) -> Unit,
    onToggleAudio: () -> Unit,
    onRestoreDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onGenerate: () -> Unit,
) {
    val isImage = uiState.currentTab == QuickCreateTab.IMAGE
    val imageConfig = uiState.imageConfig
    val videoConfig = uiState.videoConfig
    val configPrompt = if (isImage) imageConfig.prompt else videoConfig.prompt
    val configCharCount = if (isImage) imageConfig.promptCharCount else videoConfig.promptCharCount
    val configNearLimit = if (isImage) imageConfig.promptNearLimit else videoConfig.promptNearLimit
    val configOverLimit = if (isImage) imageConfig.promptOverLimit else videoConfig.promptOverLimit
    val configMediaRefs = (if (isImage) imageConfig.mediaReferences else videoConfig.mediaReferences)
        .quickCreationGlobalMediaReferences()
    val selectedServiceModel = if (isImage) uiState.selectedImageServiceModelUi else uiState.selectedVideoServiceModelUi
    val isTaskActive = uiState.taskStatus in listOf(
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING,
    )

    // 当前默认启用紧凑编辑器，保留旧布局分支是为了后续自适应实验能在同一边界内调整。
    val useCompactComposer = remember { true }
    if (useCompactComposer) {
        QuickCreateCompactComposer(
            uiState = uiState,
            isImage = isImage,
            prompt = configPrompt,
            onPromptChange = onPromptChange,
            charCount = configCharCount,
            nearLimit = configNearLimit,
            overLimit = configOverLimit,
            mediaReferences = configMediaRefs,
            selectedServiceModel = selectedServiceModel,
            serviceModelsLoading = uiState.serviceModelsLoading,
            isTaskActive = isTaskActive,
            canGenerate = !isTaskActive &&
                configPrompt.isNotBlank() &&
                !configOverLimit &&
                !uiState.feePreviewLoading,
            onTabSwitch = onTabSwitch,
            onLaunchImagePicker = onLaunchImagePicker,
            onRemoveMedia = onRemoveMedia,
            onOpenModelSheet = onOpenModelSheet,
            onOpenParamsSheet = onOpenParamsSheet,
            onGenerate = onGenerate,
        )
    } else {
        QuickCreateClassicComposer(
            uiState = uiState,
            isImage = isImage,
            prompt = configPrompt,
            charCount = configCharCount,
            nearLimit = configNearLimit,
            overLimit = configOverLimit,
            mediaReferences = configMediaRefs,
            isTaskActive = isTaskActive,
            onTabSwitch = onTabSwitch,
            onPromptChange = onPromptChange,
            onLaunchImagePicker = onLaunchImagePicker,
            onLaunchVideoPicker = onLaunchVideoPicker,
            onLaunchAudioPicker = onLaunchAudioPicker,
            onRemoveMedia = onRemoveMedia,
            onOpenModelSheet = onOpenModelSheet,
            onOpenParamsSheet = onOpenParamsSheet,
            onImageRatioChange = onImageRatioChange,
            onImageResChange = onImageResChange,
            onImageQualityChange = onImageQualityChange,
            onImageCountChange = onImageCountChange,
            onVideoRatioChange = onVideoRatioChange,
            onVideoResChange = onVideoResChange,
            onVideoDurationChange = onVideoDurationChange,
            onToggleAudio = onToggleAudio,
            onRestoreDraft = onRestoreDraft,
            onDiscardDraft = onDiscardDraft,
            onGenerate = onGenerate,
        )
    }
}
