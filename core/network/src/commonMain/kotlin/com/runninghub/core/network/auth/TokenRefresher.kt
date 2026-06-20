package com.runninghub.core.network.auth

import com.runninghub.core.network.RunningHubApiEnvironment
import com.runninghub.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 负责刷新 RunningHub 访问令牌的网络组件。
 *
 * 本类位于 core/network，封装 401 后的 refresh token 请求、并发去重和新令牌写回。
 * 它不直接处理页面导航或会话失效状态；调用方根据返回值决定是否通知 SessionManager。
 *
 * 并发约束：
 * - 同一实例内部使用 [Mutex] 串行刷新，避免多个 401 请求同时刷新 token。
 * - 如果进入锁后发现 access token 已被其他协程更新，直接视为刷新成功。
 * - refresh 失败时只返回 false，不清理本地凭据，避免网络层执行不可逆会话操作。
 *
 * @param refreshClient 不带认证拦截器的 Ktor 客户端，避免 refresh 请求再次触发 401 刷新循环。
 * @param credentialStore access token 与 refresh token 的读取和写回边界。
 * @param refreshEndpoint 令牌刷新接口地址，测试可传入本地 MockEngine 可识别的地址。
 */
class TokenRefresher(
    private val refreshClient: HttpClient,
    private val credentialStore: CredentialStore,
    private val refreshEndpoint: String = RunningHubApiEnvironment.TOKEN_REFRESH_URL,
) {
    private val refreshMutex = Mutex()

    /**
     * 在收到 401 后尝试刷新访问令牌。
     *
     * @param tokenBeforeLock 401 发生时锁外读取到的旧 access token，用于判断其他协程是否已经刷新成功。
     * @return 刷新成功或发现其他协程已刷新时返回 true；缺少 refresh token、远端失败或响应无 token 时返回 false。
     */
    suspend fun refreshAfterUnauthorized(tokenBeforeLock: String?): Boolean =
        refreshMutex.withLock {
            // 锁内重新读取 token；若值已变化，说明并发请求已经完成刷新。
            val currentToken = credentialStore.getAuthToken()
            if (tokenBeforeLock != currentToken && !currentToken.isNullOrEmpty()) {
                return@withLock true
            }

            val refreshToken = credentialStore.getRefreshToken() ?: return@withLock false
            val refreshResponse = try {
                refreshClient.post(refreshEndpoint) {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $refreshToken")
                    setBody(emptyMap<String, String>())
                }
            } catch (_: Exception) {
                return@withLock false
            }

            if (refreshResponse.status != HttpStatusCode.OK) return@withLock false

            val body = refreshResponse.bodyAsText()
            val accessToken = ACCESS_TOKEN_REGEX.find(body)?.groupValues?.getOrNull(1)
                ?: return@withLock false
            credentialStore.setAuthToken(accessToken)
            REFRESH_TOKEN_REGEX.find(body)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { credentialStore.setRefreshToken(it) }
            true
        }

    private companion object {
        val ACCESS_TOKEN_REGEX = """"access_token"\s*:\s*"([^"]+)"""".toRegex()
        val REFRESH_TOKEN_REGEX = """"refresh_token"\s*:\s*"([^"]+)"""".toRegex()
    }
}
