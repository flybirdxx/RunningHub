package com.runninghub.feature.community.presentation

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaRepository
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 社区广场页面的内容模式。
 *
 * 该枚举属于 Community Presentation 契约，UI 只根据模式选择展示灵感瀑布流或短片列表；
 * 远端 API 的具体 endpoint、分类编码和分页协议仍由 Community Data 层处理。
 */
enum class PlazaMode {
    /**
     * 展示社区灵感创作内容。
     *
     * 该模式使用标签和排序筛选，数据来源为 [PlazaRepository.listCreations]。
     */
    CREATIONS,

    /**
     * 展示社区短片内容。
     *
     * 该模式使用短片分类筛选，数据来源为 [PlazaRepository.listShorts]。
     */
    SHORTS,
}

/**
 * 社区广场页面可展示的稳定错误语义。
 *
 * 该枚举属于 Community Presentation 层，只描述 UI 可以理解的失败类别，不携带服务端 `msg`、
 * Data 层异常消息或本地诊断字符串。最终用户可见文案由应用壳通过 Compose Resources 映射。
 */
enum class PlazaPresentationError {
    /**
     * 灵感创作列表加载失败。
     *
     * 首屏失败时页面可能展示本地 fallback 内容；分页或刷新失败时页面可保留旧数据并允许用户重试。
     */
    CreationsLoadFailed,

    /**
     * 短片列表加载失败。
     *
     * 该错误只影响短片模式，不代表灵感创作列表或标签加载失败。
     */
    ShortsLoadFailed,
}

/**
 * Plaza 本地 fallback 标签的稳定文案语义。
 *
 * 该枚举只标识客户端内置降级内容目录中的标签文案；服务端返回的标签名称仍作为运行时内容展示，
 * 不进入 Compose Resources 静态文案映射。
 */
enum class PlazaFallbackTagLabel {
    /**
     * 图片类 fallback 内容标签。
     */
    Images,

    /**
     * 视频类 fallback 内容标签。
     */
    Videos,

    /**
     * 工作流类 fallback 内容标签。
     */
    Workflows,
}

/**
 * Plaza 本地 fallback 卡片简介的稳定文案语义。
 *
 * 该枚举只用于远端灵感首屏失败时的客户端内置示例内容；服务端返回的卡片简介仍保持运行时文本。
 */
enum class PlazaFallbackCardIntro {
    /**
     * All Power Image V2 图片提示词示例。
     */
    ImageV2PromptGallery,

    /**
     * Seedream V5 Lite 文生图示例。
     */
    SeedreamLiteTextToImage,

    /**
     * 文档采集视频工作流示例。
     */
    VideoWorkflowFromDocs,

    /**
     * API 模型调用工作流示例。
     */
    ReusableApiWorkflow,
}

/**
 * Plaza 本地 fallback 卡片作者的稳定文案语义。
 *
 * 该枚举只覆盖客户端内置示例内容的作者展示；服务端作者名仍作为运行时内容展示。
 */
enum class PlazaFallbackCardOwner {
    /**
     * RunningHub API 示例作者。
     */
    RunningHubApi,

    /**
     * RunningHub 创作者示例作者。
     */
    RunningHubCreator,
}

/**
 * Plaza 本地 fallback 卡片媒体类型的稳定文案语义。
 *
 * 该枚举只覆盖客户端内置示例内容的媒体类型展示；服务端媒体类型仍作为运行时内容展示。
 */
enum class PlazaFallbackCardMediaType {
    /**
     * 图片示例媒体类型。
     */
    Image,

    /**
     * 视频示例媒体类型。
     */
    Video,

    /**
     * 工作流示例媒体类型。
     */
    Workflow,
}

/**
 * Plaza 本地 fallback 卡片的资源化文案索引。
 *
 * @property intro 卡片简介语义，由 composeApp 映射为最终展示文案。
 * @property owner 作者名称语义，由 composeApp 映射为最终展示文案。
 * @property mediaType 媒体类型语义，由 composeApp 映射为最终展示文案。
 */
