package com.runninghub.shared.db

import kotlin.Long
import kotlin.String

public data class BannerEntity(
  public val id: String,
  public val title: String,
  public val description: String,
  public val thumbnailUrl: String?,
  public val previewUrl: String?,
  public val authorName: String?,
  public val authorAvatar: String?,
  public val authorId: String?,
  public val likeCount: Long,
  public val collectCount: Long,
  public val useCount: Long,
  public val viewCount: Long,
  public val sortOrder: Long,
  public val updatedAt: Long,
)
