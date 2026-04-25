package com.runninghub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.runninghub.app.data.local.dao.DiscoveryDao
import com.runninghub.app.data.local.entity.AppDetailEntity
import com.runninghub.app.data.local.entity.AppEntity
import com.runninghub.app.data.local.entity.BannerEntity
import com.runninghub.app.data.local.entity.CategoryEntity
import com.runninghub.app.data.local.entity.DiscoveryConverters

@Database(
    entities = [BannerEntity::class, CategoryEntity::class, AppEntity::class, AppDetailEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DiscoveryConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun discoveryDao(): DiscoveryDao
}
