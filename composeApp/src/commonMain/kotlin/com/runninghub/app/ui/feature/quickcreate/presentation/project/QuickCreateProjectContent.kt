package com.runninghub.app.ui.feature.quickcreate.presentation.project

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDeleteConfirmationText
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailUiItem
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailRowLabel
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectDetailRowValue
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectPinContentDescription
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectPinStatusText
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectTaskCountText
import com.runninghub.feature.quickcreate.presentation.project.QuickCreateProjectUiItem
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_project_cancel_action
import runninghub.composeapp.generated.resources.quick_create_project_create_confirm
import runninghub.composeapp.generated.resources.quick_create_project_create_content_description
import runninghub.composeapp.generated.resources.quick_create_project_create_title
import runninghub.composeapp.generated.resources.quick_create_project_delete_action
import runninghub.composeapp.generated.resources.quick_create_project_delete_confirmation_format
import runninghub.composeapp.generated.resources.quick_create_project_delete_title
import runninghub.composeapp.generated.resources.quick_create_project_detail_close
import runninghub.composeapp.generated.resources.quick_create_project_detail_loading_body
import runninghub.composeapp.generated.resources.quick_create_project_detail_loading_title
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_created_at
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_pin_status
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_task_count
import runninghub.composeapp.generated.resources.quick_create_project_detail_row_updated_at
import runninghub.composeapp.generated.resources.quick_create_project_detail_title
import runninghub.composeapp.generated.resources.quick_create_project_load_more
import runninghub.composeapp.generated.resources.quick_create_project_loading_more
import runninghub.composeapp.generated.resources.quick_create_project_menu_content_description
import runninghub.composeapp.generated.resources.quick_create_project_menu_detail
import runninghub.composeapp.generated.resources.quick_create_project_menu_rename
import runninghub.composeapp.generated.resources.quick_create_project_name_label
import runninghub.composeapp.generated.resources.quick_create_project_not_pinned_status
import runninghub.composeapp.generated.resources.quick_create_project_pin_content_description
import runninghub.composeapp.generated.resources.quick_create_project_pinned_status
import runninghub.composeapp.generated.resources.quick_create_project_recent
import runninghub.composeapp.generated.resources.quick_create_project_rename_confirm
import runninghub.composeapp.generated.resources.quick_create_project_rename_title
import runninghub.composeapp.generated.resources.quick_create_project_section_title
import runninghub.composeapp.generated.resources.quick_create_project_task_count_format
import runninghub.composeapp.generated.resources.quick_create_project_unpin_content_description

/**
 * 展示快捷创作默认页顶部的项目筛选与项目操作入口。
 *
 * 该组件属于 project 子区域，只负责展示项目列表以及收集新建、重命名、删除和置顶动作。
 * 真正的项目状态变更仍通过回调交给 ScreenModel/ProjectStateHolder，避免 UI 直接修改业务状态。
 *
 * @param projects 当前已加载的项目 UI 模型集合，顺序由 ProjectStateHolder 保留。
 * @param isLoading 是否正在刷新项目第一页，`true` 时只展示标题旁的小型进度，不阻塞已加载项目操作。
 * @param isLoadingMore 是否正在加载更多项目，`true` 时加载更多 chip 禁用并显示进度。
 * @param hasMore 是否还有下一页项目，`true` 时展示加载更多入口。
 * @param selectedProjectId 当前用于筛选历史任务的项目 ID；`null` 表示查看最近创作。
 * @param pinningIds 正在提交置顶/取消置顶请求的项目 ID 集合，用于禁用对应按钮。
 * @param mutatingIds 正在执行重命名或删除等变更的项目 ID 集合，用于禁用操作菜单。
 * @param onProjectSelected 用户选择项目筛选时触发，参数为项目 ID。
 * @param onClearSelectedProject 用户切回最近创作时触发。
 * @param onLoadMoreProjects 用户请求加载下一页项目时触发。
 * @param onToggleProjectPin 用户切换项目置顶状态时触发，参数为项目 ID。
 * @param onCreateProject 用户确认新建项目时触发，参数为已裁剪的项目名称。
 * @param onRenameProject 用户确认重命名时触发，参数依次为项目 ID 和已裁剪的新名称。
 * @param onDeleteProject 用户确认删除项目时触发，参数为项目 ID。
 * @param onShowProjectDetail 用户请求查看项目详情时触发，参数为项目 ID。
 */
