package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作服务模型的业务类别。
 *
 * 该枚举是 Presentation 与 Domain 之间的查询语义，避免 UI 直接传递远端分类 ID。
 * 具体接口参数由 Data 层映射，后续服务端字段变化时不需要改动页面调用点。
 */
enum class QuickCreationServiceKind {
    /** 图片快捷创作服务模型。 */
    IMAGE,

    /** 视频快捷创作服务模型。 */
    VIDEO,
}

/**
 * 快捷创作可选服务模型。
 *
 * 该类型位于 quickcreate Domain 层，用于表达用户可选择的图片或视频生成服务。
 * 它只保存 Presentation 和业务规则需要的稳定字段；远端 DTO、接口路径、原始 JSON 和认证信息
 * 均由 quickcreate Data 实现处理。
 *
 * @property categoryId 服务所属的业务类别 ID，来源于服务端目录或 Data 层兜底分类。
 * 空字符串不应进入 Domain；该值可用于后续请求映射，但不直接作为 UI 文案。
 * @property groupName 服务端分组名称，通常来自目录父节点。
 * `null` 表示服务端没有分组；Presentation 可以选择隐藏分组副标题。
 * @property bindingId 服务模型的绑定 ID，来源于服务端，通常用于提交和计费请求。
 * 该值应稳定可持久化；空字符串表示 Data 映射失败，不应构造本模型。
 * @property skuId 服务模型的 SKU ID，来源于服务端计费配置。
 * 该值用于计费和提交请求；空字符串表示 Data 映射失败，不应构造本模型。
 * @property name 展示给用户的模型名称。
 * Data 层会按中文名、通用名、AI 名和 SKU 顺序兜底；空字符串不应进入 Domain。
 * @property description 服务说明文本。
 * `null` 表示服务端未提供说明；空字符串应在 Data 层归一化为 `null`。
 * @property fields 服务模型声明的动态输入字段。
 * 顺序保留服务端返回顺序；空列表表示该服务没有动态字段，只使用基础提示词和固定参数。
 * @property pricing 服务模型的计费摘要。
 * `null` 表示服务端未返回可解析计费信息；调用方不得据此假设免费，应继续走计费预览。
 */
data class QuickCreationServiceModel(
    val categoryId: String,
    val groupName: String?,
    val bindingId: String,
    val skuId: String,
    val name: String,
    val description: String?,
    val fields: List<QuickCreationServiceField>,
    val pricing: QuickCreationServicePricing? = null,
)

/**
 * 快捷创作服务模型的计费摘要。
 *
 * 该类型只保存目录接口可展示的计费提示，真实扣费仍以计费预览结果为准。
 *
 * @property pricingMode 服务端计价模式原始标记。
 * `null` 表示服务端未返回该字段；Presentation 不应把它直接作为用户文案。
 * @property settlementMode 服务端结算模式原始标记。
 * `null` 表示未声明；调用方只能用于调试或后续规则扩展。
 * @property paidPriceKind 付费价格类型原始标记。
 * `null` 表示未声明；该值不代表最终扣费金额。
 * @property flatPriceRaw 固定价格原始文本。
 * `null` 表示没有固定价格；该字段可能包含服务端格式化后的金额或点数。
 * @property dimensionPricingRaw 维度计价原始文本。
 * `null` 表示没有维度计价信息；调用方不在客户端解析复杂计价公式。
 * @property discountPercent 折扣百分比，单位为百分比点。
 * `null` 表示无折扣信息；有效值通常在 0 到 100 之间。
 * @property isFree 目录层是否标记为免费。
 * `true` 表示服务端目录提示免费；`false` 表示未标记免费，最终仍以计费预览为准。
 * @property freeRemaining 免费剩余次数，单位为次。
 * `0` 表示无剩余次数或服务端未提供；不允许负数进入 Domain。
 * @property isTimeFree 是否处于限时免费活动。
 * `true` 表示目录提示限免；`false` 表示没有限免提示。
 * @property promoType 促销类型原始标记。
 * `null` 表示无促销；该值不直接作为用户文案。
 */
