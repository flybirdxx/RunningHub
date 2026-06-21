package com.runninghub.feature.model.data.repository

import com.runninghub.feature.model.data.remote.dto.SkuDetailDto
import com.runninghub.feature.model.data.remote.dto.SkuSummaryDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelCatalogRepositoryMapperTest {
    private val mapper = ApiModelFieldMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun `maps sku summary to domain summary`() {
        val registry = ModelEndpointRegistry()
        val summary = SkuSummaryDto(
            id = "sku-1",
            name = "Image V2",
            type = "text-to-image",
            groupName = "Image",
            source = "rh-ai",
            price = "0.16 CNY/run",
            rhEndpoint = "/rhart-image/text-to-image",
        ).also {
            registry.register(it.id, it.rhEndpoint)
        }.toDomain()

        assertEquals("sku-1", summary.id)
        assertEquals("Image V2", summary.name)
        assertEquals("text-to-image", summary.type)
        assertEquals("0.16 CNY/run", summary.priceSummary)
        assertEquals("/rhart-image/text-to-image", registry.get("sku-1"))
    }

    @Test
    fun `maps sku detail with parsed fields`() {
        val registry = ModelEndpointRegistry()
        val detail = SkuDetailDto(
            id = "sku-1",
            name = "Image V2",
            rhEndpoint = "/rhart-image/text-to-image",
            inputConfigJson = """[{"fieldKey":"prompt","type":"STRING","required":true}]""",
        ).also {
            registry.register(it.id, it.rhEndpoint)
        }.toDomain(mapper)

        assertEquals("sku-1", detail.id)
        assertEquals("prompt", detail.fields.single().fieldKey)
        assertEquals("/rhart-image/text-to-image", registry.get("sku-1"))
    }
}
