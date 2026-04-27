package com.runninghub.app.ui.feature.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val user: User? = null
)

class LoginScreenModel(
    private val authRepository: AuthRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChanged(phone: String) {
        _uiState.update { it.copy(phone = phone, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入手机号") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入密码") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.login(state.phone, state.password)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(isLoading = false, loginSuccess = true, user = user)
                    }
                }
                .onFailure { e ->
                    val msg = when {
                        e.message?.contains("ACCOUNT_NOT_EXIST") == true -> "账号不存在"
                        e.message?.contains("PASSWORD_ERROR") == true -> "密码错误"
                        e.message?.contains("ACCOUNT_DISABLED") == true -> "账号已被禁用"
                        e.message?.contains("TOKEN_INVALID") == true -> "登录状态异常，请重试"
                        e.message?.contains("length must be") == true -> "密码格式异常"
                        else -> e.message ?: "登录失败，请稍后重试"
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