data class PlazaFallbackCardText(
    val intro: PlazaFallbackCardIntro,
    val owner: PlazaFallbackCardOwner,
    val mediaType: PlazaFallbackCardMediaType,
)

/**
 * 社区广场页面的完整可渲染状态。
 *
 * 状态由 [PlazaStateHolder] 维护，只承载页面展示、筛选和分页状态；网络请求、
 * DTO 兼容和错误映射由 [PlazaRepository] 的实现层负责。该状态不得保存 Token、
 * Cookie、API Key 或平台对象。
 *
 * @property isLoading 灵感创作首屏是否正在加载。`true` 表示灵感列表为空且正在请求首屏；
 * `false` 表示当前没有灵感首屏加载任务，不代表短片列表加载状态。
 * @property isRefreshing 灵感创作是否正在执行下拉或显式刷新。`true` 时 UI 可展示刷新状态；
 * 请求成功、失败或被页面作用域取消后应恢复为 `false`。
 * @property isLoadingMore 灵感创作是否正在加载下一页。`true` 时加载更多入口应避免重复点击；
 * `false` 表示当前没有灵感分页请求。
 * @property mode 当前广场内容模式。默认 [PlazaMode.CREATIONS] 表示展示灵感创作；
 * [PlazaMode.SHORTS] 表示展示短片列表。
 * @property tags 灵感创作筛选标签，来源于服务端标签接口并过滤为一级可用标签；
 * 顺序按服务端返回保留，空集合表示标签尚未加载或服务端没有可用标签。
 * @property fallbackTagLabels 本地 fallback 标签 ID 到资源化文案语义的映射。空集合表示当前标签来自
 * 服务端或 UI 静态兜底；非空时 UI 应优先使用该映射展示内置标签文案。
 * @property selectedTagId 当前选中的灵感标签 ID，来源于用户点击；`null` 表示不过滤标签，
 * 与空字符串“无效标签 ID”不同。
 * @property sort 当前灵感排序协议值，来源于 UI 分段控件；默认 `RECOMMEND` 表示推荐排序。
 * 该字符串仍是社区接口协议值，后续可迁移为 Community Domain 枚举。
 * @property creations 当前灵感创作卡片列表，来源于服务端分页或本地 fallback；
 * 顺序用于瀑布流展示，空集合表示尚无可展示创作。
 * @property fallbackCreationTexts 本地 fallback 创作 ID 到资源化文案语义的映射。空集合表示当前创作来自
 * 服务端；非空时 UI 应优先使用该映射展示内置卡片简介、作者和媒体类型。
 * @property currentPage 已加载的灵感页码，从 1 开始；0 表示尚未成功加载任何灵感页。
 * @property total 灵感创作总数，单位为条；0 表示服务端未返回总数或当前无数据。
 * @property hasMore 灵感创作是否还有下一页。`true` 表示可以继续请求下一页；
 * `false` 表示没有更多数据或 fallback 内容已完整展示。
 * @property shortCategories 短片分类列表，来源于服务端短片分类接口；顺序按服务端配置保留，
 * 空集合表示分类尚未加载或服务端没有分类。
 * @property selectedShortCategoryCode 当前选中的短片分类编码，来源于用户点击；`null` 表示全部短片。
 * @property shorts 当前短片卡片列表，来源于服务端分页；顺序按服务端返回保留。
 * 空集合表示尚无可展示短片。
 * @property shortPage 已加载的短片页码，从 1 开始；0 表示尚未成功加载任何短片页。
 * @property isShortsLoading 短片分类或短片页是否正在加载。`true` 时短片加载入口应避免重复点击；
 * `false` 表示当前没有短片请求。
 * @property error 当前等待页面展示的稳定错误语义。
 * `null` 表示没有错误；非空时由 UI 按当前列表区域映射最终文案。
 * 该字段不得保存服务端 `msg` 或 [Throwable.message]；fallback 内容可与错误同时存在，
 * 用于提示当前展示的是降级数据。
 */
