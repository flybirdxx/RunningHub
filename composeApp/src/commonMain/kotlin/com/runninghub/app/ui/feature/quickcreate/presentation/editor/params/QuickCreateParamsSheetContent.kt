package com.runninghub.app.ui.feature.quickcreate.presentation.editor.params

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.feature.quickcreate.presentation.editor.ImageModel
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.app.ui.feature.quickcreate.presentation.upload.QuickCreateServiceUploadFieldPicker
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral100
import com.runninghub.app.ui.theme.Neutral200
import com.runninghub.app.ui.theme.Neutral300
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldControlType
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceFieldUi
import com.runninghub.feature.quickcreate.presentation.fields.QuickCreationServiceUploadMediaType
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

/**
 * 展示当前快捷创作入口使用的“更多参数”底部面板。
 *
 * 该组件属于 editor/params 子区域，只负责把 [QuickCreateUiState] 中的图片或视频参数映射为
 * 可编辑表单。模型选择、动态字段值、上传字段点击和媒体移除均通过回调回到 ScreenModel，
 * 组件自身不读取存储、不发起网络请求，也不直接调用平台媒体选择器。
 *
 * @param visible 是否显示底部面板，`true` 时从底部进入。
 * @param isImage 当前是否编辑图片创作参数；`false` 表示编辑视频参数。
 * @param uiState 快捷创作页面状态，提供当前本地参数、媒体引用和 Seed。
 * @param serviceFields 当前图片或视频服务端模型已经映射好的动态字段 UI 列表。
 * @param onDismiss 用户关闭面板时触发。
 * @param onImageModelSelected 用户选择本地兼容图片模型时触发。
 * @param onImageServiceParamChange 图片服务端动态字段变更回调。
 * @param onVideoServiceParamChange 视频服务端动态字段变更回调。
 * @param onToggleRealistic 用户切换视频真人模式时触发。
 * @param onImageSeedChange 图片 Seed 变更回调，`null` 表示随机 Seed。
 * @param onVideoSeedChange 视频 Seed 变更回调，`null` 表示随机 Seed。
 * @param onServiceUploadFieldClick 用户点击服务端上传字段时触发，调用方负责权限和平台选择器。
 * @param onRemoveMedia 用户移除字段素材时触发。
 * @param modifier 外层调用方用于控制面板定位的修饰符。
 */