data class QuickCreationServicePricing(
    val pricingMode: String? = null,
    val settlementMode: String? = null,
    val paidPriceKind: String? = null,
    val flatPriceRaw: String? = null,
    val dimensionPricingRaw: String? = null,
    val discountPercent: Int? = null,
    val isFree: Boolean = false,
    val freeRemaining: Int = 0,
    val isTimeFree: Boolean = false,
    val promoType: String? = null,
)

/**
 * 快捷创作服务模型的输入字段声明。
 *
 * 该模型位于 Domain 层，描述字段身份、默认值、可见性、上传约束和已解析的附加元数据。
 * Data 层负责把远端 `skuInputExtraJson` 等协议字段解析为 [inputExtra] 和 [uploadMediaKind]，
 * 因此本模型不保存原始 JSON，避免 Presentation 或 Domain 规则继续依赖序列化细节。
 *
 * @property fieldKey 服务字段的原始字段 key，用于兼容模板和旧接口返回。
 * 空字符串不应进入 Domain；模板别名解析会同时兼容该值和 [paramKey]。
 * @property paramKey 生成请求使用的标准参数 key。
 * 空字符串不应进入 Domain；计费、生成和上传字段绑定均使用该值。
 * @property fieldType 服务端字段类型的兼容标记。
 * 该值可能包含历史接口类型名，Domain 规则会按关键字归类为选项、文本或上传。
 * @property required 字段是否必填。
 * `true` 表示缺少有效值会阻止计费或提交；`false` 表示字段可省略。
 * @property defaultValue 默认值。
 * `null` 表示服务端未声明默认值；空字符串表示服务端明确返回空值。
 * @property options 可选值列表，非选项字段为空列表。
 * 顺序保留服务端返回顺序，Presentation 不应重新排序。
 * @property maxUploadCount 上传字段最大文件数，单位为个。
 * `null` 表示未声明；非空值应为非负数。
 * @property maxUploadSize 单文件最大字节数。
 * `null` 表示未声明；非空值以字节为单位。
 * @property multipleInputs 是否允许同一字段接收多个输入。
 * `true` 表示可提交多值；`false` 表示通常只接受单个值或文件。
 * @property uploadMediaKind Data 层推断出的上传媒体类型。
 * `null` 表示无法从服务端声明确定，调用方需要根据创作类型兜底。
 * @property inputExtra 已解析的附加字段配置。
 * `null` 表示服务端没有可解析附加配置。
 * @property visible 字段是否对用户可见。
 * `true` 表示可参与 UI 展示；`false` 表示隐藏字段，仅可能携带默认值参与请求。
 */
data class QuickCreationServiceField(
    val fieldKey: String,
    val paramKey: String,
    val fieldType: String,
    val required: Boolean,
    val defaultValue: String?,
    val options: List<QuickCreationServiceFieldOption>,
    val maxUploadCount: Int? = null,
    val maxUploadSize: Long? = null,
    val multipleInputs: Boolean = false,
    val uploadMediaKind: QuickCreationUploadMediaKind? = null,
    val inputExtra: QuickCreationServiceFieldExtra? = null,
    val visible: Boolean = true,
)

/**
 * 快捷创作服务字段的扩展配置。
 *
 * @property title 字段中文标题；`null` 表示服务端未提供，调用方应使用字段 key 兜底。
 * @property titleEn 字段英文标题；`null` 表示没有英文标题。
 * @property paramDescription 字段中文说明；`null` 表示没有说明。
 * @property paramDescriptionEn 字段英文说明；`null` 表示没有英文说明。
 * @property placeholder 文本输入占位内容；`null` 表示没有占位提示。
 * @property acceptFormats 上传字段接受的格式列表。
 * 空列表表示服务端未声明格式限制；顺序保留服务端返回顺序。
 * @property maxLength 文本最大长度，单位为字符。
 * `null` 表示不限制；非空值应为非负数。
 * @property minLength 文本最小长度，单位为字符。
 * `null` 表示不限制；非空值应为非负数。
 * @property maxInputCount 子输入或上传最大数量，单位为个。
 * `null` 表示不限制；非空值应为非负数。
 * @property ignoreListValueCaseSensitive 服务端列表值是否忽略大小写。
 * `true` 表示匹配时可忽略大小写；`false` 表示按服务端默认精确匹配。
 * @property inputChildren 动态子输入声明。
 * 空列表表示没有条件子字段；顺序保留服务端配置顺序。
 */
