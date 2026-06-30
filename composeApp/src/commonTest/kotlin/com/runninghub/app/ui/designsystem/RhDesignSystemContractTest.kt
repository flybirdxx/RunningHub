package com.runninghub.app.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.components.badges.RhPriceBadgeState
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.components.buttons.RhButtonStyle
import com.runninghub.app.ui.designsystem.theme.RhDarkColors
import com.runninghub.app.ui.designsystem.theme.RhDefaultShapes
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTypography
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhDesignSystemContractTest {
    @Test
    fun `dark color tokens expose roadmap semantic palette`() {
        assertEquals(Color(0xFF050608), RhDarkColors.backgroundPrimary)
        assertEquals(Color(0xFF11151A), RhDarkColors.surfaceDefault)
        assertEquals(Color(0xFFB6FF00), RhDarkColors.brandPrimary)
        assertEquals(Color(0xFF4ADE80), RhDarkColors.statusSuccess)
        assertEquals(Color(0xFFB6FF00), RhDarkColors.priceCredit)
    }

    @Test
    fun `spacing and shape scales match roadmap primitives`() {
        assertEquals(4.dp, RhSpacing.xs)
        assertEquals(8.dp, RhSpacing.sm)
        assertEquals(12.dp, RhSpacing.md)
        assertEquals(16.dp, RhSpacing.lg)
        assertEquals(20.dp, RhSpacing.xl)
        assertEquals(24.dp, RhSpacing.xxl)
        assertEquals(32.dp, RhSpacing.xxxl)
        assertEquals(40.dp, RhSpacing.huge)

        assertEquals(4.dp, RhDefaultShapes.xs)
        assertEquals(8.dp, RhDefaultShapes.sm)
        assertEquals(12.dp, RhDefaultShapes.md)
        assertEquals(16.dp, RhDefaultShapes.lg)
        assertEquals(20.dp, RhDefaultShapes.xl)
        assertEquals(24.dp, RhDefaultShapes.sheet)
    }

    @Test
    fun `typography tokens keep zero letter spacing`() {
        val styles = listOf(
            RhTypography.display,
            RhTypography.pageTitle,
            RhTypography.sectionTitle,
            RhTypography.cardTitle,
            RhTypography.body,
            RhTypography.bodyStrong,
            RhTypography.caption,
            RhTypography.meta,
            RhTypography.button,
            RhTypography.price,
            RhTypography.statusBadge,
        )

        assertTrue(styles.all { it.letterSpacing.value == 0f })
    }

    @Test
    fun `component states are semantic and feature agnostic`() {
        assertEquals("primary", RhButtonStyle.Primary.tokenName)
        assertEquals("pending", RhPriceBadgeState.Pending.tokenName)
        assertEquals("amount", RhPriceBadgeState.Amount("37 RHB").tokenName)
        assertEquals("running", RhTaskStatus.Running.tokenName)
        assertEquals("failed", RhTaskStatus.Failed.tokenName)
    }
}
