package com.runninghub.shared.db

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class DiscoveryCacheQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> getAllBanners(mapper: (
    id: String,
    title: String,
    description: String,
    thumbnailUrl: String?,
    previewUrl: String?,
    authorName: String?,
    authorAvatar: String?,
    authorId: String?,
    likeCount: Long,
    collectCount: Long,
    useCount: Long,
    viewCount: Long,
    sortOrder: Long,
    updatedAt: Long,
  ) -> T): Query<T> = Query(1_638_563_006, arrayOf("BannerEntity"), driver, "DiscoveryCache.sq",
      "getAllBanners",
      "SELECT BannerEntity.id, BannerEntity.title, BannerEntity.description, BannerEntity.thumbnailUrl, BannerEntity.previewUrl, BannerEntity.authorName, BannerEntity.authorAvatar, BannerEntity.authorId, BannerEntity.likeCount, BannerEntity.collectCount, BannerEntity.useCount, BannerEntity.viewCount, BannerEntity.sortOrder, BannerEntity.updatedAt FROM BannerEntity ORDER BY sortOrder ASC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getLong(13)!!
    )
  }

  public fun getAllBanners(): Query<BannerEntity> = getAllBanners { id, title, description,
      thumbnailUrl, previewUrl, authorName, authorAvatar, authorId, likeCount, collectCount,
      useCount, viewCount, sortOrder, updatedAt ->
    BannerEntity(
      id,
      title,
      description,
      thumbnailUrl,
      previewUrl,
      authorName,
      authorAvatar,
      authorId,
      likeCount,
      collectCount,
      useCount,
      viewCount,
      sortOrder,
      updatedAt
    )
  }

  public fun <T : Any> getAllCategories(mapper: (
    id: String,
    name: String,
    parentId: String?,
    sortOrder: Long,
  ) -> T): Query<T> = Query(159_602_885, arrayOf("CategoryEntity"), driver, "DiscoveryCache.sq",
      "getAllCategories",
      "SELECT CategoryEntity.id, CategoryEntity.name, CategoryEntity.parentId, CategoryEntity.sortOrder FROM CategoryEntity ORDER BY sortOrder ASC") {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!
    )
  }

  public fun getAllCategories(): Query<CategoryEntity> = getAllCategories { id, name, parentId,
      sortOrder ->
    CategoryEntity(
      id,
      name,
      parentId,
      sortOrder
    )
  }

  public fun insertBanner(
    id: String,
    title: String,
    description: String,
    thumbnailUrl: String?,
    previewUrl: String?,
    authorName: String?,
    authorAvatar: String?,
    authorId: String?,
    likeCount: Long,
    collectCount: Long,
    useCount: Long,
    viewCount: Long,
    sortOrder: Long,
    updatedAt: Long,
  ) {
    driver.execute(-1_384_385_501, """
        |INSERT OR REPLACE INTO BannerEntity(id, title, description, thumbnailUrl, previewUrl, authorName, authorAvatar, authorId, likeCount, collectCount, useCount, viewCount, sortOrder, updatedAt)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 14) {
          bindString(0, id)
          bindString(1, title)
          bindString(2, description)
          bindString(3, thumbnailUrl)
          bindString(4, previewUrl)
          bindString(5, authorName)
          bindString(6, authorAvatar)
          bindString(7, authorId)
          bindLong(8, likeCount)
          bindLong(9, collectCount)
          bindLong(10, useCount)
          bindLong(11, viewCount)
          bindLong(12, sortOrder)
          bindLong(13, updatedAt)
        }
    notifyQueries(-1_384_385_501) { emit ->
      emit("BannerEntity")
    }
  }

  public fun deleteAllBanners() {
    driver.execute(-47_842_353, """DELETE FROM BannerEntity""", 0)
    notifyQueries(-47_842_353) { emit ->
      emit("BannerEntity")
    }
  }

  public fun insertCategory(
    id: String,
    name: String,
    parentId: String?,
    sortOrder: Long,
  ) {
    driver.execute(-1_343_238_091, """
        |INSERT OR REPLACE INTO CategoryEntity(id, name, parentId, sortOrder)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, id)
          bindString(1, name)
          bindString(2, parentId)
          bindLong(3, sortOrder)
        }
    notifyQueries(-1_343_238_091) { emit ->
      emit("CategoryEntity")
    }
  }

  public fun deleteAllCategories() {
    driver.execute(-1_309_985_772, """DELETE FROM CategoryEntity""", 0)
    notifyQueries(-1_309_985_772) { emit ->
      emit("CategoryEntity")
    }
  }
}
