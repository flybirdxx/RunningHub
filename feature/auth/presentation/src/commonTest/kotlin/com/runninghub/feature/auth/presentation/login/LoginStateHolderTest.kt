package com.runninghub.feature.auth.presentation.login

import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.SmsError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginStateHolderTest {
    @Test
    fun `wrong sms code clears input and maps presentation message`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(smsLoginResult = Result.failure(SmsError.WrongCode())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onSmsCodeChanged("1234")
        stateHolder.smsLogin()
        advanceUntilIdle()

        assertEquals("", stateHolder.uiState.value.smsCode)
        assertEquals("验证码错误，请重新输入", stateHolder.uiState.value.errorMessage)
    }

    @Test
    fun `network sms login failure preserves input for retry`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(smsLoginResult = Result.failure(SmsError.Network())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onSmsCodeChanged("1234")
        stateHolder.smsLogin()
        advanceUntilIdle()

        assertEquals("1234", stateHolder.uiState.value.smsCode)
        assertEquals("网络连接失败，请检查网络后重试", stateHolder.uiState.value.errorMessage)
    }

    @Test
    fun `network password login failure maps presentation message`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(passwordLoginResult = Result.failure(AuthError.Network())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onPasswordChanged("password")
        stateHolder.pwdLogin()
        advanceUntilIdle()

        assertEquals("网络连接失败，请检查网络后重试", stateHolder.uiState.value.errorMessage)
    }

    @Test
    fun `captcha verification error asks presentation to open captcha dialog`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(sendSmsCodeResult = Result.failure(SmsError.CaptchaRequired())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.sendSmsCode()
        advanceUntilIdle()

        assertEquals(true, stateHolder.uiState.value.requiresSmsCaptcha)
        assertEquals("请先完成图形验证后再获取验证码", stateHolder.uiState.value.errorMessage)
    }

    @Test
    fun `verified captcha token closes dialog and retries sending sms`() = runTest {
        val repository = FakeAuthRepository(
            sendSmsCodeResult = Result.failure(SmsError.CaptchaRequired()),
        )
        val stateHolder = LoginStateHolder(
            authRepository = repository,
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.sendSmsCode()
        advanceUntilIdle()
        assertEquals(true, stateHolder.uiState.value.requiresSmsCaptcha)

        repository.sendSmsCodeResult = Result.success(Unit)
        stateHolder.onSmsCaptchaVerified("captcha-token")
        assertEquals(false, stateHolder.uiState.value.requiresSmsCaptcha)
        advanceUntilIdle()

        assertEquals("captcha-token", repository.lastCaptchaToken)
    }

    private class FakeAuthRepository(
        private val smsLoginResult: Result<User> = Result.failure(NotImplementedError()),
        private val passwordLoginResult: Result<User> = Result.failure(NotImplementedError()),
        var sendSmsCodeResult: Result<Unit> = Result.success(Unit),
    ) : AuthRepository {
        var lastCaptchaToken: String? = null
            private set

        override suspend fun login(phone: String, password: String): Result<User> =
            passwordLoginResult

        override suspend fun sendSmsCode(phone: String, captchaToken: String?): Result<Unit> {
            lastCaptchaToken = captchaToken
            return sendSmsCodeResult
        }

        override suspend fun smsLogin(phone: String, code: String): Result<User> =
            smsLoginResult

        override suspend fun logout() = Unit

        override suspend fun isLoggedIn(): Boolean = false

        override suspend fun refreshTokenIfNeeded(): Result<String> =
            Result.failure(NotImplementedError())

        override suspend fun getCurrentAuthToken(): String? = null

        override suspend fun getCurrentUserId(): String? = null
    }
}
