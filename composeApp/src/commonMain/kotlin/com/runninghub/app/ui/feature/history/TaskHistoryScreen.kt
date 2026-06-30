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
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoCard
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCard
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionType
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostKind
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusType
import com.runninghub.app.ui.designsystem.components.result.ResultPreview
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState
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
import runninghub.composeapp.generated.resources.task_history_action_download
import runninghub.composeapp.generated.resources.task_history_action_no_retry_params
import runninghub.composeapp.generated.resources.task_history_action_no_reusable_params
import runninghub.composeapp.generated.resources.task_history_action_output_detail_loaded
import runninghub.composeapp.generated.resources.task_history_action_retry
import runninghub.composeapp.generated.resources.task_history_action_retry_params_prepared
import runninghub.composeapp.generated.resources.task_history_action_refund_status
import runninghub.composeapp.generated.resources.task_history_action_reuse
import runninghub.composeapp.generated.resources.task_history_action_reusable_params_prepared_format
import runninghub.composeapp.generated.resources.task_history_action_save
import runninghub.composeapp.generated.resources.task_history_action_view
import runninghub.composeapp.generated.resources.task_history_action_view_result
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
import runninghub.composeapp.generated.resources.task_history_detail_no_billing
import runninghub.composeapp.generated.resources.task_history_detail_output_notice
import runninghub.composeapp.generated.resources.task_history_detail_prompt_parameters
import runninghub.composeapp.generated.resources.task_history_detail_refund_check_available
import runninghub.composeapp.generated.resources.task_history_detail_refund_not_required
import runninghub.composeapp.generated.resources.task_history_detail_request_info
import runninghub.composeapp.generated.resources.task_history_detail_response_info
import runninghub.composeapp.generated.resources.task_history_detail_save_not_saved
import runninghub.composeapp.generated.resources.task_history_detail_save_unavailable
import runninghub.composeapp.generated.resources.task_history_detail_technical_details
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
import runninghub.composeapp.generated.resources.task_history_status_canceled
import runninghub.composeapp.generated.resources.task_history_status_unknown
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
import com.runninghub.feature.task.presentation.TaskHistoryCardAction
import com.runninghub.feature.task.presentation.TaskHistoryCardStatus
import com.runninghub.feature.task.presentation.TaskHistoryCostKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailAction
import com.runninghub.feature.task.presentation.TaskHistoryDetailBillingKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailMediaType
import com.runninghub.feature.task.presentation.TaskHistoryDetailRefundState
import com.runninghub.feature.task.presentation.TaskHistoryDetailSaveState
import com.runninghub.feature.task.presentation.TaskHistoryDetailSectionType
import com.runninghub.feature.task.presentation.TaskHistoryDetailStatus
import com.runninghub.feature.task.presentation.TaskHistoryDetailTechnicalKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailUiModel
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryExpiryUi
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
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
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
                shape = RoundedCornerShape(6.dp),
            )
        },
    )
    Text(
        text = layout.saveStateLabel,
        color = RhMuted,
        style = MaterialTheme.typography.labelSmall,
    )
    Text(
        text = layout.refundStateLabel,
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
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
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
            .background(RhCard)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
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


private fun GenerationHistoryOutput.sizeLabel(): String? =
    width?.let { w -> height?.let { h -> "${w}x$h" } }

@Composable
private fun GenerationHistoryOutput.expireLabelText(): String? =
    expireDays?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.task_history_remaining_days_format, it) }
        ?: expireTime?.takeIf { it.isNotBlank() }

