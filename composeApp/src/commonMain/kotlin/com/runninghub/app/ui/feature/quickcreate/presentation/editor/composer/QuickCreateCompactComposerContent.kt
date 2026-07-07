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
import com.runninghub.app.ui.designsystem.components.segmented.RhSegmentedControl
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.feature.quickcreate.QuickCreateCatThumbnail
import com.runninghub.app.ui.feature.quickcreate.quickCreateBillingAmountText
import com.runninghub.app.ui.feature.quickcreate.quickCreateNavigationText
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateBillingPreviewUi
import com.runninghub.feature.quickcreate.presentation.billing.QuickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.billing.quickCreateSendButtonLabel
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.fields.quickCreationServiceFieldUiItems
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateCompactServiceModelLabel
import com.runninghub.feature.quickcreate.presentation.modelcatalog.QuickCreateServiceModelUi
import com.runninghub.feature.quickcreate.presentation.modelcatalog.quickCreateCompactServiceModelLabel
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.state.navigationLabel
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_compact_add_media_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_char_count_format
import runninghub.composeapp.generated.resources.quick_create_compact_generate_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_generate_with_detail_format
import runninghub.composeapp.generated.resources.quick_create_compact_image_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_compact_model_loading
import runninghub.composeapp.generated.resources.quick_create_compact_model_unavailable
import runninghub.composeapp.generated.resources.quick_create_compact_prompt_required_hint
import runninghub.composeapp.generated.resources.quick_create_compact_remove_media_content_description
import runninghub.composeapp.generated.resources.quick_create_compact_video_prompt_placeholder
import runninghub.composeapp.generated.resources.quick_create_params_empty_parameters
import runninghub.composeapp.generated.resources.quick_create_send_fee_confirming
import runninghub.composeapp.generated.resources.quick_create_send_fee_pending
import runninghub.composeapp.generated.resources.quick_create_send_generate
import runninghub.composeapp.generated.resources.quick_create_service_model_parameter_count_format

internal val CompactMediaStripHeight = 96.dp
internal val CompactMediaSlotSize = 88.dp

