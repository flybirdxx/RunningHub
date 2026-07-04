package com.runninghub.app.ui.feature.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.components.buttons.RhButton
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.components.buttons.RhPrimaryButton
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBar
import com.runninghub.app.ui.designsystem.components.states.RhEmptyState
import com.runninghub.app.ui.designsystem.components.states.RhErrorState
import com.runninghub.app.ui.designsystem.components.states.RhLoadingState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.feature.discovery.DiscoveryAppCard
import com.runninghub.core.model.User
import com.runninghub.feature.auth.presentation.creator.CreatorProfileError
import com.runninghub.feature.auth.presentation.creator.CreatorProfileUiState
import com.runninghub.feature.discovery.presentation.toDiscoveryAppCardUiModel
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.creator_profile_back_content_description
import runninghub.composeapp.generated.resources.creator_profile_default_title
import runninghub.composeapp.generated.resources.creator_profile_empty_apps
import runninghub.composeapp.generated.resources.creator_profile_error_load_failed
import runninghub.composeapp.generated.resources.creator_profile_follow_action
import runninghub.composeapp.generated.resources.creator_profile_following
import runninghub.composeapp.generated.resources.creator_profile_loading
import runninghub.composeapp.generated.resources.creator_profile_published_apps_title
import runninghub.composeapp.generated.resources.creator_profile_retry_action
import runninghub.composeapp.generated.resources.creator_profile_stat_fans
import runninghub.composeapp.generated.resources.creator_profile_stat_following
import runninghub.composeapp.generated.resources.creator_profile_stat_likes
import runninghub.composeapp.generated.resources.creator_profile_unknown_user

/**
 * 创作者主页的 Voyager Screen。
 *
 * 该 Screen 属于应用壳层，负责把 Voyager 导航参数交给认证 Presentation 状态机，
 * 并把作品点击继续转发到应用详情页；创作者资料加载、关注状态和作品列表去重等业务状态
 * 已由 `feature:auth:presentation` 承担，避免页面直接持有 Data 实现。
 *
 * @property userId 服务端创作者用户 ID，来自上游页面导航参数。
 * 空字符串不应由正常导航传入；该值会参与 ScreenKey 生成，并用于触发资料加载请求。
 */
data class CreatorProfileScreen(val userId: String) : Screen {

    override val key: ScreenKey get() = "CreatorProfile_$userId"

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<CreatorProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(userId) { screenModel.loadProfile(userId) }

        ProfileScaffold(
            uiState = uiState,
            onBack = { navigator.pop() },
            onReload = { screenModel.loadProfile(userId) },
            onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
            onToggleFollow = screenModel::toggleFollow,
        )
    }
}

@Composable
private fun creatorProfileErrorMessage(error: CreatorProfileError): String =
    when (error) {
        CreatorProfileError.LoadFailed -> stringResource(Res.string.creator_profile_error_load_failed)
    }

/* ──────────────────────── 主体骨架 ──────────────────────── */

