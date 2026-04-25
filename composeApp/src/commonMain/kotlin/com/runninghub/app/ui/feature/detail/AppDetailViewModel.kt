package com.runninghub.app.ui.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.TaskRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val detail: WebAppDetail? = null,
    val error: String? = null,
    val taskState: TaskUiState = TaskUiState.Idle,
    val inputValues: Map<String, String> = emptyMap()
)

sealed class TaskUiState {
    data object Idle : TaskUiState()
    data object Submitting : TaskUiState()
    data class Running(val taskId: Long) : TaskUiState()
    data class Success(val outputs: List<TaskOutput>) : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

class AppDetailViewModel(
    private val webAppRepository: WebAppRepository,
    private val taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    fun loadDetail(appId: String) {
        viewModelScope.launch {
            val apiKey = settingsRepository.getApiKeySync()
            webAppRepository.getWebAppDetail(appId, apiKey).collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update { it.copy(isLoading = false, detail = result.data) }
                    is AppResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is AppResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun updateInputValue(fieldName: String, value: String) {
        _uiState.update { it.copy(inputValues = it.inputValues + (fieldName to value)) }
    }

    fun runTask() {
        val detail = _uiState.value.detail ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(taskState = TaskUiState.Submitting) }
            val apiKey = settingsRepository.getApiKeySync()
            val inputNodes = detail.inputNodes.map { node ->
                node.copy(fieldValue = _uiState.value.inputValues[node.fieldName] ?: node.fieldValue)
            }
            val result = taskRepository.runTask(
                webappId = detail.id.toLongOrNull() ?: 0,
                apiKey = apiKey,
                inputNodes = inputNodes
            )
            when (result) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(taskState = TaskUiState.Running(result.data.taskId)) }
                    pollOutputs(result.data.taskId, apiKey)
                }
                is AppResult.Error -> _uiState.update { it.copy(taskState = TaskUiState.Error(result.message)) }
                else -> {}
            }
        }
    }

    private fun pollOutputs(taskId: Long, apiKey: String) {
        viewModelScope.launch {
            taskRepository.pollTaskOutputs(taskId, apiKey).collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        if (result.data.status == TaskStatus.SUCCESS) {
                            _uiState.update { it.copy(taskState = TaskUiState.Success(result.data.outputs)) }
                        } else if (result.data.status == TaskStatus.FAILED) {
                            _uiState.update { it.copy(taskState = TaskUiState.Error(result.data.errorMessage ?: "Task failed")) }
                        }
                    }
                    is AppResult.Error -> _uiState.update { it.copy(taskState = TaskUiState.Error(result.message)) }
                    else -> {}
                }
            }
        }
    }

    fun resetTask() {
        _uiState.update { it.copy(taskState = TaskUiState.Idle) }
    }
}
