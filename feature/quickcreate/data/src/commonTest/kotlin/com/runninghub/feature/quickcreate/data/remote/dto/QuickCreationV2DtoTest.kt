package com.runninghub.feature.quickcreate.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuickCreationV2DtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `commit request wraps create request under prepare token`() {
        val createRequest = QuickCreationCreateRequestDto(
            bindingId = "2046586338670891013",
            categoryId = "IMAGE",
            skuId = "2046514150500524034",
            params = mapOf(
                "prompt" to JsonPrimitive("测试生成一张极简风格的绿色圆形图标"),
                "aspectRatio" to JsonPrimitive("16:9"),
                "resolution" to JsonPrimitive("2k"),
                "quality" to JsonPrimitive("medium"),
            ),
        )

        val payload = json.encodeToString(
            QuickCreationCommitRequestDto(
                prepareToken = "prepare-token",
                createRequest = createRequest,
            )
        )

        assertTrue(payload.contains(""""prepareToken":"prepare-token""""))
        assertTrue(payload.contains(""""createRequest":{"""))
        assertTrue(payload.contains(""""bindingId":"2046586338670891013""""))
        assertTrue(payload.contains(""""prompt":"测试生成一张极简风格的绿色圆形图标""""))
    }

    @Test
    fun `prepare response parses fee preview and token ttl`() {
        val response = json.decodeFromString<QuickCreationEnvelopeDto<QuickCreationPrepareDataDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "prepareToken": "97b44d8ff8d846ffa3cece953ec981cd",
                "expireAtMillis": "1781713572845",
                "ttlSeconds": 120,
                "skuId": "2046514150500524034",
                "feePreview": {
                  "passed": true,
                  "requiredCashAmount": 0.76,
                  "userCashBalance": 157.136,
                  "cashCurrency": "CNY"
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(0, response.code)
        val data = assertNotNull(response.data)
        assertEquals("97b44d8ff8d846ffa3cece953ec981cd", data.prepareToken)
        assertEquals(120, data.ttlSeconds)
        assertTrue(assertNotNull(data.feePreview).passed)
        assertEquals(0.76, data.feePreview.requiredCashAmount)
        assertEquals("CNY", data.feePreview.cashCurrency)
    }

    @Test
    fun `task list response parses quick creation output records`() {
        val response = json.decodeFromString<QuickCreationEnvelopeDto<QuickCreationTaskPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "page": 1,
                "size": 10,
                "total": 1,
                "list": [
                  {
                    "taskId": "2067282648442818561",
                    "taskStatus": "SUCCESS",
                    "taskCostTime": "66",
                    "taskType": "FAST_WEBAPP_V2",
                    "skuId": "2046514150500524034",
                    "bindingId": "2046586338670891013",
                    "bindingCategoryId": "IMAGE",
                    "apiRequestParams": "{\"prompt\":\"hello\",\"quality\":\"medium\"}",
                    "prepayRecord": {
                      "settlementMode": "cash_only",
                      "rhAmount": 0,
                      "cashAmount": 0.76,
                      "cashCurrency": "CNY",
                      "isFree": 0,
                      "status": "PREPAID"
                    },
                    "outputList": [
                      {
                        "id": "2067282926235770882",
                        "outputName": "result.png",
                        "outputType": "png",
                        "fileUrl": "https://example.com/result.png",
                        "filePreviewUrl": "https://example.com/preview.png",
                        "outputSize": "2048x1152",
                        "auditStatus": 1,
                        "expireTime": "2026-07-02 00:27:12",
                        "expireDays": "13"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val record = assertNotNull(response.data).list.single()
        assertEquals("2067282648442818561", record.taskId)
        assertEquals("SUCCESS", record.taskStatus)
        assertEquals("FAST_WEBAPP_V2", record.taskType)
        assertFalse(record.isRunning)
        assertEquals(0.76, assertNotNull(record.prepayRecord).cashAmount)

        val output = record.outputList.single()
        assertEquals("2067282926235770882", output.id)
        assertEquals("png", output.outputType)
        assertEquals("https://example.com/result.png", output.fileUrl)
        assertEquals("2048x1152", output.outputSize)
    }

    @Test
    fun `inspiration template detail parses captured preset params`() {
        val requestPayload = json.encodeToString(
            QuickCreationInspirationTemplateDetailRequestDto(
                templateId = "2066785582547582978",
            )
        )
        assertEquals("""{"templateId":"2066785582547582978"}""", requestPayload)

        val response = json.decodeFromString<QuickCreationEnvelopeDto<QuickCreationInspirationTemplateDetailDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "templateId": "2066785582547582978",
                "nameCn": "薯片赛场",
                "nameAi": "Snack Stadium",
                "coverUrl": "https://example.com/cover.jpg",
                "categoryId": "VIDEO",
                "videoUrl": "https://example.com/preview.mp4",
                "snapshot": {
                  "presetParams": {
                    "duration": "8",
                    "videoDuration": "8",
                    "realPersonMode": false,
                    "prompt": "薯片人偶踢足球",
                    "promptAi": "Snack mascot kicks a football",
                    "imageUrls": ["https://example.com/ref.png"],
                    "aspectRatio": "3:4",
                    "resolution": "720p",
                    "ratio": "3:4",
                    "generateAudio": false
                  },
                  "sourceTaskId": "2066778680721629185",
                  "snapshotAt": "2026-06-16T15:30:20.909+08:00"
                },
                "bindingId": "2046057458784591873",
                "bindingCnName": "Seedance2.0",
                "apiRequestParamsRaw": "{\"ratio\":\"3:4\",\"prompt\":\"薯片人偶踢足球\"}",
                "skuId": "2132764885651525665",
                "bindingAiName": "Seedance2.0"
              }
            }
            """.trimIndent()
        )

        val detail = assertNotNull(response.data)
        assertEquals("2066785582547582978", detail.templateId)
        assertEquals("VIDEO", detail.categoryId)
        assertEquals("2046057458784591873", detail.bindingId)
        assertEquals("2132764885651525665", detail.skuId)
        val presetParams = assertNotNull(detail.snapshot).presetParams.jsonObject
        assertEquals("薯片人偶踢足球", presetParams.getValue("prompt").jsonPrimitive.content)
        assertEquals("3:4", presetParams.getValue("ratio").jsonPrimitive.content)
        assertEquals(false, presetParams.getValue("generateAudio").jsonPrimitive.boolean)
        assertEquals("https://example.com/ref.png", presetParams.getValue("imageUrls").jsonArray.single().jsonPrimitive.content)
    }
}
