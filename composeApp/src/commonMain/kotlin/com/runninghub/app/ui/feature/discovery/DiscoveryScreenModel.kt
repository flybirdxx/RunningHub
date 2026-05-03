package com.runninghub.app.ui.feature.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption(
    val label: String,
    val apiValue: String,
    val days: Int? = null
) {
    RECOMMEND("推荐", "RECOMMEND"),
    REPUTATION("口碑", "REPUTATION", days = 3),
    HOTTEST("最热", "HOTTEST"),
    NEWEST("最新", "NEWEST"),
}

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val banners: List<WebApp> = emptyList(),
    val categories: List<Tag> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val selectedSort: SortOption = SortOption.RECOMMEND,
    val apps: List<WebApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)

private const val PAGE_SIZE = 30

class DiscoveryScreenModel(
    private val webAppRepository: WebAppRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var loadMoreJob: Job? = null

    // Called from Screen via LaunchedEffect(Unit) to avoid suspend-in-init anti-pattern
    fun loadInitialData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categoriesDeferred = launch { loadCategories() }
                categoriesDeferred.join()

                val bannersDeferred = launch { loadCustomMade() }
                val appsDeferred = launch { loadApps(page = 1, reset = true) }

                bannersDeferred.join()
                appsDeferred.join()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadCustomMade() {
        val tags = selectedTags()
        webAppRepository.getCustomMadeWebappList(tags)
            .onSuccess { banners ->
                _uiState.update { it.copy(banners = banners) }
            }
    }

    private suspend fun loadCategories() {
        webAppRepository.getTagTree()
            .onSuccess { tags ->
                val level1Tags = tags.filter { it.level == 1 }
                _uiState.update { it.copy(categories = level1Tags) }
            }
    }

    private suspend fun loadApps(
        page: Int,
        reset: Boolean
    ) {
        val state = _uiState.value
        val sort = state.selectedSort
        val tags = selectedTags()
        webAppRepository.getAppList(
            pageNum = page,
            pageSize = PAGE_SIZE,
            tags = tags,
            sort = sort.apiValue,
            days = sort.days
        ).onSuccess { pageData ->
            _uiState.update {
                it.copy(
                    apps = if (reset) pageData.records else it.apps + pageData.records,
                    currentPage = page,
                    hasMore = pageData.hasNext,
                    isLoadingApps = false,
                    error = null
                )
            }
        }.onFailure { e ->
            _uiState.update {
                it.copy(
                    isLoadingApps = false,
                    error = if (reset) e.message ?: "加载失败" else it.error
                )
            }
        }
    }

    fun selectCategory(index: Int) {
        val state = _uiState.value
        if (index == state.selectedCategoryIndex) return

        loadMoreJob?.cancel()
        loadMoreJob = null

        _uiState.update {
            it.copy(
                selectedCategoryIndex = index,
                apps = emptyList(),
                isLoadingApps = true,
                currentPage = 1,
                hasMore = true,
                error = null
            )
        }

        screenModelScope.launch {
            loadCustomMade()
            loadApps(page = 1, reset = true)
            _uiState.update { it.copy(isLoadingApps = false) }
        }
    }

    fun selectSort(sort: SortOption) {
        val state = _uiState.value
        if (sort == state.selectedSort) return

        loadMoreJob?.cancel()
        loadMoreJob = null

        _uiState.update {
            it.copy(
                selectedSort = sort,
                apps = emptyList(),
                isLoadingApps = true,
                currentPage = 1,
                hasMore = true,
                error = null
            )
        }

        screenModelScope.launch {
            loadApps(page = 1, reset = true)
            _uiState.update { it.copy(isLoadingApps = false) }
        }
    }

    fun refresh() {
        loadMoreJob?.cancel()
        loadMoreJob = null

        screenModelScope.launch {
            _uiState.update {
                it.copy(isRefreshing = true, isLoadingMore = false, error = null)
            }
            try {
                loadCategories()
                loadCustomMade()
                loadApps(page = 1, reset = true)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.isLoading || state.isRefreshing) return

        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }

        loadMoreJob = screenModelScope.launch {
            loadApps(page = nextPage, reset = false)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private fun selectedTags(index: Int = _uiState.value.selectedCategoryIndex): List<String> {
        if (index <= 0) return emptyList()
        val level1Tag = _uiState.value.categories.getOrNull(index - 1) ?: return emptyList()
        return level1Tag.childTags?.map { it.id } ?: emptyList()
    }
}
