package com.runninghub.feature.auth.data.repository

import com.runninghub.core.common.MissingCredential
import com.runninghub.core.common.MissingCredentialException
import com.runninghub.core.storage.CredentialStore
import com.runninghub.feature.auth.data.remote.api.AuthApi
import com.runninghub.feature.auth.domain.UserRepositoryException
import com.runninghub.feature.auth.domain.UserRepositoryIssue
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
import kotlin.test.assertIs
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
     * 缺少 API Key 时应返回稳定凭据错误，而不是最终中文 UI 文案。
     */
    @Test
    fun `getAccountStatus missing api key exposes stable credential issue`() = runBlocking {
        var networkCalls = 0
        val repository = UserRepositoryImpl(
            api = AuthApi(
                HttpClient(
                    MockEngine {
                        networkCalls += 1
                        error("Network should not be called without API Key")
                    },
                ) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                },
            ),
            credentialStore = FakeCredentialStore(),
        )

        val result = repository.getAccountStatus()

        assertTrue(result.isFailure)
        assertEquals(0, networkCalls)
        val error = assertIs<MissingCredentialException>(result.exceptionOrNull())
        assertEquals(MissingCredential.ApiKey, error.credential)
        assertEquals("MISSING_API_KEY", error.message)
    }

    /**
     * 用户资料业务失败时不得把远端 msg 写入异常消息。
     */
    @Test
    fun `getUserInfo failure does not expose remote msg as exception message`() = runBlocking {
        val remoteMessage = "REMOTE_USER_DETAIL_REASON"
        val repository = repositoryWithResponse(
            """{"code":455,"msg":"$remoteMessage","data":null}""",
        )

        val result = repository.getUserInfo(userId = "user-1")

        assertTrue(result.isFailure)
        val error = assertIs<UserRepositoryException>(result.exceptionOrNull())
        assertEquals(UserRepositoryIssue.UserInfoFailed, error.issue)
        assertEquals(455, error.remoteCode)
        assertTrue(error.message != remoteMessage)
    }

    @Test
    fun `getAccountStatus missing data maps to stable issue`() = runBlocking {
        val repository = repositoryWithResponse(
            content = """{"code":0,"msg":"success","data":null}""",
            apiKey = "local-api-key",
        )

        val result = repository.getAccountStatus()

        assertTrue(result.isFailure)
        val error = assertIs<UserRepositoryException>(result.exceptionOrNull())
        assertEquals(UserRepositoryIssue.AccountStatusMissing, error.issue)
        assertEquals(null, error.remoteCode)
    }

    @Test
    fun `getUserDetail missing data maps to stable issue`() = runBlocking {
        val repository = repositoryWithResponse("""{"code":0,"msg":"success","data":null}""")

        val result = repository.getUserDetail("user-1")

        assertTrue(result.isFailure)
        val error = assertIs<UserRepositoryException>(result.exceptionOrNull())
        assertEquals(UserRepositoryIssue.UserDetailMissing, error.issue)
        assertEquals(null, error.remoteCode)
    }

    @Test
    fun `follow operations map failed responses to stable issues`() = runBlocking {
        val failures = listOf(
            UserRepositoryIssue.FollowStatusFailed to suspend { repositoryWithResponse().isFollow("user-1") },
            UserRepositoryIssue.FollowUserFailed to suspend { repositoryWithResponse().followUser("user-1") },
            UserRepositoryIssue.UnfollowUserFailed to suspend { repositoryWithResponse().unFollowUser("user-1") },
        )

        failures.forEach { (issue, call) ->
            val result = call()
            assertTrue(result.isFailure)
            val error = assertIs<UserRepositoryException>(result.exceptionOrNull())
            assertEquals(issue, error.issue)
            assertEquals(409, error.remoteCode)
        }
    }

    private fun repositoryWithResponse(
        content: String = """{"code":409,"msg":"REMOTE_REASON","data":null}""",
        apiKey: String? = null,
    ): UserRepositoryImpl =
        UserRepositoryImpl(
            api = AuthApi(
                HttpClient(
                    MockEngine {
                        respond(
                            content = content,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    },
                ) {
                    install(ContentNegotiation) {
                        json(json)
                    }
                },
            ),
            credentialStore = FakeCredentialStore(apiKey = apiKey),
        )

    private class FakeCredentialStore(
        private val apiKey: String? = null,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = apiKey
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
