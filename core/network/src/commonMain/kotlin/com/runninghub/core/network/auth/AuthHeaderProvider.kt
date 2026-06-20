package com.runninghub.core.network.auth

import com.runninghub.core.storage.CredentialStore

/**
 * 为 RunningHub 请求生成认证相关请求头。
 *
 * 本类位于 core/network，是认证横切逻辑中不依赖 Ktor pipeline 的纯决策组件。
 * 它只负责从 [CredentialStore] 读取 access token 与 Cookie，并按调用方传入的已有请求头状态
 * 决定是否补充 Authorization/Cookie。具体把请求头写入 Ktor 请求的动作由拦截器完成。
 *
 * 安全约束：
 * - 读取到的凭据只返回给调用方写入请求头，不输出日志或异常。
 * - 如果请求已经显式设置 Authorization，则不再附加任何默认认证信息，避免覆盖特殊接口的认证方案。
 * - 如果请求只显式设置 Cookie，则仍可补充 Authorization，但不会覆盖已有 Cookie。
 *
 * @param credentialStore 跨平台敏感凭据存储边界。
 */
class AuthHeaderProvider(
    private val credentialStore: CredentialStore,
) {

    /**
     * 构建可追加到 RunningHub 请求上的认证请求头。
     *
     * @param hasAuthorizationHeader 当前请求是否已经显式设置 Authorization。
     * @param hasCookieHeader 当前请求是否已经显式设置 Cookie。
     * @return 可追加的认证头集合；字段为 null 表示该头不应写入请求。
     */
    suspend fun provideForRunningHubRequest(
        hasAuthorizationHeader: Boolean,
        hasCookieHeader: Boolean,
    ): RunningHubAuthHeaders {
        if (hasAuthorizationHeader) {
            return RunningHubAuthHeaders()
        }

        val authorization = credentialStore.getAuthToken()
            ?.takeIf { it.isNotBlank() }
            ?.let { "Bearer $it" }
        val cookie = if (hasCookieHeader) {
            null
        } else {
            credentialStore.getCookie()?.takeIf { it.isNotBlank() }
        }

        return RunningHubAuthHeaders(
            authorization = authorization,
            cookie = cookie,
        )
    }
}

/**
 * RunningHub 请求可追加的认证头。
 *
 * @property authorization Authorization 请求头完整值，例如 `Bearer xxx`。
 * 为空表示当前请求不应追加默认 access token。
 * @property cookie Cookie 请求头完整值。
 * 为空表示当前请求不应追加默认 Cookie，通常是本地没有 Cookie 或调用方已显式设置。
 */
data class RunningHubAuthHeaders(
    val authorization: String? = null,
    val cookie: String? = null,
)
