package com.runninghub.shared.domain.repository

import kotlinx.coroutines.flow.Flow

// ── 图片模型枚举 ────────────────────────────────────────

enum class ImageModel(
    val displayName: String,
    val apiTier: String,
    val apiEndpoint: String,
    val modelKey: String,
    val description: String,
) {
    // 全能图片 G 系列
    ALL_POWER_IMAGE_G_2_OFFICIAL(
        displayName = "全能图片G-2.0-官方版",
        apiTier = "G",
        apiEndpoint = "/openapi/v2/rhart-image-g-2-official",
        modelKey = "all-power-image-g2",
        description = "顶级图像生成底座，支持任意分辨率，革命性文本渲染能力"
    ),
    ALL_POWER_IMAGE_G_2_CHEAP(
        displayName = "全能图片G-2.0-低价版",
        apiTier = "G",
        apiEndpoint = "/openapi/v2/rhart-image-g-2",
        modelKey = "all-power-image-g2",
        description = "全能图片G-2.0的低价渠道版，价格更优但不稳定"
    ),
    // 全能图片 X 系列
    ALL_POWER_IMAGE_X_OFFICIAL(
        displayName = "全能图片X-官方版",
        apiTier = "X",
        apiEndpoint = "/openapi/v2/rhart-image-x-official",
        modelKey = "all-power-image-x-official",
        description = "全能图片X文生图官方稳定版，精准文本解析"
    ),
    ALL_POWER_IMAGE_X_CHEAP(
        displayName = "全能图片X-低价版",
        apiTier = "X",
        apiEndpoint = "/openapi/v2/rhart-image-x",
        modelKey = "all-power-image-x",
        description = "全能图片X低价渠道版，价格更优但不稳定"
    ),
    // 全能图片 Pro 系列
    ALL_POWER_IMAGE_PRO_OFFICIAL(
        displayName = "全能图片Pro-官方版",
        apiTier = "PRO",
        apiEndpoint = "/openapi/v2/rhart-image-pro-official",
        modelKey = "all-power-image-pro-official",
        description = "专业级图像编辑，支持4K超清画质输出"
    ),
    ALL_POWER_IMAGE_PRO_CHEAP(
        displayName = "全能图片Pro-低价版",
        apiTier = "PRO",
        apiEndpoint = "/openapi/v2/rhart-image-n-pro",
        modelKey = "all-power-image-pro",
        description = "全能图片Pro低价渠道版"
    ),
    // 全能图片 2.0 系列
    ALL_POWER_IMAGE_2_OFFICIAL(
        displayName = "全能图片2.0-官方版",
        apiTier = "V2",
        apiEndpoint = "/openapi/v2/rhart-image-v2-official",
        modelKey = "all-power-image-v2-official",
        description = "全能图片V2官方稳定版，4K超高清画质"
    ),
    ALL_POWER_IMAGE_2_CHEAP(
        displayName = "全能图片2.0-低价版",
        apiTier = "V2",
        apiEndpoint = "/openapi/v2/rhart-image-n-g31-flash",
        modelKey = "all-power-image-v2",
        description = "全能图片2.0低价渠道版"
    ),
    // Seedream 系列
    SEEDREAM_5_0_LITE(
        displayName = "Seedream 5.0 Lite",
        apiTier = "SEEDREAM5",
        apiEndpoint = "seedream-v5-lite",
        modelKey = "seedream5",
        description = "豆包大模型新一代智能视觉创作引擎，支持文生组图"
    ),
    SEEDREAM_4_0(
        displayName = "Seedream 4.0",
        apiTier = "SEEDREAM4",
        apiEndpoint = "seedream-v4",
        modelKey = "seedream4",
        description = "字节跳动专业级布局感知型文生图模型"
    ),
}

// ── 视频模型枚举 ────────────────────────────────────────

