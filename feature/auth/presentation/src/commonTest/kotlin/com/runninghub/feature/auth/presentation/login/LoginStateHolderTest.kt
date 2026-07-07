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
    fun `wrong sms code clears input and maps stable error`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(smsLoginResult = Result.failure(SmsError.WrongCode())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onSmsCodeChanged("1234")
        stateHolder.smsLogin()
        advanceUntilIdle()

        assertEquals("", stateHolder.uiState.value.smsCode)
        assertEquals(LoginErrorText.WrongSmsCode, stateHolder.uiState.value.error)
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
        assertEquals(LoginErrorText.Network, stateHolder.uiState.value.error)
    }

    @Test
    fun `network password login failure maps stable error`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(passwordLoginResult = Result.failure(AuthError.Network())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onPasswordChanged("password")
        stateHolder.pwdLogin()
        advanceUntilIdle()

        assertEquals(LoginErrorText.Network, stateHolder.uiState.value.error)
    }

    @Test
    fun `unknown throwable never exposes throwable message`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(
                passwordLoginResult = Result.failure(IllegalStateException("REMOTE_MSG_SHOULD_NOT_APPEAR")),
            ),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onPasswordChanged("password")
        stateHolder.pwdLogin()
        advanceUntilIdle()

        assertEquals(LoginErrorText.LoginFailed, stateHolder.uiState.value.error)
    }

    @Test
    fun `empty phone maps stable local validation error`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(),
            coroutineScope = this,
        )

        stateHolder.sendSmsCode()

        assertEquals(LoginErrorText.PhoneRequired, stateHolder.uiState.value.error)
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
        assertEquals(LoginErrorText.CaptchaRequired, stateHolder.uiState.value.error)
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

    @Test
    fun `dismissed captcha ignores stale token and preserves input`() = runTest {
        val repository = FakeAuthRepository(
            sendSmsCodeResult = Result.failure(SmsError.CaptchaRequired()),
        )
        val stateHolder = LoginStateHolder(
            authRepository = repository,
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.onSmsCodeChanged("123456")
        stateHolder.sendSmsCode()
        advanceUntilIdle()
        assertEquals(true, stateHolder.uiState.value.requiresSmsCaptcha)
        assertEquals(1, repository.sendSmsCodeCalls)

        repository.sendSmsCodeResult = Result.success(Unit)
        stateHolder.dismissSmsCaptcha()
        stateHolder.onSmsCaptchaVerified("stale-captcha-token")
        advanceUntilIdle()

        assertEquals(false, stateHolder.uiState.value.requiresSmsCaptcha)
        assertEquals("13800138000", stateHolder.uiState.value.phone)
        assertEquals("123456", stateHolder.uiState.value.smsCode)
        assertEquals(1, repository.sendSmsCodeCalls)
        assertEquals(null, repository.lastCaptchaToken)
    }

    @Test
    fun `blank captcha token keeps active dialog open and maps stable error`() = runTest {
        val stateHolder = LoginStateHolder(
            authRepository = FakeAuthRepository(sendSmsCodeResult = Result.failure(SmsError.CaptchaRequired())),
            coroutineScope = this,
        )

        stateHolder.onPhoneChanged("13800138000")
        stateHolder.sendSmsCode()
        advanceUntilIdle()
        stateHolder.onSmsCaptchaVerified("")

        assertEquals(true, stateHolder.uiState.value.requiresSmsCaptcha)
        assertEquals(LoginErrorText.CaptchaInvalid, stateHolder.uiState.value.error)
    }

    private class FakeAuthRepository(
        private val smsLoginResult: Result<User> = Result.failure(NotImplementedError()),
        private val passwordLoginResult: Result<User> = Result.failure(NotImplementedError()),
        var sendSmsCodeResult: Result<Unit> = Result.success(Unit),
    ) : AuthRepository {
        var sendSmsCodeCalls: Int = 0
            private set
        var lastCaptchaToken: String? = null
            private set

        override suspend fun login(phone: String, password: String): Result<User> =
            passwordLoginResult

        override suspend fun sendSmsCode(phone: String, captchaToken: String?): Result<Unit> {
            sendSmsCodeCalls += 1
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
