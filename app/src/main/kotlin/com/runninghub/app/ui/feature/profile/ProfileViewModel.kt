package com.runninghub.app.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.local.UserPreferencesRepository
import com.runninghub.app.data.remote.api.WebAppApi
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
    private val webAppApi: WebAppApi,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val apiKey = userPreferencesRepository.getApiKey()
        val cookie = userPreferencesRepository.getCookie()
        
        val hasLogin = !apiKey.isNullOrEmpty() || !cookie.isNullOrEmpty()
        val hasAppKey = !apiKey.isNullOrEmpty()
        
        _uiState.update { it.copy(hasApiKey = hasLogin, hasAppApiKey = hasAppKey) }

        if (hasLogin) {
            refresh()
        }
    }

    fun bindApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(bindError = "内容不能为空") }
            return
        }

        // Determine if input is a Cookie or API Key
        if (apiKey.contains("Rh-AccessToken", ignoreCase = true) || apiKey.contains("userid=", ignoreCase = true)) {
            bindCookie(apiKey)
        } else {
            bindRealApiKey(apiKey)
        }
    }

    // New function specifically for setting App API Key (Hybrid Mode)
    fun bindAppApiKey(apiKey: String) {
        viewModelScope.launch {
            if (apiKey.isBlank()) return@launch
            userPreferencesRepository.saveApiKey(apiKey)
            _uiState.update { it.copy(hasAppApiKey = true) }
            refresh()
        }
    }

    private fun bindCookie(cookie: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBinding = true, bindError = null) }
            userPreferencesRepository.saveCookie(cookie)
            // User: Support Hybrid Auth - DO NOT clear API Key
            // userPreferencesRepository.clearApiKey()

            try {
                // Verify by fetching UserInfo directly
                val userId = Regex("userId=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)?.groupValues?.get(1) ?: ""
                val userResponse = webAppApi.getUserInfo(if (userId.isNotEmpty()) mapOf("userId" to userId) else emptyMap())
                
                if (userResponse.code == 0) {
                    val user = userResponse.data
                    _uiState.update { it.copy(
                        isBinding = false,
                        hasApiKey = true,
                        user = user,
                        accountStatus = null,
                        hasAppApiKey = !userPreferencesRepository.getApiKey().isNullOrEmpty()
                    ) }
                    // Also try to sync account status for completeness
                    val accountResponse = webAppApi.getAccountStatus(AccountStatusRequest(apikey = "")) 
                    if (accountResponse.code == 0) {
                         _uiState.update { it.copy(accountStatus = accountResponse.data) }
                    }
                } else {
                     userPreferencesRepository.clearCookie()
                    _uiState.update { it.copy(
                        isBinding = false,
                        bindError = "Cookie 无效: ${userResponse.msg}"
                    ) }
                }
            } catch (e: Exception) {
                userPreferencesRepository.clearCookie()
                _uiState.update { it.copy(isBinding = false, bindError = "网络错误: ${e.localizedMessage}") }
            }
        }
    }

    private fun bindRealApiKey(apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBinding = true, bindError = null) }
            
            // Save temporarily to test connectivity
            userPreferencesRepository.saveApiKey(apiKey)
            // Valid valid hybrid state: DO NOT clear cookie if it exists
            // userPreferencesRepository.clearCookie()
            
            try {
                // Verify by fetching account status
                val statusResponse = webAppApi.getAccountStatus(AccountStatusRequest(apikey = apiKey))
                
                if (statusResponse.code == 0) {
                    // Success! Key is valid.
                    val status = statusResponse.data
                    val cookie = userPreferencesRepository.getCookie()
                    val isCookieLogin = !cookie.isNullOrEmpty()
                    
                    val user = if (isCookieLogin) _uiState.value.user else UserDto(
                         id = "--",
                         nickName = "RunningHub用户", // Default name
                         headIcon = null,
                         mobile = null,
                         totalCoin = status?.remainCoins,
                         memberInfo = null,
                         walletInfo = WalletInfoDto(
                             balance = status?.remainMoney?.toDoubleOrNull() ?: 0.0,
                             currency = status?.currency,
                             currencySymbol = "¥"
                         ),
                         apiKey = apiKey,
                         apiType = status?.apiType
                     )
                    _uiState.update { it.copy(
                        isBinding = false, 
                        hasApiKey = true, 
                        hasAppApiKey = true,
                        accountStatus = status,
                        user = user
                    ) }
                    
                    // If cookie exists, refresh to ensure full profile
                    if (isCookieLogin) refresh()
                } else {
                    // Failed
                    if (userPreferencesRepository.getCookie().isNullOrEmpty()) {
                         userPreferencesRepository.clearApiKey()
                         _uiState.update { it.copy(
                            isBinding = false, 
                            hasAppApiKey = false,
                            hasApiKey = false, 
                            bindError = "绑定失败: ${statusResponse.msg} (Code: ${statusResponse.code})"
                        ) }
                    } else {
                        // Cookie exists, just clear key and report error
                         userPreferencesRepository.clearApiKey()
                         _uiState.update { it.copy(
                            isBinding = false, 
                            hasAppApiKey = false,
                            bindError = "API Key 无效: ${statusResponse.msg}"
                        ) }
                    }
                }
            } catch (e: Exception) {
                 if (userPreferencesRepository.getCookie().isNullOrEmpty()) {
                    userPreferencesRepository.clearApiKey()
                    _uiState.update { it.copy(
                        isBinding = false, 
                        hasApiKey = false, 
                        bindError = "网络错误: ${e.localizedMessage}"
                    ) }
                } else {
                     userPreferencesRepository.clearApiKey()
                     _uiState.update { it.copy(isBinding = false, bindError = "验证 API Key 网络错误") }
                }
            }
        }
    }

    fun unbindApiKey() {
        userPreferencesRepository.clearApiKey()
        userPreferencesRepository.clearCookie()
        _uiState.update { ProfileUiState() } // Reset all state
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentKey = userPreferencesRepository.getApiKey() ?: ""
            val cookie = userPreferencesRepository.getCookie()
            
            try {

                if (!cookie.isNullOrEmpty()) {
                    // Optimized path: Cookie exists, call UserInfo
                    // Extract userId from cookie if present
                    val userId = Regex("userId=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)?.groupValues?.get(1) ?: ""
                    
                    val userResponse = webAppApi.getUserInfo(if (userId.isNotEmpty()) mapOf("userId" to userId) else emptyMap())
                    
                    if (userResponse.code == 0) {
                        _uiState.update { it.copy(isLoading = false, user = userResponse.data) }
                        // Refresh Account Status silently
                        launch {
                            try {
                                val statusResponse = webAppApi.getAccountStatus(AccountStatusRequest(apikey = ""))
                                if (statusResponse.code == 0) {
                                    _uiState.update { it.copy(accountStatus = statusResponse.data) }
                                }
                            } catch (_: Exception) {}
                        }
                    } else {
                       // Cookie might have expired or params invalid
                       _uiState.update { it.copy(isLoading = false, error = "Login failed: ${userResponse.msg}") }
                    }
                } else {
                    // Legacy path: API Key Only
                    val statusResponse = webAppApi.getAccountStatus(AccountStatusRequest(apikey = currentKey))
                    if (statusResponse.code == 0) {
                        val status = statusResponse.data
                        val fallbackUser = UserDto(
                             id = "--",
                             nickName = "RunningHub用户",
                             headIcon = null,
                             mobile = null,
                             totalCoin = status?.remainCoins,
                             memberInfo = null,
                             walletInfo = WalletInfoDto(
                                 balance = status?.remainMoney?.toDoubleOrNull() ?: 0.0,
                                 currency = status?.currency,
                                 currencySymbol = "¥"
                             ),
                             apiKey = null,
                             apiType = status?.apiType
                         )
                        _uiState.update { it.copy(isLoading = false, accountStatus = status, user = fallbackUser) }
                    } else {
                         _uiState.update { it.copy(isLoading = false, error = statusResponse.msg) }
                    }
                }
            } catch (e: Exception) {
                 _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }
}