/**
 * 渲染快捷创作当前默认启用的设计稿版底部输入条。
 *
 * 组件只负责提示词、已选素材预览、模型入口、参数入口和生成按钮的可视化；模型选择、
 * 媒体选择、参数编辑和任务提交仍通过回调交给 ScreenModel，避免 Composable 直接触碰业务逻辑。
 * 空输入时右侧主按钮承担当前 tab 的素材上传入口；已有素材时，上方只展示已选素材和紧邻的追加入口。
 * 已选图片优先使用本地 URI 做即时预览，避免远端上传地址暂不可读时出现空缩略图。
 *
 * @param uiState 快捷创作页面状态，用于读取当前 Sheet、计费预览和本地参数摘要。
 * @param isImage 当前是否处于图片创作；`false` 表示视频创作。
 * @param prompt 当前提示词。
 * @param onPromptChange 提示词变更回调。
 * @param charCount 当前提示词字符数。
 * @param nearLimit `true` 表示接近字数上限，计数文案转为警示色。
 * @param overLimit `true` 表示超过字数上限，输入栏描边转为错误色。
 * @param mediaReferences 当前全局素材列表，空列表表示没有素材缩略图。
 * @param selectedServiceModel 当前选中的服务端模型摘要；为空时使用本地兼容模型名。
 * @param serviceModelsLoading `true` 表示模型目录加载中。
 * @param isTaskActive `true` 表示任务提交、排队或运行中，生成按钮显示加载态。
 * @param canGenerate `true` 表示当前允许提交任务。
 * @param onTabSwitch 切换图片/视频创作类型的回调。
 * @param onLaunchImagePicker 图片 tab 添加素材回调。
 * @param onLaunchVideoPicker 视频 tab 添加素材回调。
 * @param onRemoveMedia 移除素材回调。
 * @param onOpenModelSheet 打开模型选择面板。
 * @param onOpenParamsSheet 打开参数面板。
 * @param onGenerate 提交生成任务。
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
    onLaunchVideoPicker: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onGenerate: () -> Unit,
) {
    val hasPrompt = prompt.isNotBlank()
    val showMediaUploadStrip = mediaReferences.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val onAddMedia = when (compactMediaAddAction(isImage)) {
        CompactMediaAddAction.Image -> onLaunchImagePicker
        CompactMediaAddAction.Video -> onLaunchVideoPicker
    }

    LaunchedEffect(isTaskActive) {
        if (isTaskActive) {
            // 生成提交后输入框内容会被清空，这里同步释放焦点，避免输入法继续把输入条顶到高位。
            focusManager.clearFocus(force = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .navigationBarsPadding()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showMediaUploadStrip) {
            CompactMediaUploadStrip(
                mediaReferences = mediaReferences,
                onAdd = onAddMedia,
                onRemove = onRemoveMedia,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = RhTheme.colors.overlaySheet,
            shape = RoundedCornerShape(18.dp),
            border = if (overLimit) BorderStroke(1.dp, RhTheme.colors.statusFailed) else null,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RhSegmentedControl(
                    options = listOf(
                        quickCreateNavigationText(QuickCreateTab.IMAGE.navigationLabel),
                        quickCreateNavigationText(QuickCreateTab.VIDEO.navigationLabel),
                    ),
                    selectedIndex = if (isImage) 0 else 1,
                    onSelect = { index ->
                        onTabSwitch(if (index == 0) QuickCreateTab.IMAGE else QuickCreateTab.VIDEO)
                    },
                )
                CompactPromptField(
                    prompt = prompt,
                    onPromptChange = onPromptChange,
                    charCount = charCount,
                    nearLimit = nearLimit,
                    overLimit = overLimit,
                    showRequiredHint = !hasPrompt,
                    placeholder = if (isImage) {
                        stringResource(Res.string.quick_create_compact_image_prompt_placeholder)
                    } else {
                        stringResource(Res.string.quick_create_compact_video_prompt_placeholder)
                    },
                )
                CompactControlRow(
                    modelText = quickCreateCompactServiceModelLabel(
                        model = selectedServiceModel,
                        loading = serviceModelsLoading,
                    ).asCompactModelText(),
                    paramsText = compactParamsSummary(
                        selectedServiceModel = selectedServiceModel,
                        serviceModelsLoading = serviceModelsLoading,
                        serviceParams = if (isImage) uiState.imageServiceParams else uiState.videoServiceParams,
                    ),
                    modelSelected = uiState.activeSheet == QuickCreateSheet.MODEL_PICKER,
                    paramsSelected = uiState.activeSheet == QuickCreateSheet.PARAMS,
                    hasPrompt = hasPrompt,
                    enabled = if (hasPrompt) canGenerate else !isTaskActive,
                    isLoading = isTaskActive,
                    feePreviewLoading = uiState.feePreviewLoading,
                    feePreviewError = uiState.feePreviewError,
                    billingPreview = uiState.billingPreview,
                    onOpenModelSheet = onOpenModelSheet,
                    onOpenParamsSheet = onOpenParamsSheet,
                    onGenerate = onGenerate,
                    onLaunchMediaPicker = onAddMedia,
                )
            }
        }
    }

}

/**
 * 渲染紧凑输入栏底部的模型、参数和执行入口。
 *
 * 模型名称来自服务端目录，计费文案也会随预估状态变化；三个控件必须共享剩余宽度，
 * 避免窄屏或重组测量时把右侧执行入口挤压成不可点击的细条。
 */
