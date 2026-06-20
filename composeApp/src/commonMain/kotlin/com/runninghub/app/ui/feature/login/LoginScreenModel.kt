package com.runninghub.app.ui.feature.login

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.feature.auth.domain.SmsError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val phone: String = "",
    val smsCode: String = "",
    val password: String = "",
    val isSmsMode: Boolean = true,  // true=SMS verification, false=password
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val countdownSeconds: Int = 0,
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

    fun onSmsCodeChanged(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(smsCode = filtered, errorMessage = null) }
    }

    fun sendSmsCode() {
        val phone = _uiState.value.phone
        if (phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入手机号") }
            return
        }
        if (!isValidPhone(phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (_uiState.value.countdownSeconds > 0) return

        screenModelScope.launch {
            _uiState.update { it.copy(isSendingCode = true, errorMessage = null) }
            authRepository.sendSmsCode(phone)
                .onSuccess {
                    _uiState.update { it.copy(isSendingCode = false, countdownSeconds = 60) }
                    startCountdown()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSendingCode = false,
                            errorMessage = e.message ?: "发送失败，请稍后重试"
                        )
                    }
                }
        }
    }

    fun smsLogin() {
        val state = _uiState.value
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入手机号") }
            return
        }
        if (!isValidPhone(state.phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (state.smsCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入验证码") }
            return
        }
        if (state.smsCode.length < 4) {
            _uiState.update { it.copy(errorMessage = "请输入完整验证码") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.smsLogin(state.phone, state.smsCode)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(isLoading = false, loginSuccess = true, user = user)
                    }
                }
                .onFailure { e ->
                    val msg = e.message ?: "登录失败，请稍后重试"
                    val codeExpired = e is SmsError.CodeExpired
                    val wrongCode = e is SmsError.WrongCode
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = msg,
                            // AC3: wrong code → clear. AC4: expired → preserve. AC6: network → preserve.
                            smsCode = if (wrongCode) "" else it.smsCode,
                            countdownSeconds = if (codeExpired) 0 else it.countdownSeconds
                        )
                    }
                }
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun toggleMode() {
        _uiState.update { it.copy(isSmsMode = !it.isSmsMode, errorMessage = null) }
    }

    fun pwdLogin() {
        val state = _uiState.value
        if (state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入手机号") }
            return
        }
        if (!isValidPhone(state.phone)) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "登录失败，请稍后重试"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun startCountdown() {
        screenModelScope.launch {
            while (_uiState.value.countdownSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(countdownSeconds = it.countdownSeconds - 1) }
            }
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 11 && phone.all { it.isDigit() }
    }
}
