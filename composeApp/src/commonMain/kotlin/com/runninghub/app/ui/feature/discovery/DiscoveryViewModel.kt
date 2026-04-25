package com.runninghub.app.ui.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DiscoveryViewModel(
    private val webAppRepository: WebAppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            launch {
                webAppRepository.getFeaturedApps().collect { result ->
                    when (result) {
                        is AppResult.Success -> _uiState.update { it.copy(banners = result.data) }
                        is AppResult.Error -> {}
                        is AppResult.Loading -> {}
                    }
                }
            }

            launch {
                webAppRepository.getCategories().collect { result ->
                    when (result) {
                        is AppResult.Success -> _uiState.update { it.copy(categories = result.data) }
                        is AppResult.Error -> {}
                        is AppResult.Loading -> {}
                    }
                }
            }

            launch {
                loadApps(page = 1)
            }
        }
    }

    fun selectCategory(index: Int) {
        _uiState.update { it.copy(selectedCategoryIndex = index, apps = emptyList(), currentPage = 1, hasMore = true) }
        viewModelScope.launch { loadApps(page = 1) }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, currentPage = 1, hasMore = true) }
        viewModelScope.launch {
            loadApps(page = 1)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            loadApps(page = state.currentPage + 1, append = true)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    private suspend fun loadApps(page: Int, append: Boolean = false) {
        val state = _uiState.value
        val tags = state.categories.getOrNull(state.selectedCategoryIndex)
            ?.children?.map { it.id } ?: emptyList()

        webAppRepository.getWebApps(page = page, pageSize = 30, tags = tags, sort = "RECOMMEND")
            .collect { result ->
                when (result) {
                    is AppResult.Success -> {
                        val newApps = result.data
                        _uiState.update {
                            it.copy(
                                apps = if (append) it.apps + newApps else newApps,
                                currentPage = page,
                                hasMore = newApps.size >= 30,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.update { it.copy(error = result.message, isLoading = false) }
                    }
                    is AppResult.Loading -> {}
                }
            }
    }
}
