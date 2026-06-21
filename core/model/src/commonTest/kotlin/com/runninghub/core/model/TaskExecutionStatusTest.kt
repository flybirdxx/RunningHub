package com.runninghub.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskExecutionStatusTest {
    @Test
    fun `fromRaw maps known remote statuses`() {
        assertEquals(TaskExecutionStatus.Submitted, TaskExecutionStatus.fromRaw("SUBMITTED"))
        assertEquals(TaskExecutionStatus.Queued, TaskExecutionStatus.fromRaw("pending"))
        assertEquals(TaskExecutionStatus.Running, TaskExecutionStatus.fromRaw("PROCESSING"))
        assertEquals(TaskExecutionStatus.Success, TaskExecutionStatus.fromRaw("completed"))
        assertEquals(TaskExecutionStatus.Failed, TaskExecutionStatus.fromRaw("failure"))
        assertEquals(TaskExecutionStatus.Cancelled, TaskExecutionStatus.fromRaw("canceled"))
    }

    @Test
    fun `fromRaw keeps unknown status for diagnostics`() {
        val status = TaskExecutionStatus.fromRaw("waiting_for_gpu_capacity")

        assertEquals(TaskExecutionStatus.Unknown("waiting_for_gpu_capacity"), status)
        assertFalse(status.isTerminal())
    }

    @Test
    fun `status helpers classify terminal states`() {
        assertTrue(TaskExecutionStatus.Success.isTerminal())
        assertTrue(TaskExecutionStatus.Failed.isFailed())
        assertTrue(TaskExecutionStatus.Success.isSuccessful())
        assertFalse(TaskExecutionStatus.Running.isTerminal())
    }
}
