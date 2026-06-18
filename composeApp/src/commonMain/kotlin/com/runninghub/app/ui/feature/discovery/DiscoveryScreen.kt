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
import com.runninghub.app.ui.component.AppBarLogo
import com.runninghub.app.ui.component.AppCard
import com.runninghub.app.ui.component.AppSearchBar
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewDiscoveryUiState
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.adaptiveAppBarHeight
import com.runninghub.app.ui.theme.adaptiveGridColumns
import com.runninghub.app.ui.theme.adaptiveGridSpacing
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.shared.domain.model.CoverMediaType
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.TagSimple
import com.runninghub.shared.domain.model.WebApp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

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
private fun DiscoveryContent(
    uiState: DiscoveryUiState,
    modifier: Modifier = Modifier,
    onExpandSearch: () -> Unit = {},
    onCollapseSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: (String) -> Unit = {},
    onLoadMoreSearchResults: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onCategorySelected: (Int) -> Unit = {},
    onSortSelected: (SortOption) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val windowSizeClass = rememberWindowSizeClass()
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
                                    contentDescription = "搜索",
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
                                    contentDescription = "关闭",
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

        if (uiState.error != null && uiState.apps.isEmpty()) {
            ErrorState(
                message = uiState.error,
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
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(LayoutDirection.Ltr),
                    end = padding.calculateEndPadding(LayoutDirection.Ltr),
                    top = gridSpacing,
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
                        item(key = "featured_section", span = { GridItemSpan(maxLineSpan) }) {
                            FeaturedAppsSection(
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
                            Text("暂无应用", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    items(
                        count = uiState.apps.size,
                        key = { idx -> uiState.apps[idx].id },
                    ) { idx ->
                        AppGridCard(
                            app = uiState.apps[idx],
                            onClick = { onAppClick(uiState.apps[idx].id) },
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
                                    text = "没有更多了",
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

    Column(modifier = modifier.padding(vertical = Dimens.SpaceSM)) {
        when {
            uiState.isSearching && uiState.searchResults.isEmpty() -> {
                LoadingIndicator(modifier = Modifier.fillMaxWidth().height(200.dp))
            }
            uiState.searchError != null && uiState.searchResults.isEmpty() -> {
                ErrorState(
                    message = uiState.searchError,
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
                        text = "输入关键词搜索 AI 应用",
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
                        text = "未找到 \"${uiState.searchQuery}\" 相关结果",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    Text(
                        text = "搜索结果",
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
                            ((uiState.searchResults.size.coerceAtMost(10) / columns + 1) * 240).dp
                        ),
                    ) {
                        items(
                            count = uiState.searchResults.size.coerceAtMost(10),
                            key = { idx -> uiState.searchResults[idx].id },
                        ) { idx ->
                            val app = uiState.searchResults[idx]
                            AppCard(
                                title = app.title,
                                imageUrl = app.coverUrl ?: app.thumbnailUrl,
                                authorName = app.author?.name,
                                authorAvatar = app.author?.avatar,
                                likeCount = app.likeCount,
                                useCount = app.useCount,
                                onClick = { onAppClick(app.id) },
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
                                text = "没有更多了",
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

// region Featured Apps

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedAppsSection(
    banners: List<WebApp>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    val spacing = adaptiveGridSpacing(windowSizeClass)

    Column(modifier = modifier) {
        var isUserInteracting by remember { mutableStateOf(false) }
        LaunchedEffect(banners.size, isUserInteracting) {
            if (banners.size <= 1 || isUserInteracting) return@LaunchedEffect
            while (true) {
                delay(4000)
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }

        Text(
            text = "推荐应用",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = spacing),
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing * 1.5f),
                pageSpacing = spacing,
            ) { page ->
                FeaturedAppBanner(
                    app = banners[page],
                    onClick = { onAppClick(banners[page].id) },
                )
            }

            if (banners.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(banners.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedAppBanner(
    app: WebApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                        ),
                    )
                ),
        ) {
            val displayUrl = app.thumbnailUrl ?: app.coverUrl
            if (displayUrl != null) {
                AsyncImage(
                    model = displayUrl,
                    contentDescription = app.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                            ),
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(18.dp),
            ) {
                Text(
                    text = "精选工作流",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.82f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = app.title.trim(),
                    style = MaterialTheme.typography.titleLarge,
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
                label = "全部",
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
    selectedSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("全部应用", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedSort.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "排序",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, style = MaterialTheme.typography.bodyMedium) },
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

// endregion

// region App Grid Card

@Composable
private fun AppGridCard(
    app: WebApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
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
                        text = app.author?.name?.takeIf { it.isNotBlank() } ?: "RunningHub",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    CardStatChip(
                        icon = Icons.Default.Favorite,
                        count = app.collectCount,
                        label = "收藏",
                    )
                    CardStatChip(
                        icon = Icons.Default.Person,
                        count = app.useCount,
                        label = "使用",
                    )
                    CardStatChip(
                        icon = Icons.Default.Visibility,
                        count = app.pv,
                        label = "浏览",
                    )
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
        num >= 10_000 -> "%.1fw".format(num / 10_000.0)
        num >= 1_000 -> "%.1fk".format(num / 1_000.0)
        else -> raw
    }
}

// endregion
