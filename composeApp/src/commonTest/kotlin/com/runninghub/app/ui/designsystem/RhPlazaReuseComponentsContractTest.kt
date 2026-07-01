package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardAction
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardPreviewType
import com.runninghub.app.ui.designsystem.components.cards.PlazaWorkCardState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterState
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateParameterStatus
import com.runninghub.app.ui.designsystem.components.sheets.ReuseTemplateSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RhPlazaReuseComponentsContractTest {
    @Test
    fun `plaza work card exposes use same action and source summary`() {
        val state = PlazaWorkCardState(
            id = "work-1",
            title = "电影感山间小屋",
            authorName = "Ada",
            preview = PlazaWorkCardPreviewState(
                url = "https://example.com/work.png",
                type = PlazaWorkCardPreviewType.Image,
            ),
            metric = PlazaWorkCardMetricState(label = "使用", value = "12"),
            primaryAction = PlazaWorkCardAction.UseSame,
            sourceProtected = true,
        )

        assertEquals("work-1", state.id)
        assertEquals("Ada", state.authorName)
        assertEquals(PlazaWorkCardPreviewType.Image, state.preview.type)
        assertEquals(PlazaWorkCardAction.UseSame, state.primaryAction)
        assertTrue(state.sourceProtected)
    }

    @Test
    fun `reuse template sheet carries only available parameters before create`() {
        val state = ReuseTemplateSheetState(
            title = "使用同款",
            sourceTitle = "电影感山间小屋",
            sourceAuthor = "Ada",
            sourceProtectionText = "保留原作者来源",
            parameters = listOf(
                ReuseTemplateParameterState(
                    label = "Prompt",
                    value = "cinematic mountain house",
                    status = ReuseTemplateParameterStatus.Available,
                ),
            ),
            confirmActionLabel = "带入创作页",
            dismissActionLabel = "取消",
        )

        assertEquals("Ada", state.sourceAuthor)
        assertTrue(state.parameters.any { it.status == ReuseTemplateParameterStatus.Available })
        assertFalse(state.parameters.any { it.status == ReuseTemplateParameterStatus.Missing })
        assertFalse(state.confirmBypassesPriceConfirmation)
    }
}
