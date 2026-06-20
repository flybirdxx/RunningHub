package com.runninghub.shared.data.repository

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.RunningHubApi
import com.runninghub.shared.data.remote.dto.AccountStatusRequest
import com.runninghub.shared.data.remote.dto.toDomain
import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.UserRepository

/**
 * UserRepository 的 Data 层实现。
 *
 * 用户资料接口沿用当前登录会话；账户状态接口需要 API Key，本类通过 [CredentialStore]
 * 自行读取凭据，避免 Profile 等 UI 层继续传递敏感值。
 *
 * @param api RunningHub 用户相关网络接口。
 * @param credentialStore API Key 读取边界。
 */
class UserRepositoryImpl(
    private val api: RunningHubApi,
    private val credentialStore: CredentialStore,
) : UserRepository {

    override suspend fun getAccountStatus(): Result<AccountStatus> = runCatching {
        val apiKey = credentialStore.getApiKey()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("请先在设置中绑定 API Key")
        val response = api.getAccountStatus(AccountStatusRequest(apikey = apiKey))
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    override suspend fun getUserInfo(userId: String?): Result<User> = runCatching {
        val params = if (!userId.isNullOrEmpty()) mapOf("userId" to userId) else emptyMap()
        val response = api.getUserInfo(params)
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    /**
     * 查询指定用户详情。
     *
     * 用户中心接口会校验 Web 主页 Referer，因此 Referer 由 Data 层基于统一环境地址生成，
     * 避免调用方或 Domain 层了解 RunningHub 的页面 URL 规则。
     *
     * @param userId 目标用户 ID。
     * @return 成功时返回用户领域模型；远端业务错误或空数据会转为失败结果。
     */
    override suspend fun getUserDetail(userId: String): Result<User> = runCatching {
        val referer = profileReferer(userId)
        val response = api.getUserDetail(referer, mapOf("userId" to userId))
        check(response.code == 0) { response.msg }
        response.data?.toDomain() ?: throw IllegalStateException("Empty response data")
    }

    /**
     * 查询当前用户是否关注目标用户。
     *
     * Referer 在 Data 层统一生成，保持关注相关接口与用户详情接口使用相同的环境配置。
     *
     * @param targetUserId 目标用户 ID。
     * @return 成功时返回关注状态；远端缺失布尔值时按 false 处理。
     */
    override suspend fun isFollow(targetUserId: String): Result<Boolean> = runCatching {
        val referer = profileReferer(targetUserId)
        val response = api.isFollow(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data ?: false
    }

    /**
     * 关注目标用户。
     *
     * @param targetUserId 目标用户 ID。
     * @return 成功时返回服务端确认状态；远端缺失布尔值时按 false 处理。
     */
    override suspend fun followUser(targetUserId: String): Result<Boolean> = runCatching {
        val referer = profileReferer(targetUserId)
        val response = api.followUser(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data ?: false
    }

    /**
     * 取消关注目标用户。
     *
     * @param targetUserId 目标用户 ID。
     * @return 成功时返回服务端确认状态；远端缺失布尔值时按 false 处理。
     */
    override suspend fun unFollowUser(targetUserId: String): Result<Boolean> = runCatching {
        val referer = profileReferer(targetUserId)
        val response = api.unFollowUser(referer, mapOf("followId" to targetUserId))
        check(response.code == 0) { response.msg }
        response.data ?: false
    }

    private fun profileReferer(userId: String): String =
        RunningHubApiEnvironment.webUrl("profile/$userId")
}
