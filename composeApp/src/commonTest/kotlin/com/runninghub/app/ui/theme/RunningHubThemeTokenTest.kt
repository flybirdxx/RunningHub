package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RunningHubThemeTokenTest {
    @Test
    fun `brand accent stays muted for mobile dark UI`() {
        assertEquals(Color(0xFFA3B565), BrandLime)
    }

    @Test
    fun `dark surfaces keep stable app chrome palette`() {
        assertEquals(Color(0xFF000000), BaseBlack)
        assertEquals(Color(0xFF09090B), Surface850)
        assertEquals(Color(0xFF18181B), Surface800)
    }

    @Test
    fun `app chrome tokens use the shared RunningHub dark palette`() {
        assertEquals(BaseBlack, RhAppBackground)
        assertEquals(Surface850, RhAppSurface)
        assertEquals(Surface800, RhAppCard)
        assertEquals(Color(0xFF30363A), RhAppLine)
        assertEquals(TextPrimaryDark, RhAppText)
        assertEquals(TextMutedDark, RhAppMuted)
    }
}
