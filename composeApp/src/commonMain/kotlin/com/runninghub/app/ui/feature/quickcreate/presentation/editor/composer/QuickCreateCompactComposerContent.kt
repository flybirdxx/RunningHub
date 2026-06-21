package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Neutral100
import com.runninghub.app.ui.theme.Neutral300
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.WarningDark
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.modelcatalog.quickCreateCompactServiceModelLabel
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_compact_add_media_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_char_count_format
import runninghub.composeapp.generated.resources.quick_create_compact_generate_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_image_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_compact_image_tab
import runninghub.composeapp.generated.resources.quick_create_compact_media_count_format
import runninghub.composeapp.generated.resources.quick_create_compact_video_params_summary
import runninghub.composeapp.generated.resources.quick_create_compact_video_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_compact_video_tab

/**
 * 渲染快捷创作当前默认启用的紧凑底部输入条。
 *
 * 该组件属于 editor/composer 子区域，封装提示词输入、全局素材入口、模型入口、
 * 参数入口和生成按钮。它只接收 [QuickCreateUiState] 的派生状态和用户行为回调，
 * 不直接访问 Repository、存储、平台媒体选择器或上传任务，保持 UI -> Action 的边界。
 *
 * @param uiState 快捷创作页面状态，用于读取当前选中的 Sheet、计费预览和模型默认展示名。
 * @param isImage 当前是否处于图片创作 tab；`false` 表示视频创作 tab。
 * @param prompt 当前 tab 的提示词文本。
 * @param onPromptChange 用户输入提示词时触发，参数为受长度限制后的最新文本。
 * @param charCount 当前提示词字符数，用于临近上限时展示计数。
 * @param nearLimit `true` 表示提示词接近字符上限，应展示黄色计数提示。
 * @param overLimit `true` 表示提示词超过限制，应展示错误边框和红色计数提示。
 * @param mediaReferences 当前 tab 的全局媒体引用列表，不包含字段级上传素材。
 * @param selectedServiceModel 当前服务端模型 UI 摘要；为空时显示本地兼容模型名称。
 * @param serviceModelsLoading `true` 表示服务端模型目录仍在加载。
 * @param isTaskActive `true` 表示生成任务正在提交、排队或运行，输入条应禁用提交相关动作。
 * @param canGenerate `true` 表示当前提示词、计费预览和任务状态允许提交生成。
 * @param onTabSwitch 用户切换图片/视频 tab 时触发。
 * @param onLaunchImagePicker 用户点击添加素材入口时触发；保留既有行为，不在本轮改动中区分视频入口。
 * @param onRemoveMedia 用户点击素材计数 pill 时移除第一个全局素材。
 * @param onOpenModelSheet 用户打开模型选择面板时触发。
 * @param onOpenParamsSheet 用户打开参数面板时触发。
 * @param onGenerate 用户点击生成按钮时触发。
 */
