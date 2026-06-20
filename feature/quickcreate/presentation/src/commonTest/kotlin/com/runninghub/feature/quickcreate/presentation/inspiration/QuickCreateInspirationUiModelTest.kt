package com.runninghub.feature.quickcreate.presentation.inspiration

import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTag
import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class QuickCreateInspirationUiModelTest {

    @Test
    fun `tag mapper marks only first tag selected`() {
        val tags = listOf(
            QuickCreateInspirationTag(id = "hot", name = "热门"),
            QuickCreateInspirationTag(id = "new", name = "新品"),
        )

        val items = tags.toQuickCreateInspirationTagUiItems()

        assertEquals(listOf(true, false), items.map { it.selected })
        assertEquals(listOf("热门", "新品"), items.map { it.label })
    }

    @Test
    fun `template mapper prefers video preview before image cover`() {
        val template = inspirationTemplate(
            coverUrl = "https://example.com/cover.png",
            videoUrl = "https://example.com/preview.mp4",
            tagHot = true,
            tagNew = true,
        )

        val item = template.toQuickCreateInspirationTemplateUi()

        val preview = assertIs<QuickCreateInspirationPreviewUi.Video>(item.preview)
        assertEquals("https://example.com/preview.mp4", preview.url)
        // 服务端可以同时标记热门和新品，映射层保持 HOT 在前，保证卡片徽标顺序稳定。
        assertEquals(
            listOf(QuickCreateInspirationBadgeTone.HOT, QuickCreateInspirationBadgeTone.NEW),
            item.badges.map { it.tone },
        )
    }

    @Test
    fun `template mapper creates video placeholder from category`() {
        val template = inspirationTemplate(
            categoryId = "video",
            coverUrl = null,
            videoUrl = null,
        )

        val item = template.toQuickCreateInspirationTemplateUi()

        val preview = assertIs<QuickCreateInspirationPreviewUi.Placeholder>(item.preview)
        assertEquals(QuickCreateInspirationPlaceholderMediaType.VIDEO, preview.mediaType)
    }

    private fun inspirationTemplate(
        categoryId: String? = "IMAGE",
        coverUrl: String? = "https://example.com/cover.png",
        videoUrl: String? = null,
        tagHot: Boolean = false,
        tagNew: Boolean = false,
    ): QuickCreateInspirationTemplate =
        QuickCreateInspirationTemplate(
            templateId = "template_1",
            title = "灵感模板",
            categoryId = categoryId,
            coverUrl = coverUrl,
            videoUrl = videoUrl,
            tagHot = tagHot,
            tagNew = tagNew,
        )
}
