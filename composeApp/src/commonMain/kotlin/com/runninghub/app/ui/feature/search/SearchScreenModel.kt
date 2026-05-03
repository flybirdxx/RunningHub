package com.runninghub.app.ui.feature.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<WebApp> = emptyList(),
    val hotTags: List<Tag> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null
)

private const val SEARCH_DEBOUNCE_MS = 350L
private const val PAGE_SIZE = 10

class SearchScreenModel(
    private val webAppRepository: WebAppRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // Called from Screen via LaunchedEffect(Unit) to avoid suspend-in-init anti-pattern
    fun loadHotTags() {
        screenModelScope.launch {
            webAppRepository.getTagTree()
                .onSuccess { tags ->
                    val hot = tags
                        .flatMap { it.childTags.orEmpty() + it }
                        .take(12)
                    _uiState.update { it.copy(hotTags = hot) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    isSearching = false,
                    error = null,
                    currentPage = 1,
                    hasMore = true,
                )
            }
            return
        }

        searchJob = screenModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(query, page = 1, reset = true)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            performSearch(query, page = 1, reset = true)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update {
            SearchUiState(hotTags = it.hotTags)
        }
    }

    fun searchByTag(tag: Tag) {
        search(tag.name)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isSearching || !state.hasMore || state.query.isBlank()) return

        searchJob = screenModelScope.launch {
            performSearch(state.query, page = state.currentPage + 1, reset = false)
        }
    }

    private suspend fun performSearch(query: String, page: Int, reset: Boolean) {
        _uiState.update { it.copy(isSearching = true, error = null) }

        webAppRepository.searchApps(
            keyword = query,
            pageNum = page,
            pageSize = PAGE_SIZE,
        ).onSuccess { pageData ->
            _uiState.update { state ->
                state.copy(
                    results = if (reset) pageData.records else state.results + pageData.records,
                    currentPage = page,
                    hasMore = pageData.hasNext,
                    isSearching = false,
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isSearching = false,
                    error = e.message ?: "搜索失败",
                )
            }
        }
    }
}
