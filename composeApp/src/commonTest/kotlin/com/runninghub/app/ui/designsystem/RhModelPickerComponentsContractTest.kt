package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.cards.ModelCardState
import com.runninghub.app.ui.designsystem.components.cards.ModelCardVisualState
import com.runninghub.app.ui.designsystem.components.sheets.ModelPickerFilterItem
import com.runninghub.app.ui.designsystem.components.sheets.ModelPickerSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RhModelPickerComponentsContractTest {
    @Test
    fun `model card exposes fixed visible slots`() {
        val state = ModelCardState(
            title = "全能图片 G-2.0",
            capability = "图片生成",
            scene = "适合通用图片创作",
            technicalTags = listOf("text-to-image"),
            price = "12 RHB",
            selected = true,
            visualState = ModelCardVisualState.Selected,
        )

        assertEquals("全能图片 G-2.0", state.title)
        assertEquals("图片生成", state.capability)
        assertEquals("适合通用图片创作", state.scene)
        assertEquals(listOf("text-to-image"), state.technicalTags)
        assertEquals("12 RHB", state.price)
        assertTrue(state.selected)
    }

    @Test
    fun `model picker sheet distinguishes loading empty and search empty states`() {
        val state = ModelPickerSheetState(
            title = "选择模型",
            searchQuery = "Seedance",
            searchPlaceholder = "搜索模型",
            filters = listOf(
                ModelPickerFilterItem(id = "video", label = "视频", selected = true),
            ),
            cards = emptyList(),
            loading = false,
            emptyText = "没有匹配的模型",
        )

        assertEquals("Seedance", state.searchQuery)
        assertFalse(state.loading)
        assertEquals("没有匹配的模型", state.emptyText)
        assertTrue(state.filters.single().selected)
    }
}
