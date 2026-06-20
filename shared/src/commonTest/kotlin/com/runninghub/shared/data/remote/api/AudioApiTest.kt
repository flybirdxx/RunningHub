package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.MiniMaxAudioRequestDto
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

class AudioApiTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `textToAudio posts to openapi v2 audio endpoint`() = runBlocking {
        val captured = mutableListOf<Pair<HttpMethod, String>>()
        val api = AudioApi(clientWithPathCapture(captured))

        val response = api.textToAudio(
            MiniMaxAudioRequestDto(
                text = "hello",
                voiceId = "voice-1",
            ),
        )

        assertEquals("task-audio", response.taskId)
        assertEquals(
            listOf(HttpMethod.Post to "/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd"),
            captured,
        )
    }

    @Test
    fun `queryTask gets openapi v2 query endpoint with task id`() = runBlocking {
        var capturedTaskId: String? = null
        val captured = mutableListOf<Pair<HttpMethod, String>>()
        val api = AudioApi(
            clientWithPathCapture(captured) { requestUrl ->
                capturedTaskId = requestUrl.parameters["taskId"]
            },
        )

        val response = api.queryTask("task-audio")

        assertEquals("task-audio", response.taskId)
        assertEquals("task-audio", capturedTaskId)
        assertEquals(listOf(HttpMethod.Get to "/openapi/v2/query"), captured)
    }

    private fun clientWithPathCapture(
        captured: MutableList<Pair<HttpMethod, String>>,
        onRequest: (io.ktor.http.Url) -> Unit = {},
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                captured += request.method to request.url.encodedPath
                onRequest(request.url)
                respond(
                    content = """{"taskId":"task-audio","status":"SUBMITTED"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
}
