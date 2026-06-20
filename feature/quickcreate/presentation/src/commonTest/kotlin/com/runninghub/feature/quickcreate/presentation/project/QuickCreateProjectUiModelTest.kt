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
        assertEquals("3 个任务", item.taskCountText)
        assertEquals("取消置顶项目", item.pinContentDescription)
        assertEquals("已置顶", item.pinStatusText)
        assertEquals("删除「作品集」后，项目入口会从当前列表移除。", item.deleteConfirmationText)
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
                "任务数量" to "0",
                "置顶状态" to "未置顶",
                "创建时间" to "bad-time",
            ),
            detail.rows.map { it.label to it.value },
        )
    }
}
