package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.User

interface AuthRepository {
    suspend fun login(phone: String, password: String): Result<User>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun refreshTokenIfNeeded(): Result<String>
    suspend fun getCurrentAuthToken(): String?
    suspend fun getCurrentUserId(): String?
}
