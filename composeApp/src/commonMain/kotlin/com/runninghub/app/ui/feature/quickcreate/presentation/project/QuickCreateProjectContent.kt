package com.runninghub.app.ui.feature.quickcreate.presentation.project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailUiItem
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailRowLabel
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailRowValue
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectPinStatusText
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_project_cancel_action
import runninghub.composeapp.generated.resources.quick_create_project_create_confirm
import runninghub.composeapp.generated.resources.quick_create_project_create_content_description
import runninghub.composeapp.generated.resources.quick_create_project_create_title
import runninghub.composeapp.generated.resources.quick_create_project_detail_close
import runninghub.composeapp.generated.resources.quick_create_project_detail_loading_body
import runninghub.composeapp.generated.resources.quick_create_project_detail_loading_title
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_created_at
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_pin_status
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_task_count
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_updated_at
import runninghub.composeapp.generated.resources.quick_create_project_detail_title
import runninghub.composeapp.generated.resources.quick_create_project_name_label
import runninghub.composeapp.generated.resources.quick_create_project_not_pinned_status
import runninghub.composeapp.generated.resources.quick_create_project_pinned_status

/**
 * 渲染快捷创作顶栏里的新建项目入口。
 *
 * 该入口复用项目区的新建弹窗，位置由调用方决定；确认后仍只把已裁剪的项目名称回传给
 * ScreenModel，保持项目创建逻辑不进入 Composable。
 *
 * @param onCreateProject 用户确认新建项目时触发，参数为已裁剪的项目名称。
 */
@Composable
internal fun QuickCreateCreateProjectAction(
    onCreateProject: (String) -> Unit,
) {
    var createDialogVisible by remember { mutableStateOf(false) }

    IconButton(
        onClick = { createDialogVisible = true },
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(
                Res.string.quick_create_project_create_content_description,
            ),
            tint = Primary300,
            modifier = Modifier.size(18.dp),
        )
    }

    if (createDialogVisible) {
        ProjectNameDialog(
            title = stringResource(Res.string.quick_create_project_create_title),
            initialName = "",
            confirmText = stringResource(Res.string.quick_create_project_create_confirm),
            onDismiss = { createDialogVisible = false },
            onConfirm = { name ->
                createDialogVisible = false
                onCreateProject(name)
            },
        )
    }
}

/**
 * 展示项目详情弹窗。
 *
 * 详情数据由外层状态提供，弹窗自身不触发加载；这样可以让 ProjectStateHolder 统一控制
 * 详情请求、加载态和关闭时机，避免弹窗重组导致重复请求。
 *
 * @param isLoading 是否正在加载项目详情，`true` 时 [project] 可以为 `null` 并展示加载提示。
 * @param project 当前选中的项目详情 UI 模型；`null` 且 [isLoading] 为 `false` 时表示没有可展示数据。
 * @param onDismiss 用户关闭弹窗时触发。
 */
@Composable
internal fun QuickCreateProjectDetailDialog(
    isLoading: Boolean,
    project: QuickCreateProjectDetailUiItem?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.quick_create_project_detail_close))
            }
        },
        title = {
            Text(
                if (isLoading) {
                    stringResource(Res.string.quick_create_project_detail_loading_title)
                } else {
                    stringResource(Res.string.quick_create_project_detail_title)
                },
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isLoading || project == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.quick_create_project_detail_loading_body))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    if (project.coverUrl != null) {
                        SmartAsyncImage(
                            imageUrl = project.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusMD)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        project.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    project.rows.forEach { row ->
                        ProjectDetailRow(
                            label = quickCreateProjectDetailRowLabel(row.label),
                            value = quickCreateProjectDetailRowValue(row.value),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ProjectNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.SemiBold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.quick_create_project_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotBlank(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.quick_create_project_cancel_action))
            }
        },
    )
}

@Composable
private fun quickCreateProjectPinStatusText(status: QuickCreateProjectPinStatusText): String =
    when (status) {
        QuickCreateProjectPinStatusText.Pinned -> stringResource(Res.string.quick_create_project_pinned_status)
        QuickCreateProjectPinStatusText.NotPinned -> stringResource(Res.string.quick_create_project_not_pinned_status)
    }

@Composable
private fun quickCreateProjectDetailRowLabel(label: QuickCreateProjectDetailRowLabel): String =
    when (label) {
        QuickCreateProjectDetailRowLabel.TaskCount ->
            stringResource(Res.string.quick_create_project_detail_row_task_count)
        QuickCreateProjectDetailRowLabel.PinStatus ->
            stringResource(Res.string.quick_create_project_detail_row_pin_status)
        QuickCreateProjectDetailRowLabel.CreatedAt ->
            stringResource(Res.string.quick_create_project_detail_row_created_at)
        QuickCreateProjectDetailRowLabel.UpdatedAt ->
            stringResource(Res.string.quick_create_project_detail_row_updated_at)
    }

@Composable
private fun quickCreateProjectDetailRowValue(value: QuickCreateProjectDetailRowValue): String =
    when (value) {
        is QuickCreateProjectDetailRowValue.TaskCount -> value.count.toString()
        is QuickCreateProjectDetailRowValue.PinStatus -> quickCreateProjectPinStatusText(value.status)
        is QuickCreateProjectDetailRowValue.Timestamp -> value.value
    }

@Composable
private fun ProjectDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Neutral400, fontSize = 13.sp)
        Text(
            value,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
