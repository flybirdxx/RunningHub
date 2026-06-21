package com.runninghub.feature.auth.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.domain.SessionRestoreRepository

/**
 * 会话恢复仓库的数据层实现。
 *
 * 本类只通过 [CredentialStore] 判断本地是否存在 access token，不把 Token 内容暴露给
 * Presentation。令牌是否过期、是否需要刷新仍由认证仓库和 core/network 拦截器统一处理。
 */
class SessionRestoreRepositoryImpl(
    private val credentialStore: CredentialStore,
) : SessionRestoreRepository {

    /**
     * 判断本地是否存在可尝试恢复的会话。
     *
     * 该检查只读取 access token 是否存在，不在启动阶段做网络请求；后续 token 有效性和刷新失败处理
     * 由 AuthRepository/core:network 统一完成，避免冷启动阻塞在用户中心接口上。
     */
    override suspend fun hasRestorableSession(): Boolean =
        !credentialStore.getAuthToken().isNullOrBlank()
}
