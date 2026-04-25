package com.runninghub.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AppResultTest {

    @Test
    fun `Success result contains data`() {
        val result = AppResult.Success("hello")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
        assertEquals("hello", result.getOrNull())
        assertNull(result.errorMessageOrNull())
    }

    @Test
    fun `Error result contains message`() {
        val result = AppResult.Error("something went wrong")
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertNull(result.getOrNull())
        assertEquals("something went wrong", result.errorMessageOrNull())
    }

    @Test
    fun `Loading result has correct state`() {
        val result = AppResult.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `map transforms Success data`() {
        val result = AppResult.Success(42)
        val mapped = result.map { it.toString() }
        assertTrue(mapped.isSuccess)
        assertEquals("42", mapped.getOrNull())
    }

    @Test
    fun `map passes through Error`() {
        val result: AppResult<Int> = AppResult.Error("fail")
        val mapped = result.map { it.toString() }
        assertTrue(mapped.isError)
        assertEquals("fail", mapped.errorMessageOrNull())
    }

    @Test
    fun `map passes through Loading`() {
        val result: AppResult<Int> = AppResult.Loading
        val mapped = result.map { it.toString() }
        assertTrue(mapped.isLoading)
    }
}
