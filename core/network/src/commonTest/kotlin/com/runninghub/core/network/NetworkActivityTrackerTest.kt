package com.runninghub.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkActivityTrackerTest {
    @Test
    fun `network activity tracker counts successful request lifecycle`() = runBlocking {
        val tracker = CountingNetworkActivityTracker()
        val client = HttpClient(
            MockEngine {
                respond(content = "{}", status = HttpStatusCode.OK)
            }
        ).also { client ->
            client.installRunningHubNetworkActivityTracking(tracker)
        }

        client.get("https://www.runninghub.cn/api/test")

        assertEquals(
            NetworkActivitySnapshot(
                startedCount = 1,
                completedCount = 1,
                inFlightCount = 0,
            ),
            tracker.snapshots.value,
        )
    }

    @Test
    fun `network activity tracker clears in flight count when request throws`() = runBlocking {
        val tracker = CountingNetworkActivityTracker()
        val client = HttpClient(
            MockEngine {
                throw IllegalStateException("network failed")
            }
        ).also { client ->
            client.installRunningHubNetworkActivityTracking(tracker)
        }

        assertFailsWith<IllegalStateException> {
            client.get("https://www.runninghub.cn/api/test")
        }

        assertEquals(
            NetworkActivitySnapshot(
                startedCount = 1,
                completedCount = 1,
                inFlightCount = 0,
            ),
            tracker.snapshots.value,
        )
    }
}
