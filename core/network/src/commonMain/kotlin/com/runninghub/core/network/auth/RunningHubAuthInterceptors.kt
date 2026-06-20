package com.runninghub.core.network.auth

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * 为 RunningHub 主业务 HttpClient 安装认证相关拦截器。
 *
 * 该函数位于 core/network，是从 shared 组合根迁出的网络横切能力：请求阶段从
 * [CredentialStore] 读取 access token 与 Cookie，并只为 RunningHub 域名补充认证头；
 * 响应阶段在收到 401 时委托 [TokenRefresher] 执行刷新和并发去重，刷新失败后通过
 * [onSessionExpired] 通知上层会话状态。core/network 不直接依赖 Presentation 或 shared
 * 的 SessionManager，从而保持网络模块可复用、可测试。
 *
 * 安全约束：
 * - 读取到的 token 和 Cookie 只写入请求头，不得输出到日志或异常消息。
 * - 已由调用方显式设置的 Authorization/Cookie 不会被覆盖，避免特殊接口使用自己的认证方案时被污染。
 * - 本函数不清理本地凭据；会话失效后的清理策略由应用层或 Auth Repository 决定。
 *
 * @param credentialStore 敏感凭据读取边界，提供 access token 和 Cookie。
 * @param tokenRefresher 401 后的令牌刷新协调器，负责 refresh token 请求、并发去重和令牌写回。
 * @param onSessionExpired 刷新失败后的会话失效回调，通常由组合根连接到 SessionManager。
 */
fun HttpClient.installRunningHubAuthInterceptors(
    credentialStore: CredentialStore,
    tokenRefresher: TokenRefresher,
    onSessionExpired: suspend () -> Unit,
) {
    val authHeaderProvider = AuthHeaderProvider(credentialStore)

    requestPipeline.intercept(HttpRequestPipeline.State) {
        val url = context.url.buildString()
        if (url.contains(RunningHubApiEnvironment.HOST_MARKER)) {
            val authHeaders = authHeaderProvider.provideForRunningHubRequest(
                hasAuthorizationHeader = context.headers.contains(HttpHeaders.Authorization),
                hasCookieHeader = context.headers.contains(HttpHeaders.Cookie),
            )
            authHeaders.authorization?.let { context.headers.append(HttpHeaders.Authorization, it) }
            authHeaders.cookie?.let { context.headers.append(HttpHeaders.Cookie, it) }
        }
    }

    responsePipeline.intercept(HttpResponsePipeline.State) {
        val response = context.response
        if (response.status == HttpStatusCode.Unauthorized) {
            // 锁外读取旧 token，TokenRefresher 会在锁内再次读取并判断是否已有其他协程完成刷新。
            val tokenBeforeLock = credentialStore.getAuthToken()
            val refreshed = tokenRefresher.refreshAfterUnauthorized(tokenBeforeLock)
            if (!refreshed) {
                onSessionExpired()
            }
        }
    }
}
