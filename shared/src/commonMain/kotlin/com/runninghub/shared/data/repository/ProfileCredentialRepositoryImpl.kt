package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.domain.repository.ProfileCredentialRepository

/**
 * 个人中心凭据仓库的数据层实现。
 *
 * 本类把个人中心的绑定/解绑操作转发到更底层的 [CredentialStore]，让 Presentation
 * 不再直接知道 API Key、Cookie 的具体存储边界。后续替换为平台安全存储时，只需要调整
 * [CredentialStore] 的实现或本类的组合方式。
 *
 * @param credentialStore 跨平台敏感凭据存储抽象，负责实际读写 API Key、企业 Key 和 Cookie。
 */
class ProfileCredentialRepositoryImpl(
    private val credentialStore: CredentialStore,
) : ProfileCredentialRepository {

    /**
     * 保存个人中心绑定的普通 API Key。
     *
     * 该方法不做格式校验，避免 Data 层复制 Presentation 的输入规则；调用方传入空白值时
     * 应在进入仓库前直接忽略。
     */
    override suspend fun bindApiKey(key: String) {
        credentialStore.setApiKey(key)
    }

    /**
     * 保存个人中心绑定的 Cookie。
     *
     * Cookie 可能包含多个键值和过期信息，本实现只负责原样持久化，后续请求头拼装由网络层处理。
     */
    override suspend fun bindCookie(cookie: String) {
        credentialStore.setCookie(cookie)
    }

    /**
     * 清理所有会影响创作接口调用的用户凭据。
     *
     * 企业 API Key 与普通 API Key 互斥但都能影响 OpenAPI 调用，因此解绑时需要一起清理；
     * Cookie 同时清理，避免旧 Web 会话继续被接口复用。
     */
    override suspend fun clearCreationCredentials() {
        credentialStore.clearApiKey()
        credentialStore.clearEnterpriseApiKey()
        credentialStore.clearCookie()
    }
}
