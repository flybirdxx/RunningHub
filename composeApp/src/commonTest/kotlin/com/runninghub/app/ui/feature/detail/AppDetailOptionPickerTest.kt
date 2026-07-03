package com.runninghub.app.ui.feature.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDetailOptionPickerTest {
    @Test
    fun `filter matches case-insensitively and blank query returns all`() {
        val options = listOf("Karras", "Euler A", "DPM++ 2M")
        assertEquals(options, filterPickerOptions(options, "  "))
        assertEquals(listOf("Karras"), filterPickerOptions(options, "kar"))
        assertEquals(listOf("Euler A"), filterPickerOptions(options, "euler"))
    }
}
