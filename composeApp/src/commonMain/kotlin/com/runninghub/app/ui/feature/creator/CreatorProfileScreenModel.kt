package com.runninghub.app.ui.feature.creator

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.repository.UserRepository
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatorProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val apps: List<WebApp> = emptyList(),
    val isFollowing: Boolean = false,
    val error: String? = null
)

class CreatorProfileScreenModel(
    private val userRepository: UserRepository,
    private val webAppRepository: WebAppRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(CreatorProfileUiState())
    val uiState: StateFlow<CreatorProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String = ""

    fun loadProfile(userId: String) {
        if (userId == currentUserId && _uiState.value.user != null) return
        currentUserId = userId

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val userDeferred = async { userRepository.getUserDetail(userId) }
            val followDeferred = async { userRepository.isFollow(userId) }
            val appsDeferred = async { webAppRepository.getUserAppList(userId, pageNum = 1, pageSize = 20) }

            userDeferred.await()
                .onSuccess { user -> _uiState.update { it.copy(user = user) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "加载失败") } }

            followDeferred.await()
                .onSuccess { following -> _uiState.update { it.copy(isFollowing = following) } }

            appsDeferred.await()
                .onSuccess { page -> _uiState.update { it.copy(apps = page.records) } }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleFollow() {
        val userId = currentUserId.ifBlank { return }
        screenModelScope.launch {
            val isFollowing = _uiState.value.isFollowing
            val result = if (isFollowing) {
                userRepository.unFollowUser(userId)
            } else {
                userRepository.followUser(userId)
            }
            result.onSuccess {
                _uiState.update { state ->
                    val updatedUser = state.user?.let { user ->
                        val fans = user.fanCount.toIntOrNull() ?: 0
                        val newFans = if (isFollowing) (fans - 1).coerceAtLeast(0) else fans + 1
                        user.copy(fanCount = newFans.toString())
                    }
                    state.copy(isFollowing = !isFollowing, user = updatedUser)
                }
            }
        }
    }

    fun loadUserApps() {
        val userId = currentUserId.ifBlank { return }
        screenModelScope.launch {
            webAppRepository.getUserAppList(userId, pageNum = 1, pageSize = 20)
                .onSuccess { page -> _uiState.update { it.copy(apps = page.records) } }
        }
    }
}
