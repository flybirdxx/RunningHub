package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationProject
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 快捷创作项目列表使用的 Presentation UI 模型。
 *
 * 项目接口返回的 [QuickCreationProject] 是 Domain 层任务组织实体；项目名称、任务数量文案、
 * 置顶文案和封面展示策略属于当前页面展示边界。该模型把这些字段在写入 UiState 前统一派生，
 * 避免 Composable 直接理解服务端字段和时间戳格式。
 *
 * @property source 原始项目实体，保留给迁移期测试和后续可能的下载、分享等非展示操作使用。
 * Composable 不应继续从该对象派生展示文案，应使用下列稳定字段。
 * @property projectId 项目稳定 ID，来自服务端项目列表。
 * 空字符串表示异常数据；StateHolder 会拒绝对空 ID 发起远程变更。
 * @property name 项目名称，来自服务端或用户新建/重命名输入。
 * 空字符串表示服务端返回异常名称，UI 仍按空标题渲染，不在展示层自行兜底。
 * @property coverUrl 项目封面远程地址。
 * `null` 表示服务端未提供有效封面；空字符串会在映射时转为 `null`，UI 直接省略封面。
 * @property taskCountText 任务数量展示文本，格式为“n 个任务”。
 * 服务端任务数小于 0 时按原值展示，便于暴露异常数据而不是在 UI 层静默修正。
 * @property isPinned 当前项目是否置顶。
 * `true` 表示项目置顶并使用强调边框；`false` 表示普通项目排序状态。
 * @property pinContentDescription 置顶按钮无障碍文本。
 * `isPinned=true` 时提示取消置顶，`false` 时提示置顶项目。
 * @property pinStatusText 详情页使用的置顶状态文案。
 * `isPinned=true` 时为“已置顶”，`false` 时为“未置顶”。
 * @property deleteConfirmationText 删除确认弹窗正文。
 * 该文案由项目名称派生，集中在映射层生成，避免 Composable 拼接业务提示。
 */
data class QuickCreateProjectUiItem(
    val source: QuickCreationProject,
    val projectId: String,
    val name: String,
    val coverUrl: String?,
    val taskCountText: String,
    val isPinned: Boolean,
    val pinContentDescription: String,
    val pinStatusText: String,
    val deleteConfirmationText: String,
)

/**
 * 快捷创作项目详情弹窗使用的 Presentation UI 模型。
 *
 * 详情弹窗只负责渲染已请求到的项目快照；封面、标题和详情行均在映射阶段派生，
 * 以便 ProjectStateHolder 统一处理服务端时间戳兼容策略。
 *
 * @property source 原始项目详情实体，保留给迁移期测试使用。
 * @property projectId 项目稳定 ID，来自详情接口返回值。
 * @property name 项目名称，来自详情接口返回值。
 * @property coverUrl 详情封面远程地址；`null` 表示没有可展示封面。
 * @property rows 详情弹窗中按顺序展示的键值行。
 * 空集合表示除标题和封面外没有额外详情，当前实现至少包含任务数量和置顶状态。
 */
data class QuickCreateProjectDetailUiItem(
    val source: QuickCreationProject,
    val projectId: String,
    val name: String,
    val coverUrl: String?,
    val rows: List<QuickCreateProjectDetailRowUi>,
)

/**
 * 项目详情弹窗中的一行展示数据。
 *
 * @property label 左侧字段名，由 Presentation 层固定生成。
 * 空字符串表示异常配置，UI 仍会展示以暴露问题。
 * @property value 右侧字段值，已完成时间戳和状态转换。
 * 空字符串表示服务端返回空值或映射层没有可展示内容。
 */
data class QuickCreateProjectDetailRowUi(
    val label: String,
    val value: String,
)

/**
 * 将 Domain 项目映射为列表可渲染模型。
 */
internal fun QuickCreationProject.toQuickCreateProjectUiItem(): QuickCreateProjectUiItem =
    QuickCreateProjectUiItem(
        source = this,
        projectId = projectId,
        name = name,
        coverUrl = coverUrl.takeUnless { it.isNullOrBlank() },
        taskCountText = "$taskCount 个任务",
        isPinned = pinned,
        pinContentDescription = pinned.toProjectPinContentDescription(),
        pinStatusText = pinned.toProjectPinStatusText(),
        deleteConfirmationText = "删除「$name」后，项目入口会从当前列表移除。",
    )

/**
 * 将 Domain 项目详情映射为弹窗可渲染模型。
 *
 * 服务端当前返回毫秒时间戳字符串；如果格式异常，映射层保留原始值，
 * 避免单个时间字段解析失败导致整个详情弹窗不可用。
 */
internal fun QuickCreationProject.toQuickCreateProjectDetailUiItem(): QuickCreateProjectDetailUiItem =
    QuickCreateProjectDetailUiItem(
        source = this,
        projectId = projectId,
        name = name,
        coverUrl = coverUrl.takeUnless { it.isNullOrBlank() },
        rows = buildList {
            add(QuickCreateProjectDetailRowUi(label = "任务数量", value = taskCount.toString()))
            add(QuickCreateProjectDetailRowUi(label = "置顶状态", value = pinned.toProjectPinStatusText()))
            createdAt?.let { add(QuickCreateProjectDetailRowUi(label = "创建时间", value = formatProjectTime(it))) }
            updatedAt?.let { add(QuickCreateProjectDetailRowUi(label = "更新时间", value = formatProjectTime(it))) }
        },
    )

private fun Boolean.toProjectPinContentDescription(): String =
    if (this) "取消置顶项目" else "置顶项目"

private fun Boolean.toProjectPinStatusText(): String =
    if (this) "已置顶" else "未置顶"

private fun formatProjectTime(raw: String): String {
    val millis = raw.toLongOrNull() ?: return raw
    val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(localDateTime.year.toString().padStart(4, '0'))
        append('-')
        append(localDateTime.monthNumber.toString().padStart(2, '0'))
        append('-')
        append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        append(' ')
        append(localDateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(localDateTime.minute.toString().padStart(2, '0'))
    }
}
