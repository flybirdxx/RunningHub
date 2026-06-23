package com.runninghub.feature.quickcreate.data.repository

import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationFieldDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationFieldOptionDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationModelDto
import com.runninghub.feature.quickcreate.data.remote.dto.QuickCreationPricingDto
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldExtra
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldOption
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldVisibilityCondition
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServicePricing
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 将快捷创作模型目录 DTO 映射为 quickcreate Domain 模型。
 *
 * 该对象属于 QuickCreate Data 层，是远端接口协议与 `feature:quickcreate:domain` 之间的隔离边界。
 * 它负责过滤缺少绑定 ID 或 SKU ID 的异常模型、解析服务端动态字段扩展 JSON、兼容 child 字段结构，
 * 并把上传媒体类型推断结果写入 Domain 模型。Domain 层因此不需要依赖 DTO、JSON 解析或接口字段名。
 */
internal object QuickCreationModelMapper {
    private val inputExtraJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    /**
     * 展平快捷创作模型目录树。
     *
     * 服务端可能把可提交的模型放在分组节点的 children 中，也可能直接返回叶子模型。
     * 本函数保留父节点分组名作为 [QuickCreationServiceModel.groupName]，并丢弃无法构造提交身份的节点，
     * 避免 Presentation 层看到不可计费、不可提交的目录项。
     *
     * @param fallbackCategoryId 当前请求分类 ID；当单个模型 DTO 未返回分类时作为兜底。
     * @param models 服务端返回的原始模型树，顺序带有运营配置含义，映射后保持原顺序。
     * @return 可供快捷创作页面选择的服务模型列表；空列表表示接口没有返回有效叶子模型。
     */
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
        val extra = extraJson?.toInputExtra()
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
            // skuInputExtraJson 是远端序列化细节，只在 Data 层用于解析字段元数据和媒体类型。
            uploadMediaKind = inferUploadMediaKind(fieldType, key, mappedApiParamKey, extraJson, extra?.acceptFormats.orEmpty()),
            inputExtra = extra,
            rawInputExtraJson = extraJson,
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
            inputChildren = extra.inputChildren(),
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
                it.ignoreListValueCaseSensitive ||
                it.inputChildren.isNotEmpty()
        }
    }

    private fun JsonObject.inputChildren(): List<QuickCreationServiceFieldInputChild> {
        val children = this["inputsChildList"]
            ?: this["inputChildList"]
            ?: this["children"]
            ?: return emptyList()
        val array = children as? JsonArray
            ?: children.stringValue()
                ?.let { value -> runCatching { inputExtraJson.parseToJsonElement(value) }.getOrNull() as? JsonArray }
            ?: return emptyList()
        return array.mapNotNull { (it as? JsonObject)?.toInputChild() }
    }

    private fun JsonObject.toInputChild(): QuickCreationServiceFieldInputChild? {
        val key = stringValue("fieldKey")
            ?: stringValue("key")
            ?: stringValue("paramKey")
            ?: stringValue("mappedApiParamKey")
            ?: return null
        val childFieldType = stringValue("fieldType") ?: stringValue("type") ?: stringValue("inputType")
        val childParamKey = stringValue("mappedApiParamKey") ?: stringValue("paramKey")
        val childExtraRaw = this["skuInputExtraJson"]?.stringValue()
        val childExtra = childExtraRaw
            ?.let { value -> runCatching { inputExtraJson.parseToJsonElement(value) }.getOrNull() as? JsonObject }
        val conditionRaw = rawVisibilityConditionJson()
        return QuickCreationServiceFieldInputChild(
            fieldKey = key,
            paramKey = childParamKey ?: key,
            fieldType = childFieldType ?: "UNKNOWN",
            required = booleanValue("required") ?: false,
            visible = booleanValue("visible") ?: true,
            defaultValue = this["defaultValue"]?.stringValue(),
            title = stringValue("title") ?: childExtra?.stringValue("title") ?: stringValue("label") ?: stringValue("name"),
            paramDescription = stringValue("paramDesc") ?: childExtra?.stringValue("paramDesc"),
            placeholder = stringValue("placeholder") ?: childExtra?.stringValue("placeholder"),
            maxLength = intValue("maxLength") ?: childExtra?.intValue("maxLength"),
            minLength = intValue("minLength") ?: childExtra?.intValue("minLength"),
            maxInputCount = intValue("maxInputCount") ?: childExtra?.intValue("maxInputCount"),
            uploadMediaKind = inferUploadMediaKind(
                childFieldType,
                key,
                childParamKey,
                childExtra?.toString(),
                childExtra?.acceptFormats().orEmpty(),
            ),
            options = optionsValue(),
            visibleWhen = visibleWhen(),
            rawInputExtraJson = childExtraRaw,
            rawVisibilityConditionJson = conditionRaw,
        )
    }

    private fun JsonObject.optionsValue(): List<QuickCreationServiceFieldOption> {
        val options = this["options"] as? JsonArray ?: return emptyList()
        return options.mapNotNull { option ->
            val optionObject = option as? JsonObject
            val value = optionObject?.get("value")?.stringValue() ?: option.stringValue() ?: return@mapNotNull null
            QuickCreationServiceFieldOption(
                label = optionObject?.stringValue("label")
                    ?: optionObject?.stringValue("name")
                    ?: optionObject?.stringValue("title")
                    ?: value,
                value = value,
            )
        }
    }

    private fun JsonObject.visibleWhen(): QuickCreationServiceFieldVisibilityCondition? {
        val condition = (this["showWhen"] ?: this["visibleWhen"] ?: this["dependsOn"]) as? JsonObject
            ?: return null
        val fieldKey = condition.stringValue("fieldKey")
            ?: condition.stringValue("key")
            ?: condition.stringValue("paramKey")
            ?: return null
        val values = condition["values"]?.stringList()
            ?: condition["value"]?.stringList()
            ?: emptyList()
        return QuickCreationServiceFieldVisibilityCondition(fieldKey = fieldKey, values = values)
    }

    private fun JsonObject.rawVisibilityConditionJson(): String? =
        (this["showWhen"] ?: this["visibleWhen"] ?: this["dependsOn"])
            ?.toString()
            ?.takeIf { it.isNotBlank() }

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

    private fun inferUploadMediaKind(
        fieldType: String?,
        fieldKey: String?,
        paramKey: String?,
        extraJson: String?,
        acceptFormats: List<String>,
    ): QuickCreationUploadMediaKind? {
        // 服务端没有稳定的上传字段类型枚举，旧接口会把媒体信息分散在 fieldType、key、
        // 扩展 JSON 和 accept 格式中。这里集中做启发式推断；无法判断时返回 null，
        // 由上层根据当前图片/视频创作入口或字段绑定关系兜底，避免 Data 层误判后污染 Domain 状态。
        val marker = buildList {
            listOfNotNull(fieldType, fieldKey, paramKey, extraJson).forEach { add(it) }
            acceptFormats.forEach { add(it) }
        }.joinToString(" ").uppercase()
        return when {
            "AUDIO" in marker || "M4A" in marker || "MP3" in marker || "WAV" in marker -> QuickCreationUploadMediaKind.AUDIO
            "VIDEO" in marker || "MP4" in marker || "MOV" in marker || "WEBM" in marker -> QuickCreationUploadMediaKind.VIDEO
            "IMAGE" in marker || "PHOTO" in marker || "IMG" in marker ||
                "PNG" in marker || "JPG" in marker || "JPEG" in marker || "WEBP" in marker -> QuickCreationUploadMediaKind.IMAGE
            else -> null
        }
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

    private fun JsonElement.stringList(): List<String> {
        val array = this as? JsonArray
            ?: stringValue()
                ?.let { value -> runCatching { inputExtraJson.parseToJsonElement(value) }.getOrNull() as? JsonArray }
        if (array != null) {
            return array.mapNotNull { it.stringValue()?.takeIf(String::isNotBlank) }
        }
        return listOfNotNull(stringValue()?.takeIf(String::isNotBlank))
    }
}

private fun kotlinx.serialization.json.JsonElement.stringValue(): String? {
    val primitive = this as? JsonPrimitive ?: return toString()
    return primitive.contentOrNull
        ?: primitive.intOrNull?.toString()
        ?: primitive.doubleOrNull?.toString()
        ?: primitive.booleanOrNull?.toString()
}
