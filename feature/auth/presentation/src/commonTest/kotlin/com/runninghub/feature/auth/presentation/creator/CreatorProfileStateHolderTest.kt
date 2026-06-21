package com.runninghub.feature.auth.presentation.creator

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.PageData
import com.runninghub.core.model.Tag
import com.runninghub.core.model.User
import com.runninghub.core.model.WebApp
import com.runninghub.feature.auth.domain.UserRepository
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogTagRange
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreatorProfileStateHolderTest {
    @Test
    fun `loadProfile combines user follow state and published apps`() = runTest {
        val userRepository = FakeUserRepository(
            userDetailResult = Result.success(user(id = "creator-1", fanCount = "2")),
            followResult = Result.success(true),
        )
        val catalogRepository = FakeCatalogRepository(
            appsResult = Result.success(appPage(app("app-1"), app("app-2"))),
        )
        val stateHolder = createStateHolder(userRepository, catalogRepository, this)

        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertFalse(state.isLoading)
        assertEquals("creator-1", state.user?.id)
        assertTrue(state.isFollowing)
        assertEquals(listOf("app-1", "app-2"), state.apps.map { it.id })
        assertEquals(null, state.error)
    }

    @Test
    fun `loadProfile skips duplicate request for already loaded creator`() = runTest {
        val userRepository = FakeUserRepository(
            userDetailResult = Result.success(user(id = "creator-1")),
            followResult = Result.success(false),
        )
        val catalogRepository = FakeCatalogRepository(
            appsResult = Result.success(appPage(app("app-1"))),
        )
        val stateHolder = createStateHolder(userRepository, catalogRepository, this)

        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()
        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()

        assertEquals(listOf("creator-1"), userRepository.userDetailCalls)
        assertEquals(listOf("creator-1"), userRepository.followCalls)
        assertEquals(listOf(UserAppCall("creator-1", page = 1, size = 20)), catalogRepository.userAppCalls)
    }

    @Test
    fun `profile load failure still keeps successful follow and apps results`() = runTest {
        val userRepository = FakeUserRepository(
            userDetailResult = Result.failure(IllegalStateException("profile failed")),
            followResult = Result.success(true),
        )
        val catalogRepository = FakeCatalogRepository(
            appsResult = Result.success(appPage(app("app-1"))),
        )
        val stateHolder = createStateHolder(userRepository, catalogRepository, this)

        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.user)
        assertTrue(state.isFollowing)
        assertEquals(listOf("app-1"), state.apps.map { it.id })
        assertEquals(CreatorProfileError.LoadFailed, state.error)
    }

    @Test
    fun `toggleFollow follows current creator and increments local fan count`() = runTest {
        val userRepository = FakeUserRepository(
            userDetailResult = Result.success(user(id = "creator-1", fanCount = "2")),
            followResult = Result.success(false),
            followActionResult = Result.success(true),
        )
        val stateHolder = createStateHolder(
            userRepository = userRepository,
            catalogRepository = FakeCatalogRepository(appsResult = Result.success(appPage())),
            coroutineScope = this,
        )

        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()
        stateHolder.toggleFollow()
        advanceUntilIdle()

        assertEquals(listOf("creator-1"), userRepository.followUserCalls)
        assertEquals(true, stateHolder.uiState.value.isFollowing)
        assertEquals("3", stateHolder.uiState.value.user?.fanCount)
    }

    @Test
    fun `toggleFollow unfollows current creator and does not produce negative fan count`() = runTest {
        val userRepository = FakeUserRepository(
            userDetailResult = Result.success(user(id = "creator-1", fanCount = "0")),
            followResult = Result.success(true),
            unfollowActionResult = Result.success(true),
        )
        val stateHolder = createStateHolder(
            userRepository = userRepository,
            catalogRepository = FakeCatalogRepository(appsResult = Result.success(appPage())),
            coroutineScope = this,
        )

        stateHolder.loadProfile("creator-1")
        advanceUntilIdle()
        stateHolder.toggleFollow()
        advanceUntilIdle()

        assertEquals(listOf("creator-1"), userRepository.unfollowUserCalls)
        assertEquals(false, stateHolder.uiState.value.isFollowing)
        assertEquals("0", stateHolder.uiState.value.user?.fanCount)
    }

    private fun createStateHolder(
        userRepository: FakeUserRepository,
        catalogRepository: FakeCatalogRepository,
        coroutineScope: CoroutineScope,
    ): CreatorProfileStateHolder =
        CreatorProfileStateHolder(
            userRepository = userRepository,
            webAppRepository = catalogRepository,
            coroutineScope = coroutineScope,
        )

    private class FakeUserRepository(
        private val userDetailResult: Result<User>,
        private val followResult: Result<Boolean>,
        private val followActionResult: Result<Boolean> = Result.success(true),
        private val unfollowActionResult: Result<Boolean> = Result.success(true),
    ) : UserRepository {
        val userDetailCalls = mutableListOf<String>()
        val followCalls = mutableListOf<String>()
        val followUserCalls = mutableListOf<String>()
        val unfollowUserCalls = mutableListOf<String>()

        override suspend fun getAccountStatus(): Result<AccountStatus> =
            Result.failure(NotImplementedError())

        override suspend fun getUserInfo(userId: String?): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun getUserDetail(userId: String): Result<User> {
            userDetailCalls += userId
            return userDetailResult
        }

        override suspend fun isFollow(targetUserId: String): Result<Boolean> {
            followCalls += targetUserId
            return followResult
        }

        override suspend fun followUser(targetUserId: String): Result<Boolean> {
            followUserCalls += targetUserId
            return followActionResult
        }

        override suspend fun unFollowUser(targetUserId: String): Result<Boolean> {
            unfollowUserCalls += targetUserId
            return unfollowActionResult
        }
    }

    private class FakeCatalogRepository(
        private val appsResult: Result<PageData<WebApp>>,
    ) : WebAppCatalogRepository {
        val userAppCalls = mutableListOf<UserAppCall>()

        override suspend fun getAppList(query: CatalogQuery): Result<PageData<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getCarefullyChosenList(): Result<List<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> =
            Result.failure(NotImplementedError())

        override suspend fun getUserAppList(
            userId: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> {
            userAppCalls += UserAppCall(userId, pageNum, pageSize)
            return appsResult
        }

        override suspend fun getTagTree(range: CatalogTagRange): Result<List<Tag>> =
            Result.failure(NotImplementedError())

        override suspend fun getAppDetail(appId: String): Result<AppDetail> =
            Result.failure(NotImplementedError())

        override suspend fun searchApps(
            keyword: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> = Result.failure(NotImplementedError())
    }

    private data class UserAppCall(
        val userId: String,
        val page: Int,
        val size: Int,
    )
}

private fun user(
    id: String,
    fanCount: String = "0",
): User = User(
    id = id,
    nickName = "Creator $id",
    headIcon = null,
    mobile = null,
    totalCoin = null,
    memberInfo = null,
    walletInfo = null,
    apiKey = null,
    apiType = null,
    introduce = null,
    fanCount = fanCount,
    followCount = "0",
    likeCount = "0",
    collectCount = "0",
)

private fun app(id: String): WebApp = WebApp(
    id = id,
    title = "App $id",
    description = null,
    thumbnailUrl = null,
    coverUrl = null,
    coverMediaType = CoverMediaType.IMAGE,
    coverWidth = null,
    coverHeight = null,
    author = null,
    tags = emptyList(),
    likeCount = "0",
    collectCount = "0",
    useCount = "0",
    pv = "0",
)

private fun appPage(
    vararg apps: WebApp,
): PageData<WebApp> = PageData(
    records = apps.toList(),
    total = apps.size,
    size = 20,
    current = 1,
    hasNext = false,
)
