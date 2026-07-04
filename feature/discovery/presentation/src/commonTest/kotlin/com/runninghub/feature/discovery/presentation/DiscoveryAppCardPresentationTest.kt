package com.runninghub.feature.discovery.presentation

import com.runninghub.core.model.Author
import com.runninghub.core.model.CoverMediaType
import com.runninghub.core.model.WebApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoveryAppCardPresentationTest {
    @Test
    fun `featured maps from carefullyChosen`() {
        val chosen = webApp(id = "chosen").copy(carefullyChosen = true)
        val plain = webApp(id = "plain").copy(carefullyChosen = false)

        assertTrue(chosen.toDiscoveryAppCardUiModel().featured)
        assertEquals(false, plain.toDiscoveryAppCardUiModel().featured)
    }

    @Test
    fun `metrics lead with use count and append like count`() {
        val webApp = webApp(id = "metrics").copy(useCount = "42", pv = "1000", likeCount = "7")

        val metrics = webApp.toDiscoveryAppCardUiModel().metrics

        assertEquals(DiscoveryAppCardMetricKind.USE_COUNT, metrics.first().kind)
        assertEquals("42", metrics.first().value)
        assertEquals(DiscoveryAppCardMetricKind.LIKE_COUNT, metrics.last().kind)
        assertEquals("7", metrics.last().value)
        assertEquals(2, metrics.size)
    }

    @Test
    fun `metrics fall back to view count when use count blank`() {
        val webApp = webApp(id = "pv").copy(useCount = "", pv = "500", likeCount = "")

        val metrics = webApp.toDiscoveryAppCardUiModel().metrics

        assertEquals(1, metrics.size)
        assertEquals(DiscoveryAppCardMetricKind.VIEW_COUNT, metrics.single().kind)
        assertEquals("500", metrics.single().value)
    }

    @Test
    fun `ui model exposes only real metrics and featured flag`() {
        // 编译期契约：模型仅保留真实指标与精选标记，虚构费用字段已删除。
        // 构造使用具名参数覆盖全部字段；若模型仍保留 estimatedCost/primaryAction，此处将无法编译。
        val model = DiscoveryAppCardUiModel(
            id = "id",
            templateName = "name",
            capability = DiscoveryAppCapability.GENERAL,
            preview = DiscoveryAppPreviewUi(url = null, type = DiscoveryAppPreviewType.EMPTY),
            metrics = emptyList(),
            featured = false,
        )

        assertTrue(model.metrics.isEmpty())
        assertEquals(false, model.featured)
    }

    private fun webApp(id: String): WebApp = WebApp(
        id = id,
        title = "App $id",
        description = null,
        thumbnailUrl = null,
        coverUrl = "https://cdn.example.com/$id.png",
        coverMediaType = CoverMediaType.IMAGE,
        coverWidth = null,
        coverHeight = null,
        author = Author(
            id = "author",
            name = "Author",
            avatar = null,
            intro = null,
            followCount = "0",
            fansCount = "0",
            likeCount = "0",
            collectCount = "0",
            bgImage = null,
        ),
        tags = emptyList(),
        likeCount = "0",
        collectCount = "0",
        useCount = "0",
        pv = "0",
    )
}
