package com.runninghub.feature.quickcreate.presentation.fields

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreationServiceFieldUiModelTest {
    @Test
    fun `service field ui items map selected option text upload and active children`() {
        val model = QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = null,
            bindingId = "binding-1",
            skuId = "sku-1",
            name = "Image model",
            description = null,
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "style",
                    paramKey = "style",
                    fieldType = "LIST",
                    required = false,
                    defaultValue = "realistic",
                    options = listOf(
                        QuickCreationServiceFieldOption(label = "写实", value = "realistic"),
                        QuickCreationServiceFieldOption(label = "水彩", value = "watercolor"),
                    ),
                    inputExtra = QuickCreationServiceFieldExtra(title = "风格"),
                ),
                QuickCreationServiceField(
                    fieldKey = "tagline",
                    paramKey = "tagline",
                    fieldType = "STRING",
                    required = false,
                    defaultValue = null,
                    options = emptyList(),
                    inputExtra = QuickCreationServiceFieldExtra(
                        title = "短标题",
                        placeholder = "输入短标题",
                        maxLength = 5,
                    ),
                ),
                QuickCreationServiceField(
                    fieldKey = "referenceMode",
                    paramKey = "referenceMode",
                    fieldType = "LIST",
                    required = false,
                    defaultValue = "none",
                    options = listOf(QuickCreationServiceFieldOption(label = "参考图", value = "image")),
                    inputExtra = QuickCreationServiceFieldExtra(
                        inputChildren = listOf(
                            QuickCreationServiceFieldInputChild(
                                fieldKey = "referenceImage",
                                paramKey = "referenceImages",
                                fieldType = "IMAGE_UPLOAD",
                                title = "参考图片",
                                visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                    fieldKey = "referenceMode",
                                    values = listOf("image"),
                                ),
                            )
                        ),
                    ),
                ),
                QuickCreationServiceField(
                    fieldKey = "upload",
                    paramKey = "uploadImages",
                    fieldType = "IMAGE_UPLOAD",
                    required = false,
                    defaultValue = null,
                    options = emptyList(),
                    maxUploadSize = 10L * 1024 * 1024,
                    inputExtra = QuickCreationServiceFieldExtra(
                        title = "上传图片",
                        acceptFormats = listOf("PNG"),
                        maxInputCount = 2,
                    ),
                ),
            ),
        )

        val fields = model.quickCreationServiceFieldUiItems(
            params = mapOf(
                "style" to "watercolor",
                "tagline" to "abcdef",
                "referenceMode" to "image",
            ),
        )

        assertEquals(4, fields.size)
        assertEquals(QuickCreationServiceFieldControlType.OPTIONS, fields[0].controlType)
        assertEquals(listOf(false, true), fields[0].options.map { it.selected })
        assertEquals("短标题", fields[1].title)
        assertEquals(QuickCreationServiceFieldControlType.TEXT, fields[1].controlType)
        assertEquals("abcde", fields[1].constrainTextInput("abcdef"))
        assertEquals("5/5", fields[1].textLimitCounter)
        assertEquals("参考图片", fields[2].childFields.single().title)
        assertEquals(1, fields[2].childFields.single().indentLevel)
        assertEquals(QuickCreationServiceFieldControlType.UPLOAD, fields[3].controlType)
        assertEquals(QuickCreationServiceUploadMediaType.IMAGE, fields[3].uploadMediaType)
        assertEquals("PNG · 最多 2 个文件 · 单文件 10MB", fields[3].uploadHint)
    }
}
