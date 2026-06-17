package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationFieldDto
import com.runninghub.shared.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.shared.data.remote.dto.QuickCreationModelDto
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals("aspectRatio", model.fields.single().paramKey)
        assertEquals("16:9", model.fields.single().options.single().value)
    }
}
