package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceModel

internal fun QuickCreationServiceField.supportsQuickCreationTextEntry(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("STRING") ||
        type.contains("TEXT") ||
        type.contains("NUMBER") ||
        type.contains("INTEGER") ||
        type.contains("FLOAT")
}

internal fun QuickCreationServiceField.isQuickCreationUploadField(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("UPLOAD") ||
        type.contains("IMAGE") ||
        type.contains("VIDEO") ||
        type.contains("AUDIO")
}

internal fun QuickCreationServiceField.isQuickCreationServiceFieldRenderable(): Boolean =
    visible && (options.isNotEmpty() || supportsQuickCreationTextEntry() || isQuickCreationUploadField())

internal fun QuickCreationServiceFieldInputChild.supportsQuickCreationTextEntry(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("STRING") ||
        type.contains("TEXT") ||
        type.contains("NUMBER") ||
        type.contains("INTEGER") ||
        type.contains("FLOAT")
}

internal fun QuickCreationServiceFieldInputChild.isQuickCreationUploadField(): Boolean {
    val type = fieldType.uppercase()
    return type.contains("UPLOAD") ||
        type.contains("IMAGE") ||
        type.contains("VIDEO") ||
        type.contains("AUDIO")
}

internal fun QuickCreationServiceFieldInputChild.isQuickCreationServiceFieldRenderable(): Boolean =
    visible && (options.isNotEmpty() || supportsQuickCreationTextEntry() || isQuickCreationUploadField())

internal fun QuickCreationServiceField.quickCreationFieldTitle(): String =
    inputExtra?.title?.takeIf { it.isNotBlank() } ?: fieldKey

internal fun QuickCreationServiceFieldInputChild.quickCreationFieldTitle(): String =
    title?.takeIf { it.isNotBlank() } ?: fieldKey

internal fun QuickCreationServiceField.quickCreationInputPlaceholder(): String =
    inputExtra?.placeholder?.takeIf { it.isNotBlank() } ?: paramKey

internal fun QuickCreationServiceFieldInputChild.quickCreationInputPlaceholder(): String =
    placeholder?.takeIf { it.isNotBlank() } ?: paramKey

internal fun QuickCreationServiceField.quickCreationActiveInputChildren(
    params: Map<String, String>,
): List<QuickCreationServiceFieldInputChild> {
    val parentValue = params[paramKey] ?: params[fieldKey] ?: defaultValue.orEmpty()
    return inputExtra?.inputChildren.orEmpty()
        .filter { it.isQuickCreationServiceFieldRenderable() }
        .filter { child ->
            val condition = child.visibleWhen ?: return@filter true
            val conditionValue = params[condition.fieldKey]
                ?: if (condition.fieldKey == fieldKey || condition.fieldKey == paramKey) parentValue else null
            if (condition.values.isEmpty()) {
                !conditionValue.isNullOrBlank()
            } else {
                conditionValue in condition.values
            }
        }
}

internal fun QuickCreationServiceModel?.quickCreationParamsWithFieldAliases(
    params: Map<String, String>,
): Map<String, String> {
    val fields = this?.fields.orEmpty().filter { it.visible }
    if (fields.isEmpty()) return params
    return buildMap<String, String> {
        fields.forEach { field ->
            putQuickCreationParamAliases(
                fieldKey = field.fieldKey,
                paramKey = field.paramKey,
                defaultValue = field.defaultValue,
                params = params,
            )
        }
        var changed: Boolean
        do {
            changed = false
            val snapshot = toMap()
            fields.forEach { field ->
                field.quickCreationActiveInputChildren(snapshot).forEach { child ->
                    val beforeSize = size
                    putQuickCreationParamAliases(
                        fieldKey = child.fieldKey,
                        paramKey = child.paramKey,
                        defaultValue = child.defaultValue,
                        params = params,
                    )
                    if (size != beforeSize) {
                        changed = true
                    }
                }
            }
        } while (changed)
    }
}

private fun MutableMap<String, String>.putQuickCreationParamAliases(
    fieldKey: String,
    paramKey: String,
    defaultValue: String?,
    params: Map<String, String>,
) {
    val value = params[paramKey] ?: params[fieldKey] ?: defaultValue?.takeIf { it.isNotBlank() } ?: return
    if (fieldKey.isNotBlank() && fieldKey !in this) {
        put(fieldKey, value)
    }
    if (paramKey.isNotBlank() && paramKey !in this) {
        put(paramKey, value)
    }
}

internal fun QuickCreationServiceField.constrainQuickCreationTextInput(value: String): String {
    val maxLength = inputExtra?.maxLength?.takeIf { it >= 0 } ?: return value
    return value.take(maxLength)
}

internal fun QuickCreationServiceFieldInputChild.constrainQuickCreationTextInput(value: String): String {
    val maxLength = maxLength?.takeIf { it >= 0 } ?: return value
    return value.take(maxLength)
}

internal fun QuickCreationServiceField.quickCreationTextLimitCounter(value: String): String? {
    val maxLength = inputExtra?.maxLength?.takeIf { it >= 0 } ?: return null
    return "${value.length.coerceAtMost(maxLength)}/$maxLength"
}

