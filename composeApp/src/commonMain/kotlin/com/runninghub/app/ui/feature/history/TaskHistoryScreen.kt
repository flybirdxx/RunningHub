package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSelected
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_action_cancel
import runninghub.composeapp.generated.resources.task_history_action_cancel_requested
import runninghub.composeapp.generated.resources.task_history_action_no_retry_params
import runninghub.composeapp.generated.resources.task_history_action_no_reusable_params
import runninghub.composeapp.generated.resources.task_history_action_output_detail_loaded
import runninghub.composeapp.generated.resources.task_history_action_retry
import runninghub.composeapp.generated.resources.task_history_action_retry_params_prepared
import runninghub.composeapp.generated.resources.task_history_action_reuse
import runninghub.composeapp.generated.resources.task_history_action_reusable_params_prepared_format
import runninghub.composeapp.generated.resources.task_history_action_view
import runninghub.composeapp.generated.resources.task_history_chevron
import runninghub.composeapp.generated.resources.task_history_detail_api_key_type_member
import runninghub.composeapp.generated.resources.task_history_detail_basic_info
import runninghub.composeapp.generated.resources.task_history_detail_close_content_description
import runninghub.composeapp.generated.resources.task_history_detail_cost_info
import runninghub.composeapp.generated.resources.task_history_detail_empty_outputs
import runninghub.composeapp.generated.resources.task_history_detail_field_account
import runninghub.composeapp.generated.resources.task_history_detail_field_api_key
import runninghub.composeapp.generated.resources.task_history_detail_field_api_key_type
import runninghub.composeapp.generated.resources.task_history_detail_field_call_time
import runninghub.composeapp.generated.resources.task_history_detail_field_call_type
import runninghub.composeapp.generated.resources.task_history_detail_field_discount_amount
import runninghub.composeapp.generated.resources.task_history_detail_field_discount_ratio
import runninghub.composeapp.generated.resources.task_history_detail_field_final_amount
import runninghub.composeapp.generated.resources.task_history_detail_field_mode
import runninghub.composeapp.generated.resources.task_history_detail_field_original_amount
import runninghub.composeapp.generated.resources.task_history_detail_field_rh_coins
import runninghub.composeapp.generated.resources.task_history_detail_field_task_id
import runninghub.composeapp.generated.resources.task_history_detail_field_task_name
import runninghub.composeapp.generated.resources.task_history_detail_field_task_source
import runninghub.composeapp.generated.resources.task_history_detail_generation_result
import runninghub.composeapp.generated.resources.task_history_detail_loading
import runninghub.composeapp.generated.resources.task_history_detail_metric_duration
import runninghub.composeapp.generated.resources.task_history_detail_metric_final_amount
import runninghub.composeapp.generated.resources.task_history_detail_metric_result
import runninghub.composeapp.generated.resources.task_history_detail_metric_rhb
import runninghub.composeapp.generated.resources.task_history_detail_output_notice
import runninghub.composeapp.generated.resources.task_history_detail_request_info
import runninghub.composeapp.generated.resources.task_history_detail_response_info
import runninghub.composeapp.generated.resources.task_history_detail_title
import runninghub.composeapp.generated.resources.task_history_detail_unknown_value
import runninghub.composeapp.generated.resources.task_history_empty_filter
import runninghub.composeapp.generated.resources.task_history_empty_history
import runninghub.composeapp.generated.resources.task_history_error_auth_sync
import runninghub.composeapp.generated.resources.task_history_error_cancel_failed
import runninghub.composeapp.generated.resources.task_history_error_detail_load_failed
import runninghub.composeapp.generated.resources.task_history_error_history_load_failed
import runninghub.composeapp.generated.resources.task_history_filter_all
import runninghub.composeapp.generated.resources.task_history_filter_completed
import runninghub.composeapp.generated.resources.task_history_filter_failed
import runninghub.composeapp.generated.resources.task_history_filter_in_progress
import runninghub.composeapp.generated.resources.task_history_notice_cloud_output
import runninghub.composeapp.generated.resources.task_history_notice_guest
import runninghub.composeapp.generated.resources.task_history_notice_icon
import runninghub.composeapp.generated.resources.task_history_output_count_format
import runninghub.composeapp.generated.resources.task_history_output_detail_title
import runninghub.composeapp.generated.resources.task_history_group_all
import runninghub.composeapp.generated.resources.task_history_reference_title_3d_model
import runninghub.composeapp.generated.resources.task_history_reference_title_character_setting
import runninghub.composeapp.generated.resources.task_history_reference_title_concept_image
import runninghub.composeapp.generated.resources.task_history_reference_title_marketing_video
import runninghub.composeapp.generated.resources.task_history_reference_title_portrait_master
import runninghub.composeapp.generated.resources.task_history_reference_title_video_turbo
import runninghub.composeapp.generated.resources.task_history_refresh_content_description
import runninghub.composeapp.generated.resources.task_history_remaining_days_format
import runninghub.composeapp.generated.resources.task_history_reusable_params_title
import runninghub.composeapp.generated.resources.task_history_search_content_description
import runninghub.composeapp.generated.resources.task_history_source_api_model
import runninghub.composeapp.generated.resources.task_history_source_quick_create
import runninghub.composeapp.generated.resources.task_history_source_webapp
import runninghub.composeapp.generated.resources.task_history_source_workflow
import runninghub.composeapp.generated.resources.task_history_status_failed
import runninghub.composeapp.generated.resources.task_history_status_in_progress
import runninghub.composeapp.generated.resources.task_history_status_success
import runninghub.composeapp.generated.resources.task_history_task_id_format
import runninghub.composeapp.generated.resources.task_history_timeline_completed_format
import runninghub.composeapp.generated.resources.task_history_timeline_failed_format
import runninghub.composeapp.generated.resources.task_history_timeline_running_format
import runninghub.composeapp.generated.resources.task_history_title
import runninghub.composeapp.generated.resources.task_history_total_count_format
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationTaskDetail
import com.runninghub.feature.task.domain.GenerationTaskDetailField
import com.runninghub.feature.task.domain.GenerationTaskDetailFieldKey
import com.runninghub.feature.task.presentation.TaskHistoryActionMessage
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryPresentationError
import com.runninghub.feature.task.presentation.TaskHistoryUiState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class TaskHistoryVoyagerScreen : Screen, KoinComponent {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get(), get()) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHistory() }

        TaskHistoryContent(
            uiState = uiState,
            onFilterSelected = screenModel::setFilter,
            onRetry = screenModel::loadHistory,
            onViewOutput = screenModel::selectOutput,
            onOpenTaskDetail = screenModel::openTaskDetail,
            onCloseTaskDetail = screenModel::closeTaskDetail,
            onReuseParams = screenModel::prepareReuseParams,
            onRetryTask = screenModel::retryTask,
            onCancelTask = screenModel::cancelTask,
        )
    }
}

