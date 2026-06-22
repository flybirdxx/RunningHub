package com.runninghub.feature.quickcreate.presentation.result

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateTaskStatusUiTest {
    @Test
    fun `failed task status uses error indicator`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.FAILED,
            statusText = QuickCreateTaskStatusText.Custom("render failed"),
        )

        assertEquals(QuickCreateTaskIndicator.Error, display.indicator)
        assertEquals(QuickCreateTaskStatusText.Custom("render failed"), display.text)
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
}
