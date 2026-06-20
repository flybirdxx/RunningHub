package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import kotlin.test.Test
import kotlin.test.assertEquals

class GenerationHistoryMapperTest {
    @Test
    fun `maps quick creation item to unified history item`() {
        val item = QuickCreationHistoryItem(
            taskId = "task-1",
            status = "SUCCESS",
            skuId = "sku-1",
            taskType = "image",
            cashAmount = 0.16,
            outputs = listOf(
                QuickCreationHistoryOutput(
                    outputId = "out-1",
                    url = "https://example.com/a.png",
                    type = "png",
                    width = 1024,
                    height = 1024,
                )
            )
        ).toGenerationHistoryItem()

        assertEquals("task-1", item.taskId)
        assertEquals("quick_creation", item.source.key)
        assertEquals("SUCCESS", item.status)
        assertEquals("sku-1", item.modelId)
        assertEquals("image", item.taskType)
        assertEquals(0.16, item.costAmount)
        assertEquals("https://example.com/a.png", item.outputs.single().url)
        assertEquals(true, item.outputs.single().isImage)
    }
}