@Composable
private fun CompactControlRow(
    modelText: String,
    paramsText: String,
    modelSelected: Boolean,
    paramsSelected: Boolean,
    hasPrompt: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    billingPreview: QuickCreateBillingPreviewUi?,
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onGenerate: () -> Unit,
    onLaunchMediaPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactControlPill(
            text = modelText,
            selected = modelSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = RhTheme.colors.textPrimary,
                )
            },
            onClick = onOpenModelSheet,
            modifier = Modifier.weight(1f),
        )
        CompactControlPill(
            text = paramsText,
            selected = paramsSelected,
            onClick = onOpenParamsSheet,
            modifier = Modifier.weight(0.72f),
        )
        CompactGenerateButton(
            hasPrompt = hasPrompt,
            enabled = enabled,
            isLoading = isLoading,
            feePreviewLoading = feePreviewLoading,
            feePreviewError = feePreviewError,
            billingPreview = billingPreview,
            onGenerate = onGenerate,
            onAddMedia = onLaunchMediaPicker,
        )
    }
}

@Composable
private fun CompactPromptField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    showRequiredHint: Boolean,
    placeholder: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    color = RhTheme.colors.textSecondary,
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
                    color = RhTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(RhTheme.colors.brandSecondary),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showRequiredHint) {
                Text(
                    text = stringResource(Res.string.quick_create_compact_prompt_required_hint),
                    color = RhTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
            Text(
                text = stringResource(
                    Res.string.quick_create_compact_char_count_format,
                    charCount,
                    MAX_PROMPT_CHARS,
                ),
                color = compactPromptCharCountColor(
                    nearLimit = nearLimit,
                    overLimit = overLimit,
                ),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun compactPromptCharCountColor(
    nearLimit: Boolean,
    overLimit: Boolean,
): Color =
    when {
        overLimit -> RhTheme.colors.statusFailed
        nearLimit -> RhTheme.colors.statusWarning
        else -> RhTheme.colors.textSecondary
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
            .height(CompactMediaStripHeight)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mediaReferences.forEach { reference ->
            CompactMediaPreviewSlot(
                reference = reference,
                onRemove = onRemove,
                modifier = Modifier.size(CompactMediaSlotSize),
            )
        }
        CompactMediaAddSlot(
            onAdd = onAdd,
            framed = mediaReferences.isNotEmpty(),
            modifier = Modifier.size(CompactMediaSlotSize),
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
                            listOf(Color.Transparent, RhTheme.colors.overlayScrim.copy(alpha = 0.20f)),
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
                        color = RhTheme.colors.textPrimary,
                        strokeWidth = 2.dp,
                    )
                }
            }
            Surface(
                onClick = { onRemove(reference.id) },
                shape = CircleShape,
                color = RhTheme.colors.surfaceDefault,
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
                    tint = RhTheme.colors.textPrimary,
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
        border = BorderStroke(1.dp, RhTheme.colors.brandSecondary.copy(alpha = 0.54f)),
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
        color = RhTheme.colors.brandSecondary.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, RhTheme.colors.brandSecondary.copy(alpha = 0.72f)),
        modifier = Modifier.size(38.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(
                    Res.string.quick_create_compact_add_media_content_description,
                ),
                modifier = Modifier.size(22.dp),
                tint = RhTheme.colors.textPrimary,
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
        color = if (selected) RhTheme.colors.brandMuted else RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (selected) RhTheme.colors.brandSecondary else RhTheme.colors.borderDefault,
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
                color = RhTheme.colors.textPrimary,
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
                tint = RhTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun CompactGenerateButton(
    hasPrompt: Boolean,
    enabled: Boolean,
    isLoading: Boolean,
    feePreviewLoading: Boolean,
    feePreviewError: QuickCreateUiMessage?,
    billingPreview: QuickCreateBillingPreviewUi?,
    onGenerate: () -> Unit,
    onAddMedia: () -> Unit,
) {
    val primaryAction = compactPrimaryAction(hasPrompt)
    val buttonClick = when (primaryAction) {
        CompactPrimaryAction.AddMedia -> onAddMedia
        CompactPrimaryAction.Generate -> onGenerate
    }
    val buttonShape = if (hasPrompt) RoundedCornerShape(18.dp) else CircleShape
    val sendButtonLabel = quickCreateSendButtonLabel(
        feePreviewLoading = feePreviewLoading,
        feePreviewError = feePreviewError,
        billingPreview = billingPreview,
    )
    val buttonModifier = if (hasPrompt) {
        Modifier
            .height(36.dp)
            .widthIn(min = 76.dp)
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
                .background(RhTheme.colors.brandPrimary, buttonShape)
                .padding(horizontal = if (hasPrompt) 12.dp else 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    color = RhTheme.colors.textInverse,
                    strokeWidth = 2.dp,
                )
            } else if (!hasPrompt) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(
                        Res.string.quick_create_compact_add_media_content_description,
                    ),
                    modifier = Modifier.size(18.dp),
                    tint = RhTheme.colors.textInverse,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(Res.string.quick_create_compact_generate_content_description),
                    modifier = Modifier.size(15.dp),
                    tint = RhTheme.colors.textInverse,
                )
            }
            if (hasPrompt) {
                Text(
                    text = quickCreateCompactGenerateButtonText(sendButtonLabel),
                    color = RhTheme.colors.textInverse,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

internal enum class CompactPrimaryAction {
    AddMedia,
    Generate,
}

internal enum class CompactMediaAddAction {
    Image,
    Video,
}

internal enum class CompactGenerateTextMode {
    GenerateOnly,
    GenerateWithDetail,
}

internal fun compactPrimaryAction(hasPrompt: Boolean): CompactPrimaryAction =
    if (hasPrompt) {
        CompactPrimaryAction.Generate
    } else {
        CompactPrimaryAction.AddMedia
    }

internal fun compactMediaAddAction(isImage: Boolean): CompactMediaAddAction =
    if (isImage) {
        CompactMediaAddAction.Image
    } else {
        CompactMediaAddAction.Video
    }

internal fun compactGenerateTextMode(
    label: QuickCreateSendButtonLabel,
): CompactGenerateTextMode =
    when (label) {
        QuickCreateSendButtonLabel.Generate -> CompactGenerateTextMode.GenerateOnly
        QuickCreateSendButtonLabel.Confirming,
        QuickCreateSendButtonLabel.Pending,
        is QuickCreateSendButtonLabel.Amount,
        -> CompactGenerateTextMode.GenerateWithDetail
    }

@Composable
private fun quickCreateSendButtonText(label: QuickCreateSendButtonLabel): String =
    when (label) {
        QuickCreateSendButtonLabel.Confirming -> stringResource(Res.string.quick_create_send_fee_confirming)
        QuickCreateSendButtonLabel.Pending -> stringResource(Res.string.quick_create_send_fee_pending)
        QuickCreateSendButtonLabel.Generate -> stringResource(Res.string.quick_create_send_generate)
        is QuickCreateSendButtonLabel.Amount -> quickCreateBillingAmountText(label.billingAmount)
    }

@Composable
private fun quickCreateCompactGenerateButtonText(label: QuickCreateSendButtonLabel): String =
    when (compactGenerateTextMode(label)) {
        CompactGenerateTextMode.GenerateOnly -> quickCreateSendButtonText(label)
        CompactGenerateTextMode.GenerateWithDetail -> stringResource(
            Res.string.quick_create_compact_generate_with_detail_format,
            quickCreateSendButtonText(label),
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
        QuickCreateCompactServiceModelLabel.Unavailable ->
            stringResource(Res.string.quick_create_compact_model_unavailable)
    }

@Composable
private fun compactParamsSummary(
    selectedServiceModel: QuickCreateServiceModelUi?,
    serviceModelsLoading: Boolean,
    serviceParams: Map<String, String>,
): String {
    if (selectedServiceModel == null) {
        return if (serviceModelsLoading) {
            stringResource(Res.string.quick_create_compact_model_loading)
        } else {
            stringResource(Res.string.quick_create_compact_model_unavailable)
        }
    }
    val parameterCount = selectedServiceModel.source
        .quickCreationServiceFieldUiItems(params = serviceParams)
        .size
    return if (parameterCount > 0) {
        stringResource(Res.string.quick_create_service_model_parameter_count_format, parameterCount)
    } else {
        stringResource(Res.string.quick_create_params_empty_parameters)
    }
}
