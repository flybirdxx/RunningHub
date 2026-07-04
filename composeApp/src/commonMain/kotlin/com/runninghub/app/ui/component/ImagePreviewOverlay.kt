package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.runninghub.app.ui.designsystem.theme.RhTheme
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.image_preview_close_content_description
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 图片预览器的最小展示模型。
 *
 * @property id 图片稳定标识，用作候选列表 key。
 * @property imageUrl 远程图片地址。
 * @property contentDescription 图片无障碍描述；可为空表示装饰性预览。
 */
data class ImagePreviewItem(
    val id: String,
    val imageUrl: String,
    val contentDescription: String? = null,
)

@Composable
fun RhImagePreviewOverlay(
    items: List<ImagePreviewItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hasMoreItems: Boolean = false,
    isLoadingMoreItems: Boolean = false,
    onLoadMoreItems: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val prefetchUrls = remember(items, safeSelectedIndex) {
        imagePreviewPrefetchUrls(items = items, selectedIndex = safeSelectedIndex)
    }
    val selectedItem = items[safeSelectedIndex]

    ImagePreviewCandidatePreloader(imageUrls = prefetchUrls)

    Box(
        modifier = modifier
            .fillMaxSize()
            // 全屏图片预览遮罩固定用黑色底，保证任意图片在其上都有稳定对比。
            .background(Color.Black.copy(alpha = 0.88f)),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 54.dp)
                .size(44.dp)
                .clip(CircleShape)
                // 关闭按钮叠在媒体遮罩上，沿用黑色半透明底保证可点可见。
                .background(Color.Black.copy(alpha = 0.46f)),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.image_preview_close_content_description),
                // 关闭图标叠在媒体遮罩上，固定白色保证在任意画面上的可见度。
                tint = Color.White,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 106.dp, end = 12.dp, bottom = 42.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .blockImagePreviewClickThrough(),
                contentAlignment = Alignment.Center,
            ) {
                SmartAsyncImage(
                    imageUrl = selectedItem.imageUrl,
                    contentDescription = selectedItem.contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    shape = RoundedCornerShape(8.dp),
                )
            }

            ImagePreviewCandidatePicker(
                items = items,
                selectedIndex = safeSelectedIndex,
                onSelectedIndexChange = onSelectedIndexChange,
                hasMoreItems = hasMoreItems,
                isLoadingMoreItems = isLoadingMoreItems,
                onLoadMoreItems = onLoadMoreItems,
                modifier = Modifier
                    .width(ImagePreviewPickerWidthDp.dp)
                    .fillMaxHeight(),
            )
        }
    }
}

private fun Modifier.blockImagePreviewClickThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Final)
            event.changes.forEach { pointerInputChange ->
                pointerInputChange.consume()
            }
        }
    }
}

@Composable
private fun ImagePreviewCandidatePreloader(imageUrls: List<String>) {
    val context = LocalPlatformContext.current

    DisposableEffect(context, imageUrls) {
        val imageLoader = SingletonImageLoader.get(context)
        val disposables = imageUrls.map { imageUrl ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(ImagePreviewPrefetchSizePx)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
            )
        }

        onDispose {
            disposables.forEach { disposable -> disposable.dispose() }
        }
    }
}

