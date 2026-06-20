package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelFieldType
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiModelFieldMapperTest {
    private val mapper = ApiModelFieldMapper(
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    )

    @Test
    fun `maps string list and image fields from input config json`() {
        val fields = mapper.parse(
            """
            [
              {
                "fieldKey": "prompt",
                "mappedApiParamKey": "prompt",
                "type": "STRING",
                "required": true,
                "title": "Prompt",
                "defaultValue": "a cat",
                "minLength": 1,
                "maxLength": 2000
              },
              {
                "fieldKey": "aspectRatio",
                "type": "LIST",
                "required": false,
                "defaultValue": "16:9",
                "options": [
                  { "label": "16:9", "value": "16:9" },
                  { "label": "1:1", "apiValue": "1:1" }
                ]
              },
              {
                "fieldKey": "imageUrls",
                "type": "IMAGE",
                "required": true,
                "multipleInputs": true,
                "maxUploadCount": 4,
                "maxUploadSize": 52428800,
                "accept": ["JPG", "PNG"]
              }
            ]
            """.trimIndent()
        )

        assertEquals(3, fields.size)
        assertEquals(ApiModelFieldType.STRING, fields[0].type)
        assertEquals("prompt", fields[0].paramKey)
        assertEquals(true, fields[0].required)
        assertEquals(2000, fields[0].maxLength)
        assertEquals(ApiModelFieldType.LIST, fields[1].type)
        assertEquals("1:1", fields[1].options[1].value)
        assertEquals(ApiModelFieldType.IMAGE, fields[2].type)
        assertEquals(true, fields[2].multipleInputs)
        assertEquals(4, fields[2].maxUploadCount)
        assertEquals(listOf("JPG", "PNG"), fields[2].acceptFormats)
    }

    @Test
    fun `unknown or malformed input config returns empty list`() {
        assertEquals(emptyList(), mapper.parse(null))
        assertEquals(emptyList(), mapper.parse(""))
        assertEquals(emptyList(), mapper.parse("{not-json"))
    }
}
