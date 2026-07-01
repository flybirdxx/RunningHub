package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.PageData
import com.runninghub.core.model.Tag
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogError
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.domain.CatalogTagRange
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryStateHolderTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `category index zero means all and first real category starts at one`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)

        screenModel.loadInitialData()
        advanceUntilIdle()
        screenModel.selectCategory(1)
        advanceUntilIdle()
        screenModel.selectCategory(0)
        advanceUntilIdle()

        assertEquals(emptyList(), repository.appListCalls[0].tags)
        assertEquals(listOf("child-art"), repository.appListCalls[1].tags)
        assertEquals(emptyList(), repository.appListCalls[2].tags)
    }

    @Test
    fun `delayed category response does not overwrite latest category result`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)
        screenModel.loadInitialData()
        advanceUntilIdle()

        val artResponse = repository.enqueueAppListResponse()
        val videoResponse = repository.enqueueAppListResponse()

        screenModel.selectCategory(1)
        runCurrent()
        screenModel.selectCategory(2)
        runCurrent()

        videoResponse.complete(Result.success(page(app("video-latest"))))
        advanceUntilIdle()
        artResponse.complete(Result.success(page(app("art-stale"))))
        advanceUntilIdle()

        assertEquals(listOf("video-latest"), screenModel.uiState.value.apps.map { it.id })
        assertEquals(2, screenModel.uiState.value.selectedCategoryIndex)
    }

    @Test
    fun `delayed sort response does not overwrite latest sort result`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)
        screenModel.loadInitialData()
        advanceUntilIdle()

        val reputationResponse = repository.enqueueAppListResponse()
        val newestResponse = repository.enqueueAppListResponse()

        screenModel.selectSort(CatalogSort.REPUTATION)
        runCurrent()
        screenModel.selectSort(CatalogSort.NEWEST)
        runCurrent()

        newestResponse.complete(Result.success(page(app("newest-latest"))))
        advanceUntilIdle()
        reputationResponse.complete(Result.success(page(app("reputation-stale"))))
        advanceUntilIdle()

        assertEquals(listOf("newest-latest"), screenModel.uiState.value.apps.map { it.id })
        assertEquals(CatalogSort.NEWEST, screenModel.uiState.value.selectedSort)
    }

    @Test
    fun `load more appends by stable id and failure does not advance page`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)
        screenModel.loadInitialData()
        advanceUntilIdle()

        repository.enqueueAppListResult(Result.success(page(app("initial"), app("second"), hasNext = true)))
        screenModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("initial", "second"), screenModel.uiState.value.apps.map { it.id })
        assertEquals(2, screenModel.uiState.value.currentPage)

        repository.enqueueAppListResult(Result.failure(IllegalStateException("network")))
        screenModel.loadMore()
        advanceUntilIdle()

        assertEquals(2, screenModel.uiState.value.currentPage)
        assertEquals(listOf("initial", "second"), screenModel.uiState.value.apps.map { it.id })
    }

    @Test
    fun `refresh preserves selected filter`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)
        screenModel.loadInitialData()
        advanceUntilIdle()
        screenModel.selectCategory(1)
        advanceUntilIdle()
        screenModel.selectSort(CatalogSort.HOTTEST)
        advanceUntilIdle()

        screenModel.refresh()
        advanceUntilIdle()

        val refreshCall = repository.appListCalls.last()
        assertEquals(1, screenModel.uiState.value.selectedCategoryIndex)
        assertEquals(CatalogSort.HOTTEST, screenModel.uiState.value.selectedSort)
        assertEquals(listOf("child-art"), refreshCall.tags)
        assertEquals(CatalogSort.HOTTEST, refreshCall.sort)
    }

    @Test
    fun `initial load exposes carefully chosen apps as banners`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        repository.enqueueCarefullyChosenResult(
            Result.success(
                listOf(
                    app("featured").copy(title = "Featured App"),
                    app("missing-title").copy(title = ""),
                ),
            ),
        )
        val screenModel = DiscoveryStateHolder(repository, this)

        screenModel.loadInitialData()
        advanceUntilIdle()

        assertEquals(listOf("featured"), screenModel.uiState.value.banners.map { it.id })
    }

    @Test
    fun `catalog server message is mapped to stable presentation error`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)
        repository.enqueueAppListResult(Result.failure(CatalogError.Remote(code = 500, serverMessage = "raw server msg")))

        screenModel.loadInitialData()
        advanceUntilIdle()

        assertEquals(CatalogPresentationError.ServiceUnavailable, screenModel.uiState.value.error)
    }

    @Test
    fun `app card model exposes creation path slots`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        repository.enqueueAppListResult(
            Result.success(
                page(
                    app("video-template").copy(
                        title = "Video Template",
                        description = "Generate a short product clip",
                        coverUrl = "https://cdn.example.com/video-cover.png",
                        coverMediaType = CoverMediaType.VIDEO,
                        tags = listOf(TagSimple(id = "video", name = "视频生成")),
                        useCount = "42",
                    ),
                    hasNext = false,
                ),
            ),
        )
        val screenModel = DiscoveryStateHolder(repository, this)

        screenModel.loadInitialData()
        advanceUntilIdle()

        val card = screenModel.uiState.value.appCards.single()
        assertEquals("video-template", card.id)
        assertEquals("Video Template", card.templateName)
        assertEquals(DiscoveryAppCapability.VIDEO, card.capability)
        assertEquals("https://cdn.example.com/video-cover.png", card.preview.url)
        assertEquals(DiscoveryAppPreviewType.VIDEO, card.preview.type)
        assertEquals(DiscoveryAppEstimatedCostKind.UNKNOWN, card.estimatedCost.kind)
        assertEquals(DiscoveryAppCardPrimaryAction.GENERATE, card.primaryAction)
        assertEquals(DiscoveryAppCardMetricKind.USE_COUNT, card.supportingMetric?.kind)
    }

    @Test
    fun `search result card model keeps empty search state separate`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        repository.enqueueSearchResponse().complete(Result.success(page(hasNext = false)))
        val screenModel = DiscoveryStateHolder(repository, this)

        screenModel.searchSubmit("missing")
        advanceUntilIdle()

        assertEquals(emptyList(), screenModel.uiState.value.searchResultCards)
        assertEquals(false, screenModel.uiState.value.isSearching)
        assertEquals(null, screenModel.uiState.value.searchError)
    }

    @Test
    fun `delayed search response does not overwrite latest keyword result`() = runTest(dispatcher) {
        val repository = FakeWebAppCatalogRepository()
        val screenModel = DiscoveryStateHolder(repository, this)

        val oldSearch = repository.enqueueSearchResponse()
        val newSearch = repository.enqueueSearchResponse()

        screenModel.searchSubmit("old")
        runCurrent()
        screenModel.searchSubmit("new")
        runCurrent()

        newSearch.complete(Result.success(page(app("new-result"))))
        advanceUntilIdle()
        oldSearch.complete(Result.success(page(app("old-result"))))
        advanceUntilIdle()

        assertEquals("new", screenModel.uiState.value.searchQuery)
        assertEquals(listOf("new-result"), screenModel.uiState.value.searchResults.map { it.id })
    }

    private class FakeWebAppCatalogRepository : WebAppCatalogRepository {
        val appListCalls = mutableListOf<AppListCall>()
        private val appListResponses = ArrayDeque<CompletableDeferred<Result<PageData<WebApp>>>>()
        private val carefullyChosenResponses = ArrayDeque<CompletableDeferred<Result<List<WebApp>>>>()
        private val searchResponses = ArrayDeque<CompletableDeferred<Result<PageData<WebApp>>>>()

        fun enqueueAppListResponse(): CompletableDeferred<Result<PageData<WebApp>>> =
            CompletableDeferred<Result<PageData<WebApp>>>().also { appListResponses.addLast(it) }

        fun enqueueSearchResponse(): CompletableDeferred<Result<PageData<WebApp>>> =
            CompletableDeferred<Result<PageData<WebApp>>>().also { searchResponses.addLast(it) }

        fun enqueueAppListResult(result: Result<PageData<WebApp>>) {
            enqueueAppListResponse().complete(result)
        }

        fun enqueueCarefullyChosenResult(result: Result<List<WebApp>>) {
            CompletableDeferred<Result<List<WebApp>>>().also {
                it.complete(result)
                carefullyChosenResponses.addLast(it)
            }
        }

        override suspend fun getAppList(
            query: CatalogQuery,
        ): Result<PageData<WebApp>> {
            appListCalls += AppListCall(pageNum = query.pageNum, tags = query.tagIds, sort = query.sort)
            return appListResponses.removeFirstOrNull()?.await()
                ?: Result.success(page(app("initial"), hasNext = true, current = query.pageNum))
        }

        override suspend fun getCarefullyChosenList(): Result<List<WebApp>> =
            carefullyChosenResponses.removeFirstOrNull()?.await() ?: Result.success(emptyList())

        override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> =
            Result.success(emptyList())

        override suspend fun getUserAppList(
            userId: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> = Result.success(page())

        override suspend fun getTagTree(range: CatalogTagRange): Result<List<Tag>> = Result.success(
            listOf(
                tag("art", childId = "child-art"),
                tag("video", childId = "child-video"),
            )
        )

        override suspend fun getAppDetail(appId: String): Result<AppDetail> =
            Result.failure(UnsupportedOperationException("detail is not used by discovery state tests"))

        override suspend fun searchApps(
            keyword: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> =
            searchResponses.removeFirstOrNull()?.await()
                ?: Result.success(page(app("$keyword-result"), current = pageNum))
    }

    private data class AppListCall(
        val pageNum: Int,
        val tags: List<String>,
        val sort: CatalogSort?,
    )
}

private fun page(
    vararg apps: WebApp,
    hasNext: Boolean = true,
    current: Int = 1,
): PageData<WebApp> = PageData(
    records = apps.toList(),
    total = apps.size,
    size = 30,
    current = current,
    hasNext = hasNext,
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
    author = Author(
        id = "author",
        name = "Author",
        avatar = null,
        intro = null,
        followCount = "0",
        fansCount = "0",
        likeCount = "0",
        collectCount = "0",
        bgImage = null,
    ),
    tags = emptyList(),
    likeCount = "0",
    collectCount = "0",
    useCount = "0",
    pv = "0",
)

private fun tag(id: String, childId: String): Tag = Tag(
    id = id,
    name = id,
    level = 1,
    parentId = null,
    rang = "WEBAPP",
    enable = true,
    childTags = listOf(
        Tag(
            id = childId,
            name = childId,
            level = 2,
            parentId = id,
            rang = "WEBAPP",
            enable = true,
            childTags = null,
        )
    ),
)
