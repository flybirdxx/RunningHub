package com.runninghub.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 基础响应 (所有模型共用) ─────────────────────────────

@Serializable
data class QuickCreateTaskQueryResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("progress") val progress: Int = 0,
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("estimatedCost") val estimatedCost: Double = 0.0,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
    @SerialName("failedReason") val failedReason: Map<String, String>? = null,
    @SerialName("usage") val usage: TaskUsageDto? = null,
) : TaskResponse

interface TaskResponse {
    val taskId: String
    val status: String
    val errorCode: String
    val errorMessage: String?
}

@Serializable
data class QuickCreateResultDto(
    @SerialName("url") val url: String,
    @SerialName("type") val type: String? = null,
    @SerialName("outputType") val outputType: String? = null,
    @SerialName("text") val text: String? = null,
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

// ── 图片创作 ──────────────────────────────────────────

@Serializable
data class ImageGenerationRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
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

// ── 图片模型专用 DTO ──────────────────────────────────

// Seedream v5-lite - 文生图
// 官方规范：prompt 必填；width/height 可选；resolution 可选（优先级高于 width×height）
// sequentialImageGeneration 控制文生组图；maxImages 最大生成数量；toolsType 默认 web_search
@Serializable
data class SeedreamV5LiteTextToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("width") val width: Int = 2048,
    @SerialName("height") val height: Int = 2048,
    @SerialName("sequentialImageGeneration") val sequentialImageGeneration: String = "disabled",
    @SerialName("maxImages") val maxImages: Int = 1,
    @SerialName("toolsType") val toolsType: String = "web_search",
    @SerialName("resolution") val resolution: String? = null,
)

@Serializable
data class SeedreamV5LiteTextToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// Seedream v5-lite - 图生图
// 官方规范：prompt 必填，imageUrls 数组必填（最多10张）；其余可选
@Serializable
data class SeedreamV5LiteImageToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("width") val width: Int = 2048,
    @SerialName("height") val height: Int = 2048,
    @SerialName("sequentialImageGeneration") val sequentialImageGeneration: String = "disabled",
    @SerialName("maxImages") val maxImages: Int = 1,
    @SerialName("resolution") val resolution: String? = null,
)

@Serializable
data class SeedreamV5LiteImageToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// Seedream v4 - 图生图
// 官方规范：prompt 必填，imageUrls 数组必填（最多10张）；width/height 可选；resolution 可选
@Serializable
data class SeedreamV4ImageToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("width") val width: Int = 2048,
    @SerialName("height") val height: Int = 2048,
    @SerialName("sequentialImageGeneration") val sequentialImageGeneration: String = "disabled",
    @SerialName("maxImages") val maxImages: Int = 1,
    @SerialName("resolution") val resolution: String? = null,
)

@Serializable
data class SeedreamV4ImageToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 G-2 系列 - 文生图
// 官方规范：prompt/aspectRatio/resolution/quality 均为必填
@Serializable
data class AllPowerImageG2TextToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String,
    @SerialName("resolution") val resolution: String,
    @SerialName("quality") val quality: String,
)

@Serializable
data class AllPowerImageG2TextToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 G-2 系列 - 图生图
// 官方规范：imageUrls(数组，最多10张)/resolution/quality 必填；prompt 可选
@Serializable
data class AllPowerImageG2ImageToImageRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String,
    @SerialName("quality") val quality: String,
)

@Serializable
data class AllPowerImageG2ImageToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 X - 文生图
// 官方规范：prompt 必填，outputFormat 必填，aspectRatio 可选
// 注意：无 resolution/negative_prompt/batch_count/seed 等字段
@Serializable
data class AllPowerImageXTextToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String = "1:1",
    @SerialName("outputFormat") val outputFormat: String,
)

@Serializable
data class AllPowerImageXTextToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 X - 图生图（官方规范待确认，此处预留）
@Serializable
data class AllPowerImageXImageToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("aspectRatio") val aspectRatio: String = "1:1",
)

@Serializable
data class AllPowerImageXImageToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 V2/PRO 系列 - 文生图
// ⚠️ 官方文档尚未确认 V2/PRO 文生图参数，此处为推测
// 待官方确认后可删除 batchCount/seed，或确认无此参数时移除
@Serializable
data class AllPowerImageV2ProTextToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String = "1K",
    @SerialName("batchCount") val batchCount: Int = 1,
    @SerialName("seed") val seed: Int? = null,
)

@Serializable
data class AllPowerImageV2ProTextToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// 全能图片 V2/PRO 系列 - 图生图
// 官方规范：imageUrls 数组必填（最多14张），resolution 可选（无默认值）
@Serializable
data class AllPowerImageV2ProImageToImageRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String? = null,
)

