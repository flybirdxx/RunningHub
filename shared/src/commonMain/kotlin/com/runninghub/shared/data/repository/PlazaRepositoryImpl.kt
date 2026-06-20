package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.PlazaApi
import com.runninghub.shared.data.remote.dto.PlazaCreationCardDto
import com.runninghub.shared.data.remote.dto.PlazaCreationListRequestDto
import com.runninghub.shared.data.remote.dto.PlazaCreationPageDto
import com.runninghub.shared.data.remote.dto.PlazaShortCardDto
import com.runninghub.shared.data.remote.dto.PlazaShortCategoryDto
import com.runninghub.shared.data.remote.dto.PlazaShortListRequestDto
import com.runninghub.shared.data.remote.dto.PlazaTagDto
import com.runninghub.shared.domain.model.PlazaCreationCard
import com.runninghub.shared.domain.model.PlazaCreationPage
import com.runninghub.shared.domain.model.PlazaShortCard
import com.runninghub.shared.domain.model.PlazaShortCategory
import com.runninghub.shared.domain.model.PlazaTag
import com.runninghub.shared.domain.repository.PlazaRepository

class PlazaRepositoryImpl(
    private val api: PlazaApi,
) : PlazaRepository {
    override suspend fun getTags(): Result<List<PlazaTag>> =
        runCatching {
            val response = api.getCreationTags()
            check(response.code == 0) { response.msg.ifEmpty { "Plaza tags load failed" } }
            response.data.orEmpty().flatMap { it.flatten() }
        }

    override suspend fun listCreations(
        page: Int,
        size: Int,
        sort: String,
        tags: List<String>,
    ): Result<PlazaCreationPage> =
        runCatching {
            val response = api.listCreations(
                PlazaCreationListRequestDto(current = page, size = size, sort = sort, tags = tags)
            )
            check(response.code == 0) { response.msg.ifEmpty { "Plaza creations load failed" } }
            response.data?.toDomain() ?: PlazaCreationPage(page = page, total = 0, items = emptyList())
        }

    override suspend fun listShortCategories(): Result<List<PlazaShortCategory>> =
        runCatching {
            val response = api.listShortCategories()
            check(response.code == 0) { response.msg.ifEmpty { "Short categories load failed" } }
            response.data.orEmpty().map { it.toDomain() }
        }

    override suspend fun listShorts(page: Int, size: Int, categoryCode: String?): Result<List<PlazaShortCard>> =
        runCatching {
            val response = api.listShorts(PlazaShortListRequestDto(page = page, size = size, categoryCode = categoryCode))
            check(response.code == 0) { response.msg.ifEmpty { "Short list load failed" } }
            response.data?.items.orEmpty().map { it.toDomain() }
        }
}

fun PlazaCreationPageDto.toDomain(): PlazaCreationPage =
    PlazaCreationPage(
        page = current,
        total = total,
        items = items.map { it.toDomain() },
    )

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

fun PlazaTagDto.flatten(): List<PlazaTag> =
    listOf(toDomain()) + childTags.flatMap { it.flatten() }

fun PlazaTagDto.toDomain(): PlazaTag =
    PlazaTag(id = id, name = name, level = level, enable = enable)

fun PlazaShortCategoryDto.toDomain(): PlazaShortCategory =
    PlazaShortCategory(
        id = id,
        code = code ?: id.orEmpty(),
        name = name,
    )

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