@Composable
private fun ImagePreviewCandidatePicker(
    items: List<ImagePreviewItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    hasMoreItems: Boolean,
    isLoadingMoreItems: Boolean,
    onLoadMoreItems: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImagePreviewSegmentIndicator(
            itemCount = items.size,
            selectedIndex = selectedIndex,
            modifier = Modifier.width(16.dp),
        )
        ImagePreviewCandidateList(
            items = items,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = onSelectedIndexChange,
            hasMoreItems = hasMoreItems,
            isLoadingMoreItems = isLoadingMoreItems,
            onLoadMoreItems = onLoadMoreItems,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ImagePreviewCandidateList(
    items: List<ImagePreviewItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    hasMoreItems: Boolean,
    isLoadingMoreItems: Boolean,
    onLoadMoreItems: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = imagePreviewInitialFirstVisibleItemIndex(
            itemCount = items.size,
            selectedIndex = selectedIndex,
        ),
    )
    val programmaticScrollTarget = remember { mutableStateOf<Int?>(null) }
    val latestSelectedIndex = rememberUpdatedState(selectedIndex)
    val latestOnLoadMoreItems = rememberUpdatedState(onLoadMoreItems)

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val centerPadding = ((maxHeight - ImagePreviewSelectedThumbnailHeightDp.dp) / 2).coerceAtLeast(0.dp)

        // 候选列表也可能只拿到当前已加载页；滑到尾部附近时把分页请求交回页面层。
        LaunchedEffect(items.size, hasMoreItems, isLoadingMoreItems) {
            snapshotFlow {
                val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                imagePreviewShouldAutoLoadMore(
                    loadedItemCount = items.size,
                    lastVisibleItemIndex = lastVisibleItemIndex,
                    hasMore = hasMoreItems,
                    isLoading = isLoadingMoreItems,
                )
            }
                .distinctUntilChanged()
                .collect { shouldLoadMore ->
                    if (shouldLoadMore) {
                        latestOnLoadMoreItems.value?.invoke()
                    }
                }
        }

        // 点击候选图时需要把被点选项带回中间；用户手势滚动过程中则由下面的 snapshotFlow
        // 反向驱动 selectedIndex，避免自动滚动与手势互相抢控制权。
        LaunchedEffect(selectedIndex, items.size) {
            val targetIndex = selectedIndex.coerceIn(0, items.lastIndex)
            if (!listState.isScrollInProgress) {
                programmaticScrollTarget.value = targetIndex
                try {
                    listState.animateScrollToItem(targetIndex)
                } finally {
                    programmaticScrollTarget.value = null
                }
            }
        }

        LaunchedEffect(items.size) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val centeredIndex = imagePreviewClosestCenterIndex(
                    visibleItems = layoutInfo.visibleItemsInfo.map { itemInfo ->
                        ImagePreviewVisibleItem(
                            index = itemInfo.index,
                            offset = itemInfo.offset,
                            size = itemInfo.size,
                        )
                    },
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
                if (imagePreviewShouldSyncCenteredIndex(
                        centeredIndex = centeredIndex,
                        selectedIndex = latestSelectedIndex.value,
                        isScrollInProgress = listState.isScrollInProgress,
                        programmaticScrollTargetIndex = programmaticScrollTarget.value,
                    )
                ) {
                    centeredIndex
                } else {
                    null
                }
            }
                .distinctUntilChanged()
                .collect { centeredIndex ->
                    if (centeredIndex != null) {
                        onSelectedIndexChange(centeredIndex)
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = centerPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                ImagePreviewCandidateThumbnail(
                    item = item,
                    visual = imagePreviewCandidateVisual(index = index, selectedIndex = selectedIndex),
                    onClick = { onSelectedIndexChange(index) },
                )
            }
        }
    }
}

@Composable
private fun ImagePreviewCandidateThumbnail(
    item: ImagePreviewItem,
    visual: ImagePreviewCandidateVisual,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val blurModifier = if (visual.blurDp > 0f) {
        Modifier.blur(visual.blurDp.dp)
    } else {
        Modifier
    }
    val selectedModifier = if (visual.selected) {
        Modifier.border(2.dp, RhTheme.colors.brandPrimary, shape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .size(width = visual.widthDp.dp, height = visual.heightDp.dp)
            .graphicsLayer { alpha = visual.alpha }
            .clip(shape)
            .then(blurModifier)
            .then(selectedModifier)
            .clickable(onClick = onClick),
    ) {
        SmartAsyncImage(
            imageUrl = item.imageUrl,
            contentDescription = item.contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            shape = shape,
        )
    }
}

@Composable
private fun ImagePreviewSegmentIndicator(
    itemCount: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val segmentCount = imagePreviewIndicatorSegmentCount(itemCount)
    val activeSegment = imagePreviewIndicatorActiveSegment(itemCount = itemCount, selectedIndex = selectedIndex)

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(segmentCount) { index ->
                Box(
                    modifier = Modifier
                        .size(width = 13.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (index == activeSegment) {
                                RhTheme.colors.brandSecondary
                            } else {
                                RhTheme.colors.textTertiary.copy(alpha = 0.62f)
                            },
                        ),
                )
            }
        }
    }
}

internal data class ImagePreviewCandidateVisual(
    val widthDp: Int,
    val heightDp: Int,
    val alpha: Float,
    val blurDp: Float,
    val selected: Boolean,
)

internal fun imagePreviewCandidateVisual(index: Int, selectedIndex: Int): ImagePreviewCandidateVisual {
    val distance = abs(index - selectedIndex)
    if (distance == 0) {
        return ImagePreviewCandidateVisual(
            widthDp = ImagePreviewSelectedThumbnailWidthDp,
            heightDp = ImagePreviewSelectedThumbnailHeightDp,
            alpha = 1f,
            blurDp = 0f,
            selected = true,
        )
    }

    val cappedDistance = distance.coerceAtMost(ImagePreviewMaxDistance)
    val widthShrink = cappedDistance * 8
    val heightShrink = cappedDistance * 12

    return ImagePreviewCandidateVisual(
        widthDp = (ImagePreviewSelectedThumbnailWidthDp - widthShrink).coerceAtLeast(44),
        heightDp = (ImagePreviewSelectedThumbnailHeightDp - heightShrink).coerceAtLeast(56),
        alpha = (0.76f - cappedDistance * 0.08f).coerceAtLeast(0.42f),
        blurDp = 2.0f + cappedDistance * 0.75f,
        selected = false,
    )
}

