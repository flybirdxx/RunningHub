package com.runninghub.feature.auth.data.repository

import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.feature.auth.domain.SmsError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * AuthRepositoryImpl 的短信验证码错误映射测试。
 *
 * 这些测试锁定 Data 层和用户中心接口之间的错误语义，避免 Presentation
 * 直接依赖 `CAPTCHA_VERIFY_ERROR` 这类服务端原始字符串。
 */
class AuthRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * 服务端要求图形验证时应返回稳定的 Domain 错误。
     *
     * 网页端当前会先完成 TAC 滑块验证并把 `validToken` 提交到 `sendSms`；
     * 移动端若缺少 token 会收到 `CAPTCHA_VERIFY_ERROR`，Data 层需要把它映射成
     * [SmsError.CaptchaRequired]，由登录页打开验证码容器后重试。
     */
    @Test
    fun `sendSmsCode maps captcha verify error to captcha required`() = runBlocking {
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"code":301,"msg":"CAPTCHA_VERIFY_ERROR","data":null}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = AuthRepositoryImpl(
            api = AuthApi(client),
            credentialStore = FakeCredentialStore(),
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = FakeCredentialStore(),
            ),
        )

        val result = repository.sendSmsCode("13800138000")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SmsError.CaptchaRequired)
    }

    private class FakeCredentialStore : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) = Unit
        override suspend fun clearApiKey() = Unit
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) = Unit
        override suspend fun clearEnterpriseApiKey() = Unit
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) = Unit
        override suspend fun clearCookie() = Unit
        override suspend fun getAuthToken(): String? = null
        override suspend fun setAuthToken(token: String) = Unit
        override suspend fun clearAuthToken() = Unit
        override suspend fun getRefreshToken(): String? = null
        override suspend fun setRefreshToken(token: String) = Unit
        override suspend fun clearRefreshToken() = Unit
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun clearAll() = Unit
    }
}
