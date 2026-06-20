package com.runninghub.feature.quickcreate.presentation.result

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
 * 快捷创作任务状态的可渲染展示模型。
 *
 * @property text 状态区域展示的中文文案；优先使用调用方传入的服务端/本地进度说明。
 * @property indicator 状态区域的视觉指示类型，决定图标、颜色和进度样式。
 */
data class QuickCreateTaskStatusDisplay(
    val text: String,
    val indicator: QuickCreateTaskIndicator,
)

/**
 * 将任务阶段和可选进度文案映射为结果区域可渲染状态。
 *
 * 状态文案属于 Presentation 规则，不能由 Data 层生成。这里保留 `statusText` 优先级，
 * 是为了让轮询控制器传入更具体的服务端进度或失败原因；缺失时再使用稳定默认文案。
 *
 * @param status 当前任务 UI 阶段，来源于任务提交或轮询流程。
 * @param statusText 服务端状态流或本地提交流程生成的补充文案；`null` 表示使用默认文案。
 * @return 结果区域可直接消费的文案与视觉指示模型。
 */
fun quickCreateTaskStatusDisplay(
    status: QuickCreateTaskPresentationStatus,
    statusText: String?,
): QuickCreateTaskStatusDisplay =
    when (status) {
        QuickCreateTaskPresentationStatus.FAILED -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "生成失败",
            indicator = QuickCreateTaskIndicator.Error,
        )
        QuickCreateTaskPresentationStatus.SUCCESS -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "生成完成",
            indicator = QuickCreateTaskIndicator.Success,
        )
        QuickCreateTaskPresentationStatus.IDLE,
        QuickCreateTaskPresentationStatus.SUBMITTING,
        QuickCreateTaskPresentationStatus.QUEUING,
        QuickCreateTaskPresentationStatus.RUNNING -> QuickCreateTaskStatusDisplay(
            text = statusText ?: "处理中...",
            indicator = QuickCreateTaskIndicator.Progress,
        )
    }
