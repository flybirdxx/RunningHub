package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.designsystem.components.states.RhEmptyState
import com.runninghub.app.ui.designsystem.components.states.RhErrorState
import com.runninghub.app.ui.designsystem.components.states.RhLoadingState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.search.SearchVoyagerScreen
import com.runninghub.app.ui.theme.adaptiveGridSpacing
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.presentation.CatalogPresentationError
import com.runninghub.feature.discovery.presentation.DiscoveryUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_empty_apps
import runninghub.composeapp.generated.resources.discovery_end_of_results
import runninghub.composeapp.generated.resources.discovery_error_empty_response
import runninghub.composeapp.generated.resources.discovery_error_load_failed
import runninghub.composeapp.generated.resources.discovery_error_retry
import runninghub.composeapp.generated.resources.discovery_error_search_failed
import runninghub.composeapp.generated.resources.discovery_error_service_unavailable
import runninghub.composeapp.generated.resources.discovery_loading

/**
 * 发现页 Voyager 入口。顶栏搜索图标跳转独立搜索页，
 * 页面本体只做目录浏览（banner、分类、排序与应用网格）。
 */
class DiscoveryVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<DiscoveryScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadInitialData() }

        DiscoveryContent(
            uiState = uiState,
            onSearchClick = { navigator.push(SearchVoyagerScreen()) },
            onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
            onCategorySelected = screenModel::selectCategory,
            onSortSelected = screenModel::selectSort,
            onRefresh = screenModel::refresh,
            onLoadMore = screenModel::loadMore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiscoveryContent(
    uiState: DiscoveryUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onCategorySelected: (Int) -> Unit = {},
    onSortSelected: (CatalogSort) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val windowSizeClass = rememberWindowSizeClass()
    val windowInfo = LocalRhWindowInfo.current
    val gridSpacing: Dp = adaptiveGridSpacing(windowSizeClass)
    val colors = RhTheme.colors

    Scaffold(
        topBar = { DiscoveryTopBar(onSearchClick = onSearchClick) },
        containerColor = colors.backgroundPrimary,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { padding ->

        if (uiState.isLoading && uiState.apps.isEmpty() && uiState.banners.isEmpty() && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                RhLoadingState(title = stringResource(Res.string.discovery_loading))
            }
            return@Scaffold
        }

        val discoveryError = uiState.error?.let { catalogPresentationErrorMessage(it) }
        if (discoveryError != null && uiState.apps.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                RhErrorState(
                    title = discoveryError,
                    actionLabel = stringResource(Res.string.discovery_error_retry),
                    onAction = onRefresh,
                )
            }
            return@Scaffold
        }

        val pullRefreshState = rememberPullToRefreshState()
        val gridState = rememberLazyGridState()
        val cards = uiState.appCards

        // 切换分类或排序后回到列表顶部，避免滚动位置残留在列表中部，
        // 甚至因残留位置靠近底部而未滚动就误触发下一页加载。
        LaunchedEffect(uiState.selectedCategoryIndex, uiState.selectedSort) {
            gridState.scrollToItem(0)
        }

        // 基于可见项判断加载更多，替换旧的 LaunchedEffect(currentPage) 反模式，
        // 避免页码更新即刻链式触发下一页请求。
        val shouldLoadMore by remember {
            derivedStateOf {
                val info = gridState.layoutInfo
                val total = info.totalItemsCount
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                // 阈值 -4 比搜索页的 -3 多一行：发现页网格包含 banner 与分类
                // 两个满行 header，预取窗口相应放大一行才能提前等量触发。
                total > 0 && lastVisible >= total - 4
            }
        }
        // 双 key：列表增长后即使 shouldLoadMore 保持 true 也能再次触发下一页。
        LaunchedEffect(shouldLoadMore, cards.size) {
            if (shouldLoadMore && uiState.apps.isNotEmpty()) onLoadMore()
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyVerticalGrid(
                state = gridState,
                // 360dp 设备如果继续用 180dp 作为最小列宽，会在包含间距后退成单列，
                // 单张竖图卡片接近整屏高度；这里复用窗口信息中的 feed 列宽，让常见手机保持两列密度。
                columns = GridCells.Adaptive(minSize = windowInfo.feedGridMinCardWidth),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(LayoutDirection.Ltr),
                    top = 0.dp,
                    bottom = RhSpacing.xxl,
                ),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (uiState.banners.isNotEmpty()) {
                    item(key = "featured_banners", span = { GridItemSpan(maxLineSpan) }) {
                        DiscoveryBannerSection(
                            banners = uiState.banners,
                            windowSizeClass = windowSizeClass,
                            onAppClick = onAppClick,
                        )
                    }
                }

                item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        DiscoveryCategoryRow(
                            categories = uiState.categories,
                            selectedIndex = uiState.selectedCategoryIndex,
                            onSelected = onCategorySelected,
                        )
                        DiscoverySortRow(
                            selectedSort = uiState.selectedSort,
                            onSortSelected = onSortSelected,
                        )
                    }
                }

                if (uiState.isLoadingApps && uiState.apps.isEmpty()) {
                    item(key = "content_loading", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            RhLoadingState(title = stringResource(Res.string.discovery_loading))
                        }
                    }
                } else if (uiState.apps.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            RhEmptyState(title = stringResource(Res.string.discovery_empty_apps))
                        }
                    }
                } else {
                    items(
                        count = cards.size,
                        key = { idx -> cards[idx].id },
                    ) { idx ->
                        DiscoveryAppCard(
                            card = cards[idx],
                            onClick = { onAppClick(cards[idx].id) },
                        )
                    }

                    item(key = "list_footer", span = { GridItemSpan(maxLineSpan) }) {
                        when {
                            uiState.isLoadingMore || uiState.isLoadingApps -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = colors.brandPrimary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                            // 加载更多失败时不清空已有列表，在网格尾部提示错误并提供手动重试入口。
                            uiState.loadMoreFailed -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.discovery_error_load_failed),
                                        style = RhTypography.caption,
                                        color = colors.statusFailed,
                                    )
                                    Spacer(Modifier.padding(start = RhSpacing.sm))
                                    Text(
                                        text = stringResource(Res.string.discovery_error_retry),
                                        style = RhTypography.caption,
                                        color = colors.brandPrimary,
                                        modifier = Modifier.clickable(onClick = onLoadMore),
                                    )
                                }
                            }
                            !uiState.hasMore -> {
                                Text(
                                    text = stringResource(Res.string.discovery_end_of_results),
                                    modifier = Modifier.fillMaxWidth().padding(RhSpacing.lg),
                                    textAlign = TextAlign.Center,
                                    style = RhTypography.caption,
                                    color = colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 将目录 Presentation 错误枚举映射为 Compose Resources 文案。
 */
@Composable
internal fun catalogPresentationErrorMessage(error: CatalogPresentationError): String =
    when (error) {
        CatalogPresentationError.LoadFailed -> stringResource(Res.string.discovery_error_load_failed)
        CatalogPresentationError.SearchFailed -> stringResource(Res.string.discovery_error_search_failed)
        CatalogPresentationError.ServiceUnavailable -> stringResource(
            Res.string.discovery_error_service_unavailable
        )
        CatalogPresentationError.EmptyResponse -> stringResource(Res.string.discovery_error_empty_response)
    }
