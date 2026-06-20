package com.runninghub.shared.domain.model

/**
 * 标准模型列表项的领域摘要。
 *
 * 该模型只承载目录展示和选择所需的信息。远端 `rhEndpoint` 属于 Data 层路由细节，
 * 会由目录仓库登记到内部缓存，不暴露给 Presentation。
 *
 * @property id 标准模型 SKU 稳定标识。
 * @property name 用户可见模型名称。
 * @property type 服务端返回的模型类型标识，用于筛选或分组展示。
 * @property groupName 模型所属分组名称。
 * @property source 模型来源标识，保留给展示和筛选，不参与网络路由。
 * @property priceSummary 价格摘要文案，来源于服务端目录配置。
 * @property requiredFields 必填字段名称摘要；当前目录接口可能为空。
 * @property optionalFields 可选字段名称摘要；当前目录接口可能为空。
 */
data class ApiModelSummary(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val priceSummary: String? = null,
    val requiredFields: List<String> = emptyList(),
    val optionalFields: List<String> = emptyList(),
)

/**
 * 标准模型详情的领域模型。
 *
 * 详情用于驱动模型调用表单和基础能力展示，不携带原始 inputConfig JSON 或远端 endpoint。
 * JSON 解析和 endpoint 缓存都由 Data 层完成，Presentation 只依赖结构化字段定义。
 *
 * @property id 标准模型 SKU 稳定标识。
 * @property name 用户可见模型名称。
 * @property type 服务端返回的模型类型标识。
 * @property groupName 模型所属分组名称。
 * @property source 模型来源标识。
 * @property priceSummary 价格摘要文案。
 * @property queueSize 当前排队量；服务端未返回时为空。
 * @property concurrencyLimit 并发限制；服务端未返回时为空。
 * @property fields 已解析后的模型输入字段定义。
 */
data class ApiModelDetail(
    val id: String,
    val name: String,
    val type: String? = null,
    val groupName: String? = null,
    val source: String? = null,
    val priceSummary: String? = null,
    val queueSize: Int? = null,
    val concurrencyLimit: Int? = null,
    val fields: List<ApiModelField> = emptyList(),
)

/**
 * 标准模型输入字段定义。
 *
 * 字段定义由 Data 层从服务端 inputConfig 解析得到，用于 Presentation 渲染表单和 Data 层构造请求体。
 *
 * @property fieldKey 服务端字段标识，通常用于 UI 稳定 key。
 * @property paramKey 提交请求中的参数名。
 * @property type 字段类型，决定 UI 控件和请求体值类型。
 * @property required 是否必填。
 * @property title 用户可见字段标题。
 * @property description 字段说明。
 * @property placeholder 输入占位提示。
 * @property defaultValue 服务端配置的默认值。
 * @property options 列表类字段的可选项。
 * @property minLength 文本最小长度；为空表示不限制。
 * @property maxLength 文本最大长度；为空表示不限制。
 * @property min 数值最小值；为空表示不限制。
 * @property max 数值最大值；为空表示不限制。
 * @property step 数值步进；为空表示由 UI 默认处理。
 * @property precision 数值精度；为空表示不强制。
 * @property multipleInputs 是否允许多输入。
 * @property maxInputCount 多输入最大数量；为空表示不限制。
 * @property maxUploadCount 上传字段最大文件数量；为空表示不限制。
 * @property maxUploadSizeBytes 上传字段最大文件大小，单位字节；为空表示不限制。
 * @property acceptFormats 可接受文件扩展名或 MIME 片段。
 * @property visible 字段是否可见；隐藏字段不应渲染给用户。
 */
data class ApiModelField(
    val fieldKey: String,
    val paramKey: String,
    val type: ApiModelFieldType,
    val required: Boolean,
    val title: String? = null,
    val description: String? = null,
    val placeholder: String? = null,
    val defaultValue: String? = null,
    val options: List<ApiModelFieldOption> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val precision: Int? = null,
    val multipleInputs: Boolean = false,
    val maxInputCount: Int? = null,
    val maxUploadCount: Int? = null,
    val maxUploadSizeBytes: Long? = null,
    val acceptFormats: List<String> = emptyList(),
    val visible: Boolean = true,
)

/**
 * 标准模型字段选项。
 *
 * @property label 用户可见选项名。
 * @property value 提交到服务端的选项值。
 */
data class ApiModelFieldOption(
    val label: String,
    val value: String,
)

/**
 * 标准模型字段类型。
 *
 * 该枚举是领域层对服务端字段类型的归一化结果，Presentation 根据它选择控件，
 * Data 层根据它决定提交值的 JSON 类型。
 */
enum class ApiModelFieldType {
    STRING,
    NUMBER,
    INTEGER,
    BOOLEAN,
    LIST,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    MODEL,
    UNKNOWN,
}

/**
 * LLM 模型目录摘要。
 *
 * @property modelKey LLM 模型稳定键。
 * @property provider 模型提供方。
 * @property version 模型版本。
 * @property contextLength 上下文长度，单位 token；未知时为空。
 * @property capabilities 服务端声明的能力标签。
 * @property inputPrice 输入计费摘要。
 * @property outputPrice 输出计费摘要。
 */
data class LlmModelSummary(
    val modelKey: String,
    val provider: String,
    val version: String? = null,
    val contextLength: Int? = null,
    val capabilities: List<String> = emptyList(),
    val inputPrice: String? = null,
    val outputPrice: String? = null,
)
