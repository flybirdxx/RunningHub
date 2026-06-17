package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuickCreationModelDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `models response keeps group children and dynamic fields`() {
        val response = json.decodeFromString<QuickCreationEnvelopeDto<List<QuickCreationModelDto>>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": [
                {
                  "type": "group",
                  "categoryId": "IMAGE",
                  "name": "全能图片",
                  "children": [
                    {
                      "type": "model",
                      "categoryId": "IMAGE",
                      "bindingId": "2046586338670891013",
                      "skuId": "2046514150500524034",
                      "name": "全能图片G-2.0-官方版",
                      "fields": [
                        {
                          "fieldKey": "aspectRatio",
                          "mappedApiParamKey": "aspectRatio",
                          "fieldType": "LIST",
                          "required": true,
                          "defaultValue": "16:9",
                          "options": [
                            { "label": "16:9", "value": "16:9" },
                            { "label": "1:1", "value": "1:1" }
                          ]
                        },
                        {
                          "fieldKey": "prompt",
                          "mappedApiParamKey": "prompt",
                          "fieldType": "STRING",
                          "required": true
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val group = assertNotNull(response.data).single()
        assertEquals("group", group.type)
        assertEquals("全能图片", group.name)

        val model = group.children.single()
        assertEquals("model", model.type)
        assertEquals("2046586338670891013", model.bindingId)
        assertEquals("2046514150500524034", model.skuId)

        val aspectRatio = model.fields.first()
        assertEquals("aspectRatio", aspectRatio.fieldKey)
        assertEquals("LIST", aspectRatio.fieldType)
        assertEquals("16:9", aspectRatio.defaultValue?.jsonPrimitive?.content)
        assertEquals("1:1", aspectRatio.options[1].value?.jsonPrimitive?.content)
    }
}
