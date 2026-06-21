package com.runninghub.feature.community.data.remote.dto

import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaCreationPage
import com.runninghub.feature.community.domain.PlazaShortCard
import com.runninghub.feature.community.domain.PlazaShortCategory
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
    )

/**
 * 将服务端标签树压平成可展示标签列表。
 *
 * PlazaScreen 当前按平铺标签筛选，Data 层在这里消化远端树形结构，避免 Presentation 遍历 DTO。
 */
fun PlazaTagDto.flatten(): List<PlazaTag> =
    listOf(toDomain()) + childTags.flatMap { it.flatten() }

/**
 * 将单个标签节点映射为 Domain 标签。
 */
fun PlazaTagDto.toDomain(): PlazaTag =
    PlazaTag(id = id, name = name, level = level, enable = enable)

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
 * 将短片卡片 DTO 映射为 Domain 短片卡片。
 *
 * 缩略图优先使用新版 [PlazaShortCardDto.thumbnailUrl]，缺失时兼容旧版 [PlazaShortCardDto.coverUrl]。
 */
fun PlazaShortCardDto.toDomain(): PlazaShortCard =
    PlazaShortCard(
        id = id,
        name = name,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl ?: coverUrl,
        durationSeconds = duration,
        categoryName = categoryName,
        authorName = authorName,
        authorAvatar = authorAvatar,
    )
