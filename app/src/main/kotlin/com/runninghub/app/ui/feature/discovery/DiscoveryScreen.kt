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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.runninghub.app.ui.component.SmartAsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.runninghub.app.ui.component.FabMenuOverlay
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.valentinilk.shimmer.shimmer
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.ui.navigation.Screen
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import coil.decode.Decoder
import coil.decode.VideoFrameDecoder
import coil.request.Options
import coil.fetch.SourceResult
import coil.ImageLoader

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runninghub.app.ui.feature.profile.ProfileViewModel
import com.runninghub.app.ui.feature.profile.ProfileSettingsDialog

@Composable
fun DiscoveryShimmerLoading() {
    Column(Modifier.padding(horizontal = 16.dp)) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.8f)
                            .clip(RoundedCornerShape(12.dp))
                            .shimmer()
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    uiState: DiscoveryUiState,
    navController: NavHostController,
    onCategorySelected: (Category) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        ProfileSettingsDialog(
            uiState = profileUiState,
            onDismiss = { showProfileDialog = false },
            onNavigateToHistory = {
                showProfileDialog = false
                navController.navigate(Screen.TaskHistory.route)
            },
            onBindApiKey = profileViewModel::bindApiKey,
            onBindEnterpriseApiKey = profileViewModel::bindEnterpriseApiKey,
            onUnbindApiKey = profileViewModel::unbindApiKey
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { 
                DiscoveryHeader(
                    onRefresh = onRefresh,
                    user = profileUiState.user,
                    onAvatarClick = { showProfileDialog = true }
                ) 
            },
            bottomBar = { 
                DiscoveryBottomNav(
                    navController = navController,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = !isMenuExpanded }
                ) 
            }
        ) { padding ->
        val pullRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // 1. Banner
                if (uiState.banners.isNotEmpty()) {
                    item(key = "banners") {
                        BannerSection(
                            banners = uiState.banners,
                            onBannerClick = { appId ->
                                navController.navigate(Screen.AppDetail.createRoute(appId))
                            }
                        )
                    }
                }

                // 2. Categories
                item(key = "categories") {
                    CategoryPills(uiState.categories, uiState.selectedCategory, onCategorySelected)
                }

                // 5. Discovery Feed (Discovery 作品使用瀑布流)
                if (uiState.isLoading && uiState.discoveryApps.isEmpty()) {
                    // Full area loading animation
                    item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 100.dp, bottom = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = RunningHubTeal,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "正在寻找优质作品...",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else if (uiState.discoveryApps.isEmpty() && !uiState.isLoading) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Spacer(Modifier.height(16.dp))
                                Text("暂无作品", color = Color.Gray, fontSize = 16.sp)
                            }
                        }
                    }
                } else {
                    val chunkedApps = uiState.discoveryApps.chunked(2)
                    items(
                        items = chunkedApps,
                        key = { it.first().id } // Use first item id as stable key
                    ) { rowApps ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp, start = 16.dp, end = 16.dp)) {
                            for (app in rowApps) {
                                AiAppMasonryItem(
                                    app = app, 
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Screen.AppDetail.createRoute(app.id)) }
                                )
                            }
                            if (rowApps.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    
                    // 加载更多提示
                    if (uiState.discoveryApps.isNotEmpty()) {
                        item(key = "load_more") {
                            // 监听列表滚动到底部，用 snapshotFlow 更加精确地防抖和过滤
                            LaunchedEffect(listState) {
                                androidx.compose.runtime.snapshotFlow {
                                    val totalItems = listState.layoutInfo.totalItemsCount
                                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    // 预加载阈值设为最后3个元素之前
                                    totalItems > 0 && lastVisibleItemIndex >= totalItems - 3
                                }
                                .distinctUntilChanged()
                                .filter { it } // 只过滤状态从 false 变为 true 这一触发瞬间
                                .collect {
                                    if (uiState.hasMore && !uiState.isLoadingMore && !uiState.isRefreshing) {
                                        onLoadMore()
                                    }
                                }
                            }

                            if (uiState.isLoadingMore) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RunningHubTeal, strokeWidth = 2.dp)
                                }
                            } else if (uiState.hasMore) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().clickable { onLoadMore() }.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("下拉或点击加载更多", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                Text(
                                    "没有更多作品了",
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else if (!uiState.isRefreshing) {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

        FabMenuOverlay(
            isVisible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            onMenuItemClick = { title ->
                if (title == "音频处理 API") {
                    navController.navigate(Screen.AudioGeneration.route)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryHeader(
    onRefresh: () -> Unit = {},
    user: com.runninghub.app.data.remote.model.UserDto? = null,
    onAvatarClick: () -> Unit = {}
) {
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
            if (user?.headIcon != null) {
                AsyncImage(
                    model = user.headIcon,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(RunningHubTeal, Color.Blue)))
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(20.dp), tint = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSection(banners: List<Banner>, onBannerClick: (String) -> Unit) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onBannerClick(banner.id) }
            ) {
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



@Composable
fun DiscoveryBottomNav(
    navController: NavHostController,
    isMenuExpanded: Boolean,
    onMenuToggle: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = Color.Black.copy(alpha = 0.95f),
            modifier = Modifier.height(80.dp),
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple(Screen.Discovery.route, Icons.Default.Explore, "探索"),
                Triple(Screen.Search.route, Icons.Default.Search, "搜索"),
                Triple("", Icons.Default.Add, ""),
                Triple(Screen.Community.route, Icons.Default.Build, "创意工坊"),
                Triple(Screen.Profile.route, Icons.Default.Person, "我的")
            )

            items.forEachIndexed { index, item ->
                if (index == 2) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Spacer(Modifier.size(24.dp)) },
                        label = { Text("", fontSize = 10.sp) },
                        enabled = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
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
        val rotation by animateFloatAsState(
            targetValue = if (isMenuExpanded) 45f else 0f,
            label = "fab_rotation"
        )

        Box(
            modifier = Modifier
                .offset(y = (-36).dp)
                .size(56.dp)
                .graphicsLayer { rotationZ = rotation }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(RunningHubTeal, Color(0xFF00D1FF))
                    ),
                    shape = CircleShape
                )
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .clickable { onMenuToggle() },
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


