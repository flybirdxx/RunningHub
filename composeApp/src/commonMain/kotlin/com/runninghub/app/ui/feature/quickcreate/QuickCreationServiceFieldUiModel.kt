package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild

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

internal fun QuickCreationServiceField.quickCreationUploadHintParts(): List<String> =
    listOfNotNull(
        inputExtra?.acceptFormats?.takeIf { it.isNotEmpty() }?.joinToString("/"),
        (inputExtra?.maxInputCount ?: maxUploadCount)?.let { "最多 $it 个文件" },
        maxUploadSize?.let { "单文件 ${it / 1024 / 1024}MB" },
    )
