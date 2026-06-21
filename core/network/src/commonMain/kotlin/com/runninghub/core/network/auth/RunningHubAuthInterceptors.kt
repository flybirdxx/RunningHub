package com.runninghub.core.network.auth

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val sessionExpirationMutex = Mutex()
    var sessionExpirationNotified = false

    suspend fun notifySessionExpiredOnce() {
        sessionExpirationMutex.withLock {
            if (!sessionExpirationNotified) {
                sessionExpirationNotified = true
                onSessionExpired()
            }
        }
    }

    suspend fun HttpRequestBuilder.applyLatestStoredAuthHeaders() {
        headers.remove(HttpHeaders.Authorization)
        headers.remove(HttpHeaders.Cookie)
        val authHeaders = authHeaderProvider.provideForRunningHubRequest(
            hasAuthorizationHeader = false,
            hasCookieHeader = false,
        )
        authHeaders.authorization?.let { headers.append(HttpHeaders.Authorization, it) }
        authHeaders.cookie?.let { headers.append(HttpHeaders.Cookie, it) }
    }

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

    plugin(HttpSend).intercept { request ->
        val call = execute(request)
        val isRunningHubRequest = call.request.url.host.contains(RunningHubApiEnvironment.HOST_MARKER)
        if (!isRunningHubRequest || call.response.status != HttpStatusCode.Unauthorized) {
            return@intercept call
        }

        val tokenBeforeUnauthorized = request.headers[HttpHeaders.Authorization]
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotBlank() }

        // 第一阶段：只在收到 RunningHub 401 后刷新一次 token。TokenRefresher 内部用 Mutex
        // 合并并发 401，避免多请求同时命中 refresh endpoint。
        val refreshed = tokenRefresher.refreshAfterUnauthorized(tokenBeforeUnauthorized)
        if (!refreshed) {
            notifySessionExpiredOnce()
            return@intercept call
        }

        // 第二阶段：原请求 builder 在首次发送后仍携带旧 Authorization/Cookie。
        // HttpSend 重试不会重新经过 requestPipeline.State，因此这里必须手动替换为最新凭据。
        request.applyLatestStoredAuthHeaders()
        val retryCall = execute(request)
        if (retryCall.response.status == HttpStatusCode.Unauthorized) {
            // 第三阶段：刷新后最多只重试一次。若服务端仍返回 401，直接通知会话失效，
            // 防止在拦截器内部形成无限 refresh/retry 循环。
            notifySessionExpiredOnce()
        } else {
            sessionExpirationMutex.withLock {
                sessionExpirationNotified = false
            }
        }
        retryCall
    }
}
