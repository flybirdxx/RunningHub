package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldExtra
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldVisibilityCondition
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
    fun `required upload validation uses service metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "imageUrls",
            paramKey = "imageUrls",
            fieldType = "IMAGE",
            required = true,
            defaultValue = null,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(title = "Reference image"),
        )

        assertEquals("Reference image 不能为空", field.quickCreationUploadValidationError(uploadedCount = 0))
        assertEquals(null, field.quickCreationUploadValidationError(uploadedCount = 1))
    }

    @Test
    fun `upload max count validation uses extra metadata before broad defaults`() {
        val field = QuickCreationServiceField(
            fieldKey = "imageUrls",
            paramKey = "imageUrls",
            fieldType = "IMAGE",
            required = false,
            defaultValue = null,
            options = emptyList(),
            maxUploadCount = 10,
            inputExtra = QuickCreationServiceFieldExtra(title = "Reference image", maxInputCount = 4),
        )

        assertEquals("Reference image 最多 4 个文件", field.quickCreationUploadValidationError(uploadedCount = 5))
        assertEquals(null, field.quickCreationUploadValidationError(uploadedCount = 4))
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

    @Test
    fun `text input is constrained by max length metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "prompt",
            paramKey = "prompt",
            fieldType = "STRING",
            required = true,
            defaultValue = null,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(maxLength = 5),
        )

        assertEquals("abcde", field.constrainQuickCreationTextInput("abcdefg"))
        assertEquals("abc", field.constrainQuickCreationTextInput("abc"))
    }

    @Test
    fun `text limit counter uses max length metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "prompt",
            paramKey = "prompt",
            fieldType = "STRING",
            required = true,
            defaultValue = null,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(maxLength = 5),
        )

        assertEquals("3/5", field.quickCreationTextLimitCounter("abc"))
    }

    @Test
    fun `required text validation uses service metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "tagline",
            paramKey = "tagline",
            fieldType = "STRING",
            required = true,
            defaultValue = null,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(title = "Tagline"),
        )

        assertEquals("Tagline 不能为空", field.quickCreationTextValidationError(""))
    }

    @Test
    fun `min length validation uses service metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "tagline",
            paramKey = "tagline",
            fieldType = "STRING",
            required = true,
            defaultValue = null,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(title = "Tagline", minLength = 3),
        )

        assertEquals("Tagline 至少 3 个字符", field.quickCreationTextValidationError("ab"))
        assertEquals(null, field.quickCreationTextValidationError("abc"))
    }

    @Test
    fun `active child inputs follow parent selection metadata`() {
        val field = QuickCreationServiceField(
            fieldKey = "creationMode",
            paramKey = "creationMode",
            fieldType = "LIST",
            required = true,
            defaultValue = "text",
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(
                inputChildren = listOf(
                    QuickCreationServiceFieldInputChild(
                        fieldKey = "alwaysVisible",
                        paramKey = "alwaysVisible",
                        fieldType = "STRING",
                    ),
                    QuickCreationServiceFieldInputChild(
                        fieldKey = "referenceStrength",
                        paramKey = "referenceStrength",
                        fieldType = "NUMBER",
                        visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                            fieldKey = "creationMode",
                            values = listOf("imageReference"),
                        ),
                    ),
                )
            ),
        )

        assertEquals(
            listOf("alwaysVisible"),
            field.quickCreationActiveInputChildren(params = emptyMap()).map { it.paramKey },
        )
        assertEquals(
            listOf("alwaysVisible", "referenceStrength"),
            field.quickCreationActiveInputChildren(params = mapOf("creationMode" to "imageReference"))
                .map { it.paramKey },
        )
    }

    @Test
    fun `required child text validation uses child metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "referenceStrength",
            paramKey = "referenceStrength",
            fieldType = "NUMBER",
            required = true,
            title = "Reference strength",
        )

        assertEquals("Reference strength 不能为空", child.quickCreationTextValidationError(""))
        assertEquals(null, child.quickCreationTextValidationError("0.65"))
    }

    @Test
    fun `child min length validation uses child metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "subPrompt",
            paramKey = "subPrompt",
            fieldType = "STRING",
            required = true,
            title = "Sub prompt",
            minLength = 3,
        )

        assertEquals("Sub prompt 至少 3 个字符", child.quickCreationTextValidationError("ab"))
        assertEquals(null, child.quickCreationTextValidationError("abc"))
    }

    @Test
    fun `child text input is constrained by max length metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "subPrompt",
            paramKey = "subPrompt",
            fieldType = "STRING",
            maxLength = 5,
        )

        assertEquals("abcde", child.constrainQuickCreationTextInput("abcdefg"))
        assertEquals("abc", child.constrainQuickCreationTextInput("abc"))
    }

    @Test
    fun `child text limit counter uses max length metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "subPrompt",
            paramKey = "subPrompt",
            fieldType = "STRING",
            maxLength = 5,
        )

        assertEquals("3/5", child.quickCreationTextLimitCounter("abc"))
    }

    @Test
    fun `required child upload validation uses child metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "referenceImage",
            paramKey = "referenceImages",
            fieldType = "IMAGE",
            required = true,
            title = "Reference image",
        )

        assertEquals(
            true,
            child.quickCreationUploadValidationError(uploadedCount = 0)?.startsWith("Reference image"),
        )
        assertEquals(null, child.quickCreationUploadValidationError(uploadedCount = 1))
    }

    @Test
    fun `child upload max count validation uses child metadata`() {
        val child = QuickCreationServiceFieldInputChild(
            fieldKey = "referenceImage",
            paramKey = "referenceImages",
            fieldType = "IMAGE",
            title = "Reference image",
            maxInputCount = 1,
        )

        assertEquals(
            true,
            child.quickCreationUploadValidationError(uploadedCount = 2)?.startsWith("Reference image"),
        )
        assertEquals(null, child.quickCreationUploadValidationError(uploadedCount = 1))
    }
}
