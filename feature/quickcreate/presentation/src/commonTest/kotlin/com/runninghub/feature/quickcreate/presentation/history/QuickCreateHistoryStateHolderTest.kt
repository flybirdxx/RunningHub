package com.runninghub.feature.quickcreate.presentation.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class QuickCreateHistoryStateHolderTest {

    @Test
    fun `load recent history writes first page and starts from recent source`() = runTest {
        val repository = FakeHistoryRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadRecentHistory()
        runCurrent()

        assertEquals(listOf(1 to 2), repository.requestedHistoryPages)
        assertFalse(state.value.historyLoading)
        assertEquals(1, state.value.historyPage)
        assertEquals(3, state.value.historyTotal)
        assertEquals(true, state.value.historyHasMore)
        assertEquals(listOf("recent-task-1", "recent-task-2"), state.value.historyItems.map { it.taskId })
        assertEquals("recent prompt 1", state.value.historyItems.first().title)
    }

    @Test
    fun `load more history appends next page and deduplicates by task id`() = runTest {
        val repository = FakeHistoryRepository().apply {
            historyPages = mapOf(
                1 to historyPage(
                    page = 1,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("recent-task-1"), historyItem("recent-task-2")),
                ),
                2 to historyPage(
                    page = 2,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("recent-task-2"), historyItem("recent-task-3")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadRecentHistory()
        runCurrent()
        holder.loadMoreHistory()
        runCurrent()

        assertEquals(listOf(1 to 2, 2 to 2), repository.requestedHistoryPages)
        assertEquals(listOf("recent-task-1", "recent-task-2", "recent-task-3"), state.value.historyItems.map { it.taskId })
        assertEquals(2, state.value.historyPage)
        assertFalse(state.value.historyHasMore)
        assertFalse(state.value.historyLoadingMore)
    }

    @Test
    fun `load more history enters loading synchronously and ignores duplicate trigger`() = runTest {
        val repository = FakeHistoryRepository().apply {
            historyPages = mapOf(
                1 to historyPage(
                    page = 1,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("recent-task-1"), historyItem("recent-task-2")),
                ),
                2 to historyPage(
                    page = 2,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("recent-task-3")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadRecentHistory()
        runCurrent()
        holder.loadMoreHistory()
        holder.loadMoreHistory()

        assertEquals(listOf(1 to 2), repository.requestedHistoryPages)
        assertEquals(true, state.value.historyLoadingMore)

        runCurrent()

        assertEquals(listOf(1 to 2, 2 to 2), repository.requestedHistoryPages)
        assertEquals(listOf("recent-task-1", "recent-task-2", "recent-task-3"), state.value.historyItems.map { it.taskId })
        assertFalse(state.value.historyLoadingMore)
    }

    @Test
    fun `select project loads project task source and load more keeps project source`() = runTest {
        val repository = FakeHistoryRepository().apply {
            projectTaskPages = mapOf(
                ("project-1" to 1) to historyPage(
                    page = 1,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("project-task-1"), historyItem("project-task-2")),
                ),
                ("project-1" to 2) to historyPage(
                    page = 2,
                    size = 2,
                    total = 3,
                    items = listOf(historyItem("project-task-3")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.selectProject("project-1")
        runCurrent()
        holder.loadMoreHistory()
        runCurrent()

        assertEquals(emptyList(), repository.requestedHistoryPages)
        assertEquals(listOf("project-1" to 1, "project-1" to 2), repository.requestedProjectTaskPages)
        assertEquals("project-1", state.value.selectedProjectId)
        assertEquals(listOf("project-task-1", "project-task-2", "project-task-3"), state.value.historyItems.map { it.taskId })
        assertFalse(state.value.projectTasksLoading)
    }

    @Test
    fun `switching to project ignores delayed recent history result`() = runTest {
        val recentGate = CompletableDeferred<Unit>()
        val repository = FakeHistoryRepository().apply {
            historyPageGate = recentGate
            projectTaskPages = mapOf(
                ("project-1" to 1) to historyPage(
                    items = listOf(historyItem("project-task-1", prompt = "project prompt")),
                ),
            )
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadRecentHistory()
        runCurrent()
        holder.selectProject("project-1")
        runCurrent()
        recentGate.complete(Unit)
        runCurrent()

        assertEquals("project-1", state.value.selectedProjectId)
        assertEquals(listOf("project-task-1"), state.value.historyItems.map { it.taskId })
        assertEquals(listOf(1 to 2), repository.requestedHistoryPages)
        assertEquals(listOf("project-1" to 1), repository.requestedProjectTaskPages)
    }

    @Test
    fun `cancel active recent task refreshes current recent history source`() = runTest {
        val repository = FakeHistoryRepository().apply {
            historyPages = mapOf(
                1 to historyPage(
                    items = listOf(historyItem("active-task", status = "RUNNING")),
                )
            )
            refreshHistoryPage = historyPage(items = listOf(historyItem("active-task", status = "CANCELED")))
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.loadRecentHistory()
        runCurrent()
        holder.cancelHistoryTask("active-task")
        runCurrent()

        assertEquals(listOf("active-task"), repository.cancelledTaskIds)
        assertEquals(listOf(1 to 2, 1 to 2), repository.requestedHistoryPages)
        assertEquals("CANCELED", state.value.historyItems.single().source.status)
        assertEquals(emptySet(), state.value.historyCancellingTaskIds)
    }

    @Test
    fun `cancel active project task refreshes selected project task source`() = runTest {
        val repository = FakeHistoryRepository().apply {
            projectTaskPages = mapOf(
                ("project-1" to 1) to historyPage(items = listOf(historyItem("project-active-task", status = "RUNNING"))),
            )
            refreshProjectTaskPage = historyPage(items = listOf(historyItem("project-active-task", status = "CANCELED")))
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.selectProject("project-1")
        runCurrent()
        holder.cancelHistoryTask("project-active-task")
        runCurrent()

        assertEquals(listOf("project-active-task"), repository.cancelledTaskIds)
        assertEquals(listOf("project-1" to 1, "project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals(emptyList(), repository.requestedHistoryPages)
        assertEquals("CANCELED", state.value.historyItems.single().source.status)
    }

    @Test
    fun `polling refreshes non terminal task and stops after terminal result`() = runTest {
        val repository = FakeHistoryRepository().apply {
            historyPages = mapOf(
                1 to historyPage(items = listOf(historyItem("polling-task", status = "RUNNING"))),
            )
            refreshHistoryPage = historyPage(items = listOf(historyItem("polling-task", status = "SUCCESS")))
        }
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this, refreshIntervalMillis = 1_000L)

        holder.loadRecentHistory()
        runCurrent()
        advanceTimeBy(1_000L)
        runCurrent()
        advanceTimeBy(2_000L)
        runCurrent()

        assertEquals(listOf(1 to 2, 1 to 2), repository.requestedHistoryPages)
        assertEquals("SUCCESS", state.value.historyItems.single().source.status)
        assertFalse(state.value.historyItems.single().needsRefresh)
    }

    @Test
    fun `select history output loads detail into state`() = runTest {
        val repository = FakeHistoryRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.selectHistoryOutput("output-1")
        runCurrent()

        assertEquals("output-1", repository.lastHistoryDetailOutputId)
        assertFalse(state.value.historyDetailLoading)
        assertEquals("history-detail-task", state.value.selectedHistoryDetail?.source?.taskId)
        assertEquals("detail prompt", state.value.selectedHistoryDetail?.title)
        assertEquals("IMAGE · SUCCESS · PNG · 1024x1024", state.value.selectedHistoryDetail?.metadataText)
    }

    @Test
    fun `clear selected project resets project source and reloads recent history`() = runTest {
        val repository = FakeHistoryRepository()
        val state = MutableStateFlow(QuickCreateUiState())
        val holder = createHolder(repository, state, this)

        holder.selectProject("project-1")
        runCurrent()
        holder.clearSelectedProject()
        runCurrent()

        assertNull(state.value.selectedProjectId)
        assertEquals(listOf(1 to 2), repository.requestedHistoryPages)
        assertEquals(listOf("project-1" to 1), repository.requestedProjectTaskPages)
        assertEquals(listOf("recent-task-1", "recent-task-2"), state.value.historyItems.map { it.taskId })
    }

    private fun createHolder(
        repository: FakeHistoryRepository,
        state: MutableStateFlow<QuickCreateUiState>,
        scope: TestScope,
        pageSize: Int = 2,
        refreshIntervalMillis: Long = 5_000L,
    ): QuickCreateHistoryStateHolder =
        QuickCreateHistoryStateHolder(
            historyRepository = repository,
            scope = scope,
            uiState = state,
            pageSize = pageSize,
            refreshIntervalMillis = refreshIntervalMillis,
        )

    /**
     * 历史 StateHolder 的最小仓库替身。
     *
     * 测试只覆盖最近历史、项目任务、详情和取消任务，因此 fake 只实现历史仓库接口，
     * 不混入项目元数据、生成、上传或灵感模板能力，确保 feature presentation 测试保持单一职责。
     */
    private class FakeHistoryRepository : QuickCreationTaskHistoryRepository {
        var historyPages: Map<Int, QuickCreationHistoryPage>? = null
        var projectTaskPages: Map<Pair<String, Int>, QuickCreationHistoryPage>? = null
        var historyPageGate: CompletableDeferred<Unit>? = null
        var projectTaskPageGate: CompletableDeferred<Unit>? = null
        var refreshHistoryPage: QuickCreationHistoryPage? = null
        var refreshProjectTaskPage: QuickCreationHistoryPage? = null
        var historyDetail = historyItem(
            taskId = "history-detail-task",
            status = "SUCCESS",
            prompt = "detail prompt",
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "output-1",
                    url = "https://example.com/detail.png",
                    type = "png",
                    width = 1024,
                    height = 1024,
                )
            ),
        )
        val requestedHistoryPages = mutableListOf<Pair<Int, Int>>()
        val requestedProjectTaskPages = mutableListOf<Pair<String, Int>>()
        val cancelledTaskIds = mutableListOf<String>()
        var lastHistoryDetailOutputId: String? = null

        /**
         * 返回最近历史分页；第二次及之后第一页请求可用 [refreshHistoryPage] 模拟轮询或取消后的刷新。
         */
        override suspend fun listQuickCreationHistory(page: Int, size: Int): Result<QuickCreationHistoryPage> {
            requestedHistoryPages += page to size
            historyPageGate?.await()
            val isRefresh = page == 1 && requestedHistoryPages.count { it.first == 1 } > 1
            val configuredPage = historyPages?.get(page) ?: defaultHistoryPage(page = page, size = size)
            val pageResult = if (isRefresh) {
                refreshHistoryPage ?: configuredPage
            } else {
                configuredPage
            }
            return Result.success(pageResult.copy(page = page, size = size))
        }

        /**
         * 返回项目任务分页；第二次及之后第一页请求可用 [refreshProjectTaskPage] 模拟当前项目刷新。
         */
        override suspend fun listQuickCreationProjectTasks(
            projectId: String,
            page: Int,
            size: Int,
        ): Result<QuickCreationHistoryPage> {
            requestedProjectTaskPages += projectId to page
            projectTaskPageGate?.await()
            val isRefresh = page == 1 && requestedProjectTaskPages.count { it == (projectId to 1) } > 1
            val configuredPage = projectTaskPages?.get(projectId to page) ?: historyPage(
                page = page,
                size = size,
                total = 1,
                items = listOf(historyItem("project-task-1", prompt = "project prompt")),
            )
            val pageResult = if (isRefresh) refreshProjectTaskPage ?: configuredPage else configuredPage
            return Result.success(pageResult.copy(page = page, size = size))
        }

        /**
         * 返回历史输出详情，并记录 outputId 以验证 StateHolder 传参。
         */
        override suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem> {
            lastHistoryDetailOutputId = outputId
            return Result.success(historyDetail)
        }

        /**
         * 记录取消任务请求；刷新当前历史来源由 StateHolder 在成功后触发。
         */
        override suspend fun cancelQuickCreationTask(taskId: String): Result<Unit> {
            cancelledTaskIds += taskId
            return Result.success(Unit)
        }
    }
}

private fun defaultHistoryPage(page: Int, size: Int): QuickCreationHistoryPage =
    historyPage(
        page = page,
        size = size,
        total = 3,
        items = listOf(
            historyItem("recent-task-1", prompt = "recent prompt 1"),
            historyItem("recent-task-2", prompt = "recent prompt 2"),
        ),
    )

private fun historyPage(
    page: Int = 1,
    size: Int = 2,
    total: Int = 1,
    items: List<QuickCreationHistoryItem>,
): QuickCreationHistoryPage =
    QuickCreationHistoryPage(
        page = page,
        size = size,
        total = total,
        items = items,
    )

private fun historyItem(
    taskId: String,
    status: String = "SUCCESS",
    prompt: String = "$taskId prompt",
    outputs: List<QuickCreationHistoryOutput> = listOf(
        QuickCreationHistoryOutput(
            outputId = "$taskId-output",
            url = "https://example.com/$taskId.png",
            type = "png",
        )
    ),
): QuickCreationHistoryItem =
    QuickCreationHistoryItem(
        taskId = taskId,
        status = status,
        categoryId = "IMAGE",
        params = mapOf("prompt" to prompt),
        outputs = outputs,
    )