@Composable
private fun TaskHistoryDetailUiModel.toTaskDetailLayoutState(): TaskDetailLayoutState {
    val previewOutput = result?.outputs?.firstOrNull()
    val resultPreview = ResultPreviewState(
        title = stringResource(Res.string.task_history_detail_generation_result),
        taskIdLabel = null,
        statusLabel = status.toTaskDetailStatusLabel(),
        status = status.toRhTaskStatus(),
        media = previewOutput?.toResultPreviewMediaState(),
        expiryLabel = result?.expiry?.toDetailExpiryLabel(),
        actions = actions.map { it.toResultPreviewActionState() },
    )
    return TaskDetailLayoutState(
        sectionOrder = sectionOrder.map { it.toTaskDetailLayoutSectionType() },
        resultPreview = resultPreview,
        billingRows = billing?.rows?.map { it.toBillingInfoRow() }
            ?: listOf(BillingInfoRow(stringResource(Res.string.task_history_detail_cost_info), stringResource(Res.string.task_history_detail_no_billing))),
        saveStateLabel = saveState.toTaskDetailSaveStateLabel(),
        refundStateLabel = refundState.toTaskDetailRefundStateLabel(),
        promptParameters = promptParameters.map { TaskDetailPromptParameterState(label = it.key, value = it.value) },
        technicalSections = technicalSections.map {
            TaskDetailTechnicalSectionState(
                title = it.kind.toTaskDetailTechnicalTitle(),
                content = it.content,
                initiallyExpanded = it.initiallyExpanded,
            )
        },
    )
}

private fun TaskHistoryDetailSectionType.toTaskDetailLayoutSectionType(): TaskDetailLayoutSectionType = when (this) {
    TaskHistoryDetailSectionType.STATUS_SUMMARY -> TaskDetailLayoutSectionType.StatusSummary
    TaskHistoryDetailSectionType.RESULT_PREVIEW -> TaskDetailLayoutSectionType.ResultPreview
    TaskHistoryDetailSectionType.ACTIONS -> TaskDetailLayoutSectionType.Actions
    TaskHistoryDetailSectionType.BILLING -> TaskDetailLayoutSectionType.Billing
    TaskHistoryDetailSectionType.PROMPT_PARAMETERS -> TaskDetailLayoutSectionType.PromptParameters
    TaskHistoryDetailSectionType.TECHNICAL_DETAILS -> TaskDetailLayoutSectionType.TechnicalDetails
}

@Composable
private fun TaskHistoryDetailStatus.toTaskDetailStatusLabel(): String = when (this) {
    TaskHistoryDetailStatus.SUCCESS -> stringResource(Res.string.task_history_status_success)
    TaskHistoryDetailStatus.FAILED -> stringResource(Res.string.task_history_status_failed)
    TaskHistoryDetailStatus.IN_PROGRESS -> stringResource(Res.string.task_history_status_in_progress)
    TaskHistoryDetailStatus.CANCELED -> stringResource(Res.string.task_history_status_canceled)
    TaskHistoryDetailStatus.UNKNOWN -> stringResource(Res.string.task_history_status_unknown)
}

private fun TaskHistoryDetailStatus.toRhTaskStatus(): RhTaskStatus = when (this) {
    TaskHistoryDetailStatus.SUCCESS -> RhTaskStatus.Success
    TaskHistoryDetailStatus.FAILED -> RhTaskStatus.Failed
    TaskHistoryDetailStatus.IN_PROGRESS -> RhTaskStatus.Running
    TaskHistoryDetailStatus.CANCELED -> RhTaskStatus.Canceled
    TaskHistoryDetailStatus.UNKNOWN -> RhTaskStatus.Running
}

private fun TaskHistoryDetailStatus.toLegacyStatusText(): String = when (this) {
    TaskHistoryDetailStatus.SUCCESS -> "SUCCESS"
    TaskHistoryDetailStatus.FAILED -> "FAILED"
    TaskHistoryDetailStatus.IN_PROGRESS -> "RUNNING"
    TaskHistoryDetailStatus.CANCELED -> "CANCELED"
    TaskHistoryDetailStatus.UNKNOWN -> "UNKNOWN"
}

private fun com.runninghub.feature.task.presentation.TaskHistoryDetailOutputUi.toResultPreviewMediaState(): ResultPreviewMediaState? {
    val mediaType = when (mediaType) {
        TaskHistoryDetailMediaType.IMAGE -> ResultPreviewMediaType.Image
        TaskHistoryDetailMediaType.VIDEO -> ResultPreviewMediaType.Video
        TaskHistoryDetailMediaType.FILE -> return null
    }
    return ResultPreviewMediaState(
        url = url,
        previewUrl = previewUrl,
        mediaType = mediaType,
        aspectRatio = aspectRatio,
    )
}

