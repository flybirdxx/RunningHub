package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.*
import kotlinx.coroutines.flow.Flow

interface WebAppRepository {
    fun getWebApps(page: Int, pageSize: Int, tags: List<String>, sort: String): Flow<AppResult<List<WebApp>>>
    fun getFeaturedApps(): Flow<AppResult<List<WebApp>>>
    fun getWebAppDetail(appId: String, apiKey: String): Flow<AppResult<WebAppDetail>>
    fun getCategories(): Flow<AppResult<List<Tag>>>
    fun searchApps(query: String, page: Int, pageSize: Int): Flow<AppResult<List<WebApp>>>
    fun getUserApps(userId: String, page: Int, pageSize: Int): Flow<AppResult<List<WebApp>>>
}
