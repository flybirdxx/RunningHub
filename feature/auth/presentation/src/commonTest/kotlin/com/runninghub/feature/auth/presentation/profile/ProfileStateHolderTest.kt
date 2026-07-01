package com.runninghub.feature.auth.presentation.profile

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.MemberInfo
import com.runninghub.core.model.User
import com.runninghub.core.model.WalletInfo
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileStateHolderTest {
    @Test
    fun `asset center separates rhb points and wallet balance without inferred generation capacity`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = true, currentUserId = "user-1"),
            userRepository = FakeUserRepository(
                userInfoResult = Result.success(
                    testUser(
                        totalCoin = "12880",
                        walletInfo = WalletInfo(
                            balance = 248.6,
                            currency = "CNY",
                            currencySymbol = "¥",
                        ),
                    ),
                ),
                accountStatusResult = Result.success(
                    AccountStatus(
                        remainCoins = "12000",
                        currentTaskCounts = "0",
                        remainMoney = "248.60",
                        currency = "CNY",
                        apiType = "member",
                    ),
                ),
            ),
            coroutineScope = this,
        )
        advanceUntilIdle()

        val wallet = stateHolder.uiState.value.assetCenter.wallet

        assertTrue(stateHolder.uiState.value.assetCenter.visible)
        assertEquals("12000", wallet.rhbPoints)
        assertEquals("248.60", wallet.walletBalance)
        assertEquals("CNY", wallet.walletCurrency)
        assertEquals(ProfileWalletRisk.None, wallet.risk)
    }

    @Test
    fun `asset center does not infer insufficient balance from zero account fields`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = true, currentUserId = "user-1"),
            userRepository = FakeUserRepository(
                userInfoResult = Result.success(
                    testUser(
                        totalCoin = "0",
                        walletInfo = WalletInfo(balance = 0.0, currency = "CNY", currencySymbol = "¥"),
                    ),
                ),
                accountStatusResult = Result.success(
                    AccountStatus(
                        remainCoins = "0",
                        currentTaskCounts = "0",
                        remainMoney = "0",
                        currency = "CNY",
                        apiType = "member",
                    ),
                ),
            ),
            coroutineScope = this,
        )
        advanceUntilIdle()

        val wallet = stateHolder.uiState.value.assetCenter.wallet

        assertEquals("0", wallet.rhbPoints)
        assertEquals("0", wallet.walletBalance)
        assertEquals(ProfileWalletRisk.None, wallet.risk)
    }

    @Test
    fun `asset center keeps logged out profile free of wallet and member data`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = false),
            coroutineScope = this,
        )
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.assetCenter.visible)
        assertEquals(ProfileAssetLoadState.Hidden, stateHolder.uiState.value.assetCenter.loadState)
    }

    @Test
    fun `membership center exposes expired status without inventing benefits`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = true, currentUserId = "user-1"),
            userRepository = FakeUserRepository(
                userInfoResult = Result.success(
                    testUser(
                        memberInfo = MemberInfo(
                            memberName = "RunningHub Pro",
                            memberExpiredTime = "2026-06-01 00:00:00",
                            userType = "PRO",
                            memberRemainingDays = "0",
                            expired = true,
                        ),
                    ),
                ),
            ),
            coroutineScope = this,
        )
        advanceUntilIdle()

        val membership = stateHolder.uiState.value.assetCenter.membership

        assertEquals(ProfileMembershipStatus.Expired, membership.status)
        assertEquals("RunningHub Pro", membership.levelName)
    }

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

    @Test
    fun `refreshUserData exposes stable error when user info load fails`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(isLoggedIn = true, currentUserId = "user-1"),
            userRepository = FakeUserRepository(
                userInfoResult = Result.failure(IllegalStateException("remote profile failed")),
            ),
            coroutineScope = this,
        )

        stateHolder.refreshUserData()
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.isLoading)
        assertEquals(true, stateHolder.uiState.value.isLoggedIn)
        assertEquals(ProfileError.LoadUserFailed, stateHolder.uiState.value.error)
    }

    @Test
    fun `refreshUserData exposes stable network error without leaking exception message`() = runTest {
        val stateHolder = createStateHolder(
            profileCredentialRepository = FakeProfileCredentialRepository(),
            authRepository = FakeAuthRepository(
                isLoggedIn = true,
                currentUserIdFailure = IllegalStateException("token diagnostics"),
            ),
            coroutineScope = this,
        )

        stateHolder.refreshUserData()
        advanceUntilIdle()

        assertFalse(stateHolder.uiState.value.isLoading)
        assertEquals(ProfileError.NetworkFailed, stateHolder.uiState.value.error)
    }

    private fun createStateHolder(
        profileCredentialRepository: FakeProfileCredentialRepository,
        authRepository: FakeAuthRepository = FakeAuthRepository(),
        userRepository: FakeUserRepository = FakeUserRepository(),
        coroutineScope: CoroutineScope,
    ): ProfileStateHolder =
        ProfileStateHolder(
            userRepository = userRepository,
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

    private class FakeAuthRepository(
        private val isLoggedIn: Boolean = false,
        private val currentUserId: String? = null,
        private val currentUserIdFailure: Throwable? = null,
    ) : AuthRepository {
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

        override suspend fun isLoggedIn(): Boolean = isLoggedIn

        override suspend fun refreshTokenIfNeeded(): Result<String> =
            Result.failure(NotImplementedError())

        override suspend fun getCurrentAuthToken(): String? = null

        override suspend fun getCurrentUserId(): String? {
            currentUserIdFailure?.let { throw it }
            return currentUserId
        }
    }

    private class FakeUserRepository(
        private val userInfoResult: Result<User> = Result.failure(NotImplementedError()),
        private val accountStatusResult: Result<AccountStatus> = Result.failure(NotImplementedError()),
    ) : UserRepository {
        override suspend fun getAccountStatus(): Result<AccountStatus> =
            accountStatusResult

        override suspend fun getUserInfo(userId: String?): Result<User> =
            userInfoResult

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

private fun testUser(
    totalCoin: String? = "0",
    memberInfo: MemberInfo? = null,
    walletInfo: WalletInfo? = null,
): User =
    User(
        id = "user-1",
        nickName = "Creator",
        headIcon = null,
        mobile = "13800138000",
        totalCoin = totalCoin,
        memberInfo = memberInfo,
        walletInfo = walletInfo,
        apiKey = null,
        apiType = "runninghub",
        introduce = null,
        fanCount = "0",
        followCount = "0",
        likeCount = "0",
        collectCount = "0",
    )
