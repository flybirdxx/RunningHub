package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 图片创作 ──────────────────────────────────────────

@Serializable
data class ImageGenerationRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspect_ratio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String = "1K",
    @SerialName("quality") val quality: String = "medium",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("seed") val seed: Int? = null,
    @SerialName("num_images") val numImages: Int = 1,
)

@Serializable
data class ImageGenerationResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("estimatedCost") val estimatedCost: Double = 0.0,
)

// ── 视频创作 ──────────────────────────────────────────

@Serializable
data class VideoGenerationRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("model") val model: String,
    @SerialName("aspect_ratio") val aspectRatio: String = "16:9",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("realistic") val realistic: Boolean = false,
    @SerialName("generate_audio") val generateAudio: Boolean = false,
)

@Serializable
data class VideoGenerationResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("estimatedCost") val estimatedCost: Double = 0.0,
)

// ── 任务状态 ──────────────────────────────────────────

@Serializable
data class QuickCreateTaskQueryResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("progress") val progress: Int = 0,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("estimatedCost") val estimatedCost: Double = 0.0,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
)

@Serializable
data class QuickCreateResultDto(
    @SerialName("url") val url: String,
    @SerialName("type") val type: String,
    @SerialName("thumbnailUrl") val thumbnailUrl: String? = null,
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    @SerialName("duration") val duration: Int? = null,
)

object QuickCreateResult {
    const val STATUS_PENDING = "PENDING"
    const val STATUS_QUEUING = "QUEUING"
    const val STATUS_RUNNING = "RUNNING"
    const val STATUS_SUCCESS = "SUCCESS"
    const val STATUS_FAILED = "FAILED"
}

// ── 媒体上传 ──────────────────────────────────────────

@Serializable
data class MediaUploadResponseDto(
    @SerialName("url") val url: String,
    @SerialName("fileId") val fileId: String? = null,
    @SerialName("fileName") val fileName: String? = null,
    @SerialName("fileSize") val fileSize: Long? = null,
)

// ── 费用估算 (静态) ──────────────────────────────────

enum class ImageCostEstimate(val model: String, val costPerImage: Double) {
    ALL_POWER_IMAGE("全能图片G-2.0", 0.93),
    SEEDREAM_5("Seedream 5.0", 1.50),
    SEEDREAM_4("Seedream 4.0", 0.80),
    ;

    companion object {
        fun estimate(aspectRatio: String, resolution: String, quality: String): Double {
            val base = entries.firstOrNull()?.costPerImage ?: 0.93
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
}

enum class VideoCostEstimate(val model: String, val apiValue: String) {
    SEEDANCE_2("Seedance2.0", "seedance2"),
    SEEDANCE_2_FAST("Seedance2.0-Fast", "seedance2-fast"),
    WANXIANG_2_6("万相2.6", "wanxiang2.6"),
    WANXIANG_2_7("万相2.7", "wanxiang2.7"),
    KLING_O1("可灵O1", "kling-o1"),
    KLING_O3("可灵O3-4k", "kling-o3-4k"),
    ;

    fun estimateCost(resolution: String, duration: Int, generateAudio: Boolean): Double {
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

    companion object {
        fun fromApiValue(value: String): VideoCostEstimate {
            return entries.find { it.apiValue == value } ?: SEEDANCE_2
        }
    }
}
