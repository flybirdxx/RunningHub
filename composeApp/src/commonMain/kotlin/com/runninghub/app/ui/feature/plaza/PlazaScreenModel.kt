package com.runninghub.app.ui.feature.plaza

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaRepository
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class PlazaMode {
    CREATIONS,
    SHORTS,
}

data class PlazaUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val mode: PlazaMode = PlazaMode.CREATIONS,
    val tags: List<PlazaTag> = emptyList(),
    val selectedTagId: String? = null,
    val sort: String = "RECOMMEND",
    val creations: List<PlazaCreationCard> = emptyList(),
    val currentPage: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val shortCategories: List<PlazaShortCategory> = emptyList(),
    val selectedShortCategoryCode: String? = null,
    val shorts: List<PlazaShortCard> = emptyList(),
    val shortPage: Int = 0,
    val isShortsLoading: Boolean = false,
    val error: String? = null,
)

class PlazaScreenModel(
    private val plazaRepository: PlazaRepository,
) : ScreenModel {
    private val _uiState = MutableStateFlow(PlazaUiState())
    val uiState: StateFlow<PlazaUiState> = _uiState.asStateFlow()

    fun loadInitialData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tagsResult = withTimeoutOrNull(6_000) { plazaRepository.getTags() }
                ?: Result.failure(IllegalStateException("Plaza tags request timed out"))
            tagsResult.onSuccess { tags ->
                _uiState.update { it.copy(tags = tags.filter { tag -> tag.level <= 1 && tag.enable }) }
            }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    fun refreshCreations() {
        screenModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    fun loadMoreCreations() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        screenModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            loadCreations(page = state.currentPage + 1, append = true, fallbackOnFailure = false)
        }
    }

    fun selectTag(tagId: String?) {
        _uiState.update { it.copy(selectedTagId = tagId, currentPage = 0, hasMore = true) }
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = it.creations.isEmpty(), error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    fun selectSort(sort: String) {
        _uiState.update { it.copy(sort = sort, currentPage = 0, hasMore = true) }
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = it.creations.isEmpty(), error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    fun selectMode(mode: PlazaMode) {
        _uiState.update { it.copy(mode = mode) }
        if (mode == PlazaMode.SHORTS && _uiState.value.shorts.isEmpty()) {
            loadShorts()
        }
    }
    fun loadShorts() {
        screenModelScope.launch {
            _uiState.update { it.copy(isShortsLoading = true, shortPage = 0, error = null) }
            val categoriesResult = withTimeoutOrNull(6_000) { plazaRepository.listShortCategories() }
                ?: Result.failure(IllegalStateException("Short categories request timed out"))
            categoriesResult.onSuccess { categories ->
                _uiState.update { it.copy(shortCategories = categories) }
            }
            loadShortPage(page = 1, append = false)
        }
    }

    fun selectShortCategory(categoryCode: String?) {
        _uiState.update {
            it.copy(
                selectedShortCategoryCode = categoryCode,
                shorts = emptyList(),
                shortPage = 0,
                isShortsLoading = true,
                error = null,
            )
        }
        screenModelScope.launch { loadShortPage(page = 1, append = false) }
    }

    fun loadMoreShorts() {
        val state = _uiState.value
        if (state.isShortsLoading) return
        val nextPage = state.shortPage + 1
        _uiState.update { it.copy(isShortsLoading = true, error = null) }
        screenModelScope.launch { loadShortPage(page = nextPage, append = true) }
    }

    private suspend fun loadCreations(page: Int, append: Boolean, fallbackOnFailure: Boolean) {
        val state = _uiState.value
        val selectedTags = state.selectedTagId?.let(::listOf).orEmpty()
        val result = withTimeoutOrNull(6_000) {
            plazaRepository.listCreations(page = page, sort = state.sort, tags = selectedTags)
        } ?: Result.failure(IllegalStateException("Plaza creations request timed out"))

        result
            .onSuccess { creationPage ->
                val merged = if (append) state.creations + creationPage.items else creationPage.items
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        creations = merged,
                        currentPage = creationPage.page,
                        total = creationPage.total,
                        hasMore = merged.size < creationPage.total && creationPage.items.isNotEmpty(),
                        error = null,
                    )
                }
            }
            .onFailure { error ->
                if (fallbackOnFailure && _uiState.value.creations.isEmpty()) {
                    applyFallbackContent(error.message ?: "Plaza load failed")
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = error.message ?: "Plaza load failed",
                        )
                    }
                }
            }
    }

    private suspend fun loadShortPage(page: Int, append: Boolean) {
        val state = _uiState.value
        val result = withTimeoutOrNull(6_000) {
            plazaRepository.listShorts(page = page, categoryCode = state.selectedShortCategoryCode)
        } ?: Result.failure(IllegalStateException("Short list request timed out"))

        result
            .onSuccess { cards ->
                _uiState.update {
                    it.copy(
                        isShortsLoading = false,
                        shorts = if (append) it.shorts + cards else cards,
                        shortPage = page,
                        error = null,
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        isShortsLoading = false,
                        error = error.message ?: "Short list load failed",
                    )
                }
            }
    }

    private fun applyFallbackContent(message: String) {
        val state = _uiState.value
        val cards = PlazaFallbackCatalog.creations(state.selectedTagId, state.sort)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                tags = if (it.tags.isEmpty()) PlazaFallbackCatalog.tags else it.tags,
                creations = cards,
                currentPage = 1,
                total = cards.size,
                hasMore = false,
                error = message,
            )
        }
    }
}

private object PlazaFallbackCatalog {
    val tags = listOf(
        PlazaTag(id = "image", name = "Images", level = 1),
        PlazaTag(id = "video", name = "Videos", level = 1),
        PlazaTag(id = "workflow", name = "Workflows", level = 1),
    )

    private val cards = listOf(
        FallbackCard(
            tagId = "image",
            card = PlazaCreationCard(
                id = "fallback-image-v2",
                intro = "All Power Image V2 prompt gallery example",
                ownerName = "RunningHub API",
                mediaType = "IMAGE",
                likeCount = "128",
                useCount = "42",
            ),
        ),
        FallbackCard(
            tagId = "image",
            card = PlazaCreationCard(
                id = "fallback-seedream",
                intro = "Seedream V5 Lite text-to-image showcase",
                ownerName = "RunningHub API",
                mediaType = "IMAGE",
                likeCount = "96",
                useCount = "35",
            ),
        ),
        FallbackCard(
            tagId = "video",
            card = PlazaCreationCard(
                id = "fallback-video-workflow",
                intro = "Video workflow result prepared from captured docs",
                ownerName = "RunningHub creator",
                mediaType = "VIDEO",
                likeCount = "64",
                useCount = "18",
            ),
        ),
        FallbackCard(
            tagId = "workflow",
            card = PlazaCreationCard(
                id = "fallback-workflow",
                intro = "Reusable API model invocation workflow",
                ownerName = "RunningHub creator",
                mediaType = "WORKFLOW",
                likeCount = "51",
                useCount = "27",
            ),
        ),
    )

    fun creations(tagId: String?, sort: String): List<PlazaCreationCard> {
        val filtered = cards.filter { tagId == null || it.tagId == tagId }.map { it.card }
        return when (sort) {
            "LATEST" -> filtered.asReversed()
            "HOT" -> filtered.sortedByDescending { it.likeCount?.toIntOrNull() ?: 0 }
            else -> filtered
        }
    }

    private data class FallbackCard(
        val tagId: String,
        val card: PlazaCreationCard,
    )
}
