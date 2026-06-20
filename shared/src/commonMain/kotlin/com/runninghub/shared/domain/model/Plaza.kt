package com.runninghub.shared.domain.model

data class PlazaTag(
    val id: String,
    val name: String,
    val level: Int = 0,
    val enable: Boolean = true,
)

data class PlazaCreationPage(
    val page: Int,
    val total: Int,
    val items: List<PlazaCreationCard>,
)

data class PlazaCreationCard(
    val id: String,
    val intro: String? = null,
    val publishTime: String? = null,
    val ownerName: String? = null,
    val ownerAvatar: String? = null,
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val likeCount: String? = null,
    val useCount: String? = null,
    val collectCount: String? = null,
    val liked: Boolean = false,
    val collected: Boolean = false,
)

data class PlazaShortCategory(
    val id: String? = null,
    val code: String,
    val name: String,
)

data class PlazaShortCard(
    val id: String,
    val name: String,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val categoryName: String? = null,
    val authorName: String? = null,
    val authorAvatar: String? = null,
)
