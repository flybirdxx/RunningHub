package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.SkuDetailDto
import com.runninghub.shared.data.remote.dto.SkuSummaryDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelCatalogRepositoryMapperTest {
    private val mapper = ApiModelFieldMapper(Json { ignoreUnknownKeys = true })

    @Test
    fun `maps sku summary to domain summary`() {
        val summary = SkuSummaryDto(
            id = "sku-1",
            name = "Image V2",
            type = "text-to-image",
            groupName = "Image",
            source = "rh-ai",
            price = "0.16 CNY/run",
            rhEndpoint = "/rhart-image/text-to-image",
        ).toDomain()

        assertEquals("sku-1", summary.id)
        assertEquals("Image V2", summary.name)
        assertEquals("text-to-image", summary.type)
        assertEquals("/rhart-image/text-to-image", summary.endpoint)
        assertEquals("0.16 CNY/run", summary.priceSummary)
    }

    @Test
    fun `maps sku detail with parsed fields`() {
        val detail = SkuDetailDto(
            id = "sku-1",
            name = "Image V2",
            rhEndpoint = "/rhart-image/text-to-image",
            inputConfigJson = """[{"fieldKey":"prompt","type":"STRING","required":true}]""",
        ).toDomain(mapper)

        assertEquals("sku-1", detail.id)
        assertEquals("/rhart-image/text-to-image", detail.endpoint)
        assertEquals("prompt", detail.fields.single().fieldKey)
    }
}
