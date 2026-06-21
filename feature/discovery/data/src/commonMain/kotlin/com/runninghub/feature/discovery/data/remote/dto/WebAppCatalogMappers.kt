package com.runninghub.feature.discovery.data.remote.dto

import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.Cover
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.PageData
import com.runninghub.core.model.StatisticsInfo
import com.runninghub.core.model.Tag
import com.runninghub.core.model.TagSimple
import com.runninghub.core.model.WebApp

/**
 * 将目录卡片 DTO 转换为 Domain WebApp。
 *
 * 映射时优先使用新接口的 covers 字段，旧接口只返回 preview 时仍能展示封面。
 * 视频资源使用缩略图作为列表封面，并把原始视频地址写入 videoUrl，避免 UI 把视频地址当图片加载。
 */
internal fun WebAppCatalogDto.toDomain(): WebApp {
    val firstCover = covers?.firstOrNull()
    val rawCoverUrl = firstCover?.url ?: preview?.url
    val mediaType = inferMediaType(rawCoverUrl)
    val resolvedThumbnailUrl = thumbnailUrl ?: firstCover?.thumbnailUri
    val resolvedCoverUrl = when (mediaType) {
        CoverMediaType.VIDEO -> resolvedThumbnailUrl ?: rawCoverUrl
        else -> firstCover?.thumbnailUri ?: rawCoverUrl
    }
    return WebApp(
        id = id ?: "",
        title = title ?: "",
        description = desc,
        thumbnailUrl = resolvedThumbnailUrl,
        coverUrl = resolvedCoverUrl,
        coverMediaType = mediaType,
        videoUrl = rawCoverUrl.takeIf { mediaType == CoverMediaType.VIDEO },
        coverWidth = firstCover?.imageWidth,
        coverHeight = firstCover?.imageHeight,
        author = author?.toDomain(),
        tags = tags?.map { it.toDomain() } ?: emptyList(),
        likeCount = statisticsInfo?.likeCount ?: likeCount ?: "0",
        collectCount = statisticsInfo?.collectCount ?: collectCount ?: "0",
        useCount = statisticsInfo?.useCount ?: useCount ?: "0",
        pv = statisticsInfo?.pv ?: pv ?: "0",
        carefullyChosen = carefullyChosen,
    )
}

/**
 * 将详情 DTO 转换为 Domain AppDetail。
 *
 * 公开详情接口可能缺少输入节点或统计字段；缺失列表统一降级为空列表，避免详情页处理 null 集合。
 */
internal fun WebAppDetailCatalogDto.toDomain(): AppDetail = AppDetail(
    id = id ?: "",
    name = name,
    workflowId = workflowId,
    description = description,
    tags = tags?.map { it.toDomain() } ?: emptyList(),
    owner = owner?.toDomain(),
    publishTime = publishTime,
    inputNodes = inputNodes?.map { it.toDomain() } ?: emptyList(),
    covers = covers?.map { it.toDomain() } ?: emptyList(),
    statisticsInfo = statisticsInfo?.toDomain(),
    authorName = authorName,
    authorAvatar = authorAvatar,
    runningSuccessRate = runningSuccessRate,
    avgRunningSeconds = avgRunningSeconds,
    instanceType = instanceType,
)

/**
 * 将作者 DTO 转换为 Domain Author。
 *
 * 计数字段使用字符串保留服务端格式，缺失时降级为 "0"，避免 Presentation 重复兜底。
 */
internal fun WebAppAuthorDto.toDomain(): Author = Author(
    id = id,
    name = name,
    avatar = avatar,
    intro = intro,
    followCount = followCount ?: "0",
    fansCount = fansCount ?: "0",
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    bgImage = bgImage,
)

/**
 * 将轻量标签 DTO 转换为 Domain TagSimple。
 */
internal fun WebAppTagSimpleDto.toDomain(): TagSimple =
    TagSimple(id = id, name = name, nameEn = nameEn, labels = labels)

/**
 * 将封面 DTO 转换为 Domain Cover。
 *
 * Domain Cover 的 url 优先使用缩略图地址，保证详情页在列表或弱网场景下优先展示轻量资源。
 */
internal fun WebAppCoverDto.toDomain(): Cover =
    Cover(url = thumbnailUri ?: url, imageWidth = imageWidth, imageHeight = imageHeight)

/**
 * 将统计 DTO 转换为 Domain 统计信息。
 */
internal fun WebAppStatisticsInfoDto.toDomain(): StatisticsInfo = StatisticsInfo(
    likeCount = likeCount ?: "0",
    collectCount = collectCount ?: "0",
    useCount = useCount ?: "0",
    pv = pv ?: "0",
)

/**
 * 将输入节点 DTO 转换为 Domain InputNode。
 */
internal fun WebAppInputNodeDto.toDomain(): InputNode = InputNode(
    nodeId = nodeId,
    nodeName = nodeName,
    fieldName = fieldName,
    fieldValue = fieldValue,
    fieldData = fieldData,
    fieldType = fieldType,
    description = description,
    descriptionEn = descriptionEn,
)

/**
 * 将标签树 DTO 转换为 Domain Tag。
 *
 * @param maxDepth 最大递归深度，用于防御服务端异常循环或过深树形数据。
 */
internal fun CatalogTagDto.toDomain(maxDepth: Int = 10): Tag = Tag(
    id = id,
    name = name,
    level = level,
    parentId = parentId,
    rang = rang,
    enable = enable,
    childTags = if (maxDepth > 0) childTags?.map { it.toDomain(maxDepth - 1) } else null,
)

/**
 * 将目录分页 DTO 转换为 Domain PageData。
 */
internal fun DiscoveryPageDataDto<WebAppCatalogDto>.toDomain(): PageData<WebApp> = PageData(
    records = records.map { it.toDomain() },
    total = total,
    size = size,
    current = current,
    hasNext = hasNext,
)

private fun inferMediaType(url: String?): CoverMediaType {
    if (url == null) return CoverMediaType.IMAGE
    val path = url.substringBefore("?").lowercase()
    return when {
        path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mov") -> CoverMediaType.VIDEO
        path.endsWith(".gif") -> CoverMediaType.GIF
        else -> CoverMediaType.IMAGE
    }
}
