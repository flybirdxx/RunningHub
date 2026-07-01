package com.runninghub.feature.detail.presentation

import com.runninghub.core.model.InputNode

/**
 * AppDetail 输入字段在 UI 中应使用的控件语义。
 *
 * 本类型只描述输入控件类别和运行时服务端选项，不依赖 Compose、Material 组件或平台媒体选择器。
 * composeApp 负责把这些语义映射为下拉框、分段按钮、文本框或上传控件。
 */
sealed interface AppDetailInputControl {
    /**
     * 服务端提供固定候选项的下拉选择。
     *
     * @property options 服务端字段配置解析出的候选项；空列表表示没有可展示候选项，调用方应避免展示空菜单。
     */
    data class Dropdown(
        val options: List<String>,
    ) : AppDetailInputControl

    /**
     * 服务端提供固定候选项的分段选择。
     *
     * @property options 服务端字段配置解析出的候选项；空列表不会生成该控件，会降级为 [BooleanSwitch]。
     */
    data class Segmented(
        val options: List<String>,
    ) : AppDetailInputControl

    /**
     * 媒体上传控件。
     *
     * @property mediaType 期望选择的媒体类型，由字段类型或服务端字段标题启发式推断。
     */
    data class MediaUpload(
        val mediaType: AppDetailMediaType,
    ) : AppDetailInputControl

    /** 布尔开关控件，值使用字符串 `true` / `false` 写回服务端字段。 */
    data object BooleanSwitch : AppDetailInputControl

    /** 整数输入框。 */
    data object IntegerText : AppDetailInputControl

    /** 浮点数输入框。 */
    data object DecimalText : AppDetailInputControl

    /**
     * 普通文本输入框。
     *
     * @property multiline `true` 表示字段配置或默认文本需要多行输入，UI 应至少展示多行文本框；
     * `false` 表示短文本输入。
     */
    data class Text(
        val multiline: Boolean,
    ) : AppDetailInputControl
}

/**
 * AppDetail 输入字段的结构化 UI 模型。
 *
 * @property nodeId 输入节点 ID，用于上传状态、媒体选择和任务提交回写。
 * @property fieldName 输入字段名，用于任务提交回写和稳定输入 key。
 * @property inputKey [AppDetailUiState.inputValues] 使用的稳定 key。
 * @property title 字段展示标题，优先使用服务端描述或节点名；为空表示接口未提供用户可读标题。
 * @property currentValue 当前输入值，来自页面状态或节点默认值；允许为空字符串。
 * @property control 当前字段应使用的控件语义。
 */
data class AppDetailInputFieldUiModel(
    val nodeId: String,
    val fieldName: String,
    val inputKey: String,
    val title: String,
    val currentValue: String,
    val control: AppDetailInputControl,
)

/**
 * AppDetail 输入区的行级结构。
 *
 * 连续多个图片上传字段会被聚合成同一行横向上传区；其他字段保持单字段行。
 */
sealed interface AppDetailInputRowUiModel {
    /**
     * 单个输入字段。
     *
     * @property field 可直接渲染的字段模型。
     */
    data class Single(
        val field: AppDetailInputFieldUiModel,
    ) : AppDetailInputRowUiModel

    /**
     * 连续图片上传字段组。
     *
     * @property fields 图片上传字段模型；至少包含两个字段，少于两个时调用方应使用 [Single]。
     */
    data class ImageUploadGroup(
        val fields: List<AppDetailInputFieldUiModel>,
    ) : AppDetailInputRowUiModel
}

/**
 * 返回当前详情输入节点的结构化渲染行。
 *
 * @return 由 Presentation 规则解析后的输入行；详情尚未加载时返回空列表。
 */
fun AppDetailUiState.inputRows(): List<AppDetailInputRowUiModel> =
    appDetailInputRows(detail?.inputNodes.orEmpty(), inputValues)

/**
 * 把服务端输入节点解析为 AppDetail 输入区行模型。
 *
 * 本函数集中维护字段类型到控件、媒体类型启发式、默认值读取和连续图片上传聚合规则。
 * composeApp 不应再重复解析 [InputNode.fieldType] 或自行判断服务端标题关键字。
 *
 * @param inputNodes 服务端返回的输入节点，顺序决定页面渲染顺序。
 * @param inputValues 当前页面编辑值，key 必须由 [appDetailInputKey] 生成。
 * @return 可供 UI 直接渲染的输入行模型。
 */
fun appDetailInputRows(
    inputNodes: List<InputNode>,
    inputValues: Map<String, String>,
): List<AppDetailInputRowUiModel> {
    val fields = inputNodes.map { node -> node.toInputFieldUiModel(inputValues) }
    return buildList {
        var index = 0
        while (index < fields.size) {
            val field = fields[index]
            val mediaType = (field.control as? AppDetailInputControl.MediaUpload)?.mediaType
            if (mediaType == AppDetailMediaType.IMAGE) {
                val imageFields = mutableListOf<AppDetailInputFieldUiModel>()
                var cursor = index
                while (cursor < fields.size) {
                    val nextField = fields[cursor]
                    val nextMediaType = (nextField.control as? AppDetailInputControl.MediaUpload)?.mediaType
                    if (nextMediaType != AppDetailMediaType.IMAGE) break
                    imageFields += nextField
                    cursor++
                }
                if (imageFields.size > 1) {
                    add(AppDetailInputRowUiModel.ImageUploadGroup(imageFields))
                    index = cursor
                } else {
                    add(AppDetailInputRowUiModel.Single(field))
                    index++
                }
            } else {
                add(AppDetailInputRowUiModel.Single(field))
                index++
            }
        }
    }
}

