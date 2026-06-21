package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.AccountStatusRequest
import com.runninghub.shared.data.remote.dto.PwdLoginRequest
import com.runninghub.shared.data.remote.dto.SmsCodeRequest
import com.runninghub.shared.data.remote.dto.SmsLoginRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunningHubApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun `runninghub api calls expected endpoint paths`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = RunningHubApi(clientWithPathCapture(captured))

        api.pwdLogin(PwdLoginRequest(mobile = "13800000000", password = "password"))
        api.tokenRefresh("refresh-token")
        api.sendSmsCode(SmsCodeRequest(mobile = "13800000000", token = "captcha-token"))
        api.smsLogin(
            SmsLoginRequest(
                mobile = "13800000000",
                code = "123456",
                channel = "utm-source",
                inviteCode = "invite-code",
            ),
        )
        api.logout("access-token")
        api.getUserInfoWithToken(accessToken = "access-token", userId = "user-1")
        api.getAccountStatus(AccountStatusRequest(apikey = "api-key"))
        api.getUserInfo()
        api.getUserDetail(
            referer = "https://www.runninghub.cn/user/user-1",
            params = mapOf("userId" to "user-1"),
        )
        api.isFollow(
            referer = "https://www.runninghub.cn/user/user-1",
            params = mapOf("followId" to "user-1"),
        )
        api.followUser(
            referer = "https://www.runninghub.cn/user/user-1",
            params = mapOf("followId" to "user-1"),
        )
        api.unFollowUser(
            referer = "https://www.runninghub.cn/user/user-1",
            params = mapOf("followId" to "user-1"),
        )

        assertEquals(
            listOf(
                HttpMethod.Post to "/uc/pwdLogin",
                HttpMethod.Post to "/uc/token/refresh",
                HttpMethod.Post to "/uc/sendSms",
                HttpMethod.Post to "/uc/smsLogin",
                HttpMethod.Post to "/uc/logout",
                HttpMethod.Post to "/uc/getUserInfo",
                HttpMethod.Post to "/uc/openapi/accountStatus",
                HttpMethod.Post to "/uc/getUserInfo",
                HttpMethod.Post to "/uc/getUserInfo",
                HttpMethod.Post to "/uc/follow/isFollow",
                HttpMethod.Post to "/uc/follow/followUser",
                HttpMethod.Post to "/uc/follow/unFollowUser",
            ),
            captured.map { it.method to it.path },
        )
        assertTrue(captured[2].body.contains(""""token":"captcha-token""""))
        assertTrue(captured[3].body.contains(""""serviceAgreement":true"""))
        assertTrue(captured[3].body.contains(""""channel":"utm-source""""))
        assertTrue(captured[3].body.contains(""""inviteCode":"invite-code""""))
        assertTrue(captured[3].body.contains(""""rememberMe":false"""))
    }

    @Test
    fun `runninghub api preserves authentication and referer headers`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = RunningHubApi(clientWithPathCapture(captured))

        api.tokenRefresh("refresh-token")
        api.logout("access-token")
        api.getUserInfoWithToken(accessToken = "access-token", userId = "user-1")
        api.getUserDetail(
            referer = "https://www.runninghub.cn/user/user-1",
            params = mapOf("userId" to "user-1"),
        )

        assertEquals("Bearer refresh-token", captured[0].authorization)
        assertEquals("Bearer access-token", captured[1].authorization)
        assertEquals("Bearer access-token", captured[2].authorization)
        assertEquals("https://www.runninghub.cn/user/user-1", captured[3].referer)
    }

    private fun clientWithPathCapture(
        captured: MutableList<CapturedRequest>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += CapturedRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    body = request.body.toRequestBodyText(),
                    authorization = request.headers[HttpHeaders.Authorization],
                    referer = request.headers[HttpHeaders.Referrer],
                )
                respond(
                    content = """{"code":0,"msg":"success","data":null}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }

    private data class CapturedRequest(
        val method: HttpMethod,
        val path: String,
        val body: String,
        val authorization: String?,
        val referer: String?,
    )

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
        }
}
