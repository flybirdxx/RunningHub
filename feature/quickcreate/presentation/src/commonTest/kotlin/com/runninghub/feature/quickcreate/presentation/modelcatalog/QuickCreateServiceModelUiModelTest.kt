package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServicePricing
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuickCreateServiceModelUiModelTest {
    @Test
    fun `service model ui keeps server order and marks selected item`() {
        val selected = serviceModel(bindingId = "binding-2", skuId = "sku-2", name = "全能图片 G-2.0 官方版")
        val items = listOf(
            serviceModel(bindingId = "binding-1", skuId = "sku-1", name = "Alpha", groupName = "图像"),
            selected,
        ).toQuickCreateServiceModelUiItems(selected)

        assertEquals(listOf("binding-1|sku-1", "binding-2|sku-2"), items.map { it.identityKey })
        assertFalse(items.first().selected)
        assertTrue(items.last().selected)
        assertEquals("全能图片 G-2.0", items.last().compactName)
    }

    @Test
    fun `service model ui normalizes all power image G2 route names for compact entry`() {
        val textToImage = serviceModel(name = "全能图片G-2-文生图-官方稳定版").toQuickCreateServiceModelUi()
        val imageToImage = serviceModel(name = "全能图片G-2-图生图-官方稳定版").toQuickCreateServiceModelUi()
        val lowPrice = serviceModel(name = "全能图片G-2.0-文生图-低价渠道版").toQuickCreateServiceModelUi()

        assertEquals("全能图片 G-2.0", textToImage.compactName)
        assertEquals("全能图片 G-2.0", imageToImage.compactName)
        assertEquals("全能图片 G-2.0", lowPrice.compactName)
    }

    @Test
    fun `service model ui uses stable text semantics for local fallback copy`() {
        val model = serviceModel(
            name = "",
            groupName = null,
            fields = listOf(serviceField("prompt"), serviceField("style")),
        ).toQuickCreateServiceModelUi()

        assertEquals(QuickCreateServiceModelDisplayName.Unnamed, model.displayName)
        assertEquals(QuickCreateServiceModelGroupTitle.Other, model.groupTitle)
        assertEquals(QuickCreateServiceModelSubtitle.ParameterCount(2), model.subtitle)
    }

    @Test
    fun `service model subtitle counts only documented seedance params`() {
        val model = serviceModel(
            categoryId = "VIDEO",
            groupName = "Seedance2.0",
            name = "seedance2.0-Mini/多模态视频",
            apiType = "reference-to-video",
            fields = listOf(
                serviceField("duration"),
                serviceField("realPersonMode"),
                serviceField("conversionSlots"),
            ),
        ).toQuickCreateServiceModelUi()

        assertEquals(
            QuickCreateServiceModelSubtitle.GroupAndParameterCount("Seedance2.0", 2),
            model.subtitle,
        )
    }

    @Test
    fun `compact label keeps selected model and avoids fallback while catalog is loading`() {
        val model = serviceModel(name = "示例模型").toQuickCreateServiceModelUi()
        val g2Model = serviceModel(name = "全能图片G-2-文生图-官方稳定版").toQuickCreateServiceModelUi()

        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("示例模型"),
            quickCreateCompactServiceModelLabel(model, loading = true),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("示例模型"),
            quickCreateCompactServiceModelLabel(model, loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("全能图片 G-2.0"),
            quickCreateCompactServiceModelLabel(g2Model, loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.Unavailable,
            quickCreateCompactServiceModelLabel(null, loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.Loading,
            quickCreateCompactServiceModelLabel(null, loading = true),
        )
    }

    @Test
    fun `compact label uses same short family name before and after catalog loads`() {
        val model = serviceModel(
            name = "全能图片G-2.0-文生图-官方版",
            groupName = "全能图片G-2.0-官方版",
        ).toQuickCreateServiceModelUi()

        assertEquals("全能图片 G-2.0", model.compactName)
        assertEquals(
            QuickCreateCompactServiceModelLabel.Loading,
            quickCreateCompactServiceModelLabel(
                model = null,
                loading = true,
            ),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("全能图片 G-2.0"),
            quickCreateCompactServiceModelLabel(
                model = model,
                loading = false,
            ),
        )
    }

    @Test
    fun `same service model compares binding and sku together`() {
        val base = serviceModel(bindingId = "binding", skuId = "sku-a")

        assertTrue(base.isSameQuickCreationServiceModel(serviceModel(bindingId = "binding", skuId = "sku-a")))
        assertFalse(base.isSameQuickCreationServiceModel(serviceModel(bindingId = "binding", skuId = "sku-b")))
    }

    @Test
    fun `model card semantics expose capability scene price and technical tags without ui reading source`() {
        val model = serviceModel(
            categoryId = "VIDEO",
            groupName = "短视频",
            name = "Seedance2.0-Mini/text-to-video",
            apiType = "text-to-video",
            apiSource = "bytedance",
            pricing = QuickCreationServicePricing(priceSummaryRaw = "12.0000 RHB"),
        ).toQuickCreateServiceModelUi(selected = true)

        assertEquals(QuickCreateServiceModelKind.Video, model.kind)
        assertEquals(QuickCreateServiceModelScene.VideoGeneration, model.scene)
        assertEquals(QuickCreateServiceModelPrice.Known("12 RHB"), model.price)
        assertEquals(listOf("text-to-video", "bytedance"), model.technicalTags)
        assertTrue(model.selected)
    }

    @Test
    fun `model ui keeps target tab from catalog source instead of inferred capability`() {
        val item = listOf(
            serviceModel(
                categoryId = "CUSTOM",
                name = "实验模型",
                apiType = "unknown",
            ),
        ).toQuickCreateServiceModelUiItems(
            selected = null,
            targetTab = QuickCreateTab.IMAGE,
        ).single()

        assertEquals(QuickCreateServiceModelKind.Other, item.kind)
        assertEquals(QuickCreateTab.IMAGE, item.targetTab)
    }

    @Test
    fun `model picker state filters by query and category while distinguishing empty reasons`() {
        val imageModel = serviceModel(
            categoryId = "IMAGE",
            bindingId = "image-binding",
            skuId = "image-sku",
            name = "全能图片G-2.0-文生图-官方版",
            apiType = "text-to-image",
        ).toQuickCreateServiceModelUi(selected = true)
        val videoModel = serviceModel(
            categoryId = "VIDEO",
            bindingId = "video-binding",
            skuId = "video-sku",
            name = "Seedance2.0-Mini/text-to-video",
            apiType = "text-to-video",
        ).toQuickCreateServiceModelUi()

        val videoOnly = quickCreateModelPickerState(
            QuickCreateUiState(
                serviceImageModelItems = listOf(imageModel),
                serviceVideoModelItems = listOf(videoModel),
                modelPickerQuery = "Seedance",
                modelPickerFilter = QuickCreateModelPickerFilter.Video,
            ),
        )
        val noSearchResult = quickCreateModelPickerState(
            QuickCreateUiState(
                serviceImageModelItems = listOf(imageModel),
                serviceVideoModelItems = listOf(videoModel),
                modelPickerQuery = "不存在",
                modelPickerFilter = QuickCreateModelPickerFilter.Image,
            ),
        )
        val emptyCatalog = quickCreateModelPickerState(
            QuickCreateUiState(
                modelPickerFilter = QuickCreateModelPickerFilter.Image,
            ),
        )

        assertEquals(listOf("video-binding|video-sku"), videoOnly.visibleItems.map { it.identityKey })
        assertEquals(QuickCreateModelPickerEmptyReason.SearchNoResult, noSearchResult.emptyReason)
        assertEquals(QuickCreateModelPickerEmptyReason.EmptyCatalog, emptyCatalog.emptyReason)
    }
}

private fun serviceModel(
    bindingId: String = "binding",
    skuId: String = "sku",
    name: String = "模型",
    groupName: String? = null,
    categoryId: String = "IMAGE",
    apiType: String? = null,
    apiSource: String? = null,
    fields: List<com.runninghub.feature.quickcreate.domain.QuickCreationServiceField> = emptyList(),
    pricing: QuickCreationServicePricing? = null,
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = categoryId,
        groupName = groupName,
        bindingId = bindingId,
        skuId = skuId,
        name = name,
        description = null,
        apiType = apiType,
        apiSource = apiSource,
        fields = fields,
        pricing = pricing,
    )

private fun serviceField(paramKey: String): com.runninghub.feature.quickcreate.domain.QuickCreationServiceField =
    com.runninghub.feature.quickcreate.domain.QuickCreationServiceField(
        fieldKey = paramKey,
        paramKey = paramKey,
        fieldType = "TEXT",
        required = false,
        defaultValue = null,
        options = emptyList(),
    )
