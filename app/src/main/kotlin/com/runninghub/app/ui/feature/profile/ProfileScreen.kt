package com.runninghub.app.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.runninghub.app.ui.component.FabMenuOverlay
import com.runninghub.app.ui.feature.creator.CreatorHeader
import com.runninghub.app.ui.feature.creator.CreatorProfileViewModel
import com.runninghub.app.ui.feature.discovery.AiAppMasonryItem
import com.runninghub.app.ui.feature.discovery.DiscoveryBottomNav
import com.runninghub.app.ui.feature.discovery.AiApp
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.data.remote.model.WebAppDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    navController: NavHostController,
    onRefresh: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onBindApiKey: (String) -> Unit = {},
    onBindEnterpriseApiKey: (String) -> Unit = {},
    onUnbindApiKey: () -> Unit = {}
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val creatorViewModel: CreatorProfileViewModel = hiltViewModel()
    val creatorUiState by creatorViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.user?.id) {
        uiState.user?.id?.let { userId ->
            creatorViewModel.loadCreatorProfile(userId)
        }
    }

    val gridState = rememberLazyStaggeredGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && totalItems - lastVisibleItemIndex < 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            creatorViewModel.loadMore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                DiscoveryBottomNav(
                    navController = navController,
                    isMenuExpanded = isMenuExpanded,
                    onMenuToggle = { isMenuExpanded = !isMenuExpanded }
                )
            },
            containerColor = Color.Black
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (!uiState.hasApiKey) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请在首页右上角绑定 API Key 登录", color = Color.Gray)
                    }
                } else {
                    when {
                        creatorUiState.isLoading && creatorUiState.webAppList.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    color = RunningHubTeal,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        creatorUiState.error != null && creatorUiState.webAppList.isEmpty() -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = creatorUiState.error!!,
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = { uiState.user?.id?.let { creatorViewModel.loadCreatorProfile(it) } },
                                        colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
                                    ) {
                                        Text("重试", color = Color.White)
                                    }
                                }
                            }
                        }

                        else -> {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                state = gridState,
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalItemSpacing = 12.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                    // CreatorHeader expects author object
                                    CreatorHeader(
                                        creator = creatorUiState.creatorInfo,
                                        isFollowing = false,
                                        onFollowClick = { }
                                    )
                                }

                                if (creatorUiState.webAppList.isEmpty() && !creatorUiState.isLoading) {
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(64.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Text(
                                                    text = "暂无作品",
                                                    color = Color.Gray,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    items(creatorUiState.webAppList) { webApp ->
                                        val aiApp = webApp.toAiApp()
                                        AiAppMasonryItem(
                                            app = aiApp,
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                navController.navigate(Screen.AppDetail.createRoute(aiApp.id))
                                            }
                                        )
                                    }
                                }

                                if (creatorUiState.isLoading && creatorUiState.webAppList.isNotEmpty()) {
                                    item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                        Box(
                                            Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = RunningHubTeal,
                                                modifier = Modifier.size(24.dp)
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

// Extension to map WebAppDto to AiApp
private fun WebAppDto.toAiApp(): AiApp {
    return AiApp(
        id = this.id ?: "",
        title = this.title ?: "Untitled",
        author = this.author?.name ?: "Anonymous",
        authorAvatar = this.author?.avatar,
        imageUrl = this.thumbnailUrl ?: this.covers?.firstOrNull()?.url ?: this.preview?.url ?: "",
        likes = this.statisticsInfo?.likeCount?.toIntOrNull() ?: this.likeCount?.toIntOrNull() ?: 0,
        stars = this.statisticsInfo?.collectCount?.toIntOrNull() ?: this.collectCount?.toIntOrNull() ?: 0,
        useCount = this.statisticsInfo?.useCount ?: this.useCount ?: "0",
        views = this.statisticsInfo?.pv ?: this.pv ?: "0"
    )
}
