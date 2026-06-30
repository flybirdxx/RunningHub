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
        assertEquals(
            QuickCreationServiceUploadHint(
                acceptFormats = listOf("PNG"),
                maxUploadCount = 2,
                maxUploadSizeMegabytes = 10,
            ),
            fields[3].uploadHint,
        )
    }

    @Test
    fun `service field ui items render standard model lora and file fields`() {
        val model = QuickCreationServiceModel(
            categoryId = "VIDEO",
            groupName = "自部署开源模型",
            bindingId = "binding-standard",
            skuId = "sku-standard",
            name = "Standard model",
            description = null,
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "16##lora_name",
                    paramKey = "lora",
                    fieldType = "MODEL",
                    required = false,
                    defaultValue = "example.safetensors",
                    options = emptyList(),
                    inputExtra = QuickCreationServiceFieldExtra(title = "LoRA"),
                ),
                QuickCreationServiceField(
                    fieldKey = "3##file",
                    paramKey = "file",
                    fieldType = "FILE",
                    required = true,
                    defaultValue = null,
                    options = emptyList(),
                    inputExtra = QuickCreationServiceFieldExtra(
                        title = "Source file",
                        acceptFormats = listOf("mp4", "wav"),
                    ),
                ),
            ),
        )

        val fields = model.quickCreationServiceFieldUiItems(params = emptyMap())

        assertEquals(listOf("lora", "file"), fields.map { it.paramKey })
        assertEquals(QuickCreationServiceFieldControlType.TEXT, fields[0].controlType)
        assertEquals("example.safetensors", fields[0].textValue)
        assertEquals(QuickCreationServiceFieldControlType.UPLOAD, fields[1].controlType)
        assertEquals(null, fields[1].uploadMediaType)
        assertEquals(listOf("mp4", "wav"), fields[1].uploadHint.acceptFormats)
    }

    @Test
    fun `parameter sheet state prioritizes common fields and folds technical fields`() {
        val model = QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = null,
            bindingId = "binding-params",
            skuId = "sku-params",
            name = "Params model",
            description = null,
            fields = listOf(
                field("prompt", "prompt", "STRING", title = "Prompt"),
                field("aspectRatio", "aspectRatio", "LIST", title = "Aspect", defaultValue = "16:9"),
                field("resolution", "resolution", "LIST", title = "Resolution", defaultValue = "1K"),
                field("count", "count", "INTEGER", title = "Count", defaultValue = "1"),
                field("upload", "uploadImages", "IMAGE_UPLOAD", title = "Upload"),
                field("endpoint", "endpoint", "STRING", title = "Endpoint", defaultValue = "/v1/run"),
                field("seed", "seed", "INTEGER", title = "Seed"),
                field("negative", "negativePrompt", "STRING", title = "Negative prompt"),
                field("node", "workflowNode", "STRING", title = "Workflow node", defaultValue = "12"),
            ),
        )

        val fields = model.quickCreationServiceFieldUiItems(params = emptyMap())
        val sheet = quickCreateParameterSheetState(fields)

        assertEquals(
            listOf("aspectRatio", "resolution", "count", "uploadImages"),
            sheet.commonFields.map { it.paramKey },
        )
        assertEquals(listOf("prompt"), sheet.promptFields.map { it.paramKey })
        assertEquals(
            listOf("endpoint", "seed", "negativePrompt", "workflowNode"),
            sheet.advancedFields.map { it.paramKey },
        )
        assertEquals(QuickCreationServiceFieldDisplayLabel.AspectRatio, fields.first { it.paramKey == "aspectRatio" }.displayLabel)
        assertEquals(QuickCreationServiceFieldDisplayLabel.Resolution, fields.first { it.paramKey == "resolution" }.displayLabel)
        assertEquals(QuickCreationServiceFieldDisplayLabel.Count, fields.first { it.paramKey == "count" }.displayLabel)
        assertEquals(QuickCreationServiceFieldDisplayLabel.TechnicalEndpoint, fields.first { it.paramKey == "endpoint" }.displayLabel)
        assertEquals(QuickCreationServiceFieldSection.ADVANCED, fields.first { it.paramKey == "endpoint" }.section)
        assertEquals(true, sheet.advancedCollapsed)
        assertEquals(false, sheet.empty)
    }

    @Test
    fun `field ui status exposes missing conflict readonly and selected option states`() {
        val model = QuickCreationServiceModel(
            categoryId = "IMAGE",
            groupName = null,
            bindingId = "binding-status",
            skuId = "sku-status",
            name = "Status model",
            description = null,
            fields = listOf(
                field(
                    fieldKey = "style",
                    paramKey = "style",
                    fieldType = "LIST",
                    title = "Style",
                    defaultValue = "realistic",
                    options = listOf(
                        QuickCreationServiceFieldOption(label = "写实", value = "realistic"),
                        QuickCreationServiceFieldOption(label = "水彩", value = "watercolor"),
                    ),
                ),
                field(
                    fieldKey = "mode",
                    paramKey = "mode",
                    fieldType = "LIST",
                    title = "Mode",
                    defaultValue = "safe",
                    options = listOf(QuickCreationServiceFieldOption(label = "安全", value = "safe")),
                ),
                field("readonly", "readonly", "LIST", title = "Readonly", defaultValue = "auto"),
                field("negative", "negativePrompt", "STRING", title = "Negative", required = true),
            ),
        )

        val fields = model.quickCreationServiceFieldUiItems(params = mapOf("mode" to "unknown"))

        assertEquals(
            listOf(
                QuickCreationServiceFieldOptionVisualState.SELECTED,
                QuickCreationServiceFieldOptionVisualState.DEFAULT,
            ),
            fields.first { it.paramKey == "style" }.options.map { it.visualState },
        )
        assertEquals(QuickCreationServiceFieldVisualState.ERROR, fields.first { it.paramKey == "mode" }.visualState)
        assertEquals(QuickCreationServiceFieldVisualState.DISABLED, fields.first { it.paramKey == "readonly" }.visualState)
        assertEquals(QuickCreationServiceFieldVisualState.ERROR, fields.first { it.paramKey == "negativePrompt" }.visualState)
        assertEquals(QuickCreationServiceFieldSection.ADVANCED, fields.first { it.paramKey == "negativePrompt" }.section)
    }
}

private fun field(
    fieldKey: String,
    paramKey: String,
    fieldType: String,
    title: String,
    required: Boolean = false,
    defaultValue: String? = null,
    options: List<QuickCreationServiceFieldOption> = emptyList(),
): QuickCreationServiceField =
    QuickCreationServiceField(
        fieldKey = fieldKey,
        paramKey = paramKey,
        fieldType = fieldType,
        required = required,
        defaultValue = defaultValue,
        options = options,
        inputExtra = QuickCreationServiceFieldExtra(title = title),
    )
