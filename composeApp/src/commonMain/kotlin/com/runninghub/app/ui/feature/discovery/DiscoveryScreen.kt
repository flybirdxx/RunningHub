package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.AppBarLogo
import com.runninghub.app.ui.component.AppSearchBar
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewDiscoveryUiState
import com.runninghub.app.ui.designsystem.components.cards.AppCard
import com.runninghub.app.ui.designsystem.components.cards.AppCardActionState
import com.runninghub.app.ui.designsystem.components.cards.AppCardActionType
import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.AppCardState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewType as DsAppCardPreviewType
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.adaptiveAppBarHeight
import com.runninghub.app.ui.theme.adaptiveGridColumns
import com.runninghub.app.ui.theme.adaptiveGridSpacing
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.app.util.formatOneDecimal
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.Tag
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.presentation.CatalogPresentationError
import com.runninghub.feature.discovery.presentation.DiscoveryAppCapability
import com.runninghub.feature.discovery.presentation.DiscoveryAppCardMetricKind
import com.runninghub.feature.discovery.presentation.DiscoveryAppCardPrimaryAction
import com.runninghub.feature.discovery.presentation.DiscoveryAppCardUiModel
import com.runninghub.feature.discovery.presentation.DiscoveryAppEstimatedCostKind
import com.runninghub.feature.discovery.presentation.DiscoveryAppPreviewType
import com.runninghub.feature.discovery.presentation.DiscoveryUiState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_all_apps_title
import runninghub.composeapp.generated.resources.discovery_action_generate
import runninghub.composeapp.generated.resources.discovery_action_view_detail
import runninghub.composeapp.generated.resources.discovery_capability_audio
import runninghub.composeapp.generated.resources.discovery_capability_general
import runninghub.composeapp.generated.resources.discovery_capability_image
import runninghub.composeapp.generated.resources.discovery_capability_video
import runninghub.composeapp.generated.resources.discovery_category_all
import runninghub.composeapp.generated.resources.discovery_close_search_content_description
import runninghub.composeapp.generated.resources.discovery_collect_stat_content_description
import runninghub.composeapp.generated.resources.discovery_cost_unknown
import runninghub.composeapp.generated.resources.discovery_default_author_name
import runninghub.composeapp.generated.resources.discovery_empty_apps
import runninghub.composeapp.generated.resources.discovery_end_of_results
import runninghub.composeapp.generated.resources.discovery_error_empty_response
import runninghub.composeapp.generated.resources.discovery_error_load_failed
import runninghub.composeapp.generated.resources.discovery_error_search_failed
import runninghub.composeapp.generated.resources.discovery_error_service_unavailable
import runninghub.composeapp.generated.resources.discovery_home_banner_action
import runninghub.composeapp.generated.resources.discovery_home_banner_eyebrow
import runninghub.composeapp.generated.resources.discovery_home_banner_title
import runninghub.composeapp.generated.resources.discovery_inline_search_hint
import runninghub.composeapp.generated.resources.discovery_metric_use_count
import runninghub.composeapp.generated.resources.discovery_metric_view_count
import runninghub.composeapp.generated.resources.discovery_model_api_badge
import runninghub.composeapp.generated.resources.discovery_search_content_description
import runninghub.composeapp.generated.resources.discovery_search_empty_results_format
import runninghub.composeapp.generated.resources.discovery_search_results_title
import runninghub.composeapp.generated.resources.discovery_sort_content_description
import runninghub.composeapp.generated.resources.discovery_sort_hottest
import runninghub.composeapp.generated.resources.discovery_sort_newest
import runninghub.composeapp.generated.resources.discovery_sort_recommend
import runninghub.composeapp.generated.resources.discovery_sort_reputation
import runninghub.composeapp.generated.resources.discovery_use_stat_content_description
import runninghub.composeapp.generated.resources.discovery_view_stat_content_description

class DiscoveryVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current
        val screenModel = koinScreenModel<DiscoveryScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadInitialData() }

        DiscoveryContent(
            uiState = uiState,
            onExpandSearch = screenModel::expandSearch,
            onCollapseSearch = screenModel::collapseSearch,
            onSearchQueryChange = screenModel::onSearchQueryChange,
            onSearchSubmit = screenModel::searchSubmit,
            onLoadMoreSearchResults = screenModel::loadMoreSearchResults,
            onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
            onModelBannerClick = { skuId -> uriHandler.openUri("$CALL_API_DETAIL_BASE_URL$skuId") },
            onCategorySelected = screenModel::selectCategory,
            onSortSelected = screenModel::selectSort,
            onRefresh = screenModel::refresh,
            onLoadMore = screenModel::loadMore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoveryContent(
    uiState: DiscoveryUiState,
    modifier: Modifier = Modifier,
    onExpandSearch: () -> Unit = {},
    onCollapseSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onLoadMoreSearchResults: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onModelBannerClick: (String) -> Unit = {},
    onCategorySelected: (Int) -> Unit = {},
    onSortSelected: (CatalogSort) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val windowSizeClass = rememberWindowSizeClass()
    val windowInfo = LocalRhWindowInfo.current
    val gridSpacing: Dp = adaptiveGridSpacing(windowSizeClass)
    val appBarHeight = adaptiveAppBarHeight(windowSizeClass)
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(appBarHeight)
                            .padding(horizontal = Dimens.SpaceLG),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Logo (left side)
                        AppBarLogo(
                            assetPath = "logo_appbar.svg",
                            modifier = Modifier
                                .height(28.dp)
                                .widthIn(max = 120.dp),
                        )

                        Spacer(Modifier.weight(1f))

                        // Search / actions (right side)
                        if (!uiState.isSearchExpanded) {
                            IconButton(onClick = onExpandSearch) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(Res.string.discovery_search_content_description),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            AppSearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChange,
                                onSearch = onSearchSubmit,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onCollapseSearch) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.discovery_close_search_content_description),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                    // Bottom border
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = borderColor,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
    ) { padding ->

        if (uiState.isLoading && uiState.apps.isEmpty() && uiState.banners.isEmpty() && uiState.categories.isEmpty()) {
            LoadingIndicator(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val discoveryError = uiState.error?.let { catalogPresentationErrorMessage(it) }
        if (discoveryError != null && uiState.apps.isEmpty()) {
            ErrorState(
                message = discoveryError,
                modifier = Modifier.padding(padding),
                onRetry = onRefresh,
            )
            return@Scaffold
        }

        val pullRefreshState = rememberPullToRefreshState()
        val gridState = rememberLazyGridState()
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
                    bottom = Dimens.Space3XL
                ),
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
                modifier = Modifier.fillMaxSize(),
            ) {
                // ── Inline Search Results ──
                if (uiState.isSearchExpanded) {
                    item(key = "search_results", span = { GridItemSpan(maxLineSpan) }) {
                        InlineSearchResults(
                            uiState = uiState,
                            windowSizeClass = windowSizeClass,
                            onAppClick = onAppClick,
                            onLoadMore = onLoadMoreSearchResults,
                        )
                    }
                } else {
                    // ── Normal Discovery Content ──
                    item(key = "home_model_banner", span = { GridItemSpan(maxLineSpan) }) {
                        HomeModelBannerSection(
                            windowSizeClass = windowSizeClass,
                            onModelClick = onModelBannerClick,
                        )
                    }

                    item(key = "categories", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            CategoryTagsRow(
                                categories = uiState.categories,
                                selectedIndex = uiState.selectedCategoryIndex,
                                onSelected = onCategorySelected,
                            )
                            SortRow(
                                selectedSort = uiState.selectedSort,
                                onSortSelected = onSortSelected,
                            )
                        }
                    }

                if (uiState.isLoadingApps && uiState.apps.isEmpty()) {
                    item(key = "content_loading", span = { GridItemSpan(maxLineSpan) }) {
                        LoadingIndicator(
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                    }
                } else if (uiState.apps.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                stringResource(Res.string.discovery_empty_apps),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    items(
                        count = uiState.appCards.size,
                        key = { idx -> uiState.appCards[idx].id },
                    ) { idx ->
                        DiscoveryAppCard(
                            card = uiState.appCards[idx],
                            onClick = { onAppClick(uiState.appCards[idx].id) },
                        )
                    }

                    item(key = "load_more", span = { GridItemSpan(maxLineSpan) }) {
                        if (uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoadingApps) {
                            LaunchedEffect(uiState.currentPage) {
                                onLoadMore()
                            }
                        }

                        when {
                            uiState.isLoadingApps -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                            uiState.hasMore -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                            uiState.apps.isNotEmpty() -> {
                                Text(
                                    text = stringResource(Res.string.discovery_end_of_results),
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

// region Inline Search Results

@Composable
private fun InlineSearchResults(
    uiState: DiscoveryUiState,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val columns = adaptiveGridColumns(windowSizeClass)

    val searchError = uiState.searchError

    Column(modifier = modifier.padding(vertical = Dimens.SpaceSM)) {
        when {
            uiState.isSearching && uiState.searchResults.isEmpty() -> {
                LoadingIndicator(modifier = Modifier.fillMaxWidth().height(200.dp))
            }
            searchError != null && uiState.searchResults.isEmpty() -> {
                val searchErrorMessage = catalogPresentationErrorMessage(searchError)
                ErrorState(
                    message = searchErrorMessage,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    onRetry = { onLoadMore() },
                )
            }
            uiState.searchQuery.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.discovery_inline_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.discovery_search_empty_results_format,
                            uiState.searchQuery,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    Text(
                        text = stringResource(Res.string.discovery_search_results_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceLG),
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(horizontal = Dimens.SpaceLG),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                        modifier = Modifier.height(
                            ((uiState.searchResultCards.size.coerceAtMost(10) / columns + 1) * 260).dp
                        ),
                    ) {
                        items(
                            count = uiState.searchResultCards.size.coerceAtMost(10),
                            key = { idx -> uiState.searchResultCards[idx].id },
                        ) { idx ->
                            val card = uiState.searchResultCards[idx]
                            DiscoveryAppCard(
                                card = card,
                                onClick = { onAppClick(card.id) },
                            )
                        }
                    }

                    // Load more
                    if (uiState.searchResults.size >= 10) {
                        if (uiState.searchHasMore && !uiState.isSearching) {
                            LaunchedEffect(Unit) { onLoadMore() }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceMD),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimens.IconSizeMD),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                        if (!uiState.searchHasMore) {
                            Text(
                                text = stringResource(Res.string.discovery_end_of_results),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceMD),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

// region Home Model Banner

private const val CALL_API_DETAIL_BASE_URL = "https://www.runninghub.cn/call-api/api-detail/"

private enum class ModelBannerMediaType {
    IMAGE,
    VIDEO,
}

private data class HomeModelBannerTile(
    val skuId: String,
    val title: String,
    val mediaUrl: String,
    val mediaType: ModelBannerMediaType,
)

private val homeModelBannerTiles = listOf(
    HomeModelBannerTile(
        skuId = "2031354034474311686",
        title = "Qwen Image 2.0",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-12/f889a4e2d48abe14e07346396c888af1.png",
        mediaType = ModelBannerMediaType.IMAGE,
    ),
    HomeModelBannerTile(
        skuId = "2026215209183760386",
        title = "Seedream V5 Lite",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-12/51c0e118e907ef0b0cafea99667eba6d.png",
        mediaType = ModelBannerMediaType.IMAGE,
    ),
    HomeModelBannerTile(
        skuId = "2039648613636050946",
        title = "WAN 2.7",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/fae338274c9053123688d63ac419cd59/2026-04-27/aaeffcd7ce935afdcd816327b4e86a04.png",
        mediaType = ModelBannerMediaType.IMAGE,
    ),
    HomeModelBannerTile(
        skuId = "2034917373414539277",
        title = "Seedance 2.0",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/7f4f309e26148d357fa9d461e25ddcc5.mp4",
        mediaType = ModelBannerMediaType.VIDEO,
    ),
    HomeModelBannerTile(
        skuId = "2019623243725737985",
        title = "Kling o3-pro",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/593abc725f555ef4d79fae1981bb83bc.mp4",
        mediaType = ModelBannerMediaType.VIDEO,
    ),
    HomeModelBannerTile(
        skuId = "2039648613636050945",
        title = "WAN 2.7 Video",
        mediaUrl = "https://rh-images.xiaoyaoyou.com/22820eb19d5010de41dbf6856e984340/2026-06-15/29449b321bfd27201b18dd189dc30d93.mp4",
        mediaType = ModelBannerMediaType.VIDEO,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeModelBannerSection(
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onModelClick: (String) -> Unit = {},
) {
    val spacing = adaptiveGridSpacing(windowSizeClass)
    val pageCount = homeModelBannerTiles.size + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    LaunchedEffect(pageCount) {
        while (true) {
            delay(5_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % pageCount)
        }
    }

    Column(modifier = modifier.padding(bottom = spacing)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = if (windowSizeClass.isWide) spacing else 0.dp),
                pageSpacing = spacing,
            ) { page ->
                if (page == 0) {
                    HomeModelBannerHeroSlide(
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (windowSizeClass.isWide) 21f / 9f else 16f / 9f),
                        onClick = { onModelClick("2034917373414539277") },
                    )
                } else {
                    val tile = homeModelBannerTiles[page - 1]
                    HomeModelBannerMediaSlide(
                        tile = tile,
                        modifier = Modifier.fillMaxWidth().aspectRatio(if (windowSizeClass.isWide) 21f / 9f else 16f / 9f),
                        onClick = { onModelClick(tile.skuId) },
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.45f)
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeModelBannerHeroSlide(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Color(0xFFCCFF00),
        shape = RoundedCornerShape(0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.discovery_home_banner_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(Res.string.discovery_home_banner_title),
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.discovery_home_banner_action),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeModelBannerMediaSlide(
    tile: HomeModelBannerTile,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Color(0xFF101010),
        shape = RoundedCornerShape(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (tile.mediaType) {
                ModelBannerMediaType.IMAGE -> {
                    AsyncImage(
                        model = tile.mediaUrl,
                        contentDescription = tile.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                ModelBannerMediaType.VIDEO -> {
                    VideoThumbnail(
                        url = tile.mediaUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        )
                    )
            )

            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            ) {
                Text(
                    text = stringResource(Res.string.discovery_model_api_badge),
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tile.title,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// endregion

@Composable
private fun DiscoveryAdaptivePreview(
    spec: RhPreviewSpec,
    searchExpanded: Boolean = false,
) {
    RhAdaptivePreview(spec = spec) {
        DiscoveryContent(
            uiState = previewDiscoveryUiState(searchExpanded = searchExpanded),
            onExpandSearch = {},
            onCollapseSearch = {},
            onSearchQueryChange = {},
            onSearchSubmit = {},
            onLoadMoreSearchResults = {},
            onAppClick = {},
            onCategorySelected = {},
            onSortSelected = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

@Preview
@Composable
private fun DiscoveryPhone320Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun DiscoveryPhone360Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun DiscoveryPhone430Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun DiscoveryMedium600Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun DiscoveryExpanded840Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun DiscoveryLandscapePreview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun DiscoveryFontScale13Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun DiscoveryFontScale15Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.FontScale15)
}

@Preview
@Composable
private fun DiscoveryInlineSearchPreview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone360, searchExpanded = true)
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

// region Category Tags + Sort

@Composable
private fun CategoryTagsRow(
    categories: List<Tag>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 4.dp),
    ) {
        item(key = "category_all") {
            CategoryTag(
                label = stringResource(Res.string.discovery_category_all),
                selected = selectedIndex == 0,
                onClick = { onSelected(0) },
            )
        }
        itemsIndexed(categories, key = { _, tag -> tag.id }) { idx, tag ->
            CategoryTag(
                label = tag.name,
                selected = selectedIndex == idx + 1,
                onClick = { onSelected(idx + 1) },
            )
        }
    }
}

@Composable
private fun CategoryTag(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.RadiusFull),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun SortRow(
    selectedSort: CatalogSort,
    onSortSelected: (CatalogSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.discovery_all_apps_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = catalogSortLabel(selectedSort),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(Res.string.discovery_sort_content_description),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                CatalogSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = catalogSortLabel(option),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onSortSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

/**
 * 将发现页排序枚举映射为 Compose Resources 文案。
 *
 * 排序语义仍由 Domain 层 [CatalogSort] 表达，UI 只在最终渲染前选择本地化文案，
 * 避免独立 Presentation 模块继续持有用户可见固定字符串。
 */
@Composable
private fun catalogSortLabel(sort: CatalogSort): String {
    val resource = when (sort) {
        CatalogSort.RECOMMEND -> Res.string.discovery_sort_recommend
        CatalogSort.REPUTATION -> Res.string.discovery_sort_reputation
        CatalogSort.HOTTEST -> Res.string.discovery_sort_hottest
        CatalogSort.NEWEST -> Res.string.discovery_sort_newest
    }
    return stringResource(resource)
}

// endregion

// region App Grid Card

@Composable
private fun DiscoveryAppCard(
    card: DiscoveryAppCardUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AppCard(
        state = card.toAppCardState(),
        onClick = onClick,
        onAction = { onClick() },
        modifier = modifier,
        previewContent = { preview ->
            DiscoveryAppCardPreview(preview)
        },
    )
}

@Composable
private fun DiscoveryAppCardPreview(preview: AppCardPreviewState) {
    val url = preview.url
    if (url.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {}
        return
    }

    when (preview.type) {
        DsAppCardPreviewType.Video -> VideoThumbnail(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
        else -> SmartAsyncImage(
            imageUrl = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun DiscoveryAppCardUiModel.toAppCardState(): AppCardState = AppCardState(
    id = id,
    title = templateName,
    capabilityLabel = capability.toCapabilityLabel(),
    preview = AppCardPreviewState(
        url = preview.url,
        type = preview.type.toDesignSystemPreviewType(),
    ),
    estimatedCostLabel = estimatedCost.kind.toEstimatedCostLabel(),
    metric = supportingMetric?.let { metric ->
        AppCardMetricState(
            label = metric.kind.toMetricLabel(),
            value = formatCount(metric.value),
        )
    },
    primaryAction = AppCardActionState(
        type = primaryAction.toDesignSystemActionType(),
        label = primaryAction.toActionLabel(),
    ),
)

@Composable
private fun DiscoveryAppCapability.toCapabilityLabel(): String = stringResource(
    when (this) {
        DiscoveryAppCapability.IMAGE -> Res.string.discovery_capability_image
        DiscoveryAppCapability.VIDEO -> Res.string.discovery_capability_video
        DiscoveryAppCapability.AUDIO -> Res.string.discovery_capability_audio
        DiscoveryAppCapability.GENERAL -> Res.string.discovery_capability_general
    },
)

@Composable
private fun DiscoveryAppEstimatedCostKind.toEstimatedCostLabel(): String = stringResource(
    when (this) {
        DiscoveryAppEstimatedCostKind.UNKNOWN -> Res.string.discovery_cost_unknown
    },
)

@Composable
private fun DiscoveryAppCardMetricKind.toMetricLabel(): String = stringResource(
    when (this) {
        DiscoveryAppCardMetricKind.USE_COUNT -> Res.string.discovery_metric_use_count
        DiscoveryAppCardMetricKind.VIEW_COUNT -> Res.string.discovery_metric_view_count
    },
)

@Composable
private fun DiscoveryAppCardPrimaryAction.toActionLabel(): String = stringResource(
    when (this) {
        DiscoveryAppCardPrimaryAction.GENERATE -> Res.string.discovery_action_generate
        DiscoveryAppCardPrimaryAction.VIEW_DETAIL -> Res.string.discovery_action_view_detail
    },
)

private fun DiscoveryAppPreviewType.toDesignSystemPreviewType(): DsAppCardPreviewType = when (this) {
    DiscoveryAppPreviewType.IMAGE -> DsAppCardPreviewType.Image
    DiscoveryAppPreviewType.VIDEO -> DsAppCardPreviewType.Video
    DiscoveryAppPreviewType.AUDIO -> DsAppCardPreviewType.Audio
    DiscoveryAppPreviewType.EMPTY -> DsAppCardPreviewType.Empty
}

private fun DiscoveryAppCardPrimaryAction.toDesignSystemActionType(): AppCardActionType = when (this) {
    DiscoveryAppCardPrimaryAction.GENERATE -> AppCardActionType.Generate
    DiscoveryAppCardPrimaryAction.VIEW_DETAIL -> AppCardActionType.ViewDetail
}

@Composable
private fun AppGridCard(
    app: WebApp,
    statsLimit: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val visibleStats = listOf(
        Triple(
            Icons.Default.Favorite,
            app.collectCount,
            stringResource(Res.string.discovery_collect_stat_content_description),
        ),
        Triple(
            Icons.Default.Person,
            app.useCount,
            stringResource(Res.string.discovery_use_stat_content_description),
        ),
        Triple(
            Icons.Default.Visibility,
            app.pv,
            stringResource(Res.string.discovery_view_stat_content_description),
        ),
    ).take(statsLimit.coerceIn(1, 3))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.RadiusMD),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Cover image fills entire card
            when (app.coverMediaType) {
                CoverMediaType.VIDEO -> {
                    app.coverUrl?.let {
                        VideoThumbnail(
                            url = it,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                else -> {
                    AsyncImage(
                        model = app.coverUrl ?: app.thumbnailUrl,
                        contentDescription = app.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            // Bottom gradient for text readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            if (app.tags.isNotEmpty()) {
                CardTagRow(
                    tags = app.tags,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            ) {
                Text(
                    text = app.title.trim(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SmartAsyncImage(
                        imageUrl = app.author?.avatar,
                        contentDescription = app.author?.name,
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        shape = CircleShape,
                    )
                    Text(
                        text = app.author?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.discovery_default_author_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    visibleStats.forEachIndexed { index, (icon, count, label) ->
                        CardStatChip(
                            icon = icon,
                            count = count,
                            label = label,
                            modifier = if (index == visibleStats.lastIndex) Modifier else Modifier.padding(end = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTagRow(
    tags: List<TagSimple>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tags.take(3).forEach { tag ->
            Text(
                text = tag.name,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = 58.dp)
                    .background(Color.White.copy(alpha = 0.24f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CardStatChip(
    icon: ImageVector,
    count: String,
    modifier: Modifier = Modifier,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(9.dp),
            tint = Color.White.copy(alpha = 0.82f),
        )
        Spacer(Modifier.width(1.dp))
        Text(
            text = formatCount(count),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = Color.White.copy(alpha = 0.82f),
            maxLines = 1,
        )
    }
}

// endregion

// region Utils

private fun formatCount(raw: String): String {
    val num = raw.toLongOrNull() ?: return raw
    return when {
        // Kotlin/Native 不支持 JVM 的 String.format；使用项目内跨平台格式化保持 iOS 编译稳定。
        num >= 10_000 -> "${formatOneDecimal(num / 10_000.0)}w"
        num >= 1_000 -> "${formatOneDecimal(num / 1_000.0)}k"
        else -> raw
    }
}

// endregion