@Serializable
data class AllPowerImageV2ProImageToImageResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
)

// ── 视频创作 ──────────────────────────────────────────

@Serializable
data class VideoGenerationRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("model") val model: String,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("realistic") val realistic: Boolean = false,
    @SerialName("generateAudio") val generateAudio: Boolean = false,
)

@Serializable
data class VideoGenerationResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("estimatedCost") val estimatedCost: Double = 0.0,
)

// ── 视频模型专用 DTO ─────────────────────────────────

// HappyHorse 文生视频
@Serializable
data class HappyHorseTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "1080p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("seed") val seed: Int? = null,
)

@Serializable
data class HappyHorseTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

@Serializable
data class HappyHorseImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("resolution") val resolution: String = "1080p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
)

// Seedance 2.0 / 2.0-Fast 文生视频
@Serializable
data class SeedanceTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("generateAudio") val generateAudio: Boolean = true,
    @SerialName("ratio") val ratio: String = "adaptive",
    @SerialName("webSearch") val webSearch: Boolean = false,
    @SerialName("returnLastFrame") val returnLastFrame: Boolean = false,
)

@Serializable
data class SeedanceTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// Seedance 2.0 / 2.0-Fast 图生视频
@Serializable
data class SeedanceImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("firstFrameUrl") val firstFrameUrl: String,
    @SerialName("lastFrameUrl") val lastFrameUrl: String? = null,
    @SerialName("generateAudio") val generateAudio: Boolean = true,
    @SerialName("ratio") val ratio: String = "adaptive",
    @SerialName("realPersonMode") val realPersonMode: Boolean = true,
    @SerialName("conversionSlots") val conversionSlots: List<String>? = null,
    @SerialName("returnLastFrame") val returnLastFrame: Boolean = false,
)

@Serializable
data class SeedanceImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O1 文生视频
@Serializable
data class KlingO1TextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("generateAudio") val generateAudio: Boolean = false,
    @SerialName("negativePrompt") val negativePrompt: String? = null,
)

@Serializable
data class KlingO1TextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O1 图生视频
@Serializable
data class KlingO1ImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("generateAudio") val generateAudio: Boolean = false,
)

@Serializable
data class KlingO1ImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
)

// 可灵 O3-4K 图生视频
@Serializable
data class KlingO34KImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("lastImageUrl") val lastImageUrl: String? = null,
    @SerialName("duration") val duration: Int = 5,
    @SerialName("sound") val sound: Boolean = false,
    @SerialName("shotType") val shotType: String = "customize",
    @SerialName("multiPrompt") val multiPrompt: List<String>? = null,
    @SerialName("elementList") val elementList: List<String>? = null,
)

@Serializable
data class KlingO34KImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O3-Pro 文生视频
@Serializable
data class KlingO3ProTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = true,
)

@Serializable
data class KlingO3ProTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O3-Pro 图生视频
@Serializable
data class KlingO3ProImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("lastImageUrl") val lastImageUrl: String? = null,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = false,
)

@Serializable
data class KlingO3ProImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O3-Std 文生视频
@Serializable
data class KlingO3StdTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = true,
)

@Serializable
data class KlingO3StdTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 可灵 O3-Std 图生视频
@Serializable
data class KlingO3StdImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("lastImageUrl") val lastImageUrl: String? = null,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = false,
)

@Serializable
data class KlingO3StdImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 万相 2.7 文生视频
// 官方规范：prompt/duration/resolution/aspectRatio 必填
// promptExtend 默认 true，negativePrompt/audioUrl/seed 可选
@Serializable
data class Wan27TextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("duration") val duration: String,
    @SerialName("resolution") val resolution: String,
    @SerialName("aspectRatio") val aspectRatio: String,
    @SerialName("negativePrompt") val negativePrompt: String? = null,
    @SerialName("audioUrl") val audioUrl: String? = null,
    @SerialName("promptExtend") val promptExtend: Boolean = true,
    @SerialName("seed") val seed: Int? = null,
)

@Serializable
data class Wan27TextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 万相 2.7 图生视频
// 官方规范：firstImageUrl/resolution/duration 必填；prompt 可选
// promptExtend 默认 true，negativePrompt/audioUrl/lastImageUrl/seed 可选
@Serializable
data class Wan27ImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("lastImageUrl") val lastImageUrl: String? = null,
    @SerialName("audioUrl") val audioUrl: String? = null,
    @SerialName("negativePrompt") val negativePrompt: String? = null,
    @SerialName("resolution") val resolution: String,
    @SerialName("duration") val duration: String,
    @SerialName("promptExtend") val promptExtend: Boolean = true,
    @SerialName("seed") val seed: Int? = null,
)

