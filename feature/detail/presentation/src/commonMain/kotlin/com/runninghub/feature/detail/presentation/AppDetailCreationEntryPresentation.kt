package com.runninghub.feature.detail.presentation

/** AppDetail 首屏创作入口的展示区块顺序。 */
enum class AppDetailCreationSection {
    DESCRIPTION,
    INPUT_NODES,
    ESTIMATED_COST,
    PRIMARY_ACTION,
    TECHNICAL_DETAILS,
}

/** AppDetail 首屏预计费用的稳定状态。 */
enum class AppDetailEstimatedCostKind {
    UNKNOWN,
}

/** AppDetail 首屏主操作类型。 */
enum class AppDetailCreationPrimaryAction {
    GENERATE_NOW,
    VIEW_RESULT,
    RETRY,
}

/**
 * AppDetail 首屏预计费用语义。
 *
 * @property kind 当前费用状态；没有稳定费用协议时使用 [AppDetailEstimatedCostKind.UNKNOWN]。
 * @property amountLabel 已格式化费用摘要；为空时由 composeApp 映射“运行前确认费用”资源文案。
 */
data class AppDetailEstimatedCostUi(
    val kind: AppDetailEstimatedCostKind,
    val amountLabel: String? = null,
)

sealed interface AppDetailInputNodeValuePreview {
    data object Missing : AppDetailInputNodeValuePreview

    data class Text(val value: String) : AppDetailInputNodeValuePreview

    data object MediaProvided : AppDetailInputNodeValuePreview
}

/**
 * AppDetail 首屏主操作语义。
 *
 * @property type 当前应展示的主操作。
 * @property enabled 是否允许点击；任务运行或上传中时应为 false。
 */
data class AppDetailCreationPrimaryActionUi(
    val type: AppDetailCreationPrimaryAction,
    val enabled: Boolean,
)

/**
 * AppDetail 输入节点摘要。
 *
 * @property inputKey 输入字段稳定 key。
 * @property title 服务端返回的可展示字段标题；为空表示接口未提供用户可读标题。
 * @property filled 当前字段是否已有值。
 * @property valuePreview 当前字段值的安全摘要；仅由 inputNodes 默认值或当前编辑值派生。
 */
data class AppDetailInputNodeSummaryUi(
    val inputKey: String,
    val title: String,
    val filled: Boolean,
    val valuePreview: AppDetailInputNodeValuePreview,
)

/**
 * AppDetail 技术详情摘要。
 *
 * @property key 技术字段稳定键名，不直接作为首屏用户文案。
 * @property value 技术字段值，空值不会进入列表。
 */
data class AppDetailTechnicalDetailUi(
    val key: String,
    val value: String,
)

/**
 * AppDetail 首屏创作入口 UI 模型。
 *
 * @property title 模板名称。
 * @property description 详情简介，来自接口 description 清洗结果；为空表示接口未提供。
 * @property inputNodes 输入节点摘要，只由接口 inputNodes 映射。
 * @property estimatedCost 预计费用状态。
 * @property primaryAction 首屏主操作。
 * @property firstScreenSections 首屏固定展示顺序。
 * @property technicalDetailsExpanded 技术详情默认是否展开，RM-11 固定为 false。
 * @property technicalDetails 后置技术信息，不作为首屏主要内容。
 */
data class AppDetailCreationEntryUiModel(
    val title: String,
    val description: String?,
    val inputNodes: List<AppDetailInputNodeSummaryUi>,
    val estimatedCost: AppDetailEstimatedCostUi,
    val primaryAction: AppDetailCreationPrimaryActionUi,
    val firstScreenSections: List<AppDetailCreationSection>,
    val technicalDetailsExpanded: Boolean,
    val technicalDetails: List<AppDetailTechnicalDetailUi>,
)

/** 当前详情状态对应的首屏创作入口语义。 */
val AppDetailUiState.creationEntry: AppDetailCreationEntryUiModel?
    get() {
        val detail = detail ?: return null
        val inputFieldByKey = appDetailInputRows(detail.inputNodes, inputValues)
            .flatMap { row ->
                when (row) {
                    is AppDetailInputRowUiModel.ImageUploadGroup -> row.fields
                    is AppDetailInputRowUiModel.Single -> listOf(row.field)
                }
            }
            .associateBy { it.inputKey }
        return AppDetailCreationEntryUiModel(
            title = detail.name.orEmpty().ifBlank { detail.id },
            description = detail.description
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()
                ?.ifBlank { null },
            inputNodes = detail.inputNodes.map { node ->
                val key = appDetailInputKey(node)
                val valuePreview = inputFieldByKey[key].toInputNodeValuePreview()
                AppDetailInputNodeSummaryUi(
                    inputKey = key,
                    title = node.displayTitle(),
                    filled = valuePreview !is AppDetailInputNodeValuePreview.Missing,
                    valuePreview = valuePreview,
                )
            },
            estimatedCost = AppDetailEstimatedCostUi(kind = AppDetailEstimatedCostKind.UNKNOWN),
            primaryAction = AppDetailCreationPrimaryActionUi(
                type = when {
                    taskOutputs.isNotEmpty() -> AppDetailCreationPrimaryAction.VIEW_RESULT
                    taskError != null -> AppDetailCreationPrimaryAction.RETRY
                    else -> AppDetailCreationPrimaryAction.GENERATE_NOW
                },
                enabled = !isRunningTask && uploadingNodes.isEmpty(),
            ),
            firstScreenSections = listOf(
                AppDetailCreationSection.DESCRIPTION,
                AppDetailCreationSection.INPUT_NODES,
                AppDetailCreationSection.ESTIMATED_COST,
                AppDetailCreationSection.PRIMARY_ACTION,
                AppDetailCreationSection.TECHNICAL_DETAILS,
            ),
            technicalDetailsExpanded = false,
            technicalDetails = buildList {
                detail.workflowId?.takeIf { it.isNotBlank() }?.let { add(AppDetailTechnicalDetailUi("workflowId", it)) }
                detail.instanceType?.takeIf { it.isNotBlank() }?.let { add(AppDetailTechnicalDetailUi("instanceType", it)) }
            },
        )
    }

internal fun com.runninghub.core.model.InputNode.displayTitle(): String =
    description?.takeIf { it.isNotBlank() }
        ?: descriptionEn?.takeIf { it.isNotBlank() }
        ?: nodeName.takeIf { it.isNotBlank() }
        ?: fieldName.takeIf { it.isNotBlank() }
        ?: nodeId.takeIf { it.isNotBlank() }
        ?: ""

private fun AppDetailInputFieldUiModel?.toInputNodeValuePreview(): AppDetailInputNodeValuePreview {
    val field = this ?: return AppDetailInputNodeValuePreview.Missing
    val value = field.currentValue.trim()
    if (value.isBlank()) return AppDetailInputNodeValuePreview.Missing
    return when (field.control) {
        is AppDetailInputControl.MediaUpload -> AppDetailInputNodeValuePreview.MediaProvided
        else -> AppDetailInputNodeValuePreview.Text(value.toSingleLinePreview())
    }
}

private fun String.toSingleLinePreview(): String {
    val normalized = lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
    return if (normalized.length <= APP_DETAIL_INPUT_VALUE_PREVIEW_MAX_CHARS) {
        normalized
    } else {
        normalized.take(APP_DETAIL_INPUT_VALUE_PREVIEW_MAX_CHARS).trimEnd() + "..."
    }
}

private const val APP_DETAIL_INPUT_VALUE_PREVIEW_MAX_CHARS = 48
