package com.runninghub.shared.domain.model

enum class CoverMediaType { IMAGE, GIF, VIDEO }

data class WebApp(
    val id: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val coverUrl: String?,
    val coverMediaType: CoverMediaType,
    val coverWidth: String?,
    val coverHeight: String?,
    val author: Author?,
    val tags: List<TagSimple>,
    val likeCount: String,
    val collectCount: String,
    val useCount: String,
    val pv: String,
    val carefullyChosen: Boolean = false,
)

data class Author(
    val id: String?,
    val name: String?,
    val avatar: String?,
    val intro: String?,
    val followCount: String,
    val fansCount: String,
    val likeCount: String,
    val collectCount: String,
    val bgImage: String?
)

data class TagSimple(
    val id: String,
    val name: String,
    val nameEn: String? = null,
    val labels: String? = null
)

data class Cover(
    val url: String?,
    val imageWidth: String?,
    val imageHeight: String?
)

data class StatisticsInfo(
    val likeCount: String,
    val collectCount: String,
    val useCount: String,
    val pv: String
)
