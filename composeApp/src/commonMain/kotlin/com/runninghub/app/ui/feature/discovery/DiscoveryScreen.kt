package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.search.SearchVoyagerScreen
import com.runninghub.shared.domain.model.CoverMediaType
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.TagSimple
import com.runninghub.shared.domain.model.WebApp
import kotlinx.coroutines.delay

private val DarkBg = Color(0xFF0B0F1A)
private val CardBg = Color(0xFF141929)
private val PrimaryText = Color.White
private val SecondaryText = Color(0xFF94A3B8)
private val DimText = Color(0xFF64748B)
private val AccentPurple = Color(0xFF6C5CE7)
private val TagUnselectedBg = Color(0xFF1E2336)
private val SearchBarBg = Color(0xFF141929)

class DiscoveryVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<DiscoveryScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

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
private fun DiscoveryContent(
    uiState: DiscoveryUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onCategorySelected: (Int) -> Unit = {},
    onSortSelected: (SortOption) -> Unit = {},
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    Scaffold(
        containerColor = DarkBg,
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
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "search_bar", span = { GridItemSpan(maxLineSpan) }) {
                    SearchBar(onClick = onSearchClick)
                }

                if (uiState.banners.isNotEmpty()) {
                    item(key = "featured_section", span = { GridItemSpan(maxLineSpan) }) {
                        FeaturedAppsSection(
                            banners = uiState.banners,
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

                if (uiState.isLoadingApps) {
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
                            Text("暂无应用", color = SecondaryText, fontSize = 14.sp)
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
                        if (uiState.hasMore && !uiState.isLoadingMore) {
                            LaunchedEffect(uiState.currentPage) {
                                onLoadMore()
                            }
                        }

                        when {
                            uiState.hasMore -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AccentPurple,
                                        strokeWidth = 2.dp,
                                    )
                                }
                            }
                            uiState.apps.isNotEmpty() -> {
                                Text(
                                    text = "没有更多了",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = DimText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// region SearchBar

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SearchBarBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = DimText,
            )
            Spacer(Modifier.width(10.dp))
            Text("搜索AI应用", fontSize = 14.sp, color = DimText)
        }
    }
}

// endregion

// region Featured Apps

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedAppsSection(
    banners: List<WebApp>,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })

    Column(modifier = modifier) {
        Text(
            text = "推荐应用",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 10.dp,
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
                            shape = RoundedCornerShape(12.dp),
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
                                    if (index == pagerState.currentPage) AccentPurple else Color.White.copy(alpha = 0.4f)
                                ),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(banners.size) {
        if (banners.size <= 1) return@LaunchedEffect
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }
}

@Composable
private fun FeaturedAppBanner(
    app: WebApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
    ) {
        val displayUrl = app.thumbnailUrl ?: app.coverUrl
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }

        if (displayUrl != null) {
            AsyncImage(
                model = displayUrl,
                contentDescription = app.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true; isError = false },
                onSuccess = { isLoading = false; isError = false },
                onError = { isLoading = false; isError = true },
            )
            if (isLoading || isError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A3E)),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2A2A3E)),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        ),
                        startY = 100f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                text = app.title.trim(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    app.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = AccentPurple.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = tag.name,
                                fontSize = 10.sp,
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// endregion

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
        modifier = modifier.padding(vertical = 8.dp),
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
    val bgColor = if (selected) AccentPurple else TagUnselectedBg
    val textColor = if (selected) Color.White else SecondaryText

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
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
        Text("全部应用", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryText)

        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(selectedSort.label, fontSize = 12.sp, color = SecondaryText)
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SecondaryText,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, fontSize = 13.sp) },
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .clickable(onClick = onClick),
    ) {
        when (app.coverMediaType) {
            CoverMediaType.VIDEO -> {
                val videoUrl = app.coverUrl
                if (videoUrl != null) {
                    VideoThumbnail(
                        url = videoUrl,
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

        if (app.tags.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                        )
                    )
                    .padding(6.dp),
            ) {
                FlowTagRow(app.tags.take(3))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f),
                        ),
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = app.title.trim(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp,
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = app.author?.avatar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = app.author?.name ?: "",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                StatChip(Icons.Default.FavoriteBorder, app.collectCount)
                Spacer(Modifier.width(5.dp))
                StatChip(Icons.Outlined.PlayArrow, app.useCount)
                Spacer(Modifier.width(5.dp))
                StatChip(Icons.Outlined.RemoveRedEye, app.pv)
            }
        }
    }
}

@Composable
private fun FlowTagRow(tags: List<TagSimple>) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        tags.forEach { tag ->
            Text(
                text = tag.name,
                fontSize = 8.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    count: String,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = formatCount(count),
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.7f),
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
