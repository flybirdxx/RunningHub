package com.runninghub.feature.auth.data.repository

import com.runninghub.core.common.MissingCredential
import com.runninghub.core.common.MissingCredentialException
import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.User
import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.data.remote.dto.AccountStatusRequestDto
import com.runninghub.feature.auth.data.remote.dto.toDomain
import com.runninghub.feature.auth.domain.UserRepositoryException
import com.runninghub.feature.auth.domain.UserRepositoryIssue
import com.runninghub.feature.auth.domain.UserRepository

/**
 * 用户资料、账户状态和关注关系的数据层实现。
 *
 * 本实现通过 [CredentialStore] 自行读取 API Key，Presentation 不需要也不允许传递敏感凭据。
 * 用户主页 Referer 在 Data 层统一生成，避免 Domain 或 UI 知道 RunningHub Web URL 规则。
 */
class UserRepositoryImpl(
    private val api: AuthApi,
    private val credentialStore: CredentialStore,
) : UserRepository {

    /**
     * 查询当前 API Key 对应账户的余额和权益状态。
     *
     * API Key 从 [CredentialStore] 读取，避免调用方在 UI 或 Domain 间传递敏感凭据；本地未绑定
     * API Key 时返回失败结果，远端业务错误保留服务端消息供上层错误映射。
     */
    override suspend fun getAccountStatus(): Result<AccountStatus> = runCatching {
        val apiKey = credentialStore.getApiKey()?.takeIf { it.isNotBlank() }
            ?: throw MissingCredentialException(MissingCredential.ApiKey)
        val response = api.getAccountStatus(AccountStatusRequestDto(apikey = apiKey))
        requireSuccessfulResponse(response.code, UserRepositoryIssue.AccountStatusFailed)
        response.data?.toDomain()
            ?: throw UserRepositoryException(UserRepositoryIssue.AccountStatusMissing)
    }

    /**
     * 查询当前用户或指定用户的基础资料。
     *
     * `userId` 为空时按服务端默认语义查询当前会话用户；非空时显式传递用户 ID。
     * DTO 到 Domain 的字段兼容和默认值由 mapper 统一处理。
     */
    override suspend fun getUserInfo(userId: String?): Result<User> = runCatching {
        val params = if (!userId.isNullOrEmpty()) mapOf("userId" to userId) else emptyMap()
        val response = api.getUserInfo(params)
        requireSuccessfulResponse(response.code, UserRepositoryIssue.UserInfoFailed)
        response.data?.toDomain()
            ?: throw UserRepositoryException(UserRepositoryIssue.UserInfoMissing)
    }

    /**
     * 查询目标用户主页详情。
     *
     * RunningHub 用户中心要求部分主页接口携带 Web Referer，因此 Referer 在 Data 层根据用户 ID
     * 生成，避免 Presentation 知道 Web URL 拼接规则。
     */
    override suspend fun getUserDetail(userId: String): Result<User> = runCatching {
        val referer = profileReferer(userId)
        val response = api.getUserDetail(referer, mapOf("userId" to userId))
        requireSuccessfulResponse(response.code, UserRepositoryIssue.UserDetailFailed)
        response.data?.toDomain()
            ?: throw UserRepositoryException(UserRepositoryIssue.UserDetailMissing)
    }

    /**
     * 查询当前登录用户是否关注目标用户。
     *
     * 服务端缺少布尔数据时按 false 处理，这是兼容旧接口空响应的降级策略，不代表远端一定返回未关注。
     */
    override suspend fun isFollow(targetUserId: String): Result<Boolean> = runCatching {
        val response = api.isFollow(profileReferer(targetUserId), mapOf("followId" to targetUserId))
        requireSuccessfulResponse(response.code, UserRepositoryIssue.FollowStatusFailed)
        response.data ?: false
    }

    /**
     * 关注目标用户。
     *
     * 关注接口依赖用户主页 Referer，Data 层统一附加该请求头；服务端返回空数据时按 false 处理，
     * 调用方可据此保持原有关注状态。
     */
    override suspend fun followUser(targetUserId: String): Result<Boolean> = runCatching {
        val response = api.followUser(profileReferer(targetUserId), mapOf("followId" to targetUserId))
        requireSuccessfulResponse(response.code, UserRepositoryIssue.FollowUserFailed)
        response.data ?: false
    }

    /**
     * 取消关注目标用户。
     *
     * 取消关注使用与关注相同的 Referer 规则；远端业务失败会进入 [Result.failure]，由调用方决定
     * 是否回滚 UI 乐观状态。
     */
    override suspend fun unFollowUser(targetUserId: String): Result<Boolean> = runCatching {
        val response = api.unFollowUser(profileReferer(targetUserId), mapOf("followId" to targetUserId))
        requireSuccessfulResponse(response.code, UserRepositoryIssue.UnfollowUserFailed)
        response.data ?: false
    }

    private fun requireSuccessfulResponse(code: Int, issue: UserRepositoryIssue) {
        if (code != 0) {
            // 服务端 msg 不能进入异常 message；这里只保留稳定语义和业务 code 供上层分类。
            throw UserRepositoryException(issue, code)
        }
    }

    private fun profileReferer(userId: String): String =
        RunningHubApiEnvironment.webUrl("profile/$userId")
}
