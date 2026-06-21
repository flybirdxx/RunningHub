package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.theme.BrandLime
import com.runninghub.app.ui.theme.RhAppBackground
import com.runninghub.app.ui.theme.RhAppCard
import com.runninghub.app.ui.theme.RhAppLine
import com.runninghub.app.ui.theme.RhAppMuted
import com.runninghub.app.ui.theme.RhAppSelected
import com.runninghub.app.ui.theme.RhAppSurface
import com.runninghub.app.ui.theme.RhAppText
import com.runninghub.app.ui.theme.StatusError
import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import com.runninghub.feature.community.presentation.PlazaMode
import com.runninghub.feature.community.presentation.PlazaUiState
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_category_all
import runninghub.composeapp.generated.resources.plaza_default_creation_owner
import runninghub.composeapp.generated.resources.plaza_default_short_owner
import runninghub.composeapp.generated.resources.plaza_empty_creations
import runninghub.composeapp.generated.resources.plaza_empty_shorts
import runninghub.composeapp.generated.resources.plaza_fallback_tag_api
import runninghub.composeapp.generated.resources.plaza_fallback_tag_avatar
import runninghub.composeapp.generated.resources.plaza_fallback_tag_photo
import runninghub.composeapp.generated.resources.plaza_fallback_tag_video_generation
import runninghub.composeapp.generated.resources.plaza_featured_badge
import runninghub.composeapp.generated.resources.plaza_filter_label
import runninghub.composeapp.generated.resources.plaza_image_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_like_count_format
import runninghub.composeapp.generated.resources.plaza_load_more
import runninghub.composeapp.generated.resources.plaza_loading_more
import runninghub.composeapp.generated.resources.plaza_mode_creations
import runninghub.composeapp.generated.resources.plaza_mode_shorts
import runninghub.composeapp.generated.resources.plaza_refresh_content_description
import runninghub.composeapp.generated.resources.plaza_search_content_description
import runninghub.composeapp.generated.resources.plaza_short_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_sort_hot
import runninghub.composeapp.generated.resources.plaza_sort_latest
import runninghub.composeapp.generated.resources.plaza_sort_recommend
import runninghub.composeapp.generated.resources.plaza_tag_image_generation
import runninghub.composeapp.generated.resources.plaza_title
import runninghub.composeapp.generated.resources.plaza_untitled_creation
import runninghub.composeapp.generated.resources.plaza_untitled_short
import runninghub.composeapp.generated.resources.plaza_use_count_format

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
    val columns = if (windowInfo.windowWidth < 390.dp) 1 else 2

    Scaffold(
        modifier = modifier,
        containerColor = RhBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(RhBackground)
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .widthIn(max = windowInfo.feedContentMaxWidth),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlazaHeader(onRefresh = onRefresh)
            PlazaSortTabs(selectedSort = uiState.sort, onSortSelected = onSortSelected)
            PlazaModeTabs(selectedMode = uiState.mode, onModeSelected = onModeSelected)
            if (uiState.mode == PlazaMode.SHORTS) {
                PlazaShortCategoryRow(
                    categories = uiState.shortCategories,
                    selectedCode = uiState.selectedShortCategoryCode,
                    onCategorySelected = onShortCategorySelected,
                )
            } else {
                PlazaTagRow(tags = uiState.tags, selectedTagId = uiState.selectedTagId, onTagSelected = onTagSelected)
            }

            val showingShorts = uiState.mode == PlazaMode.SHORTS
            val errorMessage = uiState.error
            when {
                showingShorts && uiState.isShortsLoading && uiState.shorts.isEmpty() -> LoadingPanel()
                !showingShorts && uiState.isLoading && uiState.creations.isEmpty() -> LoadingPanel()
                errorMessage != null && showingShorts && uiState.shorts.isEmpty() -> ErrorPanel(errorMessage)
                errorMessage != null && !showingShorts && uiState.creations.isEmpty() -> ErrorPanel(errorMessage)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 92.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showingShorts) {
                        if (uiState.shorts.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyTile(message = stringResource(Res.string.plaza_empty_shorts))
                            }
                        } else {
                            items(uiState.shorts, key = { it.id }) { card ->
                                PlazaShortTile(card = card)
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LoadMoreTile(
                                    label = if (uiState.isShortsLoading) {
                                        stringResource(Res.string.plaza_loading_more)
                                    } else {
                                        stringResource(Res.string.plaza_load_more)
                                    },
                                    onClick = onLoadMoreShorts,
                                )
                            }
                        }
                    } else {
                        if (uiState.creations.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyTile(message = stringResource(Res.string.plaza_empty_creations))
                            }
                        } else {
                            items(uiState.creations, key = { it.id }) { card ->
                                PlazaCreationTile(card = card)
                            }
                        }
                        if (uiState.hasMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                LoadMoreTile(
                                    label = if (uiState.isLoadingMore) {
                                        stringResource(Res.string.plaza_loading_more)
                                    } else {
                                        stringResource(Res.string.plaza_load_more)
                                    },
                                    onClick = onLoadMoreCreations,
                                )
                            }
                        }
                    }
                }
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
private fun PlazaSortTabs(
    selectedSort: String,
    onSortSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.54f)
            .height(46.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(24.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            "RECOMMEND" to stringResource(Res.string.plaza_sort_recommend),
            "HOT" to stringResource(Res.string.plaza_sort_hot),
            "LATEST" to stringResource(Res.string.plaza_sort_latest),
        ).forEach { (value, label) ->
            SegmentedTab(
                label = label,
                selected = selectedSort == value,
                modifier = Modifier.weight(1f),
                onClick = { onSortSelected(value) },
            )
        }
    }
}

