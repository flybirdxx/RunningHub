package com.runninghub.feature.quickcreate.presentation.fields

import com.runninghub.feature.quickcreate.domain.QuickCreationResolvedFieldKind
import com.runninghub.feature.quickcreate.domain.QuickCreationResolvedServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceSchema
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind

/**
 * 服务端动态字段在参数面板中的控件类型。
 *
 * 该类型属于 QuickCreate Presentation 层，只描述 UI 应该如何渲染字段，
 * 不泄漏服务端原始 `fieldType` 字符串，也不包含 Compose 类型。
 */
enum class QuickCreationServiceFieldControlType {
    /** 由服务端 options 渲染为横向选项组。 */
    OPTIONS,

    /** 由服务端文本或数字字段渲染为输入框。 */
    TEXT,

    /** 由服务端媒体字段渲染为上传入口。 */
    UPLOAD,
}

/**
 * 动态字段上传控件允许选择的媒体类型。
 *
 * 该枚举是动态字段层自己的稳定类型；参数面板会把它转换为 editor 包中的 `QuickCreateMediaType`，
 * 避免字段映射逻辑直接依赖媒体上传状态和编辑器素材模型。
 */
enum class QuickCreationServiceUploadMediaType {
    /** 上传字段只接受图片素材。 */
    IMAGE,

    /** 上传字段只接受视频素材。 */
    VIDEO,

    /** 上传字段只接受音频素材。 */
    AUDIO,
}

/**
 * 动态字段上传控件的结构化约束提示。
 *
 * 该模型位于 QuickCreate Presentation 层，只保存服务端约束的稳定数据，
 * 不拼接最终中文提示。composeApp 根据本模型使用 Compose Resources 生成用户可见文案。
 *
 * @property acceptFormats 服务端允许的文件格式列表，顺序保留服务端配置；
 * 空集合表示服务端未声明格式限制。元素通常为扩展名或 MIME 简写，不在本层大小写归一。
 * @property maxUploadCount 单次最多可选文件数量，来源于服务端 `maxInputCount`；
 * `null` 表示未声明数量上限，`0` 或负数保留原值用于暴露异常配置，不在本层静默修正。
 * @property maxUploadSizeMegabytes 单文件大小上限，单位为 MB，由服务端字节数向下换算；
 * `null` 表示未声明大小上限，`0` 表示服务端返回小于 1MB 的限制或异常配置。
 */
data class QuickCreationServiceUploadHint(
    val acceptFormats: List<String> = emptyList(),
    val maxUploadCount: Int? = null,
    val maxUploadSizeMegabytes: Long? = null,
) {
    /**
     * 上传控件是否存在任何可展示约束。
     *
     * @return `true` 表示至少有格式、数量或大小约束；`false` 表示 UI 可省略辅助提示。
     */
    fun hasConstraints(): Boolean =
        acceptFormats.isNotEmpty() || maxUploadCount != null || maxUploadSizeMegabytes != null
}

enum class QuickCreationServiceFieldSection {
    COMMON,
    UPLOAD,
    PROMPT,
    ADVANCED,
}

sealed interface QuickCreationServiceFieldDisplayLabel {
    data object AspectRatio : QuickCreationServiceFieldDisplayLabel
    data object Resolution : QuickCreationServiceFieldDisplayLabel
    data object Count : QuickCreationServiceFieldDisplayLabel
    data object TechnicalEndpoint : QuickCreationServiceFieldDisplayLabel
    data object Seed : QuickCreationServiceFieldDisplayLabel
    data object NegativePrompt : QuickCreationServiceFieldDisplayLabel
    data object Prompt : QuickCreationServiceFieldDisplayLabel
    data object Upload : QuickCreationServiceFieldDisplayLabel
    data class ServerText(val value: String) : QuickCreationServiceFieldDisplayLabel
}

enum class QuickCreationServiceFieldVisualState {
    DEFAULT,
    DISABLED,
    ERROR,
}

enum class QuickCreationServiceFieldOptionVisualState {
    DEFAULT,
    SELECTED,
    DISABLED,
    ERROR,
}

/**
 * 服务端动态字段选项的 UI 模型。
 *
 * @property label 选项展示文案，来自服务端 label；空字符串表示服务端未提供可读标题。
 * @property value 回传给服务端参数 Map 的原始值，必须保持服务端返回格式，不在 UI 层改写。
 * @property selected `true` 表示当前参数值等于 [value]；`false` 表示该选项未被选中。
 */
data class QuickCreationServiceFieldOptionUi(
    val label: String,
    val value: String,
    val selected: Boolean,
    val visualState: QuickCreationServiceFieldOptionVisualState,
)

