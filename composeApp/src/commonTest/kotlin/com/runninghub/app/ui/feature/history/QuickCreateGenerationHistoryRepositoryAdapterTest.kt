package com.runninghub.app.ui.feature.history

import com.runninghub.core.model.TaskExecutionStatus
import com.runninghub.core.model.TaskHistoryItem
import com.runninghub.core.model.TaskHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationTaskDetail
import com.runninghub.feature.task.domain.GenerationHistorySource
import com.runninghub.feature.task.domain.WebAppTaskHistoryRepository
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

    @Test
    fun `unified history repository merges quick creation and webapp history`() = runTest {
        val quickRepository = FakeQuickCreationTaskHistoryRepository()
        val webAppRepository = FakeWebAppTaskHistoryRepository()
        val repository = UnifiedGenerationHistoryRepository(quickRepository, webAppRepository)

        val page = repository.listHistory(page = 1, size = 20).getOrThrow()

        assertEquals(listOf("web-task-1", "task-1"), page.items.map { it.taskId })
        assertEquals("webapp", page.items[0].source.key)
        assertEquals("quick_creation", page.items[1].source.key)
        assertEquals("SUCCESS", page.items[0].status)
        assertEquals("WebApp job", page.items[0].taskType)
        assertEquals("00:42", page.items[0].costTime)
        assertEquals("https://example.com/web-preview.png", page.items[0].outputs.single().thumbnailUrl)
    }

    @Test
    fun `unified history includes running ai app task from billing usage`() = runTest {
        val quickRepository = FakeQuickCreationTaskHistoryRepository()
        val webAppRepository = FakeWebAppTaskHistoryRepository(
            items = listOf(
                TaskHistoryItem(
                    taskId = "2071208241819508737",
                    outputs = emptyList(),
                    status = TaskExecutionStatus.Running,
                    taskCostTime = "00:01",
                    createTime = "2026-06-28 20:25:01",
                    taskName = "全能图片G-2.0-文生图-低价渠道版",
                    webappId = "2046794551444119554",
                    taskCategoryCode = "WEBAPP_API",
                    taskCategoryDisplay = "AI应用API",
                    taskRelation = "PARENT",
                    coinAmount = 12.0,
                )
            )
        )
        val repository = UnifiedGenerationHistoryRepository(quickRepository, webAppRepository)

        val page = repository.listHistory(page = 1, size = 20).getOrThrow()
        val aiAppTask = page.items.first()

        assertEquals("2071208241819508737", aiAppTask.taskId)
        assertEquals("webapp", aiAppTask.source.key)
        assertEquals("RUNNING", aiAppTask.status)
        assertEquals("全能图片G-2.0-文生图-低价渠道版", aiAppTask.taskType)
        assertEquals(12.0, aiAppTask.costAmount)
        assertEquals("RHB", aiAppTask.costCurrency)
    }

    @Test
    fun `unified history includes locally submitted webapp task before output history syncs`() = runTest {
        val quickRepository = FakeQuickCreationTaskHistoryRepository()
        val webAppRepository = FakeWebAppTaskHistoryRepository(items = emptyList())
        val overlayStore = WebAppTaskHistoryOverlayStore()
        overlayStore.upsert(
            GenerationHistoryItem(
                taskId = "2071208241819508737",
                source = GenerationHistorySource.WEBAPP,
                status = "RUNNING",
                modelId = "webapp-123",
                taskType = "全能图片G-2.0-文生图-低价渠道版",
            )
        )
        val repository = UnifiedGenerationHistoryRepository(quickRepository, webAppRepository, overlayStore)

        val page = repository.listHistory(page = 1, size = 20).getOrThrow()

        assertEquals(listOf("2071208241819508737", "task-1"), page.items.map { it.taskId })
        assertEquals("RUNNING", page.items[0].status)
        assertEquals("全能图片G-2.0-文生图-低价渠道版", page.items[0].taskType)
    }

    @Test
    fun `unified history delegates console task detail to webapp repository`() = runTest {
        val quickRepository = FakeQuickCreationTaskHistoryRepository()
        val webAppRepository = FakeWebAppTaskHistoryRepository()
        val repository = UnifiedGenerationHistoryRepository(quickRepository, webAppRepository)

        val detail = repository.getTaskDetail("web-task-1").getOrThrow()

        assertEquals("web-task-1", webAppRepository.lastDetailTaskId)
        assertEquals("Web detail", detail.title)
        assertEquals("https://example.com/web-detail.png", detail.outputs.single().url)
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

    private class FakeWebAppTaskHistoryRepository(
        private val items: List<TaskHistoryItem> = listOf(
            TaskHistoryItem(
                taskId = "web-task-1",
                outputs = listOf(
                    TaskHistoryOutput(
                        id = "web-output-1",
                        outputName = "web-output.png",
                        outputType = "png",
                        fileUrl = "https://example.com/web.png",
                        filePreviewUrl = "https://example.com/web-preview.png",
                        outputSize = null,
                        expireDays = "7",
                    )
                ),
                status = TaskExecutionStatus.Success,
                taskCostTime = "00:42",
                createTime = "2026-06-28 13:58:00",
                taskName = "WebApp job",
                webappId = "webapp-1",
            )
        ),
    ) : WebAppTaskHistoryRepository {
        var lastDetailTaskId: String? = null

        override suspend fun getTaskHistory(pageNum: Int, pageSize: Int): Result<List<TaskHistoryItem>> =
            Result.success(items)

        override suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail> {
            lastDetailTaskId = taskId
            return Result.success(
                GenerationTaskDetail(
                    taskId = taskId,
                    title = "Web detail",
                    status = "SUCCESS",
                    outputs = listOf(
                        com.runninghub.feature.task.domain.GenerationHistoryOutput(
                            outputId = "web-detail-output",
                            url = "https://example.com/web-detail.png",
                            type = "png",
                        )
                    ),
                )
            )
        }
    }
}
