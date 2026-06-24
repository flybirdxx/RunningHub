package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
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
    fun `compact label keeps selected or fallback model while catalog is loading`() {
        val model = serviceModel(name = "示例模型").toQuickCreateServiceModelUi()
        val g2Model = serviceModel(name = "全能图片G-2-文生图-官方稳定版").toQuickCreateServiceModelUi()

        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("示例模型"),
            quickCreateCompactServiceModelLabel(model, fallback = "本地", loading = true),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("示例模型"),
            quickCreateCompactServiceModelLabel(model, fallback = "本地", loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.ModelName("全能图片 G-2.0"),
            quickCreateCompactServiceModelLabel(g2Model, fallback = "全能图片 G-2.0", loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.FallbackName("本地"),
            quickCreateCompactServiceModelLabel(null, fallback = "本地", loading = false),
        )
        assertEquals(
            QuickCreateCompactServiceModelLabel.FallbackName("本地"),
            quickCreateCompactServiceModelLabel(null, fallback = "本地", loading = true),
        )
    }

    @Test
    fun `same service model compares binding and sku together`() {
        val base = serviceModel(bindingId = "binding", skuId = "sku-a")

        assertTrue(base.isSameQuickCreationServiceModel(serviceModel(bindingId = "binding", skuId = "sku-a")))
        assertFalse(base.isSameQuickCreationServiceModel(serviceModel(bindingId = "binding", skuId = "sku-b")))
    }
}

private fun serviceModel(
    bindingId: String = "binding",
    skuId: String = "sku",
    name: String = "模型",
    groupName: String? = null,
    fields: List<com.runninghub.feature.quickcreate.domain.QuickCreationServiceField> = emptyList(),
): QuickCreationServiceModel =
    QuickCreationServiceModel(
        categoryId = "IMAGE",
        groupName = groupName,
        bindingId = bindingId,
        skuId = skuId,
        name = name,
        description = null,
        fields = fields,
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
