package com.runninghub.app.ui.feature.quickcreate.presentation.editor.params

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelector
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorOptionState
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorOptionVisualState
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorState
import com.runninghub.app.ui.designsystem.components.sheets.AdvancedSettingsSectionState
import com.runninghub.app.ui.designsystem.components.sheets.AdvancedSettingsSheet
import com.runninghub.app.ui.designsystem.components.sheets.AdvancedSettingsSheetState
import com.runninghub.app.ui.feature.quickcreate.QuickCreateDesignTokens
import com.runninghub.app.ui.feature.quickcreate.QuickCreateModelGlyph
import com.runninghub.app.ui.feature.quickcreate.QuickCreateSheetHandle
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.app.ui.feature.quickcreate.presentation.upload.QuickCreateServiceUploadFieldPicker
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldControlType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldDisplayLabel
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldOptionVisualState
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUi
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceUploadMediaType
import com.runninghub.feature.quickcreate.presentation.fields.quickCreateParameterSheetState
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_params_close_content_description
import runninghub.composeapp.generated.resources.quick_create_params_common_title
import runninghub.composeapp.generated.resources.quick_create_params_done
import runninghub.composeapp.generated.resources.quick_create_params_empty_parameters
import runninghub.composeapp.generated.resources.quick_create_params_advanced_title
import runninghub.composeapp.generated.resources.quick_create_params_advanced_expand
import runninghub.composeapp.generated.resources.quick_create_params_advanced_collapse
import runninghub.composeapp.generated.resources.quick_create_params_model_label
import runninghub.composeapp.generated.resources.quick_create_params_sheet_title
import runninghub.composeapp.generated.resources.quick_create_params_upload_formats_format
import runninghub.composeapp.generated.resources.quick_create_params_upload_hint_separator
import runninghub.composeapp.generated.resources.quick_create_params_upload_max_count_format
import runninghub.composeapp.generated.resources.quick_create_params_upload_max_size_format
import runninghub.composeapp.generated.resources.quick_create_service_model_unnamed

internal val QuickCreateParamsSectionGap = 10.dp
internal val QuickCreateParamFieldMinHeight = 72.dp
internal val QuickCreateParamFieldPadding = 10.dp
internal val QuickCreateParamFieldGap = 8.dp

/**
 * 展示设计稿版快捷创作参数底部面板。
 *
 * 该组件渲染当前模型声明的全部可用服务字段，参数变更通过回调回到 ScreenModel。
 * 不再使用客户端硬编码参数兜底：模型有字段才显示，没有字段就展示空态。
 *
 * @param visible 是否显示底部面板。
 * @param isImage 当前是否编辑图片创作；`false` 时仍展示模型摘要和当前视频配置的只读降级面板。
 * @param uiState 快捷创作页面状态。
 * @param serviceFields 当前服务端动态字段列表，已按当前参数解析可见性和 child 激活状态。
 * @param onDismiss 关闭面板回调。
 * @param onImageServiceParamChange 图片服务动态字段变更回调。
 * @param onVideoServiceParamChange 视频服务动态字段变更回调。
 * @param onServiceUploadFieldClick 字段级上传回调。
 * @param onRemoveMedia 移除字段素材回调。
 * @param modifier 外层定位修饰符。
 * @param onSheetDragStart 用户按住顶部手柄开始拖动时触发。
 * @param onSheetDrag 顶部手柄拖动中的纵向像素变化，正数表示向下。
 * @param onSheetDragEnd 用户松手结束拖动时触发。
 * @param onSheetDragCancel 手柄拖动被系统取消时触发。
 */