data class PlazaUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val mode: PlazaMode = PlazaMode.CREATIONS,
    val tags: List<PlazaTag> = emptyList(),
    val fallbackTagLabels: Map<String, PlazaFallbackTagLabel> = emptyMap(),
    val selectedTagId: String? = null,
    val sort: String = "RECOMMEND",
    val creations: List<PlazaCreationCard> = emptyList(),
    val fallbackCreationTexts: Map<String, PlazaFallbackCardText> = emptyMap(),
    val currentPage: Int = 0,
    val total: Int = 0,
    val hasMore: Boolean = true,
    val shortCategories: List<PlazaShortCategory> = emptyList(),
    val selectedShortCategoryCode: String? = null,
    val shorts: List<PlazaShortCard> = emptyList(),
    val shortPage: Int = 0,
    val isShortsLoading: Boolean = false,
    val error: PlazaPresentationError? = null,
)

/**
 * 协调社区广场页面的加载、筛选和分页状态。
 *
 * 本类是从 composeApp PlazaScreenModel 中拆出的 Community Presentation 状态持有者。
 * 它只依赖 Community Domain 的 [PlazaRepository] 和外部注入的页面生命周期作用域，
 * 不依赖 Compose、Voyager、Ktor、DataStore 或平台 SDK。
 *
 * 并发约束：
 * - 所有异步任务都挂在 [scope]，页面销毁时由调用方取消作用域。
 * - 灵感分页通过 [PlazaUiState.isLoadingMore] 防止重复加载更多。
 * - 短片分页通过 [PlazaUiState.isShortsLoading] 防止重复加载更多。
 *
 * @param plazaRepository 社区领域仓库，负责提供标签、灵感创作、短片分类和短片列表。
 * @param scope 页面生命周期协程作用域，通常来自 composeApp 的 ScreenModel。
 */
