package com.runninghub.feature.task.presentation

import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryPage
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistorySource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryStateHolderTest {
    @Test
    fun `setFilter filters loaded real history rows`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.setFilter(TaskHistoryFilter.COMPLETED)

        assertEquals(listOf("success-task"), stateHolder.uiState.value.items.map { it.taskId })
    }

    @Test
    fun `selectOutput loads history detail and exposes reusable params`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.selectOutput("output-1")
        runCurrent()
        stateHolder.prepareReuseParams("success-task")

        val state = stateHolder.uiState.value
        assertEquals("output-1", repository.lastDetailOutputId)
        assertEquals("success-task", state.selectedDetail?.taskId)
        assertEquals("city", state.reuseParams["prompt"])
    }

    @Test
    fun `selectOutput exposes selected output and visible action message`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.selectOutput("output-1")
        runCurrent()

        val state = stateHolder.uiState.value
        assertEquals("output-1", state.selectedOutput?.outputId)
        assertEquals("https://example.com/image.png", state.selectedOutput?.url)
        assertEquals("已加载输出详情", state.actionMessage)
    }

    @Test
    fun `prepareReuseParams exposes visible reusable parameter count`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.prepareReuseParams("success-task")

        val state = stateHolder.uiState.value
        assertEquals(mapOf("prompt" to "city"), state.reuseParams)
        assertEquals("已准备 1 个可复用参数", state.actionMessage)
    }

    @Test
    fun `retryTask prepares failed task params without submitting`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.retryTask("failed-task")

        val state = stateHolder.uiState.value
        assertEquals(mapOf("prompt" to "retry city"), state.reuseParams)
        assertEquals("已准备重试参数，请在创建页确认后重新生成", state.actionMessage)
    }

    @Test
    fun `cancelTask delegates running task cancellation and refreshes history`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.cancelTask("running-task")
        runCurrent()

        assertEquals("running-task", repository.cancelledTaskId)
        assertEquals("已请求取消任务", stateHolder.uiState.value.actionMessage)
        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `history load failure does not expose repository exception message`() = runTest {
        val repository = FakeGenerationHistoryRepository().apply {
            listFailureMessage = "HISTORY_CODE_500 msg=database shard internal"
        }
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()

        assertEquals("历史记录加载失败，请稍后重试", stateHolder.uiState.value.error)
    }

    @Test
    fun `cancel failure does not expose repository exception message`() = runTest {
        val repository = FakeGenerationHistoryRepository().apply {
            cancelFailureMessage = "CANCEL_CODE_500 msg=remote detail"
        }
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.cancelTask("running-task")
        runCurrent()

        assertEquals("取消失败，请稍后重试", stateHolder.uiState.value.error)
    }

    @Test
    fun `dispose cancels active polling before next refresh`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = true)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.dispose()
        advanceTimeBy(10_000)
        runCurrent()

        // 页面离开后轮询 Job 必须取消，否则不可见 History Tab 会继续拉取历史列表。
        assertEquals(1, repository.listCalls)
    }

    private class FakeGenerationHistoryRepository : GenerationHistoryRepository {
        var lastDetailOutputId: String? = null
        var cancelledTaskId: String? = null
        var listCalls = 0
        var listFailureMessage: String? = null
        var cancelFailureMessage: String? = null

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
            listFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
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
            cancelFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(Unit)
        }
    }
}