internal data class ImagePreviewVisibleItem(
    val index: Int,
    val offset: Int,
    val size: Int,
)

internal fun imagePreviewClosestCenterIndex(
    visibleItems: List<ImagePreviewVisibleItem>,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Int? {
    if (visibleItems.isEmpty()) return null

    val viewportCenter = (viewportStartOffset + viewportEndOffset) / 2
    return visibleItems.minBy { item ->
        abs(item.offset + item.size / 2 - viewportCenter)
    }.index
}

internal fun imagePreviewInitialFirstVisibleItemIndex(itemCount: Int, selectedIndex: Int): Int =
    if (itemCount <= 0) {
        0
    } else {
        selectedIndex.coerceIn(0, itemCount - 1)
    }

internal fun imagePreviewShouldSyncCenteredIndex(
    centeredIndex: Int?,
    selectedIndex: Int,
    isScrollInProgress: Boolean,
    programmaticScrollTargetIndex: Int?,
): Boolean =
    centeredIndex != null &&
        centeredIndex != selectedIndex &&
        isScrollInProgress &&
        programmaticScrollTargetIndex == null

internal fun imagePreviewShouldAutoLoadMore(
    loadedItemCount: Int,
    lastVisibleItemIndex: Int?,
    hasMore: Boolean,
    isLoading: Boolean,
    prefetchDistance: Int = ImagePreviewAutoLoadMorePrefetchItemDistance,
): Boolean {
    if (loadedItemCount <= 0 || lastVisibleItemIndex == null) return false
    if (!hasMore || isLoading) return false

    val remainingItems = loadedItemCount - 1 - lastVisibleItemIndex
    return remainingItems <= prefetchDistance
}

internal fun imagePreviewPrefetchUrls(
    items: List<ImagePreviewItem>,
    selectedIndex: Int,
    prefetchCount: Int = ImagePreviewDefaultPrefetchCount,
): List<String> {
    val range = imagePreviewPrefetchIndexRange(
        itemCount = items.size,
        selectedIndex = selectedIndex,
        prefetchCount = prefetchCount,
    )
    return range.map { index -> items[index].imageUrl }
}

internal fun imagePreviewPrefetchIndexRange(
    itemCount: Int,
    selectedIndex: Int,
    prefetchCount: Int = ImagePreviewDefaultPrefetchCount,
): IntRange {
    if (itemCount <= 0 || prefetchCount <= 0) return IntRange.EMPTY

    val safeSelectedIndex = selectedIndex.coerceIn(0, itemCount - 1)
    val safeCount = prefetchCount.coerceAtMost(itemCount)
    val beforeCount = safeCount / 2
    var start = safeSelectedIndex - beforeCount
    var end = start + safeCount - 1

    if (start < 0) {
        end += -start
        start = 0
    }
    if (end >= itemCount) {
        start -= end - itemCount + 1
        end = itemCount - 1
    }

    return start.coerceAtLeast(0)..end.coerceAtMost(itemCount - 1)
}

internal fun imagePreviewIndicatorSegmentCount(itemCount: Int): Int =
    itemCount.coerceIn(0, ImagePreviewMaxIndicatorSegments)

internal fun imagePreviewIndicatorActiveSegment(itemCount: Int, selectedIndex: Int): Int {
    val segmentCount = imagePreviewIndicatorSegmentCount(itemCount)
    if (segmentCount == 0) return -1
    if (itemCount <= 1) return 0

    val safeIndex = selectedIndex.coerceIn(0, itemCount - 1)
    return ((safeIndex.toFloat() / (itemCount - 1)) * (segmentCount - 1)).roundToInt()
}

private const val ImagePreviewSelectedThumbnailWidthDp = 76
private const val ImagePreviewSelectedThumbnailHeightDp = 104
private const val ImagePreviewPickerWidthDp = 108
private const val ImagePreviewMaxDistance = 4
private const val ImagePreviewMaxIndicatorSegments = 12
private const val ImagePreviewDefaultPrefetchCount = 10
private const val ImagePreviewPrefetchSizePx = 320
private const val ImagePreviewAutoLoadMorePrefetchItemDistance = 4
