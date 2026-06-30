package com.runninghub.feature.task.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TaskHistoryInvalidationBusTest {
    @Test
    fun `notifyTaskHistoryInvalidated emits one refresh signal`() = runTest {
        val bus = TaskHistoryInvalidationBus()
        val events: TaskHistoryInvalidationEvents = bus
        val notifier: TaskHistoryInvalidationNotifier = bus
        val received = async { events.invalidations.first() }
        runCurrent()

        notifier.notifyTaskHistoryInvalidated()

        assertEquals(Unit, received.await())
    }
}
