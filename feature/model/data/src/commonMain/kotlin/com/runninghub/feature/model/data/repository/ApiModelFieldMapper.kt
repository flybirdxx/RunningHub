package com.runninghub.feature.model.data.repository

import com.runninghub.feature.model.domain.ApiModelField
import com.runninghub.feature.model.domain.ApiModelFieldOption
import com.runninghub.feature.model.domain.ApiModelFieldType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * 标准模型动态字段配置的 Data 层解析器。
 *
 * RunningHub SKU 详情中的 `inputConfigJson` 是服务端表单协议，字段名和结构存在历史兼容差异。
 * 本类把该 JSON 映射为 Domain 层稳定的 [ApiModelField] 列表，并在解析失败时返回空列表，
 * 避免 Presentation 直接依赖服务端表单 JSON。
 *
 * @param json 由运行期组合根提供的 Kotlin Serialization 配置，需与网络 DTO 解码策略一致。
 */
class ApiModelFieldMapper(private val json: Json) {
    /**
     * 解析标准模型输入字段配置。
     *
     * @param inputConfigJson 服务端返回的字段配置 JSON；`null` 或空白字符串表示该模型没有动态字段。
     * @return 按服务端配置顺序映射得到的字段列表；解析失败或字段缺少稳定 key 时忽略对应项。
     */
    fun parse(inputConfigJson: String?): List<ApiModelField> {
        if (inputConfigJson.isNullOrBlank()) return emptyList()
        return runCatching {
            when (val root = json.parseToJsonElement(inputConfigJson)) {
                is JsonArray -> root.mapNotNull { it.toFieldOrNull() }
                is JsonObject -> (root["inputs"] as? JsonArray).orEmpty().mapNotNull { it.toFieldOrNull() }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun JsonElement.toFieldOrNull(): ApiModelField? {
        val obj = this as? JsonObject ?: return null
        val fieldKey = obj.string("fieldKey") ?: obj.string("key") ?: obj.string("name") ?: return null
        val paramKey = obj.string("mappedApiParamKey") ?: obj.string("paramKey") ?: fieldKey
        val type = obj.string("type") ?: obj.string("fieldType")
        // 服务端不同版本会混用 fieldKey、key、name 和 mappedApiParamKey；
        // 这里先归一为领域字段，再由请求构建器只按 paramKey 生成 OpenAPI 请求体。
        return ApiModelField(
            fieldKey = fieldKey,
            paramKey = paramKey,
            type = type.toFieldType(),
            required = obj.boolean("required") ?: false,
            title = obj.string("title") ?: obj.string("name"),
            description = obj.string("paramDesc") ?: obj.string("description"),
            placeholder = obj.string("placeholder"),
            defaultValue = obj.string("defaultValue"),
            options = obj.array("options").mapNotNull { it.toOptionOrNull() },
            minLength = obj.int("minLength"),
            maxLength = obj.int("maxLength"),
            min = obj.double("min"),
            max = obj.double("max"),
            step = obj.double("step"),
            precision = obj.int("precision"),
            multipleInputs = obj.boolean("multipleInputs") ?: false,
            maxInputCount = obj.int("maxInputCount") ?: obj.int("maxInpuNum"),
            maxUploadCount = obj.int("maxUploadCount"),
            maxUploadSizeBytes = obj.long("maxUploadSize") ?: obj.long("maxSize"),
            acceptFormats = obj.acceptFormats(),
            visible = obj.boolean("visible") ?: true,
        )
    }

    private fun JsonElement.toOptionOrNull(): ApiModelFieldOption? {
        val obj = this as? JsonObject ?: return null
        val value = obj.string("apiValue") ?: obj.string("value") ?: return null
        val label = obj.string("label") ?: obj.string("name") ?: value
        return ApiModelFieldOption(label = label, value = value)
    }

    private fun String?.toFieldType(): ApiModelFieldType = when (this?.uppercase()) {
        "STRING", "TEXT" -> ApiModelFieldType.STRING
        "NUMBER", "FLOAT", "DOUBLE" -> ApiModelFieldType.NUMBER
        "INTEGER", "INT" -> ApiModelFieldType.INTEGER
        "BOOLEAN", "BOOL" -> ApiModelFieldType.BOOLEAN
        "LIST", "SELECT", "ENUM" -> ApiModelFieldType.LIST
        "IMAGE", "IMAGE_UPLOAD" -> ApiModelFieldType.IMAGE
        "VIDEO", "VIDEO_UPLOAD" -> ApiModelFieldType.VIDEO
        "AUDIO", "AUDIO_UPLOAD" -> ApiModelFieldType.AUDIO
        "UPLOAD", "FILE" -> ApiModelFieldType.FILE
        "MODEL" -> ApiModelFieldType.MODEL
        else -> ApiModelFieldType.UNKNOWN
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.boolean(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.int(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull

    private fun JsonObject.long(key: String): Long? =
        this[key]?.jsonPrimitive?.longOrNull

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.array(key: String): List<JsonElement> =
        (this[key] as? JsonArray).orEmpty()

    private fun JsonObject.acceptFormats(): List<String> {
        val accept = this["accept"] ?: this["acceptFormats"] ?: this["fileTypes"] ?: return emptyList()
        // accept 既可能是数组，也可能是以逗号或竖线分隔的字符串；保持原顺序用于 UI 展示上传限制。
        return when (accept) {
            is JsonArray -> accept.mapNotNull { it.jsonPrimitive.contentOrNull }
            else -> accept.jsonPrimitive.contentOrNull
                ?.split(",", "|")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        }
    }
}
