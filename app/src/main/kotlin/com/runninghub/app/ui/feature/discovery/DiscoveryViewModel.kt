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
import com.runninghub.app.data.remote.model.TagTreeRequest
import com.runninghub.app.data.remote.model.WebAppListRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val webAppApi: WebAppApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    init {
        fetchBanners()
        fetchCategories()
        fetchWebAppList()
    }

    private fun fetchBanners() {
        viewModelScope.launch {
            try {
                val response = webAppApi.getCarefullyChosenList()
                if (response.code == 0 && response.data.isNotEmpty()) {
                    val banners = response.data.map { dto ->
                        Banner(
                            id = dto.id,
                            title = dto.title,
                            description = dto.desc,
                            imageUrl = dto.preview?.url ?: dto.covers?.firstOrNull()?.url ?: "",
                            tag = dto.tags?.firstOrNull()?.name
                        )
                    }
                    _uiState.update { it.copy(banners = banners) }
                }
            } catch (e: Exception) {
                // Keep default or handle error
            }
        }
    }

    private fun loadDiscoveryData() {
        viewModelScope.launch {
            // 目前依然保留 mock 的 Creator 逻辑，直到获取相应接口
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
            // 并行重载所有数据
            fetchBanners()
            fetchCategories()
            fetchWebAppListInternal(isRefreshing = true, page = 1)
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMore || currentState.isRefreshing) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            fetchWebAppListInternal(isRefreshing = false, page = currentState.currentPage + 1)
        }
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            try {
                val response = webAppApi.getTagTree(TagTreeRequest())
                if (response.code == 0 && response.data.isNotEmpty()) {
                    val allTags = response.data
                    // 仅显示一级分类作为 Tab
                    val level1Tags = allTags.filter { it.level == 1 }
                    
                    val remoteCategories = level1Tags.map { parent ->
                        Category(
                            tagIds = collectLeafIds(parent, allTags),
                            name = parent.name
                        )
                    }
                    val allCategories = listOf(Category(emptyList(), "推荐")) + remoteCategories
                    _uiState.update { it.copy(categories = allCategories) }
                } else {
                    useFallbackCategories()
                }
            } catch (e: Exception) {
                useFallbackCategories()
            }
        }
    }

    /**
     * 递归收集叶子节点 ID。
     * 如果一个节点没有子节点，则它是叶子；如果有子节点，则只收集子节点的叶子。
     */
    private fun collectLeafIds(parent: com.runninghub.app.data.remote.model.TagDto, allTags: List<com.runninghub.app.data.remote.model.TagDto>): List<String> {
        // 优先使用嵌套结构，如果为空则说明是叶子（或扁平结构，但 Hub 目前返回的是嵌套）
        val children = parent.childTags ?: emptyList()
        
        if (children.isEmpty()) {
            return listOf(parent.id)
        }
        
        return children.flatMap { collectLeafIds(it, allTags) }
    }

    private fun useFallbackCategories() {
        val fallbacks = listOf(
            Category(emptyList(), "推荐"),
            // 文生图(197), 图生图(198), 反推提示词(203)
            Category(listOf("1871151815242543197", "1871151815242543198", "1871151815242543203"), "图片生成"),
            // 文生视频(200), 图生视频(199), 视频生视频(201)
            Category(listOf("1871151815242543200", "1871151815242543199", "1871151815242543201"), "视频生成"),
            Category(listOf("1871151815242543272", "1875941016195787300", "1875941016195787301"), "视频特效")
            // ... 可根据需要补充
        )
        _uiState.update { it.copy(categories = fallbacks) }
    }

    private fun fetchWebAppList() {
        fetchWebAppListInternal(isRefreshing = false, page = 1)
    }

    private fun fetchWebAppListInternal(isRefreshing: Boolean, page: Int) {
        viewModelScope.launch {
            if (!isRefreshing && page == 1) _uiState.update { it.copy(isLoading = true) }
            try {
                val selectedTags = _uiState.value.selectedCategory.tagIds
                
                val response = webAppApi.getWebAppList(
                    WebAppListRequest(
                        size = 30, 
                        current = page,
                        tags = selectedTags
                    )
                )
                if (response.code == 0) {
                    val apps = response.data.records.map { dto ->
                        AiApp(
                            id = dto.id,
                            title = dto.title,
                            author = dto.author?.nickname ?: dto.owner?.name ?: "Anonymous",
                            authorAvatar = dto.owner?.avatar ?: dto.author?.avatar,
                            imageUrl = dto.covers?.firstOrNull()?.url ?: dto.preview?.url ?: "",
                            likes = dto.statisticsInfo?.likeCount?.toIntOrNull() ?: 0,
                            stars = dto.statisticsInfo?.collectCount?.toIntOrNull() ?: 0,
                            useCount = dto.statisticsInfo?.useCount ?: "0",
                            views = dto.statisticsInfo?.pv ?: "0"
                        )
                    }
                    
                    val hasMoreData = apps.size >= 30 // 简单判断，实际应依赖 total
                    
                    _uiState.update { 
                        it.copy(
                            discoveryApps = if (page == 1) apps else it.discoveryApps + apps, 
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            currentPage = page,
                            hasMore = hasMoreData
                        ) 
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
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
                discoveryApps = emptyList(), // 清空旧数据，触发加载状态
                isLoading = true 
            ) 
        }
        fetchWebAppListInternal(isRefreshing = false, page = 1)
    }
}
