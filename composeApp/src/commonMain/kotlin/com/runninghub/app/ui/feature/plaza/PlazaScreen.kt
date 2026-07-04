package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.RhImagePreviewOverlay
import com.runninghub.app.ui.component.RhVideoPreviewOverlay
import com.runninghub.app.ui.component.VideoPreviewItem
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCard
import com.runninghub.app.ui.designsystem.components.states.RhEmptyState
import com.runninghub.app.ui.designsystem.components.states.RhErrorState
import com.runninghub.app.ui.designsystem.components.states.RhLoadingState
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.feature.creator.CreatorProfileScreen
import com.runninghub.feature.community.presentation.PlazaMode
import com.runninghub.feature.community.presentation.PlazaPresentationError
import com.runninghub.feature.community.presentation.PlazaWorkDetailUiModel
import com.runninghub.feature.community.presentation.PlazaUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.error_state_retry_action
import runninghub.composeapp.generated.resources.plaza_empty_creations
import runninghub.composeapp.generated.resources.plaza_empty_shorts
import runninghub.composeapp.generated.resources.plaza_error_creations_load_failed
import runninghub.composeapp.generated.resources.plaza_error_shorts_load_failed
import runninghub.composeapp.generated.resources.plaza_loading_more

class PlazaVoyagerScreen(
    private val onUseSameWork: (PlazaWorkDetailUiModel) -> Unit = {},
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel: PlazaScreenModel = koinScreenModel()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadInitialData() }

        PlazaScreenContent(
            uiState = uiState,
            onRefresh = screenModel::refreshCreations,
            onTagSelected = screenModel::selectTag,
            onSortSelected = screenModel::selectSort,
            onModeSelected = screenModel::selectMode,
            onLoadMoreCreations = screenModel::loadMoreCreations,
            onLoadMoreShorts = screenModel::loadMoreShorts,
            onShortCategorySelected = screenModel::selectShortCategory,
            onOpenWorkDetail = screenModel::openWorkDetail,
            onDismissWorkDetail = screenModel::dismissWorkDetail,
            onUseSameWork = onUseSameWork,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlazaScreenContent(
    uiState: PlazaUiState,
    onRefresh: () -> Unit,
    onTagSelected: (String?) -> Unit,
    onSortSelected: (String) -> Unit,
    onModeSelected: (PlazaMode) -> Unit,
    onLoadMoreCreations: () -> Unit,
    onLoadMoreShorts: () -> Unit,
    onShortCategorySelected: (String?) -> Unit,
    onOpenWorkDetail: (String) -> Unit = {},
    onDismissWorkDetail: () -> Unit = {},
    onUseSameWork: (PlazaWorkDetailUiModel) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.currentOrThrow
    val windowInfo = LocalRhWindowInfo.current
    val columns = if (windowInfo.windowWidth < 340.dp) 1 else 2
    val showingShorts = uiState.mode == PlazaMode.SHORTS
    val creationGridState = rememberLazyStaggeredGridState()
    val shortGridState = rememberLazyGridState()
    val currentOnLoadMoreCreations = rememberUpdatedState(onLoadMoreCreations)
    val currentOnLoadMoreShorts = rememberUpdatedState(onLoadMoreShorts)
    val previewItems = remember(uiState.creations) { plazaCreationPreviewItems(uiState.creations) }
    var previewSelectedIndex by remember { mutableStateOf<Int?>(null) }
    var previewShortItem by remember { mutableStateOf<VideoPreviewItem?>(null) }
    val activePreviewSelectedIndex = previewSelectedIndex?.takeIf { it in previewItems.indices } ?: -1
    val hasActivePreview = activePreviewSelectedIndex >= 0 || previewShortItem != null

    // 滚动接近底部时自动触发分页；StateHolder 仍负责最终的重复请求保护。
    LaunchedEffect(
        creationGridState,
        showingShorts,
        uiState.hasMore,
        uiState.isLoading,
        uiState.isLoadingMore,
        uiState.creations.size,
    ) {
        if (showingShorts) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = creationGridState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            plazaShouldAutoLoadMore(
                loadedItemCount = uiState.creations.size,
                lastVisibleItemIndex = lastVisibleItemIndex,
                hasMore = uiState.hasMore,
                isLoading = uiState.isLoading || uiState.isLoadingMore,
            )
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    currentOnLoadMoreCreations.value()
                }
            }
    }

    LaunchedEffect(
        shortGridState,
        showingShorts,
        uiState.shortHasMore,
        uiState.isShortsLoading,
        uiState.shorts.size,
    ) {
        if (!showingShorts) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = shortGridState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
            plazaShouldAutoLoadMore(
                loadedItemCount = uiState.shorts.size,
                lastVisibleItemIndex = lastVisibleItemIndex,
                hasMore = uiState.shortHasMore,
                isLoading = uiState.isShortsLoading,
            )
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore) {
                    currentOnLoadMoreShorts.value()
                }
            }
    }

    LaunchedEffect(previewItems, previewSelectedIndex) {
        if (previewSelectedIndex != null && activePreviewSelectedIndex == -1) {
            previewSelectedIndex = null
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = RhTheme.colors.surfaceDefault,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RhTheme.colors.surfaceDefault),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.md)
                    .widthIn(max = windowInfo.feedContentMaxWidth)
                    .then(if (hasActivePreview) Modifier.blur(10.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
            ) {
                PlazaHeader(onRefresh = onRefresh)
                PlazaModeSortRow(
                    selectedMode = uiState.mode,
                    onModeSelected = onModeSelected,
                    selectedSort = uiState.sort,
                    onSortSelected = onSortSelected,
                )
                if (showingShorts) {
                    PlazaShortCategoryRow(
                        categories = uiState.shortCategories,
                        selectedCode = uiState.selectedShortCategoryCode,
                        onCategorySelected = onShortCategorySelected,
                    )
                } else {
                    PlazaTagRow(
                        tags = uiState.tags,
                        selectedTagId = uiState.selectedTagId,
                        onTagSelected = onTagSelected,
                    )
                }

                val errorMessage = uiState.error?.let { plazaPresentationErrorMessage(it) }
                when {
                    showingShorts && uiState.isShortsLoading && uiState.shorts.isEmpty() ->
                        PlazaCenteredState {
                            RhLoadingState(title = stringResource(Res.string.plaza_loading_more))
                        }
                    !showingShorts && uiState.isLoading && uiState.creations.isEmpty() ->
                        PlazaCenteredState {
                            RhLoadingState(title = stringResource(Res.string.plaza_loading_more))
                        }
                    errorMessage != null && showingShorts && uiState.shorts.isEmpty() ->
                        PlazaCenteredState {
                            RhErrorState(
                                title = errorMessage,
                                actionLabel = stringResource(Res.string.error_state_retry_action),
                                onAction = onRefresh,
                            )
                        }
                    errorMessage != null && !showingShorts && uiState.creations.isEmpty() ->
                        PlazaCenteredState {
                            RhErrorState(
                                title = errorMessage,
                                actionLabel = stringResource(Res.string.error_state_retry_action),
                                onAction = onRefresh,
                            )
                        }
                    showingShorts -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = shortGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 92.dp),
                            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(RhSpacing.md),
                        ) {
                            if (uiState.shorts.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    RhEmptyState(title = stringResource(Res.string.plaza_empty_shorts))
                                }
                            } else {
                                gridItemsIndexed(uiState.shorts, key = { _, card -> card.id }) { _, card ->
                                    val shortPreviewItem = plazaShortPreviewItem(card)
                                    PlazaShortTile(
                                        card = card,
                                        onPreviewClick = shortPreviewItem?.let { item ->
                                            {
                                                previewSelectedIndex = null
                                                previewShortItem = item
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(columns),
                            state = creationGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 92.dp),
                            horizontalArrangement = Arrangement.spacedBy(RhSpacing.sm),
                            verticalItemSpacing = RhSpacing.sm,
                        ) {
                            if (uiState.creations.isEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    RhEmptyState(title = stringResource(Res.string.plaza_empty_creations))
                                }
                            } else {
                                staggeredItemsIndexed(uiState.workCards, key = { _, card -> card.id }) { _, workCard ->
                                    val cardState = workCard.toPlazaWorkCardState()
                                    PlazaWorkCard(
                                        state = cardState,
                                        onClick = { onOpenWorkDetail(workCard.id) },
                                        onUseSame = { onOpenWorkDetail(workCard.id) },
                                        onAuthorClick = { ownerId -> navigator.push(CreatorProfileScreen(ownerId)) },
                                        authorAvatarContent = {
                                            PlazaWorkCardAuthorAvatar(
                                                avatarUrl = cardState.authorAvatar,
                                                contentDescription = cardState.authorName,
                                            )
                                        },
                                        previewContent = { preview ->
                                            PlazaWorkCardPreview(preview = preview)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activePreviewSelectedIndex >= 0) {
                RhImagePreviewOverlay(
                    items = previewItems,
                    selectedIndex = activePreviewSelectedIndex,
                    onSelectedIndexChange = { index ->
                        if (previewItems.getOrNull(index) != null) {
                            previewSelectedIndex = index
                        }
                    },
                    onDismiss = { previewSelectedIndex = null },
                    modifier = Modifier.fillMaxSize(),
                    hasMoreItems = uiState.hasMore,
                    isLoadingMoreItems = uiState.isLoading || uiState.isLoadingMore,
                    onLoadMoreItems = { currentOnLoadMoreCreations.value() },
                )
            }

            previewShortItem?.let { item ->
                RhVideoPreviewOverlay(
                    item = item,
                    onDismiss = { previewShortItem = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            uiState.selectedWorkDetail?.let { detail ->
                PlazaReuseTemplateOverlay(
                    detail = detail,
                    onDismiss = onDismissWorkDetail,
                    onConfirm = {
                        onUseSameWork(detail)
                        onDismissWorkDetail()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/** 把 Rh 空/加载/错误占位状态居中铺满内容区，保持三态视觉一致。 */
@Composable
private fun PlazaCenteredState(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun plazaPresentationErrorMessage(error: PlazaPresentationError): String =
    when (error) {
        PlazaPresentationError.CreationsLoadFailed -> stringResource(Res.string.plaza_error_creations_load_failed)
        PlazaPresentationError.ShortsLoadFailed -> stringResource(Res.string.plaza_error_shorts_load_failed)
    }
