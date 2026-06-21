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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
 * @param json 刷新响应使用的 JSON 解码器，默认忽略未知字段以兼容用户中心新增字段。
 */
class TokenRefresher(
    private val refreshClient: HttpClient,
    private val credentialStore: CredentialStore,
    private val refreshEndpoint: String = RunningHubApiEnvironment.TOKEN_REFRESH_URL,
    private val json: Json = REFRESH_TOKEN_JSON,
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
            val refreshTokenResponse = decodeRefreshTokenResponse(body)
                ?: return@withLock false
            val accessToken = refreshTokenResponse.accessToken.takeIf { it.isNotBlank() }
                ?: return@withLock false
            val tokenBeforeWrite = credentialStore.getAuthToken()
            if (tokenBeforeLock != null && tokenBeforeWrite.isNullOrEmpty()) {
                // refresh 请求飞行期间若 logout 已经清理 access token，不得再写回新 token，
                // 否则用户主动结束的会话会被并发 401 恢复成已登录状态。
                return@withLock false
            }
            if (tokenBeforeLock != null && tokenBeforeWrite != tokenBeforeLock) {
                // 其他路径已经建立了新会话或完成刷新时，保留现有凭据，避免旧 refresh 响应覆盖较新的登录结果。
                return@withLock true
            }
            credentialStore.setAuthToken(accessToken)
            refreshTokenResponse.refreshToken
                ?.takeIf { it.isNotBlank() }
                ?.let { credentialStore.setRefreshToken(it) }
            true
        }

    private fun decodeRefreshTokenResponse(body: String): RefreshTokenResponseDto? =
        try {
            // 令牌刷新响应必须按 JSON 语义解析，确保 unicode escape、字段顺序和未知字段都按协议处理；
            // 解析失败只让刷新返回 false，不记录响应体，避免 token 或服务端细节进入日志。
            val direct = json.decodeFromString<RefreshTokenResponseDto>(body)
            if (direct.accessToken.isNotBlank()) {
                direct
            } else {
                // 迁移期用户中心可能返回 code/msg/data 包装；只读取 data 中的凭据字段，
                // 不根据 msg 生成日志或错误，避免把认证接口细节暴露到客户端输出。
                json.decodeFromString<RefreshTokenEnvelopeDto>(body).data
            }
        } catch (_: Exception) {
            null
        }

    /**
     * 用户中心刷新令牌响应中的实际凭据数据。
     *
     * 该 DTO 只在网络层内部使用，字段值属于敏感凭据，解析后立即写入 [CredentialStore]；
     * 不得向 Presentation、日志或错误消息暴露。
     *
     * @property accessToken 新 access token，服务端字段名为 `access_token`。
     * 空字符串表示响应无可用访问令牌，调用方必须视为刷新失败。
     * @property refreshToken 新 refresh token，服务端字段名为 `refresh_token`。
     * `null` 表示服务端沿用旧 refresh token；空字符串同样不应覆盖本地旧值。
     */
    @Serializable
    private data class RefreshTokenResponseDto(
        @SerialName("access_token") val accessToken: String = "",
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    /**
     * 用户中心旧版刷新令牌响应 envelope。
     *
     * 旧接口把凭据放在 `data` 字段内，并携带 `code`、`msg` 等业务包装字段；网络层只关心
     * [data] 中的敏感凭据，其他字段通过 [Json.ignoreUnknownKeys] 忽略，避免刷新组件耦合 UI 文案。
     *
     * @property data 服务端 `data` 字段中的 token 数据。
     * `null` 表示响应没有提供可写回的凭据，调用方必须视为刷新失败；不应回退到旧 token。
     */
    @Serializable
    private data class RefreshTokenEnvelopeDto(
        val data: RefreshTokenResponseDto? = null,
    )

    private companion object {
        val REFRESH_TOKEN_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
