package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getCurrentUser(): Flow<AppResult<User>>
    fun getUserDetail(userId: String): Flow<AppResult<User>>
    fun getAccountStatus(apiKey: String): Flow<AppResult<AccountStatus>>
    suspend fun isFollowing(userId: String): AppResult<Boolean>
    suspend fun followUser(userId: String): AppResult<Boolean>
    suspend fun unfollowUser(userId: String): AppResult<Boolean>
}
