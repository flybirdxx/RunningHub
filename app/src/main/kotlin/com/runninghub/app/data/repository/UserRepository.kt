package com.runninghub.app.data.repository

import com.runninghub.app.data.local.UserPreferencesRepository
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.remote.model.AccountStatusDto
import com.runninghub.app.data.remote.model.AccountStatusRequest
import com.runninghub.app.data.remote.model.UserDto
import com.runninghub.app.data.remote.model.WalletInfoDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val webAppApi: WebAppApi,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val _user = MutableStateFlow<UserDto?>(null)
    val user: StateFlow<UserDto?> = _user.asStateFlow()

    private val _accountStatus = MutableStateFlow<AccountStatusDto?>(null)
    val accountStatus: StateFlow<AccountStatusDto?> = _accountStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun refreshUserData() {
        _isLoading.value = true
        _error.value = null
        
        val currentKey = userPreferencesRepository.getApiKey() ?: ""
        val cookie = userPreferencesRepository.getCookie()

        try {
            if (!cookie.isNullOrEmpty()) {
                // Optimized path: Cookie exists, call UserInfo
                val userId = Regex("userId=([^;]+)", RegexOption.IGNORE_CASE).find(cookie)?.groupValues?.get(1) ?: ""
                
                val userResponse = webAppApi.getUserInfo(if (userId.isNotEmpty()) mapOf("userId" to userId) else emptyMap())
                
                if (userResponse.code == 0) {
                    _user.value = userResponse.data
                    // Refresh Account Status silently
                    try {
                        val statusResponse = webAppApi.getAccountStatus(AccountStatusRequest(apikey = ""))
                        if (statusResponse.code == 0) {
                            _accountStatus.value = statusResponse.data
                        }
                    } catch (_: Exception) {}
                } else {
                    _error.value = "Login failed: ${userResponse.msg}"
                }
            } else if (currentKey.isNotEmpty()) {
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
                    _accountStatus.value = status
                    _user.value = fallbackUser
                } else {
                    _error.value = statusResponse.msg
                }
            } else {
                // No auth data
                 _user.value = null
                 _accountStatus.value = null
            }
        } catch (e: Exception) {
            _error.value = e.localizedMessage
        } finally {
            _isLoading.value = false
        }
    }

    fun clearUserData() {
        _user.value = null
        _accountStatus.value = null
        _error.value = null
    }

    // Helper for binding logic updates
    fun updateUser(user: UserDto) {
        _user.value = user
    }

    fun updateAccountStatus(status: AccountStatusDto) {
        _accountStatus.value = status
    }
}
