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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.feature.quickcreate.QuickCreateDesignTokens
import com.runninghub.app.ui.feature.quickcreate.QuickCreateModelGlyph
import com.runninghub.app.ui.feature.quickcreate.QuickCreateSheetHandle
import com.runninghub.app.ui.feature.quickcreate.presentation.asServiceModelText
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.asImageModelText
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.composer.asImageQualityText
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUi
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_params_aspect_ratio_key
import runninghub.composeapp.generated.resources.quick_create_params_aspect_ratio_title
import runninghub.composeapp.generated.resources.quick_create_params_close_content_description
import runninghub.composeapp.generated.resources.quick_create_params_common_title
import runninghub.composeapp.generated.resources.quick_create_params_count_key
import runninghub.composeapp.generated.resources.quick_create_params_count_title
import runninghub.composeapp.generated.resources.quick_create_params_done
import runninghub.composeapp.generated.resources.quick_create_params_endpoint_label
import runninghub.composeapp.generated.resources.quick_create_params_endpoint_value
import runninghub.composeapp.generated.resources.quick_create_params_model_label
import runninghub.composeapp.generated.resources.quick_create_params_quality_key
import runninghub.composeapp.generated.resources.quick_create_params_quality_recommended_format
import runninghub.composeapp.generated.resources.quick_create_params_quality_title
import runninghub.composeapp.generated.resources.quick_create_params_resolution_key
import runninghub.composeapp.generated.resources.quick_create_params_resolution_title
import runninghub.composeapp.generated.resources.quick_create_params_restore_defaults
import runninghub.composeapp.generated.resources.quick_create_params_sheet_title
import runninghub.composeapp.generated.resources.quick_create_params_changed_count_format

