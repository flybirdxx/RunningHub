package com.runninghub.feature.task.presentation

import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryPage
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import com.runninghub.feature.task.domain.GenerationHistorySource
import com.runninghub.feature.task.domain.GenerationTaskDetail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `history entries keep server cost and actual output count`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()

        val entries = stateHolder.uiState.value.items.associateBy { it.taskId }
        assertEquals(1.236, entries.getValue("success-task").costAmount)
        assertEquals("CNY", entries.getValue("success-task").costCurrency)
        assertEquals(1, entries.getValue("success-task").outputCount)
        assertEquals(0.0, entries.getValue("running-task").costAmount)
        assertEquals(null, entries.getValue("running-task").costCurrency)
        assertEquals(0, entries.getValue("running-task").outputCount)
        assertEquals(0.42, entries.getValue("failed-task").costAmount)
        assertEquals("USD", entries.getValue("failed-task").costCurrency)
        assertEquals(0, entries.getValue("failed-task").outputCount)
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
        assertEquals(TaskHistoryActionMessage.OutputDetailLoaded, state.actionMessage)
    }

    @Test
    fun `openTaskDetail loads console task detail and close clears drawer`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.openTaskDetail("console-task-1")
        runCurrent()

        assertEquals("console-task-1", repository.lastTaskDetailId)
        assertEquals("console-task-1", stateHolder.uiState.value.selectedTaskDetail?.taskId)
        assertEquals("https://example.com/detail.png", stateHolder.uiState.value.selectedTaskDetail?.outputs?.single()?.url)

        stateHolder.closeTaskDetail()

        assertEquals(null, stateHolder.uiState.value.selectedTaskDetail)
        assertEquals(false, stateHolder.uiState.value.isTaskDetailLoading)
    }

    @Test
    fun `prepareReuseParams exposes visible reusable parameter count`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.prepareReuseParams("success-task")

        val state = stateHolder.uiState.value
        assertEquals(mapOf("prompt" to "city"), state.reuseParams)
        assertEquals(TaskHistoryActionMessage.ReusableParamsPrepared(1), state.actionMessage)
    }

    @Test
    fun `prepareReuseParams exposes stable empty reusable parameter action`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.prepareReuseParams("running-task")

        assertEquals(TaskHistoryActionMessage.NoReusableParams, stateHolder.uiState.value.actionMessage)
    }

    @Test
    fun `retryTask prepares failed task params without submitting`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.retryTask("failed-task")

        val state = stateHolder.uiState.value
        assertEquals(mapOf("prompt" to "retry city"), state.reuseParams)
        assertEquals(TaskHistoryActionMessage.RetryParamsPrepared, state.actionMessage)
    }

    @Test
    fun `retryTask exposes stable empty retry parameter action`() = runTest {
        val stateHolder = TaskHistoryStateHolder(FakeGenerationHistoryRepository(), this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.retryTask("failed-empty-task")

        assertEquals(TaskHistoryActionMessage.NoRetryParams, stateHolder.uiState.value.actionMessage)
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
        assertEquals(TaskHistoryActionMessage.CancelRequested, stateHolder.uiState.value.actionMessage)
        assertEquals(2, repository.listCalls)
    }

    @Test
    fun `task history invalidation refreshes immediately and schedules follow up refresh`() = runTest {
        val repository = FakeGenerationHistoryRepository()
        val invalidations = MutableSharedFlow<Unit>()
        val stateHolder = TaskHistoryStateHolder(
            generationHistoryRepository = repository,
            coroutineScope = this,
            historyInvalidations = invalidations,
            enablePolling = false,
        )
        runCurrent()

        stateHolder.loadHistory()
        runCurrent()
        invalidations.emit(Unit)
        runCurrent()

        assertEquals(2, repository.listCalls)

        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(3, repository.listCalls)
        stateHolder.dispose()
    }

    @Test
    fun `history load failure does not expose repository exception message`() = runTest {
        val repository = FakeGenerationHistoryRepository().apply {
            listFailureMessage = "HISTORY_CODE_500 msg=database shard internal"
        }
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()

        assertEquals(TaskHistoryPresentationError.HistoryLoadFailed, stateHolder.uiState.value.error)
    }

    @Test
    fun `history auth failure exposes stable auth error without repository message`() = runTest {
        val repository = FakeGenerationHistoryRepository().apply {
            listFailureMessage = "HTTP 401 TOKEN expired internal detail"
        }
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()

        assertEquals(TaskHistoryPresentationError.AuthRequired, stateHolder.uiState.value.error)
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

        assertEquals(TaskHistoryPresentationError.CancelFailed, stateHolder.uiState.value.error)
    }

    @Test
    fun `detail failure exposes stable detail error without repository message`() = runTest {
        val repository = FakeGenerationHistoryRepository().apply {
            detailFailureMessage = "DETAIL_CODE_500 msg=remote stack"
        }
        val stateHolder = TaskHistoryStateHolder(repository, this, enablePolling = false)

        stateHolder.loadHistory()
        runCurrent()
        stateHolder.selectOutput("output-1")
        runCurrent()

        assertEquals(TaskHistoryPresentationError.DetailLoadFailed, stateHolder.uiState.value.error)
        assertIs<TaskHistoryPresentationError>(stateHolder.uiState.value.error)
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
        var lastTaskDetailId: String? = null
        var cancelledTaskId: String? = null
        var listCalls = 0
        var listFailureMessage: String? = null
        var detailFailureMessage: String? = null
        var cancelFailureMessage: String? = null

        private val successItem = GenerationHistoryItem(
            taskId = "success-task",
            source = GenerationHistorySource.QUICK_CREATION,
            status = "SUCCESS",
            taskType = "\u89d2\u8272\u8bbe\u5b9a",
            costAmount = 1.236,
            costCurrency = "CNY",
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
            taskType = "\u751f\u6210\u89c6\u9891",
        )

        private val failedItem = GenerationHistoryItem(
            taskId = "failed-task",
            source = GenerationHistorySource.STANDARD_MODEL,
            status = "FAILED",
            taskType = "Image",
            costAmount = 0.42,
            costCurrency = "USD",
            params = mapOf("prompt" to "retry city"),
        )

        private val failedEmptyItem = GenerationHistoryItem(
            taskId = "failed-empty-task",
            source = GenerationHistorySource.STANDARD_MODEL,
            status = "FAILED",
            taskType = "Image",
            params = emptyMap(),
        )

        override suspend fun listHistory(page: Int, size: Int): Result<GenerationHistoryPage> {
            listCalls += 1
            listFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(
                GenerationHistoryPage(
                    page = page,
                    size = size,
                    total = 4,
                    items = listOf(successItem, runningItem, failedItem, failedEmptyItem),
                )
            )
        }

        override suspend fun getHistoryDetail(outputId: String): Result<GenerationHistoryItem> {
            lastDetailOutputId = outputId
            detailFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(successItem)
        }

        override suspend fun getTaskDetail(taskId: String): Result<GenerationTaskDetail> {
            lastTaskDetailId = taskId
            return Result.success(
                GenerationTaskDetail(
                    taskId = taskId,
                    title = "Console detail",
                    status = "SUCCESS",
                    outputs = listOf(
                        GenerationHistoryOutput(
                            outputId = "detail-output-1",
                            url = "https://example.com/detail.png",
                            type = "png",
                        )
                    ),
                )
            )
        }

        override suspend fun cancelTask(taskId: String): Result<Unit> {
            cancelledTaskId = taskId
            cancelFailureMessage?.let { return Result.failure(IllegalStateException(it)) }
            return Result.success(Unit)
        }
    }
}