@Serializable
data class Wan27ImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 万相 2.6 图生视频
// 官方规范待确认，此处基于代码结构修正字段命名
@Serializable
data class Wan26ImageToVideoRequestDto(
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("resolution") val resolution: String = "1080p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("shotType") val shotType: String = "single",
)

@Serializable
data class Wan26ImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// PixVerse V6 文生视频
@Serializable
data class PixVerseV6TextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("generate_audio_switch") val generateAudioSwitch: Boolean = true,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
)

@Serializable
data class PixVerseV6TextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// PixVerse V6 图生视频
@Serializable
data class PixVerseV6ImageToVideoRequestDto(
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("generate_audio_switch") val generateAudioSwitch: Boolean = true,
)

@Serializable
data class PixVerseV6ImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 X 文生视频 (官方稳定版)
@Serializable
data class AllPowerVideoXTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 6,
)

@Serializable
data class AllPowerVideoXTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 X 图生视频 (官方稳定版)
@Serializable
data class AllPowerVideoXImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: String = "6",
)

@Serializable
data class AllPowerVideoXImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
)

// 全能视频 X 图生视频 (低价版) - 使用 imageUrls (多图)
@Serializable
data class AllPowerVideoXCheapImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("imageUrls") val imageUrls: List<String>,
    @SerialName("resolution") val resolution: String = "480p",
    @SerialName("duration") val duration: Int = 6,
)

@Serializable
data class AllPowerVideoXCheapImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 V3.1 文生视频 (Fast 官方)
@Serializable
data class AllPowerVideoV31FastTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("generateAudio") val generateAudio: Boolean = true,
)

@Serializable
data class AllPowerVideoV31FastTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 V3.1 图生视频 (Fast 官方)
@Serializable
data class AllPowerVideoV31FastImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("generateAudio") val generateAudio: Boolean = true,
)

@Serializable
data class AllPowerVideoV31FastImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 V3.1 首尾帧 (Fast 低价)
@Serializable
data class AllPowerVideoV31FastStartEndToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("firstFrameUrl") val firstFrameUrl: String,
    @SerialName("lastFrameUrl") val lastFrameUrl: String? = null,
    @SerialName("aspectRatio") val aspectRatio: String = "9:16",
    @SerialName("duration") val duration: Int = 8,
    @SerialName("resolution") val resolution: String = "720p",
)

@Serializable
data class AllPowerVideoV31FastStartEndToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 V3.1 文生视频 (Pro 官方)
@Serializable
data class AllPowerVideoV31ProTextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("resolution") val resolution: String = "4K",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = true,
)

@Serializable
data class AllPowerVideoV31ProTextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// 全能视频 V3.1 图生视频 (Pro 官方)
@Serializable
data class AllPowerVideoV31ProImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("firstFrameUrl") val firstFrameUrl: String? = null,
    @SerialName("lastFrameUrl") val lastFrameUrl: String? = null,
    @SerialName("resolution") val resolution: String = "4K",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("audio") val audio: Boolean = true,
)

@Serializable
data class AllPowerVideoV31ProImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// Vidu Q3 文生视频
@Serializable
data class ViduQ3TextToVideoRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("style") val style: String = "general",
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("audio") val audio: Boolean = true,
)

@Serializable
data class ViduQ3TextToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// Vidu Q3 图生视频
@Serializable
data class ViduQ3ImageToVideoRequestDto(
    @SerialName("prompt") val prompt: String? = null,
    @SerialName("firstImageUrl") val firstImageUrl: String,
    @SerialName("lastImageUrl") val lastImageUrl: String? = null,
    @SerialName("style") val style: String = "general",
    @SerialName("aspectRatio") val aspectRatio: String = "16:9",
    @SerialName("resolution") val resolution: String = "720p",
    @SerialName("duration") val duration: Int = 5,
    @SerialName("audio") val audio: Boolean = false,
)

@Serializable
data class ViduQ3ImageToVideoResponseDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("status") val status: String,
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String? = null,
    @SerialName("results") val results: List<QuickCreateResultDto>? = null,
    @SerialName("clientId") val clientId: String = "",
    @SerialName("promptTips") val promptTips: String = "",
) : TaskResponse

// ── 媒体上传 ──────────────────────────────────────────

@Serializable
data class MediaUploadDataDto(
    @SerialName("type") val type: String? = null,
    @SerialName("downloadUrl") val downloadUrl: String? = null,
    @SerialName("size") val size: String? = null,
    @SerialName("fileName") val fileName: String? = null,
)

@Serializable
data class MediaUploadResponseDto(
    @SerialName("code") val code: Int = 0,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: MediaUploadDataDto? = null,
) {
    val isSuccess: Boolean get() = code == 0
    val url: String? get() = data?.downloadUrl
}