data class QuickCreationServiceFieldExtra(
    val title: String? = null,
    val titleEn: String? = null,
    val paramDescription: String? = null,
    val paramDescriptionEn: String? = null,
    val placeholder: String? = null,
    val acceptFormats: List<String> = emptyList(),
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val maxInputCount: Int? = null,
    val ignoreListValueCaseSensitive: Boolean = false,
    val inputChildren: List<QuickCreationServiceFieldInputChild> = emptyList(),
)

/**
 * 服务字段的动态子输入声明。
 *
 * 子输入通常来自父字段的附加配置，并会根据 [visibleWhen] 在特定选项下激活。
 * Data 层同样负责把远端子字段附加 JSON 解析为明确属性，Domain 只保留业务规则需要的结果。
 *
 * @property fieldKey 子字段原始 key。
 * @property paramKey 生成请求使用的标准参数 key。
 * @property fieldType 服务端字段类型兼容标记。
 * @property required 子字段是否必填；`true` 表示激活后必须填写。
 * @property visible 子字段是否默认可见；`false` 表示即使条件满足也不应渲染。
 * @property defaultValue 默认值；`null` 表示未声明默认值。
 * @property title 标题文案；`null` 表示使用 [fieldKey] 兜底。
 * @property paramDescription 参数说明；`null` 表示无说明。
 * @property placeholder 输入占位内容；`null` 表示无占位提示。
 * @property maxLength 文本最大长度，单位为字符；`null` 表示不限制。
 * @property minLength 文本最小长度，单位为字符；`null` 表示不限制。
 * @property maxInputCount 上传或列表输入最大数量，单位为个；`null` 表示不限制。
 * @property uploadMediaKind Data 层推断出的上传媒体类型；`null` 表示无法确定。
 * @property options 子字段选项列表；空列表表示不是选项字段。
 * @property visibleWhen 子字段激活条件；`null` 表示只要父字段可见就激活。
 */
data class QuickCreationServiceFieldInputChild(
    val fieldKey: String,
    val paramKey: String,
    val fieldType: String,
    val required: Boolean = false,
    val visible: Boolean = true,
    val defaultValue: String? = null,
    val title: String? = null,
    val paramDescription: String? = null,
    val placeholder: String? = null,
    val maxLength: Int? = null,
    val minLength: Int? = null,
    val maxInputCount: Int? = null,
    val uploadMediaKind: QuickCreationUploadMediaKind? = null,
    val options: List<QuickCreationServiceFieldOption> = emptyList(),
    val visibleWhen: QuickCreationServiceFieldVisibilityCondition? = null,
)

/**
 * 服务字段动态子输入的可见条件。
 *
 * @property fieldKey 作为条件来源的父字段或同级字段 key。
 * 空字符串不应进入 Domain；匹配时会兼容字段别名。
 * @property values 能激活子字段的取值列表。
 * 空列表表示只要条件字段存在非空值即可激活；非空列表表示值必须命中其中之一。
 */
data class QuickCreationServiceFieldVisibilityCondition(
    val fieldKey: String,
    val values: List<String>,
)

/**
 * 服务字段的可选值。
 *
 * @property label 展示给用户的选项名称，来源于服务端。
 * 空字符串表示服务端未提供可读标签，Presentation 可以使用 [value] 兜底。
 * @property value 写回服务参数 Map 的原始值。
 * 空字符串通常表示服务端配置异常，调用方不应主动构造。
 */
data class QuickCreationServiceFieldOption(
    val label: String,
    val value: String,
)
