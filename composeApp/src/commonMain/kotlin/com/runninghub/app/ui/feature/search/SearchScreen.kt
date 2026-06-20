package com.runninghub.app.ui.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.ui.component.AppCard
import com.runninghub.app.ui.component.AppSearchBar
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewSearchUiState
import com.runninghub.app.ui.feature.detail.AppDetailScreen
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.RunningHubThemeExt
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.adaptiveGridColumns
import com.runninghub.app.ui.theme.rememberWindowSizeClass
import com.runninghub.core.model.Tag
import org.jetbrains.compose.ui.tooling.preview.Preview

class SearchVoyagerScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<SearchScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(Unit) { screenModel.loadHotTags() }

        SearchContent(
            uiState = uiState,
            onQueryChange = screenModel::onQueryChange,
            onSearch = screenModel::search,
            onClearSearch = screenModel::clearSearch,
            onTagClick = screenModel::searchByTag,
            onAppClick = { appId -> navigator.push(AppDetailScreen(appId)) },
            onBack = { navigator.pop() },
            onLoadMore = screenModel::loadMore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    uiState: SearchUiState,
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    onTagClick: (Tag) -> Unit = {},
    onAppClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && uiState.results.isNotEmpty()) onLoadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            // Search bar
            AppSearchBar(
                query = uiState.query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                placeholder = "搜索应用、工作流...",
                modifier = Modifier.padding(
                    horizontal = Dimens.SpaceLG,
                    vertical = Dimens.SpaceSM,
                ),
            )

            when {
                // Searching state
                uiState.isSearching && uiState.results.isEmpty() -> {
                    LoadingIndicator()
                }

                // Error
                uiState.error != null && uiState.results.isEmpty() -> {
                    ErrorState(
                        message = uiState.error,
                        onRetry = { onSearch(uiState.query) },
                    )
                }

                // No query — show hot tags
                uiState.query.isBlank() -> {
                    HotTagsSection(
                        tags = uiState.hotTags,
                        onTagClick = onTagClick,
                    )
                }

                // Empty results
                uiState.results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "未找到 \"${uiState.query}\" 相关结果",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Results list
                else -> {
                    val sizeClass = rememberWindowSizeClass()
                    if (sizeClass >= WindowSizeClass.Medium) {
                        // Medium+: grid layout
                        val columns = adaptiveGridColumns(sizeClass)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columns),
                            contentPadding = PaddingValues(
                                horizontal = Dimens.SpaceLG,
                                vertical = Dimens.SpaceSM,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(uiState.results, key = { it.id }) { app ->
                                AppCard(
                                    title = app.title,
                                    imageUrl = app.coverUrl ?: app.thumbnailUrl,
                                    authorName = app.author?.name,
                                    authorAvatar = app.author?.avatar,
                                    likeCount = app.likeCount,
                                    useCount = app.useCount,
                                    onClick = { onAppClick(app.id) },
                                )
                            }

                            if (uiState.isSearching && uiState.results.isNotEmpty()) {
                                item(key = "search_loading_more", span = { GridItemSpan(maxLineSpan) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceLG),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Dimens.IconSizeMD),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }

                            if (!uiState.hasMore && uiState.results.isNotEmpty()) {
                                item(key = "search_end", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "没有更多了",
                                        modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceLG),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        // Compact: list layout (unchanged)
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = Dimens.SpaceLG,
                                vertical = Dimens.SpaceSM,
                            ),
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(
                                items = uiState.results,
                                key = { it.id },
                            ) { app ->
                                AppCard(
                                    title = app.title,
                                    imageUrl = app.coverUrl ?: app.thumbnailUrl,
                                    authorName = app.author?.name,
                                    authorAvatar = app.author?.avatar,
                                    likeCount = app.likeCount,
                                    useCount = app.useCount,
                                    onClick = { onAppClick(app.id) },
                                )
                            }

                            if (uiState.isSearching && uiState.results.isNotEmpty()) {
                                item(key = "search_loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Dimens.SpaceLG),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(Dimens.IconSizeMD),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }

                            if (!uiState.hasMore && uiState.results.isNotEmpty()) {
                                item(key = "search_end") {
                                    Text(
                                        text = "没有更多了",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(Dimens.SpaceLG),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotTagsSection(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onTagClick: (Tag) -> Unit = {},
) {
    if (tags.isEmpty()) return

    val extColors = RunningHubThemeExt.colors

    Column(
        modifier = modifier.padding(
            horizontal = Dimens.SpaceLG,
            vertical = Dimens.SpaceMD,
        ),
    ) {
        // Section header
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "热门",
                tint = extColors.hotBadge,
                modifier = Modifier.size(Dimens.IconSizeMD),
            )
            Spacer(Modifier.padding(start = Dimens.SpaceXS))
            Text(
                text = "热门标签",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceMD))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = { onTagClick(tag) },
                    label = {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SearchAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        SearchContent(
            uiState = previewSearchUiState(),
            onQueryChange = {},
            onSearch = {},
            onClearSearch = {},
            onTagClick = {},
            onAppClick = {},
            onBack = {},
            onLoadMore = {},
        )
    }
}

@Preview
@Composable
private fun SearchPhone320Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun SearchPhone360Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun SearchPhone430Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun SearchMedium600Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun SearchExpanded840Preview() {
    SearchAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun SearchLandscapePreview() {
    SearchAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun SearchFontScale13Preview() {
    SearchAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun SearchFontScale15Preview() {
    SearchAdaptivePreview(RhPreviewSpec.FontScale15)
}
