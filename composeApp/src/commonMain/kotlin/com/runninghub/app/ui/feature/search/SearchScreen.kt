package com.runninghub.app.ui.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewSearchUiState
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.components.inputs.RhSearchBar
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBar
import com.runninghub.app.ui.designsystem.components.states.RhEmptyState
import com.runninghub.app.ui.designsystem.components.states.RhErrorState
import com.runninghub.app.ui.designsystem.components.states.RhLoadingState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryAppCard
import com.runninghub.core.model.Tag
import com.runninghub.feature.discovery.presentation.CatalogPresentationError
import com.runninghub.feature.discovery.presentation.SearchUiState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_search_bar_clear_content_description
import runninghub.composeapp.generated.resources.app_search_bar_search_content_description
import runninghub.composeapp.generated.resources.discovery_error_empty_response
import runninghub.composeapp.generated.resources.discovery_error_load_failed
import runninghub.composeapp.generated.resources.discovery_error_search_failed
import runninghub.composeapp.generated.resources.discovery_error_service_unavailable
import runninghub.composeapp.generated.resources.search_back_content_description
import runninghub.composeapp.generated.resources.search_bar_placeholder
import runninghub.composeapp.generated.resources.search_empty_results_format
import runninghub.composeapp.generated.resources.search_end_of_results
import runninghub.composeapp.generated.resources.search_error_retry
import runninghub.composeapp.generated.resources.search_hot_content_description
import runninghub.composeapp.generated.resources.search_hot_tags_title
import runninghub.composeapp.generated.resources.search_loading
import runninghub.composeapp.generated.resources.search_screen_title

/**
 * 搜索页的 Voyager Screen。
 *
 * 该类型只负责把 Voyager 生命周期、导航返回和详情跳转适配到 [SearchScreenModel]；
 * 搜索输入、防抖、分页和热门标签状态由 `feature:discovery:presentation` 维护。
 */
class SearchVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SearchScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHotTags() }

        SearchContent(
            uiState = uiState,
            onQueryChange = screenModel::onQueryChange,
            onSearch = screenModel::search,
            onTagClick = screenModel::searchByTag,
            onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
            onBack = { navigator.pop() },
            onLoadMore = screenModel::loadMore,
        )
    }
}

/**
 * 搜索页内容。顶栏、搜索框与状态区全部使用封板设计系统组件，
 * 颜色只从 [RhTheme.colors] 读取，避免旧 Material 暗色 scheme 残留。
 */
@Composable
private fun SearchContent(
    uiState: SearchUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onTagClick: (Tag) -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val colors = RhTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradientBrush),
    ) {
        RhTopBar(
            title = stringResource(Res.string.search_screen_title),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.search_back_content_description),
                        tint = colors.textPrimary,
                    )
                }
            },
        )

        RhSearchBar(
            query = uiState.query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            placeholder = stringResource(Res.string.search_bar_placeholder),
            searchIconContentDescription = stringResource(Res.string.app_search_bar_search_content_description),
            clearContentDescription = stringResource(Res.string.app_search_bar_clear_content_description),
            modifier = Modifier.padding(horizontal = RhSpacing.lg, vertical = RhSpacing.sm),
        )

        // Presentation 只输出稳定错误语义，应用壳在靠近 UI 的位置映射本地化文案。
        val errorMessage = uiState.error?.let { catalogPresentationErrorMessage(it) }

        when {
            // 搜索中或防抖等待中且尚无结果时只展示加载态，
            // 避免旧结果与新请求状态混在一起，也避免防抖窗口内误闪「无结果」空态。
            (uiState.isSearching || (uiState.query.isNotBlank() && !uiState.hasSearched)) &&
                uiState.results.isEmpty() -> {
                CenteredStateBox {
                    RhLoadingState(title = stringResource(Res.string.search_loading))
                }
            }

            // 仅在没有可展示结果时显示错误整页；已有结果时保留列表，错误由网格尾部的错误行提示。
            errorMessage != null && uiState.results.isEmpty() -> {
                CenteredStateBox {
                    RhErrorState(
                        title = errorMessage,
                        actionLabel = stringResource(Res.string.search_error_retry),
                        onAction = { onSearch(uiState.query) },
                    )
                }
            }

            // 没有输入关键词时回到热词入口，保留已加载热词供用户继续探索。
            uiState.query.isBlank() -> {
                HotTagsSection(
                    tags = uiState.hotTags,
                    onTagClick = onTagClick,
                )
            }

            // 有关键词、已完成过一次搜索且没有结果时才展示空态，防抖等待中不落入此分支。
            uiState.results.isEmpty() && uiState.hasSearched -> {
                CenteredStateBox {
                    RhEmptyState(
                        title = stringResource(Res.string.search_empty_results_format, uiState.query),
                    )
                }
            }

            else -> {
                SearchResultsGrid(
                    uiState = uiState,
                    onAppClick = onAppClick,
                    onLoadMore = onLoadMore,
                )
            }
        }
    }
}