@Composable
internal fun TaskHistoryContent(
    uiState: TaskHistoryUiState,
    modifier: Modifier = Modifier,
    onFilterSelected: (TaskHistoryFilter) -> Unit = {},
    onRetry: () -> Unit = {},
    onViewOutput: (String) -> Unit = {},
    onOpenTaskDetail: (String) -> Unit = {},
    onCloseTaskDetail: () -> Unit = {},
    onReuseParams: (String) -> Unit = {},
    onRetryTask: (String) -> Unit = {},
    onCancelTask: (String) -> Unit = {},
) {
    val loadedEntries = uiState.items
    val useReferenceFallback = loadedEntries.isEmpty() && uiState.error == TaskHistoryPresentationError.AuthRequired
    val timelineEntries = if (useReferenceFallback) referenceHistoryEntries() else loadedEntries
    val filteredItems = timelineEntries.filteredBy(uiState.filter)
    val errorMessage = uiState.error?.toDisplayHistoryError()
    val actionMessage = uiState.actionMessage?.toDisplayActionMessage()

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = RhBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RhBackground),
            contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { HistoryTopBar(onRetry = onRetry) }
            item {
                TaskHistoryFilterRow(
                    selectedFilter = uiState.filter,
                    onFilterSelected = onFilterSelected,
                )
            }
            item { NoticeBar(useReferenceFallback = useReferenceFallback) }
            if (actionMessage != null || uiState.selectedOutput != null || uiState.reuseParams.isNotEmpty()) {
                item {
                    HistoryActionPanel(
                        message = actionMessage,
                        selectedOutput = uiState.selectedOutput,
                        hasPreparedParams = uiState.reuseParams.isNotEmpty(),
                    )
                }
            }

            when {
                uiState.isLoading && timelineEntries.isEmpty() -> item { LoadingPanel(Modifier.height(360.dp)) }
                !useReferenceFallback && errorMessage != null && timelineEntries.isEmpty() -> item {
                    TaskHistoryErrorState(message = errorMessage, onRetry = onRetry)
                }
                timelineEntries.isEmpty() -> item {
                    TaskHistoryEmptyState(message = stringResource(Res.string.task_history_empty_history))
                }
                filteredItems.isEmpty() -> item {
                    TaskHistoryEmptyState(message = stringResource(Res.string.task_history_empty_filter))
                }
                else -> {
                    item { DateGroupHeader(total = filteredItems.size) }
                    items(items = filteredItems, key = { item -> item.taskId }) { item ->
                        TaskTimelineRow(
                            item = item,
                            onOpenTaskDetail = onOpenTaskDetail,
                            onViewOutput = onViewOutput,
                            onReuseParams = onReuseParams,
                            onRetryTask = onRetryTask,
                            onCancelTask = onCancelTask,
                        )
                    }
                }
            }
        }
    }
        if (uiState.selectedTaskDetail != null || uiState.isTaskDetailLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(onClick = onCloseTaskDetail),
            )
            TaskDetailDrawer(
                detail = uiState.selectedTaskDetail,
                isLoading = uiState.isTaskDetailLoading,
                onClose = onCloseTaskDetail,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun HistoryTopBar(onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(44.dp))
        Text(
            text = stringResource(Res.string.task_history_title),
            color = RhText,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(Res.string.task_history_search_content_description),
                tint = RhText,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onRetry, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.task_history_refresh_content_description),
                tint = RhText,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun TaskHistoryFilterRow(
    selectedFilter: TaskHistoryFilter,
    onFilterSelected: (TaskHistoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            TaskHistoryFilter.ALL to stringResource(Res.string.task_history_filter_all),
            TaskHistoryFilter.IN_PROGRESS to stringResource(Res.string.task_history_filter_in_progress),
            TaskHistoryFilter.COMPLETED to stringResource(Res.string.task_history_filter_completed),
            TaskHistoryFilter.FAILED to stringResource(Res.string.task_history_filter_failed),
        ).forEach { (filter, label) ->
            StatusTab(
                label = label,
                selected = selectedFilter == filter,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun StatusTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = if (selected) BrandLime else RhMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(3.dp)
                .fillMaxWidth(0.78f)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrandLime else Color.Transparent),
        )
    }
}