@Composable
private fun SegmentedTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) BrandLime else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.Black else RhText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun PlazaModeTabs(
    selectedMode: PlazaMode,
    onModeSelected: (PlazaMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        PlazaModeTab(
            stringResource(Res.string.plaza_mode_creations),
            selectedMode == PlazaMode.CREATIONS,
        ) { onModeSelected(PlazaMode.CREATIONS) }
        PlazaModeTab(
            stringResource(Res.string.plaza_mode_shorts),
            selectedMode == PlazaMode.SHORTS,
        ) { onModeSelected(PlazaMode.SHORTS) }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(
            label = stringResource(Res.string.plaza_category_all),
            selected = selectedCode == null,
            onClick = { onCategorySelected(null) },
        )
        categories.take(8).forEach { category ->
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
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(
            label = stringResource(Res.string.plaza_tag_image_generation),
            selected = selectedTagId == null,
            onClick = { onTagSelected(null) },
        )
        val visibleTags = tags.take(5).ifEmpty {
            listOf(
                PlazaTag("video", stringResource(Res.string.plaza_fallback_tag_video_generation), 1, true),
                PlazaTag("avatar", stringResource(Res.string.plaza_fallback_tag_avatar), 1, true),
                PlazaTag("api", stringResource(Res.string.plaza_fallback_tag_api), 1, true),
                PlazaTag("photo", stringResource(Res.string.plaza_fallback_tag_photo), 1, true),
            )
        }
        visibleTags.forEach { tag ->
            TagChip(
                label = tag.name,
                selected = selectedTagId == tag.id,
                onClick = { onTagSelected(tag.id) },
            )
        }
        TagChip(label = stringResource(Res.string.plaza_filter_label), selected = false, onClick = {})
    }
}

@Composable
private fun TagChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
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
private fun PlazaCreationTile(card: PlazaCreationCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .background(RhCard),
    ) {
        if (card.mediaUrl.isNullOrBlank()) {
            PlazaFallbackVisual(card)
        } else {
            SmartAsyncImage(
                imageUrl = card.mediaUrl,
                contentDescription = card.intro,
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
                text = card.intro ?: stringResource(Res.string.plaza_untitled_creation),
                color = RhText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(card.ownerName, card.mediaType)
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
private fun PlazaShortTile(card: PlazaShortCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .background(RhCard),
    ) {
        val media = card.thumbnailUrl ?: card.videoUrl
        if (media.isNullOrBlank()) {
            PlazaFallbackVisual(
                PlazaCreationCard(
                    id = card.id,
                    intro = card.name,
                    mediaType = stringResource(Res.string.plaza_short_media_type_fallback),
                ),
            )
        } else {
            SmartAsyncImage(
                imageUrl = media,
                contentDescription = card.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(8.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color(0xCC000000)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = card.name.ifBlank { stringResource(Res.string.plaza_untitled_short) },
                color = RhText,
                style = MaterialTheme.typography.labelLarge,
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
private fun LoadMoreTile(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RhSurface)
            .border(1.dp, RhLine, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = BrandLime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlazaFallbackVisual(card: PlazaCreationCard) {
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
            text = card.mediaType ?: stringResource(Res.string.plaza_image_media_type_fallback),
            color = BrandLime,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
        )
    }
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

private val RhBackground = RhAppBackground
private val RhSurface = RhAppSurface
private val RhCard = RhAppCard
private val RhSelected = RhAppSelected
private val RhLine = RhAppLine
private val RhText = RhAppText
private val RhMuted = RhAppMuted
