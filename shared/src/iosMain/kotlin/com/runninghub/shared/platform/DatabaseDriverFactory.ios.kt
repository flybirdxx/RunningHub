package com.runninghub.shared.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.runninghub.shared.db.RunningHubDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(RunningHubDatabase.Schema, "runninghub.db")
    }
}
