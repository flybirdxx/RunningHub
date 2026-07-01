package com.runninghub.app.ui.feature.history

import com.runninghub.app.ui.designsystem.components.billing.BillingInfoRow
import com.runninghub.app.ui.designsystem.components.result.ResultPreviewState

internal enum class TaskDetailLayoutSectionType {
    StatusSummary,
    ResultPreview,
    Actions,
    Billing,
    PromptParameters,
    TechnicalDetails,
}

internal data class TaskDetailPromptParameterState(
    val label: String,
    val value: String,
)

internal data class TaskDetailTechnicalSectionState(
    val title: String,
    val content: String,
    val initiallyExpanded: Boolean,
)

internal data class TaskDetailLayoutState(
    val sectionOrder: List<TaskDetailLayoutSectionType>,
    val resultPreview: ResultPreviewState,
    val billingRows: List<BillingInfoRow>,
    val saveStateLabel: String,
    val promptParameters: List<TaskDetailPromptParameterState>,
    val technicalSections: List<TaskDetailTechnicalSectionState>,
)
