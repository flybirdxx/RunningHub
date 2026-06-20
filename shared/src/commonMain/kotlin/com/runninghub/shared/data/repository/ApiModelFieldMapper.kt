package com.runninghub.shared.data.repository

import com.runninghub.shared.domain.model.ApiModelField
import com.runninghub.shared.domain.model.ApiModelFieldOption
import com.runninghub.shared.domain.model.ApiModelFieldType
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

class ApiModelFieldMapper(private val json: Json) {
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
