package com.runninghub.app.ui.feature.plaza

import com.runninghub.feature.community.domain.PlazaShortCategory
import com.runninghub.feature.community.domain.PlazaTag
import kotlin.test.Test
import kotlin.test.assertEquals

class PlazaScreenVisibilityTest {
    @Test
    fun `creation category row keeps top level server tags`() {
        val tags = (1..12).map { index ->
            PlazaTag(id = "tag-$index", name = "Tag $index", level = 1, enable = true)
        }

        val visibleTags = plazaVisibleCreationTags(tags)

        assertEquals(tags.map { it.id }, visibleTags.map { it.id })
    }

    @Test
    fun `creation category row hides child tags when parent contains children`() {
        val tags = listOf(
            PlazaTag(id = "digital-human-parent", name = "数字人", level = 1, childIds = listOf("digital-human-child")),
            PlazaTag(id = "digital-human-child", name = "数字人", level = 2),
            PlazaTag(
                id = "image-generation-parent",
                name = "图片生成",
                level = 1,
                childIds = listOf("text-to-image", "image-to-image"),
            ),
            PlazaTag(id = "text-to-image", name = "文生图", level = 2),
            PlazaTag(id = "image-to-image", name = "图生图", level = 2),
            PlazaTag(id = "video-effect", name = "视频特效", level = 1),
        )

        val visibleTags = plazaVisibleCreationTags(tags)

        assertEquals(
            listOf("digital-human-parent", "image-generation-parent", "video-effect"),
            visibleTags.map { it.id },
        )
    }

    @Test
    fun `short category row keeps all server categories`() {
        val categories = (1..10).map { index ->
            PlazaShortCategory(id = "$index", code = "CATEGORY_$index", name = "Category $index")
        }

        val visibleCategories = plazaVisibleShortCategories(categories)

        assertEquals(categories.map { it.code }, visibleCategories.map { it.code })
    }

    @Test
    fun `sort dropdown exposes recommendation sort options`() {
        val options = plazaSortOptions(
            recommendLabel = "推荐",
            hotLabel = "最热",
            latestLabel = "最新",
        )

        assertEquals(
            listOf("RECOMMEND", "HOT", "LATEST"),
            options.map { it.value },
        )
    }

    @Test
    fun `sort dropdown label uses selected sort`() {
        val options = plazaSortOptions(
            recommendLabel = "推荐",
            hotLabel = "最热",
            latestLabel = "最新",
        )

        val label = plazaSelectedSortLabel(
            selectedSort = "HOT",
            options = options,
        )

        assertEquals("最热", label)
    }
}