/**
 * 服务端动态字段在参数面板中的 UI 模型。
 *
 * 该模型把字段类型判断、标题兜底、默认值、文本限制、上传提示和子字段激活规则收敛在映射层。
 * Composable 只根据 [controlType] 渲染控件，不再直接解析服务端 `fieldType`、`inputExtra`
 * 或 `visibleWhen`。
 *
 * @property paramKey 字段写回服务端参数 Map 使用的稳定键，空字符串表示服务端字段缺少可提交参数名。
 * @property title 用户可见字段标题，优先来自服务端标题配置，缺失时回退为参数名。
 * @property description 字段说明；`null` 表示服务端未提供说明，UI 应省略说明行。
 * @property controlType 当前字段应渲染的控件类型，由 Domain 解析后的字段种类派生。
 * @property options 选项控件的候选值；非选项控件为空集合，顺序保留服务端配置顺序。
 * @property textValue 文本控件当前值，来自用户参数 Map 或服务端默认值；空字符串表示尚未输入。
 * @property placeholder 文本控件占位文案；空字符串表示没有占位提示。
 * @property maxLength 文本最大长度；`null` 表示不限制，负数会在输入约束中视为不限制。
 * @property textLimitCounter 文本长度计数文案；`null` 表示不展示计数。
 * @property uploadMediaType 上传控件允许的媒体类型；`null` 表示使用通用上传入口。
 * @property uploadHint 上传控件辅助约束；无约束时 [QuickCreationServiceUploadHint.hasConstraints] 为 `false`。
 * @property childFields 当前已激活的子字段列表，顺序来自服务端配置，不允许重复渲染同一对象。
 * @property indentLevel 字段缩进层级，父字段为 0，子字段逐层递增。
 */
data class QuickCreationServiceFieldUi(
    val paramKey: String,
    val title: String,
    val displayLabel: QuickCreationServiceFieldDisplayLabel,
    val description: String?,
    val section: QuickCreationServiceFieldSection,
    val visualState: QuickCreationServiceFieldVisualState,
    val controlType: QuickCreationServiceFieldControlType,
    val required: Boolean,
    val options: List<QuickCreationServiceFieldOptionUi>,
    val textValue: String,
    val placeholder: String,
    val maxLength: Int?,
    val textLimitCounter: String?,
    val uploadMediaType: QuickCreationServiceUploadMediaType?,
    val uploadHint: QuickCreationServiceUploadHint,
    val childFields: List<QuickCreationServiceFieldUi>,
    val indentLevel: Int,
) {
    /**
     * 根据字段最大长度约束用户输入。
     *
     * @param value 用户刚输入的原始文本，可能超过服务端允许长度。
     * @return 不超过 [maxLength] 的文本；未配置最大长度或最大长度为负数时返回原值。
     */
    fun constrainTextInput(value: String): String =
        maxLength?.takeIf { it >= 0 }?.let { value.take(it) } ?: value
}

data class QuickCreateParameterSheetState(
    val commonFields: List<QuickCreationServiceFieldUi>,
    val promptFields: List<QuickCreationServiceFieldUi>,
    val advancedFields: List<QuickCreationServiceFieldUi>,
    val advancedCollapsed: Boolean = true,
) {
    val empty: Boolean
        get() = commonFields.isEmpty() && advancedFields.isEmpty()
}

fun quickCreateParameterSheetState(
    serviceFields: List<QuickCreationServiceFieldUi>,
): QuickCreateParameterSheetState {
    val flattenedFields = serviceFields.flattenQuickCreationServiceFields()
    return QuickCreateParameterSheetState(
        commonFields = flattenedFields.filter { field ->
            field.section == QuickCreationServiceFieldSection.COMMON ||
                field.section == QuickCreationServiceFieldSection.UPLOAD
        },
        promptFields = flattenedFields.filter { field -> field.section == QuickCreationServiceFieldSection.PROMPT },
        advancedFields = flattenedFields.filter { field -> field.section == QuickCreationServiceFieldSection.ADVANCED },
    )
}

/**
 * 将服务端模型的动态字段映射为参数面板 UI 模型。
 *
 * 字段可见性、fieldKey/paramKey 别名、当前值和 child 激活规则由 Domain schema 统一解析；
 * 本层只负责把平台无关字段描述映射为参数面板控件、媒体枚举和提示文案。
 *
 * @param params 当前图片或视频服务端参数 Map，key 必须与 Domain 解析出的字段参数一致。
 * @return 当前模型和参数组合下可渲染的字段列表，顺序保留服务端配置顺序。
 */
fun QuickCreationServiceModel?.quickCreationServiceFieldUiItems(
    params: Map<String, String>,
): List<QuickCreationServiceFieldUi> =
    QuickCreationServiceSchema.resolvedFields(
        model = this,
        serviceParams = params,
    ).map { field -> field.toQuickCreationServiceFieldUi(indentLevel = 0) }

