package com.runninghub.feature.audio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * MiniMax 文本转音频提交请求 DTO。
 *
 * 该结构只存在于 Audio Data 层，用于匹配 RunningHub OpenAPI v2 的文本转音频协议；
 * Domain 层通过 [com.runninghub.feature.audio.domain.AudioRequest] 表达业务参数。
 *
 * @property text 待合成的文本内容，来源于用户输入或上游模板。
 * 空字符串表示调用方未完成校验，不应提交到远端。
 * @property voiceId 服务端音色 ID，来源于产品配置或用户选择。
 * 空字符串表示没有可用音色，不应提交到远端。
 * @property pronunciationDict 发音词典，元素顺序按服务端要求保留；`null` 表示不覆盖默认发音。
 * 空集合表示显式不提供任何自定义发音。
 * @property speed 语速倍率，服务端按浮点数解释；`null` 表示使用服务端默认值。
 * 当前默认值 `1.0f` 表示正常语速。
 * @property volume 音量倍率，服务端按浮点数解释；`null` 表示使用服务端默认值。
 * 当前默认值 `1.0f` 表示正常音量。
 * @property pitch 音高偏移，服务端按整数解释；`null` 表示使用服务端默认值。
 * 当前默认值 `0` 表示不调整音高。
 * @property enableBase64Output 是否要求服务端返回 base64 音频内容。
 * `true` 表示响应可能包含内联音频；`false` 表示优先返回远端 URL。
 * @property englishNormalization 是否启用英文文本归一化。
 * `true` 表示服务端会尝试规范化英文读法；`false` 表示按原文本提交。
 * @property emotion 情绪标签，来源于用户选择或模板配置。
 * `null` 表示不指定情绪，空字符串不应主动提交。
 */
@Serializable
data class MiniMaxAudioRequestDto(
    @SerialName("text") val text: String,
    @SerialName("voice_id") val voiceId: String,
    @SerialName("pronunciation_dict") val pronunciationDict: List<String>? = null,
    @SerialName("speed") val speed: Float? = 1.0f,
    @SerialName("volume") val volume: Float? = 1.0f,
    @SerialName("pitch") val pitch: Int? = 0,
    @SerialName("enable_base64_output") val enableBase64Output: Boolean? = false,
    @SerialName("english_normalization") val englishNormalization: Boolean? = false,
    @SerialName("emotion") val emotion: String? = null
)

/**
 * MiniMax 文本转音频提交响应 DTO。
 *
 * @property taskId 远端任务 ID，提交成功后用于轮询任务状态。
 * 空字符串表示服务端没有返回可追踪任务，Repository 应按失败处理。
 * @property status 远端任务提交状态，常见值包括 `SUBMITTED`、`FAILED`。
 * 空字符串表示服务端状态缺失，调用方应继续按兼容路径处理。
 * @property errorCode 服务端错误码。
 * `null` 表示提交阶段没有错误码，区别于返回空字符串的异常兼容值。
 * @property errorMessage 服务端错误摘要，仅供 Data 层映射，不应直接作为最终 UI 文案。
 * `null` 表示提交阶段没有错误摘要。
 * @property results 服务端可能同步返回的音频结果。
 * `null` 表示提交响应不包含结果；空集合表示明确没有结果。
 * @property clientId 服务端回传的客户端追踪 ID。
 * `null` 表示当前响应没有追踪 ID，不应作为任务 ID 使用。
 * @property promptTips 服务端返回的提示词建议或警告。
 * `null` 表示没有提示信息，Data 层不生成最终展示文案。
 */
@Serializable
data class MiniMaxAudioResponseDto(
    @SerialName("taskId") val taskId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<AudioResultDto>? = null,
    @SerialName("clientId") val clientId: String? = null,
    @SerialName("promptTips") val promptTips: String? = null
)

/**
 * 音频任务查询响应 DTO。
 *
 * @property taskId 远端任务 ID，来源于提交响应或查询参数。
 * 空字符串表示服务端响应缺失任务标识。
 * @property status 远端任务状态，常见值包括 `RUNNING`、`SUCCESS`、`FAILED`。
 * 空字符串表示服务端未返回状态，Repository 会继续按兼容逻辑处理。
 * @property errorCode 任务失败时的服务端错误码。
 * `null` 表示当前状态没有错误码。
 * @property errorMessage 任务失败时的服务端错误摘要。
 * `null` 表示当前状态没有错误摘要；该值不应直接成为最终 UI 文案。
 * @property results 任务成功后的输出结果列表，顺序为服务端输出顺序。
 * `null` 表示服务端尚未返回结果字段；空集合表示已完成但没有输出。
 * @property usage 任务消耗信息。
 * `null` 表示当前接口未返回消耗明细，区别于各字段为零的有效用量。
 */
@Serializable
data class TaskQueryResultDto(
    @SerialName("taskId") val taskId: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<AudioResultDto>? = null,
    @SerialName("usage") val usage: TaskUsageDto? = null
)

/**
 * 音频任务输出结果 DTO。
 *
 * @property url 远端音频文件 URL，通常用于播放或下载。
 * `null` 表示该结果没有可访问 URL。
 * @property outputType 输出类型，例如音频格式或服务端分类。
 * `null` 表示服务端没有返回类型。
 * @property text 与输出音频关联的文本片段。
 * `null` 表示服务端没有回传文本。
 */
@Serializable
data class AudioResultDto(
    @SerialName("url") val url: String? = null,
    @SerialName("outputType") val outputType: String? = null,
    @SerialName("text") val text: String? = null
)

/**
 * 音频任务资源消耗 DTO。
 *
 * @property consumeMoney 服务端返回的扣费金额字符串，币种和精度以服务端为准。
 * `null` 表示未返回金额信息。
 * @property consumeCoins 服务端返回的积分消耗字符串。
 * `null` 表示未返回积分消耗。
 * @property taskCostTime 服务端返回的任务耗时字符串，单位由服务端协议决定。
 * `null` 表示未返回耗时。
 * @property thirdPartyConsumeMoney 第三方模型侧消耗金额字符串。
 * `null` 表示未返回第三方消耗。
 */
@Serializable
data class TaskUsageDto(
    @SerialName("consumeMoney") val consumeMoney: String? = null,
    @SerialName("consumeCoins") val consumeCoins: String? = null,
    @SerialName("taskCostTime") val taskCostTime: String? = null,
    @SerialName("thirdPartyConsumeMoney") val thirdPartyConsumeMoney: String? = null
)

/**
 * 音频任务查询请求 DTO。
 *
 * @property taskId 远端任务 ID，来源于提交响应或历史记录。
 * 空字符串表示调用方未完成校验，不应提交到远端。
 */
@Serializable
data class TaskQueryRequestDto(
    @SerialName("taskId") val taskId: String
)
