package com.runninghub.feature.audio.domain

/**
 * 文本转音频任务在领域层暴露的状态。
 *
 * Audio 功能的领域契约已经从 `shared` 迁出；本类型只表达业务状态，
 * 不携带 DTO、HTTP 状态码或平台对象。`feature:audio:data` 负责把远端响应映射为这些状态，
 * Presentation 层负责把 [Error.message] 转换为最终用户可见文案。
 */
sealed class AudioTaskStatus {
    /**
     * 任务提交请求已经开始。
     *
     * 该状态由仓库在发起远端请求前立即发出，用于让页面禁用重复提交入口。
     */
    data object Submitting : AudioTaskStatus()

    /**
     * 远端任务已创建并处于运行或排队阶段。
     *
     * @property taskId 远端音频任务 ID，由服务端返回；非空字符串才可用于后续查询，
     * 当前对象不负责持久化该 ID。
     */
    data class Running(
        val taskId: String,
    ) : AudioTaskStatus()

    /**
     * 音频任务已经成功完成。
     *
     * @property url 服务端返回的音频文件地址，通常为远程 URL；调用方展示或播放前
     * 仍需按平台能力处理网络访问权限和缓存策略。
     */
    data class Success(
        val url: String,
    ) : AudioTaskStatus()

    /**
     * 音频任务提交、轮询或结果解析失败。
     *
     * @property message 稳定的领域错误摘要，供 Presentation 映射用户文案。
     * 该字段不得包含 Token、Cookie、API Key、密码、完整请求头或本地文件路径。
     */
    data class Error(
        val message: String,
    ) : AudioTaskStatus()
}

/**
 * 文本转音频的提交参数。
 *
 * @property text 用户输入或上游流程生成的待合成文本。
 * 空字符串没有业务意义，调用方应在提交前阻止；本对象不保存敏感凭据。
 * @property voiceId 服务端音色 ID，来自模型/音色目录或默认配置。
 * 该 ID 需要稳定可提交，空字符串表示调用方尚未选择有效音色。
 * @property speed 语速倍率，默认 `1.0` 表示服务端默认语速。
 * 有效范围由远端模型决定，调用方应在 UI 或 UseCase 层限制异常值。
 * @property volume 音量倍率，默认 `1.0` 表示服务端默认音量。
 * 该值不是分贝单位，具体范围以远端音频服务为准。
 * @property pitch 音调偏移，默认 `0` 表示不调整。
 * 正负值的具体含义由远端音频服务解释，调用方应避免提交超出服务端范围的值。
 * @property emotion 可选情绪参数，来自服务端支持的枚举字符串。
 * `null` 表示不指定情绪，与空字符串不同；空字符串应在提交前归一化为 `null`。
 */
data class AudioRequest(
    val text: String,
    val voiceId: String,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Int = 0,
    val emotion: String? = null,
)

/**
 * 单个音频生成结果。
 *
 * @property url 远端音频文件 URL。
 * `null` 表示服务端当前响应中没有可播放文件，调用方不能假设任务成功。
 * @property outputType 服务端返回的输出类型或格式，例如音频编码或文件类型。
 * `null` 表示旧接口未返回该字段，不应影响 URL 的基本展示。
 * @property text 与该音频结果对应的文本片段。
 * `null` 表示服务端未回传文本，不能用于恢复用户输入。
 */
data class AudioResult(
    val url: String?,
    val outputType: String?,
    val text: String?,
)

/**
 * 查询音频任务得到的完整领域结果。
 *
 * @property taskId 远端任务 ID，来源于提交响应或查询接口。
 * 该值用于关联轮询状态，不应作为本地数据库主键以外的长期业务事实。
 * @property status 服务端任务状态字符串，例如 `SUCCESS`、`FAILED` 或运行中状态。
 * 迁移期保留原始字符串以兼容旧接口，后续可收敛为枚举或 sealed 类型。
 * @property errorCode 服务端错误码。
 * `null` 表示任务未失败或旧接口未提供错误码；它不等同于成功状态。
 * @property errorMessage 服务端错误摘要。
 * `null` 表示任务未失败或旧接口未提供错误说明；Presentation 不得直接展示该原文。
 * @property results 服务端返回的音频结果列表。
 * `null` 表示接口未返回结果字段，空集合表示明确没有结果；顺序由服务端决定。
 */
data class AudioTaskResult(
    val taskId: String,
    val status: String,
    val errorCode: String?,
    val errorMessage: String?,
    val results: List<AudioResult>?,
)