/** 整页状态（加载、错误、空态）的居中容器。 */
@Composable
private fun CenteredStateBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 搜索结果自适应网格。卡片语义复用发现页的 [DiscoveryAppCard]，
 * 列宽跟随窗口信息中的 feed 列宽，保证与发现页一致的双列密度。
 */
@Composable
private fun SearchResultsGrid(
    uiState: SearchUiState,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val windowInfo = LocalRhWindowInfo.current
    val gridState = rememberLazyGridState()
    val cards = uiState.resultCards
    val loadMoreError = uiState.error

    // 关键词变化即代表一轮新搜索，结果列表会被整页替换，滚动位置需要回到顶部。
    LaunchedEffect(uiState.query) {
        gridState.scrollToItem(0)
    }

    // 使用可见项判断触发分页（发现页将在本批同步切换到同款可见项判断），
    // 替换按 page 变化触发的 LaunchedEffect(page) 反模式：
    // 后者会在结果替换、旋转等场景下重复请求，且无法感知用户是否真的滚到了列表尾部。
    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    // 同时以卡片数量作为 key：去重后一页净增不超过 3 条时 shouldLoadMore 不会翻转，
    // 只监听 Boolean 会导致分页停摆；追加数量变化后需要重新评估是否继续加载。
    LaunchedEffect(shouldLoadMore, cards.size) {
        if (shouldLoadMore && uiState.results.isNotEmpty()) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = windowInfo.feedGridMinCardWidth),
        contentPadding = PaddingValues(
            start = RhSpacing.lg,
            end = RhSpacing.lg,
            top = RhSpacing.sm,
            bottom = RhSpacing.xl,
        ),
        horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            count = cards.size,
            key = { index -> cards[index].id },
        ) { index ->
            val card = cards[index]
            DiscoveryAppCard(
                card = card,
                onClick = { onAppClick(card.id) },
            )
        }

        if (uiState.isSearching && uiState.results.isNotEmpty()) {
            item(key = "search_loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = RhTheme.colors.brandPrimary,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        // 加载更多失败时不清空已有结果，在网格尾部提示错误并提供重试入口。
        if (loadMoreError != null && uiState.results.isNotEmpty()) {
            item(key = "search_load_more_error", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = catalogPresentationErrorMessage(loadMoreError),
                        style = RhTypography.caption,
                        color = RhTheme.colors.statusFailed,
                    )
                    Spacer(Modifier.padding(start = RhSpacing.sm))
                    Text(
                        text = stringResource(Res.string.search_error_retry),
                        style = RhTypography.caption,
                        color = RhTheme.colors.brandPrimary,
                        modifier = Modifier.clickable(onClick = onLoadMore),
                    )
                }
            }
        }

        if (!uiState.hasMore && uiState.results.isNotEmpty()) {
            item(key = "search_end", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.search_end_of_results),
                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                    textAlign = TextAlign.Center,
                    style = RhTypography.caption,
                    color = RhTheme.colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun catalogPresentationErrorMessage(error: CatalogPresentationError): String =
    when (error) {
        CatalogPresentationError.LoadFailed -> stringResource(Res.string.discovery_error_load_failed)
        CatalogPresentationError.SearchFailed -> stringResource(Res.string.discovery_error_search_failed)
        CatalogPresentationError.ServiceUnavailable -> stringResource(
            Res.string.discovery_error_service_unavailable
        )
        CatalogPresentationError.EmptyResponse -> stringResource(Res.string.discovery_error_empty_response)
    }

/** 热门标签区。标签横向滚动排列，避免使用实验性流式布局 API。 */
@Composable
private fun HotTagsSection(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onTagClick: (Tag) -> Unit = {},
) {
    if (tags.isEmpty()) return

    Column(
        modifier = modifier.padding(
            horizontal = RhSpacing.lg,
            vertical = RhSpacing.md,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = stringResource(Res.string.search_hot_content_description),
                tint = RhTheme.colors.statusWarning,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.padding(start = RhSpacing.xs))
            Text(
                text = stringResource(Res.string.search_hot_tags_title),
                style = RhTypography.sectionTitle,
                color = RhTheme.colors.textPrimary,
            )
        }

        Spacer(Modifier.height(RhSpacing.md))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
        ) {
            tags.forEach { tag ->
                RhChip(
                    label = tag.name,
                    onClick = { onTagClick(tag) },
                )
            }
        }
    }
}

@Composable
private fun SearchAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        SearchContent(uiState = previewSearchUiState())
    }
}

@Preview
@Composable
private fun SearchPhone320Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun SearchPhone360Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun SearchPhone430Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun SearchMedium600Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun SearchExpanded840Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun SearchLandscapePreview() {
    SearchAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun SearchFontScale13Preview() {
    SearchAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun SearchFontScale15Preview() {
    SearchAdaptivePreview(RhPreviewSpec.FontScale15)
}