@Composable
internal fun QuickCreateParamsSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    serviceFields: List<QuickCreationServiceFieldUi>,
    onDismiss: () -> Unit,
    onImageServiceParamChange: (String, String) -> Unit,
    onVideoServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit = { _, _ -> },
    onRemoveMedia: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onSheetDragStart: () -> Unit = {},
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: () -> Unit = {},
    onSheetDragCancel: () -> Unit = {},
) {
    var advancedExpanded by remember { mutableStateOf(false) }
    val parameterSheetState = quickCreateParameterSheetState(serviceFields)
    val mediaReferences = if (isImage) {
        uiState.imageConfig.mediaReferences
    } else {
        uiState.videoConfig.mediaReferences
    }
    val onParamChange: (String, String) -> Unit = { paramKey, value ->
        if (isImage) {
            onImageServiceParamChange(paramKey, value)
        } else {
            onVideoServiceParamChange(paramKey, value)
        }
    }
    val commonSection = AdvancedSettingsSectionState(
        id = "common",
        title = stringResource(Res.string.quick_create_params_common_title),
        selectors = emptyList(),
        contentCount = parameterSheetState.commonFields.size,
    )
    val advancedSection = AdvancedSettingsSectionState(
        id = "advanced",
        title = stringResource(Res.string.quick_create_params_advanced_title),
        selectors = emptyList(),
        collapsed = !advancedExpanded,
        contentCount = parameterSheetState.advancedFields.size,
    )
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        AdvancedSettingsSheet(
            state = AdvancedSettingsSheetState(
                title = stringResource(Res.string.quick_create_params_sheet_title),
                commonSection = commonSection,
                advancedSection = advancedSection,
                emptyText = stringResource(Res.string.quick_create_params_empty_parameters),
                doneLabel = stringResource(Res.string.quick_create_params_done),
                advancedToggleText = stringResource(
                    if (advancedExpanded) {
                        Res.string.quick_create_params_advanced_collapse
                    } else {
                        Res.string.quick_create_params_advanced_expand
                    },
                ),
                closeContentDescription = stringResource(
                    Res.string.quick_create_params_close_content_description,
                ),
            ),
            onOptionSelected = { paramKey, value -> onParamChange(paramKey, value) },
            onAdvancedToggle = { advancedExpanded = !advancedExpanded },
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            dragHandle = {
                QuickCreateSheetHandle(
                    onDragStart = onSheetDragStart,
                    onDrag = onSheetDrag,
                    onDragEnd = onSheetDragEnd,
                    onDragCancel = onSheetDragCancel,
                )
            },
            leadingContent = {
                ModelSummaryCard(uiState = uiState, isImage = isImage)
            },
            sectionContent = { section ->
                ServiceParamsSection(
                    serviceFields = if (section.id == "advanced") {
                        parameterSheetState.advancedFields
                    } else {
                        parameterSheetState.commonFields
                    },
                    mediaReferences = mediaReferences,
                    isImage = isImage,
                    onParamChange = onParamChange,
                    onServiceUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
            },
        )
    }
}

