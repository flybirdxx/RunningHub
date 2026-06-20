package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.repository.AuthRepository
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.VideoGenerationRequest
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

class QuickCreateRepositoryImplFeePreviewTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `preview image quick creation fee uses v2 fee preview endpoint`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            respond(
                content = """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "passed": true,
                        "free": false,
                        "settlementMode": "cash_only",
                        "requiredRhAmount": 0,
                        "requiredCashAmount": 0.76,
                        "userCashBalance": 156.376,
                        "cashCurrency": "CNY"
                      }
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
        )

        val preview = repository.previewImageQuickCreationFee(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-g2",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "binding-1",
                quickCreationSkuId = "sku-1",
            )
        ).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_FEE_PREVIEW), paths)
        assertEquals(true, preview.passed)
        assertEquals(false, preview.free)
        assertEquals("cash_only", preview.settlementMode)
        assertEquals(0.0, preview.requiredRhAmount)
        assertEquals(0.76, preview.requiredCashAmount)
        assertEquals(156.376, preview.userCashBalance)
        assertEquals("CNY", preview.cashCurrency)
    }

    @Test
    fun `preview video quick creation fee uses v2 fee preview endpoint`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            respond(
                content = """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "passed": true,
                        "free": false,
                        "settlementMode": "cash_only",
                        "requiredRhAmount": 0,
                        "requiredCashAmount": 9.60,
                        "userCashBalance": 156.376,
                        "cashCurrency": "CNY"
                      }
                    }
                """.trimIndent(),
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
        )

        val preview = repository.previewVideoQuickCreationFee(
            VideoGenerationRequest(
                prompt = "green icon animation",
                model = "seedance-2",
                aspectRatio = "16:9",
                resolution = "720p",
                duration = 5,
                quickCreationCategoryId = "VIDEO",
                quickCreationBindingId = "video-binding-1",
                quickCreationSkuId = "video-sku-1",
            )
        ).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_FEE_PREVIEW), paths)
        assertEquals(true, preview.passed)
        assertEquals(false, preview.free)
        assertEquals("cash_only", preview.settlementMode)
        assertEquals(0.0, preview.requiredRhAmount)
        assertEquals(9.60, preview.requiredCashAmount)
        assertEquals(156.376, preview.userCashBalance)
        assertEquals("CNY", preview.cashCurrency)
    }

    @Test
    fun `preview image fee refreshes token and retries token invalid envelope`() = runBlocking {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            val content = if (paths.size == 1) {
                """{"code":412,"msg":"TOKEN_INVALID","data":null}"""
            } else {
                """
                    {
                      "code": 0,
                      "msg": "success",
                      "data": {
                        "passed": true,
                        "free": false,
                        "settlementMode": "cash_only",
                        "requiredRhAmount": 0,
                        "requiredCashAmount": 0.76,
                        "userCashBalance": 156.376,
                        "cashCurrency": "CNY"
                      }
                    }
                """.trimIndent()
            }
            respond(
                content = content,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        val authRepository = FakeAuthRepository()
        val repository = QuickCreateRepositoryImpl(
            quickCreateApi = QuickCreateApi(client, json),
            credentialStore = FakeSettingsRepository(),
            authRepository = authRepository,
        )

        val preview = repository.previewImageQuickCreationFee(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-g2",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
                quickCreationCategoryId = "IMAGE",
                quickCreationBindingId = "binding-1",
                quickCreationSkuId = "sku-1",
            )
        ).getOrThrow()

        assertEquals(listOf(QuickCreateApi.QC_FEE_PREVIEW, QuickCreateApi.QC_FEE_PREVIEW), paths)
        assertEquals(1, authRepository.refreshCalls)
        assertEquals(0.76, preview.requiredCashAmount)
    }

    private class FakeSettingsRepository : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) {}
        override suspend fun clearApiKey() {}
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) {}
        override suspend fun clearEnterpriseApiKey() {}
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) {}
        override suspend fun clearCookie() {}
        override suspend fun getAuthToken(): String? = null
        override suspend fun setAuthToken(token: String) {}
        override suspend fun clearAuthToken() {}
        override suspend fun getRefreshToken(): String? = null
        override suspend fun setRefreshToken(token: String) {}
        override suspend fun clearRefreshToken() {}
        override suspend fun isLoggedIn(): Boolean = false
        override suspend fun clearAll() {}
    }

    private class FakeAuthRepository : AuthRepository {
        var refreshCalls: Int = 0

        override suspend fun login(phone: String, password: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun sendSmsCode(phone: String): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun smsLogin(phone: String, code: String): Result<User> =
            Result.failure(NotImplementedError())

        override suspend fun logout() {}

        override suspend fun isLoggedIn(): Boolean = true

        override suspend fun refreshTokenIfNeeded(): Result<String> {
            refreshCalls += 1
            return Result.success("fresh-token")
        }

        override suspend fun getCurrentAuthToken(): String? = "fresh-token"

        override suspend fun getCurrentUserId(): String? = "user-1"
    }
}
