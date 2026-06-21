package com.runninghub.feature.community.data.remote.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Plaza DTO 兼容性测试。
 *
 * 固定服务端创作列表中作者、统计和媒体字段的解析行为，避免迁出 shared 后破坏现有 Plaza 页面数据源。
 */
class PlazaDtoTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /**
     * 创作列表响应应能解析媒体、作者和统计信息。
     */
    @Test
    fun `creation list response parses media and owner`() {
        val response = json.decodeFromString<CommunityBaseResponseDto<PlazaCreationPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": "2067532558576996354",
                    "intro": "portrait",
                    "owner": { "id": "owner-1", "name": "kitten HZ", "avatar": "https://avatar.png" },
                    "statisticsInfo": { "likeCount": "2", "useCount": "76", "collectCount": "1" },
                    "creationShowreelInfo": {
                      "outputId": "out-1",
                      "fileUrl": "https://image.png",
                      "fileType": "png",
                      "imageWidth": 1664,
                      "imageHeight": 2496
                    },
                    "liked": false,
                    "collected": false
                  }
                ],
                "total": 1
              }
            }
            """.trimIndent()
        )

        val card = assertNotNull(response.data).items.single()
        assertEquals("2067532558576996354", card.id)
        assertEquals("kitten HZ", card.owner?.name)
        assertEquals("https://image.png", card.creationShowreelInfo?.fileUrl)
        assertEquals("2", card.statisticsInfo?.likeCount)
    }
}
