package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationCreateRequestDto
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal object QuickCreationV2Defaults {
    private const val IMAGE_CATEGORY_ID = "IMAGE"
    private const val IMAGE_G2_BINDING_ID = "2046586338670891013"
    private const val IMAGE_G2_SKU_ID = "2046514150500524034"

    fun imageG2CreateRequest(request: ImageGenerationRequest): QuickCreationCreateRequestDto {
        val params = buildMap {
            put("prompt", JsonPrimitive(request.prompt))
            put("aspectRatio", JsonPrimitive(request.aspectRatio))
            put("resolution", JsonPrimitive(request.resolution))
            put("quality", JsonPrimitive(request.quality))

            val imageUrl = request.referenceImageUri?.takeIf { it.isNotBlank() }
            if (imageUrl != null) {
                put("imageUrls", JsonArray(listOf(JsonPrimitive(imageUrl))))
            }
        }

        return QuickCreationCreateRequestDto(
            bindingId = request.quickCreationBindingId ?: IMAGE_G2_BINDING_ID,
            categoryId = request.quickCreationCategoryId ?: IMAGE_CATEGORY_ID,
            skuId = request.quickCreationSkuId ?: IMAGE_G2_SKU_ID,
            params = params,
        )
    }
}

internal fun QuickCreationCreateRequestDto.imageUrls(): List<String> {
    val imageUrls = params["imageUrls"] as? JsonArray ?: return emptyList()
    return imageUrls.map { it.jsonPrimitive.content }
}