@Composable
private fun NoticeBar(useReferenceFallback: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.dp, RhMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.task_history_notice_icon),
                color = RhMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = if (useReferenceFallback) {
                stringResource(Res.string.task_history_notice_guest)
            } else {
                stringResource(Res.string.task_history_notice_cloud_output)
            },
            color = RhMuted,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(Res.string.task_history_chevron), color = RhMuted, style = MaterialTheme.typography.titleMedium)
    }
}


@Composable
private fun HistoryActionPanel(
    message: String?,
    selectedOutput: GenerationHistoryOutput?,
    hasPreparedParams: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, BrandLime.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        message?.let {
            Text(
                text = it,
                color = BrandLime,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        selectedOutput?.let { output ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.task_history_output_detail_title),
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = listOfNotNull(output.type.uppercase(), output.sizeLabel(), output.expireLabelText()).joinToString(" / "),
                    color = RhMuted,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = output.url,
                    color = RhMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (hasPreparedParams && message == null) {
            Text(
                text = stringResource(Res.string.task_history_reusable_params_title),
                color = RhText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DateGroupHeader(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(Res.string.task_history_group_all), color = RhText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(stringResource(Res.string.task_history_total_count_format, total), color = RhMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TaskTimelineRow(
    item: TaskHistoryEntry,
    onOpenTaskDetail: (String) -> Unit,
    onViewOutput: (String) -> Unit,
    onReuseParams: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
) {
    val failed = item.status.equals("failed", ignoreCase = true)
    val viewAction = stringResource(Res.string.task_history_action_view)
    val reuseAction = stringResource(Res.string.task_history_action_reuse)
    val retryAction = stringResource(Res.string.task_history_action_retry)
    val cancelAction = stringResource(Res.string.task_history_action_cancel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, if (failed) StatusError.copy(alpha = 0.45f) else RhLine, RoundedCornerShape(8.dp))
            .clickable { onOpenTaskDetail(item.taskId) }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TaskThumbnail(item)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = item.title,
                color = RhText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(item.source)
            }
            if (item.taskId.isNotBlank()) {
                Text(
                    stringResource(Res.string.task_history_task_id_format, item.taskId),
                    color = RhMuted,
                    style = MaterialTheme.typography.labelSmall,
                    softWrap = true,
                )
            }
            item.timelineMetaText()?.let { timelineText ->
                Text(
                    timelineText,
                    color = RhMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(
            modifier = Modifier.width(104.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskStatusPill(status = item.status)
            item.costLabelText()?.let { costLabel ->
                Text(
                    costLabel,
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                stringResource(Res.string.task_history_output_count_format, item.outputCount),
                color = RhMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (item.canViewOutput) {
                    SmallAction(viewAction, highlighted = false, onClick = { item.outputId?.let(onViewOutput) })
                }
                if (item.status.isCompletedStatus() && item.canReuseParams) {
                    SmallAction(reuseAction, highlighted = true, onClick = { onReuseParams(item.taskId) })
                } else if (failed && item.canRetry) {
                    SmallAction(retryAction, highlighted = true, onClick = { onRetryTask(item.taskId) })
                } else if (item.canCancel) {
                    SmallAction(cancelAction, highlighted = true, onClick = { onCancelTask(item.taskId) })
                }
            }
        }
    }
}

@Composable
private fun TaskThumbnail(item: TaskHistoryEntry) {
    val color = when {
        item.status.isCompletedStatus() -> Color(0xFF2F3F2C)
        item.status.equals("failed", ignoreCase = true) -> Color(0xFF3F2020)
        else -> Color(0xFF1D2A35)
    }
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .border(1.dp, RhLine, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = item.thumbnailUrl?.takeIf { it.isNotBlank() }
        if (imageUrl != null) {
            SmartAsyncImage(
                imageUrl = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(6.dp),
            )
        } else {
            Text(
                item.title.take(1),
                color = RhText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun TaskDetailDrawer(
    detail: GenerationTaskDetail?,
    isLoading: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.92f)
            .widthIn(max = 380.dp)
            .background(RhSurface)
            .border(1.dp, RhLine)
            .clickable(onClick = {})
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.task_history_detail_close_content_description),
                    tint = RhMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(Res.string.task_history_detail_title),
                color = RhText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }

        when {
            isLoading -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = BrandLime)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.task_history_detail_loading),
                    color = RhMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            detail != null -> TaskDetailContent(detail)
        }
    }
}

@Composable
private fun TaskDetailContent(detail: GenerationTaskDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskDetailSummary(detail)
        TaskDetailMetrics(detail)
        TaskDetailInfoSection(
            title = stringResource(Res.string.task_history_detail_basic_info),
            fields = detail.basicFields,
        )
        TaskDetailInfoSection(
            title = stringResource(Res.string.task_history_detail_cost_info),
            fields = detail.costFields,
        )
        TaskDetailOutputs(detail.outputs)
        TaskDetailJsonSection(
            title = stringResource(Res.string.task_history_detail_request_info),
            json = detail.requestInfo,
        )
        TaskDetailJsonSection(
            title = stringResource(Res.string.task_history_detail_response_info),
            json = detail.responseInfo,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TaskDetailSummary(detail: GenerationTaskDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = detail.title ?: detail.taskId,
            color = RhText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            detail.sourceLabel?.takeIf { it.isNotBlank() }?.let { SourceBadge(it) }
            TaskStatusPill(detail.status)
        }
        Text(
            text = detail.taskId,
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TaskDetailMetrics(detail: GenerationTaskDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailMetric(
                label = stringResource(Res.string.task_history_detail_metric_rhb),
                value = detail.rhCoins ?: stringResource(Res.string.task_history_detail_unknown_value),
                modifier = Modifier.weight(1f),
            )
            DetailMetric(
                label = stringResource(Res.string.task_history_detail_metric_final_amount),
                value = detail.finalAmount.toDetailMoneyText(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailMetric(
                label = stringResource(Res.string.task_history_detail_metric_duration),
                value = detail.duration?.toDurationLabelText()
                    ?: stringResource(Res.string.task_history_detail_unknown_value),
                modifier = Modifier.weight(1f),
            )
            DetailMetric(
                label = stringResource(Res.string.task_history_detail_metric_result),
                value = statusPillLabelText(detail.status),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DetailMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(value, color = RhText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TaskDetailInfoSection(
    title: String,
    fields: List<GenerationTaskDetailField>,
) {
    if (fields.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = title,
            color = RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        )
        fields.forEach { field ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RhLine),
            )
            DetailFieldRow(field)
        }
    }
}

@Composable
private fun DetailFieldRow(field: GenerationTaskDetailField) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = detailFieldLabelText(field.key),
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(92.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = detailFieldValueText(field),
            color = RhText,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
            softWrap = true,
        )
    }
}

@Composable
private fun TaskDetailOutputs(outputs: List<GenerationHistoryOutput>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.task_history_detail_generation_result),
            color = RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.task_history_detail_output_notice),
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
        )
        if (outputs.isEmpty()) {
            Text(
                text = stringResource(Res.string.task_history_detail_empty_outputs),
                color = RhMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else {
            outputs.forEach { output ->
                val imageUrl = output.thumbnailUrl?.takeIf { it.isNotBlank() } ?: output.url
                SmartAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = output.outputName ?: output.outputId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 260.dp),
                    contentScale = ContentScale.Fit,
                    shape = RoundedCornerShape(6.dp),
                )
            }
        }
    }
}

@Composable
private fun TaskDetailJsonSection(
    title: String,
    json: String?,
) {
    val text = json?.takeIf { it.isNotBlank() } ?: return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            softWrap = true,
        )
    }
}

@Composable
private fun SourceBadge(source: String) {
    val color = if (source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true)) Color(0xFF53D66A) else Color(0xFF60A5FA)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(sourceLabelText(source), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun TaskStatusPill(status: String) {
    val color = when {
        status.isCompletedStatus() -> Color(0xFF4ADE5C)
        status.equals("failed", ignoreCase = true) -> StatusError
        else -> Color(0xFF2F7DFF)
    }
    Box(
        modifier = Modifier
            .height(26.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(statusPillLabelText(status), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun SmallAction(label: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 46.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlighted) RhSelected else RhCard)
            .border(1.dp, if (highlighted) BrandLime else RhLine, RoundedCornerShape(5.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (highlighted) BrandLime else RhText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandLime)
    }
}

@Composable
private fun TaskHistoryErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, color = StatusError, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        SmallAction(stringResource(Res.string.task_history_action_retry), highlighted = true, onClick = onRetry)
    }
}

@Composable
private fun TaskHistoryEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = RhMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}


private fun GenerationHistoryOutput.sizeLabel(): String? =
    width?.let { w -> height?.let { h -> "${w}x$h" } }

@Composable
private fun GenerationHistoryOutput.expireLabelText(): String? =
    expireDays?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.task_history_remaining_days_format, it) }
        ?: expireTime?.takeIf { it.isNotBlank() }

private fun List<TaskHistoryEntry>.filteredBy(filter: TaskHistoryFilter): List<TaskHistoryEntry> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.status.isCompletedStatus() }
    TaskHistoryFilter.FAILED -> filter { it.status.equals("failed", ignoreCase = true) }
    TaskHistoryFilter.IN_PROGRESS -> filter { !it.status.isCompletedStatus() && !it.status.equals("failed", ignoreCase = true) }
}

