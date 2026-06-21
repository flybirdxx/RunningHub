package com.runninghub.feature.discovery.data.remote.dto

import com.runninghub.core.model.CoverMediaType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WebApp 目录 mapper 测试。
 *
 * 重点覆盖封面媒体类型和缩略图降级规则，避免迁移模块后列表卡片把视频地址当图片展示。
 */
class WebAppCatalogMappersTest {
    /**
     * 视频封面应使用缩略图作为列表封面并保留原始视频地址。
     */
    @Test
    fun `video cover maps thumbnail to cover and keeps video url`() {
        val app = WebAppCatalogDto(
            id = "app-1",
            title = "Video app",
            covers = listOf(
                WebAppCoverDto(
                    url = "https://example.com/demo.mp4?token=ignored",
                    thumbnailUri = "https://example.com/demo.jpg",
                )
            ),
        ).toDomain()

        assertEquals(CoverMediaType.VIDEO, app.coverMediaType)
        assertEquals("https://example.com/demo.jpg", app.coverUrl)
        assertEquals("https://example.com/demo.mp4?token=ignored", app.videoUrl)
    }

    /**
     * 标签树映射应限制递归深度，防止异常深层数据拖垮页面状态构建。
     */
    @Test
    fun `tag tree mapper respects max depth`() {
        val tag = CatalogTagDto(
            id = "root",
            name = "Root",
            childTags = listOf(CatalogTagDto(id = "child", name = "Child")),
        ).toDomain(maxDepth = 0)

        assertEquals(null, tag.childTags)
    }
}
