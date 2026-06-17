package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.dto.QuickCreationFieldDto
import com.runninghub.shared.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.shared.data.remote.dto.QuickCreationModelDto
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldOption
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object QuickCreationModelMapper {
    fun flatten(
        fallbackCategoryId: String,
        models: List<QuickCreationModelDto>,
    ): List<QuickCreationServiceModel> =
        models.flatMap { entry ->
            if (entry.children.isNotEmpty()) {
                entry.children.mapNotNull { child ->
                    child.toDomain(
                        fallbackCategoryId = fallbackCategoryId,
                        groupName = entry.nameCn ?: entry.name ?: entry.nameAi,
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
        )
    }

    private fun QuickCreationFieldDto.toDomain(): QuickCreationServiceField? {
        val key = fieldKey?.takeIf { it.isNotBlank() } ?: return null
        return QuickCreationServiceField(
            fieldKey = key,
            paramKey = mappedApiParamKey?.takeIf { it.isNotBlank() } ?: key,
            fieldType = fieldType ?: "UNKNOWN",
            required = required,
            defaultValue = defaultValue?.stringValue(),
            options = options.mapNotNull { it.toDomain() },
        )
    }

    private fun QuickCreationFieldOptionDto.toDomain(): QuickCreationServiceFieldOption? {
        val optionValue = value?.stringValue() ?: return null
        return QuickCreationServiceFieldOption(
            label = label ?: name ?: optionValue,
            value = optionValue,
        )
    }
}

private fun kotlinx.serialization.json.JsonElement.stringValue(): String? {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull
        ?: primitive.intOrNull?.toString()
        ?: primitive.doubleOrNull?.toString()
        ?: primitive.booleanOrNull?.toString()
}
