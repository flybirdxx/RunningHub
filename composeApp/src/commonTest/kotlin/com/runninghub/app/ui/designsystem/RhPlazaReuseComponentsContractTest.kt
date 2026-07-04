package com.runninghub.app.ui.designsystem

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
    fun `plaza work card exposes overlay author like and use same slots`() {
        val state = PlazaWorkCardState(
            id = "work-1",
            title = "电影感山间小屋",
            authorId = "owner-9",
            authorName = "Ada",
            authorAvatar = "https://example.com/avatar.png",
            likeCountLabel = "喜欢 12",
            aspectRatio = 0.8f,
            preview = PlazaWorkCardPreviewState(
                url = "https://example.com/work.png",
                type = PlazaWorkCardPreviewType.Image,
            ),
            useSameLabel = "用同款",
        )

        assertEquals("work-1", state.id)
        assertEquals("owner-9", state.authorId)
        assertEquals("Ada", state.authorName)
        assertEquals("https://example.com/avatar.png", state.authorAvatar)
        assertEquals("喜欢 12", state.likeCountLabel)
        assertEquals(0.8f, state.aspectRatio)
        assertEquals("用同款", state.useSameLabel)
        assertEquals(PlazaWorkCardPreviewType.Image, state.preview.type)
        assertTrue(state.enabled)
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
