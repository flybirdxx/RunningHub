package com.runninghub.app.ui.feature.detail

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDetailDescriptionPresentationTest {

    @Test
    fun `long description collapses to bounded rows by default`() {
        val description = "这是一段很长的简介，用于验证详情页不会被简介内容撑开。".repeat(12)
        val presentation = appDetailDescriptionPresentation(description)

        assertTrue(presentation.isCollapsible)
        assertEquals(6, presentation.collapsedMaxLines)
        assertEquals(AppDetailDescriptionToggleAffordance.BottomBorderTriangle, presentation.toggleAffordance)
        assertEquals(AppDetailDescriptionDepthEffect.BottomGradientFade, presentation.collapsedDepthEffect)
        assertEquals(18.dp, presentation.toggleIconSize)
        assertEquals(0.dp, presentation.collapsedBottomPadding)
        assertEquals(24.dp, presentation.collapsedDepthFadeHeight)
    }

    @Test
    fun `short description remains fully visible`() {
        val presentation = appDetailDescriptionPresentation("短简介")

        assertFalse(presentation.isCollapsible)
    }

    @Test
    fun `html tags are removed before display`() {
        val presentation = appDetailDescriptionPresentation("<p>简介内容</p>")

        assertEquals("简介内容", presentation.cleanedDescription)
    }
}