private fun String.isCompletedStatus(): Boolean = lowercase() in listOf("success", "completed", "done")


@Composable
private fun statusPillLabelText(status: String): String = when {
    status.isCompletedStatus() -> stringResource(Res.string.task_history_status_success)
    status.equals("failed", ignoreCase = true) -> stringResource(Res.string.task_history_status_failed)
    else -> stringResource(Res.string.task_history_status_in_progress)
}


@Composable
private fun TaskHistoryEntry.timelineMetaText(): String? {
    val serverCostTime = costTime?.takeIf { it.isNotBlank() } ?: return null
    return when {
        status.isCompletedStatus() -> stringResource(Res.string.task_history_timeline_completed_format, serverCostTime)
        status.equals("failed", ignoreCase = true) -> stringResource(Res.string.task_history_timeline_failed_format, serverCostTime)
        else -> stringResource(Res.string.task_history_timeline_running_format, serverCostTime)
    }
}


private fun TaskHistoryEntry.costLabelText(): String? {
    val hasAmount = costAmount > 0.0
    val currency = costCurrency?.trim()?.takeIf { it.isNotEmpty() }
    if (!hasAmount) return null
    val amountText = costAmount.toServerAmountText()
    return when {
        currency == null -> amountText
        currency.startsWith("$") || currency.startsWith("￥") -> "$currency$amountText"
        else -> "$amountText $currency"
    }
}

