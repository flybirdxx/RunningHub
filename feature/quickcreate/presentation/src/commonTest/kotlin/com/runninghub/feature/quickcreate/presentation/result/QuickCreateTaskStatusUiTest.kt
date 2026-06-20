package com.runninghub.feature.quickcreate.presentation.result

import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateTaskStatusUiTest {
    @Test
    fun `failed task status uses error indicator`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.FAILED,
            statusText = "render failed",
        )

        assertEquals(QuickCreateTaskIndicator.Error, display.indicator)
        assertEquals("render failed", display.text)
    }

    @Test
    fun `running task status keeps progress indicator`() {
        val display = quickCreateTaskStatusDisplay(
            status = QuickCreateTaskPresentationStatus.RUNNING,
            statusText = "running 42%",
        )

        assertEquals(QuickCreateTaskIndicator.Progress, display.indicator)
        assertEquals("running 42%", display.text)
    }
}
