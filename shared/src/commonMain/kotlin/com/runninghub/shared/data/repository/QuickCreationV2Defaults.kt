package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationCreateRequestDto
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.VideoGenerationRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal object QuickCreationV2Defaults {
    private const val IMAGE_CATEGORY_ID = "IMAGE"
    private const val IMAGE_G2_BINDING_ID = "2046586338670891013"
    private const val IMAGE_G2_SKU_ID = "2046514150500524034"

    fun imageG2CreateRequest(request: ImageGenerationRequest): QuickCreationCreateRequestDto {
        val params = buildMap {
            put("aspectRatio", JsonPrimitive(request.aspectRatio))
            put("resolution", JsonPrimitive(request.resolution))
            put("quality", JsonPrimitive(request.quality))

            request.quickCreationParams.forEach { (key, value) ->
                if (key.isNotBlank() && value.isNotBlank()) {
                    put(key, JsonPrimitive(value))
                }
            }
            request.quickCreationListParams.forEach { (key, values) ->
                val cleanedValues = values.filter { it.isNotBlank() }
                if (key.isNotBlank() && cleanedValues.isNotEmpty()) {
                    put(key, JsonArray(cleanedValues.map { JsonPrimitive(it) }))
                }
            }

            put("prompt", JsonPrimitive(request.prompt))

            val imageUrl = request.referenceImageUri?.takeIf { it.isNotBlank() }
            if (imageUrl != null && !containsKey("imageUrls")) {
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

    fun videoCreateRequest(request: VideoGenerationRequest): QuickCreationCreateRequestDto {
        val params = buildMap {
            put("ratio", JsonPrimitive(request.aspectRatio))
            put("aspectRatio", JsonPrimitive(request.aspectRatio))
            put("resolution", JsonPrimitive(request.resolution))
            put("duration", JsonPrimitive(request.duration))
            put("generateAudio", JsonPrimitive(request.generateAudio))
            put("realPersonMode", JsonPrimitive(request.realistic))

            putStringParams(request.quickCreationParams)
            putListParams(request.quickCreationListParams)

            put("prompt", JsonPrimitive(request.prompt))

            request.referenceImageUri?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                putIfAbsent("imageUrls", JsonArray(listOf(JsonPrimitive(imageUrl))))
            }
            request.referenceVideoUri?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                putIfAbsent("videoUrls", JsonArray(listOf(JsonPrimitive(videoUrl))))
            }
            request.referenceAudioUri?.takeIf { it.isNotBlank() }?.let { audioUrl ->
                putIfAbsent("audioUrls", JsonArray(listOf(JsonPrimitive(audioUrl))))
            }

            if (hasReferenceMedia()) {
                put("creationMode", JsonPrimitive("multimodal"))
                put("creationSubModeId", JsonPrimitive(1))
                put("creationSubModeKey", JsonPrimitive("MULTIMODAL_REFERENCE"))
            }
        }

        return QuickCreationCreateRequestDto(
            bindingId = requireNotNull(request.quickCreationBindingId) { "Video quick creation bindingId is required" },
            categoryId = request.quickCreationCategoryId ?: "VIDEO",
            skuId = requireNotNull(request.quickCreationSkuId) { "Video quick creation skuId is required" },
            params = params,
        )
    }

    private fun MutableMap<String, JsonElement>.putStringParams(values: Map<String, String>) {
        values.forEach { (key, value) ->
            if (key.isNotBlank() && value.isNotBlank()) {
                put(key, value.toJsonPrimitive())
            }
        }
    }

    private fun MutableMap<String, JsonElement>.putListParams(values: Map<String, List<String>>) {
        values.forEach { (key, rawValues) ->
            val cleanedValues = rawValues.filter { it.isNotBlank() }
            if (key.isNotBlank() && cleanedValues.isNotEmpty()) {
                put(key, JsonArray(cleanedValues.map { JsonPrimitive(it) }))
            }
        }
    }

    private fun String.toJsonPrimitive(): JsonPrimitive =
        when {
            equals("true", ignoreCase = true) -> JsonPrimitive(true)
            equals("false", ignoreCase = true) -> JsonPrimitive(false)
            toIntOrNull() != null -> JsonPrimitive(toInt())
            toDoubleOrNull() != null -> JsonPrimitive(toDouble())
            else -> JsonPrimitive(this)
        }

    private fun Map<String, JsonElement>.hasReferenceMedia(): Boolean =
        listOf("imageUrls", "videoUrls", "audioUrls").any { key -> containsKey(key) }
}

internal fun QuickCreationCreateRequestDto.imageUrls(): List<String> {
    val imageUrls = params["imageUrls"] as? JsonArray ?: return emptyList()
    return imageUrls.map { it.jsonPrimitive.content }
}
