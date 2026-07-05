package com.runninghub.app.ui.feature.history

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardActionType
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostKind
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardCostState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskCardStatusType
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskSourceBadgeState
import com.runninghub.app.ui.designsystem.components.cards.HistoryTaskSourceBadgeType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewActionType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaState
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewMediaType
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.presentation.TaskHistoryActionMessage
import com.runninghub.feature.task.presentation.TaskHistoryCardAction
import com.runninghub.feature.task.presentation.TaskHistoryCardStatus
import com.runninghub.feature.task.presentation.TaskHistoryCostKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailAction
import com.runninghub.feature.task.presentation.TaskHistoryDetailBillingKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailMediaType
import com.runninghub.feature.task.presentation.TaskHistoryDetailSaveState
import com.runninghub.feature.task.presentation.TaskHistoryDetailSectionType
import com.runninghub.feature.task.presentation.TaskHistoryDetailStatus
import com.runninghub.feature.task.presentation.TaskHistoryDetailTechnicalKind
import com.runninghub.feature.task.presentation.TaskHistoryDetailUiModel
import com.runninghub.feature.task.presentation.TaskHistoryEntry
import com.runninghub.feature.task.presentation.TaskHistoryExpiryUi
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import com.runninghub.feature.task.presentation.TaskHistoryPresentationError
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.task_history_action_cancel
import runninghub.composeapp.generated.resources.task_history_action_cancel_requested
import runninghub.composeapp.generated.resources.task_history_action_download
import runninghub.composeapp.generated.resources.task_history_action_no_retry_params
import runninghub.composeapp.generated.resources.task_history_action_no_reusable_params
import runninghub.composeapp.generated.resources.task_history_action_output_detail_loaded
import runninghub.composeapp.generated.resources.task_history_action_retry
import runninghub.composeapp.generated.resources.task_history_action_retry_params_prepared
import runninghub.composeapp.generated.resources.task_history_action_reuse
import runninghub.composeapp.generated.resources.task_history_action_reusable_params_prepared_format
import runninghub.composeapp.generated.resources.task_history_action_save
import runninghub.composeapp.generated.resources.task_history_action_view
import runninghub.composeapp.generated.resources.task_history_action_view_result
import runninghub.composeapp.generated.resources.task_history_detail_cost_info
import runninghub.composeapp.generated.resources.task_history_detail_generation_result
import runninghub.composeapp.generated.resources.task_history_detail_metric_duration
import runninghub.composeapp.generated.resources.task_history_detail_metric_final_amount
import runninghub.composeapp.generated.resources.task_history_detail_metric_rhb
import runninghub.composeapp.generated.resources.task_history_detail_no_billing
import runninghub.composeapp.generated.resources.task_history_detail_request_info
import runninghub.composeapp.generated.resources.task_history_detail_response_info
import runninghub.composeapp.generated.resources.task_history_detail_save_not_saved
import runninghub.composeapp.generated.resources.task_history_detail_save_unavailable
import runninghub.composeapp.generated.resources.task_history_error_auth_sync
import runninghub.composeapp.generated.resources.task_history_error_cancel_failed
import runninghub.composeapp.generated.resources.task_history_error_detail_load_failed
import runninghub.composeapp.generated.resources.task_history_error_history_load_failed
import runninghub.composeapp.generated.resources.task_history_output_count_format
import runninghub.composeapp.generated.resources.task_history_remaining_days_format
import runninghub.composeapp.generated.resources.task_history_source_api_model
import runninghub.composeapp.generated.resources.task_history_source_badge_api_model
import runninghub.composeapp.generated.resources.task_history_source_badge_quick_create
import runninghub.composeapp.generated.resources.task_history_source_badge_webapp
import runninghub.composeapp.generated.resources.task_history_source_badge_workflow
import runninghub.composeapp.generated.resources.task_history_source_quick_create
import runninghub.composeapp.generated.resources.task_history_source_webapp
import runninghub.composeapp.generated.resources.task_history_source_workflow
import runninghub.composeapp.generated.resources.task_history_status_canceled
import runninghub.composeapp.generated.resources.task_history_status_failed
import runninghub.composeapp.generated.resources.task_history_status_in_progress
import runninghub.composeapp.generated.resources.task_history_status_success
import runninghub.composeapp.generated.resources.task_history_status_unknown

internal fun GenerationHistoryOutput.sizeLabel(): String? =
    width?.let { w -> height?.let { h -> "${w}x$h" } }

@Composable
internal fun GenerationHistoryOutput.expireLabelText(): String? =
    expireDays?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.task_history_remaining_days_format, it) }
        ?: expireTime?.takeIf { it.isNotBlank() }

@Composable
internal fun TaskHistoryDetailUiModel.toTaskDetailLayoutState(): TaskDetailLayoutState {
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

internal fun TaskHistoryDetailStatus.toRhTaskStatus(): RhTaskStatus = when (this) {
    TaskHistoryDetailStatus.SUCCESS -> RhTaskStatus.Success
    TaskHistoryDetailStatus.FAILED -> RhTaskStatus.Failed
    TaskHistoryDetailStatus.IN_PROGRESS -> RhTaskStatus.Running
    TaskHistoryDetailStatus.CANCELED -> RhTaskStatus.Canceled
    TaskHistoryDetailStatus.UNKNOWN -> RhTaskStatus.Running
}

internal fun TaskHistoryDetailStatus.toLegacyStatusText(): String = when (this) {
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
        },
        label = when (this) {
            TaskHistoryDetailAction.SAVE -> stringResource(Res.string.task_history_action_save)
            TaskHistoryDetailAction.DOWNLOAD -> stringResource(Res.string.task_history_action_download)
            TaskHistoryDetailAction.REUSE_PARAMETERS -> stringResource(Res.string.task_history_action_reuse)
            TaskHistoryDetailAction.RETRY -> stringResource(Res.string.task_history_action_retry)
        },
    )