@Composable
private fun TaskHistoryExpiryUi.toDetailExpiryLabel(): String? =
    remainingDays?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.task_history_remaining_days_format, it) }
        ?: expireTime?.takeIf { it.isNotBlank() }

@Composable
private fun TaskHistoryDetailAction.toResultPreviewActionState(): ResultPreviewActionState =
    ResultPreviewActionState(
        type = when (this) {
            TaskHistoryDetailAction.SAVE -> ResultPreviewActionType.Save
            TaskHistoryDetailAction.DOWNLOAD -> ResultPreviewActionType.Download
            TaskHistoryDetailAction.REUSE_PARAMETERS -> ResultPreviewActionType.ReuseParameters
            TaskHistoryDetailAction.RETRY -> ResultPreviewActionType.Retry
            TaskHistoryDetailAction.REFUND_STATUS -> ResultPreviewActionType.RefundStatus
        },
        label = when (this) {
            TaskHistoryDetailAction.SAVE -> stringResource(Res.string.task_history_action_save)
            TaskHistoryDetailAction.DOWNLOAD -> stringResource(Res.string.task_history_action_download)
            TaskHistoryDetailAction.REUSE_PARAMETERS -> stringResource(Res.string.task_history_action_reuse)
            TaskHistoryDetailAction.RETRY -> stringResource(Res.string.task_history_action_retry)
            TaskHistoryDetailAction.REFUND_STATUS -> stringResource(Res.string.task_history_action_refund_status)
        },
    )

@Composable
private fun com.runninghub.feature.task.presentation.TaskHistoryDetailBillingRowUi.toBillingInfoRow(): BillingInfoRow =
    BillingInfoRow(
        label = when (kind) {
            TaskHistoryDetailBillingKind.RH_COINS -> stringResource(Res.string.task_history_detail_metric_rhb)
            TaskHistoryDetailBillingKind.FINAL_AMOUNT -> stringResource(Res.string.task_history_detail_metric_final_amount)
            TaskHistoryDetailBillingKind.OTHER -> stringResource(Res.string.task_history_detail_cost_info)
        },
        value = value,
        emphasized = kind == TaskHistoryDetailBillingKind.RH_COINS || kind == TaskHistoryDetailBillingKind.FINAL_AMOUNT,
    )

@Composable
private fun TaskHistoryDetailSaveState.toTaskDetailSaveStateLabel(): String = when (this) {
    TaskHistoryDetailSaveState.NOT_SAVED -> stringResource(Res.string.task_history_detail_save_not_saved)
    TaskHistoryDetailSaveState.UNAVAILABLE -> stringResource(Res.string.task_history_detail_save_unavailable)
}

@Composable
private fun TaskHistoryDetailRefundState.toTaskDetailRefundStateLabel(): String = when (this) {
    TaskHistoryDetailRefundState.NOT_REQUIRED -> stringResource(Res.string.task_history_detail_refund_not_required)
    TaskHistoryDetailRefundState.CHECK_AVAILABLE -> stringResource(Res.string.task_history_detail_refund_check_available)
}

@Composable
private fun TaskHistoryDetailTechnicalKind.toTaskDetailTechnicalTitle(): String = when (this) {
    TaskHistoryDetailTechnicalKind.REQUEST_INFO -> stringResource(Res.string.task_history_detail_request_info)
    TaskHistoryDetailTechnicalKind.RESPONSE_INFO -> stringResource(Res.string.task_history_detail_response_info)
}

@Composable
private fun TaskHistoryEntry.toHistoryTaskCardState(): HistoryTaskCardState = HistoryTaskCardState(
    title = title,
    thumbnailUrl = thumbnailUrl,
    status = cardStatus.toHistoryTaskCardStatusState(),
    sourceLabel = sourceLabelText(source),
    cost = cost?.let { cost ->
        HistoryTaskCardCostState(
            kind = cost.kind.toHistoryTaskCardCostKind(),
            amountLabel = "${cost.amountText} ${cost.unit}",
        )
    },
    durationLabel = costTime?.takeIf { it.isNotBlank() },
    expiryLabel = expiry?.remainingDays?.takeIf { it.isNotBlank() }?.let { days ->
        stringResource(Res.string.task_history_remaining_days_format, days)
    } ?: expiry?.expireTime?.takeIf { it.isNotBlank() },
    outputCountLabel = stringResource(Res.string.task_history_output_count_format, outputCount),
    primaryAction = primaryAction?.toHistoryTaskCardActionState(),
    secondaryActions = secondaryActions.map { it.toHistoryTaskCardActionState() },
)

