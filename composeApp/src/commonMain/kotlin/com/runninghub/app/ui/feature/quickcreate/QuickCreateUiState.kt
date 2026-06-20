package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreationServiceModel

enum class QuickCreateTab(val displayName: String) {
    IMAGE("图片"),
    VIDEO("视频"),
}

enum class QuickCreateMode(val displayName: String) {
    CREATION("创作"),
    INSPIRATION("灵感"),
}

enum class QuickCreateSheet {
    MODEL_PICKER,
    PARAMS,
}

const val MAX_PROMPT_CHARS = 500
const val MAX_VISIBLE_CHARS_WARN = 400

enum class QuickCreateMediaType {
    IMAGE, VIDEO, AUDIO,
}

data class MediaReference(
    val id: String,
    val type: QuickCreateMediaType,
    val uri: String,
    val displayName: String,
    val fileSizeBytes: Long,
    val fieldParamKey: String? = null,
    val durationSeconds: Int? = null,
    val uploadStatus: UploadStatus = UploadStatus.UPLOADING,
    val uploadProgress: Float = 0f,
    val remoteUrl: String? = null,
)

enum class UploadStatus {
    UPLOADING, PROCESSING, DONE, FAILED,
}

// ═══════════════════════════════════════════════════
//  Image Models
// ═══════════════════════════════════════════════════

enum class ImageAspectRatio(val displayName: String, val apiValue: String) {
    RATIO_16_9("16:9", "16:9"),
    RATIO_4_3("4:3", "4:3"),
    RATIO_1_1("1:1", "1:1"),
    RATIO_3_4("3:4", "3:4"),
    RATIO_9_16("9:16", "9:16"),
    RATIO_21_9("21:9", "21:9"),
}

enum class ImageResolution(val displayName: String, val apiValue: String) {
    RES_1K("1K", "1K"),
    RES_2K("2K", "2K"),
    RES_4K("4K", "4K"),
}

enum class ImageQuality(val displayName: String, val apiValue: String) {
    QUALITY_LOW("低", "low"),
    QUALITY_MEDIUM("中", "medium"),
    QUALITY_HIGH("高", "high"),
}

enum class ImageModel(
    val displayName: String,
    val apiValue: String,
    val defaultAspectRatio: ImageAspectRatio,
    val defaultResolution: ImageResolution,
    val defaultQuality: ImageQuality,
    val supportedResolutions: Set<ImageResolution>,
    val supportedRatios: Set<ImageAspectRatio>,
    val supportedQualities: Set<ImageQuality>,
    val supportsImageToImage: Boolean = true,
    val baseCost: Double,
) {
    ALL_POWER_IMAGE_G2(
        displayName = "全能图片 G-2.0",
        apiValue = "all-power-image-g2",
        defaultAspectRatio = ImageAspectRatio.RATIO_16_9,
        defaultResolution = ImageResolution.RES_1K,
        defaultQuality = ImageQuality.QUALITY_MEDIUM,
        supportedResolutions = ImageResolution.entries.toSet(),
        supportedRatios = ImageAspectRatio.entries.toSet(),
        supportedQualities = ImageQuality.entries.toSet(),
        supportsImageToImage = true,
        baseCost = 0.93,
    ),
    SEEDREAM_5(
        displayName = "Seedream 5.0",
        apiValue = "seedream5",
        defaultAspectRatio = ImageAspectRatio.RATIO_3_4,
        defaultResolution = ImageResolution.RES_1K,
        defaultQuality = ImageQuality.QUALITY_MEDIUM,
        supportedResolutions = ImageResolution.entries.toSet(),
        supportedRatios = setOf(
            ImageAspectRatio.RATIO_3_4, ImageAspectRatio.RATIO_1_1,
            ImageAspectRatio.RATIO_4_3, ImageAspectRatio.RATIO_16_9,
            ImageAspectRatio.RATIO_9_16,
        ),
        supportedQualities = ImageQuality.entries.toSet(),
        supportsImageToImage = true,
        baseCost = 1.50,
    ),
    SEEDREAM_4(
        displayName = "Seedream 4.0",
        apiValue = "seedream4",
        defaultAspectRatio = ImageAspectRatio.RATIO_3_4,
        defaultResolution = ImageResolution.RES_1K,
        defaultQuality = ImageQuality.QUALITY_MEDIUM,
        supportedResolutions = setOf(ImageResolution.RES_1K, ImageResolution.RES_2K),
        supportedRatios = setOf(
            ImageAspectRatio.RATIO_3_4, ImageAspectRatio.RATIO_1_1,
            ImageAspectRatio.RATIO_4_3, ImageAspectRatio.RATIO_16_9,
            ImageAspectRatio.RATIO_9_16,
        ),
        supportedQualities = ImageQuality.entries.toSet(),
        supportsImageToImage = true,
        baseCost = 0.80,
    ),
    ;

    fun estimateCost(resolution: String, quality: String): Double {
        val resMultiplier = when (resolution) {
            "1K" -> 1.0; "2K" -> 1.5; "4K" -> 2.5; else -> 1.0
        }
        val qualityMultiplier = when (quality) {
            "low" -> 0.7; "medium" -> 1.0; "high" -> 1.3; else -> 1.0
        }
        return baseCost * resMultiplier * qualityMultiplier
    }
}

