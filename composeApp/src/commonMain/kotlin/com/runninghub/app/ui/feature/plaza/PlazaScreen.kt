package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed as staggeredItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.component.ImagePreviewItem
import com.runninghub.app.ui.component.RhImagePreviewOverlay
import com.runninghub.app.ui.component.RhVideoPreviewOverlay
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoPreviewItem
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground as RhBackground
import com.runninghub.app.ui.theme.RhAppCard as RhCard
import com.runninghub.app.ui.theme.RhAppLine as RhLine
import com.runninghub.app.ui.theme.RhAppMuted as RhMuted
import com.runninghub.app.ui.theme.RhAppSelected as RhSelected
import com.runninghub.app.ui.theme.RhAppSurface as RhSurface
import com.runninghub.app.ui.theme.RhAppText as RhText
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import com.runninghub.feature.community.presentation.PlazaFallbackCardIntro
import com.runninghub.feature.community.presentation.PlazaFallbackCardMediaType
import com.runninghub.feature.community.presentation.PlazaFallbackCardOwner
import com.runninghub.feature.community.presentation.PlazaFallbackCardText
import com.runninghub.feature.community.presentation.PlazaFallbackTagLabel
import com.runninghub.feature.community.presentation.PlazaMode
import com.runninghub.feature.community.presentation.PlazaPresentationError
import com.runninghub.feature.community.presentation.PlazaUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_category_all
import runninghub.composeapp.generated.resources.plaza_default_creation_owner
import runninghub.composeapp.generated.resources.plaza_default_short_owner
import runninghub.composeapp.generated.resources.plaza_empty_creations
import runninghub.composeapp.generated.resources.plaza_empty_shorts
import runninghub.composeapp.generated.resources.plaza_error_creations_load_failed
import runninghub.composeapp.generated.resources.plaza_error_shorts_load_failed
import runninghub.composeapp.generated.resources.plaza_fallback_card_image_v2_intro
import runninghub.composeapp.generated.resources.plaza_fallback_card_owner_creator
import runninghub.composeapp.generated.resources.plaza_fallback_card_owner_runninghub_api
import runninghub.composeapp.generated.resources.plaza_fallback_card_seedream_intro
import runninghub.composeapp.generated.resources.plaza_fallback_card_video_workflow_intro
import runninghub.composeapp.generated.resources.plaza_fallback_card_workflow_intro
import runninghub.composeapp.generated.resources.plaza_fallback_catalog_tag_images
import runninghub.composeapp.generated.resources.plaza_fallback_catalog_tag_videos
import runninghub.composeapp.generated.resources.plaza_fallback_catalog_tag_workflows
import runninghub.composeapp.generated.resources.plaza_fallback_tag_api
import runninghub.composeapp.generated.resources.plaza_fallback_tag_avatar
import runninghub.composeapp.generated.resources.plaza_fallback_tag_photo
import runninghub.composeapp.generated.resources.plaza_fallback_tag_video_generation
import runninghub.composeapp.generated.resources.plaza_featured_badge
import runninghub.composeapp.generated.resources.plaza_filter_label
import runninghub.composeapp.generated.resources.plaza_image_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_like_count_format
import runninghub.composeapp.generated.resources.plaza_mode_creations
import runninghub.composeapp.generated.resources.plaza_mode_shorts
import runninghub.composeapp.generated.resources.plaza_refresh_content_description
import runninghub.composeapp.generated.resources.plaza_search_content_description
import runninghub.composeapp.generated.resources.plaza_short_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_sort_hot
import runninghub.composeapp.generated.resources.plaza_sort_latest
import runninghub.composeapp.generated.resources.plaza_sort_recommend
import runninghub.composeapp.generated.resources.plaza_title
import runninghub.composeapp.generated.resources.plaza_untitled_creation
import runninghub.composeapp.generated.resources.plaza_untitled_short
import runninghub.composeapp.generated.resources.plaza_use_count_format
import runninghub.composeapp.generated.resources.plaza_video_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_workflow_media_type_fallback
import kotlinx.coroutines.flow.distinctUntilChanged

