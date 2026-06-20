package com.runninghub.feature.quickcreate.presentation.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryPage
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HISTORY_REFRESH_INTERVAL_MS = 5_000L

/**
 * 管理快捷创作页面的历史记录区域。
 *
 * 该类位于 Presentation 层，负责最近历史、选中项目任务列表、历史详情、取消任务和轮询刷新。
 * 它只依赖 Domain Repository 接口与页面状态容器，不直接触碰网络、存储或平台能力。ScreenModel
 * 仍保留生成流程、项目管理与历史区域之间的顶层协调，只通过本类的公开方法刷新当前历史来源。
 * 本类不依赖 composeApp、Composable 或平台 API，可在 feature presentation 模块内独立验证分页和轮询规则。
 *
 * @param historyRepository 快捷创作历史仓库接口，提供最近历史、项目任务、详情和取消任务能力。
 * @param scope ScreenModel 生命周期作用域，历史加载和轮询任务随页面释放而取消。
 * @param uiState 页面状态容器，历史区域通过不可变 `copy` 更新状态。
 * @param pageSize 历史分页大小，单位为条；测试可替换它以覆盖分页边界。
 * @param refreshIntervalMillis 非终态任务轮询间隔，单位为毫秒；测试使用虚拟时间推进该间隔。
 */
