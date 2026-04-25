package com.runninghub.app.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.local.UserPreferencesRepository
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.repository.UserRepository
import com.runninghub.app.data.remote.model.AccountStatusRequest
import com.runninghub.app.data.remote.model.UserDto
import com.runninghub.app.data.remote.model.WalletInfoDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [INPUT]: WebAppApi
 * [OUTPUT]: ProfileUiState
 * [POS]: 个人中心业务逻辑，负责获取账户状态
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        // Observe Repository State (Preloaded or Updated)
        viewModelScope.launch {
            userRepository.user.collect { user ->
                val apiKey = userPreferencesRepository.getApiKey()
                val enterpriseKey = userPreferencesRepository.getEnterpriseApiKey()
                val hasAppKey = !apiKey.isNullOrEmpty()
                val hasEnterpriseKey = !enterpriseKey.isNullOrEmpty()
                _uiState.update { 
                    it.copy(
                        user = user, 
                        hasApiKey = user != null, 
                        hasAppApiKey = hasAppKey,
                        hasEnterpriseApiKey = hasEnterpriseKey
                    ) 
                }
            }
        }

        viewModelScope.launch {
            userRepository.accountStatus.collect { status ->
                 _uiState.update { it.copy(accountStatus = status) }
            }
        }

        viewModelScope.launch {
            userRepository.isLoading.collect { loading ->
                if (!loading) {
                    // Only update loading to false, as we might want to control localized loading
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                     // Can optionally show loading if state is empty
                     if (_uiState.value.user == null) {
                         _uiState.update { it.copy(isLoading = true) }
                     }
                }
            }
        }
        
        viewModelScope.launch {
             userRepository.error.collect { error ->
                 if (error != null) {
                      _uiState.update { it.copy(error = error) }
                 }
             }
        }
    }

    fun bindApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(bindError = "内容不能为空") }
            return
        }

        if (apiKey.contains("Rh-AccessToken", ignoreCase = true) || apiKey.contains("userid=", ignoreCase = true)) {
            bindCookie(apiKey)
        } else {
            bindRealApiKey(apiKey)
        }
    }

    fun bindAppApiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) return@launch
            userPreferencesRepository.saveApiKey(apiKey)
            _uiState.update { it.copy(hasAppApiKey = true) }
            userRepository.refreshUserData()
        }
    }

    fun bindEnterpriseApiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) return@launch
            userPreferencesRepository.saveEnterpriseApiKey(apiKey)
            _uiState.update { it.copy(hasEnterpriseApiKey = true) }
        }
    }

    private fun bindCookie(cookie: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBinding = true, bindError = null) }
            userPreferencesRepository.saveCookie(cookie)
            // Do NOT clear API Key
            
            // Delegate verification to Repository
            userRepository.refreshUserData()
            
            // Check result via repository state delay or assume success provided flow updates
            // For better UX, we might want to manually check error state
            _uiState.update { it.copy(isBinding = false) }
        }
    }

    private fun bindRealApiKey(apiKey: String) {
        viewModelScope.launch {
             _uiState.update { it.copy(isBinding = true, bindError = null) }
             userPreferencesRepository.saveApiKey(apiKey)
             // DO NOT clear Cookie
             
             // Delegate to Repository
             userRepository.refreshUserData()
             
             _uiState.update { it.copy(isBinding = false) }
        }
    }

    fun unbindApiKey() {
        userPreferencesRepository.clearApiKey()
        userPreferencesRepository.clearEnterpriseApiKey()
        userPreferencesRepository.clearCookie()
        userRepository.clearUserData()
        // State update will happen via Flow collection
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            userRepository.refreshUserData()
        }
    }
}
