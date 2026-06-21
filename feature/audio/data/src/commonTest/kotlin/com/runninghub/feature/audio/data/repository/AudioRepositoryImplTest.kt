package com.runninghub.feature.audio.data.repository

import com.runninghub.feature.audio.data.remote.api.AudioApi
import com.runninghub.feature.audio.domain.AudioRequest
import com.runninghub.feature.audio.domain.AudioTaskStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
        assertEquals(AudioTaskStatus.Error("Network error occurred"), statuses.last())
    }

    @Test
    fun `convertTextToAudio keeps non network failure message`() = runBlocking {
        val repository = repositoryWithFailure("invalid audio request")

        val statuses = repository.convertTextToAudio(sampleRequest()).toList()

        assertEquals(AudioTaskStatus.Submitting, statuses.first())
        assertEquals(AudioTaskStatus.Error("invalid audio request"), statuses.last())
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
