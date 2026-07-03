package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogQuery
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.domain.WebAppCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 发现页的完整可渲染状态。
 *
 * 状态由 [DiscoveryStateHolder] 维护，只保存目录浏览和分页所需的 UI 派生状态；
 * 搜索状态完全由独立搜索页的 [SearchStateHolder] 承担。
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
 * `0` 表示“全部”；`1` 表示 [categories] 的第一个分类。该约定与 UI 中固定的“全部”标签一致，
 * 因此访问真实分类时必须使用 `index - 1`。
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
 * @property error 等待页面展示的主列表稳定错误语义。
 * `null` 表示当前没有主列表错误；非空通常来自网络或服务端业务错误，可由刷新动作重试。
 * 该字段不得保存服务端 `msg` 或 [Throwable.message]，最终中文文案由应用壳资源映射。
 * @property loadMoreFailed 加载更多是否在最近一次尝试中失败，用于列表尾部错误提示与手动重试；
 * 首页加载失败仍走 [error] 字段。
 */
data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val banners: List<WebApp> = emptyList(),
    val categories: List<Tag> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val selectedSort: CatalogSort = CatalogSort.RECOMMEND,
    val apps: List<WebApp> = emptyList(),
    val isLoadingApps: Boolean = false,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: CatalogPresentationError? = null,
    val loadMoreFailed: Boolean = false,
) {
    /** 发现主列表的创作入口卡片语义，供应用壳直接渲染 AppCard。 */
    val appCards: List<DiscoveryAppCardUiModel>
        get() = apps.map { it.toDiscoveryAppCardUiModel() }
}

private const val PAGE_SIZE = 30

/**
 * 持有发现页目录浏览和分页状态。
 *
 * 搜索交互与搜索分页完全由独立搜索页的 [SearchStateHolder] 承担，本类不再保存任何搜索状态。
 * 本类位于 Discovery Presentation 层，只依赖 [WebAppCatalogRepository] 领域契约，
 * 不依赖 Compose、Voyager 或任何 Data 实现。应用壳负责传入与页面生命周期绑定的
 * [coroutineScope]，以便 Tab 切换或页面离栈时取消未完成的分页任务。
 *
 * @param webAppRepository WebApp 目录仓库，用于加载分类和分页列表。
 * @param coroutineScope 与页面生命周期绑定的协程作用域。
 */
