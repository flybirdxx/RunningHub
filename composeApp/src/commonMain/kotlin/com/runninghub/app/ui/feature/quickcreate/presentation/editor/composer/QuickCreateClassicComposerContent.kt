package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.feature.quickcreate.AdaptivePromptTextField
import com.runninghub.app.ui.feature.quickcreate.quickCreateNavigationText
import com.runninghub.app.ui.feature.quickcreate.quickCreateBillingAmountText
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.feature.quickcreate.presentation.draft.DraftData
import com.runninghub.feature.quickcreate.presentation.draft.QuickCreateDraftResumeSummary
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.app.ui.feature.quickcreate.MediaChipCard
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.state.navigationLabel
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.app.ui.feature.quickcreate.presentation.upload.QuickCreateMediaToolbarRow
import com.runninghub.feature.quickcreate.presentation.draft.resumeSummary
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral100
import com.runninghub.app.ui.theme.Neutral300
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.Secondary500
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingPreviewUi
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_classic_default_model
import runninghub.composeapp.generated.resources.quick_create_classic_default_model_subtitle
import runninghub.composeapp.generated.resources.quick_create_classic_discard_draft
import runninghub.composeapp.generated.resources.quick_create_classic_draft_image_tab
import runninghub.composeapp.generated.resources.quick_create_classic_draft_resume_summary_format
import runninghub.composeapp.generated.resources.quick_create_classic_draft_video_tab
import runninghub.composeapp.generated.resources.quick_create_classic_generate_button_label
import runninghub.composeapp.generated.resources.quick_create_classic_generate_content_description
import runninghub.composeapp.generated.resources.quick_create_classic_image_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_classic_params_content_description
import runninghub.composeapp.generated.resources.quick_create_classic_price_pending
import runninghub.composeapp.generated.resources.quick_create_classic_price_refreshing
import runninghub.composeapp.generated.resources.quick_create_classic_restore_draft
import runninghub.composeapp.generated.resources.quick_create_classic_video_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_send_fee_confirming
import runninghub.composeapp.generated.resources.quick_create_send_fee_pending
import runninghub.composeapp.generated.resources.quick_create_send_generate

/**
 * 渲染快捷创作旧版底部编辑器。
 *
 * 该组件保留在 editor 子区域内，是紧凑输入条之外的兼容布局分支。它只根据
 * [QuickCreateUiState] 的派生状态展示草稿恢复、tab、服务模型、媒体、参数入口和提交按钮，
 * 所有状态变更都通过回调交给 ScreenModel/Coordinator，避免旧布局重新引入网络、存储或上传职责。
 *
 * @param uiState 快捷创作页面完整状态，用于读取当前 tab、草稿、计费、模型和参数配置。
 * @param isImage 当前是否处于图片创作 tab；`false` 表示视频创作 tab。
 * @param prompt 当前 tab 的提示词。
 * @param charCount 当前提示词字符数。
 * @param nearLimit `true` 表示提示词接近长度上限。
 * @param overLimit `true` 表示提示词已经超过长度上限，提交按钮应禁用。
 * @param mediaReferences 当前 tab 的全局媒体引用列表。
 * @param isTaskActive `true` 表示任务正在提交、排队或运行，旧布局应禁用提交和调参入口。
 * @param onTabSwitch 用户切换图片/视频创作 tab 时触发。
 * @param onPromptChange 用户修改提示词时触发。
 * @param onLaunchImagePicker 用户点击图片素材入口时触发。
 * @param onLaunchVideoPicker 用户点击视频素材入口时触发。
 * @param onLaunchAudioPicker 用户点击音频素材入口时触发。
 * @param onRemoveMedia 用户移除媒体引用时触发，参数为媒体引用 ID。
 * @param onOpenModelSheet 用户打开服务模型面板时触发。
 * @param onOpenParamsSheet 用户打开参数面板时触发。
 * @param onImageRatioChange 用户快速切换图片比例时触发。
 * @param onImageResChange 用户快速切换图片尺寸时触发。
 * @param onImageQualityChange 用户快速切换图片质量时触发。
 * @param onImageCountChange 用户快速切换图片数量时触发。
 * @param onVideoRatioChange 用户快速切换视频比例时触发。
 * @param onVideoResChange 用户快速切换视频尺寸时触发。
 * @param onVideoDurationChange 用户快速切换视频时长时触发。
 * @param onToggleAudio 用户切换视频音频开关时触发。
 * @param onRestoreDraft 用户恢复本地草稿时触发。
 * @param onDiscardDraft 用户丢弃本地草稿时触发。
 * @param onGenerate 用户提交生成任务时触发。
 */
