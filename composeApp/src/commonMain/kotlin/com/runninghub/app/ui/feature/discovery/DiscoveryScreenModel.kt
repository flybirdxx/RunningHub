package com.runninghub.app.ui.feature.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 发现页列表排序选项。
 *
 * @property label 展示给用户的中文排序名称，由 Presentation 层持有，Data 层不依赖该文案。
 * @property apiValue 传给目录接口的排序枚举值，必须与服务端约定保持一致。
 * @property days 热度统计时间窗口，单位为天；`null` 表示该排序不限制固定时间窗口。
 */
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

/**
 * 发现页的完整可渲染状态。
 *
 * 状态由 [DiscoveryScreenModel] 维护，只保存目录浏览、分页和搜索所需的 UI 派生状态。
 * WebApp 任务提交、上传和历史不属于发现页状态，必须通过更窄的任务仓库处理。
 *
 * @property isLoading 是否正在执行首次页面初始化。
 * `true` 表示分类和首屏列表仍在加载，页面应展示整体加载态；`false` 表示初始化请求已结束。
 * @property isRefreshing 是否正在执行用户触发的刷新。
 * `true` 时刷新控件应保持激活；`false` 表示当前没有刷新请求。
 * @property banners 首页顶部推荐 WebApp 列表，来源于运营精选或后续推荐接口。
 * 当前空列表表示没有可展示 banner，不代表目录列表为空。
 * @property categories 一级标签列表，来源于 [WebAppCatalogRepository.getTagTree]。
 * 列表顺序保留服务端返回顺序，空列表表示当前没有可筛选分类。
 * @property selectedCategoryIndex 当前选中的一级分类下标。
 * `0` 表示选中第一个分类；当 [categories] 为空时该值不应被用于索引访问。
 * @property selectedSort 当前列表排序选项，默认使用推荐排序。
 * 修改该字段后必须重新加载第一页，避免不同排序结果混合在同一列表。
 * @property apps 当前发现列表的 WebApp 数据。
 * 列表顺序为服务端分页顺序；刷新或切换分类时清空，加载更多时追加。
 * @property isLoadingApps 是否正在加载发现列表页。
 * `true` 表示分类或排序变化后的主列表请求正在进行；`false` 表示没有主列表加载请求。
 * @property currentPage 当前主列表已成功加载的页码，从 1 开始。
 * `1` 表示首屏页；该值只在主列表请求成功后更新。
 * @property hasMore 主列表是否还有下一页。
 * `true` 表示允许触发加载更多；`false` 表示服务端分页已到末尾。
 * @property isLoadingMore 是否正在加载主列表下一页。
 * `true` 时必须阻止重复加载更多；`false` 表示可以根据 [hasMore] 决定是否继续分页。
 * @property error 等待页面展示的主列表错误信息。
 * `null` 表示当前没有主列表错误；非空通常来自网络或服务端业务错误，可由刷新动作重试。
 * @property isSearchExpanded 搜索框是否展开。
 * `true` 表示页面进入搜索交互态；`false` 表示展示常规发现列表。
 * @property searchQuery 用户当前输入的搜索关键词。
 * 空字符串表示尚未输入或已清空搜索；提交搜索前应去除首尾空白。
 * @property isSearching 是否正在执行搜索请求。
 * `true` 时搜索输入应避免重复提交；`false` 表示没有进行中的搜索请求。
 * @property searchResults 当前搜索结果列表。
 * 列表顺序为服务端搜索排序；空列表表示尚未搜索或当前关键词没有匹配结果。
 * @property searchPage 当前搜索结果已成功加载的页码，从 1 开始。
 * 主列表分页和搜索分页互不共享该值。
 * @property searchHasMore 搜索结果是否还有下一页。
 * `true` 表示允许继续加载搜索结果；`false` 表示当前关键词分页已结束。
 * @property searchError 等待页面展示的搜索错误信息。
 * `null` 表示当前没有搜索错误；非空时只影响搜索区域，不覆盖主列表错误。
 */
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
    val error: String? = null,
    val isSearchExpanded: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<WebApp> = emptyList(),
    val searchPage: Int = 1,
    val searchHasMore: Boolean = false,
    val searchError: String? = null,
)

private const val PAGE_SIZE = 30

/**
 * 发现页 ScreenModel。
 *
 * 本类只需要 WebApp 目录、标签和搜索能力，因此依赖 [WebAppCatalogRepository]，
 * 不再持有任务提交、上传或历史查询等更宽的 WebApp 仓库能力。这样可以逐步把目录浏览和
 * 工作流执行拆到不同领域边界。
 *
 * @param webAppRepository WebApp 目录仓库，用于加载分类、分页列表和搜索结果。
 */
