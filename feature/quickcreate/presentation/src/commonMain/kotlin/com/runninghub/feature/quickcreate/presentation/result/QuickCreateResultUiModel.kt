package com.runninghub.feature.quickcreate.presentation.result

/**
 * 快捷创作任务在页面状态中使用的阶段。
 *
 * 该状态由任务提交和轮询流程写入 UiState，Composable 只根据它渲染进度、成功或失败反馈。
 */
enum class QuickCreateTaskUiStatus {
    /** 当前没有活跃任务或结果。 */
    IDLE,

    /** 本地正在把生成请求提交给 Repository。 */
    SUBMITTING,

    /** 服务端已经接收任务，正在排队等待执行。 */
    QUEUING,

    /** 服务端正在执行生成任务。 */
    RUNNING,

    /** 生成任务已经成功完成。 */
    SUCCESS,

    /** 提交、轮询或服务端执行过程失败。 */
    FAILED,

    /** 服务端确认任务已取消。 */
    CANCELED,
}

/**
 * 快捷创作结果在 Presentation 层使用的媒体类型。
 *
 * 该类型把服务端输出的格式字符串和 URL 兼容判断收敛到状态映射阶段，Composable 只消费稳定枚举，
 * 不再直接理解 `.mp4`、`.webm` 等服务端或文件协议细节。
 */
enum class QuickCreateResultMediaType {
    /** 图片结果，页面使用图片组件按原始宽高自适应展示。 */
    IMAGE,

    /** 视频结果，页面使用视频缩略图组件并保持固定视频比例展示。 */
    VIDEO,
}

/**
 * 快捷创作生成结果的可渲染 UI 模型。
 *
 * 它只承载页面展示所需的稳定信息，不包含 Repository、网络响应对象或可变加载状态。
 *
 * @property url 结果媒体的远程访问地址，来自服务端任务输出。
 * 空字符串不应进入该模型；如果服务端缺失地址，应在映射阶段过滤或作为失败处理。
 * @property type 服务端返回的原始输出类型字符串，例如 `png`、`mp4`、`video`。
 * 该字段仅用于详情辅助展示和排错，不应由 Composable 再次解析业务含义。
 * @property mediaType 映射后的稳定媒体类型。
 * 图片结果使用 [QuickCreateResultMediaType.IMAGE]，视频结果使用 [QuickCreateResultMediaType.VIDEO]；
 * 当服务端类型未知时，映射层按 URL 后缀做兼容兜底，仍无法识别时降级为图片。
 * @property thumbnailUrl 服务端返回的视频或图片缩略图地址。
 * `null` 表示没有独立缩略图，展示层可直接使用 [url] 或默认占位。
 * @property width 服务端返回的媒体宽度，单位为像素。
 * `null` 表示服务端未提供尺寸；非空时必须为正数，当前 UI 不依赖该值做强制布局。
 * @property height 服务端返回的媒体高度，单位为像素。
 * `null` 表示服务端未提供尺寸；非空时必须为正数，并应与 [width] 同时出现。
 * @property duration 视频结果时长，单位为秒。
 * `null` 表示图片结果或服务端未提供时长；图片结果即使该值非空也不会按视频渲染。
 */
