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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoCard
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCard
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreview
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSelected
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_action_retry
import runninghub.composeapp.generated.resources.task_history_chevron
import runninghub.composeapp.generated.resources.task_history_detail_close_content_description
import runninghub.composeapp.generated.resources.task_history_detail_empty_outputs
import runninghub.composeapp.generated.resources.task_history_detail_loading
import runninghub.composeapp.generated.resources.task_history_detail_metric_result
import runninghub.composeapp.generated.resources.task_history_detail_output_notice
import runninghub.composeapp.generated.resources.task_history_detail_prompt_parameters
import runninghub.composeapp.generated.resources.task_history_detail_technical_details
import runninghub.composeapp.generated.resources.task_history_detail_title
import runninghub.composeapp.generated.resources.task_history_empty_filter
import runninghub.composeapp.generated.resources.task_history_empty_history
import runninghub.composeapp.generated.resources.task_history_filter_all
import runninghub.composeapp.generated.resources.task_history_filter_completed
import runninghub.composeapp.generated.resources.task_history_filter_failed
import runninghub.composeapp.generated.resources.task_history_filter_in_progress
import runninghub.composeapp.generated.resources.task_history_notice_cloud_output
import runninghub.composeapp.generated.resources.task_history_notice_icon
import runninghub.composeapp.generated.resources.task_history_output_detail_title
import runninghub.composeapp.generated.resources.task_history_group_all
import runninghub.composeapp.generated.resources.task_history_refresh_content_description
import runninghub.composeapp.generated.resources.task_history_reusable_params_title
import runninghub.composeapp.generated.resources.task_history_search_content_description
import runninghub.composeapp.generated.resources.task_history_task_id_format
import runninghub.composeapp.generated.resources.task_history_title
import runninghub.composeapp.generated.resources.task_history_total_count_format
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationTaskDetail
import com.runninghub.feature.task.domain.GenerationTaskDetailField
import com.runninghub.feature.task.presentation.TaskHistoryDetailUiModel
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryFilter
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
    val timelineEntries = loadedEntries
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
            item { NoticeBar() }
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
                errorMessage != null && timelineEntries.isEmpty() -> item {
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
        if (uiState.selectedTaskDetailUi != null || uiState.isTaskDetailLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable(onClick = onCloseTaskDetail),
            )
            TaskDetailDrawer(
                detail = uiState.selectedTaskDetailUi,
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
            .background(RhSurface),
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
private fun NoticeBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(RhSurface)
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
            text = stringResource(Res.string.task_history_notice_cloud_output),
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
    HistoryTaskCard(
        state = item.toHistoryTaskCardState(),
        onClick = { onOpenTaskDetail(item.taskId) },
        onAction = { action ->
            when (action) {
                HistoryTaskCardActionType.ViewResult -> item.outputId?.let(onViewOutput)
                HistoryTaskCardActionType.Retry -> onRetryTask(item.taskId)
                HistoryTaskCardActionType.Cancel -> onCancelTask(item.taskId)
                HistoryTaskCardActionType.ReuseParameters -> onReuseParams(item.taskId)
                HistoryTaskCardActionType.ViewDetail -> onOpenTaskDetail(item.taskId)
            }
        },
        thumbnailContent = {
            TaskThumbnail(
                item = item,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun TaskThumbnail(
    item: TaskHistoryEntry,
    modifier: Modifier = Modifier.size(84.dp),
) {
    val color = when {
        item.status.isCompletedStatus() -> Color(0xFF2F3F2C)
        item.status.equals("failed", ignoreCase = true) -> Color(0xFF3F2020)
        else -> Color(0xFF1D2A35)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = item.thumbnailUrl?.takeIf { it.isNotBlank() }
        if (imageUrl != null) {
            SmartAsyncImage(
                imageUrl = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
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
    detail: TaskHistoryDetailUiModel?,
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
private fun TaskDetailContent(detail: TaskHistoryDetailUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val layout = detail.toTaskDetailLayoutState()
        TaskDetailStatusSummary(detail)
        TaskDetailResultPreview(layout)
        TaskDetailBillingSection(layout)
        TaskDetailPromptParameters(layout)
        TaskDetailTechnicalDetails(layout)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TaskDetailStatusSummary(detail: TaskHistoryDetailUiModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = detail.title,
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
            TaskStatusPill(detail.status.toLegacyStatusText())
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
private fun TaskDetailResultPreview(layout: TaskDetailLayoutState) {
    ResultPreview(
        state = layout.resultPreview,
        onAction = {},
        mediaContent = { media ->
            SmartAsyncImage(
                imageUrl = media.renderUrl,
                contentDescription = layout.resultPreview.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        },
    )
    Text(
        text = layout.saveStateLabel,
        color = RhMuted,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun TaskDetailBillingSection(layout: TaskDetailLayoutState) {
    BillingInfoCard(rows = layout.billingRows)
}

@Composable
private fun TaskDetailPromptParameters(layout: TaskDetailLayoutState) {
    if (layout.promptParameters.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard),
    ) {
        Text(
            text = stringResource(Res.string.task_history_detail_prompt_parameters),
            color = RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
        )
        layout.promptParameters.forEach { param ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RhLine),
            )
            DetailKeyValueRow(label = param.label, value = param.value)
        }
    }
}

@Composable
private fun TaskDetailTechnicalDetails(layout: TaskDetailLayoutState) {
    if (layout.technicalSections.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.task_history_detail_technical_details),
            color = RhText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        layout.technicalSections.forEach { section ->
            TaskDetailTechnicalSection(section)
        }
    }
}

@Composable
private fun TaskDetailTechnicalSection(section: TaskDetailTechnicalSectionState) {
    var expanded by remember(section.title, section.content) { mutableStateOf(section.initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(RhCard),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title,
                color = RhText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.task_history_chevron), color = RhMuted, style = MaterialTheme.typography.titleMedium)
        }
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(RhLine),
            )
            Text(
                text = section.content,
                color = RhMuted,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                softWrap = true,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
private fun DetailKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = RhMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(92.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = RhText,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
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
