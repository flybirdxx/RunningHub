package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.ImageUploadButton
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.ui.designsystem.components.segmented.RhSegmentedControl
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.detail.presentation.AppDetailInputControl
import com.runninghub.feature.detail.presentation.AppDetailInputFieldUiModel
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.AppDetailUploadingState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_float_placeholder
import runninghub.composeapp.generated.resources.app_detail_int_placeholder
import runninghub.composeapp.generated.resources.app_detail_multi_image_upload_title
import runninghub.composeapp.generated.resources.app_detail_text_placeholder

/**
 * App 详情页参数输入区。
 *
 * 从 AppDetailScreen.kt 拆分而来，承载输入行渲染入口、多图上传行、
 * 按控件类型分发的 InputNodeField 与必填星标；行内不再自带水平缩进，
 * 统一由参数区容器控制，避免高级分组卡内出现双重缩进。
 */

@Composable
internal fun RenderInputNodeField(
    field: AppDetailInputFieldUiModel,
    uiState: AppDetailUiState,
    onInputChanged: (String, String, String) -> Unit,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit,
    onOpenPicker: (AppDetailInputFieldUiModel) -> Unit
) {
    InputNodeField(
        field = field,
        localUri = uiState.localUris[field.nodeId],
        uploadState = uiState.uploadingNodes[field.nodeId],
        onValueChanged = { onInputChanged(field.nodeId, field.fieldName, it) },
        onPickFile = {
            val mediaType = (field.control as? AppDetailInputControl.MediaUpload)?.mediaType ?: AppDetailMediaType.IMAGE
            onPickMedia(field.nodeId, field.fieldName, mediaType)
        },
        onRemoveFile = { onRemoveFile(field.nodeId, field.fieldName) },
        onOpenPicker = { onOpenPicker(field) }
    )
}

@Composable
internal fun MultiImageUploadRow(
    fields: List<AppDetailInputFieldUiModel>,
    uiState: AppDetailUiState,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.app_detail_multi_image_upload_title),
                color = RhTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f, fill = false)
            )
            RequiredFieldStar()
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(fields, key = { it.inputKey }) { field ->
                val uploadState = uiState.uploadingNodes[field.nodeId]
                ImageUploadButton(
                    localUri = uiState.localUris[field.nodeId] ?: field.currentValue.takeIf { it.startsWith("http") },
                    remoteUrl = field.currentValue.takeIf { it.startsWith("http") },
                    fileName = uiState.localUris[field.nodeId]?.substringAfterLast("/")?.substringAfterLast("%2F")
                        ?: field.currentValue.takeIf { it.isNotBlank() && !it.startsWith("http") },
                    isUploading = uploadState != null && !uploadState.isError,
                    uploadProgress = uploadState?.progress ?: 0f,
                    isError = uploadState?.isError == true,
                    mediaType = MediaType.IMAGE,
                    square = true,
                    onPickFile = { onPickMedia(field.nodeId, field.fieldName, AppDetailMediaType.IMAGE) },
                    onRemoveFile = { onRemoveFile(field.nodeId, field.fieldName) },
                    modifier = Modifier.width(112.dp)
                )
            }
        }
    }
}

/**
 * 必填字段星标：媒体上传字段缺省即无法运行任务，标题后追加醒目提示。
 */
@Composable
private fun RequiredFieldStar() {
    Text(
        text = " *",
        color = RhTheme.colors.statusFailed,
        style = RhTypography.caption,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputNodeField(
    field: AppDetailInputFieldUiModel,
    localUri: String?,
    uploadState: AppDetailUploadingState?,
    onValueChanged: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onOpenPicker: () -> Unit
) {
    val isUploading = uploadState != null && !uploadState.isError
    val uploadProgress = uploadState?.progress ?: 0f
    val isUploadError = uploadState?.isError == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (field.isModified) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .width(2.dp)
                        .height(12.dp)
                        .background(RhTheme.colors.brandPrimary)
                )
            }
            Text(
                text = field.title,
                color = RhTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (field.control is AppDetailInputControl.MediaUpload) {
                RequiredFieldStar()
            }
        }

        when (val control = field.control) {
            is AppDetailInputControl.Dropdown -> {
                ListDropdown(
                    options = control.options,
                    currentValue = field.currentValue,
                    onValueChanged = onValueChanged,
                    onOpenPicker = if (control.options.size > APP_DETAIL_INLINE_DROPDOWN_MAX_OPTIONS) {
                        onOpenPicker
                    } else {
                        null
                    }
                )
            }
            is AppDetailInputControl.MediaUpload -> {
                ImageUploadButton(
                    localUri = localUri ?: field.currentValue.takeIf { it.startsWith("http") },
                    remoteUrl = field.currentValue.takeIf { it.startsWith("http") },
                    fileName = localUri?.substringAfterLast("/")?.substringAfterLast("%2F"),
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    isError = isUploadError,
                    mediaType = control.mediaType.toComponentMediaType(),
                    onPickFile = onPickFile,
                    onRemoveFile = onRemoveFile
                )
            }
            AppDetailInputControl.BooleanSwitch -> {
                BooleanSwitch(
                    currentValue = field.currentValue,
                    onValueChanged = onValueChanged
                )
            }
            is AppDetailInputControl.Segmented -> {
                RhSegmentedControl(
                    options = control.options,
                    selectedIndex = control.options.indexOf(field.currentValue),
                    onSelect = { index -> onValueChanged(control.options[index]) }
                )
            }
            AppDetailInputControl.IntegerText -> {
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal.toIntOrNull() != null) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = stringResource(Res.string.app_detail_int_placeholder),
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
            }
            AppDetailInputControl.DecimalText -> {
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal == "." ||
                            newVal.toDoubleOrNull() != null || newVal.endsWith(".")
                        ) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = stringResource(Res.string.app_detail_float_placeholder),
                    keyboardType = KeyboardType.Decimal,
                    singleLine = true
                )
            }
            is AppDetailInputControl.Text -> {
                val lineLimits = appDetailTextFieldLineLimits(multiline = control.multiline)
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = onValueChanged,
                    placeholder = stringResource(Res.string.app_detail_text_placeholder),
                    singleLine = !control.multiline,
                    minLines = lineLimits.minLines,
                    maxLines = lineLimits.maxLines
                )
            }
        }
    }
}