/**
 * 展示设计稿版快捷创作参数底部面板。
 *
 * 该组件只渲染图片创作常用参数和当前服务模型摘要，参数变更通过回调回到 ScreenModel；
 * 服务端动态字段在本轮 UI 重构中不直接展开，避免把远端字段表单与设计图中的常用参数面板混杂。
 *
 * @param visible 是否显示底部面板。
 * @param isImage 当前是否编辑图片创作；`false` 时仍展示模型摘要和当前视频配置的只读降级面板。
 * @param uiState 快捷创作页面状态。
 * @param serviceFields 当前服务端动态字段列表，本设计稿版暂不直接渲染，仅保留参数以维持调用契约。
 * @param onDismiss 关闭面板回调。
 * @param onImageModelSelected 本地兼容图片模型选择回调，本设计稿中不直接展示本地模型列表。
 * @param onImageServiceParamChange 图片服务动态字段变更回调，本设计稿中不直接触发。
 * @param onVideoServiceParamChange 视频服务动态字段变更回调，本设计稿中不直接触发。
 * @param onToggleRealistic 视频真人模式切换回调，本设计稿中不直接触发。
 * @param onImageSeedChange 图片 Seed 变更回调，本设计稿中不直接触发。
 * @param onVideoSeedChange 视频 Seed 变更回调，本设计稿中不直接触发。
 * @param onServiceUploadFieldClick 字段级上传回调，本设计稿中不直接触发。
 * @param onRemoveMedia 移除字段素材回调，本设计稿中不直接触发。
 * @param onImageRatioChange 图片宽高比变更回调。
 * @param onImageResChange 图片分辨率变更回调。
 * @param onImageQualityChange 图片质量变更回调。
 * @param onImageCountChange 图片生成数量变更回调。
 * @param modifier 外层定位修饰符。
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
                QuickCreateSheetHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
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
                    ImageCommonParamsGrid(
                        uiState = uiState,
                        onImageRatioChange = onImageRatioChange,
                        onImageResChange = onImageResChange,
                        onImageQualityChange = onImageQualityChange,
                        onImageCountChange = onImageCountChange,
                    )
                    ParamsActionBar(
                        changedCount = calculateChangedCount(uiState),
                        onRestoreDefaults = {
                            val defaults = uiState.imageConfig.model
                            onImageRatioChange(defaults.defaultAspectRatio)
                            onImageResChange(defaults.defaultResolution)
                            onImageQualityChange(defaults.defaultQuality)
                            onImageCountChange(1)
                        },
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
private fun ImageCommonParamsGrid(
    uiState: QuickCreateUiState,
    onImageRatioChange: (ImageAspectRatio) -> Unit,
    onImageResChange: (ImageResolution) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onImageCountChange: (Int) -> Unit,
) {
    val imageConfig = uiState.imageConfig
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ParamCard(
                title = stringResource(Res.string.quick_create_params_aspect_ratio_title),
                keyName = stringResource(Res.string.quick_create_params_aspect_ratio_key),
                modifier = Modifier.weight(1f),
            ) {
                SegmentedControl(
                    items = listOf(
                        ImageAspectRatio.RATIO_1_1,
                        ImageAspectRatio.RATIO_9_16,
                        ImageAspectRatio.RATIO_16_9,
                    ),
                    selected = imageConfig.aspectRatio,
                    label = { it.displayName },
                    onSelect = onImageRatioChange,
                )
            }
            ParamCard(
                title = stringResource(Res.string.quick_create_params_resolution_title),
                keyName = stringResource(Res.string.quick_create_params_resolution_key),
                modifier = Modifier.weight(1f),
            ) {
                SegmentedControl(
                    items = listOf(
                        ImageResolution.RES_1K,
                        ImageResolution.RES_2K,
                        ImageResolution.RES_4K,
                    ),
                    selected = imageConfig.resolution,
                    label = { it.displayName },
                    onSelect = onImageResChange,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ParamCard(
                title = stringResource(Res.string.quick_create_params_count_title),
                keyName = stringResource(Res.string.quick_create_params_count_key),
                modifier = Modifier.weight(1f),
            ) {
                CountStepper(
                    count = imageConfig.count,
                    onChange = onImageCountChange,
                )
            }
            ParamCard(
                title = stringResource(Res.string.quick_create_params_quality_title),
                keyName = stringResource(Res.string.quick_create_params_quality_key),
                modifier = Modifier.weight(1f),
            ) {
                QualityDropdown(quality = imageConfig.quality)
            }
        }
    }
}

@Composable
private fun ParamCard(
    title: String,
    keyName: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.height(96.dp),
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
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = Color(0xFFC7C8CF),
                    modifier = Modifier.size(18.dp),
                )
            }
            content()
        }
    }
}

@Composable
private fun <T> SegmentedControl(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF1C1E23),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row {
            items.forEach { item ->
                val active = item == selected
                Surface(
                    modifier = Modifier.weight(1f),
                    onClick = { onSelect(item) },
                    color = if (active) Color(0xAA5A4590) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (active) QuickCreateDesignTokens.Purple else Color.Transparent,
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label(item),
                            color = if (active) QuickCreateDesignTokens.PurpleSoft else Color(0xFFD2D3D8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountStepper(count: Int, onChange: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF1C1E23),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onChange((count - 1).coerceAtLeast(1)) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFFD2D3D8))
            }
            Text(
                text = count.toString(),
                color = QuickCreateDesignTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { onChange((count + 1).coerceAtMost(4)) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFD2D3D8))
            }
        }
    }
}

@Composable
private fun QualityDropdown(quality: ImageQuality) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(34.dp),
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF1C1E23),
        border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    Res.string.quick_create_params_quality_recommended_format,
                    quality.label.asImageQualityText(),
                ),
                color = Color(0xFFD2D3D8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFD2D3D8),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ParamsActionBar(
    changedCount: Int,
    onRestoreDefaults: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
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
                text = stringResource(Res.string.quick_create_params_changed_count_format, changedCount),
                color = QuickCreateDesignTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onRestoreDefaults,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF24252A),
                border = BorderStroke(1.dp, QuickCreateDesignTokens.Stroke),
                modifier = Modifier.height(40.dp),
            ) {
                Box(modifier = Modifier.padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.quick_create_params_restore_defaults),
                        color = Color(0xFFC8C9CF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Surface(
                onClick = onDone,
                shape = RoundedCornerShape(18.dp),
                color = Color.Transparent,
                modifier = Modifier.height(40.dp).weight(0.8f),
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

private fun calculateChangedCount(uiState: QuickCreateUiState): Int {
    val config = uiState.imageConfig
    val model = config.model
    var count = 0
    if (config.aspectRatio != model.defaultAspectRatio) count += 1
    if (config.resolution != model.defaultResolution) count += 1
    if (config.quality != model.defaultQuality) count += 1
    if (config.count != 1) count += 1
    return count.coerceAtLeast(0)
}
