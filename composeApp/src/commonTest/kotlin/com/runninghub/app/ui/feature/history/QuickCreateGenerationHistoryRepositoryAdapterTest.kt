package com.runninghub.app.ui.feature.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreateGenerationHistoryRepositoryAdapterTest {
    @Test
    fun `maps quick creation item to unified history item`() {
        val item = QuickCreationHistoryItem(
            taskId = "task-1",
            status = "SUCCESS",
            skuId = "sku-1",
            taskType = "image",
            cashAmount = 0.16,
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "out-1",
                    url = "https://example.com/a.png",
                    type = "png",
                    width = 1024,
                    height = 1024,
                )
            )
        ).toGenerationHistoryItem()

        assertEquals("task-1", item.taskId)
        assertEquals("quick_creation", item.source.key)
        assertEquals("SUCCESS", item.status)
        assertEquals("sku-1", item.modelId)
        assertEquals("image", item.taskType)
        assertEquals(0.16, item.costAmount)
        assertEquals("https://example.com/a.png", item.outputs.single().url)
        assertEquals(true, item.outputs.single().isImage)
    }

    @Test
    fun `delegates history operations to quick creation repository`() = runTest {
        val repository = FakeQuickCreationTaskHistoryRepository()
        val adapter = QuickCreateGenerationHistoryRepositoryAdapter(repository)

        val page = adapter.listHistory(page = 2, size = 30).getOrThrow()
        val detail = adapter.getHistoryDetail("out-1").getOrThrow()
        adapter.cancelTask("task-1")

        assertEquals(2, repository.lastPage)
        assertEquals(30, repository.lastSize)
        assertEquals("out-1", repository.lastDetailOutputId)
        assertEquals("task-1", repository.cancelledTaskId)
        assertEquals("task-1", page.items.single().taskId)
        assertEquals("task-1", detail.taskId)
    }

    private class FakeQuickCreationTaskHistoryRepository : QuickCreationTaskHistoryRepository {
        var lastPage: Int? = null
        var lastSize: Int? = null
        var lastDetailOutputId: String? = null
        var cancelledTaskId: String? = null

        private val item = QuickCreationHistoryItem(
            taskId = "task-1",
            status = "RUNNING",
            skuId = "sku-1",
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "out-1",
                    url = "https://example.com/a.png",
                    type = "png",
                )
            ),
        )

        override suspend fun listQuickCreationHistory(page: Int, size: Int): Result<QuickCreationHistoryPage> {
            lastPage = page
            lastSize = size
            return Result.success(
                QuickCreationHistoryPage(
                    page = page,
                    size = size,
                    total = 1,
                    items = listOf(item),
                )
            )
        }

        override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> {
            lastDetailOutputId = outputId
            return Result.success(item)
        }

        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> {
            cancelledTaskId = taskId
            return Result.success(Unit)
        }

        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> =
            error("通用历史兼容桥不读取项目任务列表。")
    }
}
