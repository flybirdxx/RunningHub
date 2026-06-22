package com.runninghub.app.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState
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
import runninghub.composeapp.generated.resources.task_history_cost_character
import runninghub.composeapp.generated.resources.task_history_cost_default
import runninghub.composeapp.generated.resources.task_history_cost_failed
import runninghub.composeapp.generated.resources.task_history_cost_video
import runninghub.composeapp.generated.resources.task_history_current_project
import runninghub.composeapp.generated.resources.task_history_default_completed_duration
import runninghub.composeapp.generated.resources.task_history_default_failed_duration
import runninghub.composeapp.generated.resources.task_history_default_project
import runninghub.composeapp.generated.resources.task_history_default_running_duration
import runninghub.composeapp.generated.resources.task_history_dropdown_symbol
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
import runninghub.composeapp.generated.resources.task_history_output_count_completed
import runninghub.composeapp.generated.resources.task_history_output_count_failed
import runninghub.composeapp.generated.resources.task_history_output_count_running
import runninghub.composeapp.generated.resources.task_history_output_count_video
import runninghub.composeapp.generated.resources.task_history_output_detail_title
import runninghub.composeapp.generated.resources.task_history_pin_project_action
import runninghub.composeapp.generated.resources.task_history_pinned_projects
import runninghub.composeapp.generated.resources.task_history_reference_date
import runninghub.composeapp.generated.resources.task_history_reference_project_character_concept
import runninghub.composeapp.generated.resources.task_history_reference_project_marketing_video
import runninghub.composeapp.generated.resources.task_history_reference_title_3d_model
import runninghub.composeapp.generated.resources.task_history_reference_title_character_setting
import runninghub.composeapp.generated.resources.task_history_reference_title_concept_image
import runninghub.composeapp.generated.resources.task_history_reference_title_marketing_video
import runninghub.composeapp.generated.resources.task_history_reference_title_portrait_master
import runninghub.composeapp.generated.resources.task_history_reference_title_video_turbo
import runninghub.composeapp.generated.resources.task_history_refresh_content_description
import runninghub.composeapp.generated.resources.task_history_remaining_days_format
import runninghub.composeapp.generated.resources.task_history_reusable_param_format
import runninghub.composeapp.generated.resources.task_history_reusable_params_title
import runninghub.composeapp.generated.resources.task_history_search_content_description
import runninghub.composeapp.generated.resources.task_history_source_api_model
import runninghub.composeapp.generated.resources.task_history_source_quick_create
import runninghub.composeapp.generated.resources.task_history_source_webapp
import runninghub.composeapp.generated.resources.task_history_status_failed
import runninghub.composeapp.generated.resources.task_history_status_in_progress
import runninghub.composeapp.generated.resources.task_history_status_in_progress_percent
import runninghub.composeapp.generated.resources.task_history_status_success
import runninghub.composeapp.generated.resources.task_history_task_id_format
import runninghub.composeapp.generated.resources.task_history_timeline_completed_format
import runninghub.composeapp.generated.resources.task_history_timeline_failed_format
import runninghub.composeapp.generated.resources.task_history_timeline_running_format
import runninghub.composeapp.generated.resources.task_history_title
import runninghub.composeapp.generated.resources.task_history_today
import runninghub.composeapp.generated.resources.task_history_total_count_format
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.presentation.TaskHistoryActionMessage
import com.runninghub.feature.task.presentation.TaskHistoryCostText
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryOutputCountText
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
        val screenModel = rememberScreenModel { TaskHistoryScreenModel(get()) }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHistory() }

        TaskHistoryContent(
            uiState = uiState,
            onFilterSelected = screenModel::setFilter,
            onRetry = screenModel::loadHistory,
            onViewOutput = screenModel::selectOutput,
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

    Scaffold(
        modifier = modifier,
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
            item { CurrentProjectCard() }
            item { PinnedProjectStrip() }
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
                        reuseParams = uiState.reuseParams,
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
                        TaskTimelineRow(item = item, onViewOutput = onViewOutput, onReuseParams = onReuseParams, onRetryTask = onRetryTask, onCancelTask = onCancelTask)
                    }
                }
            }
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

private fun CurrentProjectCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.task_history_current_project),
            color = RhMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(76.dp),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(RhCard)
                .border(1.dp, RhLine, RoundedCornerShape(7.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FolderGlyph()
            Text(
                stringResource(Res.string.task_history_default_project),
                color = RhText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.task_history_dropdown_symbol), color = RhMuted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable

private fun PinnedProjectStrip() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.task_history_pinned_projects), color = RhMuted, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProjectChip(stringResource(Res.string.task_history_pin_project_action), selected = false, dashed = true)
            ProjectChip(stringResource(Res.string.task_history_default_project), selected = true)
            ProjectChip(stringResource(Res.string.task_history_reference_project_marketing_video), selected = false)
            ProjectChip(stringResource(Res.string.task_history_reference_project_character_concept), selected = false)
        }
    }
}