class PlazaStateHolder(
    private val plazaRepository: PlazaRepository,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(PlazaUiState())

    /**
     * 社区广场页面的只读状态流。
     *
     * UI 应通过 collect 订阅该状态，并通过本类公开方法提交用户意图。
     */
    val uiState: StateFlow<PlazaUiState> = _uiState.asStateFlow()

    /**
     * 加载社区广场首屏数据。
     *
     * 首屏先请求标签，再请求第一页灵感创作；标签失败不会阻断创作列表加载。
     */
    fun loadInitialData() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tagsResult = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { plazaRepository.getTags() }
                ?: Result.failure(IllegalStateException("Plaza tags request timed out"))
            tagsResult.onSuccess { tags ->
                _uiState.update {
                    it.copy(
                        tags = tags.filter { tag -> tag.level <= 1 && tag.enable },
                        fallbackTagLabels = emptyMap(),
                    )
                }
            }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    /**
     * 刷新当前灵感创作列表。
     *
     * 当前标签和排序筛选会被保留，成功后页码回到第一页。
     */
    fun refreshCreations() {
        scope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    /**
     * 加载下一页灵感创作。
     *
     * 当已有请求进行中或 [PlazaUiState.hasMore] 为 `false` 时直接忽略，避免重复分页请求。
     */
    fun loadMoreCreations() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        scope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            loadCreations(page = state.currentPage + 1, append = true, fallbackOnFailure = false)
        }
    }

    /**
     * 切换灵感标签筛选。
     *
     * @param tagId 用户选择的标签 ID；`null` 表示清空标签筛选并展示全部灵感。
     */
    fun selectTag(tagId: String?) {
        _uiState.update { it.copy(selectedTagId = tagId, currentPage = 0, hasMore = true) }
        scope.launch {
            _uiState.update { it.copy(isLoading = it.creations.isEmpty(), error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    /**
     * 切换灵感排序。
     *
     * @param sort 社区接口排序协议值，例如 `RECOMMEND`、`HOT` 或 `LATEST`。
     */
    fun selectSort(sort: String) {
        _uiState.update { it.copy(sort = sort, currentPage = 0, hasMore = true) }
        scope.launch {
            _uiState.update { it.copy(isLoading = it.creations.isEmpty(), error = null) }
            loadCreations(page = 1, append = false, fallbackOnFailure = true)
        }
    }

    /**
     * 切换社区广场内容模式。
     *
     * 首次进入短片模式时会自动加载短片分类和第一页短片。
     */
    fun selectMode(mode: PlazaMode) {
        _uiState.update { it.copy(mode = mode) }
        if (mode == PlazaMode.SHORTS && _uiState.value.shorts.isEmpty()) {
            loadShorts()
        }
    }

    /**
     * 加载短片分类和第一页短片。
     *
     * 分类失败不会阻断短片列表加载，便于服务端分类接口波动时仍展示默认短片流。
     */
    fun loadShorts() {
        scope.launch {
            _uiState.update { it.copy(isShortsLoading = true, shortPage = 0, error = null) }
            val categoriesResult = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { plazaRepository.listShortCategories() }
                ?: Result.failure(IllegalStateException("Short categories request timed out"))
            categoriesResult.onSuccess { categories ->
                _uiState.update { it.copy(shortCategories = categories) }
            }
            loadShortPage(page = 1, append = false)
        }
    }

    /**
     * 切换短片分类筛选。
     *
     * @param categoryCode 服务端短片分类编码；`null` 表示展示全部短片。
     */
    fun selectShortCategory(categoryCode: String?) {
        _uiState.update {
            it.copy(
                selectedShortCategoryCode = categoryCode,
                shorts = emptyList(),
                shortPage = 0,
                isShortsLoading = true,
                error = null,
            )
        }
        scope.launch { loadShortPage(page = 1, append = false) }
    }

    /**
     * 加载下一页短片。
     *
     * 当前没有短片总数字段，分页入口只防止并发重复请求；是否展示入口由 UI 决定。
     */
    fun loadMoreShorts() {
        val state = _uiState.value
        if (state.isShortsLoading) return
        val nextPage = state.shortPage + 1
        _uiState.update { it.copy(isShortsLoading = true, error = null) }
        scope.launch { loadShortPage(page = nextPage, append = true) }
    }

    private suspend fun loadCreations(page: Int, append: Boolean, fallbackOnFailure: Boolean) {
        val state = _uiState.value
        val selectedTags = state.selectedTagId?.let(::listOf).orEmpty()
        val result = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            plazaRepository.listCreations(page = page, sort = state.sort, tags = selectedTags)
        } ?: Result.failure(IllegalStateException("Plaza creations request timed out"))

        result
            .onSuccess { creationPage ->
                val merged = if (append) state.creations + creationPage.items else creationPage.items
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        creations = merged,
                        fallbackCreationTexts = emptyMap(),
                        currentPage = creationPage.page,
                        total = creationPage.total,
                        hasMore = merged.size < creationPage.total && creationPage.items.isNotEmpty(),
                        error = null,
                    )
                }
            }
            .onFailure { _ ->
                if (fallbackOnFailure && _uiState.value.creations.isEmpty()) {
                    applyFallbackContent(PlazaPresentationError.CreationsLoadFailed)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            error = PlazaPresentationError.CreationsLoadFailed,
                        )
                    }
                }
            }
    }

    private suspend fun loadShortPage(page: Int, append: Boolean) {
        val state = _uiState.value
        val result = withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) {
            plazaRepository.listShorts(page = page, categoryCode = state.selectedShortCategoryCode)
        } ?: Result.failure(IllegalStateException("Short list request timed out"))

        result
            .onSuccess { cards ->
                _uiState.update {
                    it.copy(
                        isShortsLoading = false,
                        shorts = if (append) it.shorts + cards else cards,
                        shortPage = page,
                        error = null,
                    )
                }
            }
            .onFailure { _ ->
                _uiState.update {
                    it.copy(
                        isShortsLoading = false,
                        error = PlazaPresentationError.ShortsLoadFailed,
                    )
                }
            }
    }

    private fun applyFallbackContent(error: PlazaPresentationError) {
        val state = _uiState.value
        val cards = PlazaFallbackCatalog.creations(state.selectedTagId, state.sort)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                tags = if (it.tags.isEmpty()) PlazaFallbackCatalog.tags else it.tags,
                fallbackTagLabels = if (it.tags.isEmpty()) PlazaFallbackCatalog.tagLabels else it.fallbackTagLabels,
                creations = cards,
                fallbackCreationTexts = PlazaFallbackCatalog.cardTexts(cards),
                currentPage = 1,
                total = cards.size,
                hasMore = false,
                error = error,
            )
        }
    }

    private companion object {
        private const val REQUEST_TIMEOUT_MILLIS = 6_000L
    }
}