class PlazaVoyagerScreen : Screen {
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
    modifier: Modifier = Modifier,
) {
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
        containerColor = RhBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RhBackground),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .widthIn(max = windowInfo.feedContentMaxWidth)
                    .then(if (hasActivePreview) Modifier.blur(10.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PlazaHeader(onRefresh = onRefresh)
                when (plazaSortDropdownAnchor()) {
                    PlazaSortDropdownAnchor.ModeTabs -> {
                        PlazaModeTabs(
                            selectedMode = uiState.mode,
                            onModeSelected = onModeSelected,
                            selectedSort = uiState.sort,
                            onSortSelected = onSortSelected,
                        )
                    }
                }
                if (showingShorts) {
                    PlazaShortCategoryRow(
                        categories = uiState.shortCategories,
                        selectedCode = uiState.selectedShortCategoryCode,
                        onCategorySelected = onShortCategorySelected,
                    )
                } else {
                    PlazaTagRow(
                        tags = uiState.tags,
                        fallbackTagLabels = uiState.fallbackTagLabels,
                        selectedTagId = uiState.selectedTagId,
                        onTagSelected = onTagSelected,
                    )
                }

                val errorMessage = uiState.error?.let { plazaPresentationErrorMessage(it) }
                when {
                    showingShorts && uiState.isShortsLoading && uiState.shorts.isEmpty() -> LoadingPanel()
                    !showingShorts && uiState.isLoading && uiState.creations.isEmpty() -> LoadingPanel()
                    errorMessage != null && showingShorts && uiState.shorts.isEmpty() -> ErrorPanel(errorMessage)
                    errorMessage != null && !showingShorts && uiState.creations.isEmpty() -> ErrorPanel(errorMessage)
                    showingShorts -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            state = shortGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 92.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (uiState.shorts.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyTile(message = stringResource(Res.string.plaza_empty_shorts))
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalItemSpacing = 8.dp,
                        ) {
                            if (uiState.creations.isEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    EmptyTile(message = stringResource(Res.string.plaza_empty_creations))
                                }
                            } else {
                                staggeredItemsIndexed(uiState.creations, key = { _, card -> card.id }) { creationIndex, card ->
                                    val videoPreviewItem = plazaCreationVideoPreviewItem(card)
                                    PlazaCreationTile(
                                        card = card,
                                        fallbackText = uiState.fallbackCreationTexts[card.id],
                                        onPreviewClick = {
                                            if (videoPreviewItem != null) {
                                                previewSelectedIndex = null
                                                previewShortItem = videoPreviewItem
                                            } else {
                                                previewShortItem = null
                                                previewSelectedIndex = plazaCreationPreviewIndex(
                                                    creations = uiState.creations,
                                                    creationIndex = creationIndex,
                                                )
                                            }
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
        }
    }
}

@Composable
private fun PlazaHeader(onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.plaza_title),
            color = RhText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {}, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Search,
                contentDescription = stringResource(Res.string.plaza_search_content_description),
                tint = RhText,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.plaza_refresh_content_description),
                tint = RhText,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun PlazaSortDropdown(
    selectedSort: String,
    onSortSelected: (String) -> Unit,
) {
    val options = plazaSortOptions(
        recommendLabel = stringResource(Res.string.plaza_sort_recommend),
        hotLabel = stringResource(Res.string.plaza_sort_hot),
        latestLabel = stringResource(Res.string.plaza_sort_latest),
    )
    val selectedLabel = plazaSelectedSortLabel(
        selectedSort = selectedSort,
        options = options,
    )
    var expanded by remember { mutableStateOf(false) }
    val metrics = plazaSortDropdownMetrics()
    val shape = RoundedCornerShape(metrics.cornerRadiusDp.dp)
    Box(
        modifier = Modifier
            .width(metrics.widthDp.dp)
            .height(metrics.heightDp.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(RhSurface)
                .border(1.dp, RhLine, shape)
                .clickable { expanded = true }
                .padding(horizontal = metrics.horizontalPaddingDp.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedLabel,
                color = RhText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.plaza_filter_label),
                tint = BrandLime,
                modifier = Modifier.size(metrics.iconSizeDp.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                val selected = selectedSort == option.value
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            color = if (selected) BrandLime else RhText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSortSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PlazaModeTabs(
    selectedMode: PlazaMode,
    onModeSelected: (PlazaMode) -> Unit,
    selectedSort: String,
    onSortSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        PlazaModeTab(
            stringResource(Res.string.plaza_mode_creations),
            selectedMode == PlazaMode.CREATIONS,
        ) { onModeSelected(PlazaMode.CREATIONS) }
        Spacer(Modifier.width(36.dp))
        PlazaModeTab(
            stringResource(Res.string.plaza_mode_shorts),
            selectedMode == PlazaMode.SHORTS,
        ) { onModeSelected(PlazaMode.SHORTS) }
        Spacer(Modifier.weight(1f))
        PlazaSortDropdown(
            selectedSort = selectedSort,
            onSortSelected = onSortSelected,
        )
    }
}

@Composable
private fun PlazaModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(label, color = if (selected) RhText else RhMuted, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Box(
            Modifier
                .width(18.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) BrandLime else Color.Transparent),
        )
    }
}

@Composable
private fun PlazaShortCategoryRow(
    categories: List<PlazaShortCategory>,
    selectedCode: String?,
    onCategorySelected: (String?) -> Unit,
) {
    val allLabel = stringResource(Res.string.plaza_category_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(allLabel, selectedCode == null) { onCategorySelected(null) }
        plazaVisibleShortCategories(categories).forEach { category ->
            TagChip(
                label = category.name.ifBlank { category.code },
                selected = selectedCode == category.code,
                onClick = { onCategorySelected(category.code) },
            )
        }
    }
}

@Composable
private fun PlazaTagRow(
    tags: List<PlazaTag>,
    fallbackTagLabels: Map<String, PlazaFallbackTagLabel>,
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit,
) {
    val allLabel = stringResource(Res.string.plaza_category_all)
    val visibleTags = plazaVisibleCreationTags(tags).ifEmpty {
        listOf(
            PlazaTag("video", stringResource(Res.string.plaza_fallback_tag_video_generation), 1, true),
            PlazaTag("avatar", stringResource(Res.string.plaza_fallback_tag_avatar), 1, true),
            PlazaTag("api", stringResource(Res.string.plaza_fallback_tag_api), 1, true),
            PlazaTag("photo", stringResource(Res.string.plaza_fallback_tag_photo), 1, true),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(allLabel, selectedTagId == null) { onTagSelected(null) }
        visibleTags.forEach { tag ->
            TagChip(
                label = fallbackTagLabels[tag.id]?.asText() ?: tag.name,
                selected = selectedTagId == tag.id,
                onClick = { onTagSelected(tag.id) },
            )
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) RhSelected else RhSurface)
            .border(1.dp, if (selected) BrandLime else RhLine, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) BrandLime else RhText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlazaCreationTile(
    card: PlazaCreationCard,
    fallbackText: PlazaFallbackCardText?,
    onPreviewClick: (() -> Unit)? = null,
) {
    val intro = fallbackText?.intro?.asText() ?: card.intro
    val ownerName = fallbackText?.owner?.asText() ?: card.ownerName
    val mediaType = fallbackText?.mediaType?.asText() ?: card.mediaType
    val mediaUrl = card.mediaUrl?.takeIf { it.isNotBlank() }
    val isVideo = plazaCreationIsVideo(card)
    val previewClickModifier = if (onPreviewClick != null && mediaUrl != null) {
        Modifier.clickable(onClick = onPreviewClick)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(plazaCreationTileAspectRatio(card))
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .background(RhCard)
            .then(previewClickModifier),
    ) {
        if (mediaUrl == null) {
            PlazaFallbackVisual(card, mediaType = mediaType)
        } else if (isVideo) {
            VideoThumbnail(
                url = mediaUrl,
                modifier = Modifier.fillMaxSize(),
                autoPlay = true,
            )
        } else {
            SmartAsyncImage(
                imageUrl = mediaUrl,
                contentDescription = intro,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color(0xCC000000),
                        ),
                    ),
                ),
        )
        Text(
            text = stringResource(Res.string.plaza_featured_badge),
            color = RhText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color(0x66000000))
                .padding(horizontal = 7.dp, vertical = 5.dp),
        )
        if (isVideo && mediaUrl != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.38f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (card.liked || card.collected) {
            Text(
                text = stringResource(Res.string.plaza_featured_badge),
                color = Color.Black,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandLime)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = intro ?: stringResource(Res.string.plaza_untitled_creation),
                color = RhText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(ownerName, mediaType)
                    .joinToString(" / ")
                    .ifBlank { stringResource(Res.string.plaza_default_creation_owner) },
                color = RhMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.plaza_like_count_format, card.likeCount ?: "0"),
                    color = RhText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(Res.string.plaza_use_count_format, card.useCount ?: "0"),
                    color = RhText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlazaShortTile(
    card: PlazaShortCard,
    onPreviewClick: (() -> Unit)? = null,
) {
    val previewClickModifier = if (onPreviewClick != null) {
        Modifier.clickable(onClick = onPreviewClick)
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(previewClickModifier),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val videoUrl = card.videoUrl?.takeIf { it.isNotBlank() }
        val posterUrl = card.thumbnailUrl?.takeIf { it.isNotBlank() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(plazaShortThumbnailAspectRatio())
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, RhLine, RoundedCornerShape(8.dp))
                .background(RhCard),
        ) {
            when {
                posterUrl != null -> {
                    SmartAsyncImage(
                        imageUrl = posterUrl,
                        contentDescription = card.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        shape = RoundedCornerShape(8.dp),
                    )
                }
                videoUrl != null -> {
                    VideoThumbnail(
                        url = videoUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                    )
                }
                else -> {
                    PlazaFallbackVisual(
                        PlazaCreationCard(
                            id = card.id,
                            intro = card.name,
                        ),
                        mediaType = stringResource(Res.string.plaza_short_media_type_fallback),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0x99000000)))),
            )
            if (onPreviewClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Text(
                text = card.authorName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: stringResource(Res.string.plaza_default_short_owner),
                color = RhText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, end = 52.dp, bottom = 7.dp),
            )
            plazaShortDurationLabel(card.durationSeconds)?.let { duration ->
                Text(
                    text = duration,
                    color = RhText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 7.dp),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = card.name.ifBlank { stringResource(Res.string.plaza_untitled_short) },
                color = RhText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(card.authorName, card.categoryName)
                    .joinToString(" / ")
                    .ifBlank { stringResource(Res.string.plaza_default_short_owner) },
                color = RhMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyTile(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = RhMuted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlazaFallbackVisual(card: PlazaCreationCard, mediaType: String? = card.mediaType) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF111827), Color(0xFF1F2937)),
                ),
            ),
    ) {
        Text(
            text = mediaType ?: stringResource(Res.string.plaza_image_media_type_fallback),
            color = BrandLime,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * 广场排序下拉选项。
 *
 * @property value 传给广场查询接口的排序 code；必须与 `PlazaStateHolder` 识别的排序值一致。
 * @property label 展示在排序下拉按钮和菜单里的本地化文案。
 */
internal data class PlazaSortOption(
    val value: String,
    val label: String,
)

/**
 * 广场排序下拉控件的紧凑视觉尺寸。
 *
 * @property widthDp 下拉按钮宽度，单位 dp；需要比内容分类 chip 更轻，避免压过模式 tab。
 * @property heightDp 下拉按钮高度，单位 dp；与模式 tab 文字区接近，避免右侧显得过大。
 * @property cornerRadiusDp 圆角半径，单位 dp；紧凑按钮使用小圆角，匹配内容分类 chip 的克制风格。
 * @property horizontalPaddingDp 左右内边距，单位 dp；保证短文案和箭头不挤压。
 * @property iconSizeDp 箭头图标尺寸，单位 dp；随按钮高度收窄，降低视觉重量。
 */
internal data class PlazaSortDropdownMetrics(
    val widthDp: Int,
    val heightDp: Int,
    val cornerRadiusDp: Int,
    val horizontalPaddingDp: Int,
    val iconSizeDp: Int,
)

/**
 * 广场排序下拉控件的布局锚点。
 *
 * @property ModeTabs 表示排序下拉与“灵感/短片”模式 tab 同行展示，避免在标题下方产生额外空行。
 */
internal enum class PlazaSortDropdownAnchor {
    ModeTabs,
}

/**
 * 返回广场排序下拉当前使用的布局锚点。
 *
 * 该值固定为模式 tab 行，防止排序控件再次被放回独占一行的空白区域。
 */
internal fun plazaSortDropdownAnchor(): PlazaSortDropdownAnchor = PlazaSortDropdownAnchor.ModeTabs

/**
 * 返回排序下拉在模式 tab 行使用的紧凑尺寸。
 *
 * 尺寸固定为轻量按钮，避免右侧排序控件在视觉上大于左侧“灵感/短片”模式 tab。
 */
internal fun plazaSortDropdownMetrics(): PlazaSortDropdownMetrics =
    PlazaSortDropdownMetrics(
        widthDp = 96,
        heightDp = 34,
        cornerRadiusDp = 8,
        horizontalPaddingDp = 8,
        iconSizeDp = 14,
    )

/**
 * 返回右侧排序下拉菜单展示的固定排序选项。
 *
 * 选项顺序保持与原顶部 segmented control 一致，避免用户认知变化。
 */
internal fun plazaSortOptions(
    recommendLabel: String,
    hotLabel: String,
    latestLabel: String,
): List<PlazaSortOption> =
    listOf(
        PlazaSortOption(value = "RECOMMEND", label = recommendLabel),
        PlazaSortOption(value = "HOT", label = hotLabel),
        PlazaSortOption(value = "LATEST", label = latestLabel),
    )

/**
 * 返回右侧排序下拉按钮当前应展示的排序名称。
 *
 * 未识别的排序 code 回退到第一个选项，避免接口或状态异常时按钮空白。
 */
internal fun plazaSelectedSortLabel(
    selectedSort: String,
    options: List<PlazaSortOption>,
): String =
    options.firstOrNull { it.value == selectedSort }?.label
        ?: options.firstOrNull()?.label
        ?: selectedSort

internal fun plazaShouldAutoLoadMore(
    loadedItemCount: Int,
    lastVisibleItemIndex: Int?,
    hasMore: Boolean,
    isLoading: Boolean,
    prefetchDistance: Int = PlazaAutoLoadMorePrefetchItemDistance,
): Boolean {
    if (loadedItemCount <= 0 || lastVisibleItemIndex == null) return false
    if (!hasMore || isLoading) return false

    val remainingItems = loadedItemCount - 1 - lastVisibleItemIndex
    return remainingItems <= prefetchDistance
}

internal fun plazaUsesWaterfallLayout(mode: PlazaMode): Boolean =
    when (mode) {
        PlazaMode.CREATIONS -> true
        PlazaMode.SHORTS -> false
    }

internal fun plazaCreationTileAspectRatio(card: PlazaCreationCard): Float {
    val width = card.imageWidth?.takeIf { it > 0 } ?: return plazaCreationTileFallbackAspectRatio()
    val height = card.imageHeight?.takeIf { it > 0 } ?: return plazaCreationTileFallbackAspectRatio()
    return (width.toFloat() / height.toFloat()).coerceIn(
        minimumValue = PlazaCreationTileMinAspectRatio,
        maximumValue = PlazaCreationTileMaxAspectRatio,
    )
}

internal fun plazaCreationTileFallbackAspectRatio(): Float = PlazaCreationTileFallbackAspectRatio

internal fun plazaShortThumbnailAspectRatio(): Float = PlazaShortThumbnailAspectRatio

/**
 * 返回灵感分类行应展示的服务端标签。
 *
 * 分类行支持横向滚动，因此这里不得截断服务端返回的完整标签树，否则首屏外分类无法被用户选择。
 * 当服务端返回父子标签时，只展示父标签；父标签仍携带子孙 ID，点击后会展开请求子分类。
 */
internal fun plazaVisibleCreationTags(tags: List<PlazaTag>): List<PlazaTag> {
    val childIds = tags.flatMapTo(mutableSetOf()) { it.childIds }
    return tags.filterNot { it.id in childIds }
}

/**
 * 返回短片分类行应展示的服务端分类。
 *
 * 分类行支持横向滚动，因此这里保留全部短片分类，不按首屏宽度裁剪。
 */
internal fun plazaVisibleShortCategories(categories: List<PlazaShortCategory>): List<PlazaShortCategory> = categories

internal fun plazaCreationPreviewItems(creations: List<PlazaCreationCard>): List<ImagePreviewItem> =
    creations.mapIndexedNotNull { index, card ->
        val imageUrl = card.mediaUrl?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
        if (plazaCreationIsVideo(card)) return@mapIndexedNotNull null
        ImagePreviewItem(
            id = plazaCreationPreviewItemId(index = index, card = card, imageUrl = imageUrl),
            imageUrl = imageUrl,
            contentDescription = card.intro,
        )
    }

internal fun plazaCreationPreviewIndex(creations: List<PlazaCreationCard>, creationIndex: Int): Int? {
    if (creationIndex !in creations.indices) return null
    if (creations[creationIndex].mediaUrl.isNullOrBlank()) return null
    if (plazaCreationIsVideo(creations[creationIndex])) return null

    return creations
        .take(creationIndex + 1)
        .count { !it.mediaUrl.isNullOrBlank() && !plazaCreationIsVideo(it) } - 1
}

internal fun plazaCreationVideoPreviewItem(card: PlazaCreationCard): VideoPreviewItem? {
    if (!plazaCreationIsVideo(card)) return null
    val videoUrl = card.mediaUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val subtitle = listOfNotNull(
        card.ownerName?.trim()?.takeIf { it.isNotEmpty() },
        card.mediaType?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(" / ").takeIf { it.isNotEmpty() }

    return VideoPreviewItem(
        id = plazaCreationVideoPreviewItemId(card = card, videoUrl = videoUrl),
        videoUrl = videoUrl,
        posterUrl = null,
        title = card.intro?.trim()?.takeIf { it.isNotEmpty() },
        subtitle = subtitle,
    )
}

internal fun plazaCreationIsVideo(card: PlazaCreationCard): Boolean =
    plazaMediaValueLooksVideo(card.mediaType) || plazaMediaValueLooksVideo(card.mediaUrl)

internal fun plazaMediaValueLooksVideo(value: String?): Boolean {
    val normalized = value
        ?.trim()
        ?.lowercase()
        ?.substringBefore('?')
        ?.substringBefore('#')
        ?: return false
    if (normalized.isEmpty()) return false

    return normalized == "video" ||
        normalized.startsWith("video/") ||
        PlazaVideoMediaExtensions.any { extension ->
            normalized == extension || normalized.endsWith(".$extension")
        }
}

internal fun plazaShortPreviewItem(card: PlazaShortCard): VideoPreviewItem? {
    val videoUrl = card.videoUrl?.trim()?.takeIf { it.isNotEmpty() }
    val posterUrl = card.thumbnailUrl?.trim()?.takeIf { it.isNotEmpty() }
    if (videoUrl == null && posterUrl == null) return null

    val subtitle = listOfNotNull(
        card.authorName?.trim()?.takeIf { it.isNotEmpty() },
        card.categoryName?.trim()?.takeIf { it.isNotEmpty() },
        plazaShortDurationLabel(card.durationSeconds),
    ).joinToString(" / ").takeIf { it.isNotEmpty() }

    return VideoPreviewItem(
        id = plazaShortPreviewItemId(card = card, videoUrl = videoUrl, posterUrl = posterUrl),
        videoUrl = videoUrl,
        posterUrl = posterUrl,
        title = card.name.trim().takeIf { it.isNotEmpty() },
        subtitle = subtitle,
    )
}

internal fun plazaShortDurationLabel(durationSeconds: Int?): String? {
    val totalSeconds = durationSeconds?.takeIf { it > 0 } ?: return null
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun plazaCreationPreviewItemId(index: Int, card: PlazaCreationCard, imageUrl: String): String =
    "creation-preview-$index-${card.id}-$imageUrl"

private fun plazaCreationVideoPreviewItemId(card: PlazaCreationCard, videoUrl: String): String =
    "creation-video-preview-${card.id}-$videoUrl"

private fun plazaShortPreviewItemId(card: PlazaShortCard, videoUrl: String?, posterUrl: String?): String =
    "short-preview-${card.id}-${videoUrl ?: posterUrl}"

private const val PlazaAutoLoadMorePrefetchItemDistance = 4
private const val PlazaCreationTileFallbackAspectRatio = 0.76f
private const val PlazaCreationTileMinAspectRatio = 0.58f
private const val PlazaCreationTileMaxAspectRatio = 1.35f
private const val PlazaShortThumbnailAspectRatio = 16f / 9f
private val PlazaVideoMediaExtensions = setOf("mp4", "mov", "m4v", "webm", "mkv", "avi")

@Composable
private fun PlazaFallbackTagLabel.asText(): String =
    when (this) {
        PlazaFallbackTagLabel.Images -> stringResource(Res.string.plaza_fallback_catalog_tag_images)
        PlazaFallbackTagLabel.Videos -> stringResource(Res.string.plaza_fallback_catalog_tag_videos)
        PlazaFallbackTagLabel.Workflows -> stringResource(Res.string.plaza_fallback_catalog_tag_workflows)
    }

@Composable
private fun PlazaFallbackCardIntro.asText(): String =
    when (this) {
        PlazaFallbackCardIntro.ImageV2PromptGallery -> stringResource(Res.string.plaza_fallback_card_image_v2_intro)
        PlazaFallbackCardIntro.SeedreamLiteTextToImage -> stringResource(Res.string.plaza_fallback_card_seedream_intro)
        PlazaFallbackCardIntro.VideoWorkflowFromDocs -> stringResource(Res.string.plaza_fallback_card_video_workflow_intro)
        PlazaFallbackCardIntro.ReusableApiWorkflow -> stringResource(Res.string.plaza_fallback_card_workflow_intro)
    }

@Composable
private fun PlazaFallbackCardOwner.asText(): String =
    when (this) {
        PlazaFallbackCardOwner.RunningHubApi -> stringResource(Res.string.plaza_fallback_card_owner_runninghub_api)
        PlazaFallbackCardOwner.RunningHubCreator -> stringResource(Res.string.plaza_fallback_card_owner_creator)
    }

@Composable
private fun PlazaFallbackCardMediaType.asText(): String =
    when (this) {
        PlazaFallbackCardMediaType.Image -> stringResource(Res.string.plaza_image_media_type_fallback)
        PlazaFallbackCardMediaType.Video -> stringResource(Res.string.plaza_video_media_type_fallback)
        PlazaFallbackCardMediaType.Workflow -> stringResource(Res.string.plaza_workflow_media_type_fallback)
    }

@Composable
private fun plazaPresentationErrorMessage(error: PlazaPresentationError): String =
    when (error) {
        PlazaPresentationError.CreationsLoadFailed -> stringResource(Res.string.plaza_error_creations_load_failed)
        PlazaPresentationError.ShortsLoadFailed -> stringResource(Res.string.plaza_error_shorts_load_failed)
    }

@Composable
private fun LoadingPanel() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandLime)
    }
}

@Composable
private fun ErrorPanel(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = StatusError,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(24.dp),
        )
    }
}
