package com.runninghub.app.ui.feature.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.adaptive.previewUser
import com.runninghub.app.ui.adaptive.previewWebApp
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt
import com.runninghub.core.model.User
import com.runninghub.core.model.WebApp
import org.jetbrains.compose.ui.tooling.preview.Preview

data class CreatorProfileScreen(val userId: String) : Screen {

    override val key: ScreenKey get() = "CreatorProfile_$userId"

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<CreatorProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(userId) { screenModel.loadProfile(userId) }

        when {
            uiState.isLoading && uiState.user == null -> LoadingIndicator()
            uiState.error != null && uiState.user == null -> ErrorState(
                message = uiState.error!!,
                onRetry = { screenModel.loadProfile(userId) }
            )
            else -> ProfileScaffold(
                uiState = uiState,
                onBack = { navigator.pop() },
                onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
                onToggleFollow = screenModel::toggleFollow
            )
        }
    }
}

/* ──────────────────────── Main Scaffold ──────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScaffold(
    uiState: CreatorProfileUiState,
    onBack: () -> Unit,
    onAppClick: (String) -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowInfo = LocalRhWindowInfo.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.nickName ?: "创作者") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = windowInfo.feedGridMinCardWidth),
                contentPadding = PaddingValues(
                    start = Dimens.SpaceLG,
                    end = Dimens.SpaceLG,
                    bottom = Dimens.Space6XL
                ),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = windowInfo.feedContentMaxWidth)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ProfileHeader(
                        user = uiState.user,
                        isFollowing = uiState.isFollowing,
                        onToggleFollow = onToggleFollow
                    )
                }

                if (uiState.apps.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "发布的应用",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = Dimens.SpaceSM)
                        )
                    }
                }

                items(uiState.apps, key = { it.id }) { app ->
                    AppGridItem(
                        app = app,
                        onClick = { onAppClick(app.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (uiState.apps.isEmpty() && !uiState.isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimens.Space6XL),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无作品",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ──────────────────────── Profile Header ──────────────────────── */

@Composable
private fun ProfileHeader(
    user: User?,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extColors = RunningHubThemeExt.colors

    Column(modifier = modifier.fillMaxWidth()) {
        // Banner gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(bottomStart = Dimens.RadiusLG, bottomEnd = Dimens.RadiusLG))
                .background(
                    Brush.linearGradient(
                        colors = listOf(extColors.gradientStart, extColors.gradientEnd)
                    )
                )
        )

        // Avatar + Follow button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -(Dimens.AvatarSizeXL / 2)),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmartAsyncImage(
                imageUrl = user?.headIcon,
                contentDescription = user?.nickName,
                modifier = Modifier
                    .size(Dimens.AvatarSizeXL)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            Button(
                onClick = onToggleFollow,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isFollowing) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                ),
                shape = RoundedCornerShape(Dimens.RadiusFull),
                modifier = Modifier.height(Dimens.ButtonHeightSM)
            ) {
                Text(
                    text = if (isFollowing) "已关注" else "+ 关注",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Name + Bio + Stats
        Column(modifier = Modifier.offset(y = -(Dimens.SpaceXXL))) {
            Text(
                text = user?.nickName ?: "未知用户",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!user?.introduce.isNullOrBlank()) {
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = user.introduce.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(Dimens.SpaceLG))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXXL)) {
                ProfileStat(label = "粉丝", value = user?.fanCount ?: "0")
                ProfileStat(label = "关注", value = user?.followCount ?: "0")
                ProfileStat(label = "获赞", value = user?.likeCount ?: "0")
            }
        }
    }
}

@Composable
private fun ProfileStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ──────────────────────── App Grid Item ──────────────────────── */

@Composable
private fun AppGridItem(
    app: WebApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isNarrowCard = maxWidth < 180.dp

        Card(
            onClick = onClick,
            shape = RoundedCornerShape(Dimens.RadiusMD),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.ElevationSM),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SmartAsyncImage(
                    imageUrl = app.thumbnailUrl ?: app.coverUrl,
                    contentDescription = app.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(Dimens.SpaceSM)) {
                    Text(
                        text = app.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(Dimens.SpaceXS))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
                    ) {
                        Text(
                            text = "${app.useCount}次使用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!isNarrowCard) {
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${app.likeCount}赞",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun creatorProfilePreviewState(): CreatorProfileUiState = CreatorProfileUiState(
    isLoading = false,
    user = previewUser(),
    apps = listOf(
        previewWebApp(id = "creator-1", title = "Portrait Workflow for Social Posters"),
        previewWebApp(id = "creator-2", title = "Ultra Detail Product Upscale"),
        previewWebApp(id = "creator-3", title = "Anime Character Prompt Builder"),
        previewWebApp(id = "creator-4", title = "Fashion Studio Lighting Toolkit"),
    ),
    isFollowing = true,
)

@Preview
@Composable
private fun CreatorProfileCompactPreview() {
    RunningHubPreviewSurface(windowWidth = 360.dp, windowHeight = 800.dp) {
        ProfileScaffold(
            uiState = creatorProfilePreviewState(),
            onBack = {},
            onAppClick = {},
            onToggleFollow = {},
        )
    }
}

@Preview
@Composable
private fun CreatorProfileMediumPreview() {
    RunningHubPreviewSurface(windowWidth = 600.dp, windowHeight = 840.dp) {
        ProfileScaffold(
            uiState = creatorProfilePreviewState(),
            onBack = {},
            onAppClick = {},
            onToggleFollow = {},
        )
    }
}
