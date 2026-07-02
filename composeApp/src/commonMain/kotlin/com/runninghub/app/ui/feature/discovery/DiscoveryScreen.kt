package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.AppBarLogo
import com.runninghub.app.ui.component.AppSearchBar
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.adaptiveAppBarHeight
import com.runninghub.app.ui.theme.adaptiveGridColumns
import com.runninghub.app.ui.theme.adaptiveGridSpacing
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.core.model.Tag
import com.runninghub.core.model.WebApp
import com.runninghub.feature.discovery.domain.CatalogSort
import com.runninghub.feature.discovery.presentation.CatalogPresentationError
import com.runninghub.feature.discovery.presentation.DiscoveryUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_all_apps_title
import runninghub.composeapp.generated.resources.discovery_category_all
import runninghub.composeapp.generated.resources.discovery_close_search_content_description
import runninghub.composeapp.generated.resources.discovery_empty_apps
import runninghub.composeapp.generated.resources.discovery_end_of_results
import runninghub.composeapp.generated.resources.discovery_error_empty_response
import runninghub.composeapp.generated.resources.discovery_error_load_failed
import runninghub.composeapp.generated.resources.discovery_error_search_failed
import runninghub.composeapp.generated.resources.discovery_error_service_unavailable
import runninghub.composeapp.generated.resources.discovery_inline_search_hint
import runninghub.composeapp.generated.resources.discovery_metric_use_count
import runninghub.composeapp.generated.resources.discovery_metric_view_count
import runninghub.composeapp.generated.resources.discovery_search_content_description
import runninghub.composeapp.generated.resources.discovery_search_empty_results_format
import runninghub.composeapp.generated.resources.discovery_search_results_title
import runninghub.composeapp.generated.resources.discovery_sort_content_description
import runninghub.composeapp.generated.resources.discovery_sort_hottest
import runninghub.composeapp.generated.resources.discovery_sort_newest
import runninghub.composeapp.generated.resources.discovery_sort_recommend
import runninghub.composeapp.generated.resources.discovery_sort_reputation

class DiscoveryVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
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
    onExpandSearch: () -> Unit = {},
    onCollapseSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onLoadMoreSearchResults: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
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

private const val DISCOVERY_BANNER_MAX_ITEMS = 6

@Composable
private fun DiscoveryBannerSection(
    banners: List<WebApp>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
) {
    val spacing = adaptiveGridSpacing(windowSizeClass)
    val cardWidth = if (windowSizeClass.isWide) 360.dp else 286.dp
    val cardHeight = if (windowSizeClass.isWide) 156.dp else 124.dp
    val visibleBanners = banners.take(DISCOVERY_BANNER_MAX_ITEMS)

    LazyRow(
        modifier = modifier.fillMaxWidth().padding(bottom = spacing),
        contentPadding = PaddingValues(horizontal = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        itemsIndexed(
            items = visibleBanners,
            key = { index, app -> app.id.ifBlank { "banner-$index" } },
        ) { _, app ->
            DiscoveryBannerCard(
                app = app,
                modifier = Modifier.width(cardWidth).height(cardHeight),
                onClick = { onAppClick(app.id) },
            )
        }
    }
}

@Composable
private fun DiscoveryBannerCard(
    app: WebApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val previewUrl = app.coverUrl ?: app.thumbnailUrl ?: app.videoUrl
    val showVideo = app.videoUrl != null && previewUrl == app.videoUrl
    val metricText = app.useCount.takeIf { it.isNotBlank() }?.let { value ->
        "${stringResource(Res.string.discovery_metric_use_count)} ${formatCount(value)}"
    } ?: app.pv.takeIf { it.isNotBlank() }?.let { value ->
        "${stringResource(Res.string.discovery_metric_view_count)} ${formatCount(value)}"
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!previewUrl.isNullOrBlank()) {
                if (showVideo) {
                    VideoThumbnail(
                        url = previewUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = app.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = app.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                metricText?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
