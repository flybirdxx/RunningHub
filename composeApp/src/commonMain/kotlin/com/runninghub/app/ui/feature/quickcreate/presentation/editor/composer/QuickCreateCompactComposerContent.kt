package com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.runninghub.app.ui.feature.quickcreate.QuickCreateCatThumbnail
import com.runninghub.app.ui.feature.quickcreate.QuickCreateDesignTokens
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.generation.quickCreateGenerationParameterSnapshot
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateCompactServiceModelLabel
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.modelcatalog.quickCreateCompactServiceModelLabel
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_compact_add_media_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_generate_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_image_params_format
import runninghub.composeapp.generated.resources.quick_create_compact_image_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_compact_model_loading
import runninghub.composeapp.generated.resources.quick_create_compact_remove_media_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_video_params_summary
import runninghub.composeapp.generated.resources.quick_create_compact_video_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_send_amount_format
import runninghub.composeapp.generated.resources.quick_create_send_fee_confirming
import runninghub.composeapp.generated.resources.quick_create_send_fee_pending
import runninghub.composeapp.generated.resources.quick_create_send_generate

/**
 * 渲染快捷创作当前默认启用的设计稿版底部输入条。
 *
 * 组件只负责提示词、已选素材预览、模型入口、参数入口和生成按钮的可视化；模型选择、
 * 媒体选择、参数编辑和任务提交仍通过回调交给 ScreenModel，避免 Composable 直接触碰业务逻辑。
 * 空输入时右侧主按钮承担图片上传入口；已有素材时，上方只展示已选素材和紧邻的追加入口。
 * 已选图片优先使用本地 URI 做即时预览，避免远端上传地址暂不可读时出现空缩略图。
 *
 * @param uiState 快捷创作页面状态，用于读取当前 Sheet、计费预览和本地参数摘要。
 * @param isImage 当前是否处于图片创作；`false` 表示视频创作。
 * @param prompt 当前提示词。
 * @param onPromptChange 提示词变更回调。
 * @param charCount 当前提示词字符数。
 * @param nearLimit `true` 表示接近字数上限；本布局暂不展示计数，只影响提交可用性。
 * @param overLimit `true` 表示超过字数上限，输入栏描边转为错误色。
 * @param mediaReferences 当前全局素材列表，空列表表示没有素材缩略图。
 * @param selectedServiceModel 当前选中的服务端模型摘要；为空时使用本地兼容模型名。
 * @param serviceModelsLoading `true` 表示模型目录加载中。
 * @param isTaskActive `true` 表示任务提交、排队或运行中，生成按钮显示加载态。
 * @param canGenerate `true` 表示当前允许提交任务。
 * @param onTabSwitch 切换图片/视频创作类型的回调。
 * @param onLaunchImagePicker 添加素材回调。
 * @param onRemoveMedia 移除素材回调。
 * @param onOpenModelSheet 打开模型选择面板。
 * @param onOpenParamsSheet 打开参数面板。
 * @param onGenerate 提交生成任务。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
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
    val hasPrompt = prompt.isNotBlank()
    val showMediaUploadStrip = mediaReferences.isNotEmpty()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isTaskActive) {
        if (isTaskActive) {
            // 生成提交后输入框内容会被清空，这里同步释放焦点，避免输入法继续把输入条顶到高位。
            focusManager.clearFocus(force = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showMediaUploadStrip) {
            CompactMediaUploadStrip(
                mediaReferences = mediaReferences,
                onAdd = onLaunchImagePicker,
                onRemove = onRemoveMedia,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = QuickCreateDesignTokens.Panel,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.dp,
                if (overLimit) Color(0xFFF87171) else QuickCreateDesignTokens.Stroke,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompactPromptField(
                    prompt = prompt,
                    onPromptChange = onPromptChange,
                    placeholder = if (isImage) {
                        stringResource(Res.string.quick_create_compact_image_prompt_placeholder)
                    } else {
                        stringResource(Res.string.quick_create_compact_video_prompt_placeholder)
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CompactControlPill(
                        text = quickCreateCompactServiceModelLabel(
                            model = selectedServiceModel,
                            fallback = if (isImage) {
                                uiState.imageConfig.model.label.asImageModelText()
                            } else {
                                uiState.videoConfig.model.label.asVideoModelText()
                            },
                            loading = serviceModelsLoading,
                        ).asCompactModelText(),
                        selected = uiState.activeSheet == QuickCreateSheet.MODEL_PICKER,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = QuickCreateDesignTokens.Text,
                            )
                        },
                        onClick = onOpenModelSheet,
                    )
                    CompactControlPill(
                        text = compactParamsSummary(uiState, isImage),
                        selected = uiState.activeSheet == QuickCreateSheet.PARAMS,
                        onClick = onOpenParamsSheet,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    CompactGenerateButton(
                        hasPrompt = hasPrompt,
                        enabled = if (hasPrompt) canGenerate else !isTaskActive,
                        isLoading = isTaskActive,
                        cost = uiState.estimatedCost,
                        feePreviewLoading = uiState.feePreviewLoading,
                        feePreviewError = uiState.feePreviewError,
                        onGenerate = onGenerate,
                        onAddMedia = onLaunchImagePicker,
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
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp, max = 72.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (prompt.isEmpty()) {
            Text(
                text = placeholder,
                color = QuickCreateDesignTokens.Muted,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = prompt,
            onValueChange = { value ->
                if (value.length <= MAX_PROMPT_CHARS) {
                    onPromptChange(value)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = QuickCreateDesignTokens.Text,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(QuickCreateDesignTokens.Purple),
        )
    }
}

@Composable
private fun CompactMediaUploadStrip(
    mediaReferences: List<MediaReference>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mediaReferences.forEach { reference ->
            CompactMediaPreviewSlot(
                reference = reference,
                onRemove = onRemove,
                modifier = Modifier.size(108.dp),
            )
        }
        CompactMediaAddSlot(
            onAdd = onAdd,
            framed = mediaReferences.isNotEmpty(),
            modifier = Modifier.size(108.dp),
        )
    }
}

@Composable
private fun CompactMediaPreviewSlot(
    reference: MediaReference,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewModel = reference.uri.takeIf { it.isNotBlank() }
        ?: reference.remoteUrl?.takeIf { it.isNotBlank() }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (reference.type == QuickCreateMediaType.IMAGE && previewModel != null) {
                AsyncImage(
                    model = previewModel,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                QuickCreateCatThumbnail(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x33000000)),
                        ),
                    ),
            )
            if (reference.uploadStatus == UploadStatus.UPLOADING) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = QuickCreateDesignTokens.Text,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Surface(
                onClick = { onRemove(reference.id) },
                shape = CircleShape,
                color = Color(0xDD11151A),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        Res.string.quick_create_compact_remove_media_content_description,
                    ),
                    modifier = Modifier.padding(2.dp),
                    tint = QuickCreateDesignTokens.Text,
                )
            }
        }
    }
}

@Composable
private fun CompactMediaAddSlot(
    onAdd: () -> Unit,
    framed: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!framed) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CompactMediaAddCircle(onAdd = onAdd)
        }
        return
    }

    Surface(
        onClick = onAdd,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Purple.copy(alpha = 0.54f)),
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CompactMediaAddCircle(onAdd = onAdd)
        }
    }
}

@Composable
private fun CompactMediaAddCircle(onAdd: () -> Unit) {
    Surface(
        onClick = onAdd,
        shape = CircleShape,
        color = QuickCreateDesignTokens.Purple.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Purple.copy(alpha = 0.72f)),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(
                    Res.string.quick_create_compact_add_media_content_description,
                ),
                modifier = Modifier.size(22.dp),
                tint = QuickCreateDesignTokens.Text,
            )
        }
    }
}

@Composable
private fun CompactControlPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF2A2240) else Color(0xFF24252A),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (selected) QuickCreateDesignTokens.Purple else QuickCreateDesignTokens.Stroke,
        ),
        modifier = modifier.height(36.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon?.invoke()
            Text(
                text = text,
                color = QuickCreateDesignTokens.Text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = if (icon == null) 104.dp else 132.dp),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = QuickCreateDesignTokens.Muted,
            )
        }
    }
}

@Composable
private fun CompactGenerateButton(
    hasPrompt: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    onGenerate: () -> Unit,
    onAddMedia: () -> Unit,
) {
    val buttonClick = if (hasPrompt) onGenerate else onAddMedia
    val buttonShape = if (hasPrompt) RoundedCornerShape(18.dp) else CircleShape
    val buttonModifier = if (hasPrompt) {
        Modifier
            .height(36.dp)
            .widthIn(min = 68.dp)
    } else {
        Modifier.size(36.dp)
    }
    Surface(
        onClick = if (enabled && !isLoading) buttonClick else { {} },
        enabled = enabled || isLoading,
        color = Color.Transparent,
        shape = buttonShape,
        modifier = buttonModifier,
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7556F6), Color(0xFF9B61FF)),
                    ),
                    buttonShape,
                )
                .padding(horizontal = if (hasPrompt) 12.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = QuickCreateDesignTokens.Text,
                    strokeWidth = 2.dp,
                )
            } else if (!hasPrompt) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(
                        Res.string.quick_create_compact_add_media_content_description,
                    ),
                    modifier = Modifier.size(18.dp),
                    tint = QuickCreateDesignTokens.Text,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.quick_create_compact_generate_content_description),
                    modifier = Modifier.size(15.dp),
                    tint = QuickCreateDesignTokens.Text,
                )
            }
            if (hasPrompt) {
                Text(
                    text = quickCreateSendButtonText(
                        quickCreateSendButtonLabel(
                            cost = cost,
                            feePreviewLoading = feePreviewLoading,
                            feePreviewError = feePreviewError,
                        ),
                    ),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun quickCreateSendButtonText(label: QuickCreateSendButtonLabel): String =
    when (label) {
        QuickCreateSendButtonLabel.Confirming -> stringResource(Res.string.quick_create_send_fee_confirming)
        QuickCreateSendButtonLabel.Pending -> stringResource(Res.string.quick_create_send_fee_pending)
        QuickCreateSendButtonLabel.Generate -> stringResource(Res.string.quick_create_send_generate)
        is QuickCreateSendButtonLabel.Amount -> stringResource(
            Res.string.quick_create_send_amount_format,
            label.cashAmount,
        )
    }

/**
 * 将 Presentation 层返回的模型标签语义映射为紧凑输入条的最终展示文本。
 *
 * 加载态使用资源化文案；服务端模型名和本地兼容模型名作为运行时业务数据原样展示。
 */
@Composable
private fun QuickCreateCompactServiceModelLabel.asCompactModelText(): String =
    when (this) {
        QuickCreateCompactServiceModelLabel.Loading ->
            stringResource(Res.string.quick_create_compact_model_loading)
        is QuickCreateCompactServiceModelLabel.ModelName -> value
        is QuickCreateCompactServiceModelLabel.FallbackName -> value
    }

@Composable
private fun compactParamsSummary(uiState: QuickCreateUiState, isImage: Boolean): String =
    if (isImage) {
        val parameterSnapshot = uiState.quickCreateGenerationParameterSnapshot()
        stringResource(
            Res.string.quick_create_compact_image_params_format,
            parameterSnapshot.aspectRatio,
            parameterSnapshot.resolution,
        )
    } else {
        stringResource(Res.string.quick_create_compact_video_params_summary)
    }
