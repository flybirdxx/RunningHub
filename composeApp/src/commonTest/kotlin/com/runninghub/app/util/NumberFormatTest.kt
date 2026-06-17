package com.runninghub.app.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    @Test
    fun `formatCashAmount keeps cents for quick creation billing`() {
        assertEquals("0.76", formatCashAmount(0.76))
        assertEquals("9.60", formatCashAmount(9.6))
        assertEquals("1.00", formatCashAmount(1.0))
    }
}
