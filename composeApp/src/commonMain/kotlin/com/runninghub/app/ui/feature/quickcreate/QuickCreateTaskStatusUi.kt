package com.runninghub.app.ui.feature.quickcreate

internal enum class QuickCreateTaskIndicator {
    Progress,
    Success,
    Error,
}

internal data class QuickCreateTaskStatusDisplay(
    val text: String,
    val indicator: QuickCreateTaskIndicator,
)

internal fun quickCreateTaskStatusDisplay(
    status: QuickCreateTaskUiStatus,
    statusText: String?,
): QuickCreateTaskStatusDisplay =
    when (status) {
        QuickCreateTaskUiStatus.FAILED -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "\u751f\u6210\u5931\u8d25",
            indicator = QuickCreateTaskIndicator.Error,
        )
        QuickCreateTaskUiStatus.SUCCESS -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "\u751f\u6210\u5b8c\u6210",
            indicator = QuickCreateTaskIndicator.Success,
        )
        QuickCreateTaskUiStatus.IDLE,
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "\u5904\u7406\u4e2d...",
            indicator = QuickCreateTaskIndicator.Progress,
        )
    }
