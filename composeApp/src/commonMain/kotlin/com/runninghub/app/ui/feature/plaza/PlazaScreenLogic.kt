package com.runninghub.app.ui.feature.plaza

import com.runninghub.app.ui.component.ImagePreviewItem
import com.runninghub.app.ui.component.VideoPreviewItem
import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import com.runninghub.feature.community.presentation.PlazaMode

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
