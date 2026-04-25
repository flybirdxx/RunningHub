package com.runninghub.shared.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.runninghub.shared.db.shared.newInstance
import com.runninghub.shared.db.shared.schema
import kotlin.Unit

public interface RunningHubDatabase : Transacter {
  public val discoveryCacheQueries: DiscoveryCacheQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = RunningHubDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): RunningHubDatabase =
        RunningHubDatabase::class.newInstance(driver)
  }
}