@Composable
private fun com.runninghub.feature.task.presentation.TaskHistoryDetailBillingRowUi.toBillingInfoRow(): BillingInfoRow =
    BillingInfoRow(
        label = when (kind) {
            TaskHistoryDetailBillingKind.RH_COINS -> stringResource(Res.string.task_history_detail_metric_rhb)
            TaskHistoryDetailBillingKind.FINAL_AMOUNT -> stringResource(Res.string.task_history_detail_metric_final_amount)
            TaskHistoryDetailBillingKind.DURATION -> stringResource(Res.string.task_history_detail_metric_duration)
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
private fun TaskHistoryDetailTechnicalKind.toTaskDetailTechnicalTitle(): String = when (this) {
    TaskHistoryDetailTechnicalKind.REQUEST_INFO -> stringResource(Res.string.task_history_detail_request_info)
    TaskHistoryDetailTechnicalKind.RESPONSE_INFO -> stringResource(Res.string.task_history_detail_response_info)
}

@Composable
internal fun TaskHistoryEntry.toHistoryTaskCardState(): HistoryTaskCardState = HistoryTaskCardState(
    title = title,
    thumbnailUrl = thumbnailUrl,
    status = cardStatus.toHistoryTaskCardStatusState(),
    sourceLabel = sourceLabelText(source),
    sourceBadge = sourceBadgeState(source),
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

internal fun List<TaskHistoryEntry>.filteredBy(filter: TaskHistoryFilter): List<TaskHistoryEntry> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.cardStatus == TaskHistoryCardStatus.SUCCESS }
    TaskHistoryFilter.FAILED -> filter { it.cardStatus == TaskHistoryCardStatus.FAILED }
    TaskHistoryFilter.IN_PROGRESS -> filter { it.cardStatus == TaskHistoryCardStatus.IN_PROGRESS }
}

internal fun String.isCompletedStatus(): Boolean = lowercase() in listOf("success", "completed", "done")


@Composable
internal fun statusPillLabelText(status: String): String = when {
    status.isCompletedStatus() -> stringResource(Res.string.task_history_status_success)
    status.equals("failed", ignoreCase = true) -> stringResource(Res.string.task_history_status_failed)
    else -> stringResource(Res.string.task_history_status_in_progress)
}


@Composable
internal fun sourceLabelText(source: String): String = when {
    source.contains("quick", ignoreCase = true) -> stringResource(Res.string.task_history_source_quick_create)
    source.contains("workflow", ignoreCase = true) -> stringResource(Res.string.task_history_source_workflow)
    source.contains("api", ignoreCase = true) || source.contains("model", ignoreCase = true) -> stringResource(Res.string.task_history_source_api_model)
    source.contains("web", ignoreCase = true) -> stringResource(Res.string.task_history_source_webapp)
    else -> stringResource(Res.string.task_history_source_quick_create)
}

@Composable
private fun sourceBadgeState(source: String): HistoryTaskSourceBadgeState {
    val type = source.toHistoryTaskSourceBadgeType()
    return HistoryTaskSourceBadgeState(
        type = type,
        label = when (type) {
            HistoryTaskSourceBadgeType.QuickCreate ->
                stringResource(Res.string.task_history_source_badge_quick_create)
            HistoryTaskSourceBadgeType.Workflow ->
                stringResource(Res.string.task_history_source_badge_workflow)
            HistoryTaskSourceBadgeType.Api ->
                stringResource(Res.string.task_history_source_badge_api_model)
            HistoryTaskSourceBadgeType.WebApp ->
                stringResource(Res.string.task_history_source_badge_webapp)
        },
        contentDescription = sourceLabelText(source),
    )
}

private fun String.toHistoryTaskSourceBadgeType(): HistoryTaskSourceBadgeType = when {
    contains("quick", ignoreCase = true) -> HistoryTaskSourceBadgeType.QuickCreate
    contains("workflow", ignoreCase = true) -> HistoryTaskSourceBadgeType.Workflow
    contains("api", ignoreCase = true) || contains("model", ignoreCase = true) -> HistoryTaskSourceBadgeType.Api
    contains("web", ignoreCase = true) -> HistoryTaskSourceBadgeType.WebApp
    else -> HistoryTaskSourceBadgeType.QuickCreate
}

@Composable
internal fun TaskHistoryActionMessage.toDisplayActionMessage(): String = when (this) {
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
internal fun TaskHistoryPresentationError.toDisplayHistoryError(): String = when (this) {
    TaskHistoryPresentationError.AuthRequired -> stringResource(Res.string.task_history_error_auth_sync)
    TaskHistoryPresentationError.DetailLoadFailed -> stringResource(Res.string.task_history_error_detail_load_failed)
    TaskHistoryPresentationError.CancelFailed -> stringResource(Res.string.task_history_error_cancel_failed)
    TaskHistoryPresentationError.HistoryLoadFailed -> stringResource(Res.string.task_history_error_history_load_failed)
}
