package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.repository.ImageGenerationRequest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickCreationV2DefaultsTest {
    @Test
    fun `default image g2 request uses captured web binding and sku ids`() {
        val request = QuickCreationV2Defaults.imageG2CreateRequest(
            ImageGenerationRequest(
                prompt = "绿色圆形图标",
                model = "all-power-image-g2",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
            )
        )

        assertEquals("2046586338670891013", request.bindingId)
        assertEquals("IMAGE", request.categoryId)
        assertEquals("2046514150500524034", request.skuId)
        assertEquals(JsonPrimitive("绿色圆形图标"), request.params["prompt"])
        assertEquals(JsonPrimitive("16:9"), request.params["aspectRatio"])
        assertEquals(JsonPrimitive("2k"), request.params["resolution"])
        assertEquals(JsonPrimitive("medium"), request.params["quality"])
    }

    @Test
    fun `image reference request sends imageUrls array`() {
        val request = QuickCreationV2Defaults.imageG2CreateRequest(
            ImageGenerationRequest(
                prompt = "参考图片生成",
                model = "all-power-image-g2",
                aspectRatio = "1:1",
                resolution = "2k",
                quality = "high",
                referenceImageUri = "https://example.com/reference.png",
            )
        )

        assertEquals(
            listOf("https://example.com/reference.png"),
            request.imageUrls()
        )
    }

    @Test
    fun `service params are merged into image v2 request`() {
        val request = QuickCreationV2Defaults.imageG2CreateRequest(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-g2",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
                quickCreationParams = mapOf(
                    "aspectRatio" to "1:1",
                    "resolution" to "4k",
                    "style" to "photoreal",
                ),
            )
        )

        assertEquals(JsonPrimitive("green icon"), request.params["prompt"])
        assertEquals(JsonPrimitive("16:9"), request.params["aspectRatio"])
        assertEquals(JsonPrimitive("2k"), request.params["resolution"])
        assertEquals(JsonPrimitive("medium"), request.params["quality"])
        assertEquals(JsonPrimitive("photoreal"), request.params["style"])
    }
}
