package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationFieldDto
import com.runninghub.shared.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.shared.data.remote.dto.QuickCreationModelDto
import com.runninghub.shared.data.remote.dto.QuickCreationPricingDto
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
        assertEquals("""{"accept":"image/*"}""", imageField.inputExtraJson)
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
}
