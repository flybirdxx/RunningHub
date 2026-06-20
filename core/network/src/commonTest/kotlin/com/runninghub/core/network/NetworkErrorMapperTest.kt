package com.runninghub.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkErrorMapperTest {

    @Test
    fun `map classifies unknown host as connectivity failure`() {
        val error = IllegalStateException("Unable to resolve host www.runninghub.cn")

        assertEquals(NetworkErrorCategory.CONNECTIVITY, NetworkErrorMapper.map(error))
        assertTrue(NetworkErrorMapper.isNetworkError(error))
    }

    @Test
    fun `map classifies timeout before generic connection markers`() {
        val error = IllegalStateException("connect timeout after 30000 ms")

        assertEquals(NetworkErrorCategory.TIMEOUT, NetworkErrorMapper.map(error))
        assertTrue(NetworkErrorMapper.isNetworkError(error))
    }

    @Test
    fun `map walks cause chain when wrapper message is not useful`() {
        val error = RuntimeException(
            "request failed",
            IllegalStateException("Network is unreachable"),
        )

        assertEquals(NetworkErrorCategory.CONNECTIVITY, NetworkErrorMapper.map(error))
    }

    @Test
    fun `map leaves business exceptions unknown`() {
        val error = IllegalStateException("SMS_CODE_INVALID")

        assertEquals(NetworkErrorCategory.UNKNOWN, NetworkErrorMapper.map(error))
        assertFalse(NetworkErrorMapper.isNetworkError(error))
    }
}
