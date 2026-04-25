package com.runninghub.app.ui.feature.discovery

import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.WebApp

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val banners: List<WebApp> = emptyList(),
    val categories: List<Tag> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val apps: List<WebApp> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)
