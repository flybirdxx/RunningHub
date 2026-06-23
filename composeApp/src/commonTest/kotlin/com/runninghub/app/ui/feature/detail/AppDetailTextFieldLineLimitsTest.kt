package com.runninghub.app.ui.feature.detail

import kotlin.test.Test
import kotlin.test.assertEquals

class AppDetailTextFieldLineLimitsTest {

    @Test
    fun `multiline text field shows bounded default rows`() {
        val limits = appDetailTextFieldLineLimits(multiline = true)

        assertEquals(4, limits.minLines)
        assertEquals(6, limits.maxLines)
    }

    @Test
    fun `single line text field uses one visible row`() {
        val limits = appDetailTextFieldLineLimits(multiline = false)

        assertEquals(1, limits.minLines)
        assertEquals(1, limits.maxLines)
    }
}
