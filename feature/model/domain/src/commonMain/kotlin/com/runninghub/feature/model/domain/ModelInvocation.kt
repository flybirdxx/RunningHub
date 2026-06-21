package com.runninghub.feature.model.domain

/**
 * 标准模型调用字段的领域值。
 *
 * 该 sealed interface 用平台无关类型表达用户输入，避免 Presentation 直接传递 JSON、
 * DTO 或具体 multipart 字段结构。Data 层会根据模型字段定义把这些值转换为 RunningHub
 * OpenAPI 请求体。
 */
sealed interface ModelFieldValue {
    /**
     * 文本输入值，适用于 prompt、标题和普通字符串参数。
     *
     * @property value 用户输入或模板回填后的文本内容；空字符串表示用户明确提交空文本。
     */
    data class Text(val value: String) : ModelFieldValue

    /**
     * 数值输入值，适用于浮点、整数或服务端允许数字表示的参数。
     *
     * @property value 已归一化为 Double 的数值；Data 层会根据字段定义决定是否转为整数。
     */
    data class NumberValue(val value: Double) : ModelFieldValue

    /**
     * 布尔输入值，适用于开关类模型参数。
     *
     * @property value `true` 表示开关开启，`false` 表示开关关闭。
     */
    data class BooleanValue(val value: Boolean) : ModelFieldValue

    /**
     * 字符串列表输入值，适用于多图、多文件或多选参数。
     *
     * @property values 按用户选择顺序保存的远端文件名、URL 或选项值列表；
     * 空集合表示用户未选择任何值，集合允许重复以保留用户原始选择。
     */
    data class StringList(val values: List<String>) : ModelFieldValue
}

/**
 * 标准模型调用请求。
 *
 * Domain 只保存业务提交所需的模型 ID、字段定义和用户填写值；具体 OpenAPI endpoint
 * 由 Data 层根据模型目录或详情接口解析，避免远端路由泄漏到 Presentation。
 *
 * @property modelId 标准模型 SKU 稳定标识，用于 Data 层解析实际 endpoint。
 * @property fields 当前模型详情中的字段定义，用于 Data 层按字段类型构造请求体，顺序按服务端保留。
 * @property values 用户填写或上传后的字段值，key 应使用字段的 paramKey 或 fieldKey；空 map 表示尚未填写。
 * @property webhookUrl 可选回调地址；`null` 表示不携带 webhook，空字符串应由调用方归一化为 `null`。
 */
data class ModelInvocationRequest(
    val modelId: String,
    val fields: List<ApiModelField>,
    val values: Map<String, ModelFieldValue>,
    val webhookUrl: String? = null,
)

/**
 * 标准模型远端任务状态。
 *
 * @property taskId 远端任务稳定标识，用于后续轮询；空字符串表示服务端未返回有效任务 ID。
 * @property status 服务端任务状态原始枚举值，Presentation 可映射为页面状态；`null` 表示响应缺少状态。
 * @property errorCode 服务端失败码；成功或未失败时为 `null`。
 * @property errorMessage 服务端失败原因；成功或未失败时为 `null`，Presentation 不得直接展示该原文。
 * @property resultUrls 已生成输出文件 URL 列表，任务未完成时为空集合；顺序按服务端返回保留。
 */
data class ModelInvocationTask(
    val taskId: String,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val resultUrls: List<String> = emptyList(),
)

/**
 * 标准模型调用仓库使用的稳定错误码。
 *
 * Data 层通过这些常量表达可机器识别的失败语义，不直接返回最终中文 UI 文案、
 * 服务端 `msg/message/errorMessage` 或底层异常 message。Presentation 层负责把这些错误码
 * 映射为当前页面的本地化提示和重试策略。
 */
object ModelInvocationIssueCode {
    /**
     * 用户尚未绑定可用于 OpenAPI 上传的 API Key。
     */
    const val API_KEY_MISSING = "MODEL_API_KEY_MISSING"

    /**
     * 媒体上传响应缺少可访问的远端 URL 或可拼接文件名。
     */
    const val MEDIA_UPLOAD_EMPTY_URL = "MODEL_MEDIA_UPLOAD_EMPTY_URL"
}