private fun Double.toServerAmountText(): String {
    val raw = toString()
    return if (raw.contains('.')) {
        raw.trimEnd('0').trimEnd('.')
    } else {
        raw
    }
}

@Composable
private fun sourceLabelText(source: String): String = when {
    source.contains("quick", ignoreCase = true) -> stringResource(Res.string.task_history_source_quick_create)
    source.contains("workflow", ignoreCase = true) -> stringResource(Res.string.task_history_source_workflow)
    source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true) -> stringResource(Res.string.task_history_source_api_model)
    source.contains("web", ignoreCase = true) -> stringResource(Res.string.task_history_source_webapp)
    else -> stringResource(Res.string.task_history_source_quick_create)
}

@Composable
private fun detailFieldLabelText(key: GenerationTaskDetailFieldKey): String = when (key) {
    GenerationTaskDetailFieldKey.TASK_ID -> stringResource(Res.string.task_history_detail_field_task_id)
    GenerationTaskDetailFieldKey.CALL_TIME -> stringResource(Res.string.task_history_detail_field_call_time)
    GenerationTaskDetailFieldKey.TASK_NAME -> stringResource(Res.string.task_history_detail_field_task_name)
    GenerationTaskDetailFieldKey.TASK_SOURCE -> stringResource(Res.string.task_history_detail_field_task_source)
    GenerationTaskDetailFieldKey.CALL_TYPE -> stringResource(Res.string.task_history_detail_field_call_type)
    GenerationTaskDetailFieldKey.ACCOUNT -> stringResource(Res.string.task_history_detail_field_account)
    GenerationTaskDetailFieldKey.API_KEY -> stringResource(Res.string.task_history_detail_field_api_key)
    GenerationTaskDetailFieldKey.API_KEY_TYPE -> stringResource(Res.string.task_history_detail_field_api_key_type)
    GenerationTaskDetailFieldKey.MODE -> stringResource(Res.string.task_history_detail_field_mode)
    GenerationTaskDetailFieldKey.ORIGINAL_AMOUNT -> stringResource(Res.string.task_history_detail_field_original_amount)
    GenerationTaskDetailFieldKey.DISCOUNT_RATIO -> stringResource(Res.string.task_history_detail_field_discount_ratio)
    GenerationTaskDetailFieldKey.DISCOUNT_AMOUNT -> stringResource(Res.string.task_history_detail_field_discount_amount)
    GenerationTaskDetailFieldKey.FINAL_AMOUNT -> stringResource(Res.string.task_history_detail_field_final_amount)
    GenerationTaskDetailFieldKey.RH_COINS -> stringResource(Res.string.task_history_detail_field_rh_coins)
}

