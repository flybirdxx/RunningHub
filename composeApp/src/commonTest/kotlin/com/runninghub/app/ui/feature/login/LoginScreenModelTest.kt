package com.runninghub.app.ui.feature.login

import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.SmsError
import com.runninghub.shared.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `wrong sms code clears input and maps presentation message`() = runTest {
        val screenModel = LoginScreenModel(
            FakeAuthRepository(smsLoginResult = Result.failure(SmsError.WrongCode()))
        )

        screenModel.onPhoneChanged("13800138000")
        screenModel.onSmsCodeChanged("1234")
        screenModel.smsLogin()
        advanceUntilIdle()

        assertEquals("", screenModel.uiState.value.smsCode)
        assertEquals("验证码错误，请重新输入", screenModel.uiState.value.errorMessage)
    }

    @Test
    fun `network sms login failure preserves input for retry`() = runTest {
        val screenModel = LoginScreenModel(
            FakeAuthRepository(smsLoginResult = Result.failure(SmsError.Network()))
        )

        screenModel.onPhoneChanged("13800138000")
        screenModel.onSmsCodeChanged("1234")
        screenModel.smsLogin()
        advanceUntilIdle()

        assertEquals("1234", screenModel.uiState.value.smsCode)
        assertEquals("网络连接失败，请检查网络后重试", screenModel.uiState.value.errorMessage)
    }

    @Test
    fun `network password login failure maps presentation message`() = runTest {
        val screenModel = LoginScreenModel(
            FakeAuthRepository(passwordLoginResult = Result.failure(AuthError.Network()))
        )

        screenModel.onPhoneChanged("13800138000")
        screenModel.onPasswordChanged("password")
        screenModel.pwdLogin()
        advanceUntilIdle()

        assertEquals("网络连接失败，请检查网络后重试", screenModel.uiState.value.errorMessage)
    }

    private class FakeAuthRepository(
        private val smsLoginResult: Result<User> = Result.failure(NotImplementedError()),
        private val passwordLoginResult: Result<User> = Result.failure(NotImplementedError()),
    ) : AuthRepository {
        override suspend fun login(phone: String, password: String): Result<User> =
            passwordLoginResult

        override suspend fun sendSmsCode(phone: String): Result<Unit> =
            Result.success(Unit)

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
