package com.runninghub.shared.data.repository

import com.runninghub.shared.data.mapper.toDomain
import com.runninghub.shared.data.model.AccountStatusRequest
import com.runninghub.shared.data.remote.WebAppApiService
import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class UserRepositoryImpl(
    private val api: WebAppApiService,
    private val settings: SettingsRepository
) : UserRepository {

    override fun getCurrentUser(): Flow<AppResult<User>> = flow {
        emit(AppResult.Loading)
        try {
            val cookie = settings.getCookieSync()
            val apiKey = settings.getApiKeySync()

            if (cookie.isNotEmpty()) {
                val response = api.getUserInfo()
                if (response.code == 0 && response.data != null) {
                    emit(AppResult.Success(response.data.toDomain()))
                } else {
                    emit(AppResult.Error(response.msg))
                }
            } else if (apiKey.isNotEmpty()) {
                val statusResponse = api.getAccountStatus(AccountStatusRequest(apikey = apiKey))
                if (statusResponse.code == 0 && statusResponse.data != null) {
                    val status = statusResponse.data
                    val fallbackUser = User(
                        id = "--",
                        nickName = "RunningHub用户",
                        avatarUrl = null,
                        mobile = null,
                        totalCoin = status.remainCoins,
                        memberName = null,
                        memberExpiredTime = null,
                        balance = status.remainMoney?.toDoubleOrNull() ?: 0.0,
                        currency = status.currency,
                        apiKey = null,
                        apiType = status.apiType,
                        introduce = null,
                        fanCount = 0,
                        followCount = 0,
                        likeCount = 0,
                        collectCount = 0
                    )
                    emit(AppResult.Success(fallbackUser))
                } else {
                    emit(AppResult.Error(statusResponse.msg))
                }
            } else {
                emit(AppResult.Error("Not authenticated"))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getUserDetail(userId: String): Flow<AppResult<User>> = flow {
        emit(AppResult.Loading)
        try {
            val referer = "https://www.runninghub.cn/user/$userId"
            val response = api.getUserDetail(referer, mapOf("userId" to userId))
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.toDomain()))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getAccountStatus(apiKey: String): Flow<AppResult<AccountStatus>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getAccountStatus(AccountStatusRequest(apikey = apiKey))
            if (response.code == 0 && response.data != null) {
                emit(AppResult.Success(response.data.toDomain()))
            } else {
                emit(AppResult.Error(response.msg))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Unknown error"))
        }
    }

    override suspend fun isFollowing(userId: String): AppResult<Boolean> = try {
        val referer = "https://www.runninghub.cn/user/$userId"
        val response = api.isFollow(referer, mapOf("userId" to userId))
        if (response.code == 0) AppResult.Success(response.data ?: false)
        else AppResult.Error(response.msg)
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error")
    }

    override suspend fun followUser(userId: String): AppResult<Boolean> = try {
        val referer = "https://www.runninghub.cn/user/$userId"
        val response = api.followUser(referer, mapOf("userId" to userId))
        if (response.code == 0) AppResult.Success(true)
        else AppResult.Error(response.msg)
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error")
    }

    override suspend fun unfollowUser(userId: String): AppResult<Boolean> = try {
        val referer = "https://www.runninghub.cn/user/$userId"
        val response = api.unFollowUser(referer, mapOf("userId" to userId))
        if (response.code == 0) AppResult.Success(true)
        else AppResult.Error(response.msg)
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error")
    }
}
