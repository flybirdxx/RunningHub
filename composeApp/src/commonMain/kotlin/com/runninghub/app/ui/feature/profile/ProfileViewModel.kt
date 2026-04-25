package com.runninghub.app.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val accountStatus: AccountStatus? = null,
    val error: String? = null,
    val isApiKeyBound: Boolean = false
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            userRepository.getCurrentUser().collect { result ->
                when (result) {
                    is AppResult.Success -> _uiState.update {
                        it.copy(isLoading = false, user = result.data, isApiKeyBound = true)
                    }
                    is AppResult.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    is AppResult.Loading -> {}
                }
            }
        }
    }

    fun bindApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
            refresh()
        }
    }

    fun unbindApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
            settingsRepository.clearCookie()
            _uiState.update { ProfileUiState() }
        }
    }
}
