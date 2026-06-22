package com.runninghub.feature.quickcreate.presentation.editor

import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.state.MAX_VISIBLE_CHARS_WARN

/**
 * 快捷创作素材在 Presentation 层使用的媒体类型。
 *
 * 该类型只描述用户上传或模板带入素材的展示与请求规划类型，不等同于服务端文件扩展名。
 */
enum class QuickCreateMediaType {
    /** 图片素材，用于图片参考图、视频首帧或动态图片上传字段。 */
    IMAGE,

    /** 视频素材，用于视频参考、视频模板素材或动态视频上传字段。 */
    VIDEO,

    /** 音频素材，用于视频配音或动态音频上传字段。 */
    AUDIO,
}

/**
 * 上传素材在页面状态中的生命周期状态。
 *
 * 上传流程由 feature presentation 的上传协调器驱动，平台 URI 读取由应用壳适配后注入；
 * 该枚举作为稳定 UI 契约，方便编辑器、请求构建和计费预览共享同一状态含义。
 */
enum class UploadStatus {
    /** 本地文件正在读取或上传到远端。 */
    UPLOADING,

    /** 远端已经接收文件，正在做转码、审核或其他服务端处理。 */
    PROCESSING,

    /** 远端地址已经可用，可以进入计费预览或生成请求。 */
    DONE,

    /** 上传或处理失败，素材不能参与当前请求。 */
    FAILED,
}

/**
 * 快捷创作编辑器中的素材引用。
 *
 * 该模型保存用户选择的本地素材、上传进度和远端地址。`uri` 采用字符串而不是平台 Uri，
 * 是为了确保 `commonMain` 状态不泄漏 Android 或 iOS 类型。
 *
 * @property id 客户端生成的稳定 ID，用于列表 key、上传回写和删除操作。
 * @property type 素材媒体类型，决定可匹配的上传字段和请求参数。
 * @property uri 本地或模板素材 URI 字符串；Android/iOS 平台解析由应用壳注入的媒体读取端口完成。
 * @property displayName 页面展示的文件名或模板素材名。
 * @property fileSizeBytes 本地文件大小，单位为字节；模板远端素材未知时可为 0。
 * @property fieldParamKey 绑定的动态上传字段参数名；`null` 或空字符串表示全局素材。
 * @property durationSeconds 音视频时长，单位为秒；`null` 表示未知或非音视频素材。
 * @property uploadStatus 当前上传生命周期状态。
 * @property uploadProgress 上传进度，范围通常为 0.0 到 1.0；失败时保留最后进度用于 UI 反馈。
 * @property remoteUrl 远端可访问地址；只有 [UploadStatus.DONE] 且非空时才应参与生成请求。
 * @property errorMessage 上传失败或本地媒体读取失败后的展示文案；`null` 表示当前素材没有失败原因。
 */
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
    val errorMessage: String? = null,
)

/**
 * 图片生成内置宽高比选项。
 *
 * @property displayName 页面展示文案。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class ImageAspectRatio(val displayName: String, val apiValue: String) {
    RATIO_16_9("16:9", "16:9"),
    RATIO_4_3("4:3", "4:3"),
    RATIO_1_1("1:1", "1:1"),
    RATIO_3_4("3:4", "3:4"),
    RATIO_9_16("9:16", "9:16"),
    RATIO_21_9("21:9", "21:9"),
}

/**
 * 图片生成内置分辨率选项。
 *
 * @property displayName 页面展示文案。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class ImageResolution(val displayName: String, val apiValue: String) {
    RES_1K("1K", "1K"),
    RES_2K("2K", "2K"),
    RES_4K("4K", "4K"),
}

/**
 * 图片生成内置质量选项。
 *
 * @property displayName 页面展示文案。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class ImageQuality(val displayName: String, val apiValue: String) {
    QUALITY_LOW("低", "low"),
    QUALITY_MEDIUM("中", "medium"),
    QUALITY_HIGH("高", "high"),
}

/**
 * 快捷创作迁移期保留的本地图片模型枚举。
 *
 * 服务端模型目录已经由 Domain Repository 提供；该枚举只用于旧参数入口、本地默认值和本地价格预估兜底。
 * 最终计费必须以后端 fee preview 为准。
 */
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

    /**
     * 估算当前内置模型参数的本地价格。
     *
     * 这是离线兜底展示值，只用于 fee preview 返回前的旧 UI 兼容；实际扣费必须以后端预览结果为准。
     */
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

