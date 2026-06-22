package com.runninghub.feature.auth.domain

import com.runninghub.core.model.AccountStatus
import com.runninghub.core.model.User

/**
 * 用户资料、账户状态与关注关系的领域仓库契约。
 *
 * 该接口属于 Auth Domain，负责表达当前登录用户、公开用户资料、账户状态和关注关系等
 * 与身份相关的业务能力。账户状态查询需要 API Key，但该凭据由 Data 层实现自行读取，
 * Presentation 层不应直接传递或持有完整敏感凭据。
 */
interface UserRepository {
    /**
     * 查询当前绑定 API Key 对应的账户状态。
     *
     * @return 成功时返回账户余额、套餐等状态；未绑定 API Key、凭据失效、网络失败或服务端返回异常时返回失败结果。
     */
    suspend fun getAccountStatus(): Result<AccountStatus>

    /**
     * 读取当前登录用户或指定用户的基础资料。
     *
     * @param userId 目标用户 ID；`null` 表示读取当前登录用户，非空时表示读取指定用户资料。
     * @return 成功时返回用户领域模型；会话失效、用户不存在或远端返回空数据时返回失败结果。
     */
    suspend fun getUserInfo(userId: String? = null): Result<User>

    /**
     * 读取指定用户的公开详情资料。
     *
     * @param userId 目标用户 ID，必须来自路由参数或服务端返回的稳定用户标识。
     * @return 成功时返回创作者公开资料；用户不存在、未授权或网络失败时返回失败结果。
     */
    suspend fun getUserDetail(userId: String): Result<User>

    /**
     * 查询当前用户是否已关注目标用户。
     *
     * @param targetUserId 被查询的目标用户 ID。
     * @return 成功时返回关注状态；未登录、目标不存在或远端失败时返回失败结果。
     */
    suspend fun isFollow(targetUserId: String): Result<Boolean>

    /**
     * 关注目标用户。
     *
     * @param targetUserId 被关注的目标用户 ID。
     * @return 成功时返回服务端确认状态；重复关注、未登录或远端失败时返回失败结果。
     */
    suspend fun followUser(targetUserId: String): Result<Boolean>

    /**
     * 取消关注目标用户。
     *
     * @param targetUserId 被取消关注的目标用户 ID。
     * @return 成功时返回服务端确认状态；未关注、未登录或远端失败时返回失败结果。
     */
    suspend fun unFollowUser(targetUserId: String): Result<Boolean>
}

/**
 * 用户资料仓库的结构化失败异常。
 *
 * Data 层用该异常表达账户状态、用户资料和关注接口的稳定失败语义；[message] 只保留诊断码，
 * Presentation 不得把它作为最终用户可见文案展示。
 *
 * @property issue 用户资料仓库的稳定失败语义。
 * @property remoteCode 服务端业务 code；`null` 表示失败来自响应缺字段或本地结构校验。
 */
class UserRepositoryException(
    val issue: UserRepositoryIssue,
    val remoteCode: Int? = null,
) : IllegalStateException(issue.diagnosticMessage(remoteCode))

/**
 * 用户资料仓库使用的稳定错误语义。
 *
 * 这些枚举值只用于上层按类型判断错误和日志分类，不携带服务端 `msg`、底层异常 message
 * 或最终中文 UI 文案。
 *
 * @property code 稳定诊断码，可用于测试断言和日志分类；不得作为最终 UI 文案。
 */
enum class UserRepositoryIssue(val code: String) {
    /** 账户状态接口返回非成功业务 code。 */
    AccountStatusFailed("USER_ACCOUNT_STATUS_FAILED"),

    /** 账户状态接口成功但响应缺少 data。 */
    AccountStatusMissing("USER_ACCOUNT_STATUS_MISSING"),

    /** 用户基础资料接口返回非成功业务 code。 */
    UserInfoFailed("USER_INFO_FAILED"),

    /** 用户基础资料接口成功但响应缺少 data。 */
    UserInfoMissing("USER_INFO_MISSING"),

    /** 用户公开详情接口返回非成功业务 code。 */
    UserDetailFailed("USER_DETAIL_FAILED"),

    /** 用户公开详情接口成功但响应缺少 data。 */
    UserDetailMissing("USER_DETAIL_MISSING"),

    /** 关注状态接口返回非成功业务 code。 */
    FollowStatusFailed("USER_FOLLOW_STATUS_FAILED"),

    /** 关注接口返回非成功业务 code。 */
    FollowUserFailed("USER_FOLLOW_USER_FAILED"),

    /** 取消关注接口返回非成功业务 code。 */
    UnfollowUserFailed("USER_UNFOLLOW_USER_FAILED"),
}

private fun UserRepositoryIssue.diagnosticMessage(remoteCode: Int?): String =
    if (remoteCode == null) code else "$code:$remoteCode"