enum class VideoModel(
    val displayName: String,
    val apiTier: String,
    val apiCategory: String,
    val modelKey: String,
    val description: String,
    val supportsTextToVideo: Boolean,
    val supportsImageToVideo: Boolean,
    val supportsFirstLastFrame: Boolean,
) {
    // HappyHorse
    HAPPYHORSE(
        displayName = "HappyHorse",
        apiTier = "HH",
        apiCategory = "alibaba/happyhorse-1.0",
        modelKey = "happyhorse",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // Seedance 2.0 系列
    SEEDANCE_2_0(
        displayName = "Seedance2.0",
        apiTier = "SD2",
        apiCategory = "rhart-video/sparkvideo-2.0",
        modelKey = "seedance2",
        description = "字节跳动顶级图生视频，追求最高生成品质",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    SEEDANCE_2_0_FAST(
        displayName = "Seedance2.0-Fast",
        apiTier = "SD2F",
        apiCategory = "rhart-video/sparkvideo-2.0-fast",
        modelKey = "seedance2-fast",
        description = "Seedance2.0极速版，更注重生成速度与性价比",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 可灵 3.0 系列
    KLING_3_0_4K(
        displayName = "可灵3.0-4k",
        apiTier = "KL3",
        apiCategory = "kling-video-o3-4k",
        modelKey = "kling-o3-4k",
        description = "快手4K级图生视频，影院级画质",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O3_PRO(
        displayName = "可灵03-Pro",
        apiTier = "KLO3P",
        apiCategory = "kling-video-o3-pro",
        modelKey = "kling-o3-pro",
        description = "可灵O3系列顶级画质，专业创作首选",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O3_STD(
        displayName = "可灵03-Std",
        apiTier = "KLO3S",
        apiCategory = "kling-video-o3-std",
        modelKey = "kling-o3-std",
        description = "可灵O3系列高性价比旗舰方案",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    KLING_O1(
        displayName = "可灵01",
        apiTier = "KLO1",
        apiCategory = "kling-video-o1",
        modelKey = "kling-o1",
        description = "可灵o1统一多模态视频生成引擎",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 万相 2.7
    WAN_2_7(
        displayName = "万相2.7",
        apiTier = "WAN27",
        apiCategory = "alibaba/wan-2.7",
        modelKey = "wanxiang2.7",
        description = "阿里通义万相2.7，先进文本/图像生成视频模型",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 万相 2.6
    WAN_2_6(
        displayName = "万相2.6",
        apiTier = "WAN26",
        apiCategory = "alibaba/wan-2.6",
        modelKey = "wanxiang2.6",
        description = "阿里通义万相2.6，专业级电影级视频生成",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // PixVerse V6
    PIXVERSE_V6(
        displayName = "PixVerseV6",
        apiTier = "PX6",
        apiCategory = "pixverse-v6",
        modelKey = "pixverse-v6",
        description = "PixVerse第六代图生视频，支持Thinking模式",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // 全能视频 V-Pro 系列
    ALL_POWER_VIDEO_V_PRO_OFFICIAL(
        displayName = "全能视频V-Pro-官方版",
        apiTier = "VP",
        apiCategory = "rhart-video-v3.1-pro",
        modelKey = "all-power-video-v-pro",
        description = "全能视频V3.1专业版，支持4K高清和参考生视频",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    ALL_POWER_VIDEO_V_PRO_CHEAP(
        displayName = "全能视频V-Pro-低价版",
        apiTier = "VPC",
        apiCategory = "rhart-video-v3.1-pro",
        modelKey = "all-power-video-v-pro-cheap",
        description = "全能视频V3.1专业版低价渠道",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 全能视频 V-Fast 系列
    ALL_POWER_VIDEO_V_FAST_OFFICIAL(
        displayName = "全能视频V-Fast-官方版",
        apiTier = "VFP",
        apiCategory = "rhart-video-v3.1-fast",
        modelKey = "all-power-video-v-fast",
        description = "全能视频V3.1极速版，生成速度快30%",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    ALL_POWER_VIDEO_V_FAST_CHEAP(
        displayName = "全能视频V-Fast-低价版",
        apiTier = "VFPC",
        apiCategory = "rhart-video-v3.1-fast",
        modelKey = "all-power-video-v-fast-cheap",
        description = "全能视频V3.1极速版低价渠道",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    // 全能视频 X 系列
    ALL_POWER_VIDEO_X_OFFICIAL(
        displayName = "全能视频X-官方版",
        apiTier = "VX",
        apiCategory = "rhart-video-g-official",
        modelKey = "all-power-video-x",
        description = "全能视频X官方稳定版，主体身份绝对锁定",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    ALL_POWER_VIDEO_X_CHEAP(
        displayName = "全能视频X-低价版",
        apiTier = "VXC",
        apiCategory = "rhart-video-g",
        modelKey = "all-power-video-x-cheap",
        description = "全能视频X低价渠道版",
        supportsTextToVideo = false,
        supportsImageToVideo = true,
        supportsFirstLastFrame = false,
    ),
    // Vidu Q3 系列
    VIDU_Q3_PRO(
        displayName = "ViduQ3-Pro",
        apiTier = "VQ3P",
        apiCategory = "vidu",
        modelKey = "vidu-q3-pro",
        description = "Vidu Q3专业版，音视频一体化叙事",
        supportsTextToVideo = true,
        supportsImageToVideo = true,
        supportsFirstLastFrame = true,
    ),
    VIDU_Q3_PRO_FAST(
        displayName = "ViduQ3-Pro-Fast",
        apiTier = "VQ3PF",
        apiCategory = "vidu",
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
)

data class VideoGenerationRequest(
    val prompt: String,
    val model: String,
    val apiTier: String = "S",
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

interface QuickCreateRepository {
    fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus>
    fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus>
    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String>
}
