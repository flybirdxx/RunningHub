package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.core.model.User
import com.runninghub.feature.auth.domain.AuthRepository

/**
 * QuickCreate Data 测试使用的认证仓库。
 *
 * 生产 Repository 现在必须持有 [AuthRepository]，以保证 TOKEN_INVALID 能走统一刷新语义。
 * 测试默认不关心刷新时使用本 Fake；需要断言刷新次数的用例可以读取 [refreshCalls]。
 */
internal class FakeAuthRepository : AuthRepository {
    var refreshCalls: Int = 0

    override suspend fun login(phone: String, password: String): Result<User> =
        Result.failure(NotImplementedError())

    override suspend fun sendSmsCode(phone: String): Result<Unit> =
        Result.failure(NotImplementedError())

    override suspend fun smsLogin(phone: String, code: String): Result<User> =
        Result.failure(NotImplementedError())

    override suspend fun logout() {}

    override suspend fun isLoggedIn(): Boolean = true

    override suspend fun refreshTokenIfNeeded(): Result<String> {
        refreshCalls += 1
        return Result.success("fresh-token")
    }

    override suspend fun getCurrentAuthToken(): String? = "fresh-token"

    override suspend fun getCurrentUserId(): String? = "user-1"
}
