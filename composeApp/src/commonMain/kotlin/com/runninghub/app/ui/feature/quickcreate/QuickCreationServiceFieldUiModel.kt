package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceField

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

internal fun QuickCreationServiceField.quickCreationFieldTitle(): String =
    inputExtra?.title?.takeIf { it.isNotBlank() } ?: fieldKey

internal fun QuickCreationServiceField.quickCreationInputPlaceholder(): String =
    inputExtra?.placeholder?.takeIf { it.isNotBlank() } ?: paramKey

internal fun QuickCreationServiceField.constrainQuickCreationTextInput(value: String): String {
    val maxLength = inputExtra?.maxLength?.takeIf { it >= 0 } ?: return value
    return value.take(maxLength)
}

internal fun QuickCreationServiceField.quickCreationTextLimitCounter(value: String): String? {
    val maxLength = inputExtra?.maxLength?.takeIf { it >= 0 } ?: return null
    return "${value.length.coerceAtMost(maxLength)}/$maxLength"
}

internal fun QuickCreationServiceField.quickCreationUploadHintParts(): List<String> =
    listOfNotNull(
        inputExtra?.acceptFormats?.takeIf { it.isNotEmpty() }?.joinToString("/"),
        (inputExtra?.maxInputCount ?: maxUploadCount)?.let { "最多 $it 个文件" },
        maxUploadSize?.let { "单文件 ${it / 1024 / 1024}MB" },
    )
