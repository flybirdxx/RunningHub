package com.runninghub.feature.task.domain

/**
 * 统一生成历史分页。
 *
 * 该模型属于 Task Feature Domain，用于承接历史页所需的跨创作来源历史数据。
 * 它只描述分页和业务条目，不携带 API 字段名、DTO 注解或 UI 组件类型。
 *
 * @property page 当前页码，从 1 开始。
 * @property size 每页条数，单位为条。
 * @property total 服务端可查询的总条数。
 * @property items 当前页历史任务列表，顺序保留仓库返回顺序。
 */
data class GenerationHistoryPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<GenerationHistoryItem>,
)

/**
 * 统一生成历史任务。
 *
 * 历史页需要同时展示 QuickCreate、标准模型和后续其他来源的任务，因此该模型放在
 * Task Feature Domain，而不是 composeApp 或 shared。Data/适配层负责把各来源模型映射为
 * 这里的稳定字段，Presentation 只根据领域状态进行筛选和展示。
 *
 * @property taskId 服务端任务稳定标识。
 * @property source 任务来源，用于区分快捷创作、标准模型或后续来源。
 * @property status 领域层暂保留的任务状态字符串；后续可收敛到 core:model 的任务状态值对象。
 * @property modelId 触发任务的模型或 SKU 标识，可能为空。
 * @property taskType 任务类型，例如 image、video 或 audio。
 * @property costAmount 任务消耗金额，单位由 [costCurrency] 决定。
 * @property costCurrency 金额币种或平台计费单位。
 * @property costTime 服务端返回的耗时文案，当前不在客户端解析。
 * @property params 可复用的生成参数；键值均为领域层可展示或回填的文本。
 * @property outputs 输出文件列表。
 */
data class GenerationHistoryItem(
    val taskId: String,
    val source: GenerationHistorySource,
    val status: String,
    val modelId: String? = null,
    val taskType: String? = null,
    val costAmount: Double = 0.0,
    val costCurrency: String? = null,
    val costTime: String? = null,
    val params: Map<String, String> = emptyMap(),
    val outputs: List<GenerationHistoryOutput> = emptyList(),
)

/**
 * 统一生成历史输出文件。
 *
 * @property outputId 输出稳定标识，用于查询详情或定位用户点击的输出。
 * @property url 输出文件地址。
 * @property type 输出类型，服务端可能返回 png、mp4、image、video 等文本。
 * @property thumbnailUrl 缩略图地址；为空时调用方可回退到 [url]。
 * @property width 输出宽度，单位为像素；未知时为空。
 * @property height 输出高度，单位为像素；未知时为空。
 * @property outputName 输出名称。
 * @property expireTime 输出过期时间文案；当前保留服务端格式。
 * @property expireDays 输出剩余有效天数文案；为空表示服务端未返回。
 */
data class GenerationHistoryOutput(
    val outputId: String,
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val outputName: String? = null,
    val expireTime: String? = null,
    val expireDays: String? = null,
) {
    /**
     * 判断输出是否可按图片展示。
     *
     * @return 服务端类型属于常见图片类型时返回 true。
     */
    val isImage: Boolean
        get() = type.lowercase() in setOf("png", "jpg", "jpeg", "webp", "image")

    /**
     * 判断输出是否可按视频展示。
     *
     * @return 服务端类型属于常见视频类型时返回 true。
     */
    val isVideo: Boolean
        get() = type.lowercase() in setOf("mp4", "webm", "mov", "video")
}

/**
 * 统一生成任务详情。
 *
 * 该模型用于 History 页点击任务卡片后展示控制台详情抽屉。它只保存领域可用字段和已经脱敏的
 * 请求/响应信息，不携带远端 DTO、endpoint、认证头或最终 UI 文案。
 *
 * @property taskId 服务端任务稳定标识。
 * @property title 任务名称；为空表示服务端未返回可展示标题。
 * @property sourceLabel 任务来源原始标签，例如 API、WEBAPP 或模型类别；最终文案由 UI 映射。
 * @property status 服务端任务状态原始字符串；Presentation 负责映射为稳定展示状态。
 * @property duration 任务 usage.taskCostTime 耗时文本；为空表示服务端 usage 未返回。
 * @property rhCoins 任务 usage.consumeCoins 消耗文本；为空表示服务端 usage 未返回。
 * @property finalAmount 任务 usage.consumeMoney 消耗文本；为空表示服务端 usage 未返回。
 * @property outputs 详情接口返回的任务输出文件列表。
 * @property basicFields 基础信息字段集合，字段名使用稳定枚举，UI 决定最终中文标签。
 * @property requestParameters 已脱敏的关键请求参数摘要，供结果详情页展示 Prompt 和可复用参数。
 * 该字段不得包含 API Key、Authorization、Cookie、Token 或验证码等敏感信息。
 * @property requestInfo 已脱敏后的请求 JSON 文本；为空表示服务端未返回。
 * @property responseInfo 已脱敏后的响应 JSON 文本；为空表示服务端未返回。
 */
data class GenerationTaskDetail(
    val taskId: String,
    val title: String? = null,
    val sourceLabel: String? = null,
    val status: String,
    val duration: String? = null,
    val rhCoins: String? = null,
    val finalAmount: String? = null,
    val outputs: List<GenerationHistoryOutput> = emptyList(),
    val basicFields: List<GenerationTaskDetailField> = emptyList(),
    val requestParameters: Map<String, String> = emptyMap(),
    val requestInfo: String? = null,
    val responseInfo: String? = null,
)

/**
 * 任务详情中的一行键值信息。
 *
 * @property key 稳定字段语义，UI 层据此映射本地化标签。
 * @property value 服务端返回或 Data 层归一化后的值；空白值不会进入字段集合。
 */
data class GenerationTaskDetailField(
    val key: GenerationTaskDetailFieldKey,
    val value: String,
)

/**
 * 任务详情字段的稳定语义。
 */
enum class GenerationTaskDetailFieldKey {
    /** 任务 ID。 */
    TASK_ID,

    /** 发起时间。 */
    CALL_TIME,

    /** 任务名称。 */
    TASK_NAME,

    /** 任务来源。 */
    TASK_SOURCE,

    /** 调用方式。 */
    CALL_TYPE,

    /** 账户。 */
    ACCOUNT,

    /** 密钥信息。 */
    API_KEY,

    /** 密钥类型。 */
    API_KEY_TYPE,

    /** 运行模式。 */
    MODE,

}

/**
 * 统一历史任务来源。
 *
 * @property key 稳定来源标识，供兼容 UI 或埋点区分历史任务来源。
 */
enum class GenerationHistorySource(val key: String) {
    /** QuickCreate 快捷创作任务。 */
    QUICK_CREATION("quick_creation"),

    /** WebApp 旧任务历史。 */
    WEBAPP("webapp"),

    /** 工作流任务。 */
    WORKFLOW("workflow"),

    /** 标准模型或旧模型 API 任务。 */
    STANDARD_MODEL("standard_model"),
}