@Composable
private fun ProfileScaffold(
    uiState: CreatorProfileUiState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onAppClick: (String) -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowInfo = LocalRhWindowInfo.current
    // Presentation 只输出稳定错误语义，应用壳在靠近 UI 的位置映射本地化文案。
    val errorMessage = uiState.error?.let { creatorProfileErrorMessage(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RhTheme.colors.backgroundPrimary),
    ) {
        RhTopBar(
            title = uiState.user?.nickName ?: stringResource(Res.string.creator_profile_default_title),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            Res.string.creator_profile_back_content_description,
                        ),
                        tint = RhTheme.colors.textPrimary,
                    )
                }
            },
        )

        when {
            uiState.isLoading && uiState.user == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                RhLoadingState(title = stringResource(Res.string.creator_profile_loading))
            }

            errorMessage != null && uiState.user == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                RhErrorState(
                    title = errorMessage,
                    actionLabel = stringResource(Res.string.creator_profile_retry_action),
                    onAction = onReload,
                )
            }

            else -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = windowInfo.feedGridMinCardWidth),
                    // batch-2 经验：横向内距使用 sm/8dp，避免 360dp 手机掉到单列。
                    contentPadding = PaddingValues(
                        start = RhSpacing.sm,
                        end = RhSpacing.sm,
                        bottom = RhSpacing.huge,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = windowInfo.feedContentMaxWidth),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ProfileHeader(
                            user = uiState.user,
                            isFollowing = uiState.isFollowing,
                            onToggleFollow = onToggleFollow,
                        )
                    }

                    if (uiState.apps.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(Res.string.creator_profile_published_apps_title),
                                style = RhTypography.sectionTitle,
                                color = RhTheme.colors.textPrimary,
                                modifier = Modifier.padding(
                                    horizontal = RhSpacing.sm,
                                    vertical = RhSpacing.sm,
                                ),
                            )
                        }
                    }

                    // 部分作品的 WebApp id 可能为空，直接以 id 作为 key 会因空串重复触发
                    // LazyGrid 的重复 key 崩溃；对空 id 回退为带下标的稳定 key。
                    itemsIndexed(
                        items = uiState.apps,
                        key = { index, app -> app.id.ifBlank { "creator-app-$index" } },
                    ) { _, app ->
                        DiscoveryAppCard(
                            card = app.toDiscoveryAppCardUiModel(),
                            onClick = { onAppClick(app.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (uiState.apps.isEmpty() && !uiState.isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = RhSpacing.huge),
                                contentAlignment = Alignment.Center,
                            ) {
                                RhEmptyState(
                                    title = stringResource(Res.string.creator_profile_empty_apps),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ──────────────────────── 资料头部 ──────────────────────── */

private val AvatarSize = 88.dp

@Composable
private fun ProfileHeader(
    user: User?,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val followButtonText = if (isFollowing) {
        stringResource(Res.string.creator_profile_following)
    } else {
        stringResource(Res.string.creator_profile_follow_action)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 顶部渐变只承担视觉识别，不承载远端数据，避免资料加载失败时出现空白页头。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(bottomStart = RhTheme.shapes.lg, bottomEnd = RhTheme.shapes.lg))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RhTheme.colors.brandMuted, RhTheme.colors.surfaceElevated),
                    ),
                ),
        )

        // 头像与关注按钮同属于资料主操作区，关注状态由 Presentation StateHolder 统一维护。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RhSpacing.lg)
                .offset(y = -(AvatarSize / 2)),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SmartAsyncImage(
                imageUrl = user?.headIcon,
                contentDescription = user?.nickName
                    ?: stringResource(Res.string.creator_profile_unknown_user),
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .border(3.dp, RhTheme.colors.backgroundPrimary, CircleShape),
            )

            if (isFollowing) {
                RhButton(
                    text = followButtonText,
                    onClick = onToggleFollow,
                    style = RhButtonStyle.Secondary,
                )
            } else {
                RhPrimaryButton(
                    text = followButtonText,
                    onClick = onToggleFollow,
                )
            }
        }

        // 资料统计保留服务端返回的展示字符串，只把静态标签放入 Compose Resources。
        Column(
            modifier = Modifier
                .padding(horizontal = RhSpacing.lg)
                .offset(y = -(RhSpacing.xxl)),
        ) {
            Text(
                text = user?.nickName ?: stringResource(Res.string.creator_profile_unknown_user),
                style = RhTypography.pageTitle,
                color = RhTheme.colors.textPrimary,
            )

            if (!user?.introduce.isNullOrBlank()) {
                Spacer(Modifier.height(RhSpacing.xs))
                Text(
                    text = user.introduce.orEmpty(),
                    style = RhTypography.body,
                    color = RhTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(RhSpacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(RhSpacing.xxl)) {
                ProfileStat(
                    label = stringResource(Res.string.creator_profile_stat_fans),
                    value = user?.fanCount ?: "0",
                )
                ProfileStat(
                    label = stringResource(Res.string.creator_profile_stat_following),
                    value = user?.followCount ?: "0",
                )
                ProfileStat(
                    label = stringResource(Res.string.creator_profile_stat_likes),
                    value = user?.likeCount ?: "0",
                )
            }
        }
    }
}

@Composable
private fun ProfileStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = value,
            style = RhTypography.sectionTitle,
            color = RhTheme.colors.textPrimary,
        )
        Text(
            text = label,
            style = RhTypography.caption,
            color = RhTheme.colors.textTertiary,
        )
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
            onReload = {},
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
            onReload = {},
            onAppClick = {},
            onToggleFollow = {},
        )
    }
}
