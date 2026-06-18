package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreateInspirationTag
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplate
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationProject
import com.runninghub.shared.domain.repository.QuickCreationServiceModel

enum class QuickCreateTab(val displayName: String) {
    IMAGE("图片"),
    VIDEO("视频"),
}

enum class QuickCreateMode(val displayName: String) {
    CREATION("创作"),
    INSPIRATION("灵感"),
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
    val tuneSheetVisible: Boolean = false,
    val inspirationLoading: Boolean = false,
    val inspirationTags: List<QuickCreateInspirationTag> = emptyList(),
    val inspirationTemplates: List<QuickCreateInspirationTemplate> = emptyList(),
    val historyLoading: Boolean = false,
    val historyLoadingMore: Boolean = false,
    val historyPage: Int = 0,
    val historyTotal: Int = 0,
    val historyHasMore: Boolean = false,
    val historyItems: List<QuickCreationHistoryItem> = emptyList(),
    val historyCancellingTaskIds: Set<String> = emptySet(),
    val historyDetailLoading: Boolean = false,
    val selectedHistoryDetail: QuickCreationHistoryItem? = null,
    val projectsLoading: Boolean = false,
    val projects: List<QuickCreationProject> = emptyList(),
    val projectsPage: Int = 0,
    val projectsHasMore: Boolean = false,
    val selectedProjectId: String? = null,
    val projectTasksLoading: Boolean = false,
    val projectPinningIds: Set<String> = emptySet(),
    val projectMutatingIds: Set<String> = emptySet(),
    val projectDetailLoading: Boolean = false,
    val selectedProjectDetail: QuickCreationProject? = null,
    val serviceModelsLoading: Boolean = false,
    val serviceImageModels: List<QuickCreationServiceModel> = emptyList(),
    val serviceVideoModels: List<QuickCreationServiceModel> = emptyList(),
    val selectedImageServiceModel: QuickCreationServiceModel? = null,
    val selectedVideoServiceModel: QuickCreationServiceModel? = null,
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

data class QuickCreateResultUi(
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)
