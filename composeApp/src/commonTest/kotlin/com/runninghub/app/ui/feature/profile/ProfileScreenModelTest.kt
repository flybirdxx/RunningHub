package com.runninghub.app.ui.feature.profile

import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.core.model.AccountStatus
import com.runninghub.feature.auth.domain.ProfileCredentialRepository
import com.runninghub.feature.auth.domain.UserRepository
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
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bindApiKey delegates credential write through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val screenModel = createScreenModel(profileCredentialRepository)
        advanceUntilIdle()

        screenModel.showApiKeyDialog()
        screenModel.bindApiKey("api-key-1")
        advanceUntilIdle()

        assertEquals("api-key-1", profileCredentialRepository.boundApiKey)
        assertFalse(screenModel.uiState.value.showApiKeyDialog)
    }

    @Test
    fun `bindCookie delegates credential write through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val screenModel = createScreenModel(profileCredentialRepository)
        advanceUntilIdle()

        screenModel.showCookieDialog()
        screenModel.bindCookie("cookie-1")
        advanceUntilIdle()

        assertEquals("cookie-1", profileCredentialRepository.boundCookie)
        assertFalse(screenModel.uiState.value.showCookieDialog)
    }

    @Test
    fun `unbindApiKey clears all creation credentials through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val screenModel = createScreenModel(profileCredentialRepository)
        advanceUntilIdle()

        screenModel.unbindApiKey()
        advanceUntilIdle()

        assertEquals(1, profileCredentialRepository.clearCount)
        assertFalse(screenModel.uiState.value.isLoading)
    }

    private fun createScreenModel(
        profileCredentialRepository: FakeProfileCredentialRepository,
    ): ProfileScreenModel =
        ProfileScreenModel(
            userRepository = FakeUserRepository(),
            profileCredentialRepository = profileCredentialRepository,
            authRepository = FakeAuthRepository(),
        )

    private class FakeProfileCredentialRepository : ProfileCredentialRepository {
        var boundApiKey: String? = null
            private set
        var boundCookie: String? = null
            private set
        var clearCount: Int = 0
            private set

        override suspend fun bindApiKey(key: String) {
            boundApiKey = key
        }

        override suspend fun bindCookie(cookie: String) {
            boundCookie = cookie
        }

        override suspend fun clearCreationCredentials() {
            clearCount += 1
        }
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(phone: String, password: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun sendSmsCode(phone: String): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun smsLogin(phone: String, code: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun logout() = Unit

        override suspend fun isLoggedIn(): Boolean = false

        override suspend fun refreshTokenIfNeeded(): Result<String> =
            Result.failure(NotImplementedError())

        override suspend fun getCurrentAuthToken(): String? = null

        override suspend fun getCurrentUserId(): String? = null
    }

    private class FakeUserRepository : UserRepository {
        override suspend fun getAccountStatus(): Result<AccountStatus> =
            Result.failure(NotImplementedError())

        override suspend fun getUserInfo(userId: String?): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun getUserDetail(userId: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun isFollow(targetUserId: String): Result<Boolean> =
            Result.failure(NotImplementedError())

        override suspend fun followUser(targetUserId: String): Result<Boolean> =
            Result.failure(NotImplementedError())

        override suspend fun unFollowUser(targetUserId: String): Result<Boolean> =
            Result.failure(NotImplementedError())
    }
}
