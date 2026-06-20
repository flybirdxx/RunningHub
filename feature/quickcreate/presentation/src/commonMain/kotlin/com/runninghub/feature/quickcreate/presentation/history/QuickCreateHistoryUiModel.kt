package com.runninghub.feature.quickcreate.presentation.history

import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryOutput
import kotlin.math.abs
import kotlin.math.roundToInt

private val terminalQuickCreationHistoryStatuses = setOf("SUCCESS", "FAILED", "FAIL", "ERROR", "CANCELED", "CANCELLED")

/**
 * 快捷创作历史输出在 Presentation 层使用的媒体类型。
 *
 * 历史列表和详情弹窗只需要知道输出应按图片还是视频方式渲染；服务端返回的 `type`
 * 字符串和 URL 后缀兼容规则统一收敛在映射层，避免 Composable 直接理解接口格式。
 */
enum class QuickCreateHistoryOutputMediaType {
    /** 图片或无法识别的历史输出，页面按静态预览图展示。 */
    IMAGE,

    /** 视频历史输出，页面在缩略图上叠加播放标识。 */
    VIDEO,
}

/**
 * 快捷创作历史输出的可渲染 UI 模型。
 *
 * @property source 原始历史输出实体，保留给迁移期测试和后续下载、保存等操作使用。
 * Composable 不应继续从该对象派生展示文案或播放形态，应使用下列稳定字段。
 * @property outputId 历史详情接口使用的输出 ID，空字符串表示服务端返回了不可打开的异常输出。
 * @property mediaType 根据服务端输出类型和 URL 后缀映射后的稳定媒体类型。
 * 当服务端类型未知但 URL 指向常见视频格式时仍按视频展示；都无法识别时降级为图片。
 * @property previewUrl 优先使用缩略图地址，其次使用输出原图/视频地址。
 * `null` 表示服务端没有提供可展示地址，UI 应展示占位图标并禁止打开详情。
 * @property typeLabel 输出类型的大写展示文本。
 * `null` 表示服务端未返回有效类型，元信息中应省略该段。
 * @property sizeLabel 输出尺寸展示文本，格式为 `宽x高`，缺失尺寸用 `-` 占位。
 */
data class QuickCreateHistoryOutputUi(
    val source: QuickCreationHistoryOutput,
    val outputId: String,
    val mediaType: QuickCreateHistoryOutputMediaType,
    val previewUrl: String?,
    val typeLabel: String?,
    val sizeLabel: String,
)

/**
 * 快捷创作历史列表使用的 Presentation UI 模型。
 *
 * 历史接口返回的 [QuickCreationHistoryItem] 仍是 Domain 层任务实体；列表是否展示取消入口、
 * 是否需要继续轮询刷新、首个输出如何渲染，则属于当前页面展示和交互边界。该模型把这些
 * 派生状态集中在 StateHolder 写入 UiState 前计算，避免 Composable 直接复制服务端状态字符串规则。
 *
 * @property source 原始历史任务实体，来自历史 StateHolder 请求 Domain Repository 后的返回值。
 * 该字段用于迁移期保留 Domain 任务上下文，Composable 不应继续从它派生展示文案。
 * @property taskId 历史任务 ID，用于列表 key、取消任务和测试定位。
 * @property title 列表主标题，优先使用提示词，提示词为空时回退为任务 ID。
 * @property metadataText 列表元信息文本，按服务端返回顺序组合分类、状态和输出类型。
 * @property cashText 计费展示文本；`null` 表示没有正向现金金额，不展示计费行。
 * @property primaryOutput 列表卡片使用的首个输出 UI 模型。
 * `null` 表示任务尚未产生输出或服务端未返回输出列表，此时列表卡片不可点击。
 * @property canCancelTask 当前列表是否允许对该任务发起取消请求。
 * `true` 表示服务端状态非空且尚未进入成功、失败、错误或取消等终态，UI 可以显示取消入口；
 * `false` 表示任务已处于终态或状态为空，StateHolder 会拒绝提交取消请求。
 * @property needsRefresh 当前任务是否需要历史轮询继续刷新。
 * `true` 表示任务仍可能从排队/运行态进入终态；`false` 表示轮询不应因该条记录继续保持。
 * 当前规则与 [canCancelTask] 相同，但保留独立字段，便于未来出现“可轮询但不可取消”的状态。
 */
data class QuickCreateHistoryUiItem(
    val source: QuickCreationHistoryItem,
    val taskId: String,
    val title: String,
    val metadataText: String,
    val cashText: String?,
    val primaryOutput: QuickCreateHistoryOutputUi?,
    val canCancelTask: Boolean,
    val needsRefresh: Boolean,
)

