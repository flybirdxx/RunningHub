package com.runninghub.app.ui.feature.history

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState
import com.runninghub.feature.task.presentation.TaskHistoryFilter
import org.jetbrains.compose.ui.tooling.preview.Preview

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
