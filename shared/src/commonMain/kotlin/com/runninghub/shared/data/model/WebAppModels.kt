package com.runninghub.shared.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebAppDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val title: String? = null,
    @SerialName("intro") val desc: String? = null,
    val thumbnailUrl: String? = null,
    val preview: PreviewDto? = null,
    val covers: List<CoverDto>? = null,
    @SerialName("owner") val author: AuthorDto? = null,
    val tags: List<TagSimpleDto>? = null,
    val statisticsInfo: StatisticsInfo? = null,
    val likeCount: String? = null,
    val collectCount: String? = null,
    val useCount: String? = null,
    val pv: String? = null
)

@Serializable
data class WebAppDetailDto(
    @SerialName("id") val id: String? = null,
    @SerialName("webappName") val name: String? = null,
    val tags: List<TagSimpleDto>? = null,
    @SerialName("owner") val owner: AuthorDto? = null,
    @SerialName("publishTime") val publishTime: String? = null,
    @SerialName("nodeInfoList") val inputNodes: List<InputNodeDto>? = null,
    val description: String? = null,
    val covers: List<CoverDto>? = null,
    val statisticsInfo: StatisticsInfo? = null,
    @SerialName("userName") val authorName: String? = null,
    @SerialName("userAvatar") val authorAvatar: String? = null
) {
    fun getDisplayName(): String = owner?.name ?: authorName ?: "Anonymous"
    fun getDisplayAvatar(): String? = owner?.avatar ?: authorAvatar
}

@Serializable
data class InputNodeDto(
    val nodeId: String,
    val nodeName: String,
    val fieldName: String,
    val fieldValue: String? = null,
    val fieldData: String? = null,
    val fieldType: String,
    val description: String? = null,
    val descriptionEn: String? = null
) {
    fun getOptions(): List<String> {
        if (fieldData.isNullOrEmpty()) return emptyList()
        return fieldData.split(Regex("[\\[\\]{},]"))
            .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
            .filter { it.isNotEmpty() && !it.contains(":") && !it.contains("\"") }
    }
}

@Serializable
data class TagSimpleDto(
    val id: String,
    val name: String
)

@Serializable
data class PreviewDto(
    val url: String? = null
)

@Serializable
data class AuthorDto(
    @SerialName("name") val name: String? = null,
    @SerialName("avatar") val avatar: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("intro") val intro: String? = null,
    @SerialName("followCount") val followCount: String? = "0",
    @SerialName("fansCount") val fansCount: String? = "0",
    @SerialName("likeCount") val likeCount: String? = "0",
    @SerialName("collectCount") val collectCount: String? = "0",
    @SerialName("bgImage") val bgImage: String? = null
)

@Serializable
data class StatisticsInfo(
    val likeCount: String? = "0",
    val collectCount: String? = "0",
    val useCount: String? = "0",
    val pv: String? = "0"
)

@Serializable
data class CoverDto(
    val url: String? = null,
    val imageWidth: String? = null,
    val imageHeight: String? = null
)

@Serializable
data class TagDto(
    val id: String? = null,
    val name: String? = null,
    val children: List<TagDto>? = null
)
