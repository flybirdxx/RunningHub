package com.runninghub.app.ui.feature.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val accountStatus: AccountStatus? = null,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val showApiKeyDialog: Boolean = false,
    val showCookieDialog: Boolean = false
)

class ProfileScreenModel(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshUserData()
    }

    fun loadUserData() {
        refreshUserData()
    }

    fun refreshUserData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val loggedIn = authRepository.isLoggedIn()
            if (!loggedIn) {
                _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                return@launch
            }

            try {
                val userId = authRepository.getCurrentUserId()
                val userResult = userRepository.getUserInfo(userId)
                userResult.fold(
                    onSuccess = { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = user,
                                isLoggedIn = true,
                                error = null
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                error = e.message ?: "加载用户信息失败"
                            )
                        }
                    }
                )

                val apiKey = settingsRepository.getApiKey().orEmpty()
                if (apiKey.isNotBlank()) {
                    userRepository.getAccountStatus(apiKey).onSuccess { status ->
                        _uiState.update { it.copy(accountStatus = status) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "网络请求失败")
                }
            }
        }
    }

    fun bindApiKey(key: String) {
        if (key.isBlank()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showApiKeyDialog = false) }
            settingsRepository.setApiKey(key)
            refreshUserData()
        }
    }

    fun bindCookie(cookie: String) {
        if (cookie.isBlank()) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCookieDialog = false) }
            settingsRepository.setCookie(cookie)
            refreshUserData()
        }
    }

    fun unbindApiKey() {
        screenModelScope.launch {
            settingsRepository.clearApiKey()
            settingsRepository.clearEnterpriseApiKey()
            settingsRepository.clearCookie()
            _uiState.update { ProfileUiState(isLoading = false) }
        }
    }

    fun logout(onLoggedOut: () -> Unit = {}) {
        screenModelScope.launch {
            authRepository.logout()
            _uiState.update { ProfileUiState(isLoading = false) }
            onLoggedOut()
        }
    }

    fun showApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true) }
    }

    fun dismissApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false) }
    }

    fun showCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = true) }
    }

    fun dismissCookieDialog() {
        _uiState.update { it.copy(showCookieDialog = false) }
    }
}
