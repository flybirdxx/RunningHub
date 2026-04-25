package com.runninghub.app.data.local.dao

import androidx.room.*
import com.runninghub.app.data.local.entity.AppDetailEntity
import com.runninghub.app.data.local.entity.AppEntity
import com.runninghub.app.data.local.entity.BannerEntity
import com.runninghub.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {
    // Banners
    @Query("SELECT * FROM banners ORDER BY `order` ASC")
    fun getBanners(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<BannerEntity>)

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    // Categories
    @Query("SELECT * FROM categories")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    // Apps
    @Query("SELECT * FROM discovery_apps WHERE categoryName = :categoryName ORDER BY timestamp DESC")
    fun getAppsByCategory(categoryName: String): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Query("DELETE FROM discovery_apps WHERE categoryName = :categoryName")
    suspend fun clearAppsByCategory(categoryName: String)

    @Query("SELECT MIN(timestamp) FROM discovery_apps WHERE categoryName = :categoryName")
    suspend fun getMinTimestampByCategory(categoryName: String): Long?

    @Transaction
    suspend fun refreshApps(categoryName: String, apps: List<AppEntity>) {
        clearAppsByCategory(categoryName)
        insertApps(apps)
    }

    // App Details
    @Query("SELECT * FROM app_details WHERE id = :id")
    suspend fun getAppDetail(id: String): AppDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppDetail(detail: AppDetailEntity)

    @Query("DELETE FROM app_details WHERE id = :id")
    suspend fun clearAppDetail(id: String)
}