private fun QuickCreationResolvedServiceField.toQuickCreationServiceFieldUi(
    indentLevel: Int,
): QuickCreationServiceFieldUi {
    val displayLabel = quickCreationServiceFieldDisplayLabel()
    return QuickCreationServiceFieldUi(
        paramKey = paramKey,
        title = title,
        displayLabel = displayLabel,
        description = description,
        section = quickCreationServiceFieldSection(displayLabel),
        visualState = quickCreationServiceFieldVisualState(),
        controlType = kind.toQuickCreationServiceFieldControlType(),
        required = required,
        options = options.map { option ->
            val selected = currentValue == option.value
            QuickCreationServiceFieldOptionUi(
                label = option.label,
                value = option.value,
                selected = selected,
                visualState = if (selected) {
                    QuickCreationServiceFieldOptionVisualState.SELECTED
                } else {
                    QuickCreationServiceFieldOptionVisualState.DEFAULT
                },
            )
        },
        textValue = currentValue,
        placeholder = placeholder,
        maxLength = maxLength,
        textLimitCounter = maxLength?.let { "${currentValue.length.coerceAtMost(it)}/$it" },
        uploadMediaType = uploadMediaKind?.toQuickCreationServiceUploadMediaType(),
        uploadHint = QuickCreationServiceUploadHint(
            acceptFormats = acceptFormats,
            maxUploadCount = maxUploadCount,
            maxUploadSizeMegabytes = maxUploadSizeBytes?.let { it / 1024 / 1024 },
        ),
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

private fun QuickCreationUploadMediaKind.toQuickCreationServiceUploadMediaType(): QuickCreationServiceUploadMediaType =
    when (this) {
        QuickCreationUploadMediaKind.IMAGE -> QuickCreationServiceUploadMediaType.IMAGE
        QuickCreationUploadMediaKind.VIDEO -> QuickCreationServiceUploadMediaType.VIDEO
        QuickCreationUploadMediaKind.AUDIO -> QuickCreationServiceUploadMediaType.AUDIO
    }

private fun QuickCreationResolvedServiceField.quickCreationServiceFieldDisplayLabel():
    QuickCreationServiceFieldDisplayLabel {
    val key = paramKey.quickCreationNormalizedFieldKey()
    return when {
        key.isPromptFieldKey() -> QuickCreationServiceFieldDisplayLabel.Prompt
        key.contains("aspectratio") || key == "ratio" -> QuickCreationServiceFieldDisplayLabel.AspectRatio
        key.contains("resolution") || key == "size" -> QuickCreationServiceFieldDisplayLabel.Resolution
        key == "count" || key.contains("num") || key.contains("batch") -> QuickCreationServiceFieldDisplayLabel.Count
        key.contains("endpoint") -> QuickCreationServiceFieldDisplayLabel.TechnicalEndpoint
        key.contains("seed") -> QuickCreationServiceFieldDisplayLabel.Seed
        key.contains("negative") -> QuickCreationServiceFieldDisplayLabel.NegativePrompt
        kind == QuickCreationResolvedFieldKind.UPLOAD -> QuickCreationServiceFieldDisplayLabel.Upload
        else -> QuickCreationServiceFieldDisplayLabel.ServerText(title.ifBlank { paramKey })
    }
}

private fun QuickCreationResolvedServiceField.quickCreationServiceFieldSection(
    displayLabel: QuickCreationServiceFieldDisplayLabel,
): QuickCreationServiceFieldSection {
    val key = paramKey.quickCreationNormalizedFieldKey()
    return when {
        displayLabel == QuickCreationServiceFieldDisplayLabel.Prompt -> QuickCreationServiceFieldSection.PROMPT
        kind == QuickCreationResolvedFieldKind.UPLOAD -> QuickCreationServiceFieldSection.UPLOAD
        displayLabel == QuickCreationServiceFieldDisplayLabel.AspectRatio ||
            displayLabel == QuickCreationServiceFieldDisplayLabel.Resolution ||
            displayLabel == QuickCreationServiceFieldDisplayLabel.Count -> QuickCreationServiceFieldSection.COMMON
        key.contains("endpoint") ||
            key.contains("seed") ||
            key.contains("negative") ||
            key.contains("workflow") ||
            key.contains("node") ||
            key.contains("lora") ||
            key.contains("model") -> QuickCreationServiceFieldSection.ADVANCED
        else -> QuickCreationServiceFieldSection.ADVANCED
    }
}

private fun QuickCreationResolvedServiceField.quickCreationServiceFieldVisualState():
    QuickCreationServiceFieldVisualState {
    val hasInvalidOption = kind == QuickCreationResolvedFieldKind.OPTIONS &&
        options.isNotEmpty() &&
        currentValue.isNotBlank() &&
        options.none { option -> option.value == currentValue }
    return when {
        paramKey.isBlank() -> QuickCreationServiceFieldVisualState.DISABLED
        kind == QuickCreationResolvedFieldKind.OPTIONS && options.isEmpty() ->
            QuickCreationServiceFieldVisualState.DISABLED
        required && currentValue.isBlank() -> QuickCreationServiceFieldVisualState.ERROR
        hasInvalidOption -> QuickCreationServiceFieldVisualState.ERROR
        else -> QuickCreationServiceFieldVisualState.DEFAULT
    }
}

private fun List<QuickCreationServiceFieldUi>.flattenQuickCreationServiceFields():
    List<QuickCreationServiceFieldUi> =
    flatMap { field -> listOf(field) + field.childFields.flattenQuickCreationServiceFields() }

private fun String.quickCreationNormalizedFieldKey(): String =
    lowercase()
        .filterNot { it == '_' || it == '-' || it.isWhitespace() }

private fun String.isPromptFieldKey(): Boolean =
    this == "prompt" ||
        this == "text" ||
        this == "input" ||
        this == "positiveprompt"
