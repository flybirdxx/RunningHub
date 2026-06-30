package com.runninghub.app.ui.designsystem

import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorOptionState
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorOptionVisualState
import com.runninghub.app.ui.designsystem.components.parameters.ParameterSelectorState
import com.runninghub.app.ui.designsystem.components.sheets.AdvancedSettingsSectionState
import com.runninghub.app.ui.designsystem.components.sheets.AdvancedSettingsSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RhParameterComponentsContractTest {
    @Test
    fun `parameter selector state exposes default selected disabled and error options`() {
        val selector = ParameterSelectorState(
            id = "aspectRatio",
            title = "画面比例",
            options = listOf(
                ParameterSelectorOptionState(
                    id = "16:9",
                    label = "16:9",
                    visualState = ParameterSelectorOptionVisualState.SELECTED,
                ),
                ParameterSelectorOptionState(
                    id = "1:1",
                    label = "1:1",
                    visualState = ParameterSelectorOptionVisualState.DEFAULT,
                ),
                ParameterSelectorOptionState(
                    id = "4:3",
                    label = "4:3",
                    visualState = ParameterSelectorOptionVisualState.DISABLED,
                ),
                ParameterSelectorOptionState(
                    id = "bad",
                    label = "冲突",
                    visualState = ParameterSelectorOptionVisualState.ERROR,
                ),
            ),
        )

        assertEquals("aspectRatio", selector.id)
        assertEquals(
            listOf(
                ParameterSelectorOptionVisualState.SELECTED,
                ParameterSelectorOptionVisualState.DEFAULT,
                ParameterSelectorOptionVisualState.DISABLED,
                ParameterSelectorOptionVisualState.ERROR,
            ),
            selector.options.map { it.visualState },
        )
        assertTrue(selector.options.first().selected)
        assertFalse(selector.options[2].enabled)
    }

    @Test
    fun `advanced settings sheet state keeps advanced section collapsed by default`() {
        val commonSelector = ParameterSelectorState(
            id = "resolution",
            title = "分辨率",
            options = listOf(
                ParameterSelectorOptionState(
                    id = "1K",
                    label = "1K",
                    visualState = ParameterSelectorOptionVisualState.SELECTED,
                ),
            ),
        )
        val advancedSelector = ParameterSelectorState(
            id = "endpoint",
            title = "技术端点",
            options = emptyList(),
            valueText = "/v1/run",
        )
        val sheet = AdvancedSettingsSheetState(
            title = "参数设置",
            commonSection = AdvancedSettingsSectionState(
                title = "常用参数",
                selectors = listOf(commonSelector),
            ),
            advancedSection = AdvancedSettingsSectionState(
                title = "高级设置",
                selectors = listOf(advancedSelector),
                collapsed = true,
            ),
            emptyText = "暂无可调参数",
        )

        assertEquals("参数设置", sheet.title)
        assertEquals(listOf("resolution"), sheet.commonSection.selectors.map { it.id })
        assertEquals(listOf("endpoint"), sheet.advancedSection.selectors.map { it.id })
        assertTrue(sheet.advancedSection.collapsed)
        assertFalse(sheet.empty)
    }
}
