package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.runninghub.app.ui.theme.AppDimens
import com.runninghub.shared.domain.model.WebApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    uiState: DiscoveryUiState,
    onCategorySelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onAppClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            DiscoveryTopBar(onSearchClick = onSearchClick)

            if (uiState.banners.isNotEmpty()) {
                HeroBannerCarousel(
                    banners = uiState.banners,
                    onBannerClick = { onAppClick(it.id) }
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.PaddingMedium))

            CategoryIconsRow(
                categories = uiState.categories.map { it.name },
                selectedIndex = uiState.selectedCategoryIndex,
                onCategorySelected = onCategorySelected
            )

            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))

            SectionHeader(title = "热门应用", actionText = "更多 >")

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            HotAppsList(
                apps = uiState.apps.take(5),
                onAppClick = onAppClick
            )

            Spacer(modifier = Modifier.height(AppDimens.PaddingLarge))

            SectionHeader(title = "精选作品", actionText = "更多 >")

            Spacer(modifier = Modifier.height(AppDimens.PaddingSmall))

            FeaturedWorksGrid(
                apps = uiState.banners.take(4),
                onAppClick = { onAppClick(it.id) }
            )

            Spacer(modifier = Modifier.height(64.dp + AppDimens.PaddingLarge))
        }
    }
}

@Composable
private fun DiscoveryTopBar(onSearchClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.PaddingMedium, vertical = 12.dp)
            .statusBarsPadding()
    ) {
        Text(
            text = "RunningHub",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(AppDimens.PaddingSmall))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Pro",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(2f)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onSearchClick)
                .padding(horizontal = 12.dp, vertical = AppDimens.PaddingSmall)
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "搜索应用、作品、用户...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroBannerCarousel(
    banners: List<WebApp>,
    onBannerClick: (WebApp) -> Unit
) {
    val displayBanners = banners.take(5)
    if (displayBanners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { displayBanners.size })

    Column {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = AppDimens.PaddingMedium),
            pageSpacing = 12.dp,
            modifier = Modifier.height(180.dp)
        ) { page ->
            val banner = displayBanners[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(AppDimens.CardCornerRadius))
                    .clickable { onBannerClick(banner) }
            ) {
                AsyncImage(
                    model = banner.coverUrls.firstOrNull() ?: banner.thumbnailUrl,
                    contentDescription = banner.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(AppDimens.PaddingMedium)
                ) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = banner.description.ifEmpty { "立即体验" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "立即体验",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.PaddingSmall)
        ) {
            repeat(displayBanners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
private fun CategoryIconsRow(
    categories: List<String>,
    selectedIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    val iconMap = listOf(
        Icons.Filled.Whatshot to "热门",
        Icons.Filled.CameraAlt to "摄影",
        Icons.Filled.MusicNote to "音乐",
        Icons.Filled.Restaurant to "美食",
        Icons.Filled.Brush to "文学",
        Icons.Filled.Movie to "影视",
        Icons.Filled.SportsEsports to "游戏",
        Icons.Filled.MoreHoriz to "更多"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingMedium),
        contentPadding = PaddingValues(horizontal = AppDimens.PaddingLarge),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            if (categories.isNotEmpty()) categories.take(8) else iconMap.map { it.second }
        ) { index, name ->
            val icon = iconMap.getOrNull(index)?.first ?: Icons.Filled.Apps
            val isSelected = index == selectedIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onCategorySelected(index) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.PaddingMedium)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
        if (actionText.isNotEmpty()) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HotAppsList(
    apps: List<WebApp>,
    onAppClick: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = AppDimens.PaddingMedium)
    ) {
        apps.forEachIndexed { index, app ->
            HotAppItem(
                rank = index + 1,
                app = app,
                onClick = { onAppClick(app.id) }
            )
        }
    }
}

@Composable
private fun HotAppItem(
    rank: Int,
    app: WebApp,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = AppDimens.PaddingSmall)
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = when (rank) {
                1 -> Color(0xFFFFD700)
                2 -> MaterialTheme.colorScheme.primary
                3 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.width(28.dp)
        )

        AsyncImage(
            model = app.coverUrls.firstOrNull() ?: app.thumbnailUrl,
            contentDescription = app.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${formatCount(app.useCount)}人使用 · ${formatCount(app.viewCount)}浏览",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = formatCount(app.likeCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeaturedWorksGrid(
    apps: List<WebApp>,
    onAppClick: (WebApp) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.PaddingSmall),
        contentPadding = PaddingValues(horizontal = AppDimens.PaddingMedium),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(apps) { app ->
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(AppDimens.ButtonCornerRadius))
                    .clickable { onAppClick(app) }
            ) {
                AsyncImage(
                    model = app.coverUrls.firstOrNull() ?: app.thumbnailUrl,
                    contentDescription = app.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = 120f
                            )
                        )
                )

                Text(
                    text = app.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(AppDimens.PaddingSmall)
                )
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 10000 -> "${count / 10000}.${(count % 10000) / 1000}万"
    count >= 1000 -> "${count / 1000}.${(count % 1000) / 100}k"
    else -> count.toString()
}
