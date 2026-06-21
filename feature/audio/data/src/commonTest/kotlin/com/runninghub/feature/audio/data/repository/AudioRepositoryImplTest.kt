package com.runninghub.feature.audio.data.repository

import com.runninghub.feature.audio.data.remote.api.AudioApi
import com.runninghub.feature.audio.domain.AudioRequest
import com.runninghub.feature.audio.domain.AudioTaskIssueCode
import com.runninghub.feature.audio.domain.AudioTaskStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.encodedPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AudioRepositoryImplTest {

    @Test
    fun `convertTextToAudio maps network failure to stable task error`() = runBlocking {
        val repository = repositoryWithFailure("Unable to resolve host www.runninghub.cn")

        val statuses = repository.convertTextToAudio(sampleRequest()).toList()

        assertEquals(AudioTaskStatus.Submitting, statuses.first())
        assertEquals(AudioTaskStatus.Error(AudioTaskIssueCode.NETWORK_ERROR), statuses.last())
    }

    @Test
    fun `convertTextToAudio maps non network failure to stable task error`() = runBlocking {
        val repository = repositoryWithFailure("invalid audio request")

        val statuses = repository.convertTextToAudio(sampleRequest()).toList()

        assertEquals(AudioTaskStatus.Submitting, statuses.first())
        assertEquals(AudioTaskStatus.Error(AudioTaskIssueCode.UNKNOWN_ERROR), statuses.last())
    }

    @Test
    fun `convertTextToAudio maps submit failure server message to stable task error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd" to """
                {"taskId":"","status":"FAILED","errorMessage":"server raw submit failure"}
            """.trimIndent(),
        )

        val statuses = repository.convertTextToAudio(sampleRequest()).toList()

        assertEquals(AudioTaskStatus.Submitting, statuses.first())
        assertEquals(AudioTaskStatus.Error(AudioTaskIssueCode.SUBMIT_FAILED), statuses.last())
    }

    @Test
    fun `convertTextToAudio maps query failure server message to stable task error`() = runBlocking {
        val repository = repositoryWithResponses(
            "/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd" to """
                {"taskId":"audio-task-1","status":"SUBMITTED"}
            """.trimIndent(),
            "/openapi/v2/query" to """
                {"taskId":"audio-task-1","status":"FAILED","errorMessage":"server raw task failure"}
            """.trimIndent(),
        )

        val statuses = repository.convertTextToAudio(sampleRequest()).toList()

        assertEquals(AudioTaskStatus.Submitting, statuses.first())
        assertEquals(AudioTaskStatus.Running("audio-task-1"), statuses[1])
        assertEquals(AudioTaskStatus.Error(AudioTaskIssueCode.TASK_FAILED), statuses.last())
    }

    @Test
    fun `convertTextToAudio propagates cancellation without mapping it to task error`() = runBlocking {
        val repository = repositoryWithCancellation()

        assertFailsWith<CancellationException> {
            repository.convertTextToAudio(sampleRequest()).toList()
        }
        Unit
    }

    private fun repositoryWithFailure(message: String): AudioRepositoryImpl {
        val client = HttpClient(
            MockEngine {
                error(message)
            }
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return AudioRepositoryImpl(AudioApi(client))
    }

    private fun repositoryWithCancellation(): AudioRepositoryImpl {
        val client = HttpClient(
            MockEngine {
                throw CancellationException("collector cancelled")
            }
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return AudioRepositoryImpl(AudioApi(client))
    }

    private fun repositoryWithResponses(vararg responses: Pair<String, String>): AudioRepositoryImpl {
        val responseByPath = responses.toMap()
        val client = HttpClient(
            MockEngine { request ->
                val body = responseByPath[request.url.encodedPath]
                    ?: error("Unexpected path: ${request.url.encodedPath}")
                respond(
                    content = body,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        ) {
            install(ContentNegotiation) {
                json(json)
            }
        }
        return AudioRepositoryImpl(AudioApi(client))
    }

    private fun sampleRequest(): AudioRequest =
        AudioRequest(
            text = "hello",
            voiceId = "voice-1",
        )

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }
}
