package com.runninghub.feature.model.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApiModelDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `sku list response parses model card fields`() {
        val response = json.decodeFromString<BaseResponseDto<SkuListPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": "2046514150500524034",
                    "name": "Image G-2 Text To Image Official",
                    "type": "text-to-image",
                    "source": "rh-ai",
                    "price": "0.06 CNY/run"
                  }
                ],
                "total": 1
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).records.single()
        assertEquals("2046514150500524034", record.id)
        assertEquals("Image G-2 Text To Image Official", record.name)
        assertEquals("text-to-image", record.type)
        assertEquals("rh-ai", record.source)
        assertEquals("0.06 CNY/run", record.price)
    }

    @Test
    fun `sku list response parses web page wrapper records`() {
        val response = json.decodeFromString<BaseResponseDto<SkuListPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "page": {
                  "records": [
                    {
                      "id": 2046514150500524034,
                      "name": "All Power Image G-2 Text To Image Official",
                      "type": "text-to-image",
                      "source": "rh-ai",
                      "price": { "skuId": "2046514150500524034", "priceType": 1 },
                      "rhEndpoint": "/openapi/v2/rhart-image-g-2-official/text-to-image",
                      "priceSummary": "0.06 CNY/run"
                    }
                  ],
                  "pages": 1
                }
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).items.single()
        assertEquals("2046514150500524034", record.id)
        assertEquals("All Power Image G-2 Text To Image Official", record.name)
        assertEquals("/openapi/v2/rhart-image-g-2-official/text-to-image", record.rhEndpoint)
        assertEquals("{\"skuId\":\"2046514150500524034\",\"priceType\":1}", record.price)
        assertEquals("0.06 CNY/run", record.priceSummary)
    }

    @Test
    fun `sku list response parses category and source aliases from production payload`() {
        val response = json.decodeFromString<BaseResponseDto<SkuListPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "page": {
                  "records": [
                    {
                      "id": "2004543847939751938",
                      "name": "全能图片PRO-文生图-低价渠道版",
                      "categoryName": "text-to-image",
                      "sourceTypeName": "rh-ai",
                      "categoryType": "STANDARD_MODEL"
                    }
                  ],
                  "total": "1"
                }
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).items.single()
        assertEquals("text-to-image", record.type)
        assertEquals("rh-ai", record.source)
    }

    @Test
    fun `sku tag query response parses standard model groups`() {
        val response = json.decodeFromString<BaseResponseDto<List<SkuTagDto>>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": [
                {
                  "id": 438,
                  "parentId": 4,
                  "level": 1,
                  "name": "Seedance",
                  "nameEn": "Seedance Models",
                  "apiCount": 10
                }
              ]
            }
            """.trimIndent()
        )

        val tag = assertNotNull(response.data).single()
        assertEquals("438", tag.id)
        assertEquals("4", tag.parentId)
        assertEquals("Seedance", tag.name)
        assertEquals("Seedance Models", tag.nameEn)
        assertEquals(10, tag.apiCount)
    }

    @Test
    fun `sku list response parses mixed tag formats`() {
        val response = json.decodeFromString<BaseResponseDto<SkuListPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": 2044235717485551618,
                    "name": "Seedance Video",
                    "tags": [
                      "text-to-video|bytedance",
                      { "name": "Seedance2.0" },
                      { "tagName": "最近上新" }
                    ]
                  }
                ],
                "total": 1
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).items.single()
        assertEquals(
            listOf("text-to-video", "bytedance", "Seedance2.0", "最近上新"),
            record.tags,
        )
    }

    @Test
    fun `sku detail response keeps endpoint and input config json`() {
        val response = json.decodeFromString<BaseResponseDto<SkuDetailDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "id": "2027192837726294017",
                "name": "Image V2 Text To Image Low Price",
                "rhEndpoint": "/rhart-image-n-g31-flash/text-to-image",
                "inputConfigJson": "[{\"fieldKey\":\"prompt\",\"type\":\"STRING\",\"required\":true}]",
                "queueSize": 1000,
                "concurrencyLimit": 500
              }
            }
            """.trimIndent()
        )

        val detail = assertNotNull(response.data)
        assertEquals("/rhart-image-n-g31-flash/text-to-image", detail.rhEndpoint)
        assertEquals(1000, detail.queueSize)
        assertEquals(500, detail.concurrencyLimit)
        assertEquals(true, detail.inputConfigJson?.contains("prompt"))
    }
}





