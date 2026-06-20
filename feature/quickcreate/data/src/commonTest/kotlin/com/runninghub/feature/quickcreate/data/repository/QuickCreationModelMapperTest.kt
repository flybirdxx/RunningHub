package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationFieldDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationModelDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationPricingDto
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuickCreationModelMapperTest {
    @Test
    fun `flattens service model groups to selectable domain models`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "group",
                categoryId = "IMAGE",
                name = "全能图片",
                children = listOf(
                    QuickCreationModelDto(
                        type = "model",
                        categoryId = "IMAGE",
                        bindingId = "binding-1",
                        skuId = "sku-1",
                        name = "全能图片G-2.0-官方版",
                        fields = listOf(
                            QuickCreationFieldDto(
                                fieldKey = "aspectRatio",
                                mappedApiParamKey = "aspectRatio",
                                fieldType = "LIST",
                                required = true,
                                defaultValue = JsonPrimitive("16:9"),
                                options = listOf(
                                    QuickCreationFieldOptionDto(label = "16:9", value = JsonPrimitive("16:9")),
                                ),
                            ),
                            QuickCreationFieldDto(
                                fieldKey = "image",
                                mappedApiParamKey = "imageUrls",
                                fieldType = "UPLOAD",
                                required = false,
                                maxUploadCount = 2,
                                maxUploadSize = 10485760,
                                multipleInputs = true,
                                skuInputExtraJson = JsonPrimitive("""{"accept":"image/*"}"""),
                            )
                        ),
                    )
                ),
            )
        )

        val flattened = QuickCreationModelMapper.flatten("IMAGE", models)

        assertEquals(1, flattened.size)
        val model = flattened.single()
        assertEquals("IMAGE", model.categoryId)
        assertEquals("全能图片", model.groupName)
        assertEquals("binding-1", model.bindingId)
        assertEquals("sku-1", model.skuId)
        assertEquals("全能图片G-2.0-官方版", model.name)
        val aspectRatio = model.fields.first()
        assertEquals("aspectRatio", aspectRatio.paramKey)
        assertEquals("16:9", aspectRatio.options.single().value)

        val imageField = model.fields[1]
        assertEquals("imageUrls", imageField.paramKey)
        assertEquals(2, imageField.maxUploadCount)
        assertEquals(10485760, imageField.maxUploadSize)
        assertEquals(true, imageField.multipleInputs)
        assertEquals(QuickCreationUploadMediaKind.IMAGE, imageField.uploadMediaKind)
    }

    @Test
    fun `maps service model pricing metadata`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "model",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "model",
                pricing = QuickCreationPricingDto(
                    pricingMode = "default",
                    settlementMode = "cash_only",
                    discountPercent = 100,
                    isFree = false,
                    freeRemaining = 0,
                    isTimeFree = false,
                    promoType = "none",
                ),
            )
        )

        val model = QuickCreationModelMapper.flatten("IMAGE", models).single()

        val pricing = assertNotNull(model.pricing)
        assertEquals("default", pricing.pricingMode)
        assertEquals("cash_only", pricing.settlementMode)
        assertEquals(100, pricing.discountPercent)
        assertEquals(false, pricing.isFree)
        assertEquals(0, pricing.freeRemaining)
        assertEquals(false, pricing.isTimeFree)
        assertEquals("none", pricing.promoType)
    }

    @Test
    fun `maps service field extra json metadata`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "model",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "model",
                fields = listOf(
                    QuickCreationFieldDto(
                        fieldKey = "imageUrls",
                        mappedApiParamKey = "imageUrls",
                        fieldType = "IMAGE",
                        required = true,
                        skuInputExtraJson = JsonPrimitive(
                            """
                            {
                              "title": "Reference image",
                              "titleEn": "Reference image",
                              "paramDesc": "Upload 1-4 reference images",
                              "paramDescEn": "Upload reference images",
                              "placeholder": "Upload image",
                              "accept": "[\"JPG\",\"PNG\",\"WEBP\"]",
                              "maxLength": 20000,
                              "minLength": 1,
                              "maxInpuNum": 4,
                              "ignoreListValueCaseSensitive": true
                            }
                            """.trimIndent()
                        ),
                    )
                ),
            )
        )

        val field = QuickCreationModelMapper.flatten("IMAGE", models).single().fields.single()
        val extra = assertNotNull(field.inputExtra)

        assertEquals("Reference image", extra.title)
        assertEquals("Upload 1-4 reference images", extra.paramDescription)
        assertEquals("Upload image", extra.placeholder)
        assertEquals(listOf("JPG", "PNG", "WEBP"), extra.acceptFormats)
        assertEquals(QuickCreationUploadMediaKind.IMAGE, field.uploadMediaKind)
        assertEquals(20000, extra.maxLength)
        assertEquals(1, extra.minLength)
        assertEquals(4, extra.maxInputCount)
        assertEquals(true, extra.ignoreListValueCaseSensitive)
    }

    @Test
    fun `maps invisible service fields without dropping default values`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "model",
                categoryId = "IMAGE",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "model",
                fields = listOf(
                    QuickCreationFieldDto(
                        fieldKey = "internalMode",
                        mappedApiParamKey = "internalMode",
                        fieldType = "STRING",
                        visible = false,
                        defaultValue = JsonPrimitive("stable"),
                    )
                ),
            )
        )

        val field = QuickCreationModelMapper.flatten("IMAGE", models).single().fields.single()

        assertEquals(false, field.visible)
        assertEquals("stable", field.defaultValue)
    }

    @Test
    fun `maps service field input child list metadata`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "model",
                categoryId = "VIDEO",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "model",
                fields = listOf(
                    QuickCreationFieldDto(
                        fieldKey = "creationMode",
                        mappedApiParamKey = "creationMode",
                        fieldType = "LIST",
                        required = true,
                        skuInputExtraJson = JsonPrimitive(
                            """
                            {
                              "title": "Creation mode",
                              "inputsChildList": [
                                {
                                  "fieldKey": "referenceStrength",
                                  "mappedApiParamKey": "referenceStrength",
                                  "fieldType": "NUMBER",
                                  "required": true,
                                  "visible": true,
                                  "defaultValue": "0.65",
                                  "title": "Reference strength",
                                  "paramDesc": "Controls how strongly the uploaded image is followed",
                                  "placeholder": "0.0-1.0",
                                  "maxLength": 4,
                                  "minLength": 3,
                                  "maxInputCount": 2,
                                  "options": [
                                    {"label": "Low", "value": "0.35"},
                                    {"label": "High", "value": "0.85"}
                                  ],
                                  "showWhen": {
                                    "fieldKey": "creationMode",
                                    "values": ["imageReference"]
                                  }
                                }
                              ]
                            }
                            """.trimIndent()
                        ),
                    )
                ),
            )
        )

        val extra = assertNotNull(
            QuickCreationModelMapper.flatten("VIDEO", models)
                .single()
                .fields
                .single()
                .inputExtra
        )
        val child = extra.inputChildren.single()

        assertEquals("referenceStrength", child.fieldKey)
        assertEquals("referenceStrength", child.paramKey)
        assertEquals("NUMBER", child.fieldType)
        assertEquals(true, child.required)
        assertEquals(true, child.visible)
        assertEquals("0.65", child.defaultValue)
        assertEquals("Reference strength", child.title)
        assertEquals("Controls how strongly the uploaded image is followed", child.paramDescription)
        assertEquals("0.0-1.0", child.placeholder)
        assertEquals(4, child.maxLength)
        assertEquals(3, child.minLength)
        assertEquals(2, child.maxInputCount)
        assertEquals("Low", child.options.first().label)
        assertEquals("0.85", child.options.last().value)
        assertEquals("creationMode", child.visibleWhen?.fieldKey)
        assertEquals(listOf("imageReference"), child.visibleWhen?.values)
    }

    @Test
    fun `infers upload media kind from extra accept metadata`() {
        val models = listOf(
            QuickCreationModelDto(
                type = "model",
                categoryId = "VIDEO",
                bindingId = "binding-1",
                skuId = "sku-1",
                name = "model",
                fields = listOf(
                    QuickCreationFieldDto(
                        fieldKey = "reference",
                        mappedApiParamKey = "reference",
                        fieldType = "UPLOAD",
                        skuInputExtraJson = JsonPrimitive("""{"accept":"audio/*"}"""),
                    )
                ),
            )
        )

        val field = QuickCreationModelMapper.flatten("VIDEO", models).single().fields.single()

        assertEquals(QuickCreationUploadMediaKind.AUDIO, field.uploadMediaKind)
    }
}