@Composable
private fun ModelSummaryCard(uiState: QuickCreateUiState, isImage: Boolean) {
    val selectedServiceModel = if (isImage) {
        uiState.selectedImageServiceModelUi
    } else {
        uiState.selectedVideoServiceModelUi
    }
    val modelName = selectedServiceModel?.displayName?.asServiceModelText()
        ?: stringResource(Res.string.quick_create_service_model_unnamed)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF15181D),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickCreateModelGlyph(modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    tint = QuickCreateDesignTokens.Text,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = modelName,
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.quick_create_params_model_label),
                    color = QuickCreateDesignTokens.Muted,
                    fontSize = 12.sp,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = QuickCreateDesignTokens.Text,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ServiceParamsSection(
    serviceFields: List<QuickCreationServiceFieldUi>,
    mediaReferences: List<MediaReference>,
    isImage: Boolean,
    onParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    val flattenedFields = serviceFields.flattenServiceFields()
        .filterNot { field -> field.isPromptParameterField() || field.isTechnicalEndpointField() }
    if (flattenedFields.isEmpty()) {
        EmptyParamsCard()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(QuickCreateParamsSectionGap)) {
        flattenedFields.forEach { field ->
            ServiceParamCard(
                field = field,
                mediaReferences = mediaReferences,
                isImage = isImage,
                onParamChange = onParamChange,
                onServiceUploadFieldClick = onServiceUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
    }
}

@Composable
private fun EmptyParamsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF13161B),
    ) {
        Text(
            text = stringResource(Res.string.quick_create_params_empty_parameters),
            color = QuickCreateDesignTokens.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun ServiceParamCard(
    field: QuickCreationServiceFieldUi,
    mediaReferences: List<MediaReference>,
    isImage: Boolean,
    onParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    when (field.controlType) {
        QuickCreationServiceFieldControlType.OPTIONS -> {
            if (field.options.isEmpty()) {
                ParamField(title = field.title, modifier = Modifier.fillMaxWidth()) {
                    ServiceOptionsControl(
                        field = field,
                        onParamChange = onParamChange,
                    )
                }
            } else {
                ServiceOptionsControl(
                    field = field,
                    onParamChange = onParamChange,
                )
            }
        }
        QuickCreationServiceFieldControlType.TEXT -> ParamField(
            title = field.title,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ServiceTextControl(
                field = field,
                onParamChange = onParamChange,
            )
        }
        QuickCreationServiceFieldControlType.UPLOAD -> ParamField(
            title = field.title,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ServiceUploadControl(
                field = field,
                mediaReferences = mediaReferences,
                onServiceUploadFieldClick = onServiceUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
    }
}

@Composable
private fun ServiceOptionsControl(
    field: QuickCreationServiceFieldUi,
    onParamChange: (String, String) -> Unit,
) {
    if (field.options.isEmpty()) {
        Text(
            text = field.textValue,
            color = Color(0xFFD2D3D8),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    ParameterSelector(
        state = ParameterSelectorState(
            id = field.paramKey,
            title = field.title,
            options = field.options.map { option ->
                ParameterSelectorOptionState(
                    id = option.value,
                    label = option.label.ifBlank { option.value },
                    visualState = option.visualState.toParameterSelectorVisualState(),
                )
            },
        ),
        onOptionSelected = onParamChange,
    )
}

@Composable
private fun ServiceTextControl(
    field: QuickCreationServiceFieldUi,
    onParamChange: (String, String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(38.dp),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF1C1E23),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = field.textValue,
                onValueChange = { value ->
                    onParamChange(field.paramKey, field.constrainTextInput(value))
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(QuickCreateDesignTokens.Purple),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (field.textValue.isBlank()) {
                            Text(
                                text = field.placeholder,
                                color = QuickCreateDesignTokens.Muted,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            field.textLimitCounter?.let { counter ->
                Text(
                    text = counter,
                    color = QuickCreateDesignTokens.Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ServiceUploadControl(
    field: QuickCreationServiceFieldUi,
    mediaReferences: List<MediaReference>,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    QuickCreateServiceUploadFieldPicker(
        paramKey = field.paramKey,
        mediaType = field.uploadMediaType.toQuickCreateMediaType(),
        hint = field.uploadHint.asUploadHintText(),
        mediaReferences = mediaReferences,
        onUploadFieldClick = onServiceUploadFieldClick,
        onRemoveMedia = onRemoveMedia,
    )
}

@Composable
private fun ParamField(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = QuickCreateParamFieldMinHeight),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF13161B),
    ) {
        Column(
            modifier = Modifier.padding(QuickCreateParamFieldPadding),
            verticalArrangement = Arrangement.spacedBy(QuickCreateParamFieldGap),
        ) {
            Text(
                text = title,
                color = QuickCreateDesignTokens.Text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            content()
        }
    }
}

@Composable
private fun com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceUploadHint.asUploadHintText(): String =
    listOfNotNull(
        acceptFormats.takeIf { it.isNotEmpty() }?.joinToString(", ")?.let { formats ->
            stringResource(Res.string.quick_create_params_upload_formats_format, formats)
        },
        maxUploadCount?.let { count ->
            stringResource(Res.string.quick_create_params_upload_max_count_format, count)
        },
        maxUploadSizeMegabytes?.let { size ->
            stringResource(Res.string.quick_create_params_upload_max_size_format, size)
        },
    ).joinToString(stringResource(Res.string.quick_create_params_upload_hint_separator))

internal fun QuickCreationServiceUploadMediaType?.toQuickCreateMediaType(): QuickCreateMediaType? =
    when (this) {
        QuickCreationServiceUploadMediaType.IMAGE -> QuickCreateMediaType.IMAGE
        QuickCreationServiceUploadMediaType.VIDEO -> QuickCreateMediaType.VIDEO
        QuickCreationServiceUploadMediaType.AUDIO -> QuickCreateMediaType.AUDIO
        null -> null
    }

private fun QuickCreationServiceFieldOptionVisualState.toParameterSelectorVisualState():
    ParameterSelectorOptionVisualState =
    when (this) {
        QuickCreationServiceFieldOptionVisualState.DEFAULT -> ParameterSelectorOptionVisualState.DEFAULT
        QuickCreationServiceFieldOptionVisualState.SELECTED -> ParameterSelectorOptionVisualState.SELECTED
        QuickCreationServiceFieldOptionVisualState.DISABLED -> ParameterSelectorOptionVisualState.DISABLED
        QuickCreationServiceFieldOptionVisualState.ERROR -> ParameterSelectorOptionVisualState.ERROR
    }

private fun List<QuickCreationServiceFieldUi>.flattenServiceFields(): List<QuickCreationServiceFieldUi> =
    flatMap { field ->
        listOf(field) + field.childFields.flattenServiceFields()
    }

private fun QuickCreationServiceFieldUi.isPromptParameterField(): Boolean {
    val normalizedKey = paramKey.trim().lowercase()
    return normalizedKey == "prompt" ||
        normalizedKey == "text" ||
        normalizedKey == "input" ||
        normalizedKey == "positiveprompt" ||
        normalizedKey == "positive_prompt"
}

private fun QuickCreationServiceFieldUi.isTechnicalEndpointField(): Boolean =
    displayLabel == QuickCreationServiceFieldDisplayLabel.TechnicalEndpoint
