package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.domain.session.SessionRestoreRepository

/**
 * 会话恢复仓库的数据层实现。
 *
 * 本类只通过 [CredentialStore] 判断本地是否存在 access token，不把 Token 内容暴露给
 * Presentation。令牌是否过期、是否需要刷新仍由认证仓库和 core/network 拦截器统一处理。
 *
 * @param credentialStore 本地敏感凭据存储边界。
 */
class SessionRestoreRepositoryImpl(
    private val credentialStore: CredentialStore,
) : SessionRestoreRepository {

    /**
     * 判断本地 access token 是否足以让应用启动进入主页面。
     *
     * 空字符串和 null 都视为不可恢复；具体 token 有效性由后续请求或刷新流程确认。
     */
    override suspend fun hasRestorableSession(): Boolean =
        !credentialStore.getAuthToken().isNullOrBlank()
}
