package com.runninghub.app.ui.feature.history

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.task.domain.GenerationHistoryItem
import com.runninghub.feature.task.domain.GenerationHistoryOutput
import com.runninghub.feature.task.domain.GenerationHistoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskHistoryFilter(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    IN_PROGRESS("In progress"),
}

data class TaskHistoryUiState(
    val isLoading: Boolean = false,
    val allItems: List<GenerationHistoryItem> = emptyList(),
    val items: List<TaskHistoryEntry> = emptyList(),
    val filter: TaskHistoryFilter = TaskHistoryFilter.ALL,
    val selectedDetail: GenerationHistoryItem? = null,
    val selectedOutput: GenerationHistoryOutput? = null,
    val isDetailLoading: Boolean = false,
    val reuseParams: Map<String, String> = emptyMap(),
    val actionMessage: String? = null,
    val error: String? = null,
)

data class TaskHistoryEntry(
    val taskId: String,
    val title: String,
    val status: String,
    val costTime: String? = null,
    val source: String,
    val outputId: String? = null,
    val thumbnailUrl: String? = null,
    val outputCount: Int = 0,
    val canViewOutput: Boolean = false,
    val canReuseParams: Boolean = false,
    val canRetry: Boolean = false,
    val canCancel: Boolean = false,
)

class TaskHistoryScreenModel(
    private val generationHistoryRepository: GenerationHistoryRepository,
    private val enablePolling: Boolean = true,
) : ScreenModel {

    private val _uiState = MutableStateFlow(TaskHistoryUiState())
    val uiState: StateFlow<TaskHistoryUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun loadHistory() {
        screenModelScope.launch { refreshHistory(showLoading = true) }
    }

    fun setFilter(filter: TaskHistoryFilter) {
        _uiState.update { state ->
            state.copy(
                filter = filter,
                items = state.allItems.toEntries(filter),
            )
        }
    }

    fun selectOutput(outputId: String) {
        screenModelScope.launch {
            _uiState.update { it.copy(isDetailLoading = true, error = null, actionMessage = null) }
            generationHistoryRepository.getHistoryDetail(outputId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            selectedDetail = detail,
                            selectedOutput = detail.outputs.firstOrNull { output -> output.outputId == outputId },
                            isDetailLoading = false,
                            actionMessage = "\u5df2\u52a0\u8f7d\u8f93\u51fa\u8be6\u60c5",
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            error = error.message ?: "History detail load failed",
                        )
                    }
                }
        }
    }

    fun prepareReuseParams(taskId: String) {
        val item = _uiState.value.allItems.firstOrNull { it.taskId == taskId }
            ?: _uiState.value.selectedDetail?.takeIf { it.taskId == taskId }
            ?: return
        _uiState.update {
            it.copy(
                selectedDetail = item,
                reuseParams = item.params,
                actionMessage = if (item.params.isEmpty()) {
                    "\u6ca1\u6709\u53ef\u590d\u7528\u53c2\u6570"
                } else {
                    "\u5df2\u51c6\u5907 ${item.params.size} \u4e2a\u53ef\u590d\u7528\u53c2\u6570"
                },
            )
        }
    }

    fun retryTask(taskId: String) {
        val item = _uiState.value.allItems.firstOrNull { it.taskId == taskId }
            ?: _uiState.value.selectedDetail?.takeIf { it.taskId == taskId }
            ?: return
        _uiState.update {
            it.copy(
                selectedDetail = item,
                reuseParams = item.params,
                actionMessage = if (item.params.isEmpty()) {
                    "\u6ca1\u6709\u53ef\u91cd\u8bd5\u53c2\u6570"
                } else {
                    "\u5df2\u51c6\u5907\u91cd\u8bd5\u53c2\u6570\uff0c\u8bf7\u5728\u521b\u5efa\u9875\u786e\u8ba4\u540e\u91cd\u65b0\u751f\u6210"
                },
            )
        }
    }

    fun cancelTask(taskId: String) {
        screenModelScope.launch {
            generationHistoryRepository.cancelTask(taskId)
                .onSuccess {
                    _uiState.update { state -> state.copy(actionMessage = "\u5df2\u8bf7\u6c42\u53d6\u6d88\u4efb\u52a1", error = null) }
                    refreshHistory(showLoading = false)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(actionMessage = null, error = error.message ?: "\u53d6\u6d88\u5931\u8d25")
                    }
                }
        }
    }

    private suspend fun refreshHistory(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, error = null, actionMessage = null) }
        }
        generationHistoryRepository.listHistory(page = 1, size = 50)
            .onSuccess { page ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        allItems = page.items,
                        items = page.items.toEntries(state.filter),
                        error = null,
                    )
                }
                updatePolling(page.items)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "History load failed",
                    )
                }
                updatePolling(emptyList())
            }
    }

    private fun updatePolling(items: List<GenerationHistoryItem>) {
        if (!enablePolling || items.none { it.isRunning }) {
            pollingJob?.cancel()
            pollingJob = null
            return
        }
        if (pollingJob?.isActive == true) return
        pollingJob = screenModelScope.launch {
            while (_uiState.value.allItems.any { it.isRunning }) {
                delay(10_000)
                refreshHistory(showLoading = false)
            }
        }
    }

    override fun onDispose() {
        pollingJob?.cancel()
        super.onDispose()
    }
}

private fun List<GenerationHistoryItem>.toEntries(filter: TaskHistoryFilter): List<TaskHistoryEntry> =
    filterByStatus(filter).map { it.toTaskHistoryEntry() }

private fun List<GenerationHistoryItem>.filterByStatus(filter: TaskHistoryFilter): List<GenerationHistoryItem> = when (filter) {
    TaskHistoryFilter.ALL -> this
    TaskHistoryFilter.COMPLETED -> filter { it.status.isCompletedStatus() }
    TaskHistoryFilter.FAILED -> filter { it.status.isFailedStatus() }
    TaskHistoryFilter.IN_PROGRESS -> filter { it.isRunning }
}

private fun GenerationHistoryItem.toTaskHistoryEntry(): TaskHistoryEntry {
    val primaryOutput = outputs.firstOrNull()
    return TaskHistoryEntry(
        taskId = taskId,
        title = taskType ?: modelId ?: "Generation task",
        status = status,
        costTime = costTime,
        source = source.key,
        outputId = primaryOutput?.outputId,
        thumbnailUrl = primaryOutput?.thumbnailUrl ?: primaryOutput?.url,
        outputCount = outputs.size,
        canViewOutput = primaryOutput != null,
        canReuseParams = params.isNotEmpty(),
        canRetry = status.isFailedStatus(),
        canCancel = isRunning,
    )
}

private val GenerationHistoryItem.isRunning: Boolean
    get() = !status.isCompletedStatus() && !status.isFailedStatus() && !status.isCancelledStatus()

private fun String.isCompletedStatus(): Boolean =
    uppercase() in setOf("SUCCESS", "COMPLETED", "DONE")

private fun String.isFailedStatus(): Boolean =
    uppercase() in setOf("FAILED", "FAIL", "ERROR")

private fun String.isCancelledStatus(): Boolean =
    uppercase() in setOf("CANCELED", "CANCELLED")