// ═══════════════════════════════════════════════════
//  Video Models
// ═══════════════════════════════════════════════════

enum class VideoAspectRatio(val displayName: String, val apiValue: String) {
    RATIO_AUTO("Auto", "auto"),
    RATIO_16_9("16:9", "16:9"),
    RATIO_4_3("4:3", "4:3"),
    RATIO_1_1("1:1", "1:1"),
    RATIO_3_4("3:4", "3:4"),
    RATIO_9_16("9:16", "9:16"),
    RATIO_21_9("21:9", "21:9"),
}

enum class VideoResolution(val displayName: String, val apiValue: String) {
    RES_480P("480p", "480p"),
    RES_720P("720p", "720p"),
    RES_NATIVE_1080P("原生1080p", "native1080p"),
    RES_1080P("1080p", "1080p"),
    RES_2K("2K", "2k"),
    RES_4K("4K", "4k"),
}

enum class VideoDuration(val displayName: String, val seconds: Int) {
    DURATION_5S("5秒", 5),
    DURATION_10S("10秒", 10),
}

enum class VideoApiTier { S, G, KLING }

enum class VideoModel(
    val displayName: String,
    val iconChar: String,
    val apiValue: String,
    val apiTier: VideoApiTier,
    val defaultAspectRatio: VideoAspectRatio,
    val defaultResolution: VideoResolution,
    val defaultDuration: VideoDuration,
    val supportedResolutions: Set<VideoResolution>,
    val supportedRatios: Set<VideoAspectRatio>,
    val supportedDurations: Set<VideoDuration>,
    val supportsImageToVideo: Boolean = true,
    val supportsGenerateAudio: Boolean = false,
    val supportsRealistic: Boolean = false,
    val baseCost: Double,
) {
    SEEDANCE_2(
        displayName = "Seedance2.0",
        iconChar = "🎬",  // 🎬
        apiValue = "seedance2",
        apiTier = VideoApiTier.S,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_720P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_480P, VideoResolution.RES_720P, VideoResolution.RES_1080P),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = VideoDuration.entries.toSet(),
        supportsImageToVideo = true,
        supportsGenerateAudio = true,
        supportsRealistic = false,
        baseCost = 6.0,
    ),
    SEEDANCE_2_FAST(
        displayName = "Seedance2.0-Fast",
        iconChar = "⚡",  // ⚡
        apiValue = "seedance2-fast",
        apiTier = VideoApiTier.S,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_720P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_480P, VideoResolution.RES_720P),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = setOf(VideoDuration.DURATION_5S),
        supportsImageToVideo = true,
        supportsGenerateAudio = false,
        supportsRealistic = false,
        baseCost = 3.0,
    ),
    WANXIANG_2_6(
        displayName = "万相2.6",
        iconChar = "🎞️",  // 🎞️
        apiValue = "wanxiang2.6",
        apiTier = VideoApiTier.G,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_720P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_480P, VideoResolution.RES_720P, VideoResolution.RES_1080P),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = VideoDuration.entries.toSet(),
        supportsImageToVideo = true,
        supportsGenerateAudio = false,
        supportsRealistic = false,
        baseCost = 6.0,
    ),
    WANXIANG_2_7(
        displayName = "万相2.7",
        iconChar = "🌟",  // 🌟
        apiValue = "wanxiang2.7",
        apiTier = VideoApiTier.G,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_720P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_480P, VideoResolution.RES_720P, VideoResolution.RES_1080P, VideoResolution.RES_2K),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = VideoDuration.entries.toSet(),
        supportsImageToVideo = true,
        supportsGenerateAudio = false,
        supportsRealistic = false,
        baseCost = 8.0,
    ),
    KLING_O1(
        displayName = "可灵O1",
        iconChar = "🎥",  // 🎥
        apiValue = "kling-o1",
        apiTier = VideoApiTier.KLING,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_720P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_720P, VideoResolution.RES_1080P),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = VideoDuration.entries.toSet(),
        supportsImageToVideo = true,
        supportsGenerateAudio = false,
        supportsRealistic = false,
        baseCost = 10.0,
    ),
    KLING_O3_4K(
        displayName = "可灵O3-4k",
        iconChar = "🔮",  // 🔮
        apiValue = "kling-o3-4k",
        apiTier = VideoApiTier.KLING,
        defaultAspectRatio = VideoAspectRatio.RATIO_16_9,
        defaultResolution = VideoResolution.RES_1080P,
        defaultDuration = VideoDuration.DURATION_5S,
        supportedResolutions = setOf(VideoResolution.RES_1080P, VideoResolution.RES_4K),
        supportedRatios = VideoAspectRatio.entries.toSet(),
        supportedDurations = VideoDuration.entries.toSet(),
        supportsImageToVideo = true,
        supportsGenerateAudio = false,
        supportsRealistic = false,
        baseCost = 20.0,
    ),
    ;

    fun estimateCost(resolution: String, duration: Int, generateAudio: Boolean): Double {
        val durationMultiplier = duration.toDouble() / 5.0
        val resMultiplier = when (resolution) {
            "480p" -> 0.7; "720p" -> 1.0; "native1080p", "1080p" -> 1.3
            "2k" -> 1.6; "4k" -> 2.2; else -> 1.0
        }
        val audioMultiplier = if (generateAudio) 1.15 else 1.0
        return baseCost * durationMultiplier * resMultiplier * audioMultiplier
    }

    companion object {
        fun fromApiValue(value: String): VideoModel =
            entries.find { it.apiValue == value } ?: SEEDANCE_2
    }
}

