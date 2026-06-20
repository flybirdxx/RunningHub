package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationCreateRequestDto
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * QuickCreate v2 远程创建请求的默认参数构造器。
 *
 * 该对象位于 Data 层，负责把领域层的创作请求补齐为服务端 `prepare/create` 接口需要的
 * DTO 参数。默认值只用于兼容当前快捷创作接口的固定官方模型；当用户或服务端模板已经提供
 * 同名参数时，不会覆盖显式输入，避免 UI 中的动态字段被本地兜底逻辑改写。
 */
internal object QuickCreationV2Defaults {
    private const val IMAGE_CATEGORY_ID = "IMAGE"
    private const val IMAGE_G2_BINDING_ID = "2046586338670891013"
    private const val IMAGE_G2_SKU_ID = "2046514150500524034"

    /**
     * 构造图片快捷创作的 v2 创建请求。
     *
     * @param request Presentation/Domain 已整理好的图片创作参数。
     * @return 可直接交给 QuickCreate v2 远程接口的 DTO 请求，包含图片官方模型的兜底
     * bindingId、categoryId 和 skuId。
     */
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

    /**
     * 构造视频快捷创作的 v2 创建请求。
     *
     * @param request Presentation/Domain 已整理好的视频创作参数。
     * @return 可直接交给 QuickCreate v2 远程接口的 DTO 请求。视频模型必须由服务端目录提供
     * bindingId 与 skuId，缺失时直接失败，避免向远端提交无法计费或无法路由的任务。
     */
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
                putIfMissing("imageUrls", JsonArray(listOf(JsonPrimitive(imageUrl))))
            }
            request.referenceVideoUri?.takeIf { it.isNotBlank() }?.let { videoUrl ->
                putIfMissing("videoUrls", JsonArray(listOf(JsonPrimitive(videoUrl))))
            }
            request.referenceAudioUri?.takeIf { it.isNotBlank() }?.let { audioUrl ->
                putIfMissing("audioUrls", JsonArray(listOf(JsonPrimitive(audioUrl))))
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

    private fun MutableMap<String, JsonElement>.putIfMissing(key: String, value: JsonElement) {
        // Kotlin common/native 没有稳定的 MutableMap.putIfAbsent；显式判断也能表达
        // “动态模板参数优先，本地 reference URI 只兜底”的业务规则。
        if (!containsKey(key)) {
            put(key, value)
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
