package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.AccountStatusRequest
import com.runninghub.shared.data.remote.dto.toDomain
import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.UserRepository

class UserRepositoryImpl(
    private val api: RunningHubApi
) : UserRepository {

    override suspend fun getAccountStatus(apiKey: String): Result<AccountStatus> = runCatching {
        val response = api.getAccountStatus(AccountStatusRequest(apikey = apiKey))
        check(response.code == 0) { response.msg }
        response.data.toDomain()
    }

    override suspend fun getUserInfo(userId: String?): Result<User> = runCatching {
        val params = if (!userId.isNullOrEmpty()) mapOf("userId" to userId) else emptyMap()
        val response = api.getUserInfo(params)
        check(response.code == 0) { response.msg }
        response.data.toDomain()
    }

    override suspend fun getUserDetail(userId: String): Result<User> = runCatching {
        val referer = "https://www.runninghub.cn/profile/$userId"
        val response = api.getUserDetail(referer, mapOf("userId" to userId))
        check(response.code == 0) { response.msg }
        response.data.toDomain()
    }

    override suspend fun isFollow(targetUserId: String): Result<Boolean> = runCatching {
        val referer = "https://www.runninghub.cn/profile/$targetUserId"
        val response = api.isFollow(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data
    }

    override suspend fun followUser(targetUserId: String): Result<Boolean> = runCatching {
        val referer = "https://www.runninghub.cn/profile/$targetUserId"
        val response = api.followUser(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data
    }

    override suspend fun unFollowUser(targetUserId: String): Result<Boolean> = runCatching {
        val referer = "https://www.runninghub.cn/profile/$targetUserId"
        val response = api.unFollowUser(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data
    }
}