/**
 * 快捷创作历史详情弹窗使用的 Presentation UI 模型。
 *
 * 详情接口返回完整的 Domain 历史任务；弹窗仍需要读取提示词、状态和计费信息，但输出预览
 * 和媒体类型应由映射层提前派生，避免详情弹窗和列表卡片维护两套视频判断规则。
 *
 * @property source 原始历史详情任务，来自详情接口返回值。
 * @property title 详情主标题，优先使用提示词，提示词为空时回退为任务 ID。
 * @property metadataText 详情元信息文本，包含分类、状态、输出类型和尺寸。
 * @property cashText 详情计费展示文本；`null` 表示不需要展示计费行。
 * @property primaryOutput 详情弹窗展示的首个输出 UI 模型；为空时表示详情没有可预览输出。
 */
data class QuickCreateHistoryDetailUiItem(
    val source: QuickCreationHistoryItem,
    val title: String,
    val metadataText: String,
    val cashText: String?,
    val primaryOutput: QuickCreateHistoryOutputUi?,
)

/**
 * 将 Domain 历史任务映射为页面可渲染模型。
 *
 * 服务端状态字符串是开放集合，因此这里沿用“非空且非已知终态即活跃”的兼容策略，
 * 既能支持 `PREPAID`、`QUEUED`、`RUNNING` 等现有状态，也避免终态任务继续展示取消入口。
 */
fun QuickCreationHistoryItem.toQuickCreateHistoryUiItem(): QuickCreateHistoryUiItem {
    val isActive = status.isNotBlank() && status.uppercase() !in terminalQuickCreationHistoryStatuses
    val primaryOutput = outputs.firstOrNull()?.toQuickCreateHistoryOutputUi()
    return QuickCreateHistoryUiItem(
        source = this,
        taskId = taskId,
        title = toHistoryTitle(),
        metadataText = toHistoryListMetadataText(primaryOutput),
        cashText = toHistoryCashText(),
        primaryOutput = primaryOutput,
        canCancelTask = isActive,
        needsRefresh = isActive,
    )
}

/**
 * 将历史详情任务映射为弹窗可渲染模型。
 *
 * 详情和列表都只展示首个输出，因此复用同一输出映射规则；后续如果支持多输出轮播，
 * 应在这里扩展为输出列表，而不是让 Composable 重新解析 Domain 输出集合。
 */
fun QuickCreationHistoryItem.toQuickCreateHistoryDetailUiItem(): QuickCreateHistoryDetailUiItem {
    val primaryOutput = outputs.firstOrNull()?.toQuickCreateHistoryOutputUi()
    return QuickCreateHistoryDetailUiItem(
        source = this,
        title = toHistoryTitle(),
        metadataText = toHistoryDetailMetadataText(primaryOutput),
        cashText = toHistoryCashText(),
        primaryOutput = primaryOutput,
    )
}

private fun QuickCreationHistoryOutput.toQuickCreateHistoryOutputUi(): QuickCreateHistoryOutputUi =
    QuickCreateHistoryOutputUi(
        source = this,
        outputId = outputId,
        mediaType = toQuickCreateHistoryOutputMediaType(),
        previewUrl = thumbnailUrl?.takeIf { it.isNotBlank() } ?: url.takeIf { it.isNotBlank() },
        typeLabel = type.uppercase().takeIf { it.isNotBlank() },
        sizeLabel = "${width ?: "-"}x${height ?: "-"}",
    )

private fun QuickCreationHistoryItem.toHistoryTitle(): String =
    params["prompt"]?.takeIf { it.isNotBlank() } ?: taskId

private fun QuickCreationHistoryItem.toHistoryListMetadataText(
    primaryOutput: QuickCreateHistoryOutputUi?,
): String = listOfNotNull(categoryId, status, primaryOutput?.typeLabel).joinToString(" · ")

private fun QuickCreationHistoryItem.toHistoryDetailMetadataText(
    primaryOutput: QuickCreateHistoryOutputUi?,
): String = listOfNotNull(categoryId, status, primaryOutput?.typeLabel, primaryOutput?.sizeLabel)
    .joinToString(" · ")

private fun QuickCreationHistoryItem.toHistoryCashText(): String? =
    if (cashAmount > 0.0) {
        "${formatQuickCreateCashAmount(cashAmount)} ${cashCurrency.orEmpty()}"
    } else {
        null
    }

private fun QuickCreationHistoryOutput.toQuickCreateHistoryOutputMediaType(): QuickCreateHistoryOutputMediaType {
    val normalizedType = type.lowercase()
    if (normalizedType in setOf("mp4", "webm", "mov", "video")) {
        return QuickCreateHistoryOutputMediaType.VIDEO
    }

    // 部分历史接口会把视频输出标记为 file，此时只能依赖 URL 后缀兜底识别。
    val normalizedUrl = url.substringBefore('?').substringBefore('#').lowercase()
    return if (normalizedUrl.endsWith(".mp4") ||
        normalizedUrl.endsWith(".webm") ||
        normalizedUrl.endsWith(".mov")
    ) {
        QuickCreateHistoryOutputMediaType.VIDEO
    } else {
        QuickCreateHistoryOutputMediaType.IMAGE
    }
}

private fun formatQuickCreateCashAmount(value: Double): String {
    val scaled = (value * 100).roundToInt()
    val sign = if (scaled < 0) "-" else ""
    val absolute = abs(scaled)
    return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}