@Composable
private fun detailFieldValueText(field: GenerationTaskDetailField): String =
    if (field.key == GenerationTaskDetailFieldKey.API_KEY_TYPE && field.value == "1") {
        stringResource(Res.string.task_history_detail_api_key_type_member)
    } else {
        field.value
    }

@Composable
private fun String?.toDetailMoneyText(): String {
    val value = this?.trim()?.takeIf { it.isNotEmpty() }
        ?: return stringResource(Res.string.task_history_detail_unknown_value)
    return when {
        value.startsWith("¥") || value.startsWith("￥") || value.startsWith("$") -> value
        value.contains("CNY", ignoreCase = true) || value.contains("RMB", ignoreCase = true) -> value
        else -> "¥$value"
    }
}

private fun String.toDurationLabelText(): String {
    val seconds = trim().toLongOrNull() ?: return this
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) {
        "${hours.toTwoDigits()}:${minutes.toTwoDigits()}:${remainingSeconds.toTwoDigits()}"
    } else {
        "${minutes.toTwoDigits()}:${remainingSeconds.toTwoDigits()}"
    }
}

private fun Long.toTwoDigits(): String = toString().padStart(2, '0')

@Composable
private fun TaskHistoryActionMessage.toDisplayActionMessage(): String = when (this) {
    TaskHistoryActionMessage.OutputDetailLoaded ->
        stringResource(Res.string.task_history_action_output_detail_loaded)
    TaskHistoryActionMessage.NoReusableParams ->
        stringResource(Res.string.task_history_action_no_reusable_params)
    is TaskHistoryActionMessage.ReusableParamsPrepared ->
        stringResource(Res.string.task_history_action_reusable_params_prepared_format, count)
    TaskHistoryActionMessage.NoRetryParams ->
        stringResource(Res.string.task_history_action_no_retry_params)
    TaskHistoryActionMessage.RetryParamsPrepared ->
        stringResource(Res.string.task_history_action_retry_params_prepared)
    TaskHistoryActionMessage.CancelRequested ->
        stringResource(Res.string.task_history_action_cancel_requested)
}