class DiscoveryStateHolder(
    private val webAppRepository: WebAppCatalogRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    private var loadMoreJob: Job? = null
    // 主列表使用递增版本号隔离旧响应。Repository 请求通常无法保证取消后服务端不再返回，
    // 因此状态写入前必须确认响应仍属于用户最后一次选择的分类或排序。
    private var appListRequestVersion: Long = 0

    private fun nextAppListRequestVersion(): Long {
        appListRequestVersion += 1
        return appListRequestVersion
    }

    /**
     * 加载发现页首屏数据。
     *
     * 该方法由页面的 `LaunchedEffect(Unit)` 触发，而不是在构造函数中启动协程，
     * 这样可以让 StateHolder 初始化保持纯粹，并让页面生命周期决定首屏请求何时开始。
     */
    fun loadInitialData() {
        coroutineScope.launch {
            val requestVersion = nextAppListRequestVersion()
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categoriesDeferred = launch { loadCategories() }
                val bannersDeferred = launch { loadBanners(requestVersion) }
                categoriesDeferred.join()

                val appsDeferred = launch { loadApps(page = 1, reset = true, requestVersion = requestVersion) }

                bannersDeferred.join()
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

    private suspend fun loadBanners(requestVersion: Long) {
        webAppRepository.getCarefullyChosenList()
            .onSuccess { banners ->
                if (requestVersion != appListRequestVersion) return@onSuccess
                _uiState.update {
                    it.copy(
                        banners = banners.filter { app ->
                            app.id.isNotBlank() && app.title.isNotBlank()
                        },
                    )
                }
            }
            .onFailure {
                if (requestVersion != appListRequestVersion) return@onFailure
                _uiState.update { it.copy(banners = emptyList()) }
            }
    }

    private suspend fun loadApps(
        page: Int,
        reset: Boolean,
        requestVersion: Long,
    ) {
        val state = _uiState.value
        val tags = selectedTags()
        webAppRepository.getAppList(
            CatalogQuery(
                pageNum = page,
                pageSize = PAGE_SIZE,
                tagIds = tags,
                sort = state.selectedSort,
            )
        ).onSuccess { pageData ->
            if (requestVersion != appListRequestVersion) return@onSuccess
            _uiState.update {
                it.copy(
                    apps = if (reset) {
                        pageData.records
                    } else {
                        (it.apps + pageData.records).distinctBy { app -> app.id }
                    },
                    currentPage = page,
                    hasMore = pageData.hasNext,
                    isLoadingApps = false,
                    error = null,
                    loadMoreFailed = false,
                )
            }
        }.onFailure { e ->
            if (requestVersion != appListRequestVersion) return@onFailure
            _uiState.update {
                it.copy(
                    isLoadingApps = false,
                    error = if (reset) {
                        e.toCatalogPresentationError(CatalogPresentationError.LoadFailed)
                    } else {
                        it.error
                    },
                    loadMoreFailed = !reset,
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
     * @param index UI 分类下标；`0` 表示“全部”，`1` 才对应 [categories] 中的第一个真实分类。
     * 与当前下标相同会被忽略。
     */
    fun selectCategory(index: Int) {
        val state = _uiState.value
        if (index == state.selectedCategoryIndex) return

        loadMoreJob?.cancel()
        loadMoreJob = null
        val requestVersion = nextAppListRequestVersion()

        _uiState.update {
            it.copy(
                selectedCategoryIndex = index,
                apps = emptyList(),
                isLoadingApps = true,
                currentPage = 1,
                hasMore = true,
                isLoadingMore = false,
                error = null,
                loadMoreFailed = false,
            )
        }

        coroutineScope.launch {
            loadApps(page = 1, reset = true, requestVersion = requestVersion)
            if (requestVersion == appListRequestVersion) {
                _uiState.update { it.copy(isLoadingApps = false) }
            }
        }
    }

    /**
     * 选择发现页排序方式。
     *
     * 排序变化会取消旧分页任务并重新加载第一页，因为不同排序的结果顺序不可追加复用。
     *
     * @param sort 用户选择的排序选项。
     */
    fun selectSort(sort: CatalogSort) {
        val state = _uiState.value
        if (sort == state.selectedSort) return

        loadMoreJob?.cancel()
        loadMoreJob = null
        val requestVersion = nextAppListRequestVersion()

        _uiState.update {
            it.copy(
                selectedSort = sort,
                isLoadingApps = true,
                currentPage = 1,
                hasMore = true,
                isLoadingMore = false,
                error = null,
                loadMoreFailed = false,
            )
        }

        coroutineScope.launch {
            loadApps(page = 1, reset = true, requestVersion = requestVersion)
            if (requestVersion == appListRequestVersion) {
                _uiState.update { it.copy(isLoadingApps = false) }
            }
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
        val requestVersion = nextAppListRequestVersion()

        coroutineScope.launch {
            _uiState.update {
                it.copy(isRefreshing = true, isLoadingMore = false, error = null, loadMoreFailed = false)
            }
            try {
                loadCategories()
                loadBanners(requestVersion)
                loadApps(page = 1, reset = true, requestVersion = requestVersion)
            } finally {
                if (requestVersion == appListRequestVersion) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
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
        val requestVersion = appListRequestVersion
        _uiState.update { it.copy(isLoadingMore = true) }

        loadMoreJob = coroutineScope.launch {
            loadApps(page = nextPage, reset = false, requestVersion = requestVersion)
            if (requestVersion == appListRequestVersion) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private fun selectedTags(index: Int = _uiState.value.selectedCategoryIndex): List<String> {
        if (index <= 0) return emptyList()
        val level1Tag = _uiState.value.categories.getOrNull(index - 1) ?: return emptyList()
        return level1Tag.childTags?.map { it.id } ?: emptyList()
    }
}
