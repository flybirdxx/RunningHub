package com.runninghub.app.ui.feature.detail

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue

class AppDetailLayoutMetricsTest {
    @Test
    fun `content bottom padding stays small because run action is inline`() {
        assertTrue(AppDetailContentBottomPadding <= 32.dp)
    }
}