@Composable
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
            shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
            color = DarkSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceLG),
            ) {
                ParamsSheetHeader(title = "更多参数", onDismiss = onDismiss)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = Dimens.SpaceLG),
                ) {
                    if (isImage) {
                        ImageParamsContent(
                            models = ImageModel.entries,
                            selected = uiState.imageConfig.model,
                            serviceFields = serviceFields,
                            mediaReferences = uiState.imageConfig.mediaReferences,
                            seed = uiState.imageConfig.seed,
                            onSelect = onImageModelSelected,
                            onServiceParamChange = onImageServiceParamChange,
                            onServiceUploadFieldClick = onServiceUploadFieldClick,
                            onRemoveMedia = onRemoveMedia,
                            onSeedChange = onImageSeedChange,
                        )
                    } else {
                        VideoParamsContent(
                            serviceFields = serviceFields,
                            mediaReferences = uiState.videoConfig.mediaReferences,
                            realistic = uiState.videoConfig.realisticMode,
                            seed = uiState.videoConfig.seed,
                            onServiceParamChange = onVideoServiceParamChange,
                            onServiceUploadFieldClick = onServiceUploadFieldClick,
                            onRemoveMedia = onRemoveMedia,
                            onRealisticToggle = onToggleRealistic,
                            onSeedChange = onVideoSeedChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamsSheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceSM, start = Dimens.SpaceLG, end = Dimens.SpaceLG, bottom = Dimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Neutral100,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = Neutral500,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ImageParamsContent(
    models: List<ImageModel>,
    selected: ImageModel,
    serviceFields: List<QuickCreationServiceFieldUi>,
    mediaReferences: List<MediaReference>,
    seed: Int?,
    onSelect: (ImageModel) -> Unit,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        serviceFields
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        Text(
            text = "本地兼容模型",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            models.forEach { model ->
                val isSelected = model == selected
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onSelect(model) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = model.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Primary300 else Neutral200,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Primary300,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceXL))
        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

@Composable
private fun VideoParamsContent(
    serviceFields: List<QuickCreationServiceFieldUi>,
    mediaReferences: List<MediaReference>,
    realistic: Boolean,
    seed: Int?,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onRealisticToggle: () -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        serviceFields
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        ToggleChip(
            label = "真人模式",
            enabled = realistic,
            onClick = onRealisticToggle,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.SpaceLG))
        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

/**
 * 渲染服务端动态字段表单。
 *
 * 表单根据上游已映射好的 UI 字段选择选项控件、文本输入或上传字段入口。字段值通过
 * [onParamChange] 回传给 ScreenModel，由上层统一保存到图片/视频服务端参数 Map；
 * 上传字段只触发回调，不直接执行权限请求或文件上传，确保 Presentation 子组件保持平台无关。
 *
 * @param fields 已经过滤为可渲染的字段 UI 模型，顺序来自服务端配置。
 * @param mediaReferences 当前创作配置中的媒体引用，用于展示字段级上传素材。
 * @param onParamChange 字段值变更回调。
 * @param onUploadFieldClick 用户点击上传字段时触发。
 * @param onRemoveMedia 用户移除字段素材时触发。
 */
@Composable
internal fun ServiceFieldOptionsContent(
    fields: List<QuickCreationServiceFieldUi>,
    mediaReferences: List<MediaReference>,
    onParamChange: (String, String) -> Unit,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        Text(
            text = "服务端参数",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
        )
        fields.forEach { field ->
            ServiceFieldInput(
                field = field,
                mediaReferences = mediaReferences,
                onParamChange = onParamChange,
                onUploadFieldClick = onUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
    }
}

@Composable
private fun ServiceFieldInput(
    field: QuickCreationServiceFieldUi,
    mediaReferences: List<MediaReference>,
    onParamChange: (String, String) -> Unit,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = if (field.indentLevel > 0) Dimens.SpaceMD else 0.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        Text(
            text = field.title,
            fontSize = 12.sp,
            color = Neutral300,
        )
        field.description?.let { description ->
            Text(
                text = description,
                fontSize = 11.sp,
                color = Neutral500,
            )
        }
        when (field.controlType) {
            QuickCreationServiceFieldControlType.OPTIONS -> ServiceFieldOptions(
                field = field,
                onParamChange = onParamChange,
            )
            QuickCreationServiceFieldControlType.TEXT -> ServiceFieldTextInput(
                field = field,
                onParamChange = onParamChange,
            )
            QuickCreationServiceFieldControlType.UPLOAD -> QuickCreateServiceUploadFieldPicker(
                paramKey = field.paramKey,
                // 动态字段模型已迁入 feature presentation；composeApp 迁移期只把稳定媒体枚举
                // 转换为当前上传组件仍在使用的页面状态枚举，避免新模块反向依赖应用层类型。
                mediaType = field.uploadMediaType.toAppMediaType(),
                hint = field.uploadHint,
                mediaReferences = mediaReferences,
                onUploadFieldClick = onUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
        field.childFields.forEach { child ->
            ServiceFieldInput(
                field = child,
                mediaReferences = mediaReferences,
                onParamChange = onParamChange,
                onUploadFieldClick = onUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
    }
}

private fun QuickCreationServiceUploadMediaType?.toAppMediaType(): QuickCreateMediaType? =
    when (this) {
        QuickCreationServiceUploadMediaType.IMAGE -> QuickCreateMediaType.IMAGE
        QuickCreationServiceUploadMediaType.VIDEO -> QuickCreateMediaType.VIDEO
        QuickCreationServiceUploadMediaType.AUDIO -> QuickCreateMediaType.AUDIO
        null -> null
    }

@Composable
private fun ServiceFieldOptions(
    field: QuickCreationServiceFieldUi,
    onParamChange: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        field.options.forEach { option ->
            Surface(
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (option.selected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (option.selected) Primary300 else DarkOutlineVariant),
                onClick = { onParamChange(field.paramKey, option.value) },
            ) {
                Text(
                    text = option.label,
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                    fontSize = 12.sp,
                    fontWeight = if (option.selected) FontWeight.Medium else FontWeight.Normal,
                    color = if (option.selected) Primary300 else Neutral200,
                )
            }
        }
    }
}

@Composable
private fun ServiceFieldTextInput(
    field: QuickCreationServiceFieldUi,
    onParamChange: (String, String) -> Unit,
) {
    var text by remember(field.paramKey, field.textValue) { mutableStateOf(field.textValue) }
    OutlinedTextField(
        value = text,
        onValueChange = { value ->
            val constrained = field.constrainTextInput(value)
            text = constrained
            onParamChange(field.paramKey, constrained)
        },
        singleLine = true,
        placeholder = { Text(field.placeholder, color = Neutral500, fontSize = 12.sp) },
        shape = RoundedCornerShape(Dimens.RadiusSM),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary300,
            focusedBorderColor = Primary300,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    field.textLimitCounter?.let { counter ->
        Text(
            text = counter,
            fontSize = 10.sp,
            color = Neutral500,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 渲染 Seed 输入框。
 *
 * Seed 是生成任务的可选确定性参数，空值表示交给服务端随机；组件只做整数解析，
 * 不在 UI 层校验服务端支持范围，避免和生成请求工厂中的业务规则分叉。
 *
 * @param seed 当前 Seed；`null` 表示随机。
 * @param onSeedChange Seed 变更回调，无法解析为整数时传出 `null`。
 */
@Composable
internal fun SeedInput(seed: Int?, onSeedChange: (Int?) -> Unit) {
    var text by remember(seed) { mutableStateOf(seed?.toString() ?: "") }

    Text(
        text = "Seed（留空为随机）",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary300,
        modifier = Modifier.padding(bottom = Dimens.SpaceSM),
    )
    OutlinedTextField(
        value = text,
        onValueChange = { newVal ->
            text = newVal
            val num = newVal.toIntOrNull()
            onSeedChange(num)
        },
        placeholder = { Text("随机", color = Neutral500, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary300,
            focusedBorderColor = Primary300,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 渲染参数面板中的二元开关。
 *
 * 该控件只表达当前开关状态和点击事件，具体状态含义由调用方决定，例如真人模式或生成音频。
 *
 * @param label 展示给用户的开关名称。
 * @param enabled `true` 表示该选项已开启，`false` 表示未开启。
 * @param onClick 用户点击开关时触发。
 * @param modifier 外层布局修饰符。
 */
@Composable
internal fun ToggleChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = if (enabled) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (enabled) Primary300 else DarkOutlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                if (enabled) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (enabled) Primary300 else Neutral500,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
                color = if (enabled) Primary300 else Neutral200,
            )
        }
    }
}