class DiscoveryScreenModel(
    private val webAppRepository: WebAppCatalogRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var loadMoreJob: Job? = null

    /**
     * 加载发现页首屏数据。
     *
     * 该方法由页面的 `LaunchedEffect(Unit)` 触发，而不是在构造函数中启动协程，
     * 这样可以让 ScreenModel 初始化保持纯粹，并让页面生命周期决定首屏请求何时开始。
     */
    fun loadInitialData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categoriesDeferred = launch { loadCategories() }
                categoriesDeferred.join()

                val appsDeferred = launch { loadApps(page = 1, reset = true) }

                appsDeferred.join()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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

    /**
     * 选择发现页分类。
     *
     * 切换分类会取消正在进行的加载更多任务，并清空旧列表后重新加载第一页，
     * 避免不同分类的分页结果混合展示。
     *
     * @param index [categories] 中的分类下标；与当前下标相同会被忽略。
     */
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
            loadApps(page = 1, reset = true)
            _uiState.update { it.copy(isLoadingApps = false) }
        }
    }

    /**
     * 选择发现页排序方式。
     *
     * 排序变化会取消旧分页任务并重新加载第一页，因为不同排序的结果顺序不可追加复用。
     *
     * @param sort 用户选择的排序选项。
     */
    fun selectSort(sort: SortOption) {
        val state = _uiState.value
        if (sort == state.selectedSort) return

        loadMoreJob?.cancel()
        loadMoreJob = null

        _uiState.update {
            it.copy(
                selectedSort = sort,
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

    /**
     * 刷新当前分类和排序下的发现列表。
     *
     * 刷新只替换主列表第一页，不会修改当前分类和排序；请求结束后无论成功失败都会关闭刷新态。
     */
    fun refresh() {
        loadMoreJob?.cancel()
        loadMoreJob = null

        screenModelScope.launch {
            _uiState.update {
                it.copy(isRefreshing = true, isLoadingMore = false, error = null)
            }
            try {
                loadCategories()
                loadApps(page = 1, reset = true)
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 加载发现列表下一页。
     *
     * 当主列表正在加载、分页已结束或已有加载更多任务时直接返回，避免重复请求导致列表重复追加。
     */
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoadingApps || !state.hasMore || state.isLoading || state.isRefreshing) return

        val nextPage = state.currentPage + 1
        _uiState.update { it.copy(isLoadingMore = true) }

        loadMoreJob = screenModelScope.launch {
            loadApps(page = nextPage, reset = false)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    /**
     * 展开搜索交互区域。
     *
     * 展开只改变 UI 交互态，不会立即发起远端搜索；搜索请求由用户提交关键词触发。
     */
    fun expandSearch() {
        _uiState.update { it.copy(isSearchExpanded = true) }
    }

    /**
     * 收起搜索交互区域并清理搜索状态。
     *
     * 收起时保留主发现列表，清空搜索关键词、结果和搜索错误，避免旧搜索结果影响常规浏览。
     */
    fun collapseSearch() {
        _uiState.update {
            it.copy(
                isSearchExpanded = false,
                searchQuery = "",
                searchResults = emptyList(),
                searchPage = 1,
                searchHasMore = false,
                searchError = null,
            )
        }
    }

    /**
     * 更新搜索输入内容。
     *
     * 该方法只同步输入框状态，不自动防抖搜索；发现页搜索由显式提交触发，避免页面重组或输入中间态造成请求风暴。
     *
     * @param query 用户输入框中的原始关键词，允许为空字符串。
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * 提交搜索关键词并加载第一页结果。
     *
     * 空白关键词会清空搜索结果并停止搜索态；非空关键词会重置搜索分页，确保新结果不会与旧关键词结果混合。
     *
     * @param query 待提交的关键词，默认使用当前输入框内容。
     */
    fun searchSubmit(query: String = _uiState.value.searchQuery) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    searchResults = emptyList(),
                    searchPage = 1,
                    searchHasMore = false,
                    searchError = null,
                )
            }
            return
        }

        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    searchQuery = trimmed,
                    isSearching = true,
                    searchError = null,
                    searchPage = 1,
                )
            }
            webAppRepository.searchApps(
                keyword = trimmed,
                pageNum = 1,
                pageSize = PAGE_SIZE,
            ).onSuccess { pageData ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = pageData.records,
                        searchHasMore = pageData.hasNext,
                        searchError = null,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = emptyList(),
                        searchHasMore = false,
                        searchError = e.message ?: "搜索失败",
                    )
                }
            }
        }
    }

    /**
     * 加载当前搜索关键词的下一页结果。
     *
     * 当搜索请求进行中、没有下一页或关键词为空时直接返回，避免重复分页请求和无效搜索。
     */
    fun loadMoreSearchResults() {
        val state = _uiState.value
        if (state.isSearching || !state.searchHasMore || state.searchQuery.isBlank()) return

        val nextPage = state.searchPage + 1
        screenModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            webAppRepository.searchApps(
                keyword = state.searchQuery,
                pageNum = nextPage,
                pageSize = PAGE_SIZE,
            ).onSuccess { pageData ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchResults = it.searchResults + pageData.records,
                        searchPage = nextPage,
                        searchHasMore = pageData.hasNext,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = e.message ?: "搜索失败",
                    )
                }
            }
        }
    }

    private fun selectedTags(index: Int = _uiState.value.selectedCategoryIndex): List<String> {
        if (index <= 0) return emptyList()
        val level1Tag = _uiState.value.categories.getOrNull(index - 1) ?: return emptyList()
        return level1Tag.childTags?.map { it.id } ?: emptyList()
    }
}
