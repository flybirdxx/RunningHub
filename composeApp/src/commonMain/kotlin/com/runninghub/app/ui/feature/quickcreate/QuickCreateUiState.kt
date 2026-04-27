package com.runninghub.app.ui.feature.quickcreate

import coil3.Uri

enum class QuickCreateTab(val displayName: String) {
    IMAGE("图片"),
    VIDEO("视频"),
}

const val MAX_PROMPT_CHARS = 500
const val MAX_VISIBLE_CHARS_WARN = 400

enum class QuickCreateMediaType {
    IMAGE,
    VIDEO,
    AUDIO,
}

data class MediaReference(
    val id: String,
    val type: QuickCreateMediaType,
    val uri: String,
    val displayName: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int? = null,
    val uploadStatus: UploadStatus = UploadStatus.UPLOADING,
    val uploadProgress: Float = 0f,
    val remoteUrl: String? = null,
)

enum class UploadStatus {
    UPLOADING,
    PROCESSING,
    DONE,
    FAILED,
}

enum class ImageModel(
    val displayName: String,
    val apiValue: String,
    val defaultAspectRatio: String,
    val defaultResolution: String,
    val defaultQuality: String,
) {
    ALL_POWER_IMAGE_G2(
        displayName = "全能图片 G-2.0",
        apiValue = "all-power-image-g2",
        defaultAspectRatio = "16:9",
        defaultResolution = "1K",
        defaultQuality = "medium",
    ),
    SEEDREAM_5(
        displayName = "Seedream 5.0",
        apiValue = "seedream5",
        defaultAspectRatio = "3:4",
        defaultResolution = "1K",
        defaultQuality = "medium",
    ),
    SEEDREAM_4(
        displayName = "Seedream 4.0",
        apiValue = "seedream4",
        defaultAspectRatio = "3:4",
        defaultResolution = "1K",
        defaultQuality = "medium",
    ),
    ;

    fun estimateCost(resolution: String, quality: String): Double {
        val base = when (this) {
            ALL_POWER_IMAGE_G2 -> 0.93
            SEEDREAM_5 -> 1.50
            SEEDREAM_4 -> 0.80
        }
        val resMultiplier = when (resolution) {
            "1K" -> 1.0
            "2K" -> 1.5
            "4K" -> 2.5
            else -> 1.0
        }
        val qualityMultiplier = when (quality) {
            "low" -> 0.7
            "medium" -> 1.0
            "high" -> 1.3
            else -> 1.0
        }
        return base * resMultiplier * qualityMultiplier
    }
}

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

enum class VideoModel(
    val displayName: String,
    val iconChar: String,
    val apiValue: String,
) {
    SEEDANCE_2("Seedance2.0", "🎬", "seedance2"),
    SEEDANCE_2_FAST("Seedance2.0-Fast", "⚡", "seedance2-fast"),
    WANXIANG_2_6("万相2.6", "🎞️", "wanxiang2.6"),
    WANXIANG_2_7("万相2.7", "🌟", "wanxiang2.7"),
    KLING_O1("可灵O1", "🎥", "kling-o1"),
    KLING_O3_4K("可灵O3-4k", "🔮", "kling-o3-4k"),
    ;

    companion object {
        fun fromApiValue(value: String): VideoModel {
            return entries.find { it.apiValue == value } ?: SEEDANCE_2
        }

        fun estimateCost(
            apiValue: String,
            resolution: String,
            duration: Int,
            generateAudio: Boolean,
        ): Double {
            val base = when (apiValue) {
                "seedance2", "wanxiang2.6" -> 6.0
                "seedance2-fast" -> 3.0
                "wanxiang2.7" -> 8.0
                "kling-o1" -> 10.0
                "kling-o3-4k" -> 20.0
                else -> 6.0
            }
            val durationMultiplier = duration / 5.0
            val resMultiplier = when (resolution) {
                "480p" -> 0.7
                "720p" -> 1.0
                "native1080p", "1080p" -> 1.3
                "2k" -> 1.6
                "4k" -> 2.2
                else -> 1.0
            }
            val audioMultiplier = if (generateAudio) 1.15 else 1.0
            return base * durationMultiplier * resMultiplier * audioMultiplier
        }
    }
}

data class ImageConfig(
    val prompt: String = "",
    val model: ImageModel = ImageModel.ALL_POWER_IMAGE_G2,
    val aspectRatio: ImageAspectRatio = ImageAspectRatio.RATIO_16_9,
    val resolution: ImageResolution = ImageResolution.RES_1K,
    val quality: ImageQuality = ImageQuality.QUALITY_MEDIUM,
    val mediaReferences: List<MediaReference> = emptyList(),
) {
    val estimatedCost: Double
        get() = model.estimateCost(resolution.apiValue, quality.apiValue)

    val promptCharCount: Int
        get() = prompt.length

    val promptOverLimit: Boolean
        get() = prompt.length > MAX_PROMPT_CHARS

    val promptNearLimit: Boolean
        get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

data class VideoConfig(
    val prompt: String = "",
    val model: VideoModel = VideoModel.SEEDANCE_2,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.RATIO_16_9,
    val resolution: VideoResolution = VideoResolution.RES_720P,
    val duration: VideoDuration = VideoDuration.DURATION_5S,
    val realisticMode: Boolean = false,
    val generateAudio: Boolean = false,
    val mediaReferences: List<MediaReference> = emptyList(),
) {
    val estimatedCost: Double
        get() = VideoModel.estimateCost(model.apiValue, resolution.apiValue, duration.seconds, generateAudio)

    val promptCharCount: Int
        get() = prompt.length

    val promptOverLimit: Boolean
        get() = prompt.length > MAX_PROMPT_CHARS

    val promptNearLimit: Boolean
        get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

data class QuickCreateUiState(
    val currentTab: QuickCreateTab = QuickCreateTab.IMAGE,
    val imageConfig: ImageConfig = ImageConfig(),
    val videoConfig: VideoConfig = VideoConfig(),
    val taskStatus: QuickCreateTaskUiStatus = QuickCreateTaskUiStatus.IDLE,
    val statusText: String? = null,
    val results: List<QuickCreateResultUi> = emptyList(),
    val error: String? = null,
    val estimatedCost: Double = 0.0,
    val tuneSheetVisible: Boolean = false,
)

enum class QuickCreateTaskUiStatus {
    IDLE,
    SUBMITTING,
    QUEUING,
    RUNNING,
    SUCCESS,
    FAILED,
}

data class QuickCreateResultUi(
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)