data class QuickCreateResultUi(
    val url: String,
    val type: String,
    val mediaType: QuickCreateResultMediaType,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

/**
 * 快捷创作中间对话区的一次生成条目。
 *
 * 对话区需要保留本次页面生命周期内已经提交过的所有任务；每个条目绑定点击发送时的提示词快照、
 * 任务阶段和该任务返回的结果列表。最新任务由生成流程追加到列表末尾，轮询控制器只更新最后一个条目，
 * 从而避免新任务覆盖旧任务。
 *
 * @property prompt 点击生成时保存的提示词快照。
 * 空字符串表示该条目由兼容旧状态构造或提交参数没有可展示提示词；UI 不应显示空提示词气泡。
 * @property taskStatus 该条目的生成阶段。
 * [QuickCreateTaskUiStatus.IDLE] 表示不需要展示任务卡；非空闲状态会展示对应占位、结果或错误状态。
 * @property taskId 服务端任务 ID；排队、运行和终态可用于展示任务入口，提交前或兼容旧状态时为 `null`。
 * @property aspectRatio 点击生成时保存的宽高比协议值，例如 `16:9`；`null` 表示旧状态缺少参数快照。
 * @property resolution 点击生成时保存的分辨率协议值，例如 `2K`；`null` 表示旧状态缺少参数快照。
 * @property statusText 该条目的辅助状态语义。
 * `null` 表示没有额外阶段说明；运行态通常携带进度，终态可为空以避免在结果图上重复贴文字。
 * @property results 该条目已经返回的输出结果。
 * 空列表表示仍在生成、失败/取消或服务端成功但暂未返回可展示输出。
 */
data class QuickCreateConversationItemUi(
    val prompt: String,
    val taskStatus: QuickCreateTaskUiStatus,
    val taskId: String? = null,
    val aspectRatio: String? = null,
    val resolution: String? = null,
    val statusText: QuickCreateTaskStatusText? = null,
    val results: List<QuickCreateResultUi> = emptyList(),
)

/**
 * 快捷创作结果卡可暴露给页面层的后续动作。
 *
 * 该枚举只表达动作语义，不执行导航、保存、下载或剪贴板副作用；具体能力由 composeApp 或平台层决定。
 * 这样可以让 queued / running / success / failed / canceled 的按钮集合在 Feature Presentation 中保持稳定，
 * 同时避免 Presentation 依赖平台 API。
 */
enum class QuickCreateResultAction {
    ViewTask,
    ViewResult,
    Save,
    Download,
    ReuseParameters,
    CopyPrompt,
    TryAgain,
    Retry,
    ViewDetail,
    RefundStatus,
}

/**
 * 结果卡动作的可渲染状态。
 *
 * @property action 稳定动作语义，由 composeApp 映射为本地化文案和具体回调。
 * @property enabled 当前动作是否可点击；不可点击时 UI 仍可展示该入口但不得执行副作用。
 */
data class QuickCreateResultActionUi(
    val action: QuickCreateResultAction,
    val enabled: Boolean = true,
)

/**
 * 根据任务阶段生成结果卡下一步动作集合。
 *
 * 动作集合在 Presentation 层集中维护，避免 Composable 通过中文文案或远端状态字符串临时判断按钮。
 * 保存、下载、查看任务等平台副作用仅作为语义输出；真正执行前必须由页面或平台层再次确认能力边界。
 */
fun quickCreateResultActions(
    taskStatus: QuickCreateTaskUiStatus,
    taskId: String?,
    prompt: String,
    results: List<QuickCreateResultUi>,
): List<QuickCreateResultActionUi> {
    val hasTaskId = !taskId.isNullOrBlank()
    val hasPrompt = prompt.isNotBlank()
    val hasResults = results.isNotEmpty()
    return when (taskStatus) {
        QuickCreateTaskUiStatus.IDLE,
        QuickCreateTaskUiStatus.SUBMITTING -> emptyList()
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING -> buildList {
            if (hasTaskId) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.ViewTask))
            }
        }
        QuickCreateTaskUiStatus.SUCCESS -> buildList {
            if (hasResults) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.ViewResult))
            }
            add(QuickCreateResultActionUi(QuickCreateResultAction.TryAgain))
            if (hasResults) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.Save))
                add(QuickCreateResultActionUi(QuickCreateResultAction.Download))
                add(QuickCreateResultActionUi(QuickCreateResultAction.ReuseParameters))
            }
            if (hasPrompt) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.CopyPrompt))
            }
        }
        QuickCreateTaskUiStatus.FAILED -> buildList {
            add(QuickCreateResultActionUi(QuickCreateResultAction.Retry))
            if (hasTaskId) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.ViewDetail))
            }
            add(QuickCreateResultActionUi(QuickCreateResultAction.RefundStatus))
        }
        QuickCreateTaskUiStatus.CANCELED -> buildList {
            if (hasTaskId) {
                add(QuickCreateResultActionUi(QuickCreateResultAction.ViewDetail))
            }
            add(QuickCreateResultActionUi(QuickCreateResultAction.TryAgain))
        }
    }
}