@Composable
private fun TaskHistoryCardStatus.toHistoryTaskCardStatusState(): HistoryTaskCardStatusState =
    HistoryTaskCardStatusState(
        type = toHistoryTaskCardStatusType(),
        label = when (this) {
            TaskHistoryCardStatus.SUCCESS -> stringResource(Res.string.task_history_status_success)
            TaskHistoryCardStatus.FAILED -> stringResource(Res.string.task_history_status_failed)
            TaskHistoryCardStatus.IN_PROGRESS -> stringResource(Res.string.task_history_status_in_progress)
            TaskHistoryCardStatus.CANCELED -> stringResource(Res.string.task_history_status_canceled)
            TaskHistoryCardStatus.UNKNOWN -> stringResource(Res.string.task_history_status_unknown)
        },
    )

private fun TaskHistoryCardStatus.toHistoryTaskCardStatusType(): HistoryTaskCardStatusType = when (this) {
    TaskHistoryCardStatus.SUCCESS -> HistoryTaskCardStatusType.Success
    TaskHistoryCardStatus.FAILED -> HistoryTaskCardStatusType.Failed
    TaskHistoryCardStatus.IN_PROGRESS -> HistoryTaskCardStatusType.InProgress
    TaskHistoryCardStatus.CANCELED -> HistoryTaskCardStatusType.Canceled
    TaskHistoryCardStatus.UNKNOWN -> HistoryTaskCardStatusType.Unknown
}

@Composable
private fun TaskHistoryCardAction.toHistoryTaskCardActionState(): HistoryTaskCardActionState =
    HistoryTaskCardActionState(
        type = toHistoryTaskCardActionType(),
        label = when (this) {
            TaskHistoryCardAction.VIEW_RESULT -> stringResource(Res.string.task_history_action_view_result)
            TaskHistoryCardAction.RETRY -> stringResource(Res.string.task_history_action_retry)
            TaskHistoryCardAction.CANCEL -> stringResource(Res.string.task_history_action_cancel)
            TaskHistoryCardAction.REUSE_PARAMETERS -> stringResource(Res.string.task_history_action_reuse)
            TaskHistoryCardAction.VIEW_DETAIL -> stringResource(Res.string.task_history_action_view)
        },
    )

private fun TaskHistoryCardAction.toHistoryTaskCardActionType(): HistoryTaskCardActionType = when (this) {
    TaskHistoryCardAction.VIEW_RESULT -> HistoryTaskCardActionType.ViewResult
    TaskHistoryCardAction.RETRY -> HistoryTaskCardActionType.Retry
    TaskHistoryCardAction.CANCEL -> HistoryTaskCardActionType.Cancel
    TaskHistoryCardAction.REUSE_PARAMETERS -> HistoryTaskCardActionType.ReuseParameters
    TaskHistoryCardAction.VIEW_DETAIL -> HistoryTaskCardActionType.ViewDetail
}

private fun TaskHistoryCostKind.toHistoryTaskCardCostKind(): HistoryTaskCardCostKind = when (this) {
    TaskHistoryCostKind.RHB -> HistoryTaskCardCostKind.Rhb
    TaskHistoryCostKind.FIAT -> HistoryTaskCardCostKind.Fiat
    TaskHistoryCostKind.UNKNOWN -> HistoryTaskCardCostKind.Unknown
}

private fun List<TaskHistoryEntry>.filteredBy(filter: TaskHistoryFilter): List<TaskHistoryEntry> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.cardStatus == TaskHistoryCardStatus.SUCCESS }
    TaskHistoryFilter.FAILED -> filter { it.cardStatus == TaskHistoryCardStatus.FAILED }
    TaskHistoryFilter.IN_PROGRESS -> filter { it.cardStatus == TaskHistoryCardStatus.IN_PROGRESS }
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
