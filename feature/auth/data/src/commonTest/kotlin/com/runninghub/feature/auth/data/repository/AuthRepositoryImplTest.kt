package com.runninghub.feature.auth.data.repository

import com.runninghub.core.network.auth.TokenRefresher
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.domain.AuthError
import com.runninghub.feature.auth.domain.SessionManager
import com.runninghub.feature.auth.domain.SmsError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    /**
     * 密码登录业务失败时不得把服务端 msg 作为异常消息透出。
     *
     * 登录页会把未知异常映射到 Snackbar；如果 Data 层直接抛出远端 msg，
     * 内部错误码、诊断文本或非中文内容就会成为最终 UI 文案。
     */
    @Test
    fun `login failure does not expose remote msg as exception message`() = runBlocking {
        val remoteMessage = "REMOTE_INTERNAL_AUTH_REASON"
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"code":499,"msg":"$remoteMessage","data":null}""",
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val credentialStore = FakeCredentialStore()
        val repository = AuthRepositoryImpl(
            api = AuthApi(client),
            credentialStore = credentialStore,
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = credentialStore,
            ),
        )

        val result = repository.login("13800138000", "password")

        val error = assertIs<AuthError.Unknown>(result.exceptionOrNull())
        assertEquals("AUTH_FAILED_CODE_499", error.serverMessage)
        assertTrue(error.message != remoteMessage)
    }

    @Test
    fun `login maps empty token data to stable auth error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/uc/pwdLogin" to """{"code":0,"msg":"success","data":null}""",
        )

        val result = repository.login("13800138000", "password")

        assertTrue(result.isFailure)
        val error = assertIs<AuthError.EmptyLoginResponse>(result.exceptionOrNull())
        assertEquals("EMPTY_LOGIN_RESPONSE", error.code)
    }

    @Test
    fun `login maps missing access token to stable auth error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/uc/pwdLogin" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "access_token": "",
                    "refresh_token": "refresh-token"
                  }
                }
            """.trimIndent(),
        )

        val result = repository.login("13800138000", "password")

        assertTrue(result.isFailure)
        val error = assertIs<AuthError.MissingAccessToken>(result.exceptionOrNull())
        assertEquals("MISSING_ACCESS_TOKEN", error.code)
    }

    @Test
    fun `smsLogin maps missing access token to stable auth error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/uc/smsLogin" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "access_token": "",
                    "refresh_token": "refresh-token"
                  }
                }
            """.trimIndent(),
        )

        val result = repository.smsLogin("13800138000", "123456")

        assertTrue(result.isFailure)
        val error = assertIs<AuthError.MissingAccessToken>(result.exceptionOrNull())
        assertEquals("MISSING_ACCESS_TOKEN", error.code)
    }

    @Test
    fun `login maps empty user response to stable auth error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/uc/pwdLogin" to """
                {
                  "code": 0,
                  "msg": "success",
                  "data": {
                    "access_token": "access-token",
                    "refresh_token": "refresh-token"
                  }
                }
            """.trimIndent(),
            "/uc/getUserInfo" to """{"code":0,"msg":"success","data":null}""",
        )

        val result = repository.login("13800138000", "password")

        assertTrue(result.isFailure)
        val error = assertIs<AuthError.EmptyUserResponse>(result.exceptionOrNull())
        assertEquals("EMPTY_USER_RESPONSE", error.code)
    }

    @Test
    fun `refreshTokenIfNeeded maps refresh failure to stable auth error`() = runBlocking {
        val credentialStore = FakeCredentialStore(authToken = null, refreshToken = null)
        val repository = AuthRepositoryImpl(
            api = AuthApi(mockClient(emptyMap())),
            credentialStore = credentialStore,
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = mockClient(emptyMap()),
                credentialStore = credentialStore,
            ),
        )

        val result = repository.refreshTokenIfNeeded()

        assertTrue(result.isFailure)
        val error = assertIs<AuthError.TokenRefreshFailed>(result.exceptionOrNull())
        assertEquals("TOKEN_REFRESH_FAILED", error.code)
    }

    @Test
    fun `getCurrentUserId decodes escaped JWT subject with JSON semantics`() = runBlocking {
        val credentialStore = FakeCredentialStore(
            authToken = jwtWithPayload("""{"sub":"user\u002D123","exp":4102444800}"""),
        )
        val client = HttpClient(
            MockEngine {
                respond(
                    content = """{"code":0,"msg":"ok","data":null}""",
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
            credentialStore = credentialStore,
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = credentialStore,
            ),
        )

        assertEquals("user-123", repository.getCurrentUserId())
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun jwtWithPayload(payload: String): String {
        fun encode(value: String): String =
            kotlin.io.encoding.Base64.UrlSafe
                .encode(value.encodeToByteArray())
                .trimEnd('=')

        return listOf(
            encode("""{"alg":"none"}"""),
            encode(payload),
            "signature",
        ).joinToString(".")
    }

    private fun repositoryWithResponses(vararg responses: Pair<String, String>): AuthRepositoryImpl {
        val client = mockClient(responses.toMap())
        val credentialStore = FakeCredentialStore()
        return AuthRepositoryImpl(
            api = AuthApi(client),
            credentialStore = credentialStore,
            sessionManager = SessionManager(),
            tokenRefresher = TokenRefresher(
                refreshClient = client,
                credentialStore = credentialStore,
            ),
        )
    }

    private fun mockClient(responses: Map<String, String>): HttpClient =
        HttpClient(
            MockEngine { request ->
                respond(
                    content = responses.getValue(request.url.encodedPath),
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    private class FakeCredentialStore(
        var authToken: String? = null,
        var refreshToken: String? = null,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) = Unit
        override suspend fun clearApiKey() = Unit
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) = Unit
        override suspend fun clearEnterpriseApiKey() = Unit
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) = Unit
        override suspend fun clearCookie() = Unit
        override suspend fun getAuthToken(): String? = authToken
        override suspend fun setAuthToken(token: String) {
            authToken = token
        }
        override suspend fun clearAuthToken() {
            authToken = null
        }
        override suspend fun getRefreshToken(): String? = refreshToken
        override suspend fun setRefreshToken(token: String) {
            refreshToken = token
        }
        override suspend fun clearRefreshToken() {
            refreshToken = null
        }
        override suspend fun isLoggedIn(): Boolean = !authToken.isNullOrEmpty()
        override suspend fun clearAll() {
            authToken = null
            refreshToken = null
        }
    }
}
