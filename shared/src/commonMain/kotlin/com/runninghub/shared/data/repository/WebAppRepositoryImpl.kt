package com.runninghub.shared.data.repository

import com.runninghub.shared.data.mapper.toDomain
import com.runninghub.shared.data.model.TagTreeRequest
import com.runninghub.shared.data.model.WebAppListRequest
import com.runninghub.shared.data.remote.WebAppApiService
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.WebApp
import com.runninghub.shared.domain.model.WebAppDetail
import com.runninghub.shared.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WebAppRepositoryImpl(
    private val api: WebAppApiService
) : WebAppRepository {

    override fun getWebApps(
        page: Int,
        pageSize: Int,
        tags: List<String>,
        sort: String
    ): Flow<AppResult<List<WebApp>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getWebAppList(
                WebAppListRequest(size = pageSize, current = page, tags = tags, sort = sort)
            )
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.records.map { it.toDomain() }))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override fun getFeaturedApps(): Flow<AppResult<List<WebApp>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getCarefullyChosenList()
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.map { it.toDomain() }))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override fun getWebAppDetail(appId: String, apiKey: String): Flow<AppResult<WebAppDetail>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getApiCallDemo(apiKey = apiKey, webappId = appId)
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.toDomain()))
            } else {
                val fallback = api.getWebAppDetail(mapOf("webappId" to appId))
                if (fallback.code == 0 && fallback.data != null) {
                    emit(AppResult.Success(fallback.data.toDomain()))
                } else {
                    emit(AppResult.Error(fallback.msg))
                }
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override fun getCategories(): Flow<AppResult<List<Tag>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getTagTree(TagTreeRequest())
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.map { it.toDomain() }))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override fun searchApps(query: String, page: Int, pageSize: Int): Flow<AppResult<List<WebApp>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getWebAppList(
                WebAppListRequest(size = pageSize, current = page, tags = emptyList(), sort = "RECOMMEND")
            )
            if (response.code == 0 && response.data != null) {
                val filtered = response.data.records
                    .filter { (it.title ?: "").contains(query, ignoreCase = true) || (it.desc ?: "").contains(query, ignoreCase = true) }
                    .map { it.toDomain() }
                emit(AppResult.Success(filtered))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override fun getUserApps(userId: String, page: Int, pageSize: Int): Flow<AppResult<List<WebApp>>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getWebAppUserList(
                mapOf("userId" to userId, "current" to page.toString(), "size" to pageSize.toString())
            )
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.records.map { it.toDomain() }))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }
}
