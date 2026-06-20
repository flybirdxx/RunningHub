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
