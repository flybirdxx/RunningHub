package com.runninghub.feature.quickcreate.presentation.result

import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickCreateTaskStatusUiTest {
    @Test
    fun `failed task status keeps stable error semantic`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.FAILED,
            statusText = QuickCreateTaskStatusText.Error(QuickCreatePresentationError.TaskFailed),
        )

        assertEquals(QuickCreateTaskIndicator.Error, display.indicator)
        assertEquals(QuickCreateTaskStatusText.Error(QuickCreatePresentationError.TaskFailed), display.text)
    }

    @Test
    fun `running task status keeps progress indicator`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.RUNNING,
            statusText = QuickCreateTaskStatusText.Running(progressPercent = 42),
        )

        assertEquals(QuickCreateTaskIndicator.Progress, display.indicator)
        assertEquals(QuickCreateTaskStatusText.Running(progressPercent = 42), display.text)
    }

    @Test
    fun `submitting task status uses stable submitting text key`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.SUBMITTING,
            statusText = QuickCreateTaskStatusText.SubmittingTask,
        )

        assertEquals(QuickCreateTaskIndicator.Progress, display.indicator)
        assertEquals(QuickCreateTaskStatusText.SubmittingTask, display.text)
    }

    @Test
    fun `uploading media status keeps pending count as semantic value`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.SUBMITTING,
            statusText = QuickCreateTaskStatusText.UploadingMedia(pendingCount = 3),
        )

        assertEquals(QuickCreateTaskIndicator.Progress, display.indicator)
        assertEquals(QuickCreateTaskStatusText.UploadingMedia(pendingCount = 3), display.text)
    }

    @Test
    fun `cancelled task status uses stable cancelled text key`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.CANCELED,
            statusText = null,
        )

        assertEquals(QuickCreateTaskIndicator.Error, display.indicator)
        assertEquals(QuickCreateTaskStatusText.Canceled, display.text)
    }

    @Test
    fun `result actions expose stable next steps for task statuses`() {
        val runningActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.RUNNING,
            taskId = "task-1",
            results = emptyList(),
        )
        val successActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.SUCCESS,
            taskId = "task-2",
            results = listOf(
                QuickCreateResultUi(
                    url = "https://example.com/result.png",
                    type = "png",
                    mediaType = QuickCreateResultMediaType.IMAGE,
                )
            ),
        )
        val failedActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.FAILED,
            taskId = "task-3",
            results = emptyList(),
        )

        assertEquals(emptyList(), runningActions.map { it.action })
        assertEquals(
            listOf(
                QuickCreateResultAction.Download,
                QuickCreateResultAction.CopyToComposer,
            ),
            successActions.map { it.action },
        )
        assertEquals(
            listOf(
                QuickCreateResultAction.Retry,
                QuickCreateResultAction.ViewDetail,
            ),
            failedActions.map { it.action },
        )
        assertTrue(successActions.all { it.enabled })
    }

    @Test
    fun `success actions keep download only for video results`() {
        val successActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.SUCCESS,
            taskId = "task-4",
            results = listOf(
                QuickCreateResultUi(
                    url = "https://example.com/result.mp4",
                    type = "mp4",
                    mediaType = QuickCreateResultMediaType.VIDEO,
                )
            ),
        )

        assertEquals(
            listOf(QuickCreateResultAction.Download),
            successActions.map { it.action },
        )
    }

    @Test
    fun `success actions include copy to composer when first result is image`() {
        val successActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.SUCCESS,
            taskId = "task-5",
            results = listOf(
                QuickCreateResultUi(
                    url = "https://example.com/result.png",
                    type = "png",
                    mediaType = QuickCreateResultMediaType.IMAGE,
                ),
                QuickCreateResultUi(
                    url = "https://example.com/result.mp4",
                    type = "mp4",
                    mediaType = QuickCreateResultMediaType.VIDEO,
                ),
            ),
        )

        assertEquals(
            listOf(
                QuickCreateResultAction.Download,
                QuickCreateResultAction.CopyToComposer,
            ),
            successActions.map { it.action },
        )
    }

    @Test
    fun `success actions skip copy to composer when first result is video`() {
        val successActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.SUCCESS,
            taskId = "task-6",
            results = listOf(
                QuickCreateResultUi(
                    url = "https://example.com/result.mp4",
                    type = "mp4",
                    mediaType = QuickCreateResultMediaType.VIDEO,
                ),
                QuickCreateResultUi(
                    url = "https://example.com/result.png",
                    type = "png",
                    mediaType = QuickCreateResultMediaType.IMAGE,
                ),
            ),
        )

        assertEquals(
            listOf(QuickCreateResultAction.Download),
            successActions.map { it.action },
        )
    }

    @Test
    fun `queuing task exposes no result actions`() {
        val queuingActions = quickCreateResultActions(
            taskStatus = QuickCreateTaskUiStatus.QUEUING,
            taskId = "task-7",
            results = emptyList(),
        )

        assertEquals(emptyList(), queuingActions.map { it.action })
    }
}
