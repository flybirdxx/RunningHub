package com.runninghub.feature.auth.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.domain.ProfileCredentialRepository

/**
 * 个人中心凭据仓库的数据层实现。
 *
 * 本类把个人中心的绑定/解绑操作转发到更底层的 [CredentialStore]，让 Presentation
 * 不再直接知道 API Key、Cookie 的具体存储边界。后续替换为平台安全存储时，只需要调整
 * [CredentialStore] 的实现或本类的组合方式。
 */
class ProfileCredentialRepositoryImpl(
    private val credentialStore: CredentialStore,
) : ProfileCredentialRepository {

    /**
     * 绑定用户输入的 API Key。
     *
     * 当前实现委托给迁移期 [CredentialStore]；调用方不应缓存该值，后续迁移到平台安全存储时
     * 仍通过本仓库入口写入。
     */
    override suspend fun bindApiKey(key: String) {
        credentialStore.setApiKey(key)
    }

    /**
     * 绑定用户输入的 Cookie。
     *
     * Cookie 属于敏感凭据，只在 Data/storage 边界保存；Presentation 负责收集输入但不持久化。
     */
    override suspend fun bindCookie(cookie: String) {
        credentialStore.setCookie(cookie)
    }

    /**
     * 清理创作相关凭据。
     *
     * 该操作会同时删除普通 API Key、企业 API Key 和 Cookie，用于退出登录或用户主动解绑时
     * 防止旧凭据继续被 QuickCreate、任务提交等业务复用。
     */
    override suspend fun clearCreationCredentials() {
        credentialStore.clearApiKey()
        credentialStore.clearEnterpriseApiKey()
        credentialStore.clearCookie()
    }
}
