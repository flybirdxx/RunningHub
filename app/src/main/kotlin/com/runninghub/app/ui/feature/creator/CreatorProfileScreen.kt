package com.runninghub.app.ui.feature.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.runninghub.app.data.remote.model.WebAppDto
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.feature.discovery.AiApp
import com.runninghub.app.ui.feature.discovery.AiAppMasonryItem
import com.runninghub.app.ui.navigation.Screen
import com.runninghub.app.ui.theme.RunningHubTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileScreen(
    userId: String,
    navController: NavController,
    viewModel: CreatorProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadCreatorProfile(userId)
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
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("创作者主页") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black) // Match dark theme background
        ) {
            when {
                // 1. Loading State (Exclusive)
                uiState.isLoading && uiState.webAppList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = RunningHubTeal,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                // 2. Error State (Exclusive)
                uiState.error != null && uiState.webAppList.isEmpty() -> {
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
                                text = uiState.error!!,
                                color = Color.Red,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.loadCreatorProfile(userId) },
                                colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
                            ) {
                                Text("重试", color = Color.White)
                            }
                        }
                    }
                }
                
                // 3. Content State
                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        state = gridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalItemSpacing = 12.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header Section (Full Width)
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            CreatorHeader(
                                creator = uiState.creatorInfo,
                                isFollowing = uiState.isFollowing,
                                onFollowClick = { viewModel.toggleFollow() }
                            )
                        }

                        // Empty State Handling
                        if (uiState.webAppList.isEmpty() && !uiState.isLoading) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Use a default icon if specific asset not available, usually a box or file icon
                                        Icon(
                                            imageVector = Icons.Default.Info, // Placeholder for "No Data" icon
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
                            items(uiState.webAppList) { webApp ->
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

                        // Footer Loader (for pagination)
                        if (uiState.isLoading && uiState.webAppList.isNotEmpty()) {
                            item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = RunningHubTeal, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorHeader(
    creator: com.runninghub.app.data.remote.model.AuthorDto?,
    isFollowing: Boolean,
    onFollowClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Banner Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF2C2C2C), Color(0xFF1E1E1E))
                    )
                )
        ) {
            val bannerUrl = creator?.bgImage.takeIf { !it.isNullOrEmpty() }
                ?: "https://www.runninghub.cn/_nuxt/personalCenter-banner.miJdyDnw.png"
            
            SmartAsyncImage(
                imageUrl = bannerUrl!!,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 2. Profile Info Area
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Row for Avatar and Follow Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-32).dp), // Overlap banner
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar
                SmartAsyncImage(
                    imageUrl = creator?.avatar ?: "",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )

                // Follow Button
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.padding(bottom = 4.dp, end = 4.dp).height(32.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFF333333) else RunningHubTeal,
                        contentColor = if (isFollowing) Color.White else Color.Black
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isFollowing) "已关注" else "+ 关注",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Move content up slightly due to negative offset
            Column(modifier = Modifier.offset(y = (-24).dp)) {
                // Name and ID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creator?.name ?: "Unknown Creator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    if (!creator?.id.isNullOrEmpty()) {
                        Text(
                            text = "ID:${creator?.id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))

                // Bio
                Text(
                    text = creator?.intro ?: "这人很懒，什么都没写~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(16.dp))

                // Stats Row
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StatLabelValue("关注", creator?.followCount ?: "0")
                    StatLabelValue("粉丝", creator?.fansCount ?: "0")
                    StatLabelValue("获赞", creator?.likeCount ?: "0")
                    StatLabelValue("收藏", creator?.collectCount ?: "0")
                }
            }
        }
    }
}

@Composable
fun StatLabelValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 16.sp
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp)
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