// ═══════════════════════════════════════════════════
//  Config & State
// ═══════════════════════════════════════════════════

data class ImageConfig(
    val prompt: String = "",
    val model: ImageModel = ImageModel.ALL_POWER_IMAGE_G2,
    val aspectRatio: ImageAspectRatio = ImageAspectRatio.RATIO_16_9,
    val resolution: ImageResolution = ImageResolution.RES_1K,
    val quality: ImageQuality = ImageQuality.QUALITY_MEDIUM,
    val count: Int = 1,
    val seed: Int? = null,
    val mediaReferences: List<MediaReference> = emptyList(),
) {
    val estimatedCost: Double
        get() = model.estimateCost(resolution.apiValue, quality.apiValue) * count

    val promptCharCount: Int get() = prompt.length
    val promptOverLimit: Boolean get() = prompt.length > MAX_PROMPT_CHARS
    val promptNearLimit: Boolean get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

data class VideoConfig(
    val prompt: String = "",
    val model: VideoModel = VideoModel.SEEDANCE_2,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.RATIO_16_9,
    val resolution: VideoResolution = VideoResolution.RES_720P,
    val duration: VideoDuration = VideoDuration.DURATION_5S,
    val realisticMode: Boolean = false,
    val generateAudio: Boolean = false,
    val count: Int = 1,
    val seed: Int? = null,
    val mediaReferences: List<MediaReference> = emptyList(),
) {
    val estimatedCost: Double
        get() = model.estimateCost(resolution.apiValue, duration.seconds, generateAudio) * count

    val promptCharCount: Int get() = prompt.length
    val promptOverLimit: Boolean get() = prompt.length > MAX_PROMPT_CHARS
    val promptNearLimit: Boolean get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

data class QuickCreateUiState(
    val currentMode: QuickCreateMode = QuickCreateMode.CREATION,
    val currentTab: QuickCreateTab = QuickCreateTab.IMAGE,
    val imageConfig: ImageConfig = ImageConfig(),
    val videoConfig: VideoConfig = VideoConfig(),
    val taskStatus: QuickCreateTaskUiStatus = QuickCreateTaskUiStatus.IDLE,
    val statusText: String? = null,
    val results: List<QuickCreateResultUi> = emptyList(),
    val error: String? = null,
    val estimatedCost: Double = 0.0,
    val feePreviewLoading: Boolean = false,
    val feePreviewError: String? = null,
    val activeSheet: QuickCreateSheet? = null,
    val inspirationLoading: Boolean = false,
    val inspirationTags: List<QuickCreateInspirationTagUi> = emptyList(),
    val inspirationTemplates: List<QuickCreateInspirationTemplateUi> = emptyList(),
    val inspirationTemplatesLoadingMore: Boolean = false,
    val inspirationTemplatesPage: Int = 0,
    val inspirationTemplatesHasMore: Boolean = false,
    val historyLoading: Boolean = false,
    val historyLoadingMore: Boolean = false,
    val historyPage: Int = 0,
    val historyTotal: Int = 0,
    val historyHasMore: Boolean = false,
    val historyItems: List<QuickCreateHistoryUiItem> = emptyList(),
    val historyCancellingTaskIds: Set<String> = emptySet(),
    val historyDetailLoading: Boolean = false,
    val selectedHistoryDetail: QuickCreateHistoryDetailUiItem? = null,
    val projectsLoading: Boolean = false,
    val projectsLoadingMore: Boolean = false,
    val projects: List<QuickCreateProjectUiItem> = emptyList(),
    val projectsPage: Int = 0,
    val projectsHasMore: Boolean = false,
    val selectedProjectId: String? = null,
    val projectTasksLoading: Boolean = false,
    val projectPinningIds: Set<String> = emptySet(),
    val projectMutatingIds: Set<String> = emptySet(),
    val projectDetailLoading: Boolean = false,
    val selectedProjectDetail: QuickCreateProjectDetailUiItem? = null,
    val serviceModelsLoading: Boolean = false,
    val serviceImageModels: List<QuickCreationServiceModel> = emptyList(),
    val serviceVideoModels: List<QuickCreationServiceModel> = emptyList(),
    val selectedImageServiceModel: QuickCreationServiceModel? = null,
    val selectedVideoServiceModel: QuickCreationServiceModel? = null,
    val serviceImageModelItems: List<QuickCreateServiceModelUi> = emptyList(),
    val serviceVideoModelItems: List<QuickCreateServiceModelUi> = emptyList(),
    val selectedImageServiceModelUi: QuickCreateServiceModelUi? = null,
    val selectedVideoServiceModelUi: QuickCreateServiceModelUi? = null,
    val imageServiceParams: Map<String, String> = emptyMap(),
    val videoServiceParams: Map<String, String> = emptyMap(),
    val draftData: DraftData? = null,
) {
    val showCreationInput: Boolean
        get() = currentMode == QuickCreateMode.CREATION

    val hasDraft: Boolean
        get() = draftData != null
}

enum class QuickCreateTaskUiStatus {
    IDLE, SUBMITTING, QUEUING, RUNNING, SUCCESS, FAILED,
}

/**
 * 快捷创作结果在 Presentation 层使用的媒体类型。
 *
 * 该类型把服务端输出的格式字符串和 URL 兼容判断收敛到状态映射阶段，Composable 只消费稳定枚举，
 * 不再直接理解 `.mp4`、`.webm` 等服务端或文件协议细节。
 */
enum class QuickCreateResultMediaType {
    /** 图片结果，页面使用图片组件按原始宽高自适应展示。 */
    IMAGE,

    /** 视频结果，页面使用视频缩略图组件并保持固定视频比例展示。 */
    VIDEO,
}

/**
 * 快捷创作生成结果的可渲染 UI 模型。
 *
 * 该模型由 [QuickCreateTaskPollingController] 在任务成功后从 Domain 结果映射得到。
 * 它只承载页面展示所需的稳定信息，不包含 Repository、网络响应对象或可变加载状态。
 *
 * @property url 结果媒体的远程访问地址，来自服务端任务输出。
 * 空字符串不应进入该模型；如果服务端缺失地址，应在映射阶段过滤或作为失败处理。
 * @property type 服务端返回的原始输出类型字符串，例如 `png`、`mp4`、`video`。
 * 该字段仅用于详情辅助展示和排错，不应由 Composable 再次解析业务含义。
 * @property mediaType 映射后的稳定媒体类型。
 * 图片结果使用 [QuickCreateResultMediaType.IMAGE]，视频结果使用 [QuickCreateResultMediaType.VIDEO]；
 * 当服务端类型未知时，映射层按 URL 后缀做兼容兜底，仍无法识别时降级为图片。
 * @property thumbnailUrl 服务端返回的视频或图片缩略图地址。
 * `null` 表示没有独立缩略图，展示层可直接使用 [url] 或默认占位。
 * @property width 服务端返回的媒体宽度，单位为像素。
 * `null` 表示服务端未提供尺寸；非空时必须为正数，当前 UI 不依赖该值做强制布局。
 * @property height 服务端返回的媒体高度，单位为像素。
 * `null` 表示服务端未提供尺寸；非空时必须为正数，并应与 [width] 同时出现。
 * @property duration 视频结果时长，单位为秒。
 * `null` 表示图片结果或服务端未提供时长；图片结果即使该值非空也不会按视频渲染。
 */
data class QuickCreateResultUi(
    val url: String,
    val type: String,
    val mediaType: QuickCreateResultMediaType,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)
