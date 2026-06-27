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
                "current": "1",
                "total": "1"
              }
            }
            """.trimIndent()
        )

        val page = assertNotNull(response.data)
        val card = page.items.single()
        assertEquals(1, page.current)
        assertEquals(1, page.total)
        assertEquals("2067532558576996354", card.id)
        assertEquals("kitten HZ", card.owner?.name)
        assertEquals("https://image.png", card.creationShowreelInfo?.fileUrl)
        assertEquals("2", card.statisticsInfo?.likeCount)
    }

    /**
     * 标签树压平时父标签应保留子孙标签 ID，便于 Presentation 把一级分类展开为可查询的叶子分类。
     */
    @Test
    fun `tag tree response keeps descendant ids on parent tags`() {
        val response = json.decodeFromString<CommunityBaseResponseDto<List<PlazaTagDto>>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": [
                {
                  "id": "parent-video",
                  "rang": "CREATION",
                  "parentId": null,
                  "level": 1,
                  "name": "视频生成",
                  "enable": true,
                  "sort": 3,
                  "childTags": [
                    {
                      "id": "text-to-video",
                      "rang": "CREATION",
                      "parentId": "parent-video",
                      "level": 2,
                      "name": "文生视频",
                      "enable": true,
                      "sort": 1,
                      "childTags": []
                    },
                    {
                      "id": "image-to-video",
                      "rang": "CREATION",
                      "parentId": "parent-video",
                      "level": 2,
                      "name": "图生视频",
                      "enable": true,
                      "sort": 2,
                      "childTags": []
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val tags = assertNotNull(response.data).flatMap { it.flatten() }

        assertEquals(listOf("parent-video", "text-to-video", "image-to-video"), tags.map { it.id })
        assertEquals(listOf("text-to-video", "image-to-video"), tags.first().childIds)
        assertEquals(emptyList(), tags[1].childIds)
    }

    /**
     * 短片列表响应应兼容 explore 抓包中的 composition 字段。
     */
    @Test
    fun `short list response parses explore composition fields`() {
        val response = json.decodeFromString<CommunityBaseResponseDto<PlazaShortPageDto>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": {
                "records": [
                  {
                    "id": "2063090624344010753",
                    "name": "不扫兴的父母：不讲大道理，却让人红了眼",
                    "compositionUrl": "https://video.mp4",
                    "compositionDuration": 42,
                    "thumbnail": "https://thumb.jpg",
                    "categoryCode": "NARRATIVE_SHORT",
                    "categoryName": "叙事短片",
                    "authorName": "selene",
                    "authorAvatar": "https://avatar.png"
                  }
                ],
                "total": 274
              }
            }
            """.trimIndent()
        )

        val page = assertNotNull(response.data)
        val short = page.items.single()
        assertEquals(274, page.total)
        assertEquals("https://video.mp4", short.compositionUrl)
        assertEquals("https://thumb.jpg", short.thumbnail)
        assertEquals(42, short.compositionDuration)
        assertEquals("NARRATIVE_SHORT", short.categoryCode)
        assertEquals("selene", short.authorName)
    }

    /**
     * 短片分类响应应兼容服务端数字 ID，并保留所有分类编码。
     */
    @Test
    fun `short categories parse numeric ids from explore capture`() {
        val response = json.decodeFromString<CommunityBaseResponseDto<List<PlazaShortCategoryDto>>>(
            """
            {
              "code": 0,
              "msg": "success",
              "data": [
                { "code": "ALL", "name": "全部", "nameEn": "ALL" },
                { "id": 8, "code": "HOT", "name": "爆款集锦", "nameEn": "Hot" },
                { "id": 2, "code": "NARRATIVE_SHORT", "name": "叙事短片", "nameEn": "Narrative Short" }
              ]
            }
            """.trimIndent()
        )

        val categories = assertNotNull(response.data)
        assertEquals(listOf("ALL", "HOT", "NARRATIVE_SHORT"), categories.map { it.code })
        assertEquals("8", categories[1].id)
        assertEquals("2", categories[2].id)
    }
}
