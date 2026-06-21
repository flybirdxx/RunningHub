package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 搜索页的完整可渲染状态。
 *
 * 状态由 [SearchStateHolder] 维护，只保存搜索输入、热门标签和分页结果。
 * 该对象不得保存 API Key、Cookie 或任务执行参数，搜索页只依赖公开目录能力。
 *
 * @property query 用户当前输入或点击热门标签后生成的搜索关键词。
 * 空字符串表示尚未搜索或已经清空输入；提交搜索前会拦截空白内容。
 * @property isSearching 是否正在执行搜索请求。
 * `true` 时页面应展示搜索进度并阻止重复加载；`false` 表示当前没有搜索请求进行中。
 * @property results 当前关键词对应的 WebApp 搜索结果。
 * 列表顺序保留服务端排序；空列表表示尚未搜索、已清空输入或当前关键词无结果。
 * @property hotTags 热门标签列表，来源于目录标签树的扁平化结果。
 * 空列表表示标签尚未加载或服务端没有返回可用标签。
 * @property currentPage 当前搜索结果已成功加载的页码，从 1 开始。
 * 新关键词搜索成功后重置为 1，加载更多成功后递增；加载更多失败时保持旧页码。
 * @property hasMore 当前关键词是否还有下一页搜索结果。
 * `true` 表示允许触发加载更多；`false` 表示分页已结束。
 * @property error 等待页面展示的搜索错误信息。
 * `null` 表示当前没有错误；非空时由页面展示并可通过重新搜索覆盖。
 */
data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<WebApp> = emptyList(),
    val hotTags: List<Tag> = emptyList(),
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val error: String? = null,
)

private const val SEARCH_DEBOUNCE_MS = 350L
private const val PAGE_SIZE = 10

/**
 * 持有搜索页热门标签、关键词、防抖搜索和分页状态。
 *
 * 本类位于 Discovery Presentation 层，只依赖 [WebAppCatalogRepository] 领域契约，
 * 不依赖 Compose、Voyager 或 Data 实现。应用壳负责传入与页面生命周期绑定的
 * [coroutineScope]，页面离栈或 ScreenModel 销毁时未完成的防抖、搜索和分页任务会随作用域取消。
 *
 * @param webAppRepository WebApp 目录仓库，用于读取标签树和分页搜索结果。
 * @param coroutineScope 与搜索页面生命周期绑定的协程作用域。
 */
class SearchStateHolder(
    private val webAppRepository: WebAppCatalogRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    // 搜索请求使用递增版本号隔离旧响应。取消协程不能保证远端旧请求不返回，
    // 因此写状态前仍要确认响应属于用户最后一次输入或点击的关键词。
    private var searchRequestVersion: Long = 0

    private fun nextSearchRequestVersion(): Long {
        searchRequestVersion += 1
        return searchRequestVersion
    }

    /**
     * 加载搜索页热门标签。
     *
     * 该方法由页面的 `LaunchedEffect(Unit)` 触发，避免构造函数中启动协程导致测试和生命周期难以控制。
     */
    fun loadHotTags() {
        coroutineScope.launch {
            webAppRepository.getTagTree()
                .onSuccess { tags ->
                    val hot = tags
                        .flatMap { it.childTags.orEmpty() + it }
                        .take(12)
                    _uiState.update { it.copy(hotTags = hot) }
                }
        }
    }

    /**
     * 处理搜索输入变化。
     *
     * 每次输入都会取消上一轮未完成的防抖任务，确保只有最新关键词会触发远端搜索。
     * 空白关键词会立即清空结果并恢复分页状态，避免旧结果误显示为当前输入的结果。
     *
     * @param query 用户输入框中的原始内容，允许为空或包含空白字符。
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            nextSearchRequestVersion()
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

        val requestVersion = nextSearchRequestVersion()
        searchJob = coroutineScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(query = query, page = 1, reset = true, requestVersion = requestVersion)
        }
    }

    /**
     * 立即按指定关键词搜索。
     *
     * 该方法用于点击热门标签或提交键场景，会跳过输入防抖但仍复用同一搜索流程，
     * 保证错误映射、分页状态和列表替换规则一致。
     *
     * @param query 待搜索关键词；空白内容会被忽略。
     */
    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { it.copy(query = trimmed) }
        searchJob?.cancel()
        val requestVersion = nextSearchRequestVersion()
        searchJob = coroutineScope.launch {
            performSearch(query = trimmed, page = 1, reset = true, requestVersion = requestVersion)
        }
    }

    /**
     * 清空搜索输入和结果。
     *
     * 清空时保留已经加载的热门标签，因为热门标签是页面公共入口，不应因用户退出搜索而重新请求。
     */
    fun clearSearch() {
        searchJob?.cancel()
        nextSearchRequestVersion()
        _uiState.update {
            SearchUiState(hotTags = it.hotTags)
        }
    }

    /**
     * 通过热门标签发起搜索。
     *
     * 标签名由目录标签树返回，直接作为关键词进入搜索流程，便于复用分页和错误处理逻辑。
     *
     * @param tag 用户点击的热门标签。
     */
    fun searchByTag(tag: Tag) {
        search(tag.name)
    }

    /**
     * 加载当前关键词的下一页搜索结果。
     *
     * 当搜索请求进行中、没有更多数据或关键词为空时直接返回，避免重复分页请求和无效远端调用。
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isSearching || !state.hasMore || state.query.isBlank()) return

        val requestVersion = searchRequestVersion
        searchJob = coroutineScope.launch {
            performSearch(query = state.query, page = state.currentPage + 1, reset = false, requestVersion = requestVersion)
        }
    }

    private suspend fun performSearch(
        query: String,
        page: Int,
        reset: Boolean,
        requestVersion: Long,
    ) {
        _uiState.update { it.copy(isSearching = true, error = null) }

        webAppRepository.searchApps(
            keyword = query,
            pageNum = page,
            pageSize = PAGE_SIZE,
        ).onSuccess { pageData ->
            if (requestVersion != searchRequestVersion) return@onSuccess
            _uiState.update { state ->
                state.copy(
                    results = if (reset) {
                        pageData.records
                    } else {
                        (state.results + pageData.records).distinctBy { app -> app.id }
                    },
                    currentPage = page,
                    hasMore = pageData.hasNext,
                    isSearching = false,
                )
            }
        }.onFailure { e ->
            if (requestVersion != searchRequestVersion) return@onFailure
            _uiState.update {
                it.copy(
                    isSearching = false,
                    error = e.toCatalogErrorMessage("搜索失败"),
                )
            }
        }
    }
}