@Composable
private fun TaskHistoryPresentationError.toDisplayHistoryError(): String = when (this) {
    TaskHistoryPresentationError.AuthRequired -> stringResource(Res.string.task_history_error_auth_sync)
    TaskHistoryPresentationError.DetailLoadFailed -> stringResource(Res.string.task_history_error_detail_load_failed)
    TaskHistoryPresentationError.CancelFailed -> stringResource(Res.string.task_history_error_cancel_failed)
    TaskHistoryPresentationError.HistoryLoadFailed -> stringResource(Res.string.task_history_error_history_load_failed)
}


@Composable
private fun referenceHistoryEntries(): List<TaskHistoryEntry> = listOf(
    TaskHistoryEntry("a1b2c3d4", stringResource(Res.string.task_history_reference_title_concept_image), "running", "02:18", "quick_creation"),
    TaskHistoryEntry("e5f6g7h8", stringResource(Res.string.task_history_reference_title_portrait_master), "completed", "00:42", "api_model"),
    TaskHistoryEntry("i9j0k1l2", stringResource(Res.string.task_history_reference_title_3d_model), "failed", "01:15", "webapp"),
    TaskHistoryEntry(
        taskId = "m3n4o5p6",
        title = stringResource(Res.string.task_history_reference_title_video_turbo),
        status = "completed",
        costTime = "02:36",
        source = "quick_creation",
    ),
    TaskHistoryEntry(
        taskId = "q7r8s9t0",
        title = stringResource(Res.string.task_history_reference_title_character_setting),
        status = "completed",
        costTime = "00:58",
        source = "api_model",
    ),
    TaskHistoryEntry(
        taskId = "u1v2w3x4",
        title = stringResource(Res.string.task_history_reference_title_marketing_video),
        status = "running",
        costTime = "03:42",
        source = "webapp",
    ),
)

@Composable
private fun TaskHistoryAdaptivePreview(
    spec: RhPreviewSpec,
    filter: TaskHistoryFilter = TaskHistoryFilter.ALL,
) {
    RhAdaptivePreview(spec = spec) {
        TaskHistoryContent(
            uiState = previewTaskHistoryUiState(filter = filter),
            onFilterSelected = {},
            onRetry = {},
            onViewOutput = {},
            onOpenTaskDetail = {},
            onCloseTaskDetail = {},
            onReuseParams = {},
            onRetryTask = {},
            onCancelTask = {},
        )
    }
}

@Preview
@Composable
private fun TaskHistoryPhone320Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun TaskHistoryPhone360Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun TaskHistoryPhone430Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun TaskHistoryMedium600Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun TaskHistoryExpanded840Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun TaskHistoryLandscapePreview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun TaskHistoryFontScale13Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun TaskHistoryFontScale15Preview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.FontScale15)
}

@Preview
@Composable
private fun TaskHistoryFailedFilterPreview() {
    TaskHistoryAdaptivePreview(RhPreviewSpec.Phone360, filter = TaskHistoryFilter.FAILED)
}

private val RhBackground = RhAppBackground
private val RhSurface = RhAppSurface
private val RhCard = RhAppCard
private val RhSelected = RhAppSelected
private val RhLine = RhAppLine
private val RhText = RhAppText
private val RhMuted = RhAppMuted
