package com.runninghub.app.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val webAppRepository: WebAppRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<WebApp>>(emptyList())
    val searchResults: StateFlow<List<WebApp>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        _query.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            webAppRepository.searchApps(query = query, page = 1, pageSize = 30)
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            _searchResults.value = result.data
                            _isLoading.value = false
                        }
                        is AppResult.Error -> {
                            _isLoading.value = false
                        }
                        is AppResult.Loading -> {}
                    }
                }
        }
    }
}
