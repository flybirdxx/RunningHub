package com.runninghub.app.ui.feature.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.core.model.Tag
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import com.runninghub.feature.discovery.presentation.SearchStateHolder
import com.runninghub.feature.discovery.presentation.SearchUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * 搜索页 ScreenModel 门面。
 *
 * 搜索业务状态已迁入 Discovery Presentation 层的 [SearchStateHolder]。
 * 本类仅负责把 Voyager 的 [screenModelScope] 交给 StateHolder，并向 Compose 页面暴露同名事件入口。
 *
 * @param webAppRepository WebApp 目录仓库，由 Koin 注入领域契约实现。
 */
class SearchScreenModel(
    webAppRepository: WebAppCatalogRepository,
) : ScreenModel {

    private val stateHolder = SearchStateHolder(webAppRepository, screenModelScope)
    val uiState: StateFlow<SearchUiState> = stateHolder.uiState

    /**
     * 加载搜索页热门标签。
     *
     * 该方法由页面的 `LaunchedEffect(Unit)` 触发，避免构造函数中启动协程导致测试和生命周期难以控制。
     */
    fun loadHotTags() = stateHolder.loadHotTags()

    /**
     * 处理搜索输入变化。
     *
     * 每次输入都会取消上一轮未完成的防抖任务，确保只有最新关键词会触发远端搜索。
     * 空白关键词会立即清空结果并恢复分页状态，避免旧结果误显示为当前输入的结果。
     *
     * @param query 用户输入框中的原始内容，允许为空或包含空白字符。
     */
    fun onQueryChange(query: String) = stateHolder.onQueryChange(query)

    /**
     * 立即按指定关键词搜索。
     *
     * 该方法用于点击热门标签或提交键场景，会跳过输入防抖但仍复用同一搜索流程，
     * 保证错误映射、分页状态和列表替换规则一致。
     *
     * @param query 待搜索关键词；空白内容会被忽略。
     */
    fun search(query: String) = stateHolder.search(query)

    /**
     * 通过热门标签发起搜索。
     *
     * 标签名由目录标签树返回，直接作为关键词进入搜索流程，便于复用分页和错误处理逻辑。
     *
     * @param tag 用户点击的热门标签。
     */
    fun searchByTag(tag: Tag) = stateHolder.searchByTag(tag)

    /**
     * 加载当前关键词的下一页搜索结果。
     *
     * 当搜索请求进行中、没有更多数据或关键词为空时直接返回，避免重复分页请求和无效远端调用。
     */
    fun loadMore() = stateHolder.loadMore()
}