private object PlazaFallbackCatalog {
    val tags = listOf(
        PlazaTag(id = "image", name = "image", level = 1),
        PlazaTag(id = "video", name = "video", level = 1),
        PlazaTag(id = "workflow", name = "workflow", level = 1),
    )

    val tagLabels = mapOf(
        "image" to PlazaFallbackTagLabel.Images,
        "video" to PlazaFallbackTagLabel.Videos,
        "workflow" to PlazaFallbackTagLabel.Workflows,
    )

    private val cards = listOf(
        FallbackCard(
            tagId = "image",
            card = PlazaCreationCard(
                id = "fallback-image-v2",
                likeCount = "128",
                useCount = "42",
            ),
            text = PlazaFallbackCardText(
                intro = PlazaFallbackCardIntro.ImageV2PromptGallery,
                owner = PlazaFallbackCardOwner.RunningHubApi,
                mediaType = PlazaFallbackCardMediaType.Image,
            ),
        ),
        FallbackCard(
            tagId = "image",
            card = PlazaCreationCard(
                id = "fallback-seedream",
                likeCount = "96",
                useCount = "35",
            ),
            text = PlazaFallbackCardText(
                intro = PlazaFallbackCardIntro.SeedreamLiteTextToImage,
                owner = PlazaFallbackCardOwner.RunningHubApi,
                mediaType = PlazaFallbackCardMediaType.Image,
            ),
        ),
        FallbackCard(
            tagId = "video",
            card = PlazaCreationCard(
                id = "fallback-video-workflow",
                likeCount = "64",
                useCount = "18",
            ),
            text = PlazaFallbackCardText(
                intro = PlazaFallbackCardIntro.VideoWorkflowFromDocs,
                owner = PlazaFallbackCardOwner.RunningHubCreator,
                mediaType = PlazaFallbackCardMediaType.Video,
            ),
        ),
        FallbackCard(
            tagId = "workflow",
            card = PlazaCreationCard(
                id = "fallback-workflow",
                likeCount = "51",
                useCount = "27",
            ),
            text = PlazaFallbackCardText(
                intro = PlazaFallbackCardIntro.ReusableApiWorkflow,
                owner = PlazaFallbackCardOwner.RunningHubCreator,
                mediaType = PlazaFallbackCardMediaType.Workflow,
            ),
        ),
    )

    fun creations(tagId: String?, sort: String): List<PlazaCreationCard> {
        val filtered = cards.filter { tagId == null || it.tagId == tagId }.map { it.card }
        return when (sort) {
            "LATEST" -> filtered.asReversed()
            "HOT" -> filtered.sortedByDescending { it.likeCount?.toIntOrNull() ?: 0 }
            else -> filtered
        }
    }

    fun cardTexts(cards: List<PlazaCreationCard>): Map<String, PlazaFallbackCardText> {
        val requestedIds = cards.mapTo(mutableSetOf()) { it.id }
        return this.cards
            .filter { it.card.id in requestedIds }
            .associate { it.card.id to it.text }
    }

    private data class FallbackCard(
        val tagId: String,
        val card: PlazaCreationCard,
        val text: PlazaFallbackCardText,
    )
}
