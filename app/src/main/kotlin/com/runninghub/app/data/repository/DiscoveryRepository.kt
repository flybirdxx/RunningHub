package com.runninghub.app.data.repository

import com.runninghub.app.data.local.dao.DiscoveryDao
import com.runninghub.app.data.local.entity.AppEntity
import com.runninghub.app.data.local.entity.BannerEntity
import com.runninghub.app.data.local.entity.CategoryEntity
import com.runninghub.app.data.local.entity.AppDetailEntity
import com.runninghub.app.data.remote.api.WebAppApi
import com.runninghub.app.data.remote.model.TagTreeRequest
import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.WebAppListRequest
import com.runninghub.app.ui.feature.discovery.AiApp
import com.runninghub.app.ui.feature.discovery.Banner
import com.runninghub.app.ui.feature.discovery.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryRepository @Inject constructor(
    private val webAppApi: WebAppApi,
    private val discoveryDao: DiscoveryDao
) {

    // Streams from Local Database
    val banners: Flow<List<Banner>> = discoveryDao.getBanners().map { entities ->
        entities.map { it.toDomain() }
    }

    val categories: Flow<List<Category>> = discoveryDao.getCategories().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getApps(categoryName: String): Flow<List<AiApp>> = 
        discoveryDao.getAppsByCategory(categoryName).map { entities ->
            entities.map { it.toDomain() }
        }

    // Network Sync Operations
    suspend fun refreshBanners() {
        try {
            val response = webAppApi.getCarefullyChosenList()
            if (response.code == 0 && response.data.isNotEmpty()) {
                val entities = response.data.mapIndexed { index, dto ->
                    BannerEntity(
                        id = dto.id ?: "",
                        title = dto.title ?: "Untitled",
                        description = dto.desc,
                        imageUrl = dto.preview?.url ?: dto.covers?.firstOrNull()?.url ?: "",
                        tag = dto.tags?.firstOrNull()?.name,
                        order = index
                    )
                }
                discoveryDao.clearBanners()
                discoveryDao.insertBanners(entities)
            }
        } catch (e: Exception) {
            // Log or handle error if needed
        }
    }

    suspend fun refreshCategories() {
        try {
            val response = webAppApi.getTagTree(TagTreeRequest())
            if (response.code == 0 && response.data.isNotEmpty()) {
                val allTags = response.data
                val level1Tags = allTags.filter { it.level == 1 }
                
                val entities = level1Tags.map { parent ->
                    CategoryEntity(
                        name = parent.name,
                        tagIds = collectLeafIds(parent, allTags)
                    )
                }
                discoveryDao.clearCategories()
                discoveryDao.insertCategories(entities)
            }
        } catch (e: Exception) {
            // Use fallback logic could be moved here if we want absolute persistence of fallbacks
        }
    }

    suspend fun refreshApps(categoryName: String, tagIds: List<String>) {
        try {
            val response = webAppApi.getWebAppList(
                WebAppListRequest(
                    size = 30,
                    current = 1,
                    tags = tagIds
                )
            )
            if (response.code == 0) {
                val baseTime = System.currentTimeMillis()
                val entities = response.data.records.mapIndexed { index, dto ->
                    AppEntity(
                        id = dto.id ?: "",
                        title = dto.title ?: "Untitled",
                        author = dto.author?.name ?: "Anonymous",
                        authorAvatar = dto.author?.avatar,
                        imageUrl = dto.covers?.firstOrNull()?.url ?: dto.preview?.url ?: "",
                        likes = dto.statisticsInfo?.likeCount?.toIntOrNull() ?: 0,
                        stars = dto.statisticsInfo?.collectCount?.toIntOrNull() ?: 0,
                        useCount = dto.statisticsInfo?.useCount ?: "0",
                        views = dto.statisticsInfo?.pv ?: "0",
                        categoryName = categoryName,
                        timestamp = baseTime - index
                    )
                }
                discoveryDao.refreshApps(categoryName, entities)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    suspend fun loadMoreApps(categoryName: String, tagIds: List<String>, page: Int): Int? {
        return try {
            val response = webAppApi.getWebAppList(
                WebAppListRequest(
                    size = 30,
                    current = page,
                    tags = tagIds
                )
            )
            if (response.code == 0) {
                val records = response.data.records
                if (records.isNotEmpty()) {
                    val minTimestamp = discoveryDao.getMinTimestampByCategory(categoryName)
                    val baseTime = (minTimestamp ?: System.currentTimeMillis()) - 1000L
                    val entities = records.mapIndexed { index, dto ->
                        AppEntity(
                            id = dto.id ?: "",
                            title = dto.title ?: "Untitled",
                            author = dto.author?.name ?: "Anonymous",
                            authorAvatar = dto.author?.avatar,
                            imageUrl = dto.covers?.firstOrNull()?.url ?: dto.preview?.url ?: "",
                            likes = dto.statisticsInfo?.likeCount?.toIntOrNull() ?: 0,
                            stars = dto.statisticsInfo?.collectCount?.toIntOrNull() ?: 0,
                            useCount = dto.statisticsInfo?.useCount ?: "0",
                            views = dto.statisticsInfo?.pv ?: "0",
                            categoryName = categoryName,
                            timestamp = baseTime - index
                        )
                    }
                    discoveryDao.insertApps(entities)
                    records.size
                } else {
                    0
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAppDetail(appId: String): WebAppDetailDto? {
        val entity = discoveryDao.getAppDetail(appId) ?: return null
        return try {
            val gson = com.google.gson.Gson()
            gson.fromJson(entity.jsonContent, WebAppDetailDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveAppDetail(detail: WebAppDetailDto) {
        val id = detail.id ?: return
        try {
            val json = com.google.gson.Gson().toJson(detail)
            discoveryDao.insertAppDetail(AppDetailEntity(id, json))
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Helper conversion methods
    private fun BannerEntity.toDomain() = Banner(id, title, description, imageUrl, tag)
    private fun CategoryEntity.toDomain() = Category(tagIds, name)
    private fun AppEntity.toDomain() = AiApp(id, title, author, authorAvatar, imageUrl, likes, stars, useCount, views)

    private fun collectLeafIds(parent: com.runninghub.app.data.remote.model.TagDto, allTags: List<com.runninghub.app.data.remote.model.TagDto>): List<String> {
        val children = parent.childTags ?: emptyList()
        if (children.isEmpty()) return listOf(parent.id)
        return children.flatMap { collectLeafIds(it, allTags) }
    }
}
