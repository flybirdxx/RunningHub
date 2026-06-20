package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlazaCreationListRequestDto(
    val current: Int = 1,
    val size: Int = 30,
    val fromId: String? = null,
    val sort: String = "RECOMMEND",
    val tags: List<String> = emptyList(),
)

@Serializable
data class PlazaCreationPageDto(
    val records: List<PlazaCreationCardDto> = emptyList(),
    val list: List<PlazaCreationCardDto> = emptyList(),
    val total: Int = 0,
    val current: Int = 1,
    val nextCursor: String? = null,
) {
    val items: List<PlazaCreationCardDto>
        get() = if (records.isNotEmpty()) records else list
}

@Serializable
data class PlazaCreationCardDto(
    val id: String,
    val intro: String? = null,
    val publishTime: String? = null,
    val owner: PlazaOwnerDto? = null,
    val statisticsInfo: PlazaStatisticsDto? = null,
    val creationShowreelInfo: PlazaCreationShowreelDto? = null,
    val liked: Boolean = false,
    val collected: Boolean = false,
)

@Serializable
data class PlazaOwnerDto(
    val id: String? = null,
    val name: String? = null,
    val avatar: String? = null,
)

@Serializable
data class PlazaStatisticsDto(
    val likeCount: String? = null,
    val downloadCount: String? = null,
    val useCount: String? = null,
    val pv: String? = null,
    val collectCount: String? = null,
)

@Serializable
data class PlazaCreationShowreelDto(
    val outputId: String? = null,
    val fileUrl: String? = null,
    val outputName: String? = null,
    val fileSize: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val fileType: String? = null,
    val seq: Int? = null,
    val isWatermark: Int? = null,
)

@Serializable
data class PlazaTagTreeRequestDto(
    val rang: String = "CREATION",
)

@Serializable
data class PlazaTagDto(
    val id: String,
    val name: String,
    val level: Int = 0,
    val enable: Boolean = true,
    val childTags: List<PlazaTagDto> = emptyList(),
)

@Serializable
data class PlazaShortListRequestDto(
    val page: Int = 1,
    val size: Int = 30,
    val categoryCode: String? = null,
)

@Serializable
data class PlazaShortPageDto(
    val records: List<PlazaShortCardDto> = emptyList(),
    val list: List<PlazaShortCardDto> = emptyList(),
    val total: Int = 0,
) {
    val items: List<PlazaShortCardDto>
        get() = if (records.isNotEmpty()) records else list
}

@Serializable
data class PlazaShortCategoryDto(
    val id: String? = null,
    val code: String? = null,
    val name: String = "",
)

@Serializable
data class PlazaShortCardDto(
    val id: String,
    val name: String = "",
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null,
    val categoryName: String? = null,
    @SerialName("userName")
    val authorName: String? = null,
    @SerialName("userAvatar")
    val authorAvatar: String? = null,
)
