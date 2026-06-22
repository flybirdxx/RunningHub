package com.runninghub.feature.quickcreate.presentation.project

import com.runninghub.feature.quickcreate.domain.QuickCreationProject
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
 * @property taskCountText 任务数量展示语义。
 * 服务端任务数小于 0 时按原值保留在 [QuickCreateProjectTaskCountText.count]，
 * 便于暴露异常数据而不是在 UI 层静默修正。
 * @property isPinned 当前项目是否置顶。
 * `true` 表示项目置顶并使用强调边框；`false` 表示普通项目排序状态。
 * @property pinContentDescription 置顶按钮无障碍语义。
 * `isPinned=true` 时要求 UI 映射为取消置顶提示，`false` 时要求 UI 映射为置顶提示。
 * @property pinStatusText 详情页使用的置顶状态语义。
 * `isPinned=true` 时表示已置顶，`false` 时表示未置顶，最终文案由 composeApp 映射。
 * @property deleteConfirmationText 删除确认弹窗正文语义。
 * 只保存项目名称，避免 Presentation 拼接最终中文文案。
 */
data class QuickCreateProjectUiItem(
    val source: QuickCreationProject,
    val projectId: String,
    val name: String,
    val coverUrl: String?,
    val taskCountText: QuickCreateProjectTaskCountText,
    val isPinned: Boolean,
    val pinContentDescription: QuickCreateProjectPinContentDescription,
    val pinStatusText: QuickCreateProjectPinStatusText,
    val deleteConfirmationText: QuickCreateProjectDeleteConfirmationText,
)

/**
 * 项目任务数量的稳定展示语义。
 *
 * 该类型只保留服务端任务数量，不拼接最终 UI 文案；composeApp 根据当前语言环境
 * 使用 Compose Resources 生成“n 个任务”等可见文本。
 *
 * @property count 项目内任务数量，来源于服务端项目列表或详情接口。
 * 允许为 `0` 表示空项目；负数表示服务端异常值，本层保留原值以便排查。
 */
data class QuickCreateProjectTaskCountText(
    val count: Int,
)

/**
 * 项目置顶按钮的无障碍语义。
 *
 * 枚举位于 Presentation 层，表达用户点击按钮后的目标动作；
 * 最终无障碍文本由 composeApp 通过资源映射，避免本层固定中文文案。
 */
enum class QuickCreateProjectPinContentDescription {
    /** 当前项目未置顶，按钮动作是置顶该项目。 */
    PinProject,

    /** 当前项目已置顶，按钮动作是取消置顶。 */
    UnpinProject,
}

/**
 * 项目置顶状态的稳定展示语义。
 *
 * 该枚举用于项目卡片和详情弹窗，表示当前项目是否已经置顶；
 * 不包含“已置顶/未置顶”等最终用户文案。
 */
enum class QuickCreateProjectPinStatusText {
    /** 当前项目已经置顶。 */
    Pinned,

    /** 当前项目未置顶。 */
    NotPinned,
}

/**
 * 删除项目确认正文的稳定展示语义。
 *
 * @property projectName 将被删除的项目名称，来自服务端或用户重命名后的本地状态。
 * 空字符串表示异常项目名，UI 仍按资源格式展示，避免删除动作缺少可追踪对象。
 */
