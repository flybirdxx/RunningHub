package com.runninghub.shared.domain.repository

import com.runninghub.shared.domain.model.AccountStatus
import com.runninghub.shared.domain.model.User

/**
 * 用户资料与账户状态仓库。
 *
 * 账户状态查询需要 API Key，但该凭据由 Data 层实现自行读取，Presentation 层不应直接传递。
 */
interface UserRepository {
    /** 查询当前绑定 API Key 对应的账户状态；未绑定时返回失败结果。 */
    suspend fun getAccountStatus(): Result<AccountStatus>

    /** 读取当前登录用户或指定用户的基础资料。 */
    suspend fun getUserInfo(userId: String? = null): Result<User>

    /** 读取指定用户的公开详情资料。 */
    suspend fun getUserDetail(userId: String): Result<User>

    /** 查询当前用户是否已关注目标用户。 */
    suspend fun isFollow(targetUserId: String): Result<Boolean>

    /** 关注目标用户。 */
    suspend fun followUser(targetUserId: String): Result<Boolean>

    /** 取消关注目标用户。 */
    suspend fun unFollowUser(targetUserId: String): Result<Boolean>
}
