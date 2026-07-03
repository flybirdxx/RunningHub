package com.runninghub.app.ui.feature.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.feature.discovery.presentation.DiscoveryStateHolder
import com.runninghub.feature.discovery.presentation.DiscoveryUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Discovery Voyager 页面对 Discovery Presentation 状态持有器的生命周期适配层。
 *
 * 分类加载、列表分页、旧响应隔离和目录错误文案映射由 [DiscoveryStateHolder] 承担；
 * 搜索能力由独立搜索页承担，本类不再暴露任何搜索委托。
 * 本类只负责把 Voyager 的 [screenModelScope] 传入并为 Koin/Voyager 暴露原有 ScreenModel 类型。
 *
 * @param webAppRepository WebApp 目录仓库，用于加载公开目录和标签树。
 */
class DiscoveryScreenModel(
    webAppRepository: WebAppCatalogRepository,
) : ScreenModel {
    private val stateHolder = DiscoveryStateHolder(
        webAppRepository = webAppRepository,
        coroutineScope = screenModelScope,
    )

    /**
     * 发现页只读 UI 状态。
     *
     * UI 只能收集该状态并通过本类公开动作发送事件，避免 composeApp 重新实现 Discovery Presentation 状态规则。
     */
    val uiState: StateFlow<DiscoveryUiState> = stateHolder.uiState

    /**
     * 加载发现页首屏数据。
     */
    fun loadInitialData() {
        stateHolder.loadInitialData()
    }

    /**
     * 选择发现页分类。
     *
     * @param index UI 分类下标；`0` 表示“全部”，`1` 才对应第一个真实一级分类。
     */
    fun selectCategory(index: Int) {
        stateHolder.selectCategory(index)
    }

    /**
     * 选择发现页排序方式。
     *
     * @param sort 用户选择的排序选项。
     */
    fun selectSort(sort: CatalogSort) {
        stateHolder.selectSort(sort)
    }

    /**
     * 刷新当前分类和排序下的发现列表。
     */
    fun refresh() {
        stateHolder.refresh()
    }

    /**
     * 加载发现列表下一页。
     */
    fun loadMore() {
        stateHolder.loadMore()
    }
}
