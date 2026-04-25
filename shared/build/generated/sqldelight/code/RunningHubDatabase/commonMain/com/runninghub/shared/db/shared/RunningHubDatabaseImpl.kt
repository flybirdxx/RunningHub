package com.runninghub.shared.db.shared

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.runninghub.shared.db.DiscoveryCacheQueries
import com.runninghub.shared.db.RunningHubDatabase
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<RunningHubDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = RunningHubDatabaseImpl.Schema

internal fun KClass<RunningHubDatabase>.newInstance(driver: SqlDriver): RunningHubDatabase =
    RunningHubDatabaseImpl(driver)

private class RunningHubDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), RunningHubDatabase {
  override val discoveryCacheQueries: DiscoveryCacheQueries = DiscoveryCacheQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS BannerEntity (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    title TEXT NOT NULL DEFAULT '',
          |    description TEXT NOT NULL DEFAULT '',
          |    thumbnailUrl TEXT,
          |    previewUrl TEXT,
          |    authorName TEXT,
          |    authorAvatar TEXT,
          |    authorId TEXT,
          |    likeCount INTEGER NOT NULL DEFAULT 0,
          |    collectCount INTEGER NOT NULL DEFAULT 0,
          |    useCount INTEGER NOT NULL DEFAULT 0,
          |    viewCount INTEGER NOT NULL DEFAULT 0,
          |    sortOrder INTEGER NOT NULL DEFAULT 0,
          |    updatedAt INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE IF NOT EXISTS CategoryEntity (
          |    id TEXT NOT NULL PRIMARY KEY,
          |    name TEXT NOT NULL,
          |    parentId TEXT,
          |    sortOrder INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