data class QuickCreateProjectDeleteConfirmationText(
    val projectName: String,
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
 * @property label 左侧字段语义，由 Presentation 层固定生成，最终文案由 composeApp 映射。
 * @property value 右侧字段值语义。任务数和置顶状态保留为稳定结构；
 * 时间字段保留已格式化文本或服务端异常原文。
 */
data class QuickCreateProjectDetailRowUi(
    val label: QuickCreateProjectDetailRowLabel,
    val value: QuickCreateProjectDetailRowValue,
)

/**
 * 项目详情行左侧标签的稳定语义。
 */
enum class QuickCreateProjectDetailRowLabel {
    /** 项目内任务数量。 */
    TaskCount,

    /** 项目当前置顶状态。 */
    PinStatus,

    /** 项目创建时间。 */
    CreatedAt,

    /** 项目最近更新时间。 */
    UpdatedAt,
}

/**
 * 项目详情行右侧值的稳定语义。
 */
sealed interface QuickCreateProjectDetailRowValue {
    /**
     * 任务数量值。
     *
     * @property count 项目内任务数量，单位为个；允许 `0`，负数保留服务端异常值。
     */
    data class TaskCount(val count: Int) : QuickCreateProjectDetailRowValue

    /**
     * 置顶状态值。
     *
     * @property status 项目当前置顶状态，最终文案由 composeApp 映射。
     */
    data class PinStatus(val status: QuickCreateProjectPinStatusText) : QuickCreateProjectDetailRowValue

    /**
     * 时间或异常原文值。
     *
     * @property value 已格式化的本地时间，格式为 `yyyy-MM-dd HH:mm`；
     * 如果服务端时间戳无法解析，则保留原始字符串以便排查接口异常。
     */
    data class Timestamp(val value: String) : QuickCreateProjectDetailRowValue
}

/**
 * 将 Domain 项目映射为列表可渲染模型。
 */
fun QuickCreationProject.toQuickCreateProjectUiItem(): QuickCreateProjectUiItem =
    QuickCreateProjectUiItem(
        source = this,
        projectId = projectId,
        name = name,
        coverUrl = coverUrl.takeUnless { it.isNullOrBlank() },
        taskCountText = QuickCreateProjectTaskCountText(taskCount),
        isPinned = pinned,
        pinContentDescription = pinned.toProjectPinContentDescription(),
        pinStatusText = pinned.toProjectPinStatusText(),
        deleteConfirmationText = QuickCreateProjectDeleteConfirmationText(name),
    )

/**
 * 将 Domain 项目详情映射为弹窗可渲染模型。
 *
 * 服务端当前返回毫秒时间戳字符串；如果格式异常，映射层保留原始值，
 * 避免单个时间字段解析失败导致整个详情弹窗不可用。
 */
fun QuickCreationProject.toQuickCreateProjectDetailUiItem(): QuickCreateProjectDetailUiItem =
    QuickCreateProjectDetailUiItem(
        source = this,
        projectId = projectId,
        name = name,
        coverUrl = coverUrl.takeUnless { it.isNullOrBlank() },
        rows = buildList {
            add(
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.TaskCount,
                    value = QuickCreateProjectDetailRowValue.TaskCount(taskCount),
                )
            )
            add(
                QuickCreateProjectDetailRowUi(
                    label = QuickCreateProjectDetailRowLabel.PinStatus,
                    value = QuickCreateProjectDetailRowValue.PinStatus(pinned.toProjectPinStatusText()),
                )
            )
            createdAt?.let {
                add(
                    QuickCreateProjectDetailRowUi(
                        label = QuickCreateProjectDetailRowLabel.CreatedAt,
                        value = QuickCreateProjectDetailRowValue.Timestamp(formatProjectTime(it)),
                    )
                )
            }
            updatedAt?.let {
                add(
                    QuickCreateProjectDetailRowUi(
                        label = QuickCreateProjectDetailRowLabel.UpdatedAt,
                        value = QuickCreateProjectDetailRowValue.Timestamp(formatProjectTime(it)),
                    )
                )
            }
        },
    )

private fun Boolean.toProjectPinContentDescription(): QuickCreateProjectPinContentDescription =
    if (this) {
        QuickCreateProjectPinContentDescription.UnpinProject
    } else {
        QuickCreateProjectPinContentDescription.PinProject
    }

private fun Boolean.toProjectPinStatusText(): QuickCreateProjectPinStatusText =
    if (this) QuickCreateProjectPinStatusText.Pinned else QuickCreateProjectPinStatusText.NotPinned

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
