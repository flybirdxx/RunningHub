package com.runninghub.feature.detail.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDetailParamsLayoutTest {
    @Test
    fun `six or fewer rows stay flat`() {
        val layout = appDetailParamsLayout(rows(count = 6))
        assertEquals(6, layout.coreRows.size)
        assertTrue(layout.advancedGroups.isEmpty())
    }

    @Test
    fun `above threshold media and multiline text stay core`() {
        val all = listOf(
            mediaRow("1", "输入"), multilineTextRow("2", "输入"),
            dropdownRow("3", "采样"), intRow("4", "采样"), decimalRow("5", "采样"),
            switchRow("6", "修复"), segmentedRow("7", "修复"), textRow("8", "输出"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(2, layout.coreRows.size)
        assertEquals(listOf("采样", "修复", null), layout.advancedGroups.map { it.title })
        assertEquals(listOf(3, 2, 1), layout.advancedGroups.map { it.rows.size })
    }

    @Test
    fun `consecutive rows sharing node name form one group and blank names coalesce`() {
        val all = listOf(
            mediaRow("1", "输入"),
            intRow("2", "采样"), intRow("3", "采样"),
            intRow("4", ""), intRow("5", ""),
            intRow("6", "采样"), intRow("7", "其他"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(1, layout.coreRows.size)
        assertEquals(listOf("采样", null), layout.advancedGroups.map { it.title })
        assertEquals(4, layout.advancedGroups[1].rows.size)
    }

    @Test
    fun `alternating singleton node names coalesce into one untitled group`() {
        val all = listOf(
            mediaRow("1", "输入"),
            intRow("2", "easy int"), switchRow("3", "easy boolean"),
            decimalRow("4", "easy float"), switchRow("5", "easy boolean"),
            decimalRow("6", "easy float"), intRow("7", "ImpactSwitch"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(listOf<String?>(null), layout.advancedGroups.map { it.title })
        assertEquals(6, layout.advancedGroups.single().rows.size)
    }

    @Test
    fun `modified count aggregates per group`() {
        val all = listOf(
            mediaRow("1", "输入"),
            intRow("2", "采样", modified = true), intRow("3", "采样"),
            intRow("4", "采样", modified = true),
            intRow("5", "输出"), intRow("6", "输出"), intRow("7", "输出"),
        )
        val layout = appDetailParamsLayout(all)
        assertEquals(listOf(2, 0), layout.advancedGroups.map { it.modifiedCount })
    }

    @Test
    fun `all advanced when no media or multiline keeps first three rows core`() {
        val layout = appDetailParamsLayout(List(8) { intRow("${it + 1}", "组") })
        assertEquals(3, layout.coreRows.size)
        assertEquals(5, layout.advancedGroups.sumOf { it.rows.size })
    }

    private fun rows(count: Int): List<AppDetailInputRowUiModel> =
        List(count) { intRow("${it + 1}", "组$it") }

    private fun mediaRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.MediaUpload(AppDetailMediaType.IMAGE), modified)

    private fun multilineTextRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.Text(multiline = true), modified)

    private fun textRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.Text(multiline = false), modified)

    private fun dropdownRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.Dropdown(listOf("A", "B")), modified)

    private fun intRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.IntegerText, modified)

    private fun decimalRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.DecimalText, modified)

    private fun switchRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.BooleanSwitch, modified)

    private fun segmentedRow(nodeId: String, nodeName: String, modified: Boolean = false) =
        singleRow(nodeId, nodeName, AppDetailInputControl.Segmented(listOf("Fast", "Quality")), modified)

    private fun singleRow(
        nodeId: String,
        nodeName: String,
        control: AppDetailInputControl,
        modified: Boolean,
    ): AppDetailInputRowUiModel.Single =
        AppDetailInputRowUiModel.Single(
            field = AppDetailInputFieldUiModel(
                nodeId = nodeId,
                fieldName = "f",
                inputKey = "$nodeId#f",
                title = "字段$nodeId",
                currentValue = "值",
                control = control,
                defaultValue = "",
                isModified = modified,
                nodeName = nodeName,
            ),
        )
}
