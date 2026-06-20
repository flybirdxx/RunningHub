package com.runninghub.core.storage

/**
 * 跨平台凭据存储抽象。
 *
 * 该接口只负责访问认证和接口调用所需的敏感凭据，包括 API Key、Cookie、
 * access token 和 refresh token。业务偏好、余额缓存和草稿不应继续放入此接口，
 * 以便后续 Android 使用加密存储、iOS 使用 Keychain 时可以单独替换实现。
 *
 * 安全约束：
 * - 调用方不得把读取到的完整凭据写入日志、异常消息或 UI。
 * - commonMain 只依赖该抽象，不关心 Android/iOS 的具体安全存储方案。
 */
interface CredentialStore {
    /** 读取普通 API Key；未绑定或已清理时返回 null。 */
    suspend fun getApiKey(): String?

    /** 保存普通 API Key，具体实现负责选择安全存储位置。 */
    suspend fun setApiKey(key: String)

    /** 清理普通 API Key。 */
    suspend fun clearApiKey()

    /** 读取企业 API Key；未绑定或已清理时返回 null。 */
    suspend fun getEnterpriseApiKey(): String?

    /** 保存企业 API Key。 */
    suspend fun setEnterpriseApiKey(key: String)

    /** 清理企业 API Key。 */
    suspend fun clearEnterpriseApiKey()

    /** 读取会话 Cookie；调用方不得记录完整值。 */
    suspend fun getCookie(): String?

    /** 保存会话 Cookie。 */
    suspend fun setCookie(cookie: String)

    /** 清理会话 Cookie。 */
    suspend fun clearCookie()

    /** 读取 access token；调用方不得记录完整值。 */
    suspend fun getAuthToken(): String?

    /** 保存 access token。 */
    suspend fun setAuthToken(token: String)

    /** 清理 access token。 */
    suspend fun clearAuthToken()

    /** 读取 refresh token；调用方不得记录完整值。 */
    suspend fun getRefreshToken(): String?

    /** 保存 refresh token。 */
    suspend fun setRefreshToken(token: String)

    /** 清理 refresh token。 */
    suspend fun clearRefreshToken()

    /** 根据本地认证凭据是否存在判断会话是否可尝试恢复。 */
    suspend fun isLoggedIn(): Boolean

    /** 清理所有凭据；不得顺带删除非凭据类业务草稿，除非实现仍处于兼容迁移阶段。 */
    suspend fun clearAll()
}
