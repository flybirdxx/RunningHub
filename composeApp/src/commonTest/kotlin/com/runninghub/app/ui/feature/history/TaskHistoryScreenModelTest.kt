package com.runninghub.app.ui.feature.history

import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryPage
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistorySource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setFilter filters loaded real history rows`() = runTest {
        val screenModel = TaskHistoryScreenModel(FakeGenerationHistoryRepository(), enablePolling = false)

        screenModel.loadHistory()
        screenModel.setFilter(TaskHistoryFilter.COMPLETED)

        assertEquals(listOf("success-task"), screenModel.uiState.value.items.map { it.taskId })
    }

    @Test
    fun `selectOutput loads history detail and exposes reusable params`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val screenModel = TaskHistoryScreenModel(repository, enablePolling = false)

        screenModel.loadHistory()
        screenModel.selectOutput("output-1")
        screenModel.prepareReuseParams("success-task")

        val state = screenModel.uiState.value
        assertEquals("output-1", repository.lastDetailOutputId)
        assertEquals("success-task", state.selectedDetail?.taskId)
        assertEquals("city", state.reuseParams["prompt"])
    }



    @Test
    fun `selectOutput exposes selected output and visible action message`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val screenModel = TaskHistoryScreenModel(repository, enablePolling = false)

        screenModel.loadHistory()
        screenModel.selectOutput("output-1")

        val state = screenModel.uiState.value
        assertEquals("output-1", state.selectedOutput?.outputId)
        assertEquals("https://example.com/image.png", state.selectedOutput?.url)
        assertEquals("\u5df2\u52a0\u8f7d\u8f93\u51fa\u8be6\u60c5", state.actionMessage)
    }

    @Test
    fun `prepareReuseParams exposes visible reusable parameter count`() = runTest {
        val screenModel = TaskHistoryScreenModel(FakeGenerationHistoryRepository(), enablePolling = false)

        screenModel.loadHistory()
        screenModel.prepareReuseParams("success-task")

        val state = screenModel.uiState.value
        assertEquals(mapOf("prompt" to "city"), state.reuseParams)
        assertEquals("\u5df2\u51c6\u5907 1 \u4e2a\u53ef\u590d\u7528\u53c2\u6570", state.actionMessage)
    }

    @Test
    fun `retryTask prepares failed task params without submitting`() = runTest {
        val screenModel = TaskHistoryScreenModel(FakeGenerationHistoryRepository(), enablePolling = false)

        screenModel.loadHistory()
        screenModel.retryTask("failed-task")

        val state = screenModel.uiState.value
        assertEquals(mapOf("prompt" to "retry city"), state.reuseParams)
        assertEquals("\u5df2\u51c6\u5907\u91cd\u8bd5\u53c2\u6570\uff0c\u8bf7\u5728\u521b\u5efa\u9875\u786e\u8ba4\u540e\u91cd\u65b0\u751f\u6210", state.actionMessage)
    }

    @Test
    fun `cancelTask delegates running task cancellation and refreshes history`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val screenModel = TaskHistoryScreenModel(repository, enablePolling = false)

        screenModel.loadHistory()
        screenModel.cancelTask("running-task")

        assertEquals("running-task", repository.cancelledTaskId)
        assertEquals("\u5df2\u8bf7\u6c42\u53d6\u6d88\u4efb\u52a1", screenModel.uiState.value.actionMessage)
        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `onDispose cancels active polling before next refresh`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val repository = FakeGenerationHistoryRepository()
        val screenModel = TaskHistoryScreenModel(repository, enablePolling = true)

        screenModel.loadHistory()
        runCurrent()
        screenModel.onDispose()
        advanceTimeBy(10_000)
        runCurrent()

        // 页面离开后轮询 Job 必须取消，否则不可见 History Tab 会继续拉取历史列表。
        assertEquals(1, repository.listCalls)
    }

    private class FakeGenerationHistoryRepository : GenerationHistoryRepository {
        var lastDetailOutputId: String? = null
        var cancelledTaskId: String? = null
        var listCalls = 0

        private val successItem = GenerationHistoryItem(
            taskId = "success-task",
            source = GenerationHistorySource.QUICK_CREATION,
            status = "SUCCESS",
            taskType = "Image",
            params = mapOf("prompt" to "city"),
            outputs = listOf(
                GenerationHistoryOutput(
                    outputId = "output-1",
                    url = "https://example.com/image.png",
                    type = "png",
                )
            ),
        )

        private val runningItem = GenerationHistoryItem(
            taskId = "running-task",
            source = GenerationHistorySource.QUICK_CREATION,
            status = "RUNNING",
            taskType = "Video",
        )

        private val failedItem = GenerationHistoryItem(
            taskId = "failed-task",
            source = GenerationHistorySource.STANDARD_MODEL,
            status = "FAILED",
            taskType = "Image",
            params = mapOf("prompt" to "retry city"),
        )

        override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> {
            listCalls += 1
            return Result.success(
                GenerationHistoryPage(
                    page = page,
                    size = size,
                    total = 3,
                    items = listOf(successItem, runningItem, failedItem),
                )
            )
        }

        override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> {
            lastDetailOutputId = outputId
            return Result.success(successItem)
        }

        override suspend fun cancelTask(taskId: String): Result<Unit> {
            cancelledTaskId = taskId
            return Result.success(Unit)
        }
    }
}

