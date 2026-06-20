package com.runninghub.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RunningHubThemeTokenTest {
    @Test
    fun `brand lime matches captured web palette`() {
        assertEquals(Color(0xFFB6FF00), BrandLime)
    }

    @Test
    fun `dark surfaces match captured web palette`() {
        assertEquals(Color(0xFF000000), BaseBlack)
        assertEquals(Color(0xFF080808), Surface900)
        assertEquals(Color(0xFF09090B), Surface850)
        assertEquals(Color(0xFF18181B), Surface800)
        assertEquals(Color(0xFF27272A), Surface700)
    }

    @Test
    fun `app chrome tokens use the shared RunningHub dark palette`() {
        assertEquals(BaseBlack, RhAppBackground)
        assertEquals(Surface850, RhAppSurface)
        assertEquals(Surface800, RhAppCard)
        assertEquals(Color(0xFF202515), RhAppSelected)
        assertEquals(Color(0xFF30363A), RhAppLine)
        assertEquals(TextPrimaryDark, RhAppText)
        assertEquals(TextMutedDark, RhAppMuted)
        assertEquals(Color(0xF209090B), RhAppBottomBar)
    }
}