@Composable
internal fun QuickCreateProjectStrip(
    projects: List<QuickCreateProjectUiItem>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    selectedProjectId: String?,
    pinningIds: Set<String>,
    mutatingIds: Set<String>,
    onProjectSelected: (String) -> Unit,
    onClearSelectedProject: () -> Unit,
    onLoadMoreProjects: () -> Unit,
    onToggleProjectPin: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShowProjectDetail: (String) -> Unit,
) {
    var createDialogVisible by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<QuickCreateProjectUiItem?>(null) }
    var deleteTarget by remember { mutableStateOf<QuickCreateProjectUiItem?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.quick_create_project_section_title),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                }
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
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            RecentProjectChip(
                selected = selectedProjectId == null,
                onClick = onClearSelectedProject,
            )
            projects.forEach { project ->
                ProjectChip(
                    project = project,
                    selected = project.projectId == selectedProjectId,
                    isPinning = project.projectId in pinningIds,
                    isMutating = project.projectId in mutatingIds,
                    onClick = { onProjectSelected(project.projectId) },
                    onTogglePin = { onToggleProjectPin(project.projectId) },
                    onShowDetail = { onShowProjectDetail(project.projectId) },
                    onRename = { renameTarget = project },
                    onDelete = { deleteTarget = project },
                )
            }
            if (hasMore) {
                LoadMoreProjectsChip(
                    isLoading = isLoadingMore,
                    onClick = onLoadMoreProjects,
                )
            }
        }
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

    renameTarget?.let { project ->
        ProjectNameDialog(
            title = stringResource(Res.string.quick_create_project_rename_title),
            initialName = project.name,
            confirmText = stringResource(Res.string.quick_create_project_rename_confirm),
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                onRenameProject(project.projectId, name)
            },
        )
    }

    deleteTarget?.let { project ->
        ProjectDeleteDialog(
            project = project,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteProject(project.projectId)
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
private fun RecentProjectChip(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Primary300.copy(alpha = 0.16f) else DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, if (selected) Primary300.copy(alpha = 0.65f) else DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = if (selected) Primary300 else Neutral400,
                modifier = Modifier.size(14.dp),
            )
            Text(
                stringResource(Res.string.quick_create_project_recent),
                color = if (selected) Primary300 else Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LoadMoreProjectsChip(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        color = DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Primary300,
                )
            } else {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Primary300,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                if (isLoading) {
                    stringResource(Res.string.quick_create_project_loading_more)
                } else {
                    stringResource(Res.string.quick_create_project_load_more)
                },
                color = Primary300,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProjectChip(
    project: QuickCreateProjectUiItem,
    selected: Boolean,
    isPinning: Boolean,
    isMutating: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onShowDetail: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        color = if (selected) Primary300.copy(alpha = 0.16f) else DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(
            1.dp,
            when {
                selected -> Primary300.copy(alpha = 0.75f)
                project.isPinned -> Primary300.copy(alpha = 0.65f)
                else -> DarkOutlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onTogglePin,
                enabled = !isPinning,
                modifier = Modifier.size(28.dp),
            ) {
                if (isPinning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                } else {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = quickCreateProjectPinContentDescription(project.pinContentDescription),
                        tint = if (project.isPinned) Primary300 else Neutral400,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    project.name,
                    color = if (selected) Primary300 else Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    quickCreateProjectTaskCountText(project.taskCountText),
                    color = Neutral400,
                    fontSize = 11.sp,
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = !isMutating,
                    modifier = Modifier.size(28.dp),
                ) {
                    if (isMutating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                    } else {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(
                                Res.string.quick_create_project_menu_content_description,
                            ),
                            tint = Neutral400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.quick_create_project_menu_detail)) },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onShowDetail()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.quick_create_project_menu_rename)) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.quick_create_project_delete_action)) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
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
private fun ProjectDeleteDialog(
    project: QuickCreateProjectUiItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.quick_create_project_delete_title), fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(quickCreateProjectDeleteConfirmationText(project.deleteConfirmationText))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.quick_create_project_delete_action), color = ErrorDark)
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
private fun quickCreateProjectTaskCountText(text: QuickCreateProjectTaskCountText): String =
    stringResource(Res.string.quick_create_project_task_count_format, text.count)

@Composable
private fun quickCreateProjectPinContentDescription(
    description: QuickCreateProjectPinContentDescription,
): String =
    when (description) {
        QuickCreateProjectPinContentDescription.PinProject ->
            stringResource(Res.string.quick_create_project_pin_content_description)
        QuickCreateProjectPinContentDescription.UnpinProject ->
            stringResource(Res.string.quick_create_project_unpin_content_description)
    }

@Composable
private fun quickCreateProjectPinStatusText(status: QuickCreateProjectPinStatusText): String =
    when (status) {
        QuickCreateProjectPinStatusText.Pinned -> stringResource(Res.string.quick_create_project_pinned_status)
        QuickCreateProjectPinStatusText.NotPinned -> stringResource(Res.string.quick_create_project_not_pinned_status)
    }

@Composable
private fun quickCreateProjectDeleteConfirmationText(
    text: QuickCreateProjectDeleteConfirmationText,
): String =
    stringResource(Res.string.quick_create_project_delete_confirmation_format, text.projectName)

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
