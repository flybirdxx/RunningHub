package com.runninghub.app.ui.feature.creator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val isFollowing: Boolean = false,
    val userApps: List<WebApp> = emptyList(),
    val error: String? = null
)

class CreatorProfileViewModel(
    private val userRepository: UserRepository,
    private val webAppRepository: WebAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorProfileUiState())
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun loadProfile(userId: String) {
        currentUserId = userId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            launch {
                userRepository.getUserDetail(userId).collect { result ->
                    when (result) {
                        is AppResult.Success -> _uiState.update {
                            it.copy(user = result.data, isLoading = false)
                        }
                        is AppResult.Error -> _uiState.update {
                            it.copy(error = result.message, isLoading = false)
                        }
                        is AppResult.Loading -> {}
                    }
                }
            }

            launch {
                when (val result = userRepository.isFollowing(userId)) {
                    is AppResult.Success -> _uiState.update { it.copy(isFollowing = result.data) }
                    else -> {}
                }
            }

            launch {
                webAppRepository.getUserApps(userId = userId, page = 1, pageSize = 30)
                    .collect { result ->
                        when (result) {
                            is AppResult.Success -> _uiState.update { it.copy(userApps = result.data) }
                            is AppResult.Error -> {}
                            is AppResult.Loading -> {}
                        }
                    }
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            val following = _uiState.value.isFollowing
            val result = if (following) {
                userRepository.unfollowUser(currentUserId)
            } else {
                userRepository.followUser(currentUserId)
            }
            if (result is AppResult.Success) {
                _uiState.update { state ->
                    val updatedUser = state.user?.let { user ->
                        user.copy(
                            fanCount = user.fanCount + if (following) -1 else 1
                        )
                    }
                    state.copy(isFollowing = !following, user = updatedUser)
                }
            }
        }
    }
}
