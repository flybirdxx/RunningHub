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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.feature.quickcreate.QuickCreateDesignTokens
import com.runninghub.app.ui.feature.quickcreate.QuickCreateModelGlyph
import com.runninghub.app.ui.feature.quickcreate.QuickCreateSheetHandle
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.asImageModelText
import com.runninghub.app.ui.feature.quickcreate.presentation.upload.QuickCreateServiceUploadFieldPicker
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldControlType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUi
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceUploadMediaType
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_params_close_content_description
import runninghub.composeapp.generated.resources.quick_create_params_common_title
import runninghub.composeapp.generated.resources.quick_create_params_done
import runninghub.composeapp.generated.resources.quick_create_params_empty_parameters
import runninghub.composeapp.generated.resources.quick_create_params_endpoint_label
import runninghub.composeapp.generated.resources.quick_create_params_endpoint_value
import runninghub.composeapp.generated.resources.quick_create_params_model_label
import runninghub.composeapp.generated.resources.quick_create_params_parameter_count_format
import runninghub.composeapp.generated.resources.quick_create_params_sheet_title
import runninghub.composeapp.generated.resources.quick_create_params_upload_formats_format
import runninghub.composeapp.generated.resources.quick_create_params_upload_hint_separator
import runninghub.composeapp.generated.resources.quick_create_params_upload_max_count_format
import runninghub.composeapp.generated.resources.quick_create_params_upload_max_size_format

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
 * @param onImageModelSelected 本地兼容图片模型选择回调，本设计稿中不直接展示本地模型列表。
 * @param onImageServiceParamChange 图片服务动态字段变更回调。
 * @param onVideoServiceParamChange 视频服务动态字段变更回调。
 * @param onToggleRealistic 视频真人模式切换回调，本设计稿中不直接触发。
 * @param onImageSeedChange 图片 Seed 变更回调，本设计稿中不直接触发。
 * @param onVideoSeedChange 视频 Seed 变更回调，本设计稿中不直接触发。
 * @param onServiceUploadFieldClick 字段级上传回调。
 * @param onRemoveMedia 移除字段素材回调。
 * @param onImageRatioChange 图片宽高比变更回调。
 * @param onImageResChange 图片分辨率变更回调。
 * @param onImageQualityChange 图片质量变更回调。
 * @param onImageCountChange 图片生成数量变更回调。
 * @param modifier 外层定位修饰符。
 * @param onSheetDragStart 用户按住顶部手柄开始拖动时触发。
 * @param onSheetDrag 顶部手柄拖动中的纵向像素变化，正数表示向下。
 * @param onSheetDragEnd 用户松手结束拖动时触发。
 * @param onSheetDragCancel 手柄拖动被系统取消时触发。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun QuickCreateParamsSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    serviceFields: List<QuickCreationServiceFieldUi>,
    onDismiss: () -> Unit,
    onImageModelSelected: (ImageModel) -> Unit,
    onImageServiceParamChange: (String, String) -> Unit,
    onVideoServiceParamChange: (String, String) -> Unit,
    onToggleRealistic: () -> Unit,
    onImageSeedChange: (Int?) -> Unit = {},
    onVideoSeedChange: (Int?) -> Unit = {},
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit = { _, _ -> },
    onRemoveMedia: (String) -> Unit = {},
    onImageRatioChange: (ImageAspectRatio) -> Unit = {},
    onImageResChange: (ImageResolution) -> Unit = {},
    onImageQualityChange: (ImageQuality) -> Unit = {},
    onImageCountChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    onSheetDragStart: () -> Unit = {},
    onSheetDrag: (Float) -> Unit = {},
    onSheetDragEnd: () -> Unit = {},
    onSheetDragCancel: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = QuickCreateDesignTokens.PanelStrong,
            border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                QuickCreateSheetHandle(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onDragStart = onSheetDragStart,
                    onDrag = onSheetDrag,
                    onDragEnd = onSheetDragEnd,
                    onDragCancel = onSheetDragCancel,
                )
                ParamsSheetHeader(onDismiss = onDismiss)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ModelSummaryCard(uiState = uiState, isImage = isImage)
                    CommonSectionTitle()
                    ServiceParamsSection(
                        serviceFields = serviceFields,
                        mediaReferences = if (isImage) {
                            uiState.imageConfig.mediaReferences
                        } else {
                            uiState.videoConfig.mediaReferences
                        },
                        isImage = isImage,
                        onParamChange = { paramKey, value ->
                            if (isImage) {
                                onImageServiceParamChange(paramKey, value)
                            } else {
                                onVideoServiceParamChange(paramKey, value)
                            }
                        },
                        onServiceUploadFieldClick = onServiceUploadFieldClick,
                        onRemoveMedia = onRemoveMedia,
                    )
                    ParamsActionBar(
                        parameterCount = serviceFields.visibleFieldCount(),
                        onDone = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamsSheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.quick_create_params_sheet_title),
            color = QuickCreateDesignTokens.Text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.quick_create_params_close_content_description),
                tint = Color(0xFFC9CAD2),
                modifier = Modifier.size(28.dp),
            )
        }
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
        ?: uiState.imageConfig.model.label.asImageModelText()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF15181D),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.quick_create_params_endpoint_value),
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.quick_create_params_endpoint_label),
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
private fun CommonSectionTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(QuickCreateDesignTokens.Purple, RoundedCornerShape(2.dp)),
        )
        Text(
            text = stringResource(Res.string.quick_create_params_common_title),
            color = QuickCreateDesignTokens.Text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
        )
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
        .filterNot { field -> field.isPromptParameterField() }
    if (flattenedFields.isEmpty()) {
        EmptyParamsCard()
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
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
    ParamCard(
        title = field.title,
        keyName = field.paramKey,
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (field.controlType) {
            QuickCreationServiceFieldControlType.OPTIONS -> ServiceOptionsControl(
                field = field,
                onParamChange = onParamChange,
            )
            QuickCreationServiceFieldControlType.TEXT -> ServiceTextControl(
                field = field,
                onParamChange = onParamChange,
            )
            QuickCreationServiceFieldControlType.UPLOAD -> ServiceUploadControl(
                field = field,
                mediaReferences = mediaReferences,
                isImage = isImage,
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        field.options.chunked(3).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    Surface(
                        modifier = Modifier.weight(1f).height(34.dp),
                        onClick = { onParamChange(field.paramKey, option.value) },
                        color = if (option.selected) Color(0xAA5A4590) else Color(0xFF1C1E23),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (option.selected) QuickCreateDesignTokens.Purple else QuickCreateDesignTokens.Stroke,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = option.label.ifBlank { option.value },
                                color = if (option.selected) {
                                    QuickCreateDesignTokens.PurpleSoft
                                } else {
                                    Color(0xFFD2D3D8)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
                repeat(3 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
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
    isImage: Boolean,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    QuickCreateServiceUploadFieldPicker(
        paramKey = field.paramKey,
        mediaType = field.uploadMediaType.toQuickCreateMediaType(fallbackToImage = isImage),
        hint = field.uploadHint.asUploadHintText(),
        mediaReferences = mediaReferences,
        onUploadFieldClick = onServiceUploadFieldClick,
        onRemoveMedia = onRemoveMedia,
    )
}

@Composable
private fun ParamCard(
    title: String,
    keyName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.heightIn(min = 96.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF13161B),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = QuickCreateDesignTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = Color(0xFF282A30),
                    shape = RoundedCornerShape(5.dp),
                ) {
                    Text(
                        text = keyName,
                        color = Color(0xFFC7C8CF),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            content()
        }
    }
}

@Composable
private fun ParamsActionBar(
    parameterCount: Int,
    onDone: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF15171B),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(QuickCreateDesignTokens.Purple, CircleShape),
            )
            Text(
                text = stringResource(Res.string.quick_create_params_parameter_count_format, parameterCount),
                color = QuickCreateDesignTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onDone,
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.height(40.dp).weight(0.55f),
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFF7556F6), Color(0xFF9B61FF))),
                        RoundedCornerShape(18.dp),
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.quick_create_params_done),
                        color = QuickCreateDesignTokens.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
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

private fun QuickCreationServiceUploadMediaType?.toQuickCreateMediaType(
    fallbackToImage: Boolean,
): QuickCreateMediaType? =
    when (this) {
        QuickCreationServiceUploadMediaType.IMAGE -> QuickCreateMediaType.IMAGE
        QuickCreationServiceUploadMediaType.VIDEO -> QuickCreateMediaType.VIDEO
        QuickCreationServiceUploadMediaType.AUDIO -> QuickCreateMediaType.AUDIO
        null -> if (fallbackToImage) QuickCreateMediaType.IMAGE else null
    }

private fun List<QuickCreationServiceFieldUi>.flattenServiceFields(): List<QuickCreationServiceFieldUi> =
    flatMap { field ->
        listOf(field) + field.childFields.flattenServiceFields()
    }

private fun List<QuickCreationServiceFieldUi>.visibleFieldCount(): Int =
    flattenServiceFields()
        .count { field -> !field.isPromptParameterField() }

private fun QuickCreationServiceFieldUi.isPromptParameterField(): Boolean {
    val normalizedKey = paramKey.trim().lowercase()
    return normalizedKey == "prompt" ||
        normalizedKey == "text" ||
        normalizedKey == "input" ||
        normalizedKey == "positiveprompt" ||
        normalizedKey == "positive_prompt"
}
