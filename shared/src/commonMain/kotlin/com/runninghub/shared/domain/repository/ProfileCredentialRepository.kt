package com.runninghub.shared.domain.repository

/**
 * 个人中心用于绑定创作凭据的领域仓库契约。
 *
 * 该接口只表达“用户在个人中心绑定或清理 API Key / Cookie”的业务意图，
 * 不暴露底层凭据存储类型。Presentation 层通过它触发凭据变更，具体是否写入
 * DataStore、加密存储、Android Keystore 或 iOS Keychain 由 Data 层实现决定。
 */
interface ProfileCredentialRepository {
    /**
     * 绑定普通 API Key。
     *
     * @param key 用户从 RunningHub 后台复制的 API Key；调用方必须先过滤空白输入。
     * 实现层负责选择安全存储位置，不得把完整值写入日志或异常消息。
     */
    suspend fun bindApiKey(key: String)

    /**
     * 绑定会话 Cookie。
     *
     * @param cookie 用户手动粘贴的 RunningHub Cookie；该值属于敏感凭据，
     * 仅用于兼容仍依赖 Web 会话的接口调用，不得传回 Presentation 展示。
     */
    suspend fun bindCookie(cookie: String)

    /**
     * 清理个人中心可管理的创作凭据。
     *
     * 当前会同时清理普通 API Key、企业 API Key 和 Cookie，确保用户解绑后不会继续
     * 通过旧凭据访问创作、账户状态或 OpenAPI 能力。
     */
    suspend fun clearCreationCredentials()
}