@Composable
internal fun QuickCreateCompactComposer(
    uiState: QuickCreateUiState,
    isImage: Boolean,
    prompt: String,
    onPromptChange: (String) -> Unit,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    mediaReferences: List<MediaReference>,
    selectedServiceModel: QuickCreateServiceModelUi?,
    serviceModelsLoading: Boolean,
    isTaskActive: Boolean,
    canGenerate: Boolean,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onGenerate: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceMD)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = Dimens.SpaceMD),
        color = DarkSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(Dimens.RadiusXL),
        border = BorderStroke(1.dp, if (overLimit) ErrorDark else DarkOutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                CompactPromptField(
                    prompt = prompt,
                    onPromptChange = onPromptChange,
                    placeholder = if (isImage) {
                        stringResource(Res.string.quick_create_compact_image_prompt_placeholder)
                    } else {
                        stringResource(Res.string.quick_create_compact_video_prompt_placeholder)
                    },
                    modifier = Modifier.weight(1f),
                )
                CompactIconAction(
                    icon = if (isImage) Icons.Default.AddPhotoAlternate else Icons.Default.Image,
                    contentDescription = stringResource(Res.string.quick_create_compact_add_media_content_description),
                    highlighted = mediaReferences.isNotEmpty(),
                    enabled = !isTaskActive,
                    onClick = onLaunchImagePicker,
                )
                CompactGenerateButton(
                    enabled = canGenerate,
                    isLoading = isTaskActive,
                    cost = uiState.estimatedCost,
                    feePreviewLoading = uiState.feePreviewLoading,
                    feePreviewError = uiState.feePreviewError,
                    onClick = onGenerate,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                CompactControlPill(
                    text = if (isImage) {
                        stringResource(Res.string.quick_create_compact_image_tab)
                    } else {
                        stringResource(Res.string.quick_create_compact_video_tab)
                    },
                    icon = if (isImage) Icons.Default.Image else Icons.Default.Videocam,
                    selected = true,
                    onClick = {
                        onTabSwitch(if (isImage) QuickCreateTab.VIDEO else QuickCreateTab.IMAGE)
                    },
                )
                CompactControlPill(
                    text = quickCreateCompactServiceModelLabel(
                        model = selectedServiceModel,
                        fallback = if (isImage) uiState.imageConfig.model.displayName else uiState.videoConfig.model.displayName,
                        loading = serviceModelsLoading,
                    ),
                    icon = Icons.Default.AutoAwesome,
                    selected = uiState.activeSheet == QuickCreateSheet.MODEL_PICKER,
                    onClick = onOpenModelSheet,
                )
                CompactControlPill(
                    text = compactParamsSummary(uiState, isImage),
                    icon = Icons.Default.Tune,
                    selected = uiState.activeSheet == QuickCreateSheet.PARAMS,
                    onClick = onOpenParamsSheet,
                )
                if (mediaReferences.isNotEmpty()) {
                    CompactControlPill(
                        text = stringResource(Res.string.quick_create_compact_media_count_format, mediaReferences.size),
                        icon = Icons.Default.AttachFile,
                        selected = true,
                        onClick = {
                            mediaReferences.firstOrNull()?.let { onRemoveMedia(it.id) }
                        },
                    )
                }
                if (nearLimit || overLimit) {
                    Text(
                        text = stringResource(
                            Res.string.quick_create_compact_char_count_format,
                            charCount,
                            MAX_PROMPT_CHARS,
                        ),
                        color = if (overLimit) ErrorDark else WarningDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactPromptField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = Neutral100,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )

    Box(
        modifier = modifier.heightIn(min = 36.dp, max = 72.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (prompt.isEmpty()) {
            Text(
                text = placeholder,
                color = Neutral500,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = prompt,
            onValueChange = { newValue ->
                if (newValue.length <= MAX_PROMPT_CHARS) {
                    onPromptChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            cursorBrush = SolidColor(Primary300),
        )
    }
}

@Composable
private fun CompactIconAction(
    icon: ImageVector,
    contentDescription: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (highlighted) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                shape = RoundedCornerShape(Dimens.RadiusMD),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = if (highlighted) Primary300 else Neutral300,
        )
    }
}

@Composable
private fun CompactGenerateButton(
    enabled: Boolean,
    isLoading: Boolean,
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = if (enabled && !isLoading) onClick else { {} },
        enabled = enabled || isLoading,
        color = if (enabled) Primary300 else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusFull),
        modifier = Modifier.height(40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    color = Neutral100,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.quick_create_compact_generate_content_description),
                    modifier = Modifier.size(17.dp),
                    tint = if (enabled) Color.White else Neutral500,
                )
            }
            Text(
                text = quickCreateSendButtonLabel(
                    cost = cost,
                    feePreviewLoading = feePreviewLoading,
                    feePreviewError = feePreviewError,
                ),
                color = if (enabled) Color.White else Neutral500,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactControlPill(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Primary300.copy(alpha = 0.11f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusFull),
        border = BorderStroke(
            1.dp,
            if (selected) Primary300.copy(alpha = 0.35f) else DarkOutlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) Primary300 else Neutral400,
            )
            Text(
                text = text,
                color = if (selected) Neutral100 else Neutral300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Neutral500,
            )
        }
    }
}

@Composable
private fun compactParamsSummary(uiState: QuickCreateUiState, isImage: Boolean): String =
    if (isImage) {
        uiState.imageConfig.aspectRatio.displayName
    } else {
        stringResource(Res.string.quick_create_compact_video_params_summary)
    }
