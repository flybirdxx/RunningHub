package com.runninghub.feature.auth.presentation.profile

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthRepository
import com.runninghub.feature.auth.domain.ProfileCredentialRepository
import com.runninghub.feature.auth.domain.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileStateHolderTest {
    @Test
    fun `bindApiKey delegates credential write through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val stateHolder = createStateHolder(profileCredentialRepository, coroutineScope = this)
        advanceUntilIdle()

        stateHolder.showApiKeyDialog()
        stateHolder.bindApiKey("api-key-1")
        advanceUntilIdle()

        assertEquals("api-key-1", profileCredentialRepository.boundApiKey)
        assertFalse(stateHolder.uiState.value.showApiKeyDialog)
    }

    @Test
    fun `bindCookie delegates credential write through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val stateHolder = createStateHolder(profileCredentialRepository, coroutineScope = this)
        advanceUntilIdle()

        stateHolder.showCookieDialog()
        stateHolder.bindCookie("cookie-1")
        advanceUntilIdle()

        assertEquals("cookie-1", profileCredentialRepository.boundCookie)
        assertFalse(stateHolder.uiState.value.showCookieDialog)
    }

    @Test
    fun `unbindApiKey clears all creation credentials through domain repository`() = runTest {
        val profileCredentialRepository = FakeProfileCredentialRepository()
        val stateHolder = createStateHolder(profileCredentialRepository, coroutineScope = this)
        advanceUntilIdle()

        stateHolder.unbindApiKey()
        advanceUntilIdle()

        assertEquals(1, profileCredentialRepository.clearCount)
        assertFalse(stateHolder.uiState.value.isLoading)
    }

    @Test
    fun `logout delegates session cleanup and clears profile state`() = runTest {
        val authRepository = FakeAuthRepository()
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = authRepository,
            coroutineScope = this,
        )
        advanceUntilIdle()

        stateHolder.logout()
        advanceUntilIdle()

        assertEquals(1, authRepository.logoutCount)
        assertFalse(stateHolder.uiState.value.isLoading)
        assertFalse(stateHolder.uiState.value.isLoggedIn)
        assertEquals(null, stateHolder.uiState.value.user)
    }

    private fun createStateHolder(
        profileCredentialRepository: FakeProfileCredentialRepository,
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        coroutineScope: CoroutineScope,
    ): ProfileStateHolder =
        ProfileStateHolder(
            userRepository = FakeUserRepository(),
            profileCredentialRepository = profileCredentialRepository,
            authRepository = authRepository,
            coroutineScope = coroutineScope,
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
        var logoutCount: Int = 0
            private set

        override suspend fun login(phone: String, password: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun sendSmsCode(phone: String, captchaToken: String?): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun smsLogin(phone: String, code: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun logout() {
            logoutCount += 1
        }

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
