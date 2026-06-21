package com.runninghub.feature.auth.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
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
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 用户资料 Data Repository 的错误收口测试。
 *
 * 复核要求 Data 层不能把服务端 `msg` 直接作为最终 UI 文案；这些测试锁定迁移后
 * `feature:auth:data` 的失败路径只暴露稳定错误码。
 */
class UserRepositoryImplTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * 用户资料业务失败时不得把远端 msg 写入异常消息。
     */
    @Test
    fun `getUserInfo failure does not expose remote msg as exception message`() = runBlocking {
        val remoteMessage = "REMOTE_USER_DETAIL_REASON"
        val repository = UserRepositoryImpl(
            api = AuthApi(
                HttpClient(
                    MockEngine {
                        respond(
                            content = """{"code":455,"msg":"$remoteMessage","data":null}""",
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                },
            ),
            credentialStore = FakeCredentialStore(),
        )

        val result = repository.getUserInfo(userId = "user-1")

        val error = result.exceptionOrNull()
        assertTrue(result.isFailure)
        val message = assertNotNull(error).message
        assertEquals("USER_INFO_FAILED_CODE_455", message)
        assertTrue(message != remoteMessage)
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
