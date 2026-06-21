package com.runninghub.feature.quickcreate.domain

/**
 * 旧版图片生成入口使用的本地模型身份。
 *
 * 该枚举位于 Domain 层，只保留业务可见的模型标识、展示名称和说明。
 * 具体 OpenAPI endpoint、请求 path 和渠道分组属于 Data 层职责，由
 * `QuickCreateRepositoryImpl` 根据枚举值路由到对应 API 方法，避免远端路径泄露到 Domain。
 *
 * @property displayName 面向用户或调试信息展示的中文模型名称，允许包含版本和渠道描述。
 * 该值不参与远端路由，修改文案不应改变请求目标。
 * @property modelKey 旧版生成请求中的稳定模型标识，来自 UI 选择或草稿恢复。
 * 该值可用于跨页面传递和持久化，但不是远端 endpoint。
 * @property description 模型能力说明，用于展示或调试；空字符串不建议使用，
 * 因为调用方可能依赖该字段向用户解释模型差异。
 */
enum class ImageModel(
    val displayName: String,
    val modelKey: String,
    val description: String,
) {
    // 全能图片 G 系列
    ALL_POWER_IMAGE_G_2_OFFICIAL(
        displayName = "全能图片G-2.0-官方版",
        modelKey = "all-power-image-g2",
        description = "顶级图像生成底座，支持任意分辨率，革命性文本渲染能力"
    ),
    ALL_POWER_IMAGE_G_2_CHEAP(
        displayName = "全能图片G-2.0-低价版",
        modelKey = "all-power-image-g2",
        description = "全能图片G-2.0的低价渠道版，价格更优但不稳定"
    ),
    // 全能图片 X 系列
    ALL_POWER_IMAGE_X_OFFICIAL(
        displayName = "全能图片X-官方版",
        modelKey = "all-power-image-x-official",
        description = "全能图片X文生图官方稳定版，精准文本解析"
    ),
    ALL_POWER_IMAGE_X_CHEAP(
        displayName = "全能图片X-低价版",
        modelKey = "all-power-image-x",
        description = "全能图片X低价渠道版，价格更优但不稳定"
    ),
    // 全能图片 Pro 系列
    ALL_POWER_IMAGE_PRO_OFFICIAL(
        displayName = "全能图片Pro-官方版",
        modelKey = "all-power-image-pro-official",
        description = "专业级图像编辑，支持4K超清画质输出"
    ),
    ALL_POWER_IMAGE_PRO_CHEAP(
        displayName = "全能图片Pro-低价版",
        modelKey = "all-power-image-pro",
        description = "全能图片Pro低价渠道版"
    ),
    // 全能图片 2.0 系列
    ALL_POWER_IMAGE_2_OFFICIAL(
        displayName = "全能图片2.0-官方版",
        modelKey = "all-power-image-v2-official",
        description = "全能图片V2官方稳定版，4K超高清画质"
    ),
    ALL_POWER_IMAGE_2_CHEAP(
        displayName = "全能图片2.0-低价版",
        modelKey = "all-power-image-v2",
        description = "全能图片2.0低价渠道版"
    ),
    // Seedream 系列
    SEEDREAM_5_0_LITE(
        displayName = "Seedream 5.0 Lite",
        modelKey = "seedream5",
        description = "豆包大模型新一代智能视觉创作引擎，支持文生组图"
    ),
    SEEDREAM_4_0(
        displayName = "Seedream 4.0",
        modelKey = "seedream4",
        description = "字节跳动专业级布局感知型文生图模型"
    ),
}

