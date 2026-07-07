package com.runninghub.feature.quickcreate.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuickCreationServiceSchemaTest {

    @Test
    fun `default params include active child defaults only when parent condition matches`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childTextField(
                            paramKey = "strength",
                            defaultValue = "0.8",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val inactiveDefaults = QuickCreationServiceSchema.defaultParams(
            model = model,
            activeParams = mapOf("mode" to "text"),
        )
        val activeDefaults = QuickCreationServiceSchema.defaultParams(
            model = model,
            activeParams = mapOf("mode" to "image"),
        )

        assertEquals("text", inactiveDefaults["mode"])
        assertNull(inactiveDefaults["strength"])
        assertEquals("0.8", activeDefaults["strength"])
    }

    @Test
    fun `default params skip upload field placeholder defaults`() {
        val model = serviceModel(
            fields = listOf(
                uploadField(
                    paramKey = "imageUrls",
                    fieldType = "IMAGE",
                    maxUploadCount = 9,
                    defaultValue = "9",
                ),
                uploadField(
                    paramKey = "videoUrls",
                    fieldType = "VIDEO",
                    maxUploadCount = 3,
                    defaultValue = "3",
                ),
                optionField(paramKey = "ratio", defaultValue = "adaptive"),
            ),
        )

        val defaults = QuickCreationServiceSchema.defaultParams(model)

        assertNull(defaults["imageUrls"])
        assertNull(defaults["videoUrls"])
        assertEquals("adaptive", defaults["ratio"])
    }

    @Test
    fun `seedance text video fields follow official docs and omit asset slot params`() {
        val model = serviceModel(
            categoryId = "VIDEO",
            groupName = "Seedance2.0",
            name = "seedance2.0/文生视频",
            fields = listOf(
                serviceField(paramKey = "duration", defaultValue = "5"),
                serviceField(paramKey = "webSearch", defaultValue = "false"),
                serviceField(paramKey = "conversionSlots", defaultValue = "[\"all\"]"),
                serviceField(paramKey = "realPersonMode", defaultValue = "true"),
                uploadField(paramKey = "imageUrls", fieldType = "IMAGE", defaultValue = "9"),
            ),
        )

        val fields = QuickCreationServiceSchema.resolvedFields(model, serviceParams = emptyMap())
        val defaults = QuickCreationServiceSchema.defaultParams(model)
        val activeKeys = QuickCreationServiceSchema.activeParamKeys(
            model = model,
            serviceParams = mapOf("conversionSlots" to "[\"all\"]", "duration" to "4"),
        )

        assertEquals(listOf("duration", "webSearch"), fields.map { it.paramKey })
        assertEquals(mapOf("duration" to "5", "webSearch" to "false"), defaults)
        assertEquals(setOf("duration", "webSearch"), activeKeys)
        assertEquals(
            mapOf("duration" to "4", "webSearch" to "false"),
            QuickCreationServiceSchema.sanitizedParams(
                model = model,
                params = mapOf("duration" to "4", "conversionSlots" to "[\"all\"]"),
            ),
        )
    }

    @Test
    fun `seedance multimodal keeps documented media and real person params but omits asset slots`() {
        val model = serviceModel(
            categoryId = "VIDEO",
            groupName = "Seedance2.0",
            name = "seedance2.0-Mini/多模态视频",
            fields = listOf(
                serviceField(paramKey = "duration", defaultValue = "5"),
                uploadField(paramKey = "imageUrls", fieldType = "IMAGE", defaultValue = "9"),
                uploadField(paramKey = "videoUrls", fieldType = "VIDEO", defaultValue = "3"),
                uploadField(paramKey = "audioUrls", fieldType = "AUDIO", defaultValue = "3"),
                serviceField(paramKey = "realPersonMode", defaultValue = "true"),
                serviceField(paramKey = "conversionSlots", defaultValue = "[\"all\"]"),
            ),
        )

        val fields = QuickCreationServiceSchema.resolvedFields(model, serviceParams = emptyMap())
        val defaults = QuickCreationServiceSchema.defaultParams(model)

        assertEquals(
            listOf("duration", "imageUrls", "videoUrls", "audioUrls", "realPersonMode"),
            fields.map { it.paramKey },
        )
        assertEquals(mapOf("duration" to "5", "realPersonMode" to "true"), defaults)
        assertEquals(
            setOf("imageUrls", "videoUrls", "audioUrls"),
            QuickCreationServiceSchema.activeUploadParamKeys(model, serviceParams = emptyMap()),
        )
        assertEquals(
            mapOf("duration" to "5", "realPersonMode" to "true"),
            QuickCreationServiceSchema.sanitizedParams(
                model = model,
                params = mapOf("conversionSlots" to "[\"all\"]"),
            ),
        )
    }

    @Test
    fun `list params prefer field bound uploads over global fallback`() {
        val model = serviceModel(
            fields = listOf(uploadField(paramKey = "imageUrls", maxUploadCount = 2)),
        )
        val listParams = QuickCreationServiceSchema.listParams(
            model = model,
            uploadedMedia = listOf(
                QuickCreationUploadedMedia(
                    mediaKind = QuickCreationUploadMediaKind.IMAGE,
                    remoteUrl = "https://example.com/global.png",
                ),
                QuickCreationUploadedMedia(
                    mediaKind = QuickCreationUploadMediaKind.IMAGE,
                    remoteUrl = "https://example.com/field.png",
                    fieldParamKey = "imageUrls",
                ),
            ),
            fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
            serviceParams = emptyMap(),
        )

        assertEquals(listOf("https://example.com/field.png"), listParams["imageUrls"])
    }

    @Test
    fun `validate uploads ignores inactive child and blocks active required child`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            paramKey = "childImages",
                            required = true,
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertNull(
            QuickCreationServiceSchema.validateUploads(
                model = model,
                uploadedMedia = emptyList(),
                fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
                serviceParams = mapOf("mode" to "text"),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.Required("childImages"),
            QuickCreationServiceSchema.validateUploads(
                model = model,
                uploadedMedia = emptyList(),
                fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
                serviceParams = mapOf("mode" to "image"),
            ),
        )
    }

    @Test
    fun `active upload param keys include top level and active child fields`() {
        val model = serviceModel(
            fields = listOf(
                uploadField(paramKey = "coverImage"),
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            paramKey = "maskImage",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val keys = QuickCreationServiceSchema.activeUploadParamKeys(
            model = model,
            serviceParams = mapOf("mode" to "image"),
        )

        assertEquals(setOf("coverImage", "maskImage"), keys)
        assertTrue(QuickCreationServiceSchema.hasFieldParam(model, "maskImage"))
    }

    @Test
    fun `active upload param keys resolve sibling field key conditions from param key values`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    fieldKey = "creationMode",
                    paramKey = "creation_mode",
                    defaultValue = "text",
                    children = emptyList(),
                ),
                optionField(
                    fieldKey = "referenceGroup",
                    paramKey = "reference_group",
                    defaultValue = "none",
                    children = listOf(
                        childUploadField(
                            fieldKey = "referenceImage",
                            paramKey = "reference_images",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "creationMode",
                                values = listOf("imageReference"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val keys = QuickCreationServiceSchema.activeUploadParamKeys(
            model = model,
            serviceParams = mapOf("creation_mode" to "imageReference"),
        )

        assertEquals(setOf("reference_images"), keys)
    }

    @Test
    fun `active upload param keys ignore inactive child defaults`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "creationMode",
                    defaultValue = "text",
                    children = listOf(
                        childTextField(
                            paramKey = "referenceStrength",
                            defaultValue = "0.65",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "creationMode",
                                values = listOf("imageReference"),
                            ),
                        ),
                        childUploadField(
                            paramKey = "derivedImages",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "referenceStrength",
                                values = listOf("0.65"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val keys = QuickCreationServiceSchema.activeUploadParamKeys(
            model = model,
            serviceParams = emptyMap(),
        )

        assertEquals(emptySet(), keys)
    }

    @Test
    fun `active upload param keys ignore explicit values from inactive child fields`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "creationMode",
                    defaultValue = "text",
                    children = listOf(
                        childTextField(
                            paramKey = "referenceStrength",
                            defaultValue = "0.65",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "creationMode",
                                values = listOf("imageReference"),
                            ),
                        ),
                        childUploadField(
                            paramKey = "derivedImages",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "referenceStrength",
                                values = listOf("0.65"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val keys = QuickCreationServiceSchema.activeUploadParamKeys(
            model = model,
            serviceParams = mapOf(
                "creationMode" to "text",
                "referenceStrength" to "0.65",
            ),
        )

        assertEquals(emptySet(), keys)
    }

    @Test
    fun `canonical params map field key to param key and prefer canonical value`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    fieldKey = "modeField",
                    paramKey = "modeParam",
                    defaultValue = "text",
                    children = listOf(
                        childTextField(
                            fieldKey = "strengthField",
                            paramKey = "strengthParam",
                            defaultValue = "0.8",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "modeField",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val params = QuickCreationServiceSchema.canonicalParams(
            model = model,
            params = mapOf(
                "modeField" to "text",
                "modeParam" to "image",
                "strengthField" to "0.6",
                "custom" to "kept",
            ),
        )

        assertEquals("image", params["modeParam"])
        assertEquals("0.6", params["strengthParam"])
        assertEquals("kept", params["custom"])
    }

    @Test
    fun `active upload aliases include active fields and exclude inactive child fields`() {
        val model = serviceModel(
            fields = listOf(
                uploadField(
                    fieldKey = "coverField",
                    paramKey = "coverParam",
                    fieldType = "VIDEO_UPLOAD",
                ),
                optionField(
                    fieldKey = "modeField",
                    paramKey = "modeParam",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            fieldKey = "maskField",
                            paramKey = "maskParam",
                            fieldType = "IMAGE_UPLOAD",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "modeField",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val inactiveAliases = QuickCreationServiceSchema.activeUploadParamAliases(
            model = model,
            serviceParams = mapOf("modeField" to "text"),
        )
        val activeAliases = QuickCreationServiceSchema.activeUploadParamAliases(
            model = model,
            serviceParams = mapOf("modeField" to "image"),
        )

        assertEquals("coverParam", inactiveAliases["coverField"]?.paramKey)
        assertEquals(QuickCreationUploadMediaKind.VIDEO, inactiveAliases["coverParam"]?.mediaKind)
        assertNull(inactiveAliases["maskField"])
        assertEquals("maskParam", activeAliases["maskField"]?.paramKey)
        assertEquals(QuickCreationUploadMediaKind.IMAGE, activeAliases["maskParam"]?.mediaKind)
    }

    @Test
    fun `declared upload aliases include inactive child fields`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    fieldKey = "modeField",
                    paramKey = "modeParam",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            fieldKey = "audioField",
                            paramKey = "audioParam",
                            fieldType = "AUDIO_UPLOAD",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "modeField",
                                values = listOf("audio"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val aliases = QuickCreationServiceSchema.declaredUploadParamAliases(model)

        assertEquals("audioParam", aliases["audioField"])
        assertEquals("audioParam", aliases["audioParam"])
    }

    @Test
    fun `resolved fields include option text upload metadata and active children`() {
        val model = serviceModel(
            fields = listOf(
                optionField(
                    paramKey = "style",
                    defaultValue = "realistic",
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
                optionField(
                    paramKey = "referenceMode",
                    defaultValue = "none",
                    children = listOf(
                        childUploadField(
                            fieldKey = "referenceImage",
                            paramKey = "referenceImages",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "referenceMode",
                                values = listOf("image"),
                            ),
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
                    maxUploadCount = 10,
                    maxUploadSize = 10L * 1024 * 1024,
                    inputExtra = QuickCreationServiceFieldExtra(
                        title = "上传图片",
                        acceptFormats = listOf("PNG"),
                        maxInputCount = 2,
                    ),
                ),
            ),
        )

        val fields = QuickCreationServiceSchema.resolvedFields(
            model = model,
            serviceParams = mapOf(
                "style" to "image",
                "tagline" to "abcdef",
                "referenceMode" to "image",
            ),
        )

        assertEquals(4, fields.size)
        assertEquals(QuickCreationResolvedFieldKind.OPTIONS, fields[0].kind)
        assertEquals("image", fields[0].currentValue)
        assertEquals(listOf("text", "image"), fields[0].options.map { it.value })
        assertEquals(QuickCreationResolvedFieldKind.TEXT, fields[1].kind)
        assertEquals("短标题", fields[1].title)
        assertEquals("输入短标题", fields[1].placeholder)
        assertEquals(5, fields[1].maxLength)
        assertEquals("referenceImages", fields[2].childFields.single().paramKey)
        assertEquals(QuickCreationResolvedFieldKind.UPLOAD, fields[3].kind)
        assertEquals(QuickCreationUploadMediaKind.IMAGE, fields[3].uploadMediaKind)
        assertEquals(listOf("PNG"), fields[3].acceptFormats)
        assertEquals(2, fields[3].maxUploadCount)
        assertEquals(10L * 1024 * 1024, fields[3].maxUploadSizeBytes)
    }

    @Test
    fun `resolved fields exclude invisible fields and keep media fields renderable`() {
        val model = serviceModel(
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "hiddenText",
                    paramKey = "hiddenText",
                    fieldType = "STRING",
                    required = false,
                    defaultValue = null,
                    options = emptyList(),
                    visible = false,
                ),
                QuickCreationServiceField(
                    fieldKey = "audio",
                    paramKey = "audioUrls",
                    fieldType = "AUDIO_UPLOAD",
                    required = false,
                    defaultValue = null,
                    options = emptyList(),
                ),
            ),
        )

        val fields = QuickCreationServiceSchema.resolvedFields(
            model = model,
            serviceParams = emptyMap(),
        )

        assertEquals(listOf("audioUrls"), fields.map { it.paramKey })
        assertEquals(QuickCreationResolvedFieldKind.UPLOAD, fields.single().kind)
        assertEquals(QuickCreationUploadMediaKind.AUDIO, fields.single().uploadMediaKind)
    }

    @Test
    fun `resolved fields keep standard model lora and generic file parameters renderable`() {
        val model = serviceModel(
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
                    inputExtra = QuickCreationServiceFieldExtra(title = "Source file"),
                ),
            ),
        )

        val fields = QuickCreationServiceSchema.resolvedFields(
            model = model,
            serviceParams = emptyMap(),
        )

        assertEquals(listOf("lora", "file"), fields.map { it.paramKey })
        assertEquals(QuickCreationResolvedFieldKind.TEXT, fields[0].kind)
        assertEquals("example.safetensors", fields[0].currentValue)
        assertEquals(QuickCreationResolvedFieldKind.UPLOAD, fields[1].kind)
        assertEquals(null, fields[1].uploadMediaKind)
    }

    @Test
    fun `validate fields applies required and min length rules to top level and active child text`() {
        val model = serviceModel(
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "tagline",
                    paramKey = "tagline",
                    fieldType = "STRING",
                    required = true,
                    defaultValue = null,
                    options = emptyList(),
                    inputExtra = QuickCreationServiceFieldExtra(title = "Tagline", minLength = 3),
                ),
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childTextField(
                            paramKey = "subPrompt",
                            defaultValue = "",
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                            required = true,
                            title = "Sub prompt",
                            minLength = 3,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            QuickCreationServiceValidationIssue.Required("Tagline"),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("tagline" to ""),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.MinLength("Tagline", 3),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("tagline" to "ab"),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.Required("Sub prompt"),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("tagline" to "abc", "mode" to "image", "subPrompt" to ""),
            ),
        )
    }

    @Test
    fun `validate fields accepts json array defaults for multi select options`() {
        val model = serviceModel(
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "conversionSlots",
                    paramKey = "conversionSlots",
                    fieldType = "LIST",
                    required = false,
                    defaultValue = "[\"all\"]",
                    options = listOf(
                        QuickCreationServiceFieldOption(label = "all", value = "all"),
                        QuickCreationServiceFieldOption(label = "video1", value = "video1"),
                    ),
                    inputExtra = QuickCreationServiceFieldExtra(title = "conversionSlots"),
                ),
            ),
        )

        assertNull(
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = emptyMap(),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.InvalidOption("conversionSlots"),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("conversionSlots" to "[\"missing\"]"),
            ),
        )
    }

    @Test
    fun `validate fields accepts whole number decimals for numeric option values`() {
        val model = serviceModel(
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "duration",
                    paramKey = "duration",
                    fieldType = "LIST",
                    required = true,
                    defaultValue = "5",
                    options = listOf(
                        QuickCreationServiceFieldOption(label = "5", value = "5"),
                        QuickCreationServiceFieldOption(label = "6", value = "6"),
                    ),
                    inputExtra = QuickCreationServiceFieldExtra(title = "duration"),
                ),
            ),
        )

        assertNull(
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("duration" to "5.0"),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.InvalidOption("duration"),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("duration" to "5.5"),
            ),
        )
    }

    @Test
    fun `validate fields accepts seedance duration values even when catalog options omit documented range`() {
        val model = serviceModel(
            categoryId = "VIDEO",
            groupName = "Seedance2.0",
            name = "seedance2.0-Mini/多模态视频",
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "duration",
                    paramKey = "duration",
                    fieldType = "LIST",
                    required = true,
                    defaultValue = "5",
                    options = (6..15).map { seconds ->
                        QuickCreationServiceFieldOption(label = seconds.toString(), value = seconds.toString())
                    },
                    inputExtra = QuickCreationServiceFieldExtra(title = "duration"),
                ),
            ),
        )

        assertNull(
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("duration" to "4"),
            ),
        )
        assertNull(
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = emptyMap(),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.InvalidOption("duration"),
            QuickCreationServiceSchema.validateFields(
                model = model,
                serviceParams = mapOf("duration" to "16"),
            ),
        )
    }

    @Test
    fun `validate uploads applies required and max count rules to top level and active child uploads`() {
        val model = serviceModel(
            fields = listOf(
                QuickCreationServiceField(
                    fieldKey = "imageUrls",
                    paramKey = "imageUrls",
                    fieldType = "IMAGE",
                    required = true,
                    defaultValue = null,
                    options = emptyList(),
                    maxUploadCount = 10,
                    inputExtra = QuickCreationServiceFieldExtra(title = "Reference image", maxInputCount = 4),
                ),
                optionField(
                    paramKey = "mode",
                    defaultValue = "text",
                    children = listOf(
                        childUploadField(
                            paramKey = "childImages",
                            fieldType = "IMAGE",
                            required = true,
                            visibleWhen = QuickCreationServiceFieldVisibilityCondition(
                                fieldKey = "mode",
                                values = listOf("image"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            QuickCreationServiceValidationIssue.Required("Reference image"),
            QuickCreationServiceSchema.validateUploads(
                model = model,
                uploadedMedia = emptyList(),
                fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
                serviceParams = emptyMap(),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.MaxUploadCount("Reference image", 4),
            QuickCreationServiceSchema.validateUploads(
                model = model,
                uploadedMedia = (1..5).map { index ->
                    QuickCreationUploadedMedia(
                        mediaKind = QuickCreationUploadMediaKind.IMAGE,
                        remoteUrl = "https://example.com/$index.png",
                        fieldParamKey = "imageUrls",
                    )
                },
                fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
                serviceParams = emptyMap(),
            ),
        )
        assertEquals(
            QuickCreationServiceValidationIssue.Required("childImages"),
            QuickCreationServiceSchema.validateUploads(
                model = model,
                uploadedMedia = listOf(
                    QuickCreationUploadedMedia(
                        mediaKind = QuickCreationUploadMediaKind.IMAGE,
                        remoteUrl = "https://example.com/top.png",
                        fieldParamKey = "imageUrls",
                    ),
                ),
                fallbackMediaKind = QuickCreationUploadMediaKind.IMAGE,
                serviceParams = mapOf("mode" to "image"),
            ),
        )
    }

    private fun serviceModel(
        categoryId: String = "IMAGE",
        groupName: String = "图片生成",
        name: String = "测试模型",
        fields: List<QuickCreationServiceField> = emptyList(),
    ): QuickCreationServiceModel =
        QuickCreationServiceModel(
            categoryId = categoryId,
            groupName = groupName,
            bindingId = "binding-1",
            skuId = "sku-1",
            name = name,
            description = null,
            fields = fields,
        )

    private fun optionField(
        paramKey: String,
        fieldKey: String = paramKey,
        defaultValue: String,
        children: List<QuickCreationServiceFieldInputChild> = emptyList(),
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = "LIST",
            required = false,
            defaultValue = defaultValue,
            options = listOf(
                QuickCreationServiceFieldOption("文本", "text"),
                QuickCreationServiceFieldOption("图片", "image"),
            ),
            inputExtra = QuickCreationServiceFieldExtra(inputChildren = children),
        )

    private fun serviceField(
        paramKey: String,
        fieldKey: String = paramKey,
        fieldType: String = "STRING",
        defaultValue: String,
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = fieldType,
            required = false,
            defaultValue = defaultValue,
            options = emptyList(),
            inputExtra = QuickCreationServiceFieldExtra(title = paramKey),
        )

    private fun uploadField(
        paramKey: String,
        fieldKey: String = paramKey,
        fieldType: String = "IMAGE_UPLOAD",
        required: Boolean = false,
        maxUploadCount: Int? = 1,
        defaultValue: String? = null,
    ): QuickCreationServiceField =
        QuickCreationServiceField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = fieldType,
            required = required,
            defaultValue = defaultValue,
            options = emptyList(),
            maxUploadCount = maxUploadCount,
        )

    private fun childTextField(
        paramKey: String,
        fieldKey: String = paramKey,
        defaultValue: String,
        visibleWhen: QuickCreationServiceFieldVisibilityCondition,
        required: Boolean = false,
        title: String? = null,
        minLength: Int? = null,
    ): QuickCreationServiceFieldInputChild =
        QuickCreationServiceFieldInputChild(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = "STRING",
            required = required,
            title = title,
            defaultValue = defaultValue,
            minLength = minLength,
            visibleWhen = visibleWhen,
        )

    private fun childUploadField(
        paramKey: String,
        fieldKey: String = paramKey,
        fieldType: String = "IMAGE_UPLOAD",
        required: Boolean = false,
        visibleWhen: QuickCreationServiceFieldVisibilityCondition,
    ): QuickCreationServiceFieldInputChild =
        QuickCreationServiceFieldInputChild(
            fieldKey = fieldKey,
            paramKey = paramKey,
            fieldType = fieldType,
            required = required,
            maxInputCount = 1,
            visibleWhen = visibleWhen,
        )
}
