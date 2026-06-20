package com.runninghub.app.ui.feature.plaza

import com.runninghub.shared.domain.model.PlazaCreationCard
import com.runninghub.shared.domain.model.PlazaCreationPage
import com.runninghub.shared.domain.model.PlazaShortCard
import com.runninghub.shared.domain.model.PlazaShortCategory
import com.runninghub.shared.domain.model.PlazaTag
import com.runninghub.shared.domain.repository.PlazaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class PlazaScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadInitialData loads tags and creations`() = runTest {
        val repository = FakePlazaRepository()
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadInitialData()

        val state = screenModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(listOf("tag-1"), state.tags.map { it.id })
        assertEquals(listOf("creation-1"), state.creations.map { it.id })
        assertEquals("RECOMMEND", repository.lastSort)
    }

    @Test
    fun `selectSort reloads creations with selected sort`() = runTest {
        val repository = FakePlazaRepository()
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadInitialData()
        screenModel.selectSort("LATEST")

        assertEquals("LATEST", screenModel.uiState.value.sort)
        assertEquals("LATEST", repository.lastSort)
    }

    @Test
    fun `loadMoreCreations appends next real page`() = runTest {
        val repository = FakePlazaRepository()
        repository.pages = mapOf(
            1 to listOf(PlazaCreationCard(id = "creation-1")),
            2 to listOf(PlazaCreationCard(id = "creation-2")),
        )
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadInitialData()
        screenModel.loadMoreCreations()

        val state = screenModel.uiState.value
        assertEquals(listOf("creation-1", "creation-2"), state.creations.map { it.id })
        assertEquals(2, state.currentPage)
        assertEquals(listOf(1, 2), repository.requestedPages)
    }

    @Test
    fun `refreshCreations reloads first page with current filters`() = runTest {
        val repository = FakePlazaRepository()
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadInitialData()
        screenModel.selectTag("tag-1")
        screenModel.refreshCreations()

        assertEquals(listOf("tag-1"), repository.lastTags)
        assertEquals(1, screenModel.uiState.value.currentPage)
    }

    @Test
    fun `loadShorts loads real categories and cards`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortCategories = listOf(PlazaShortCategory(code = "HOT", name = "Hot"))
        repository.shortPages = mapOf(
            1 to listOf(PlazaShortCard(id = "short-1", name = "Short demo")),
        )
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadShorts()

        val state = screenModel.uiState.value
        assertEquals(listOf("HOT"), state.shortCategories.map { it.code })
        assertEquals(listOf("short-1"), state.shorts.map { it.id })
    }

    @Test
    fun `selectShortCategory reloads shorts with selected category`() = runTest {
        val repository = FakePlazaRepository()
        repository.shortCategories = listOf(PlazaShortCategory(code = "HOT", name = "Hot"))
        repository.shortPages = mapOf(
            1 to listOf(PlazaShortCard(id = "short-hot", name = "Hot short")),
        )
        val screenModel = PlazaScreenModel(repository)

        screenModel.loadShorts()
        screenModel.selectShortCategory("HOT")

        val state = screenModel.uiState.value
        assertEquals("HOT", state.selectedShortCategoryCode)
        assertEquals("HOT", repository.lastShortCategoryCode)
        assertEquals(listOf("short-hot"), state.shorts.map { it.id })
    }

    private class FakePlazaRepository : PlazaRepository {
        var lastSort: String? = null
        var lastTags: List<String> = emptyList()
        val requestedPages = mutableListOf<Int>()
        var pages: Map<Int, List<PlazaCreationCard>> = mapOf(
            1 to listOf(PlazaCreationCard(id = "creation-1", intro = "Demo")),
        )
        var shortCategories: List<PlazaShortCategory> = emptyList()
        var shortPages: Map<Int, List<PlazaShortCard>> = emptyMap()
        var lastShortCategoryCode: String? = null

        override suspend fun getTags(): Result<List<PlazaTag>> =
            Result.success(listOf(PlazaTag(id = "tag-1", name = "Images", level = 1)))

        override suspend fun listCreations(
            page: Int,
            size: Int,
            sort: String,
            tags: List<String>,
        ): Result<PlazaCreationPage> {
            lastSort = sort
            lastTags = tags
            requestedPages += page
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

        override suspend fun listShorts(page: Int, size: Int, categoryCode: String?): Result<List<PlazaShortCard>> {
            lastShortCategoryCode = categoryCode
            return Result.success(shortPages[page].orEmpty())
        }
    }
}
