package com.runninghub.core.network

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val MAIN_REQUEST_TIMEOUT_MILLIS = 120_000L
private const val MAIN_CONNECT_TIMEOUT_MILLIS = 15_000L
private const val MAIN_SOCKET_TIMEOUT_MILLIS = 30_000L
private const val REFRESH_REQUEST_TIMEOUT_MILLIS = 30_000L
private const val REFRESH_CONNECT_TIMEOUT_MILLIS = 15_000L
private const val RUNNINGHUB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/144.0.0.0 Mobile Safari/537.36"

/**
 * 安装 RunningHub 主业务客户端的默认网络配置。
 *
 * 该函数属于 core/network 横切配置边界，集中维护 JSON 解析、请求超时和 RunningHub Web 端兼容请求头。
 * shared 的组合根只负责调用本函数并连接认证拦截器，避免 DI 模块继续承载具体 Ktor 策略。
 *
 * @param json 跨模块共享的 JSON 配置；调用方负责决定兼容严格度和默认值策略。
 */
fun HttpClientConfig<*>.installRunningHubMainClientDefaults(json: Json) {
    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {
        requestTimeoutMillis = MAIN_REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = MAIN_CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = MAIN_SOCKET_TIMEOUT_MILLIS
    }

    defaultRequest {
        // 这些请求头是 RunningHub Web API 当前兼容所需的基础上下文，不包含任何敏感凭据。
        header("User-Agent", RUNNINGHUB_USER_AGENT)
        header("Accept", "application/json, text/plain, */*")
        header("Origin", RunningHubApiEnvironment.WEB_ORIGIN)
        header("Referer", RunningHubApiEnvironment.WEB_BASE_URL)
        header("user-language", "zh_CN")
    }
}

/**
 * 安装 refresh token 专用客户端的默认网络配置。
 *
 * refresh 客户端不能安装认证拦截器，否则刷新请求收到 401 时会再次触发刷新，形成无限循环。
 * 因此它只配置 JSON 与较短超时，具体 refresh token 请求由 [com.runninghub.core.network.auth.TokenRefresher]
 * 明确写入 Authorization header。
 *
 * @param json 跨模块共享的 JSON 配置；保持与主业务客户端一致，避免 refresh 响应解析规则漂移。
 */
fun HttpClientConfig<*>.installRunningHubRefreshClientDefaults(json: Json) {
    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {
        requestTimeoutMillis = REFRESH_REQUEST_TIMEOUT_MILLIS
        connectTimeoutMillis = REFRESH_CONNECT_TIMEOUT_MILLIS
    }
}
