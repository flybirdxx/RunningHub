package com.runninghub.shared.domain.repository

import kotlinx.coroutines.flow.Flow

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

sealed class QuickCreateTaskStatus {
    data object Submitting : QuickCreateTaskStatus()
    data class Queuing(val taskId: String) : QuickCreateTaskStatus()
    data class Running(val taskId: String, val progress: Int) : QuickCreateTaskStatus()
    data class Success(val taskId: String, val results: List<QuickCreateResultItem>) : QuickCreateTaskStatus()
    data class Failed(val taskId: String, val errorMessage: String) : QuickCreateTaskStatus()
    data class Error(val message: String) : QuickCreateTaskStatus()
}

data class QuickCreateResultItem(
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

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

data class QuickCreationHistoryPage(
    val page: Int,
    val size: Int,
    val total: Int,
    val items: List<QuickCreationHistoryItem>,
)

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
    val isImage: Boolean
        get() = type.lowercase() in setOf("png", "jpg", "jpeg", "webp", "image")

    val isVideo: Boolean
        get() = type.lowercase() in setOf("mp4", "webm", "mov", "video")
}

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

data class QuickCreationProject(
    val projectId: String,
    val name: String,
    val coverUrl: String? = null,
    val taskCount: Int = 0,
    val pinned: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class QuickCreateInspirationTag(
    val id: String,
    val name: String,
)

data class QuickCreateInspirationTemplate(
    val templateId: String,
    val title: String,
    val categoryId: String?,
    val coverUrl: String?,
    val videoUrl: String?,
    val tagHot: Boolean,
    val tagNew: Boolean,
)

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
    val inputExtraJson: String? = null,
    val inputExtra: QuickCreationServiceFieldExtra? = null,
    val visible: Boolean = true,
)

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
    val options: List<QuickCreationServiceFieldOption> = emptyList(),
    val visibleWhen: QuickCreationServiceFieldVisibilityCondition? = null,
)

data class QuickCreationServiceFieldVisibilityCondition(
    val fieldKey: String,
    val values: List<String>,
)

data class QuickCreationServiceFieldOption(
    val label: String,
    val value: String,
)

interface QuickCreateRepository {
    fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus>
    fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus>
    suspend fun previewImageQuickCreationFee(request: ImageGenerationRequest): Result<QuickCreationFeePreview>
    suspend fun previewVideoQuickCreationFee(request: VideoGenerationRequest): Result<QuickCreationFeePreview>
    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String>
    suspend fun getInspirationTags(): Result<List<QuickCreateInspirationTag>>
    suspend fun getInspirationTemplates(
        page: Int = 1,
        size: Int = 20,
        tagId: String? = null,
    ): Result<QuickCreateInspirationTemplatePage>
    suspend fun getInspirationTemplateDetail(templateId: String): Result<QuickCreateInspirationTemplateDetail>
    suspend fun getModels(categoryId: String): Result<List<QuickCreationServiceModel>>
    suspend fun listQuickCreationHistory(page: Int = 1, size: Int = 10): Result<QuickCreationHistoryPage>
    suspend fun getQuickCreationHistoryDetail(outputId: String): Result<QuickCreationHistoryItem>
    suspend fun cancelQuickCreationTask(taskId: String): Result<Unit>
    suspend fun listQuickCreationProjects(page: Int = 1, size: Int = 20): Result<QuickCreationProjectPage>
    suspend fun listQuickCreationProjectTasks(
        projectId: String,
        page: Int = 1,
        size: Int = 10,
    ): Result<QuickCreationHistoryPage>
    suspend fun createQuickCreationProject(name: String): Result<QuickCreationProject>
    suspend fun renameQuickCreationProject(projectId: String, name: String): Result<Unit>
    suspend fun deleteQuickCreationProject(projectId: String): Result<Unit>
    suspend fun pinQuickCreationProject(projectId: String, pinned: Boolean): Result<Unit>
    suspend fun getQuickCreationProjectDetail(projectId: String): Result<QuickCreationProject>
}