/**
 * 旧版视频生成入口使用的本地模型身份与能力描述。
 *
 * 该枚举只表达 Domain 层需要知道的模型选择项和能力开关。
 * 远端 provider 分类、OpenAPI path 和渠道编码由 Data 层路由表维护，避免 Presentation
 * 或 Domain 通过字段拼接网络地址。
 *
 * @property displayName 面向用户或调试信息展示的视频模型名称，允许包含版本和渠道描述。
 * @property modelKey 旧版视频生成请求中的稳定模型标识，来自页面选择或草稿恢复。
 * 该值用于匹配枚举，不代表远端接口路径。
 * @property description 模型能力说明，用于展示或诊断模型选择，不参与请求路由。
 * @property supportsTextToVideo `true` 表示该模型支持仅凭文本生成视频；
 * `false` 表示必须提供图片、首帧或其他媒体输入。
 * @property supportsImageToVideo `true` 表示该模型支持图片或首帧驱动的视频生成；
 * `false` 表示图片输入不应进入该模型的生成链路。
 * @property supportsFirstLastFrame `true` 表示模型支持首尾帧约束；
 * `false` 表示尾帧参数应在提交前被忽略或阻止。
 */
enum class VideoModel(
    val displayName: String,
    val modelKey: String,
    val description: String,
    val supportsTextToVideo: Boolean,
    val supportsImageToVideo: Boolean,
    val supportsFirstLastFrame: Boolean,
) {
    // HappyHorse
    HAPPYHORSE(
        displayName = "HappyHorse",
        modelKey = "happyhorse",
        description = "阿里巴巴多模态视频生成模型",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // Seedance 2.0 系列
    SEEDANCE_2_0(
        displayName = "Seedance2.0",
        modelKey = "seedance2",
        description = "字节跳动顶级图生视频，追求最高生成品质",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    SEEDANCE_2_0_FAST(
        displayName = "Seedance2.0-Fast",
        modelKey = "seedance2-fast",
        description = "Seedance2.0极速版，更注重生成速度与性价比",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 可灵 3.0 系列
    KLING_3_0_4K(
        displayName = "可灵3.0-4k",
        modelKey = "kling-o3-4k",
        description = "快手4K级图生视频，影院级画质",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O3_PRO(
        displayName = "可灵03-Pro",
        modelKey = "kling-o3-pro",
        description = "可灵O3系列顶级画质，专业创作首选",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O3_STD(
        displayName = "可灵03-Std",
        modelKey = "kling-o3-std",
        description = "可灵O3系列高性价比旗舰方案",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O1(
        displayName = "可灵01",
        modelKey = "kling-o1",
        description = "可灵o1统一多模态视频生成引擎",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 万相 2.7
    WAN_2_7(
        displayName = "万相2.7",
        modelKey = "wanxiang2.7",
        description = "阿里通义万相2.7，先进文本/图像生成视频模型",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 万相 2.6
    WAN_2_6(
        displayName = "万相2.6",
        modelKey = "wanxiang2.6",
        description = "阿里通义万相2.6，专业级电影级视频生成",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // PixVerse V6
    PIXVERSE_V6(
        displayName = "PixVerseV6",
        modelKey = "pixverse-v6",
        description = "PixVerse第六代图生视频，支持Thinking模式",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // 全能视频 V-Pro 系列
    ALL_POWER_VIDEO_V_PRO_OFFICIAL(
        displayName = "全能视频V-Pro-官方版",
        modelKey = "all-power-video-v-pro",
        description = "全能视频V3.1专业版，支持4K高清和参考生视频",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    ALL_POWER_VIDEO_V_PRO_CHEAP(
        displayName = "全能视频V-Pro-低价版",
        modelKey = "all-power-video-v-pro-cheap",
        description = "全能视频V3.1专业版低价渠道",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 全能视频 V-Fast 系列
    ALL_POWER_VIDEO_V_FAST_OFFICIAL(
        displayName = "全能视频V-Fast-官方版",
        modelKey = "all-power-video-v-fast",
        description = "全能视频V3.1极速版，生成速度快30%",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    ALL_POWER_VIDEO_V_FAST_CHEAP(
        displayName = "全能视频V-Fast-低价版",
        modelKey = "all-power-video-v-fast-cheap",
        description = "全能视频V3.1极速版低价渠道",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 全能视频 X 系列
    ALL_POWER_VIDEO_X_OFFICIAL(
        displayName = "全能视频X-官方版",
        modelKey = "all-power-video-x",
        description = "全能视频X官方稳定版，主体身份绝对锁定",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    ALL_POWER_VIDEO_X_CHEAP(
        displayName = "全能视频X-低价版",
        modelKey = "all-power-video-x-cheap",
        description = "全能视频X低价渠道版",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // Vidu Q3 系列
    VIDU_Q3_PRO(
        displayName = "ViduQ3-Pro",
        modelKey = "vidu-q3-pro",
        description = "Vidu Q3专业版，音视频一体化叙事",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    VIDU_Q3_PRO_FAST(
        displayName = "ViduQ3-Pro-Fast",
        modelKey = "vidu-q3-pro-fast",
        description = "Vidu Q3极速版，兼顾卓越画质与极致效率",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
}

/**
 * 旧版图片生成入口提交给 Domain Repository 的请求参数。
 *
 * 本对象只描述用户输入、模型选择、全局参考图和快捷创作服务参数，不携带 OpenAPI endpoint、
 * 请求头、Cookie 或平台文件句柄。Data 层实现负责根据 [model] 和快捷创作服务身份路由到远端接口。
 *
 * @property prompt 用户输入的图片提示词，提交前应由 Presentation 裁剪首尾空白；空字符串只适合草稿阶段。
 * @property model 旧版图片模型稳定标识，通常来自 [ImageModel.modelKey] 或服务模型映射值，不是远端 endpoint。
 * @property aspectRatio 图片画幅比例字符串，例如 `1:1`、`16:9`；默认 `16:9` 表示横向输出。
 * @property resolution 目标分辨率标识，例如 `1K`；默认值由旧版图片入口沿用。
 * @property quality 生成质量标识，例如 `medium`；具体可用值由所选模型和服务端决定。
 * @property referenceImageUri 全局参考图片远端 URL；`null` 表示未提供参考图，字段绑定素材应写入 [quickCreationListParams]。
 * @property numImages 期望生成图片数量，单位为张；默认 1，小于 1 的值应在提交前被上游阻止。
 * @property seed 可选随机种子；`null` 表示由服务端或模型自动决定随机性。
 * @property negativePrompt 可选反向提示词；`null` 表示不提交反向约束。
 * @property quickCreationCategoryId 快捷创作服务分类 ID；`null` 表示走旧版本地模型兜底链路。
 * @property quickCreationBindingId 快捷创作服务绑定 ID；`null` 表示没有选中可提交的远端服务模型。
 * @property quickCreationSkuId 快捷创作服务 SKU ID；`null` 表示没有可用于计费和提交的远端 SKU。
 * @property quickCreationParams 快捷创作单值参数，key 为服务字段标准参数名；空 Map 表示没有动态单值参数。
 * @property quickCreationListParams 快捷创作多值参数，通常用于字段级媒体 URL 列表；空 Map 表示没有动态列表参数。
 */
data class ImageGenerationRequest(
    val prompt: String,
    val model: String,
    val aspectRatio: String = "16:9",
    val resolution: String = "1K",
    val quality: String = "medium",
    val referenceImageUri: String? = null,
    val numImages: Int = 1,
    val seed: Int? = null,
    val negativePrompt: String? = null,
    val quickCreationCategoryId: String? = null,
    val quickCreationBindingId: String? = null,
    val quickCreationSkuId: String? = null,
    val quickCreationParams: Map<String, String> = emptyMap(),
    val quickCreationListParams: Map<String, List<String>> = emptyMap(),
)

/**
 * 旧版视频生成入口提交给 Domain Repository 的请求参数。
 *
 * 本对象只描述用户选择、媒体引用和快捷创作服务参数，不携带 OpenAPI endpoint、
 * provider category 或请求头信息。Data 层实现负责根据 [model] 和快捷创作服务标识路由到
 * 具体远端接口，并读取必要凭据。
 *
 * @property prompt 用户输入的视频提示词，调用方应在构造前完成首尾空白裁剪；
 * 空字符串只允许在草稿或预览构造阶段出现，真正提交前应由 Presentation 阻止。
 * @property model 旧版视频模型稳定标识，通常来自 [VideoModel.modelKey] 或页面配置的 apiValue。
 * 该值用于 Data 层匹配本地路由，不是远端 endpoint。
 * @property aspectRatio 视频画幅比例字符串，例如 `16:9`、`9:16`；默认值表示横屏。
 * @property duration 期望视频时长，单位为秒；默认 5 秒，具体有效范围由所选模型和服务端校验。
 * @property resolution 目标视频分辨率标识，例如 `720p`；默认值表示常规高清输出。
 * @property referenceImageUri 全局参考图片的远端 URL；`null` 表示未提供全局图片素材。
 * 字段绑定素材应放入 [quickCreationListParams]，不应写入该字段。
 * @property referenceVideoUri 全局参考视频的远端 URL；`null` 表示未提供全局视频素材。
 * @property referenceAudioUri 全局参考音频的远端 URL；`null` 表示未提供全局音频素材。
 * @property firstFrameImageUri 首帧图片 URL；`null` 表示本次请求不使用首帧约束。
 * 仅支持首帧能力的模型才应消费该字段。
 * @property lastFrameImageUri 尾帧图片 URL；`null` 表示本次请求不使用尾帧约束。
 * 对不支持首尾帧的模型，Data 层或上游编辑状态应忽略该值。
 * @property realistic `true` 表示启用真人/写实相关模式；`false` 表示使用模型默认风格。
 * @property generateAudio `true` 表示请求生成或保留音频；`false` 表示不要求音频输出。
 * @property numVideos 期望生成数量，默认 1；小于 1 的值应在提交前被上游校验阻止。
 * @property seed 可选随机种子；`null` 表示由服务端或模型自动生成随机性。
 * @property negativePrompt 可选反向提示词；`null` 表示不提交反向约束。
 * @property style 旧版风格标识，默认 `general` 表示通用风格。
 * @property promptExtend `true` 表示允许服务端或模型扩写提示词；`false` 表示尽量按原提示词生成。
 * @property quickCreationCategoryId 快捷创作服务分类 ID；`null` 表示走旧版本地模型兜底链路。
 * @property quickCreationBindingId 快捷创作服务绑定 ID；`null` 表示没有选中远端服务模型。
 * @property quickCreationSkuId 快捷创作服务 sku ID；`null` 表示没有可用于计费/提交的远端 SKU。
 * @property quickCreationParams 快捷创作普通参数，key 为服务字段标准参数名，value 为单值文本。
 * 空 Map 表示没有额外单值参数。
 * @property quickCreationListParams 快捷创作多值参数，通常用于媒体列表或多选字段。
 * 空 Map 表示没有额外列表参数；集合顺序按用户选择或字段顺序保留。
 */
data class VideoGenerationRequest(
    val prompt: String,
    val model: String,
    val aspectRatio: String = "16:9",
    val duration: Int = 5,
    val resolution: String = "720p",
    val referenceImageUri: String? = null,
    val referenceVideoUri: String? = null,
    val referenceAudioUri: String? = null,
    val firstFrameImageUri: String? = null,
    val lastFrameImageUri: String? = null,
    val realistic: Boolean = false,
    val generateAudio: Boolean = false,
    val numVideos: Int = 1,
    val seed: Int? = null,
    val negativePrompt: String? = null,
    val style: String = "general",
    val promptExtend: Boolean = true,
    val quickCreationCategoryId: String? = null,
    val quickCreationBindingId: String? = null,
    val quickCreationSkuId: String? = null,
    val quickCreationParams: Map<String, String> = emptyMap(),
    val quickCreationListParams: Map<String, List<String>> = emptyMap(),
)

/**
 * 快捷创作任务提交后的状态流事件。
 *
 * 该 sealed class 位于 Domain 层，用于让提交 Interactor 和 ScreenModel 观察任务生命周期。
 * 具体轮询接口、重试间隔和错误响应格式由 Data 层实现隐藏在 Repository 内部。
 */
sealed class QuickCreateTaskStatus {
    /** 已发起提交请求但尚未拿到远端任务 ID。 */
    data object Submitting : QuickCreateTaskStatus()

    /**
     * 远端已经创建任务但仍在队列中。
     *
     * @property taskId 服务端任务稳定标识，可用于后续轮询、取消和历史详情查询。
     */
    data class Queuing(val taskId: String) : QuickCreateTaskStatus()

    /**
     * 远端任务正在生成中。
     *
     * @property taskId 服务端任务稳定标识。
     * @property progress 生成进度百分比，通常为 0 到 100；服务端异常值由 Data 层尽量归一化。
     */
    data class Running(val taskId: String, val progress: Int) : QuickCreateTaskStatus()

    /**
     * 远端任务已成功完成。
     *
     * @property taskId 服务端任务稳定标识。
     * @property results 输出结果列表，顺序保留服务端返回顺序；空列表表示服务端完成但没有可展示输出。
     */
    data class Success(val taskId: String, val results: List<QuickCreateResultItem>) : QuickCreateTaskStatus()

    /**
     * 远端任务以业务失败状态结束。
     *
     * @property taskId 服务端任务稳定标识。
     * @property errorMessage Data 层从远端业务错误中提取的失败说明，或 [QuickCreateTaskIssueCode] 中的稳定错误码。
     * Presentation 必须在展示前再次映射，避免 Data 层生成最终 UI 文案。
     */
    data class Failed(val taskId: String, val errorMessage: String) : QuickCreateTaskStatus()

    /**
     * 远端任务已被取消。
     *
     * @property taskId 服务端任务稳定标识。取消属于业务终态，Data 层收到该状态后必须停止继续轮询，
     * Presentation 层可据此展示独立的取消提示，而不是把它降级成失败或继续排队。
     */
    data class Cancelled(val taskId: String) : QuickCreateTaskStatus()

    /**
     * 提交、轮询或响应解析过程中发生不可继续的异常。
     *
     * @property message Data 层返回的稳定错误码或远端错误摘要，不应包含 Token、Cookie 或完整请求头。
     * Presentation 必须在展示前再次映射，避免把内部诊断信息直接暴露给用户。
     */
    data class Error(val message: String) : QuickCreateTaskStatus()
}

/**
 * 快捷创作任务状态流使用的稳定错误码。
 *
 * 这些常量属于 Domain 层错误语义，不携带最终 UI 文案。Data 层在缺少服务端错误摘要时返回这些
 * 错误码，Presentation 层再根据页面场景映射为本地化文案，从而避免 Repository 直接生成中文提示。
 */
object QuickCreateTaskIssueCode {
    /**
     * 远端任务以失败状态结束，但服务端没有提供可用失败摘要。
     */
    const val TASK_FAILED = "TASK_FAILED"

    /**
     * 任务轮询超过客户端允许的最大次数。
     */
    const val TASK_TIMEOUT = "TASK_TIMEOUT"

    /**
     * 查询快捷创作任务列表或任务状态失败。
     */
    const val TASK_QUERY_FAILED = "TASK_QUERY_FAILED"

    /**
     * 计费预览接口失败或响应缺少必要数据。
     */
    const val FEE_PREVIEW_FAILED = "FEE_PREVIEW_FAILED"

    /**
     * 计费预览明确表示余额不足或业务条件不允许提交。
     */
    const val FEE_PREVIEW_BLOCKED = "FEE_PREVIEW_BLOCKED"

    /**
     * 任务预提交失败，无法获取后续 commit 所需 prepare token。
     */
    const val PREPARE_FAILED = "PREPARE_FAILED"

    /**
     * 任务提交失败，远端没有返回有效任务 ID。
     */
    const val COMMIT_FAILED = "COMMIT_FAILED"

    /**
     * 本地捕获到异常但无法归入更具体的任务错误。
     */
    const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
}

/**
 * 快捷创作任务输出结果。
 *
 * @property url 输出媒体远端 URL；空字符串表示服务端数据异常，Data 层通常不应构造该对象。
 * @property type 输出媒体类型或文件扩展名，例如 `image`、`video`、`mp4`、`png`。
 * @property thumbnailUrl 可选缩略图远端 URL；`null` 表示没有缩略图，应使用 [url] 或占位图展示。
 * @property width 输出宽度，单位为像素；`null` 表示服务端未提供尺寸。
 * @property height 输出高度，单位为像素；`null` 表示服务端未提供尺寸。
 * @property duration 视频或音频时长，单位为秒；`null` 表示非时长媒体或服务端未提供。
 */
data class QuickCreateResultItem(
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

/**
 * 快捷创作计费预览结果。
 *
 * @property passed `true` 表示当前余额和业务条件允许提交；`false` 表示应阻止提交并提示原因。
 * @property free `true` 表示本次预览按免费处理；`false` 表示可能需要消耗点数或现金余额。
 * @property settlementMode 服务端结算模式标记；`null` 表示服务端未返回，Presentation 不应直接展示原值。
 * @property requiredRhAmount 预计消耗 RH 点数，单位为点；默认 0 表示不消耗或服务端未返回。
 * @property requiredCashAmount 预计消耗现金余额，单位由 [cashCurrency] 决定；默认 0 表示不消耗现金。
 * @property userCashBalance 用户当前现金余额，单位由 [cashCurrency] 决定；默认 0 表示无余额或服务端未返回。
 * @property insufficientType 余额不足类型标记；`null` 表示没有余额不足原因或服务端未声明。
 * @property cashCurrency 现金币种标记；`null` 表示服务端未声明币种，调用方不得假设默认币种。
 */
data class QuickCreationFeePreview(
    val passed: Boolean,
    val free: Boolean,
    val settlementMode: String? = null,
    val requiredRhAmount: Double = 0.0,
    val requiredCashAmount: Double = 0.0,
    val userCashBalance: Double = 0.0,
    val insufficientType: String? = null,
    val cashCurrency: String? = null,
)

/**
 * 快捷创作历史分页结果。
 *
 * @property page 当前页码，从 1 开始；0 或负数表示服务端异常数据。
 * @property size 每页条数，单位为条；0 表示服务端返回空页或异常分页信息。
 * @property total 历史总条数，单位为条；0 表示没有历史或服务端未返回总数。
 * @property items 当前页历史任务，顺序保持服务端返回顺序；空列表表示当前页没有可展示任务。
 */
data class QuickCreationHistoryPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<QuickCreationHistoryItem>,
)

/**
 * 快捷创作历史任务条目。
 *
 * @property taskId 服务端任务稳定标识，用于取消、详情查询和轮询状态；空字符串表示异常数据。
 * @property status 服务端任务状态原始标记，Presentation 根据本地规则映射为可见状态。
 * @property categoryId 快捷创作服务分类 ID；`null` 表示历史来自旧版入口或服务端未返回。
 * @property bindingId 服务模型绑定 ID；`null` 表示无法回填到当前模型目录。
 * @property skuId 服务模型 SKU ID；`null` 表示无法直接复用计费或提交身份。
 * @property taskType 服务端任务类型标记；`null` 表示服务端未声明。
 * @property taskCostTime 服务端返回的耗时文本；`null` 表示未完成或未提供耗时。
 * @property params 历史任务的单值参数快照，key 为服务字段参数名；空 Map 表示没有可恢复参数。
 * @property cashAmount 本次任务现金消耗金额；0 表示免费、未扣费或服务端未返回。
 * @property cashCurrency 现金币种标记；`null` 表示服务端未声明币种。
 * @property outputs 历史输出列表，顺序保持服务端返回顺序；空列表表示没有可展示输出。
 */
data class QuickCreationHistoryItem(
    val taskId: String,
    val status: String,
    val categoryId: String? = null,
    val bindingId: String? = null,
    val skuId: String? = null,
    val taskType: String? = null,
    val taskCostTime: String? = null,
    val params: Map<String, String> = emptyMap(),
    val cashAmount: Double = 0.0,
    val cashCurrency: String? = null,
    val outputs: List<QuickCreationHistoryOutput> = emptyList(),
)

/**
 * 快捷创作历史输出条目。
 *
 * @property outputId 服务端输出稳定标识，用于详情查询；空字符串表示异常数据。
 * @property url 输出媒体远端 URL；空字符串表示服务端没有返回可访问地址。
 * @property type 输出媒体类型或扩展名，大小写不敏感。
 * @property thumbnailUrl 缩略图远端 URL；`null` 表示没有缩略图。
 * @property width 输出宽度，单位为像素；`null` 表示未知。
 * @property height 输出高度，单位为像素；`null` 表示未知。
 * @property outputName 服务端输出名称；`null` 表示未命名。
 * @property expireTime 输出过期时间文本，格式由服务端决定；`null` 表示未返回过期时间。
 * @property expireDays 距离过期的天数字符串；`null` 表示服务端未返回。
 */
data class QuickCreationHistoryOutput(
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
    /** `true` 表示 [type] 可按图片输出展示；`false` 表示不是已知图片类型。 */
    val isImage: Boolean
        get() = type.lowercase() in setOf("png", "jpg", "jpeg", "webp", "image")

    /** `true` 表示 [type] 可按视频输出展示；`false` 表示不是已知视频类型。 */
    val isVideo: Boolean
        get() = type.lowercase() in setOf("mp4", "webm", "mov", "video")
}

/**
 * 快捷创作项目分页结果。
 *
 * @property page 当前页码，从 1 开始。
 * @property size 每页项目数量，单位为个。
 * @property total 项目总数，单位为个；0 表示没有项目或服务端未返回。
 * @property pages 总页数；0 表示没有分页数据。
 * @property hasNext `true` 表示仍可继续加载下一页；`false` 表示已经到达末页。
 * @property hasPrevious `true` 表示当前页前面还有项目页；`false` 表示当前页是第一页或未知。
 * @property nextCursor 下一页游标；`null` 表示服务端使用页码分页或没有下一页。
 * @property items 当前页项目列表，顺序保持服务端返回顺序；空列表表示当前页没有项目。
 */
data class QuickCreationProjectPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val pages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val nextCursor: String? = null,
    val items: List<QuickCreationProject>,
)

/**
 * 快捷创作项目。
 *
 * @property projectId 服务端项目稳定标识，用于重命名、删除、置顶和筛选任务；空字符串表示异常数据。
 * @property name 项目名称，来源于用户输入或服务端默认名；空字符串不应主动构造。
 * @property coverUrl 项目封面远端 URL；`null` 表示项目没有封面或服务端未返回。
 * @property taskCount 项目下任务数量，单位为个；0 表示无任务或服务端未返回。
 * @property pinned `true` 表示项目已置顶；`false` 表示普通排序。
 * @property createdAt 创建时间文本，格式由服务端决定；`null` 表示未返回。
 * @property updatedAt 更新时间文本，格式由服务端决定；`null` 表示未返回。
 */
data class QuickCreationProject(
    val projectId: String,
    val name: String,
    val coverUrl: String? = null,
    val taskCount: Int = 0,
    val pinned: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

/**
 * 快捷创作灵感标签。
 *
 * @property id 标签稳定标识，用于模板分页筛选；空字符串表示异常数据。
 * @property name 标签展示名称，来源于服务端；空字符串表示服务端未提供可读名称。
 */
data class QuickCreateInspirationTag(
    val id: String,
    val name: String,
)

/**
 * 快捷创作灵感模板列表项。
 *
 * @property templateId 模板稳定标识，用于读取详情；空字符串表示异常数据。
 * @property title 模板标题，来源于服务端；空字符串表示无可读标题。
 * @property categoryId 模板关联的快捷创作分类 ID；`null` 表示无法直接定位服务分类。
 * @property coverUrl 封面图片远端 URL；`null` 表示没有图片封面。
 * @property videoUrl 预览视频远端 URL；`null` 表示没有视频预览。
 * @property tagHot `true` 表示服务端标记为热门；`false` 表示没有热门标记。
 * @property tagNew `true` 表示服务端标记为新品；`false` 表示没有新品标记。
 */
data class QuickCreateInspirationTemplate(
    val templateId: String,
    val title: String,
    val categoryId: String?,
    val coverUrl: String?,
    val videoUrl: String?,
    val tagHot: Boolean,
    val tagNew: Boolean,
)

/**
 * 快捷创作灵感模板分页结果。
 *
 * @property page 当前页码，从 1 开始。
 * @property size 每页模板数量，单位为条。
 * @property total 模板总数，单位为条；0 表示无模板或服务端未返回总数。
 * @property pages 总页数；0 表示没有分页数据。
 * @property hasNext `true` 表示仍可加载下一页；`false` 表示当前筛选已经到末页。
 * @property hasPrevious `true` 表示当前页前面还有模板页；`false` 表示当前页是第一页或未知。
 * @property nextCursor 下一页游标；`null` 表示服务端使用页码分页或没有下一页。
 * @property items 当前页模板列表，顺序保持服务端返回顺序；空列表表示当前页无模板。
 */
data class QuickCreateInspirationTemplatePage(
    val page: Int,
    val size: Int,
    val total: Int,
    val pages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean,
    val nextCursor: String? = null,
    val items: List<QuickCreateInspirationTemplate>,
)

/**
 * 快捷创作灵感模板详情。
 *
 * 详情用于把模板内容回填到编辑器、动态服务参数和字段级素材中。该模型不保存 DTO 原始 JSON，
 * Data 层负责把服务端 snapshot 解析为标准参数结构。
 *
 * @property templateId 模板稳定标识，来源于列表项或详情接口。
 * @property title 模板标题，来源于服务端；空字符串表示无可读标题。
 * @property categoryId 模板关联的快捷创作分类 ID；`null` 表示不能直接切换模型分类。
 * @property bindingId 模板关联的服务绑定 ID；`null` 表示不能直接定位服务模型。
 * @property skuId 模板关联的服务 SKU ID；`null` 表示不能直接复用服务计费身份。
 * @property prompt 模板提示词；`null` 表示模板不包含可回填提示词。
 * @property params 模板单值参数快照，key 为服务字段参数名；空 Map 表示没有动态单值参数。
 * @property listParams 模板多值参数快照，通常是字段级媒体 URL；空 Map 表示没有动态列表参数。
 * @property coverUrl 模板封面图片远端 URL；`null` 表示没有封面。
 * @property videoUrl 模板预览视频远端 URL；`null` 表示没有视频预览。
 */
data class QuickCreateInspirationTemplateDetail(
    val templateId: String,
    val title: String,
    val categoryId: String?,
    val bindingId: String?,
    val skuId: String?,
    val prompt: String?,
    val params: Map<String, String> = emptyMap(),
    val listParams: Map<String, List<String>> = emptyMap(),
    val coverUrl: String? = null,
    val videoUrl: String? = null,
)
