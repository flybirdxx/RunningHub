package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationResolvedFieldKind
import com.runninghub.shared.domain.repository.QuickCreationResolvedServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceSchema
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.QuickCreationUploadMediaKind

/**
 * 服务端动态字段在参数面板中的控件类型。
 */
internal enum class QuickCreationServiceFieldControlType {
    /** 由服务端 options 渲染为横向选项组。 */
    OPTIONS,

    /** 由服务端文本或数字字段渲染为输入框。 */
    TEXT,

    /** 由服务端媒体字段渲染为上传入口。 */
    UPLOAD,
}

/**
 * 服务端动态字段选项的 UI 模型。
 *
 * @property label 选项展示文案，来自服务端 label。
 * @property value 回传给服务端参数 Map 的原始值。
 * @property selected 当前参数值是否等于 [value]。
 */
internal data class QuickCreationServiceFieldOptionUi(
    val label: String,
    val value: String,
    val selected: Boolean,
)

/**
 * 服务端动态字段在参数面板中的 UI 模型。
 *
 * 该模型把字段类型判断、标题兜底、默认值、文本限制、上传提示和子字段激活规则收敛在映射层。
 * Composable 只根据 [controlType] 渲染控件，不再直接解析服务端 `fieldType`、`inputExtra`
 * 或 `visibleWhen`。
 *
 * @property paramKey 字段写回服务端参数 Map 使用的稳定键。
 * @property title 用户可见字段标题。
 * @property description 字段说明，`null` 表示服务端未提供说明。
 * @property controlType 当前字段应渲染的控件类型。
 * @property options 选项控件的候选值；非选项控件为空。
 * @property textValue 文本控件当前值，来自参数 Map 或默认值。
 * @property placeholder 文本控件占位文案。
 * @property maxLength 文本最大长度；`null` 表示不限制。
 * @property textLimitCounter 文本长度计数文案；`null` 表示不展示计数。
 * @property uploadMediaType 上传控件允许的媒体类型；`null` 表示使用通用上传入口。
 * @property uploadHint 上传控件辅助说明；空字符串表示没有提示。
 * @property childFields 当前已激活的子字段列表，顺序来自服务端配置。
 * @property indentLevel 字段缩进层级，父字段为 0，子字段为 1。
 */
internal data class QuickCreationServiceFieldUi(
    val paramKey: String,
    val title: String,
    val description: String?,
    val controlType: QuickCreationServiceFieldControlType,
    val options: List<QuickCreationServiceFieldOptionUi>,
    val textValue: String,
    val placeholder: String,
    val maxLength: Int?,
    val textLimitCounter: String?,
    val uploadMediaType: QuickCreateMediaType?,
    val uploadHint: String,
    val childFields: List<QuickCreationServiceFieldUi>,
    val indentLevel: Int,
) {
    /**
     * 根据字段最大长度约束用户输入。
     *
     * @param value 用户刚输入的原始文本。
     * @return 不超过 [maxLength] 的文本；未配置最大长度时返回原值。
     */
    fun constrainTextInput(value: String): String =
        maxLength?.takeIf { it >= 0 }?.let { value.take(it) } ?: value
}

/**
 * 将服务端模型的动态字段映射为参数面板 UI 模型。
 *
 * 字段可见性、fieldKey/paramKey 别名、当前值和 child 激活规则由 shared schema 统一解析；
 * 本层只负责把平台无关字段描述映射为参数面板控件、媒体枚举和提示文案。
 */
internal fun QuickCreationServiceModel?.quickCreationServiceFieldUiItems(
    params: Map<String, String>,
): List<QuickCreationServiceFieldUi> =
    QuickCreationServiceSchema.resolvedFields(
        model = this,
        serviceParams = params,
    ).map { field -> field.toQuickCreationServiceFieldUi(indentLevel = 0) }

private fun QuickCreationResolvedServiceField.toQuickCreationServiceFieldUi(
    indentLevel: Int,
): QuickCreationServiceFieldUi {
    return QuickCreationServiceFieldUi(
        paramKey = paramKey,
        title = title,
        description = description,
        controlType = kind.toQuickCreationServiceFieldControlType(),
        options = options.map { option ->
            QuickCreationServiceFieldOptionUi(
                label = option.label,
                value = option.value,
                selected = currentValue == option.value,
            )
        },
        textValue = currentValue,
        placeholder = placeholder,
        maxLength = maxLength,
        textLimitCounter = maxLength?.let { "${currentValue.length.coerceAtMost(it)}/$it" },
        uploadMediaType = uploadMediaKind?.toQuickCreateMediaType(),
        uploadHint = uploadHintParts().joinToString(" · "),
        childFields = childFields.map { child -> child.toQuickCreationServiceFieldUi(indentLevel = indentLevel + 1) },
        indentLevel = indentLevel,
    )
}

private fun QuickCreationResolvedFieldKind.toQuickCreationServiceFieldControlType(): QuickCreationServiceFieldControlType =
    when {
        this == QuickCreationResolvedFieldKind.OPTIONS -> QuickCreationServiceFieldControlType.OPTIONS
        this == QuickCreationResolvedFieldKind.TEXT -> QuickCreationServiceFieldControlType.TEXT
        else -> QuickCreationServiceFieldControlType.UPLOAD
    }

private fun QuickCreationResolvedServiceField.uploadHintParts(): List<String> =
    listOfNotNull(
        acceptFormats.takeIf { it.isNotEmpty() }?.joinToString("/"),
        maxUploadCount?.let { "最多 $it 个文件" },
        maxUploadSizeBytes?.let { "单文件 ${it / 1024 / 1024}MB" },
    )

private fun QuickCreationUploadMediaKind.toQuickCreateMediaType(): QuickCreateMediaType =
    when (this) {
        QuickCreationUploadMediaKind.IMAGE -> QuickCreateMediaType.IMAGE
        QuickCreationUploadMediaKind.VIDEO -> QuickCreateMediaType.VIDEO
        QuickCreationUploadMediaKind.AUDIO -> QuickCreateMediaType.AUDIO
    }

internal fun List<MediaReference>.quickCreationGlobalMediaReferences(): List<MediaReference> =
    filter { it.fieldParamKey.isNullOrBlank() }

internal fun List<MediaReference>.quickCreationFieldMediaReferences(paramKey: String): List<MediaReference> =
    filter { it.fieldParamKey == paramKey }

internal fun List<MediaReference>.quickCreationRelevantMediaReferences(
    activeFieldParamKeys: Set<String>,
): List<MediaReference> =
    filter { reference ->
        reference.fieldParamKey.isNullOrBlank() || reference.fieldParamKey in activeFieldParamKeys
    }
