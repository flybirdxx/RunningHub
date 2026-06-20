package com.runninghub.feature.quickcreate.presentation.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickCreateHistoryUiModelTest {

    @Test
    fun `history mapper derives active item display fields`() {
        val item = historyItem(
            status = "RUNNING",
            params = mapOf("prompt" to "生成城市夜景"),
            cashAmount = 1.236,
            cashCurrency = "CNY",
            outputs = listOf(
                historyOutput(
                    type = "file",
                    url = "https://example.com/result.mp4?token=hidden",
                    thumbnailUrl = "https://example.com/thumb.jpg",
                ),
            ),
        )

        val uiItem = item.toQuickCreateHistoryUiItem()

        assertEquals("生成城市夜景", uiItem.title)
        assertEquals("IMAGE · RUNNING · FILE", uiItem.metadataText)
        assertEquals("1.24 CNY", uiItem.cashText)
        assertEquals(QuickCreateHistoryOutputMediaType.VIDEO, uiItem.primaryOutput?.mediaType)
        assertEquals("https://example.com/thumb.jpg", uiItem.primaryOutput?.previewUrl)
        assertTrue(uiItem.canCancelTask)
        assertTrue(uiItem.needsRefresh)
    }

    @Test
    fun `history mapper disables terminal tasks and hides zero cash`() {
        val item = historyItem(
            status = "SUCCESS",
            params = emptyMap(),
            cashAmount = 0.0,
        )

        val uiItem = item.toQuickCreateHistoryUiItem()

        assertEquals("task_1", uiItem.title)
        assertNull(uiItem.cashText)
        assertFalse(uiItem.canCancelTask)
        assertFalse(uiItem.needsRefresh)
    }

    @Test
    fun `history detail mapper includes output size metadata`() {
        val item = historyItem(
            status = "SUCCESS",
            outputs = listOf(
                historyOutput(
                    type = "png",
                    url = "https://example.com/result.png",
                    width = 1280,
                    height = 720,
                ),
            ),
        )

        val detail = item.toQuickCreateHistoryDetailUiItem()

        assertEquals("IMAGE · SUCCESS · PNG · 1280x720", detail.metadataText)
        assertEquals(QuickCreateHistoryOutputMediaType.IMAGE, detail.primaryOutput?.mediaType)
    }

    private fun historyItem(
        status: String,
        params: Map<String, String> = mapOf("prompt" to "测试提示词"),
        cashAmount: Double = 0.0,
        cashCurrency: String? = null,
        outputs: List<QuickCreationHistoryOutput> = emptyList(),
    ): QuickCreationHistoryItem =
        QuickCreationHistoryItem(
            taskId = "task_1",
            status = status,
            categoryId = "IMAGE",
            params = params,
            cashAmount = cashAmount,
            cashCurrency = cashCurrency,
            outputs = outputs,
        )

    private fun historyOutput(
        type: String,
        url: String,
        thumbnailUrl: String? = null,
        width: Int? = null,
        height: Int? = null,
    ): QuickCreationHistoryOutput =
        QuickCreationHistoryOutput(
            outputId = "output_1",
            url = url,
            type = type,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height,
        )
}