internal fun QuickCreationServiceFieldInputChild.quickCreationTextLimitCounter(value: String): String? {
    val maxLength = maxLength?.takeIf { it >= 0 } ?: return null
    return "${value.length.coerceAtMost(maxLength)}/$maxLength"
}

internal fun QuickCreationServiceField.quickCreationTextValidationError(value: String): String? {
    val title = quickCreationFieldTitle()
    val trimmed = value.trim()
    if (required && trimmed.isEmpty()) {
        return "$title 不能为空"
    }
    val minLength = inputExtra?.minLength?.takeIf { it > 0 }
    if (minLength != null && trimmed.isNotEmpty() && trimmed.length < minLength) {
        return "$title 至少 $minLength 个字符"
    }
    return null
}

internal fun QuickCreationServiceFieldInputChild.quickCreationTextValidationError(value: String): String? {
    val title = quickCreationFieldTitle()
    val trimmed = value.trim()
    if (required && trimmed.isEmpty()) {
        return "$title 不能为空"
    }
    val minLength = minLength?.takeIf { it > 0 }
    if (minLength != null && trimmed.isNotEmpty() && trimmed.length < minLength) {
        return "$title 至少 $minLength 个字符"
    }
    return null
}

internal fun QuickCreationServiceField.quickCreationUploadValidationError(uploadedCount: Int): String? {
    val title = quickCreationFieldTitle()
    if (required && uploadedCount <= 0) {
        return "$title 不能为空"
    }
    val maxCount = inputExtra?.maxInputCount ?: maxUploadCount
    if (maxCount != null && uploadedCount > maxCount) {
        return "$title 最多 $maxCount 个文件"
    }
    return null
}

internal fun QuickCreationServiceFieldInputChild.quickCreationUploadValidationError(uploadedCount: Int): String? {
    val title = quickCreationFieldTitle()
    if (required && uploadedCount <= 0) {
        return "$title \u4e0d\u80fd\u4e3a\u7a7a"
    }
    val maxCount = maxInputCount
    if (maxCount != null && uploadedCount > maxCount) {
        return "$title \u6700\u591a $maxCount \u4e2a\u6587\u4ef6"
    }
    return null
}

internal fun QuickCreationServiceField.quickCreationUploadHintParts(): List<String> =
    listOfNotNull(
        inputExtra?.acceptFormats?.takeIf { it.isNotEmpty() }?.joinToString("/"),
        (inputExtra?.maxInputCount ?: maxUploadCount)?.let { "最多 $it 个文件" },
        maxUploadSize?.let { "单文件 ${it / 1024 / 1024}MB" },
    )

internal fun List<MediaReference>.quickCreationGlobalMediaReferences(): List<MediaReference> =
    filter { it.fieldParamKey.isNullOrBlank() }

internal fun List<MediaReference>.quickCreationFieldMediaReferences(paramKey: String): List<MediaReference> =
    filter { it.fieldParamKey == paramKey }

internal fun QuickCreationServiceModel?.quickCreationActiveUploadParamKeys(
    serviceParams: Map<String, String>,
): Set<String> {
    val aliasedParams = quickCreationParamsWithFieldAliases(serviceParams)
    return this?.fields.orEmpty()
        .filter { it.visible }
        .flatMap { field ->
            buildList {
                if (field.isQuickCreationServiceFieldRenderable() && field.isQuickCreationUploadField()) {
                    add(field.paramKey)
                }
                addAll(
                    field.quickCreationActiveInputChildren(aliasedParams)
                        .filter { it.isQuickCreationUploadField() }
                        .map { it.paramKey }
                )
            }
        }
        .toSet()
}

internal fun List<MediaReference>.quickCreationRelevantMediaReferences(
    activeFieldParamKeys: Set<String>,
): List<MediaReference> =
    filter { reference ->
        reference.fieldParamKey.isNullOrBlank() || reference.fieldParamKey in activeFieldParamKeys
    }

internal fun QuickCreationServiceField.quickCreationUploadMediaType(): QuickCreateMediaType? {
    val marker = listOfNotNull(fieldType, fieldKey, paramKey, inputExtraJson)
        .joinToString(" ")
        .uppercase()
    return marker.quickCreationUploadMediaTypeFromMarker()
}

internal fun QuickCreationServiceFieldInputChild.quickCreationUploadMediaType(): QuickCreateMediaType? {
    val marker = listOf(fieldType, fieldKey, paramKey)
        .joinToString(" ")
        .uppercase()
    return marker.quickCreationUploadMediaTypeFromMarker()
}

private fun String.quickCreationUploadMediaTypeFromMarker(): QuickCreateMediaType? =
    when {
        contains("AUDIO") -> QuickCreateMediaType.AUDIO
        contains("VIDEO") -> QuickCreateMediaType.VIDEO
        contains("IMAGE") || contains("PHOTO") || contains("IMG") -> QuickCreateMediaType.IMAGE
        else -> null
    }
