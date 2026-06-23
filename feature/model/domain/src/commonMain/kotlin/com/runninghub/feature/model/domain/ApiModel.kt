package com.runninghub.feature.model.domain

/**
 * 标准模型列表项的领域摘要。
 *
 * @property id 标准模型 SKU 稳定标识，来源于服务端目录接口，可用于详情查询和任务提交前的 endpoint 解析。
 * @property name 用户可见模型名称，来源于服务端目录配置；空字符串表示服务端未返回可展示名称。
 * @property type 服务端返回的模型类型标识，用于筛选或分组展示；`null` 表示旧接口未提供类型。
 * @property groupName 模型所属分组名称；`null` 表示未分组或旧接口未返回该字段。
 * @property source 模型来源标识，保留给展示和筛选，不参与网络路由；`null` 表示来源未知。
 * @property priceSummary 价格摘要文案，来源于服务端目录配置；`null` 表示暂不可展示价格摘要。
 * @property requiredFields 必填字段名称摘要，顺序按服务端返回保留；空集合表示接口未提供摘要或模型无必填项。
 * @property optionalFields 可选字段名称摘要，顺序按服务端返回保留；空集合表示接口未提供摘要或模型无可选项。
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
 * @property id 标准模型 SKU 稳定标识，来源于详情接口；用于和列表项、任务提交请求关联。
 * @property name 用户可见模型名称；空字符串表示服务端未返回可展示名称。
 * @property type 服务端返回的模型类型标识；`null` 表示旧接口未提供类型。
 * @property groupName 模型所属分组名称；`null` 表示未分组或旧接口未返回该字段。
 * @property source 模型来源标识；`null` 表示来源未知。
 * @property priceSummary 价格摘要文案；`null` 表示暂不可展示价格摘要。
 * @property queueSize 当前排队量；`null` 表示服务端未返回，`0` 表示当前无排队任务。
 * @property concurrencyLimit 并发限制；`null` 表示服务端未返回，`0` 表示服务端声明不允许并发。
 * @property fields 已解析后的模型输入字段定义，顺序按服务端 inputConfig 保留；空集合表示无可渲染字段。
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
 * @property fieldKey 服务端字段标识，通常用于 UI 稳定 key；空字符串表示解析时未找到可用字段名。
 * @property paramKey 提交请求中的参数名；Data 层构造请求体时优先使用该值。
 * @property type 字段类型，决定 UI 控件和请求体值类型。
 * @property required 是否必填；`true` 表示提交前必须有有效值，`false` 表示可省略。
 * @property title 用户可见字段标题；`null` 表示使用字段 key 或默认文案兜底。
 * @property description 字段说明；`null` 表示服务端未提供说明。
 * @property placeholder 输入占位提示；`null` 表示不展示占位提示。
 * @property defaultValue 服务端配置的默认值；`null` 表示没有默认值，空字符串表示默认值就是空。
 * @property options 列表类字段的可选项，顺序按服务端返回保留；空集合表示没有选项或字段不是列表。
 * @property minLength 文本最小长度；`null` 表示不限制，`0` 表示允许空文本。
 * @property maxLength 文本最大长度；`null` 表示不限制。
 * @property min 数值最小值；`null` 表示不限制，`0.0` 是有效边界值。
 * @property max 数值最大值；`null` 表示不限制。
 * @property step 数值步进；`null` 表示由 UI 默认处理。
 * @property precision 数值精度；`null` 表示不强制，`0` 表示整数精度。
 * @property multipleInputs 是否允许多输入；`true` 表示字段可收集多个值，`false` 表示单值。
 * @property maxInputCount 多输入最大数量；`null` 表示不限制，`0` 表示服务端声明不允许输入。
 * @property maxUploadCount 上传字段最大文件数量；`null` 表示不限制，`0` 表示不可上传。
 * @property maxUploadSizeBytes 上传字段最大文件大小，单位字节；`null` 表示不限制。
 * @property acceptFormats 可接受文件扩展名或 MIME 片段；空集合表示不限制或接口未返回。
 * @property visible 字段是否可见；`true` 表示可渲染给用户，`false` 表示隐藏字段不应展示。
 * @property rawConfigJson 服务端字段原始 JSON。
 * `null` 表示没有可保留的原始字段配置；该值仅供后续动态 UI 增量解析，不应直接展示给用户。
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
    val rawConfigJson: String? = null,
)

/**
 * 标准模型字段选项。
 *
 * @property label 用户可见选项名；空字符串表示服务端未提供展示名。
 * @property value 提交到服务端的选项值；必须按原值保留，不能在 Presentation 层翻译。
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
 * @property modelKey LLM 模型稳定键，来源于服务端模型目录，可用于后续发起 LLM 能力调用。
 * @property provider 模型提供方；空字符串表示服务端未返回提供方。
 * @property version 模型版本；`null` 表示服务端未提供版本。
 * @property contextLength 上下文长度，单位 token；`null` 表示未知，`0` 表示服务端声明无上下文能力。
 * @property capabilities 服务端声明的能力标签，顺序按服务端返回保留；空集合表示未知或无能力标签。
 * @property inputPrice 输入计费摘要；`null` 表示暂不可展示输入价格。
 * @property outputPrice 输出计费摘要；`null` 表示暂不可展示输出价格。
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
