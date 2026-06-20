package com.runninghub.shared.domain.model

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
     * @property value 用户输入或模板回填后的文本内容。
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
     * @property value 开关当前值。
     */
    data class BooleanValue(val value: Boolean) : ModelFieldValue

    /**
     * 字符串列表输入值，适用于多图、多文件或多选参数。
     *
     * @property values 按用户选择顺序保存的远端文件名、URL 或选项值列表。
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
 * @property fields 当前模型详情中的字段定义，用于 Data 层按字段类型构造请求体。
 * @property values 用户填写或上传后的字段值，key 应使用字段的 paramKey 或 fieldKey。
 * @property webhookUrl 可选回调地址；为空时提交请求不携带 webhook。
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
 * @property taskId 远端任务稳定标识，用于后续轮询。
 * @property status 服务端任务状态原始枚举值，Presentation 可映射为页面状态。
 * @property errorCode 服务端失败码；成功或未失败时为空。
 * @property errorMessage 服务端失败原因；成功或未失败时为空。
 * @property resultUrls 已生成输出文件 URL 列表，任务未完成时为空。
 */
data class ModelInvocationTask(
    val taskId: String,
    val status: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val resultUrls: List<String> = emptyList(),
)
