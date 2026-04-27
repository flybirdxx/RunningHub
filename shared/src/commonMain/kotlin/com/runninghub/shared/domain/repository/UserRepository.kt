package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User

interface UserRepository {
    suspend fun getAccountStatus(apiKey: String): Result<AccountStatus>
    suspend fun getUserInfo(userId: String? = null): Result<User>
    suspend fun getUserDetail(userId: String): Result<User>
    suspend fun isFollow(targetUserId: String): Result<Boolean>
    suspend fun followUser(targetUserId: String): Result<Boolean>
    suspend fun unFollowUser(targetUserId: String): Result<Boolean>
}
