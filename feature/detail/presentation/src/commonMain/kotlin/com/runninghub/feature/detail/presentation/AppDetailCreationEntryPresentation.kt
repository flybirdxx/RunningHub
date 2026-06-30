package com.runninghub.feature.detail.presentation

/** AppDetail 首屏创作入口的展示区块顺序。 */
enum class AppDetailCreationSection {
    PURPOSE,
    REQUIRED_INPUTS,
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
 * @property amountLabel 已格式化费用摘要；为空时由 composeApp 映射“价格待确认”资源文案。
 */
data class AppDetailEstimatedCostUi(
    val kind: AppDetailEstimatedCostKind,
    val amountLabel: String? = null,
)

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
 * AppDetail 必要输入摘要。
 *
 * @property inputKey 输入字段稳定 key。
 * @property title 普通用户可理解的字段标题。
 * @property filled 当前字段是否已有值。
 */
data class AppDetailRequiredInputUi(
    val inputKey: String,
    val title: String,
    val filled: Boolean,
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
 * @property purpose 用途摘要，来自详情描述清洗结果或模板名称兜底。
 * @property requiredInputs 必要输入摘要。
 * @property estimatedCost 预计费用状态。
 * @property primaryAction 首屏主操作。
 * @property firstScreenSections 首屏固定展示顺序。
 * @property technicalDetailsExpanded 技术详情默认是否展开，RM-11 固定为 false。
 * @property technicalDetails 后置技术信息，不作为首屏主要内容。
 */
data class AppDetailCreationEntryUiModel(
    val title: String,
    val purpose: String,
    val requiredInputs: List<AppDetailRequiredInputUi>,
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
        return AppDetailCreationEntryUiModel(
            title = detail.name.orEmpty().ifBlank { detail.id },
            purpose = detail.description
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()
                ?.ifBlank { null }
                ?: detail.name.orEmpty(),
            requiredInputs = detail.inputNodes.map { node ->
                val key = appDetailInputKey(node)
                AppDetailRequiredInputUi(
                    inputKey = key,
                    title = node.description?.takeIf { it.isNotBlank() } ?: node.fieldName,
                    filled = !inputValues[key].isNullOrBlank(),
                )
            },
            estimatedCost = AppDetailEstimatedCostUi(kind = AppDetailEstimatedCostKind.UNKNOWN),
            primaryAction = AppDetailCreationPrimaryActionUi(
                type = when {
                    taskOutputs.isNotEmpty() -> AppDetailCreationPrimaryAction.VIEW_RESULT
                    taskError != null -> AppDetailCreationPrimaryAction.RETRY
                    else -> AppDetailCreationPrimaryAction.GENERATE_NOW
                },
                enabled = !isRunningTask && uploadingNodes.values.none { !it.isError },
            ),
            firstScreenSections = listOf(
                AppDetailCreationSection.PURPOSE,
                AppDetailCreationSection.REQUIRED_INPUTS,
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