class QuickCreateHistoryStateHolder(
    private val historyRepository: QuickCreationTaskHistoryRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val pageSize: Int = 10,
    private val refreshIntervalMillis: Long = HISTORY_REFRESH_INTERVAL_MS,
) {
    private var historyLoadJob: Job? = null
    private var historyRefreshJob: Job? = null
    private var historySourceRevision = 0L

    /**
     * 加载最近历史第一页。
     *
     * 页面初始化、退出项目筛选或删除当前项目后调用。失败时只关闭加载态，不清空已有历史，
     * 避免短暂网络错误导致用户正在查看的结果列表被替换为空白。
     */
    fun loadRecentHistory() {
        cancelRefreshJob()
        cancelHistoryLoadJob()
        val requestRevision = nextHistorySourceRevision()
        historyLoadJob = scope.launch {
            uiState.update {
                it.copy(
                    historyLoading = true,
                    historyLoadingMore = false,
                )
            }
            val history = historyRepository.listQuickCreationHistory(page = 1, size = pageSize)
            if (!isCurrentHistorySource(requestRevision, selectedProjectId = null)) return@launch
            history.fold(
                onSuccess = { page ->
                    val historyItems = page.items.map { it.toQuickCreateHistoryUiItem() }
                    uiState.update { state ->
                        state.copy(
                            historyLoading = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = page.items.size < page.total,
                            historyItems = historyItems,
                        )
                    }
                    updateHistoryRefreshJob(historyItems)
                },
                onFailure = {
                    uiState.update { state -> state.copy(historyLoading = false) }
                },
            )
        }
    }

    /**
     * 加载当前历史来源的下一页。
     *
     * 当前未选中项目时读取最近历史；选中项目时读取该项目任务。分页合并时按 `taskId`
     * 去重，避免服务端分页边界变化或重复点击加载更多时出现重复任务卡片。
     */
    fun loadMoreHistory() {
        val state = uiState.value
        if (state.historyLoading || state.historyLoadingMore || !state.historyHasMore) return

        val requestRevision = historySourceRevision
        val selectedProjectId = state.selectedProjectId
        val nextPage = state.historyPage + 1
        // 分页按钮可能在同一事件帧内被连续触发，必须在启动协程前同步占用加载状态。
        uiState.update { it.copy(historyLoadingMore = true) }
        scope.launch {
            val history = if (selectedProjectId.isNullOrBlank()) {
                historyRepository.listQuickCreationHistory(page = nextPage, size = pageSize)
            } else {
                historyRepository.listQuickCreationProjectTasks(
                    projectId = selectedProjectId,
                    page = nextPage,
                    size = pageSize,
                )
            }
            if (!isCurrentHistorySource(requestRevision, selectedProjectId)) return@launch
            history.fold(
                onSuccess = { page ->
                    val loadedItems = page.items.map { it.toQuickCreateHistoryUiItem() }
                    var mergedItems: List<QuickCreateHistoryUiItem> = emptyList()
                    uiState.update { current ->
                        val merged = (current.historyItems + loadedItems).distinctBy { it.taskId }
                        mergedItems = merged
                        current.copy(
                            historyLoadingMore = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = merged.size < page.total,
                            historyItems = merged,
                        )
                    }
                    updateHistoryRefreshJob(mergedItems)
                },
                onFailure = { error ->
                    uiState.update { current ->
                        current.copy(
                            historyLoadingMore = false,
                            error = error.message ?: "历史加载失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 切换到某个项目的任务列表。
     *
     * 项目任务复用历史区域展示，因此这里会先清空最近历史的分页状态，再加载项目任务第一页。
     * 相同项目重复点击会被忽略，避免重复请求覆盖当前列表滚动位置。
     */
    fun selectProject(projectId: String) {
        if (projectId.isBlank() || uiState.value.selectedProjectId == projectId) return

        cancelRefreshJob()
        cancelHistoryLoadJob()
        loadSelectedProjectTasks(projectId, clearExisting = true)
    }

    /**
     * 清除项目筛选并恢复最近历史。
     *
     * 先立即清空项目任务状态，再重新请求最近历史，避免 UI 在项目标签已取消时仍短暂展示旧项目任务。
     */
    fun clearSelectedProject() {
        if (uiState.value.selectedProjectId == null) return
        cancelRefreshJob()
        cancelHistoryLoadJob()
        uiState.update {
            it.copy(
                selectedProjectId = null,
                projectTasksLoading = false,
                historyLoadingMore = false,
                historyItems = emptyList(),
                historyPage = 0,
                historyTotal = 0,
                historyHasMore = false,
            )
        }
        loadRecentHistory()
    }

    /**
     * 刷新当前历史来源。
     *
     * 生成成功、取消任务成功或轮询刷新时都应保持用户当前上下文：用户在最近历史就刷新最近历史，
     * 用户在项目任务列表就刷新该项目任务，而不是强制跳回最近历史。
     */
    fun refreshCurrentHistoryArea() {
        val selectedProjectId = uiState.value.selectedProjectId
        if (selectedProjectId.isNullOrBlank()) {
            loadRecentHistory()
        } else {
            loadSelectedProjectTasks(selectedProjectId, clearExisting = false)
        }
    }

    /**
     * 取消历史任务并刷新当前历史来源。
     *
     * 同一任务只允许一个取消请求在途，防止用户连续点击时重复提交取消操作。取消成功后立即
     * 重拉当前第一页，以服务端最终状态为准更新任务状态和可操作按钮。
     */
    fun cancelHistoryTask(taskId: String) {
        if (taskId.isBlank() || taskId in uiState.value.historyCancellingTaskIds) return
        // 取消权限由历史 UI 模型统一派生，防止测试或未来入口绕过 Composable 去取消终态任务。
        val target = uiState.value.historyItems.firstOrNull { it.taskId == taskId } ?: return
        if (!target.canCancelTask) return

        scope.launch {
            uiState.update { state ->
                state.copy(historyCancellingTaskIds = state.historyCancellingTaskIds + taskId)
            }
            val result = historyRepository.cancelQuickCreationTask(taskId)
            result.fold(
                onSuccess = {
                    refreshLoadedQuickCreationHistory()
                    uiState.update { state ->
                        state.copy(historyCancellingTaskIds = state.historyCancellingTaskIds - taskId)
                    }
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            historyCancellingTaskIds = state.historyCancellingTaskIds - taskId,
                            error = error.message ?: "取消任务失败",
                        )
                    }
                },
            )
        }
    }

    /**
     * 加载历史输出详情。
     *
     * 详情弹窗每次打开都重新请求，确保输出地址、缩略图和任务状态使用服务端最新数据。
     */
    fun selectHistoryOutput(outputId: String) {
        scope.launch {
            uiState.update {
                it.copy(
                    historyDetailLoading = true,
                    selectedHistoryDetail = null,
                )
            }
            val detail = historyRepository.getQuickCreationHistoryDetail(outputId)
            uiState.update { state ->
                detail.fold(
                    onSuccess = { item ->
                        state.copy(
                            historyDetailLoading = false,
                            selectedHistoryDetail = item.toQuickCreateHistoryDetailUiItem(),
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            historyDetailLoading = false,
                            error = error.message ?: "历史详情加载失败",
                        )
                    },
                )
            }
        }
    }

    /**
     * 关闭历史详情弹窗并清理详情状态。
     */
    fun dismissHistoryDetail() {
        uiState.update {
            it.copy(
                historyDetailLoading = false,
                selectedHistoryDetail = null,
            )
        }
    }

    /**
     * 释放历史区域内部的长生命周期任务。
     *
     * ScreenModel 销毁时必须取消轮询 Job，避免页面离开后继续请求历史并回写已失效的状态容器。
     */
    fun dispose() {
        cancelHistoryLoadJob()
        cancelRefreshJob()
    }

    private fun loadSelectedProjectTasks(projectId: String, clearExisting: Boolean) {
        cancelHistoryLoadJob()
        val requestRevision = nextHistorySourceRevision()
        if (clearExisting) {
            uiState.update {
                it.copy(
                    selectedProjectId = projectId,
                    projectTasksLoading = true,
                    historyLoadingMore = false,
                    historyItems = emptyList(),
                    historyPage = 0,
                    historyTotal = 0,
                    historyHasMore = false,
                    error = null,
                )
            }
        } else {
            uiState.update {
                it.copy(
                    projectTasksLoading = true,
                    error = null,
                )
            }
        }

        historyLoadJob = scope.launch {
            val tasks = historyRepository.listQuickCreationProjectTasks(
                projectId = projectId,
                page = 1,
                size = pageSize,
            )
            if (!isCurrentHistorySource(requestRevision, selectedProjectId = projectId)) return@launch
            tasks.fold(
                onSuccess = { page ->
                    val historyItems = page.items.map { it.toQuickCreateHistoryUiItem() }
                    uiState.update { state ->
                        state.copy(
                            projectTasksLoading = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = page.items.size < page.total,
                            historyItems = historyItems,
                        )
                    }
                    updateHistoryRefreshJob(historyItems)
                },
                onFailure = { error ->
                    uiState.update { state ->
                        state.copy(
                            projectTasksLoading = false,
                            error = error.message ?: "项目任务加载失败",
                        )
                    }
                },
            )
        }
    }

    private fun updateHistoryRefreshJob(items: List<QuickCreateHistoryUiItem>) {
        if (items.none { it.needsRefresh }) {
            cancelRefreshJob()
            return
        }
        if (historyRefreshJob?.isActive == true) return

        val newJob = scope.launch {
            try {
                while (uiState.value.historyItems.any { it.needsRefresh }) {
                    delay(refreshIntervalMillis)
                    refreshLoadedQuickCreationHistory()
                }
            } finally {
                // 旧轮询取消后可能晚于新轮询完成 finally；只允许当前 Job 清理自己的引用。
                if (historyRefreshJob === currentCoroutineContext()[Job]) {
                    historyRefreshJob = null
                }
            }
        }
        historyRefreshJob = newJob
    }

    private suspend fun refreshLoadedQuickCreationHistory() {
        val size = maxOf(pageSize, uiState.value.historyItems.size)
        val selectedProjectId = uiState.value.selectedProjectId
        val requestRevision = historySourceRevision
        val history = if (selectedProjectId.isNullOrBlank()) {
            historyRepository.listQuickCreationHistory(page = 1, size = size)
        } else {
            historyRepository.listQuickCreationProjectTasks(
                projectId = selectedProjectId,
                page = 1,
                size = size,
            )
        }
        history.fold(
            onSuccess = { page ->
                if (isCurrentHistorySource(requestRevision, selectedProjectId)) {
                    applyHistoryRefreshPage(page)
                }
            },
            onFailure = { error ->
                uiState.update { state ->
                    state.copy(error = error.message ?: "历史刷新失败")
                }
            },
        )
    }

    private fun applyHistoryRefreshPage(page: QuickCreationHistoryPage) {
        val historyItems = page.items.map { it.toQuickCreateHistoryUiItem() }
        uiState.update { state ->
            state.copy(
                historyTotal = page.total,
                historyHasMore = page.items.size < page.total,
                historyItems = historyItems,
            )
        }
        updateHistoryRefreshJob(historyItems)
    }

    private fun nextHistorySourceRevision(): Long {
        // 来源切换会让旧请求结果失效，版本号用于拦截未及时响应取消的 Repository 调用。
        historySourceRevision += 1
        return historySourceRevision
    }

    private fun isCurrentHistorySource(
        requestRevision: Long,
        selectedProjectId: String?,
    ): Boolean =
        historySourceRevision == requestRevision && uiState.value.selectedProjectId == selectedProjectId

    private fun cancelHistoryLoadJob() {
        historyLoadJob?.cancel()
        historyLoadJob = null
    }

    private fun cancelRefreshJob() {
        historyRefreshJob?.cancel()
        historyRefreshJob = null
    }
}
