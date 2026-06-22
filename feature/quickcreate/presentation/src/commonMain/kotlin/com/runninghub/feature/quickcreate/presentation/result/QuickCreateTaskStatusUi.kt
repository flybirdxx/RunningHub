package com.runninghub.feature.quickcreate.presentation.result

import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError

/**
 * 快捷创作任务在 Presentation 模块中的稳定状态。
 *
 * 该枚举只表达 UI 渲染需要的任务阶段，不包含远端状态原始字符串、任务 ID 或轮询细节。
 * `composeApp` 迁移期可以从旧 UiState 状态做薄映射；后续当 UiState 下沉到本模块后，
 * 该状态可直接成为结果区域的事实来源。
 */
enum class QuickCreateTaskPresentationStatus {
    /** 当前没有展示中的任务，结果区域按进度占位处理。 */
    IDLE,

    /** 本地已开始提交任务，但服务端尚未确认入队。 */
    SUBMITTING,

    /** 服务端已接收任务，正在等待执行资源。 */
    QUEUING,

    /** 服务端正在执行生成流程。 */
    RUNNING,

    /** 任务已经成功结束，结果区域可以展示成功状态或输出内容。 */
    SUCCESS,

    /** 任务提交、轮询或服务端执行失败。 */
    FAILED,

    /** 任务已被用户或服务端取消，属于已停止轮询的终态。 */
    CANCELED,
}

/**
 * 快捷创作任务状态对应的视觉指示类型。
 *
 * Result Composable 根据该值选择进度圈、成功图标或错误图标；文案生成和图标选择拆开，
 * 便于后续在不改变业务状态映射的情况下替换视觉实现。
 */
enum class QuickCreateTaskIndicator {
    /** 任务尚未终止，结果区域展示进度反馈。 */
    Progress,

    /** 任务已成功完成，结果区域展示成功反馈。 */
    Success,

    /** 任务失败或被错误状态终止，结果区域展示失败反馈。 */
    Error,
}

/**
 * 快捷创作任务状态区域的稳定文案来源。
 *
 * 该模型只描述状态区域应该使用哪类文案，不保存默认中文文案。固定状态和错误语义由 composeApp
 * 映射到 Compose Resources；服务端返回的未知摘要不得通过该类型原样透传到 UI。
 */
sealed interface QuickCreateTaskStatusText {
    /**
     * 使用稳定错误语义展示失败说明。
     *
     * @property error 任务失败或查询异常对应的 Presentation 错误语义，由 composeApp 映射为资源文案。
     */
    data class Error(
        val error: QuickCreatePresentationError,
    ) : QuickCreateTaskStatusText

    /** 提交请求已通过本地校验、正在等待远端任务创建的文案键，由 composeApp 映射为本地化资源。 */
    data object SubmittingTask : QuickCreateTaskStatusText

    /** 服务端已接收任务但尚未开始执行的文案键，由 composeApp 映射为本地化资源。 */
    data object Queuing : QuickCreateTaskStatusText

    /**
     * 服务端正在执行生成流程的进度文案。
     *
     * @property progressPercent 当前远端任务进度百分比，单位为 `%`；通常位于 `0..100`，
     * 异常服务端值会原样保留，便于排查远端状态协议问题。
     */
    data class Running(
        val progressPercent: Int,
    ) : QuickCreateTaskStatusText

    /**
     * 生成前正在等待素材上传完成的文案。
     *
     * @property pendingCount 当前提交快照仍需等待的素材数量，单位为个；正常路径应大于 `0`，
     * 若调用方传入 `0` 或负数，资源映射仍原样展示以暴露上游计数异常。
     */
    data class UploadingMedia(
        val pendingCount: Int,
    ) : QuickCreateTaskStatusText

    /** 缺省失败文案，由 composeApp 映射为本地化资源。 */
    data object Failed : QuickCreateTaskStatusText

    /** 缺省成功文案，由 composeApp 映射为本地化资源。 */
    data object Success : QuickCreateTaskStatusText

    /** 缺省取消文案，由 composeApp 映射为本地化资源。 */
    data object Canceled : QuickCreateTaskStatusText

    /** 缺省处理中占位文案，由 composeApp 映射为本地化资源。 */
    data object Processing : QuickCreateTaskStatusText
}

/**
 * 快捷创作任务状态的可渲染展示模型。
 *
 * @property text 状态区域展示的稳定文案来源；默认状态和错误文案由 composeApp 映射资源，
 * 远端未知状态摘要必须先映射为稳定语义，不允许原样透传到 UI。
 * @property indicator 状态区域的视觉指示类型，决定图标、颜色和进度样式。
 */
data class QuickCreateTaskStatusDisplay(
    val text: QuickCreateTaskStatusText,
    val indicator: QuickCreateTaskIndicator,
)

/**
 * 将任务阶段和可选进度文案映射为结果区域可渲染状态。
 *
 * 状态文案属于 Presentation 规则，不能由 Data 层生成。这里保留 `statusText` 优先级，
 * 是为了让轮询控制器传入更具体的服务端进度或失败原因；缺失时再使用稳定默认文案。
 *
 * @param status 当前任务 UI 阶段，来源于任务提交或轮询流程。
 * @param statusText 服务端状态流或本地提交流程生成的补充文案语义；`null` 表示使用默认文案。
 * @return 结果区域可直接消费的文案与视觉指示模型。
 */
fun quickCreateTaskStatusDisplay(
    status: QuickCreateTaskPresentationStatus,
    statusText: QuickCreateTaskStatusText?,
): QuickCreateTaskStatusDisplay =
    when (status) {
        QuickCreateTaskPresentationStatus.FAILED -> QuickCreateTaskStatusDisplay(
            text = statusText ?: QuickCreateTaskStatusText.Failed,
            indicator = QuickCreateTaskIndicator.Error,
        )
        QuickCreateTaskPresentationStatus.SUCCESS -> QuickCreateTaskStatusDisplay(
            text = statusText ?: QuickCreateTaskStatusText.Success,
            indicator = QuickCreateTaskIndicator.Success,
        )
        QuickCreateTaskPresentationStatus.CANCELED -> QuickCreateTaskStatusDisplay(
            text = statusText ?: QuickCreateTaskStatusText.Canceled,
            indicator = QuickCreateTaskIndicator.Error,
        )
        QuickCreateTaskPresentationStatus.IDLE,
        QuickCreateTaskPresentationStatus.SUBMITTING,
        QuickCreateTaskPresentationStatus.QUEUING,
        QuickCreateTaskPresentationStatus.RUNNING -> QuickCreateTaskStatusDisplay(
            text = statusText ?: QuickCreateTaskStatusText.Processing,
            indicator = QuickCreateTaskIndicator.Progress,
        )
    }
