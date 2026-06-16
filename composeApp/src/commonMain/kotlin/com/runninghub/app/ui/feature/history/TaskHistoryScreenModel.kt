package com.runninghub.app.ui.feature.history

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.TaskHistoryItem
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskHistoryFilter(val label: String) {
    ALL("全部"),
    COMPLETED("已完成"),
    FAILED("失败"),
    IN_PROGRESS("进行中"),
}

data class TaskHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<TaskHistoryItem> = emptyList(),
    val filter: TaskHistoryFilter = TaskHistoryFilter.ALL,
    val error: String? = null,
)

class TaskHistoryScreenModel(
    private val webAppRepository: WebAppRepository,
    private val settingsRepository: SettingsRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(TaskHistoryUiState())
    val uiState: StateFlow<TaskHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory() {
        screenModelScope.launch {
            val apiKey = settingsRepository.getApiKey().orEmpty()
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = emptyList(),
                        error = "请先绑定 API Key",
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            webAppRepository.getTaskHistory(
                apiKey = apiKey,
                pageNum = 1,
                pageSize = 50,
            ).onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        error = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "任务历史加载失败",
                    )
                }
            }
        }
    }

    fun setFilter(filter: TaskHistoryFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}
