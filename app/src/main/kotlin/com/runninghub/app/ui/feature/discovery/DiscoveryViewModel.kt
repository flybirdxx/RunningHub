/**
 * [INPUT]: 依赖 DiscoveryUiState, WebAppApi, ViewModel
 * [OUTPUT]: 对外提供 DiscoveryViewModel，管理首页真实数据流
 * [POS]: 首页业务逻辑的控制器，负责从 API 加载实时数据
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.feature.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.repository.DiscoveryRepository
import com.runninghub.app.data.remote.model.TagTreeRequest
import com.runninghub.app.data.remote.model.WebAppListRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: DiscoveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState(isLoading = true))
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refresh()
    }

    private fun observeData() {
        // Observe Banners
        viewModelScope.launch {
            repository.banners.collect { banners ->
                _uiState.update { it.copy(banners = banners) }
            }
        }

        // Observe Categories
        viewModelScope.launch {
            repository.categories.collect { categories ->
                val allCategories = listOf(Category(emptyList(), "推荐")) + categories
                _uiState.update { it.copy(categories = allCategories) }
            }
        }

        // Observe Apps for the selected category
        viewModelScope.launch {
            // Re-collect apps whenever category changes
            _uiState.map { it.selectedCategory }.distinctUntilChanged().collectLatest { category ->
                repository.getApps(category.name).collect { apps ->
                    _uiState.update { 
                        it.copy(
                            discoveryApps = apps,
                            isLoading = if (apps.isEmpty()) _uiState.value.isLoading else false,
                            isRefreshing = false
                        ) 
                    }
                }
            }
        }
        
        // Initial setup for other static UI elements
        loadDiscoveryData()
    }

    private fun loadDiscoveryData() {
        viewModelScope.launch {
            val mockTools = listOf(
                AiTool("1", "图像生成", "V2.6 动态", "auto_fix_high", "#6366F1"),
                AiTool("2", "视频专业版", "超分辨率", "videocam", "#10B981")
            )

            val mockCreators = listOf(
                Creator("1", "Sophie Art", "3D 视觉艺术家", "https://i.pravatar.cc/150?u=1"),
                Creator("2", "Milo Design", "动态设计专家", "https://i.pravatar.cc/150?u=2"),
                Creator("3", "Aria Concept", "插画师", "https://i.pravatar.cc/150?u=3", false)
            )
            
            _uiState.update { 
                it.copy(
                    aiTools = mockTools,
                    featuredCreators = mockCreators
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, currentPage = 1, hasMore = true) }
            try {
                // Fetch everything from network in background
                repository.refreshBanners()
                repository.refreshCategories()
                repository.refreshApps(
                    _uiState.value.selectedCategory.name,
                    _uiState.value.selectedCategory.tagIds
                )
            } finally {
                // Ensure isRefreshing is always reset even if network or DB insert fails 
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = _uiState.value.currentPage + 1
            
            val loadedCount = repository.loadMoreApps(
                _uiState.value.selectedCategory.name,
                _uiState.value.selectedCategory.tagIds,
                nextPage
            )
            
            if (loadedCount != null) {
                if (loadedCount > 0) {
                    _uiState.update { 
                        it.copy(
                            isLoadingMore = false,
                            currentPage = nextPage,
                            hasMore = loadedCount == 30 // 假定 PageSize 为 30
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoadingMore = false,
                            hasMore = false
                        ) 
                    }
                }
            } else {
                // 网络错误或加载失败，重置 Loading 但保留 hasMore 权限以便稍后重试
                _uiState.update { 
                    it.copy(isLoadingMore = false) 
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        if (_uiState.value.selectedCategory.tagIds == category.tagIds) return
        _uiState.update { 
            it.copy(
                selectedCategory = category, 
                currentPage = 1, 
                hasMore = true,
                isLoading = true 
            ) 
        }
        viewModelScope.launch {
            repository.refreshApps(category.name, category.tagIds)
        }
    }
}