/**
 * 视频生成内置宽高比选项。
 *
 * @property displayName 页面展示文案。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class VideoAspectRatio(val displayName: String, val apiValue: String) {
    RATIO_AUTO("Auto", "auto"),
    RATIO_16_9("16:9", "16:9"),
    RATIO_4_3("4:3", "4:3"),
    RATIO_1_1("1:1", "1:1"),
    RATIO_3_4("3:4", "3:4"),
    RATIO_9_16("9:16", "9:16"),
    RATIO_21_9("21:9", "21:9"),
}

/**
 * 视频生成内置分辨率选项。
 *
 * @property displayName 页面展示文案。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class VideoResolution(val displayName: String, val apiValue: String) {
    RES_480P("480p", "480p"),
    RES_720P("720p", "720p"),
    RES_NATIVE_1080P("原生1080p", "native1080p"),
    RES_1080P("1080p", "1080p"),
    RES_2K("2K", "2k"),
    RES_4K("4K", "4k"),
}

/**
 * 视频生成内置时长选项。
 *
 * @property displayName 页面展示文案。
 * @property seconds 请求接口使用的秒数。
 */
enum class VideoDuration(val displayName: String, val seconds: Int) {
    DURATION_5S("5秒", 5),
    DURATION_10S("10秒", 10),
}

/**
 * 视频模型旧接口的计费/路由分组。
 */
enum class VideoApiTier {
    /** Seedance 系列旧接口分组。 */
    S,

    /** 万相系列旧接口分组。 */
    G,

    /** 可灵系列旧接口分组。 */
    KLING,
}

/**
 * 快捷创作迁移期保留的本地视频模型枚举。
 *
 * 服务端模型目录已经由 Domain Repository 提供；该枚举只用于旧参数入口、本地默认值和本地价格预估兜底。
 * 最终计费必须以后端 fee preview 为准。
 */
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
        iconChar = "🎬",
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
        iconChar = "⚡",
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
        iconChar = "🎞️",
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
        iconChar = "🌟",
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
        iconChar = "🎥",
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
        iconChar = "🔮",
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

    /**
     * 估算当前内置视频模型参数的本地价格。
     *
     * 这是离线兜底展示值，只用于 fee preview 返回前的旧 UI 兼容；实际扣费必须以后端预览结果为准。
     */
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
        /**
         * 根据旧接口模型值恢复本地视频模型。
         *
         * 找不到匹配项时回退到 [SEEDANCE_2]，保持旧草稿和异常服务端数据可继续打开。
         */
        fun fromApiValue(value: String): VideoModel =
            entries.find { it.apiValue == value } ?: SEEDANCE_2
    }
}

/**
 * 图片创作编辑器配置。
 *
 * @property prompt 用户输入的描述词。
 * @property model 当前本地兼容图片模型。
 * @property aspectRatio 图片宽高比。
 * @property resolution 图片分辨率。
 * @property quality 图片质量。
 * @property count 单次生成数量。
 * @property seed 随机种子；`null` 表示由服务端自动生成。
 * @property mediaReferences 当前图片 Tab 的素材引用集合。
 */
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
    /**
     * 本地估算价格。
     *
     * 该值只用于 fee preview 返回前的占位展示；实际计费以远端预览接口结果为准。
     */
    val estimatedCost: Double
        get() = model.estimateCost(resolution.apiValue, quality.apiValue) * count

    /** 当前描述词字符数，用于 UI 字数提示。 */
    val promptCharCount: Int get() = prompt.length

    /** `true` 表示描述词已经超过生成请求允许的最大长度。 */
    val promptOverLimit: Boolean get() = prompt.length > MAX_PROMPT_CHARS

    /** `true` 表示描述词接近最大长度，应展示弱警告。 */
    val promptNearLimit: Boolean get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

