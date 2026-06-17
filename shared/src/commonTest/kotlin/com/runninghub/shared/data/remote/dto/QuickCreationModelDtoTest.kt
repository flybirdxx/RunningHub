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

    @Test
    fun `models catalog response parses category map`() {
        val response = json.decodeFromString<QuickCreationEnvelopeDto<QuickCreationModelCatalogDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "categoryMeta": [
                  { "key": "IMAGE", "name": "图片创作", "sort": 1 }
                ],
                "categories": {
                  "IMAGE": [
                    {
                      "type": "group",
                      "groupName": "全能图片G-2.0-官方版",
                      "children": [
                        {
                          "type": "model",
                          "bindingId": "2046586338670891013",
                          "skuId": "2046514150500524034",
                          "name": "全能图片G-2.0-文生图-官方版",
                          "fields": [
                            {
                              "fieldKey": "resolution",
                              "mappedApiParamKey": "resolution",
                              "fieldType": "LIST",
                              "defaultValue": "2k",
                              "options": [
                                { "value": "1k", "label": "1k" },
                                { "value": "2k", "label": "2k" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val catalog = assertNotNull(response.data)
        assertEquals("图片创作", catalog.categoryMeta.single().name)
        val imageGroup = catalog.categories.getValue("IMAGE").single()
        assertEquals("全能图片G-2.0-官方版", imageGroup.groupName)
        assertEquals("2046586338670891013", imageGroup.children.single().bindingId)
    }

    @Test
    fun `model response parses pricing metadata`() {
        val response = json.decodeFromString<QuickCreationEnvelopeDto<QuickCreationModelCatalogDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "categories": {
                  "IMAGE": [
                    {
                      "type": "model",
                      "bindingId": "binding-1",
                      "skuId": "sku-1",
                      "name": "model",
                      "pricing": {
                        "pricingMode": "default",
                        "settlementMode": "cash_only",
                        "discountPercent": 100,
                        "isFree": false,
                        "freeRemaining": 0,
                        "isTimeFree": false,
                        "promoType": "none"
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val pricing = assertNotNull(response.data?.categories?.getValue("IMAGE")?.single()?.pricing)
        assertEquals("default", pricing.pricingMode)
        assertEquals("cash_only", pricing.settlementMode)
        assertEquals(100, pricing.discountPercent)
        assertEquals(false, pricing.isFree)
        assertEquals(0, pricing.freeRemaining)
        assertEquals(false, pricing.isTimeFree)
        assertEquals("none", pricing.promoType)
    }
}
