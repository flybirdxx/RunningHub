package com.runninghub.feature.auth.data.remote.api

import com.runninghub.feature.auth.data.remote.dto.SmsCodeRequestDto
import com.runninghub.feature.auth.data.remote.dto.SmsLoginRequestDto
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

/**
 * Auth API 路径与请求体契约测试。
 *
 * 用户中心登录接口由网页端抓包校准；Data 层必须固定 endpoint 和关键字段，
 * 避免 Presentation 直接依赖远端请求细节。
 */
class AuthApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * 短信验证码接口应携带滑块校验 token。
     *
     * 网页端在启用短信滑块验证码时会把 `validToken` 写入 `token` 字段；
     * 移动端暂无验证码能力时允许该字段为 null 并由序列化配置省略。
     */
    @Test
    fun `send sms posts captured endpoint and captcha token`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = AuthApi(clientWithCapture(captured))

        api.sendSmsCode(SmsCodeRequestDto(mobile = "13800000000", token = "captcha-token"))

        assertEquals(HttpMethod.Post, captured.single().method)
        assertEquals("/uc/sendSms", captured.single().path)
        assertTrue(captured.single().body.contains(""""mobile":"13800000000""""))
        assertTrue(captured.single().body.contains(""""token":"captcha-token""""))
    }

    /**
     * 短信登录接口应保持网页端请求体字段。
     *
     * `channel` 和 `inviteCode` 来源于推广归因；移动端没有归因值时可为空并省略，
     * 但 DTO 需要保留字段，便于后续接入同一套归因链路。
     */
    @Test
    fun `sms login posts captured endpoint and web payload fields`() = runBlocking {
        val captured = mutableListOf<CapturedRequest>()
        val api = AuthApi(clientWithCapture(captured))

        api.smsLogin(
            SmsLoginRequestDto(
                mobile = "13800000000",
                code = "123456",
                channel = "utm-source",
                inviteCode = "invite-code",
            ),
        )

        assertEquals(HttpMethod.Post, captured.single().method)
        assertEquals("/uc/smsLogin", captured.single().path)
        assertTrue(captured.single().body.contains(""""mobile":"13800000000""""))
        assertTrue(captured.single().body.contains(""""code":"123456""""))
        assertTrue(captured.single().body.contains(""""serviceAgreement":true"""))
        assertTrue(captured.single().body.contains(""""channel":"utm-source""""))
        assertTrue(captured.single().body.contains(""""inviteCode":"invite-code""""))
        assertTrue(captured.single().body.contains(""""rememberMe":false"""))
    }

    private fun clientWithCapture(
        captured: MutableList<CapturedRequest>,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += CapturedRequest(
                    method = request.method,
                    path = request.url.encodedPath,
                    body = request.body.toRequestBodyText(),
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

    private fun Any.toRequestBodyText(): String =
        when (this) {
            is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            is io.ktor.http.content.TextContent -> text
            else -> toString()
        }

    private data class CapturedRequest(
        val method: HttpMethod,
        val path: String,
        val body: String,
    )
}
