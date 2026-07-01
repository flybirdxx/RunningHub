package com.runninghub.feature.community.presentation

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaCreationPage
import com.runninghub.feature.community.domain.PlazaCreationReuseSnapshot
import com.runninghub.feature.community.domain.PlazaRepository
import com.runninghub.feature.community.domain.PlazaReuseMediaKind
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaShortPage
import com.runninghub.feature.community.domain.PlazaTag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlazaStateHolderTest {
    @Test
    fun `loadInitialData loads tags and creations`() = runTest {
        val repository = FakePlazaRepository()
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("tag-1"), state.tags.map { it.id })
        assertEquals(listOf("creation-1"), state.creations.map { it.id })
        assertEquals("RECOMMEND", repository.lastSort)
    }

    @Test
    fun `loadInitialData keeps child tags from full tag tree`() = runTest {
        val repository = FakePlazaRepository()
        repository.tags = listOf(
            PlazaTag(id = "parent", name = "图片生成", level = 1, childIds = listOf("child")),
            PlazaTag(id = "child", name = "文生图", level = 2),
            PlazaTag(id = "disabled-child", name = "禁用", level = 2, enable = false),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()

        val tags = stateHolder.uiState.value.tags
        assertEquals(listOf("parent", "child"), tags.map { it.id })
        assertEquals(listOf("child"), tags.first { it.id == "parent" }.childIds)
    }

    @Test
    fun `selectTag expands parent tag to descendant ids for creation request`() = runTest {
        val repository = FakePlazaRepository()
        repository.tags = listOf(
            PlazaTag(id = "parent", name = "视频生成", level = 1, childIds = listOf("child-a", "child-b")),
            PlazaTag(id = "child-a", name = "文生视频", level = 2),
            PlazaTag(id = "child-b", name = "图生视频", level = 2),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()
        stateHolder.selectTag("parent")
        advanceUntilIdle()

        assertEquals(listOf("parent", "child-a", "child-b"), repository.lastTags)
    }

    @Test
    fun `selectSort reloads creations with selected sort`() = runTest {
        val repository = FakePlazaRepository()
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()
        stateHolder.selectSort("LATEST")
        advanceUntilIdle()

        assertEquals("LATEST", stateHolder.uiState.value.sort)
        assertEquals("LATEST", repository.lastSort)
    }

    @Test
    fun `loadMoreCreations appends next real page`() = runTest {
        val repository = FakePlazaRepository()
        repository.pages = mapOf(
            1 to listOf(PlazaCreationCard(id = "creation-1")),
            2 to listOf(PlazaCreationCard(id = "creation-2")),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()
        stateHolder.loadMoreCreations()
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals(listOf("creation-1", "creation-2"), state.creations.map { it.id })
        assertEquals(2, state.currentPage)
        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun `refreshCreations reloads first page with current filters`() = runTest {
        val repository = FakePlazaRepository()
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()
        stateHolder.selectTag("tag-1")
        advanceUntilIdle()
        stateHolder.refreshCreations()
        advanceUntilIdle()

        assertEquals(listOf("tag-1"), repository.lastTags)
        assertEquals(1, stateHolder.uiState.value.currentPage)
    }

    @Test
    fun `loadShorts loads real categories and cards`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortCategories = listOf(PlazaShortCategory(code = "HOT", name = "Hot"))
        repository.shortPages = mapOf(
            1 to listOf(PlazaShortCard(id = "short-1", name = "Short demo")),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadShorts()
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals(listOf("HOT"), state.shortCategories.map { it.code })
        assertEquals(listOf("short-1"), state.shorts.map { it.id })
        assertEquals(1, state.shortPage)
        assertEquals(1, state.shortTotal)
        assertFalse(state.shortHasMore)
    }

    @Test
    fun `loadMoreShorts stops when loaded shorts reach total`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortPages = mapOf(
            1 to listOf(PlazaShortCard(id = "short-1", name = "Short demo")),
        )
        repository.shortTotal = 1
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadShorts()
        advanceUntilIdle()
        stateHolder.loadMoreShorts()
        advanceUntilIdle()

        assertEquals(listOf(1), repository.requestedShortPages)
        assertFalse(stateHolder.uiState.value.shortHasMore)
    }

    @Test
    fun `selectShortCategory reloads shorts with selected category`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortCategories = listOf(PlazaShortCategory(code = "HOT", name = "Hot"))
        repository.shortPages = mapOf(
            1 to listOf(PlazaShortCard(id = "short-hot", name = "Hot short")),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadShorts()
        advanceUntilIdle()
        stateHolder.selectShortCategory("HOT")
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals("HOT", state.selectedShortCategoryCode)
        assertEquals("HOT", repository.lastShortCategoryCode)
        assertEquals(listOf("short-hot"), state.shorts.map { it.id })
    }

    @Test
    fun `creation failure exposes stable presentation error without throwable message`() = runTest {
        val repository = FakePlazaRepository()
        repository.tags = emptyList()
        repository.creationFailure = IllegalStateException("remote plaza msg should not reach ui")
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals(PlazaPresentationError.CreationsLoadFailed, state.error)
        assertEquals(emptyList(), state.tags)
        assertEquals(emptyList(), state.creations)
        assertFalse(state.isLoading)
    }

    @Test
    fun `short failure exposes stable presentation error without throwable message`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortFailure = IllegalStateException("remote short msg should not reach ui")
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadShorts()
        advanceUntilIdle()

        val state = stateHolder.uiState.value
        assertEquals(PlazaPresentationError.ShortsLoadFailed, state.error)
        assertEquals(emptyList(), state.shorts)
    }

    @Test
    fun `creation work card and detail expose reusable source without assuming full params`() = runTest {
        val repository = FakePlazaRepository()
        repository.pages = mapOf(
            1 to listOf(
                PlazaCreationCard(
                    id = "work-1",
                    intro = "cinematic mountain house",
                    ownerName = "Ada",
                    ownerAvatar = "https://example.com/ada.png",
                    mediaUrl = "https://example.com/work.png",
                    mediaType = "IMAGE",
                    useCount = "12",
                    reuseSnapshot = PlazaCreationReuseSnapshot(
                        prompt = "cinematic mountain house",
                        referenceMediaUrl = "https://example.com/work.png",
                        referenceMediaType = PlazaReuseMediaKind.IMAGE,
                    ),
                ),
            ),
        )
        val stateHolder = PlazaStateHolder(repository, this)

        stateHolder.loadInitialData()
        advanceUntilIdle()

        val workCard = stateHolder.uiState.value.workCards.single()
        assertEquals("work-1", workCard.id)
        assertEquals("Ada", workCard.authorName)
        assertEquals(PlazaWorkCardPrimaryAction.UseSame, workCard.primaryAction)
        assertEquals(PlazaWorkPreviewType.Image, workCard.preview.type)
        assertEquals(PlazaReuseParameterStatus.Available, workCard.reuseSummary.prompt.status)
        assertEquals(PlazaReuseParameterStatus.Missing, workCard.reuseSummary.modelTemplate.status)
        assertTrue(workCard.sourceProtected)

        stateHolder.openWorkDetail("work-1")
        val detail = assertNotNull(stateHolder.uiState.value.selectedWorkDetail)
        assertEquals("work-1", detail.id)
        assertEquals("Ada", detail.authorName)
        assertEquals(PlazaReuseParameterStatus.Available, detail.reuseSummary.referenceMedia.status)
        assertEquals(PlazaReuseParameterStatus.Missing, detail.reuseSummary.resolution.status)
        assertEquals(PlazaWorkDetailPrimaryAction.UseSameGenerate, detail.primaryAction)
        assertTrue(detail.sourceProtected)
    }

    private class FakePlazaRepository : PlazaRepository {
        var lastSort: String? = null
        var lastTags: List<String> = emptyList()
        val requestedPages = mutableListOf<Int>()
        var tags: List<PlazaTag> = listOf(PlazaTag(id = "tag-1", name = "Images", level = 1))
        var pages: Map<Int, List<PlazaCreationCard>> = mapOf(
            1 to listOf(PlazaCreationCard(id = "creation-1", intro = "Demo")),
        )
        var shortCategories: List<PlazaShortCategory> = emptyList()
        var shortPages: Map<Int, List<PlazaShortCard>> = emptyMap()
        var shortTotal: Int = 1
        val requestedShortPages = mutableListOf<Int>()
        var lastShortCategoryCode: String? = null
        var creationFailure: Throwable? = null
        var shortFailure: Throwable? = null

        override suspend fun getTags(): Result<List<PlazaTag>> =
            Result.success(tags)

        override suspend fun listCreations(
            page: Int,
            size: Int,
            sort: String,
            tags: List<String>,
        ): Result<PlazaCreationPage> {
            lastSort = sort
            lastTags = tags
            requestedPages += page
            creationFailure?.let { return Result.failure(it) }
            val items = pages[page].orEmpty()
            return Result.success(
                PlazaCreationPage(
                    page = page,
                    total = pages.values.sumOf { it.size },
                    items = items,
                )
            )
        }

        override suspend fun listShortCategories(): Result<List<PlazaShortCategory>> =
            Result.success(shortCategories)

        override suspend fun listShorts(page: Int, size: Int, categoryCode: String?): Result<PlazaShortPage> {
            lastShortCategoryCode = categoryCode
            requestedShortPages += page
            shortFailure?.let { return Result.failure(it) }
            return Result.success(
                PlazaShortPage(
                    page = page,
                    total = shortTotal,
                    items = shortPages[page].orEmpty(),
                )
            )
        }
    }
}
