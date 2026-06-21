package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.PageData
import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.domain.CatalogTagRange
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class SearchStateHolderTest {
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
    fun `query change waits for debounce before searching latest keyword`() = runTest(dispatcher) {
        val repository = FakeSearchCatalogRepository()
        val stateHolder = SearchStateHolder(repository, this)

        stateHolder.onQueryChange("cat")
        advanceTimeBy(349)
        runCurrent()

        assertEquals(emptyList(), repository.searchCalls)

        stateHolder.onQueryChange("castle")
        advanceTimeBy(350)
        advanceUntilIdle()

        assertEquals(listOf(SearchCall("castle", page = 1, size = 10)), repository.searchCalls)
        assertEquals("castle", stateHolder.uiState.value.query)
        assertEquals(listOf("castle-result"), stateHolder.uiState.value.results.map { it.id })
    }

    @Test
    fun `clear search keeps already loaded hot tags`() = runTest(dispatcher) {
        val repository = FakeSearchCatalogRepository()
        val stateHolder = SearchStateHolder(repository, this)

        stateHolder.loadHotTags()
        advanceUntilIdle()
        stateHolder.search("robot")
        advanceUntilIdle()
        stateHolder.clearSearch()

        assertEquals(listOf("child-a", "root-a", "child-b", "root-b"), stateHolder.uiState.value.hotTags.map { it.id })
        assertEquals("", stateHolder.uiState.value.query)
        assertEquals(emptyList(), stateHolder.uiState.value.results)
        assertEquals(true, stateHolder.uiState.value.hasMore)
    }

    @Test
    fun `load more appends results by stable id`() = runTest(dispatcher) {
        val repository = FakeSearchCatalogRepository()
        repository.enqueueSearchResult(Result.success(searchPage(app("first"), app("shared"), hasNext = true)))
        repository.enqueueSearchResult(Result.success(searchPage(app("shared"), app("second"), hasNext = false, current = 2)))
        val stateHolder = SearchStateHolder(repository, this)

        stateHolder.search("space")
        advanceUntilIdle()
        stateHolder.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("first", "shared", "second"), stateHolder.uiState.value.results.map { it.id })
        assertEquals(2, stateHolder.uiState.value.currentPage)
        assertEquals(false, stateHolder.uiState.value.hasMore)
    }

    @Test
    fun `delayed old search response does not overwrite latest keyword`() = runTest(dispatcher) {
        val repository = FakeSearchCatalogRepository()
        val oldResponse = repository.enqueueSearchResponse()
        val latestResponse = repository.enqueueSearchResponse()
        val stateHolder = SearchStateHolder(repository, this)

        stateHolder.search("old")
        runCurrent()
        stateHolder.search("latest")
        runCurrent()

        latestResponse.complete(Result.success(searchPage(app("latest-result"))))
        advanceUntilIdle()
        oldResponse.complete(Result.success(searchPage(app("old-result"))))
        advanceUntilIdle()

        assertEquals("latest", stateHolder.uiState.value.query)
        assertEquals(listOf("latest-result"), stateHolder.uiState.value.results.map { it.id })
    }

    private class FakeSearchCatalogRepository : WebAppCatalogRepository {
        val searchCalls = mutableListOf<SearchCall>()
        private val searchResponses = ArrayDeque<CompletableDeferred<Result<PageData<WebApp>>>>()

        fun enqueueSearchResponse(): CompletableDeferred<Result<PageData<WebApp>>> =
            CompletableDeferred<Result<PageData<WebApp>>>().also { searchResponses.addLast(it) }

        fun enqueueSearchResult(result: Result<PageData<WebApp>>) {
            enqueueSearchResponse().complete(result)
        }

        override suspend fun getAppList(query: CatalogQuery): Result<PageData<WebApp>> =
            Result.success(searchPage())

        override suspend fun getCarefullyChosenList(): Result<List<WebApp>> = Result.success(emptyList())

        override suspend fun getCustomMadeWebappList(tags: List<String>): Result<List<WebApp>> =
            Result.success(emptyList())

        override suspend fun getUserAppList(
            userId: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> = Result.success(searchPage())

        override suspend fun getTagTree(range: CatalogTagRange): Result<List<Tag>> =
            Result.success(
                listOf(
                    tag("root-a", "child-a"),
                    tag("root-b", "child-b"),
                )
            )

        override suspend fun getAppDetail(appId: String): Result<AppDetail> =
            Result.failure(UnsupportedOperationException("Search tests do not use detail"))

        override suspend fun searchApps(
            keyword: String,
            pageNum: Int,
            pageSize: Int,
        ): Result<PageData<WebApp>> {
            searchCalls += SearchCall(keyword = keyword, page = pageNum, size = pageSize)
            return searchResponses.removeFirstOrNull()?.await()
                ?: Result.success(searchPage(app("$keyword-result"), current = pageNum))
        }
    }

    private data class SearchCall(
        val keyword: String,
        val page: Int,
        val size: Int,
    )
}

private fun searchPage(
    vararg apps: WebApp,
    hasNext: Boolean = true,
    current: Int = 1,
): PageData<WebApp> = PageData(
    records = apps.toList(),
    total = apps.size,
    size = 10,
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
