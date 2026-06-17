package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldExtra
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickCreationServiceFieldUiModelTest {
    @Test
    fun `media service fields are renderable`() {
        listOf("IMAGE", "VIDEO", "AUDIO", "UPLOAD").forEach { fieldType ->
            val field = QuickCreationServiceField(
                fieldKey = "media",
                paramKey = "mediaUrls",
                fieldType = fieldType,
                required = true,
                defaultValue = null,
                options = emptyList(),
            )

            assertTrue(field.isQuickCreationServiceFieldRenderable(), "$fieldType should be renderable")
        }
    }

    @Test
    fun `upload hint parts use extra metadata before broad defaults`() {
        val field = QuickCreationServiceField(
            fieldKey = "imageUrls",
            paramKey = "imageUrls",
            fieldType = "IMAGE",
            required = true,
            defaultValue = null,
            options = emptyList(),
            maxUploadCount = 10,
            maxUploadSize = 10L * 1024 * 1024,
            inputExtra = QuickCreationServiceFieldExtra(
                acceptFormats = listOf("JPG", "PNG"),
                maxInputCount = 4,
            ),
        )

        assertEquals(
            listOf("JPG/PNG", "最多 4 个文件", "单文件 10MB"),
            field.quickCreationUploadHintParts(),
        )
    }

    @Test
    fun `invisible service fields are not renderable in tune panel`() {
        val field = QuickCreationServiceField(
            fieldKey = "internalMode",
            paramKey = "internalMode",
            fieldType = "STRING",
            required = false,
            defaultValue = "stable",
            options = emptyList(),
            visible = false,
        )

        assertEquals(false, field.isQuickCreationServiceFieldRenderable())
    }
}
