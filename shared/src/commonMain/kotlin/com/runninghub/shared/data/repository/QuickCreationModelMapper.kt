package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationFieldDto
import com.runninghub.shared.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.shared.data.remote.dto.QuickCreationModelDto
import com.runninghub.shared.data.remote.dto.QuickCreationPricingDto
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldExtra
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldOption
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.QuickCreationServicePricing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object QuickCreationModelMapper {
    private val inputExtraJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun flatten(
        fallbackCategoryId: String,
        models: List<QuickCreationModelDto>,
    ): List<QuickCreationServiceModel> =
        models.flatMap { entry ->
            if (entry.children.isNotEmpty()) {
                entry.children.mapNotNull { child ->
                    child.toDomain(
                        fallbackCategoryId = fallbackCategoryId,
                        groupName = entry.groupName ?: entry.nameCn ?: entry.name ?: entry.nameAi,
                    )
                }
            } else {
                listOfNotNull(entry.toDomain(fallbackCategoryId = fallbackCategoryId, groupName = null))
            }
        }

    private fun QuickCreationModelDto.toDomain(
        fallbackCategoryId: String,
        groupName: String?,
    ): QuickCreationServiceModel? {
        val binding = bindingId?.takeIf { it.isNotBlank() } ?: return null
        val sku = skuId?.takeIf { it.isNotBlank() } ?: return null

        return QuickCreationServiceModel(
            categoryId = categoryId ?: fallbackCategoryId,
            groupName = groupName,
            bindingId = binding,
            skuId = sku,
            name = nameCn ?: name ?: nameAi ?: sku,
            description = description,
            fields = fields.mapNotNull { it.toDomain() },
            pricing = pricing?.toDomain(),
        )
    }

    private fun QuickCreationFieldDto.toDomain(): QuickCreationServiceField? {
        val key = fieldKey?.takeIf { it.isNotBlank() } ?: return null
        val extraJson = skuInputExtraJson?.stringValue()
        return QuickCreationServiceField(
            fieldKey = key,
            paramKey = mappedApiParamKey?.takeIf { it.isNotBlank() } ?: key,
            fieldType = fieldType ?: "UNKNOWN",
            visible = visible,
            required = required,
            defaultValue = defaultValue?.stringValue(),
            options = options.mapNotNull { it.toDomain() },
            maxUploadCount = maxUploadCount,
            maxUploadSize = maxUploadSize,
            multipleInputs = multipleInputs,
            inputExtraJson = extraJson,
            inputExtra = extraJson?.toInputExtra(),
        )
    }

    private fun QuickCreationFieldOptionDto.toDomain(): QuickCreationServiceFieldOption? {
        val optionValue = value?.stringValue() ?: return null
        return QuickCreationServiceFieldOption(
            label = label ?: name ?: optionValue,
            value = optionValue,
        )
    }

    private fun QuickCreationPricingDto.toDomain(): QuickCreationServicePricing =
        QuickCreationServicePricing(
            pricingMode = pricingMode,
            settlementMode = settlementMode,
            paidPriceKind = paidPriceKind,
            flatPriceRaw = flatPrice?.toString(),
            dimensionPricingRaw = dimensionPricing?.toString(),
            discountPercent = discountPercent,
            isFree = isFree,
            freeRemaining = freeRemaining,
            isTimeFree = isTimeFree,
            promoType = promoType,
        )

    private fun String.toInputExtra(): QuickCreationServiceFieldExtra? {
        val extra = runCatching { inputExtraJson.parseToJsonElement(this) }.getOrNull() as? JsonObject
            ?: return null
        val parsed = QuickCreationServiceFieldExtra(
            title = extra.stringValue("title"),
            titleEn = extra.stringValue("titleEn"),
            paramDescription = extra.stringValue("paramDesc"),
            paramDescriptionEn = extra.stringValue("paramDescEn"),
            placeholder = extra.stringValue("placeholder"),
            acceptFormats = extra.acceptFormats(),
            maxLength = extra.intValue("maxLength"),
            minLength = extra.intValue("minLength"),
            maxInputCount = extra.intValue("maxInpuNum") ?: extra.intValue("maxInputNum"),
            ignoreListValueCaseSensitive = extra.booleanValue("ignoreListValueCaseSensitive") ?: false,
        )
        return parsed.takeIf {
            listOfNotNull(
                it.title,
                it.titleEn,
                it.paramDescription,
                it.paramDescriptionEn,
                it.placeholder,
            ).isNotEmpty() ||
                it.acceptFormats.isNotEmpty() ||
                it.maxLength != null ||
                it.minLength != null ||
                it.maxInputCount != null ||
                it.ignoreListValueCaseSensitive
        }
    }

    private fun JsonObject.acceptFormats(): List<String> {
        val accept = this["accept"] ?: return emptyList()
        val array = accept as? JsonArray
            ?: accept.stringValue()
                ?.let { value -> runCatching { inputExtraJson.parseToJsonElement(value) }.getOrNull() as? JsonArray }
        if (array != null) {
            return array.mapNotNull { it.stringValue()?.takeIf(String::isNotBlank) }
        }
        return accept.stringValue()
            ?.split(',')
            ?.map { it.trim().trim('"', '\'') }
            ?.filter { it.isNotBlank() }
            .orEmpty()
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.stringValue()?.takeIf { it.isNotBlank() }

    private fun JsonObject.intValue(key: String): Int? =
        this[key]?.jsonPrimitive?.intOrNull ?: this[key]?.stringValue()?.toIntOrNull()

    private fun JsonObject.booleanValue(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull ?: when (this[key]?.stringValue()?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
}

private fun kotlinx.serialization.json.JsonElement.stringValue(): String? {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull
        ?: primitive.intOrNull?.toString()
        ?: primitive.doubleOrNull?.toString()
        ?: primitive.booleanOrNull?.toString()
}