@Composable
internal fun QuickCreateClassicComposer(
    uiState: QuickCreateUiState,
    isImage: Boolean,
    prompt: String,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    mediaReferences: List<MediaReference>,
    isTaskActive: Boolean,
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
    val windowInfo = LocalRhWindowInfo.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = windowInfo.bottomSheetMaxWidth)
                    .padding(horizontal = Dimens.SpaceMD)
                    .navigationBarsPadding()
                    .padding(top = Dimens.SpaceSM, bottom = Dimens.SpaceMD),
            ) {
                if (uiState.hasDraft && !isTaskActive) {
                    DraftResumeRow(
                        draftData = uiState.draftData,
                        onRestoreDraft = onRestoreDraft,
                        onDiscardDraft = onDiscardDraft,
                    )
                    Spacer(Modifier.height(Dimens.SpaceSM))
                }

                TabPillRow(
                    selectedTab = uiState.currentTab,
                    onTabSelected = onTabSwitch,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                ServiceModelSummaryRow(
                    model = if (isImage) uiState.selectedImageServiceModelUi else uiState.selectedVideoServiceModelUi,
                    loading = uiState.serviceModelsLoading,
                    feePreviewLoading = uiState.feePreviewLoading,
                    feePreviewError = uiState.feePreviewError,
                    billingPreview = uiState.billingPreview,
                    onClick = onOpenModelSheet,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                QuickCreateMediaToolbarRow(
                    mediaReferences = mediaReferences,
                    onLaunchImagePicker = onLaunchImagePicker,
                    onLaunchVideoPicker = onLaunchVideoPicker,
                    onLaunchAudioPicker = onLaunchAudioPicker,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                if (mediaReferences.isNotEmpty()) {
                    mediaReferences.forEach { reference ->
                        MediaChipCard(
                            reference = reference,
                            onRemove = { onRemoveMedia(reference.id) },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceSM))
                AdaptivePromptTextField(
                    prompt = prompt,
                    onPromptChange = onPromptChange,
                    placeholder = if (isImage) {
                        stringResource(Res.string.quick_create_classic_image_prompt_placeholder)
                    } else {
                        stringResource(Res.string.quick_create_classic_video_prompt_placeholder)
                    },
                    charCount = charCount,
                    nearLimit = nearLimit,
                    overLimit = overLimit,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Dimens.SpaceSM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onOpenParamsSheet,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (uiState.activeSheet == QuickCreateSheet.PARAMS) {
                                    Primary300.copy(alpha = 0.12f)
                                } else {
                                    DarkSurfaceVariant
                                },
                                RoundedCornerShape(Dimens.RadiusMD),
                            ),
                        enabled = !isTaskActive,
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = stringResource(
                                Res.string.quick_create_classic_params_content_description,
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (uiState.activeSheet == QuickCreateSheet.PARAMS) Primary300 else Neutral400,
                        )
                    }

                    SendButton(
                        enabled = !isTaskActive &&
                            prompt.isNotBlank() &&
                            !overLimit &&
                            !uiState.feePreviewLoading,
                        isLoading = isTaskActive,
                        feePreviewLoading = uiState.feePreviewLoading,
                        feePreviewError = uiState.feePreviewError,
                        billingPreview = uiState.billingPreview,
                        onClick = onGenerate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 展示可恢复草稿的轻量提示条。
 *
 * 旧版编辑器只在没有活跃任务时显示该入口，避免用户在提交或轮询过程中恢复草稿导致
 * 当前任务参数被覆盖。草稿内容由上游状态提供，本组件只负责展示和分发恢复/丢弃动作。
 */
@Composable
private fun DraftResumeRow(
    draftData: DraftData?,
    onRestoreDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
) {
    if (draftData == null) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Primary300.copy(alpha = 0.10f),
        shape = RoundedCornerShape(Dimens.RadiusMD),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary300,
            )
            Text(
                text = quickCreateDraftResumeSummaryText(draftData.resumeSummary()),
                modifier = Modifier.weight(1f),
                color = Neutral100,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = onDiscardDraft,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.quick_create_classic_discard_draft),
                    color = Neutral400,
                    fontSize = 12.sp,
                )
            }
            TextButton(
                onClick = onRestoreDraft,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(Res.string.quick_create_classic_restore_draft),
                    color = Primary300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * 将草稿摘要的稳定结构映射为旧版编辑器展示文案。
 *
 * 草稿摘要的业务推导保留在 feature presentation 模块；这里仅完成 Compose Resources
 * 本地化映射，确保后续多语言或文案调整不需要改动业务状态模型。
 */
@Composable
private fun quickCreateDraftResumeSummaryText(summary: QuickCreateDraftResumeSummary): String {
    val tabLabel = when (summary.tab) {
        QuickCreateTab.IMAGE -> stringResource(Res.string.quick_create_classic_draft_image_tab)
        QuickCreateTab.VIDEO -> stringResource(Res.string.quick_create_classic_draft_video_tab)
    }
    return stringResource(
        Res.string.quick_create_classic_draft_resume_summary_format,
        tabLabel,
        summary.promptLength,
    )
}

/**
 * 渲染图片/视频创作模式切换 pill。
 *
 * 该控件仅发送 tab 切换动作，不在 UI 层保存额外选中状态，确保当前 tab 仍以
 * [QuickCreateUiState.currentTab] 为单一事实来源。
 */
@Composable
private fun TabPillRow(
    selectedTab: QuickCreateTab,
    onTabSelected: (QuickCreateTab) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        QuickCreateTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Surface(
                onClick = { onTabSelected(tab) },
                color = if (selected) Primary300.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(Dimens.RadiusFull),
                border = if (selected) BorderStroke(1.dp, Primary300.copy(alpha = 0.45f)) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        if (tab == QuickCreateTab.IMAGE) Icons.Default.Image else Icons.Default.Videocam,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selected) Primary300 else Neutral500,
                    )
                    Text(
                        quickCreateNavigationText(tab.navigationLabel),
                        fontSize = 12.sp,
                        color = if (selected) Color.White else Neutral500,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/**
 * 汇总当前服务端模型和计费预览状态。
 *
 * 模型详情和价格都由上游状态提供；本控件保留“运行前确认费用”的降级展示，
 * 避免计费预览失败时在旧版编辑器里误导用户认为本次生成免费。
 */
@Composable
private fun ServiceModelSummaryRow(
    model: QuickCreateServiceModelUi?,
    loading: Boolean,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    billingPreview: QuickCreateBillingPreviewUi?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusMD),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = Primary300,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        model != null -> model.displayName.asServiceModelText()
                        else -> stringResource(Res.string.quick_create_classic_default_model)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Neutral100,
                    maxLines = 1,
                )
                val subtitle = when {
                    model != null -> model.subtitle.asServiceModelText()
                    else -> stringResource(Res.string.quick_create_classic_default_model_subtitle)
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Neutral500,
                        maxLines = 1,
                    )
                }
            }
            val priceText = when {
                feePreviewLoading -> stringResource(Res.string.quick_create_classic_price_refreshing)
                feePreviewError != null -> stringResource(Res.string.quick_create_classic_price_pending)
                billingPreview == null -> stringResource(Res.string.quick_create_classic_price_pending)
                billingPreview.free -> null
                else -> billingPreview.quickCreateBillingAmountText()
                    ?: stringResource(Res.string.quick_create_classic_price_pending)
            }
            priceText?.let {
                Surface(
                    color = Primary300.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    border = BorderStroke(1.dp, Primary300.copy(alpha = 0.28f)),
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary300,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Neutral500,
            )
        }
    }
}

