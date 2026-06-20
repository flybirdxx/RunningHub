package com.runninghub.app.ui.feature.quickcreate.presentation.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.feature.quickcreate.QuickCreateHistoryDetailUiItem
import com.runninghub.app.ui.feature.quickcreate.QuickCreateHistoryOutputMediaType
import com.runninghub.app.ui.feature.quickcreate.QuickCreateHistoryUiItem
import com.runninghub.app.ui.feature.quickcreate.QuickCreateUiState
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateProjectStrip
import com.runninghub.app.ui.theme.DarkOutlineVariant
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300

/**
 * 展示快捷创作默认页的历史列表区域。
 *
 * 历史区域同时承载“项目筛选条 + 当前筛选下的历史任务列表”，因此在 UI 拆分阶段
 * 保留对 project 子区域的组合关系。分页、取消任务和项目变更仍通过回调交给
 * ScreenModel/StateHolder，避免列表渲染层直接触碰 Repository。
 *
 * @param uiState 快捷创作页面状态，当前读取项目筛选、历史列表、分页和取消任务状态。
 * @param onHistoryItemSelected 用户点击历史输出时触发，参数为 outputId。
 * @param onLoadMoreHistory 用户请求加载更多历史任务时触发。
 * @param onCancelHistoryTask 用户请求取消未结束任务时触发，参数为 taskId。
 * @param onProjectSelected 用户选择项目筛选时触发，参数为项目 ID。
 * @param onClearSelectedProject 用户切回最近创作时触发。
 * @param onLoadMoreProjects 用户请求加载更多项目时触发。
 * @param onToggleProjectPin 用户切换项目置顶状态时触发，参数为项目 ID。
 * @param onCreateProject 用户确认新建项目时触发。
 * @param onRenameProject 用户确认重命名项目时触发，参数依次为项目 ID 和新名称。
 * @param onDeleteProject 用户确认删除项目时触发，参数为项目 ID。
 * @param onShowProjectDetail 用户请求查看项目详情时触发，参数为项目 ID。
 */
@Composable
internal fun QuickCreateHistoryArea(
    uiState: QuickCreateUiState,
    onHistoryItemSelected: (String) -> Unit,
    onLoadMoreHistory: () -> Unit,
    onCancelHistoryTask: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onClearSelectedProject: () -> Unit,
    onLoadMoreProjects: () -> Unit,
    onToggleProjectPin: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShowProjectDetail: (String) -> Unit,
) {
    val selectedProject = uiState.projects.firstOrNull { it.projectId == uiState.selectedProjectId }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        item {
            QuickCreateProjectStrip(
                projects = uiState.projects,
                isLoading = uiState.projectsLoading,
                isLoadingMore = uiState.projectsLoadingMore,
                hasMore = uiState.projectsHasMore,
                selectedProjectId = uiState.selectedProjectId,
                pinningIds = uiState.projectPinningIds,
                mutatingIds = uiState.projectMutatingIds,
                onProjectSelected = onProjectSelected,
                onClearSelectedProject = onClearSelectedProject,
                onLoadMoreProjects = onLoadMoreProjects,
                onToggleProjectPin = onToggleProjectPin,
                onCreateProject = onCreateProject,
                onRenameProject = onRenameProject,
                onDeleteProject = onDeleteProject,
                onShowProjectDetail = onShowProjectDetail,
            )
            Spacer(Modifier.height(Dimens.SpaceSM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedProject?.name ?: "最近创作",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (uiState.historyLoading || uiState.projectTasksLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                }
            }
        }

        items(uiState.historyItems, key = { it.taskId }) { item ->
            HistoryItemRow(
                item = item,
                isCancelling = item.taskId in uiState.historyCancellingTaskIds,
                onClick = {
                    item.primaryOutput?.outputId?.takeIf { it.isNotBlank() }?.let(onHistoryItemSelected)
                },
                onCancel = {
                    onCancelHistoryTask(item.taskId)
                },
            )
        }

        if (uiState.historyHasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMoreHistory,
                    enabled = !uiState.historyLoadingMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    border = BorderStroke(1.dp, DarkOutlineVariant),
                ) {
                    if (uiState.historyLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                        Spacer(Modifier.width(Dimens.SpaceSM))
                    }
                    Text("加载更多")
                }
            }
        }
    }
}

/**
 * 展示快捷创作历史详情弹窗。
 *
 * 弹窗只消费外层已经选中的历史详情，不在自身内部发起请求。这样可以由 HistoryStateHolder
 * 统一管理详情加载态、失败恢复和关闭后状态清理。
 *
 * @param isLoading 是否正在加载历史详情，`true` 时 [item] 可以为 `null` 并展示加载提示。
 * @param item 当前选中的历史记录详情；`null` 且 [isLoading] 为 `false` 时表示没有详情可展示。
 * @param onDismiss 用户关闭详情弹窗时触发。
 */
@Composable
internal fun QuickCreateHistoryDetailDialog(
    isLoading: Boolean,
    item: QuickCreateHistoryDetailUiItem?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = {
            Text(
                if (isLoading) "加载详情" else "创作详情",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isLoading || item == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("正在加载历史详情")
                }
            } else {
                val output = item.primaryOutput
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    val previewUrl = output?.previewUrl
                    if (previewUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusMD)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SmartAsyncImage(
                                imageUrl = previewUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (output.mediaType == QuickCreateHistoryOutputMediaType.VIDEO) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color.White.copy(alpha = 0.86f),
                                )
                            }
                        }
                    }
                    Text(
                        item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.metadataText,
                        color = Neutral500,
                        fontSize = 12.sp,
                    )
                    item.cashText?.let { cashText ->
                        Text(
                            cashText,
                            color = Primary300,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.82f),
    )
}

@Composable
private fun HistoryItemRow(
    item: QuickCreateHistoryUiItem,
    isCancelling: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val output = item.primaryOutput
    val canOpenDetail = output?.outputId?.isNotBlank() == true
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canOpenDetail) { onClick() },
        color = DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusSM)),
                contentAlignment = Alignment.Center,
            ) {
                val previewUrl = output?.previewUrl
                if (previewUrl != null) {
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.28f),
                    )
                }
                if (output?.mediaType == QuickCreateHistoryOutputMediaType.VIDEO) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    item.title,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.metadataText,
                    color = Neutral400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.cashText?.let { cashText ->
                    Text(
                        cashText,
                        color = Primary300,
                        fontSize = 12.sp,
                    )
                }
                if (item.canCancelTask) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !isCancelling,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = Primary300,
                            )
                            Spacer(Modifier.width(Dimens.SpaceXS))
                        }
                        Text(
                            text = if (isCancelling) "取消中" else "取消任务",
                            color = Primary300,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
