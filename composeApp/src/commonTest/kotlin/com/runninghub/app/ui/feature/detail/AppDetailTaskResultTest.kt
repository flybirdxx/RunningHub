package com.runninghub.app.ui.feature.detail

import com.runninghub.core.model.TaskOutput
import kotlin.test.Test
import kotlin.test.assertTrue

class AppDetailTaskResultTest {
    @Test
    fun `task output video detection accepts signed video urls`() {
        val output = TaskOutput(
            fileUrl = "https://example.com/result.MOV?token=abc",
            fileName = "result.mov",
            fileType = "file",
            failedReason = null,
        )

        assertTrue(output.isTaskOutputVideo())
    }
}