/**
 * 渲染旧版编辑器的生成按钮。
 *
 * 按钮文案复用统一的计费展示逻辑：价格刷新中、计费失败和实际金额都会影响文案，
 * 从而避免旧版布局与紧凑输入条在余额/计费状态上出现不同解释。
 */
@Composable
private fun SendButton(
    enabled: Boolean,
    isLoading: Boolean,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    billingPreview: QuickCreateBillingPreviewUi?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                brush = if (enabled) {
                    Brush.linearGradient(listOf(Primary300, Secondary500))
                } else {
                    Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant))
                },
                shape = RoundedCornerShape(Dimens.RadiusMD),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = if (enabled && !isLoading) onClick else { {} },
            shape = RoundedCornerShape(Dimens.RadiusMD),
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val sendButtonLabel = quickCreateSendButtonLabel(
                    feePreviewLoading = feePreviewLoading,
                    feePreviewError = feePreviewError,
                    billingPreview = billingPreview,
                )
                val confirmingFee = sendButtonLabel == QuickCreateSendButtonLabel.Confirming
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else if (confirmingFee) {
                    // 价格预览中只展示环形加载态，避免旧版按钮继续用“价格确认中”文案占位。
                    FeePreviewLoadingIndicator()
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(
                            Res.string.quick_create_classic_generate_content_description,
                        ),
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
                if (!confirmingFee) {
                    Spacer(Modifier.width(6.dp))
                    val sendLabel = quickCreateSendButtonText(sendButtonLabel)
                    if (sendLabel.isNotBlank()) {
                        Text(
                            sendLabel,
                            fontSize = if (sendButtonLabel == QuickCreateSendButtonLabel.Generate) 14.sp else 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color.White else Neutral500,
                            maxLines = 1,
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.quick_create_classic_generate_button_label),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color.White else Neutral500,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeePreviewLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(18.dp),
        color = Color.White,
        strokeWidth = 2.dp,
    )
}

@Composable
private fun quickCreateSendButtonText(label: QuickCreateSendButtonLabel): String =
    when (label) {
        QuickCreateSendButtonLabel.Confirming -> stringResource(Res.string.quick_create_send_fee_confirming)
        QuickCreateSendButtonLabel.Pending -> stringResource(Res.string.quick_create_send_fee_pending)
        QuickCreateSendButtonLabel.Generate -> stringResource(Res.string.quick_create_send_generate)
        is QuickCreateSendButtonLabel.Amount -> quickCreateBillingAmountText(label.billingAmount)
    }
