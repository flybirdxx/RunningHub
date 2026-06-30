package com.runninghub.feature.community.data.remote.dto

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaCreationPage
import com.runninghub.feature.community.domain.PlazaCreationReuseSnapshot
import com.runninghub.feature.community.domain.PlazaReuseMediaKind
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaShortPage
import com.runninghub.feature.community.domain.PlazaTag

/**
 * 将 Plaza 创作分页 DTO 映射为 Community Domain 分页模型。
 *
 * 映射时保留服务端顺序，因为该顺序可能包含推荐权重或排序策略，客户端只负责后续分页去重。
 */
fun PlazaCreationPageDto.toDomain(): PlazaCreationPage =
    PlazaCreationPage(
        page = current,
        total = total,
        items = items.map { it.toDomain() },
    )

/**
 * 将 Plaza 创作卡片 DTO 映射为 Domain 卡片。
 *
 * 远端作者、统计和媒体对象都可能缺失，因此可空字段会原样传递给 Presentation 做占位展示。
 */
fun PlazaCreationCardDto.toDomain(): PlazaCreationCard =
    PlazaCreationCard(
        id = id,
        intro = intro,
        publishTime = publishTime,
        ownerName = owner?.name,
        ownerAvatar = owner?.avatar,
        mediaUrl = creationShowreelInfo?.fileUrl,
        mediaType = creationShowreelInfo?.fileType,
        imageWidth = creationShowreelInfo?.imageWidth,
        imageHeight = creationShowreelInfo?.imageHeight,
        likeCount = statisticsInfo?.likeCount,
        useCount = statisticsInfo?.useCount,
        collectCount = statisticsInfo?.collectCount,
        liked = liked,
        collected = collected,
        reuseSnapshot = PlazaCreationReuseSnapshot(
            prompt = intro?.takeIf { it.isNotBlank() },
            referenceMediaUrl = creationShowreelInfo?.fileUrl?.takeIf { it.isNotBlank() },
            referenceMediaType = creationShowreelInfo?.fileType.toPlazaReuseMediaKind(),
        ),
    )

/**
 * 将服务端标签树压平成可展示标签列表。
 *
 * PlazaScreen 当前按平铺标签筛选，Data 层在这里消化远端树形结构并保留每个节点的子孙 ID，
 * 避免 Presentation 遍历 DTO。
 */
fun PlazaTagDto.flatten(): List<PlazaTag> =
    listOf(toDomain()) + childTags.flatMap { it.flatten() }

/**
 * 将单个标签节点映射为 Domain 标签。
 */
fun PlazaTagDto.toDomain(): PlazaTag =
    PlazaTag(id = id, name = name, level = level, enable = enable, childIds = childTagIds())

private fun PlazaTagDto.childTagIds(): List<String> =
    childTags.flatMap { child -> listOf(child.id) + child.childTagIds() }

private fun String?.toPlazaReuseMediaKind(): PlazaReuseMediaKind {
    val marker = this?.uppercase().orEmpty()
    return when {
        marker.contains("VIDEO") || marker.contains("MP4") || marker.contains("MOV") ->
            PlazaReuseMediaKind.VIDEO
        marker.contains("AUDIO") || marker.contains("MP3") || marker.contains("WAV") ->
            PlazaReuseMediaKind.AUDIO
        marker.contains("IMAGE") ||
            marker.contains("PNG") ||
            marker.contains("JPG") ||
            marker.contains("JPEG") ||
            marker.contains("WEBP") -> PlazaReuseMediaKind.IMAGE
        else -> PlazaReuseMediaKind.UNKNOWN
    }
}

/**
 * 将短片分类 DTO 映射为 Domain 分类。
 *
 * 旧接口可能只返回 ID 不返回 code，因此 code 缺失时降级使用 ID，确保分类仍可作为筛选参数。
 */
fun PlazaShortCategoryDto.toDomain(): PlazaShortCategory =
    PlazaShortCategory(
        id = id,
        code = code ?: id.orEmpty(),
        name = name,
    )

/**
 * 将短片分页 DTO 映射为 Domain 分页。
 *
 * @param requestedPage 请求页码；当短片接口未返回 current 时用于保留客户端分页进度。
 */
fun PlazaShortPageDto.toDomain(requestedPage: Int): PlazaShortPage =
    PlazaShortPage(
        page = current.takeIf { it > 0 } ?: requestedPage,
        total = total,
        items = items.map { it.toDomain() },
    )

/**
 * 将短片卡片 DTO 映射为 Domain 短片卡片。
 *
 * 当前 explore 接口使用 `compositionUrl/compositionDuration/thumbnail/authorName`，旧接口可能使用
 * `videoUrl/duration/thumbnailUrl/userName`，因此在 Data 边界统一做兼容映射。
 */
fun PlazaShortCardDto.toDomain(): PlazaShortCard =
    PlazaShortCard(
        id = id,
        name = name,
        videoUrl = compositionUrl ?: videoUrl,
        thumbnailUrl = thumbnail ?: thumbnailUrl ?: coverUrl,
        durationSeconds = compositionDuration ?: duration,
        categoryName = categoryName,
        authorName = authorName ?: userName,
        authorAvatar = authorAvatar ?: userAvatar,
    )
