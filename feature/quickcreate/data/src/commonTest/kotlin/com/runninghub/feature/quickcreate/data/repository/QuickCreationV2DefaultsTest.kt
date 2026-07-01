package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
    fun `service params override local image v2 fallbacks`() {
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
                    "prompt" to "",
                    "style" to "photoreal",
                ),
            )
        )

        assertEquals(JsonPrimitive("green icon"), request.params["prompt"])
        assertEquals(JsonPrimitive("1:1"), request.params["aspectRatio"])
        assertEquals(JsonPrimitive("4k"), request.params["resolution"])
        assertEquals(JsonPrimitive("medium"), request.params["quality"])
        assertEquals(JsonPrimitive("photoreal"), request.params["style"])
    }

    @Test
    fun `service image v2 request only sends declared quick creation params`() {
        val request = QuickCreationV2Defaults.imageG2CreateRequest(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-g2",
                aspectRatio = "16:9",
                resolution = "2k",
                quality = "medium",
                quickCreationBindingId = "image-binding",
                quickCreationSkuId = "image-sku",
                quickCreationParams = mapOf("style" to "photoreal"),
            )
        )

        assertEquals(JsonPrimitive("green icon"), request.params["prompt"])
        assertEquals(JsonPrimitive("photoreal"), request.params["style"])
        assertNull(request.params["aspectRatio"])
        assertNull(request.params["resolution"])
        assertNull(request.params["quality"])
    }

    @Test
    fun `service list params are merged as json arrays`() {
        val request = QuickCreationV2Defaults.imageG2CreateRequest(
            ImageGenerationRequest(
                prompt = "green icon",
                model = "all-power-image-g2",
                quickCreationListParams = mapOf(
                    "referenceImages" to listOf(
                        "https://example.com/a.png",
                        "https://example.com/b.png",
                    ),
                ),
            )
        )

        val values = request.params["referenceImages"] as kotlinx.serialization.json.JsonArray
        assertEquals("https://example.com/a.png", values[0].jsonPrimitive.content)
        assertEquals("https://example.com/b.png", values[1].jsonPrimitive.content)
    }

    @Test
    fun `video v2 request uses service ids and captured multimodal params`() {
        val request = QuickCreationV2Defaults.videoCreateRequest(
            VideoGenerationRequest(
                prompt = "薯片人偶踢足球",
                model = "seedance2",
                aspectRatio = "3:4",
                duration = 8,
                resolution = "720p",
                generateAudio = false,
                quickCreationCategoryId = "VIDEO",
                quickCreationBindingId = "2046057458784591873",
                quickCreationSkuId = "2034917373414539277",
                quickCreationParams = mapOf(
                    "ratio" to "3:4",
                    "realPersonMode" to "false",
                ),
                quickCreationListParams = mapOf(
                    "imageUrls" to listOf("https://example.com/ref.png"),
                ),
            )
        )

        assertEquals("2046057458784591873", request.bindingId)
        assertEquals("VIDEO", request.categoryId)
        assertEquals("2034917373414539277", request.skuId)
        assertEquals(JsonPrimitive("薯片人偶踢足球"), request.params["prompt"])
        assertEquals(JsonPrimitive("3:4"), request.params["ratio"])
        assertEquals(JsonPrimitive(false), request.params["realPersonMode"])
        assertNull(request.params["aspectRatio"])
        assertNull(request.params["resolution"])
        assertNull(request.params["duration"])
        assertNull(request.params["generateAudio"])
        assertEquals(JsonPrimitive("multimodal"), request.params["creationMode"])
        assertEquals(JsonPrimitive(1), request.params["creationSubModeId"])
        assertEquals(JsonPrimitive("MULTIMODAL_REFERENCE"), request.params["creationSubModeKey"])
        val imageUrls = request.params["imageUrls"] as JsonArray
        assertEquals("https://example.com/ref.png", imageUrls.single().jsonPrimitive.content)
    }

    @Test
    fun `video v2 reference image uri enables multimodal reference params`() {
        val request = QuickCreationV2Defaults.videoCreateRequest(
            VideoGenerationRequest(
                prompt = "video prompt",
                model = "seedance2",
                aspectRatio = "3:4",
                duration = 8,
                resolution = "720p",
                referenceImageUri = "https://example.com/direct-ref.png",
                quickCreationBindingId = "video-binding",
                quickCreationSkuId = "video-sku",
            )
        )

        assertEquals(JsonPrimitive("multimodal"), request.params["creationMode"])
        assertEquals(JsonPrimitive(1), request.params["creationSubModeId"])
        assertEquals(JsonPrimitive("MULTIMODAL_REFERENCE"), request.params["creationSubModeKey"])
        val imageUrls = request.params["imageUrls"] as JsonArray
        assertEquals("https://example.com/direct-ref.png", imageUrls.single().jsonPrimitive.content)
        assertNull(request.params["ratio"])
        assertNull(request.params["aspectRatio"])
        assertNull(request.params["resolution"])
        assertNull(request.params["duration"])
        assertNull(request.params["generateAudio"])
        assertNull(request.params["realPersonMode"])
    }
}
