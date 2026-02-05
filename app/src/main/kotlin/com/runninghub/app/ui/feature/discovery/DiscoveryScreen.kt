/**
 * [INPUT]: 依赖 DiscoveryUiState, DiscoveryViewModel, Compose 基础库
 * [OUTPUT]: 对外提供 DiscoveryScreen 页面极其交互组件
 * [POS]: 首页业务模块的 View 层实现，采用命令式 UI 声明
 * [PROTOCOL]: 变更时更新此头部，然后检查 CLAUDE.md
 */
package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.ui.navigation.Screen
import coil.decode.Decoder
import coil.decode.VideoFrameDecoder
import coil.request.Options
import coil.fetch.SourceResult
import coil.ImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    uiState: DiscoveryUiState,
    navController: NavHostController,
    onCategorySelected: (Category) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    // 分页加载触发逻辑：当滑动快到底部时触发
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalHeight = scrollState.maxValue
            val currentScroll = scrollState.value
            if (totalHeight > 0) {
                totalHeight - currentScroll < 500 // 距离底部不足 500 像素时触发
            } else {
                false
            }
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Scaffold(
        topBar = { DiscoveryHeader(onRefresh = onRefresh) },
        bottomBar = { DiscoveryBottomNav(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. Banner
                if (uiState.banners.isNotEmpty()) {
                    BannerSection(uiState.banners)
                }

                // 2. Categories
                CategoryPills(uiState.categories, uiState.selectedCategory, onCategorySelected)


                // 5. Discovery Feed (Discovery 作品使用瀑布流)
                if (uiState.isLoading && uiState.discoveryApps.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RunningHubTeal)
                    }
                } else if (uiState.discoveryApps.isEmpty() && !uiState.isLoading) {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Spacer(Modifier.height(8.dp))
                            Text("暂无作品", color = Color.Gray)
                        }
                    }
                } else {
                    DiscoveryFeedSection(
                        apps = uiState.discoveryApps,
                        onAppClick = { appId ->
                            navController.navigate(Screen.AppDetail.createRoute(appId))
                        }
                    )
                    
                    // 加载更多提示
                    if (uiState.isLoadingMore) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RunningHubTeal, strokeWidth = 2.dp)
                        }
                    } else if (!uiState.hasMore && uiState.discoveryApps.isNotEmpty()) {
                        Text(
                            "没有更多作品了",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = RunningHubTeal,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryHeader(onRefresh: () -> Unit = {}) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, "Logo", tint = RunningHubTeal)
                Spacer(Modifier.width(8.dp))
                Text("RunningHUB", fontWeight = FontWeight.Bold)
            }
        },
        actions = {
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Refresh") }
            IconButton(onClick = {}) { Icon(Icons.Default.Search, "Search") }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(RunningHubTeal, Color.Blue)))
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSection(banners: List<Banner>) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    
    // Auto-play logic
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // 5 seconds interval
            val nextPage = (pagerState.currentPage + 1) % banners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .clip(RoundedCornerShape(20.dp))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val banner = banners[page]
            Box(modifier = Modifier.fillMaxSize()) {
                SmartAsyncImage(
                    imageUrl = banner.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                        .padding(20.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        banner.tag?.let { tag ->
                            Surface(
                                color = RunningHubTeal,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(banner.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        banner.description?.let { desc ->
                            Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        
        // Pager Indicators
        Row(
            Modifier
                .height(30.dp)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) RunningHubTeal else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryPills(categories: List<Category>, selected: Category, onSelected: (Category) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(categories) { category ->
            val isSelected = category.tagIds == selected.tagIds
            Surface(
                onClick = { onSelected(category) },
                color = if (isSelected) RunningHubTeal else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = category.name,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DiscoveryFeedSection(apps: List<AiApp>, onAppClick: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        // Removed '发现作品' header as requested
        // 此处仅做示范性布局
        for (rowApps in apps.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                for (app in rowApps) {
                    AiAppMasonryItem(
                        app = app, 
                        modifier = Modifier.weight(1f),
                        onClick = { onAppClick(app.id) }
                    )
                }
                if (rowApps.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AiAppMasonryItem(app: AiApp, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.8f)) {
            // 使用封装好的 SmartAsyncImage 处理视频/GIF/图片
            SmartAsyncImage(
                imageUrl = app.imageUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 内容叠加与渐变遮罩整合
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                            startY = 300f
                        )
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // 标题
                Text(
                    text = app.title.trim(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp)) // 缩小间距

                // 底部信息行：作者 + 统计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左侧：作者信息 (更紧凑)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        AsyncImage(
                            model = app.authorAvatar ?: "https://www.runninghub.cn/favicon.ico",
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = app.author,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 右侧：统计数据 (图标更小更紧凑)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(10.dp), tint = Color.White.copy(alpha = 0.7f))
                        Text(app.likes.toString(), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp, end = 4.dp))
                        
                        Icon(Icons.Default.StarBorder, null, modifier = Modifier.size(10.dp), tint = Color.White.copy(alpha = 0.7f))
                        Text(app.stars.toString(), color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp, end = 4.dp))

                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(10.dp), tint = Color.White.copy(alpha = 0.7f))
                        Text(app.useCount, color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, modifier = Modifier.padding(start = 2.dp))
                    }
                }
            }

            // 右上角角标 (可选)
            if(app.likes > 5000) {
                Surface(
                    color = Color(0xFFFFCC00), // 鲜艳的黄色
                    modifier = Modifier.padding(6.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("HOT", color = Color.Black, fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

/**
 * 智能异步图片加载组件
 * 自动识别 .mp4 并强制使用 VideoFrameDecoder
 * 自动识别 .gif 并使用 GifDecoder
 */
@Composable
fun SmartAsyncImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cleanUrl = remember(imageUrl) { imageUrl.trim() }
    val isVideo = remember(cleanUrl) { cleanUrl.contains("mp4", ignoreCase = true) }
    
    val imageRequest = remember(cleanUrl) {
        coil.request.ImageRequest.Builder(context)
            .data(cleanUrl)
            .apply {
                if (isVideo) {
                    // Force VideoFrameDecoder regardless of mime type for .mp4 extensions
                    decoderFactory(ForceVideoDecoderFactory())
                    crossfade(true)
                    // Set a placeholder to verify loading start
                    placeholder(android.R.drawable.ic_menu_gallery) // 使用系统内置图标作为占位
                    error(android.R.drawable.stat_notify_error) // Fallback if decoding fails
                } else if (cleanUrl.endsWith(".gif", ignoreCase = true)) {
                     decoderFactory(coil.decode.GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        
        // 如果是视频，额外显示一个小的标识 (可选)
        if (isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}

@Composable
fun DiscoveryBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = Color.Black.copy(alpha = 0.95f), // Matching dark theme more closely
            modifier = Modifier.height(80.dp), // Slightly taller for better spacing
            tonalElevation = 0.dp // Use background instead
        ) {
            val items = listOf(
                Triple(Screen.Discovery.route, Icons.Default.Explore, "探索"),
                Triple(Screen.Search.route, Icons.Default.Search, "搜索"),
                Triple("", Icons.Default.Add, ""), // Placeholder for FAB
                Triple(Screen.Community.route, Icons.Default.AutoGraph, "动态"),
                Triple(Screen.Profile.route, Icons.Default.Person, "我的")
            )

            items.forEachIndexed { index, item ->
                if (index == 2) {
                    // Spacer for FAB
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Spacer(Modifier.size(24.dp)) },
                        label = { Text("", fontSize = 10.sp) },
                        enabled = false
                    )
                } else {
                    val isSelected = currentRoute == item.first
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.first) {
                                navController.navigate(item.first) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { 
                            Icon(
                                item.second, 
                                contentDescription = item.third,
                                modifier = Modifier.size(26.dp)
                            ) 
                        },
                        label = { 
                            Text(
                                item.third, 
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RunningHubTeal,
                            selectedTextColor = RunningHubTeal,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }

        // Floating Action Button (+)
        Box(
            modifier = Modifier
                .offset(y = (-36).dp)
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(RunningHubTeal, Color(0xFF00D1FF))
                    ),
                    shape = CircleShape
                )
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .clickable { /* Handle Create Action */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create",
                modifier = Modifier.size(32.dp),
                tint = Color.Black
            )
        }
    }
}

/**
 * A custom factory that bypasses the contentType/applicability check
 * inside VideoFrameDecoder.Factory.
 */
class ForceVideoDecoderFactory : Decoder.Factory {

    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
        return VideoFrameDecoder(result.source, options)
    }
    
    override fun equals(other: Any?) = other is ForceVideoDecoderFactory
    override fun hashCode() = javaClass.hashCode()
}