@Composable
private fun ProjectChip(label: String, selected: Boolean, dashed: Boolean = false) {
    Row(
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 108.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) RhSelected else RhCard)
            .border(1.dp, if (selected) BrandLime else RhLine, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!dashed) FolderGlyph()
        Text(label, color = if (selected) RhText else RhMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
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
    reuseParams: Map<String, String>,
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
        if (reuseParams.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(Res.string.task_history_reusable_params_title),
                    color = RhText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                reuseParams.entries.take(3).forEach { (key, value) ->
                    Text(
                        text = stringResource(Res.string.task_history_reusable_param_format, key, value),
                        color = RhMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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
        Text(stringResource(Res.string.task_history_today), color = RhText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(Res.string.task_history_reference_date), color = RhMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(stringResource(Res.string.task_history_total_count_format, total), color = RhMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TaskTimelineRow(
    item: TaskHistoryEntry,
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
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TaskThumbnail(item)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceBadge(item.source)
                Text(
                    text = item.title,
                    color = RhText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(stringResource(Res.string.task_history_task_id_format, item.taskId), color = RhMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            ProgressLine(item)
            Text(item.timelineMetaText(), color = RhMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            modifier = Modifier.width(104.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TaskStatusPill(status = item.status)
            Text(item.costText.toDisplayCostLabel(), color = RhText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(item.outputCountText.toDisplayOutputCountLabelText(), color = RhMuted, style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallAction(viewAction, highlighted = false, onClick = { item.outputId?.let(onViewOutput) })
                if (item.status.isCompletedStatus()) {
                    SmallAction(reuseAction, highlighted = true, onClick = { onReuseParams(item.taskId) })
                } else if (failed) {
                    SmallAction(retryAction, highlighted = true, onClick = { onRetryTask(item.taskId) })
                } else {
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
        Text(item.title.take(1), color = RhText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
private fun ProgressLine(item: TaskHistoryEntry) {
    val running = !item.status.isCompletedStatus() && !item.status.equals("failed", ignoreCase = true)
    val failed = item.status.equals("failed", ignoreCase = true)
    val color = when {
        failed -> StatusError
        running -> BrandLime
        else -> BrandLime
    }
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(item.statusDisplayText(), color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        if (running) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF41464A)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(4.dp)
                        .background(BrandLime),
                )
            }
        }
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
private fun FolderGlyph() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .border(1.dp, RhMuted, RoundedCornerShape(3.dp)),
    )
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
private fun TaskHistoryEntry.statusDisplayText(): String = when {
    status.isCompletedStatus() -> stringResource(Res.string.task_history_status_success)
    status.equals("failed", ignoreCase = true) -> stringResource(Res.string.task_history_status_failed)
    else -> stringResource(Res.string.task_history_status_in_progress_percent)
}


@Composable
private fun TaskHistoryEntry.timelineMetaText(): String = when {
    status.isCompletedStatus() -> stringResource(
        Res.string.task_history_timeline_completed_format,
        costTime ?: stringResource(Res.string.task_history_default_completed_duration),
    )
    status.equals("failed", ignoreCase = true) -> stringResource(
        Res.string.task_history_timeline_failed_format,
        costTime ?: stringResource(Res.string.task_history_default_failed_duration),
    )
    else -> stringResource(
        Res.string.task_history_timeline_running_format,
        costTime ?: stringResource(Res.string.task_history_default_running_duration),
    )
}


@Composable
private fun TaskHistoryCostText.toDisplayCostLabel(): String = when (this) {
    TaskHistoryCostText.VIDEO -> stringResource(Res.string.task_history_cost_video)
    TaskHistoryCostText.CHARACTER -> stringResource(Res.string.task_history_cost_character)
    TaskHistoryCostText.FAILED -> stringResource(Res.string.task_history_cost_failed)
    TaskHistoryCostText.DEFAULT -> stringResource(Res.string.task_history_cost_default)
}


@Composable
private fun TaskHistoryOutputCountText.toDisplayOutputCountLabelText(): String = when (this) {
    TaskHistoryOutputCountText.FAILED -> stringResource(Res.string.task_history_output_count_failed)
    TaskHistoryOutputCountText.RUNNING -> stringResource(Res.string.task_history_output_count_running)
    TaskHistoryOutputCountText.VIDEO -> stringResource(Res.string.task_history_output_count_video)
    TaskHistoryOutputCountText.COMPLETED -> stringResource(Res.string.task_history_output_count_completed)
}


@Composable
private fun sourceLabelText(source: String): String = when {
    source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true) -> stringResource(Res.string.task_history_source_api_model)
    source.contains("web", ignoreCase = true) -> stringResource(Res.string.task_history_source_webapp)
    else -> stringResource(Res.string.task_history_source_quick_create)
}

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
        "m3n4o5p6",
        stringResource(Res.string.task_history_reference_title_video_turbo),
        "completed",
        "02:36",
        "quick_creation",
        costText = TaskHistoryCostText.VIDEO,
        outputCountText = TaskHistoryOutputCountText.VIDEO,
    ),
    TaskHistoryEntry(
        "q7r8s9t0",
        stringResource(Res.string.task_history_reference_title_character_setting),
        "completed",
        "00:58",
        "api_model",
        costText = TaskHistoryCostText.CHARACTER,
    ),
    TaskHistoryEntry(
        "u1v2w3x4",
        stringResource(Res.string.task_history_reference_title_marketing_video),
        "running",
        "03:42",
        "webapp",
        costText = TaskHistoryCostText.VIDEO,
        outputCountText = TaskHistoryOutputCountText.RUNNING,
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
