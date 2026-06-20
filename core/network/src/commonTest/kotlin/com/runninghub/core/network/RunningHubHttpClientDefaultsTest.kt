package com.runninghub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunningHubHttpClientDefaultsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `main client defaults apply runninghub web compatibility headers`() = runBlocking {
        var userAgent: String? = null
        var accept: String? = null
        var origin: String? = null
        var referer: String? = null
        var language: String? = null
        val client = HttpClient(
            MockEngine { request ->
                userAgent = request.headers[HttpHeaders.UserAgent]
                accept = request.headers[HttpHeaders.Accept]
                origin = request.headers["Origin"]
                referer = request.headers[HttpHeaders.Referrer]
                language = request.headers["user-language"]
                respond(content = "{}", status = HttpStatusCode.OK)
            }
        ) {
            installRunningHubMainClientDefaults(json)
        }

        client.get("https://www.runninghub.cn/api/test")

        assertTrue(userAgent?.contains("Chrome") == true)
        assertEquals("application/json, text/plain, */*", accept)
        assertEquals("https://www.runninghub.cn", origin)
        assertEquals("https://www.runninghub.cn/", referer)
        assertEquals("zh_CN", language)
    }

    @Test
    fun `refresh client defaults do not apply main business headers`() = runBlocking {
        var userAgent: String? = null
        var origin: String? = null
        var language: String? = null
        val client = HttpClient(
            MockEngine { request ->
                userAgent = request.headers[HttpHeaders.UserAgent]
                origin = request.headers["Origin"]
                language = request.headers["user-language"]
                respond(content = "{}", status = HttpStatusCode.OK)
            }
        ) {
            installRunningHubRefreshClientDefaults(json)
        }

        client.get("https://www.runninghub.cn/uc/token/refresh")

        assertNull(userAgent)
        assertNull(origin)
        assertNull(language)
    }
}