/**
 * 视频创作编辑器配置。
 *
 * @property prompt 用户输入的描述词。
 * @property model 当前本地兼容视频模型。
 * @property aspectRatio 视频宽高比。
 * @property resolution 视频分辨率。
 * @property duration 视频时长。
 * @property realisticMode 是否启用真人/写实模式；仅在模型支持时提交。
 * @property generateAudio 是否生成音频；仅在模型支持时提交。
 * @property count 单次生成数量。
 * @property seed 随机种子；`null` 表示由服务端自动生成。
 * @property mediaReferences 当前视频 Tab 的素材引用集合。
 */
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
    /**
     * 本地估算价格。
     *
     * 该值只用于 fee preview 返回前的占位展示；实际计费以远端预览接口结果为准。
     */
    val estimatedCost: Double
        get() = model.estimateCost(resolution.apiValue, duration.seconds, generateAudio) * count

    /** 当前描述词字符数，用于 UI 字数提示。 */
    val promptCharCount: Int get() = prompt.length

    /** `true` 表示描述词已经超过生成请求允许的最大长度。 */
    val promptOverLimit: Boolean get() = prompt.length > MAX_PROMPT_CHARS

    /** `true` 表示描述词接近最大长度，应展示弱警告。 */
    val promptNearLimit: Boolean get() = prompt.length > MAX_VISIBLE_CHARS_WARN
}

/**
 * 取出未绑定到具体服务字段的全局素材。
 *
 * 全局素材来自旧图片/视频创作入口或未指定字段的上传流程，只能在 Domain 规则确认同类型活跃上传字段唯一时
 * 自动回填，不能在 Presentation 层直接分配给任意字段。
 *
 * @return 字段参数名为空或未设置的素材列表，顺序保持用户上传顺序。
 */
fun List<MediaReference>.quickCreationGlobalMediaReferences(): List<MediaReference> =
    filter { it.fieldParamKey.isNullOrBlank() }

/**
 * 取出已经明确绑定到指定服务字段的素材。
 *
 * 字段绑定素材通常来自动态字段上传控件，生命周期跟随当前服务模型和参数面板。
 * 当字段被隐藏或切换为非活跃状态时，调用方应结合活跃字段集合过滤，避免把旧字段素材提交给新参数组合。
 *
 * @param paramKey 服务字段的标准参数名，必须与 Domain 解析出的字段参数一致。
 * @return 明确绑定到 [paramKey] 的素材列表；没有绑定素材时返回空列表。
 */
fun List<MediaReference>.quickCreationFieldMediaReferences(paramKey: String): List<MediaReference> =
    filter { it.fieldParamKey == paramKey }

/**
 * 保留当前生成请求仍然相关的素材。
 *
 * 全局素材始终保留给后续 Domain 兜底分配；字段绑定素材只有在字段仍处于活跃参数集合中才保留。
 * 这样可以防止用户切换模型、选项或 child 字段后，已失效字段的远程素材继续进入计费预览和生成请求。
 *
 * @param activeFieldParamKeys 当前模型与参数组合下仍然激活的上传字段参数名集合。
 * @return 可继续参与当前请求规划的素材列表。
 */
fun List<MediaReference>.quickCreationRelevantMediaReferences(
    activeFieldParamKeys: Set<String>,
): List<MediaReference> =
    filter { reference ->
        reference.fieldParamKey.isNullOrBlank() || reference.fieldParamKey in activeFieldParamKeys
    }
