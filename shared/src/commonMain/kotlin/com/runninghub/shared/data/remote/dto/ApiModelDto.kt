package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SkuListRequestDto(
    val categoryType: String = "STANDARD_MODEL",
    val tagIds: List<String> = emptyList(),
    val search: String = "",
    val categoryTagIds: List<String> = emptyList(),
    val owners: List<String> = emptyList(),
    val isCollected: Boolean = false,
    val region: String = "",
    val isWhitelist: Boolean = false,
    val pageNum: Int = 1,
    val pageSize: Int = 30,
)

@Serializable
data class SkuListPageDto(
    val records: List<SkuSummaryDto> = emptyList(),
    val list: List<SkuSummaryDto> = emptyList(),
    val page: SkuListNestedPageDto? = null,
    val total: Int = 0,
) {
    val items: List<SkuSummaryDto>
        get() = records
            .ifEmpty { list }
            .ifEmpty { page?.items.orEmpty() }
}

@Serializable
data class SkuListNestedPageDto(
    val records: List<SkuSummaryDto> = emptyList(),
    val list: List<SkuSummaryDto> = emptyList(),
) {
    val items: List<SkuSummaryDto>
        get() = records.ifEmpty { list }
}

object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            JsonNull -> ""
            is JsonPrimitive -> element.content
            else -> element.toString()
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}
@Serializable
data class SkuSummaryDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String = "",
    val name: String = "",
    val nameEn: String? = null,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val price: String = "",
    val priceSummary: String? = null,
    val rhEndpoint: String? = null,
)

@Serializable
data class SkuDetailRequestDto(
    val id: String,
)

@Serializable
data class SkuDetailDto(
    val id: String,
    val name: String = "",
    val nameEn: String? = null,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val price: String = "",
    val priceSummary: String? = null,
    val rhEndpoint: String = "",
    val inputConfigJson: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
)

@Serializable
data class LlmModelDto(
    val modelKey: String,
    val provider: String = "",
    val version: String? = null,
    @SerialName("context")
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)




