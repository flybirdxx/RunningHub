package com.runninghub.feature.quickcreate.presentation.project

import com.runninghub.feature.quickcreate.domain.QuickCreationProject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuickCreateProjectUiModelTest {

    @Test
    fun `project mapper derives list display fields`() {
        val project = QuickCreationProject(
            projectId = "project_1",
            name = "作品集",
            coverUrl = "  ",
            taskCount = 3,
            pinned = true,
        )

        val item = project.toQuickCreateProjectUiItem()

        assertEquals("project_1", item.projectId)
        assertEquals("作品集", item.name)
        assertNull(item.coverUrl)
        assertEquals(QuickCreateProjectTaskCountText(3), item.taskCountText)
        assertEquals(QuickCreateProjectPinContentDescription.UnpinProject, item.pinContentDescription)
        assertEquals(QuickCreateProjectPinStatusText.Pinned, item.pinStatusText)
        assertEquals(QuickCreateProjectDeleteConfirmationText("作品集"), item.deleteConfirmationText)
    }

    @Test
    fun `project detail mapper keeps invalid server time visible`() {
        val project = QuickCreationProject(
            projectId = "project_2",
            name = "视频项目",
            coverUrl = "https://example.com/cover.png",
            taskCount = 0,
            pinned = false,
            createdAt = "bad-time",
            updatedAt = null,
        )

        val detail = project.toQuickCreateProjectDetailUiItem()

        assertEquals("https://example.com/cover.png", detail.coverUrl)
        assertEquals(
            listOf(
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.TaskCount,
                    value = QuickCreateProjectDetailRowValue.TaskCount(0),
                ),
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.PinStatus,
                    value = QuickCreateProjectDetailRowValue.PinStatus(QuickCreateProjectPinStatusText.NotPinned),
                ),
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.CreatedAt,
                    value = QuickCreateProjectDetailRowValue.Timestamp("bad-time"),
                ),
            ),
            detail.rows,
        )
    }
}
