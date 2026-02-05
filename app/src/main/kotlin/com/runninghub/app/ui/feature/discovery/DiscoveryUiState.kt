/**
 * [INPUT]: 基础数据模型 (Banner, AiTool, Creator, AiApp)
 * [OUTPUT]: 对外提供 DiscoveryUiState 单一真相源模型
 * [POS]: 首页 UI 状态的定义者，确保视图层状态的完备性
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.feature.discovery

data class Banner(
    val id: String,
    val title: String,
    val description: String?,
    val imageUrl: String,
    val tag: String?
)

data class AiTool(
    val id: String,
    val name: String,
    val desc: String,
    val icon: String,
    val color: String
)

data class Creator(
    val id: String,
    val name: String,
    val role: String,
    val avatarUrl: String,
    val isVerified: Boolean = true
)

data class AiApp(
    val id: String,
    val title: String,
    val author: String,
    val authorAvatar: String?,
    val imageUrl: String,
    val likes: Int,
    val stars: Int,
    val useCount: String,
    val views: String
)

data class Category(
    val tagIds: List<String>,
    val name: String
)

data class DiscoveryUiState(
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = listOf(Category(emptyList(), "推荐")),
    val selectedCategory: Category = Category(emptyList(), "推荐"),
    val aiTools: List<AiTool> = emptyList(),
    val featuredCreators: List<Creator> = emptyList(),
    val discoveryApps: List<AiApp> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)