private fun InputNode.toInputFieldUiModel(
    inputValues: Map<String, String>,
): AppDetailInputFieldUiModel {
    val inputKey = appDetailInputKey(this)
    return AppDetailInputFieldUiModel(
        nodeId = nodeId,
        fieldName = fieldName,
        inputKey = inputKey,
        title = displayTitle(),
        currentValue = inputValues[inputKey] ?: fieldValue ?: "",
        control = inputControl(),
    )
}

private fun InputNode.inputControl(): AppDetailInputControl {
    mediaType()?.let { mediaType ->
        return AppDetailInputControl.MediaUpload(mediaType)
    }
    val options = getOptions()
    return when (fieldType.uppercase()) {
        "LIST" -> AppDetailInputControl.Dropdown(options)
        "BOOLEAN" -> AppDetailInputControl.BooleanSwitch
        "SWITCH" -> {
            if (options.isNotEmpty()) {
                AppDetailInputControl.Segmented(options)
            } else {
                AppDetailInputControl.BooleanSwitch
            }
        }
        "INT" -> AppDetailInputControl.IntegerText
        "FLOAT" -> AppDetailInputControl.DecimalText
        "STRING", "TEXT" -> AppDetailInputControl.Text(multiline = isTextInputMultiline())
        else -> {
            if (options.isNotEmpty()) {
                AppDetailInputControl.Dropdown(options)
            } else {
                AppDetailInputControl.Text(multiline = false)
            }
        }
    }
}

private fun InputNode.isTextInputMultiline(): Boolean =
    fieldData?.contains("multiline", ignoreCase = true) == true ||
        fieldValue.orEmpty().contains('\n') ||
        fieldValue.orEmpty().length >= APP_DETAIL_MULTILINE_TEXT_THRESHOLD

private fun InputNode.mediaType(): AppDetailMediaType? {
    val type = fieldType.uppercase()
    val label = listOfNotNull(fieldName, nodeName, description, descriptionEn)
        .joinToString(" ")
        .lowercase()
    val value = fieldValue.orEmpty().lowercase()
    return when {
        type == "IMAGE" || type == "IMAGE_UPLOAD" -> AppDetailMediaType.IMAGE
        type == "VIDEO" || type == "VIDEO_UPLOAD" -> AppDetailMediaType.VIDEO
        type == "AUDIO" || type == "AUDIO_UPLOAD" -> AppDetailMediaType.AUDIO
        label.containsAny(APP_DETAIL_VIDEO_LABEL_HINTS) -> AppDetailMediaType.VIDEO
        label.containsAll(APP_DETAIL_UPLOAD_LABEL_HINTS, APP_DETAIL_VIDEO_MEDIA_HINTS) -> AppDetailMediaType.VIDEO
        value.endsWithAny(APP_DETAIL_VIDEO_FILE_SUFFIXES) -> AppDetailMediaType.VIDEO
        label.containsAny(APP_DETAIL_AUDIO_LABEL_HINTS) -> AppDetailMediaType.AUDIO
        label.containsAll(APP_DETAIL_UPLOAD_LABEL_HINTS, APP_DETAIL_AUDIO_MEDIA_HINTS) -> AppDetailMediaType.AUDIO
        label.containsAny(APP_DETAIL_IMAGE_LABEL_HINTS) -> AppDetailMediaType.IMAGE
        label.containsAll(APP_DETAIL_UPLOAD_LABEL_HINTS, APP_DETAIL_IMAGE_MEDIA_HINTS) -> AppDetailMediaType.IMAGE
        else -> null
    }
}

private fun String.containsAny(hints: List<String>): Boolean =
    hints.any { hint -> contains(hint) }

private fun String.containsAll(firstHints: List<String>, secondHints: List<String>): Boolean =
    containsAny(firstHints) && containsAny(secondHints)

private fun String.endsWithAny(suffixes: List<String>): Boolean =
    substringBefore('?').substringBefore('#').trim().let { value ->
        suffixes.any { suffix -> value.endsWith(suffix) }
    }

private val APP_DETAIL_UPLOAD_LABEL_HINTS = listOf(
    "\u4e0a\u4f20",
    "upload",
)

private val APP_DETAIL_VIDEO_MEDIA_HINTS = listOf(
    "\u89c6\u9891",
    "\u5f55\u50cf",
    "video",
)

private val APP_DETAIL_AUDIO_MEDIA_HINTS = listOf(
    "\u97f3\u9891",
    "\u97f3\u4e50",
    "audio",
    "music",
)

private val APP_DETAIL_IMAGE_MEDIA_HINTS = listOf(
    "\u56fe\u7247",
    "\u56fe\u50cf",
    "image",
    "photo",
)

private val APP_DETAIL_VIDEO_LABEL_HINTS = listOf(
    "\u4e0a\u4f20\u89c6\u9891",
    "\u4e0a\u4f20\u5f55\u50cf",
    "upload video",
    "video upload",
    "video file",
)

private val APP_DETAIL_AUDIO_LABEL_HINTS = listOf(
    "\u4e0a\u4f20\u97f3\u9891",
    "\u4e0a\u4f20\u97f3\u4e50",
    "upload audio",
    "audio upload",
    "audio file",
)

private val APP_DETAIL_IMAGE_LABEL_HINTS = listOf(
    "\u4e0a\u4f20\u56fe\u7247",
    "\u4e0a\u4f20\u56fe\u50cf",
    "upload image",
    "image upload",
)

private val APP_DETAIL_VIDEO_FILE_SUFFIXES = listOf(
    ".mp4",
    ".mov",
    ".m4v",
    ".avi",
    ".mkv",
    ".webm",
)

private const val APP_DETAIL_MULTILINE_TEXT_THRESHOLD = 80
