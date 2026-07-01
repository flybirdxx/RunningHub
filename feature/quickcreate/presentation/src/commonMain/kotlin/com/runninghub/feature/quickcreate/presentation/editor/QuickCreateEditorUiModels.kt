package com.runninghub.feature.quickcreate.presentation.editor

import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.state.MAX_VISIBLE_CHARS_WARN
import com.runninghub.feature.quickcreate.presentation.QuickCreateUiMessage

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
 * @property errorMessage 上传失败或本地媒体读取失败后的展示消息语义；`null` 表示当前素材没有失败原因。
 * 新增上传错误必须使用稳定语义，不得保存最终中文文案或本地文件名。
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
    val errorMessage: QuickCreateUiMessage? = null,
)

/**
 * 图片生成内置宽高比选项。
 *
 * @property displayName 页面展示文案；该值为比例协议文本，不承载本地化中文内容。
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
 * @property displayName 页面展示文案；该值为分辨率规格文本，不承载本地化中文内容。
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
 * @property label 页面展示语义；最终文案由 composeApp 使用 Compose Resources 映射。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class ImageQuality(val label: ImageQualityLabel, val apiValue: String) {
    QUALITY_LOW(ImageQualityLabel.Low, "low"),
    QUALITY_MEDIUM(ImageQualityLabel.Medium, "medium"),
    QUALITY_HIGH(ImageQualityLabel.High, "high"),
}

/**
 * 图片质量选项的稳定展示语义。
 *
 * 旧接口仍使用 [ImageQuality.apiValue] 提交 `low/medium/high`；本类型只用于 UI 边界映射本地文案，
 * 避免 feature presentation 模块保存“低/中/高”这类固定中文 UI 字符串。
 */
sealed interface ImageQualityLabel {
    /** 低质量档位，通常用于更低成本或更快生成。 */
    data object Low : ImageQualityLabel

    /** 中质量档位，是当前本地兼容模型的默认选择。 */
    data object Medium : ImageQualityLabel

    /** 高质量档位，通常对应更高成本或更精细输出。 */
    data object High : ImageQualityLabel
}

/**
 * 快捷创作迁移期保留的本地图片模型枚举。
 *
 * 服务端模型目录已经由 Domain Repository 提供；该枚举只用于旧参数入口和本地默认值。
 * 最终计费必须以后端 fee preview 为准。
 */
enum class ImageModel(
    val label: ImageModelLabel,
    val apiValue: String,
    val defaultAspectRatio: ImageAspectRatio,
    val defaultResolution: ImageResolution,
    val defaultQuality: ImageQuality,
    val supportedResolutions: Set<ImageResolution>,
    val supportedRatios: Set<ImageAspectRatio>,
    val supportedQualities: Set<ImageQuality>,
    val supportsImageToImage: Boolean = true,
) {
    ALL_POWER_IMAGE_G2(
        label = ImageModelLabel.AllPowerImageG2,
        apiValue = "all-power-image-g2",
        defaultAspectRatio = ImageAspectRatio.RATIO_16_9,
        defaultResolution = ImageResolution.RES_1K,
        defaultQuality = ImageQuality.QUALITY_MEDIUM,
        supportedResolutions = ImageResolution.entries.toSet(),
        supportedRatios = ImageAspectRatio.entries.toSet(),
        supportedQualities = ImageQuality.entries.toSet(),
        supportsImageToImage = true,
    ),
    SEEDREAM_5(
        label = ImageModelLabel.RuntimeName("Seedream 5.0"),
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
    ),
    SEEDREAM_4(
        label = ImageModelLabel.RuntimeName("Seedream 4.0"),
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
    ),
    ;
}

/**
 * 图片本地兼容模型的稳定展示语义。
 *
 * 该类型只服务于迁移期本地兼容模型入口；服务端模型名称仍来自
 * `QuickCreationServiceModel.name` 运行时数据。固定中文名称交给 composeApp 资源层映射，
 * 避免 Presentation 枚举保存最终本地化文案。
 */
sealed interface ImageModelLabel {
    /** 全能图片 G-2.0 本地兼容模型。 */
    data object AllPowerImageG2 : ImageModelLabel

    /**
     * 不含本地化语义的模型商品名或英文名。
     *
     * @property value 可直接展示的运行时模型名称；空字符串不应传入。
     */
    data class RuntimeName(val value: String) : ImageModelLabel
}

/**
 * 视频生成内置宽高比选项。
 *
 * @property displayName 页面展示文案；`Auto` 和比例值是旧接口兼容文本，不承载本地化中文内容。
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
 * @property label 页面展示语义；最终文案由 composeApp 使用 Compose Resources 映射。
 * @property apiValue 兼容旧生成接口的请求参数值。
 */
enum class VideoResolution(val label: VideoResolutionLabel, val apiValue: String) {
    RES_480P(VideoResolutionLabel.Standard("480p"), "480p"),
    RES_720P(VideoResolutionLabel.Standard("720p"), "720p"),
    RES_NATIVE_1080P(VideoResolutionLabel.Native1080p, "native1080p"),
    RES_1080P(VideoResolutionLabel.Standard("1080p"), "1080p"),
    RES_2K(VideoResolutionLabel.Standard("2K"), "2k"),
    RES_4K(VideoResolutionLabel.Standard("4K"), "4k"),
}

/**
 * 视频分辨率选项的稳定展示语义。
 *
 * 大部分分辨率标签与旧接口参数相近，可以作为运行时标准文本透传；“原生1080p”属于固定中文 UI
 * 文案，因此单独建模并交给 composeApp 资源层映射，避免 Presentation 枚举继续保存本地化文本。
 */
sealed interface VideoResolutionLabel {
    /**
     * 标准分辨率文本。
     *
     * @property value 不含本地化语义的技术规格标签，例如 `720p`、`2K` 或 `4K`；空字符串不应传入。
     */
    data class Standard(val value: String) : VideoResolutionLabel

    /** 原生 1080p 档位，最终展示文案由应用资源层决定。 */
    data object Native1080p : VideoResolutionLabel
}

/**
 * 视频生成内置时长选项。
 *
 * @property label 页面展示语义；最终文案由 composeApp 使用 Compose Resources 映射。
 * @property seconds 请求接口使用的秒数。
 */
enum class VideoDuration(val label: VideoDurationLabel, val seconds: Int) {
    DURATION_5S(VideoDurationLabel.Seconds5, 5),
    DURATION_10S(VideoDurationLabel.Seconds10, 10),
}

/**
 * 视频时长选项的稳定展示语义。
 *
 * [VideoDuration.seconds] 继续作为旧接口请求值；本类型只描述 UI 展示档位，
 * 让“5秒/10秒”这类中文固定文案在应用资源层集中维护。
 */
sealed interface VideoDurationLabel {
    /** 5 秒视频生成档位。 */
    data object Seconds5 : VideoDurationLabel

    /** 10 秒视频生成档位。 */
    data object Seconds10 : VideoDurationLabel
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
 * 服务端模型目录已经由 Domain Repository 提供；该枚举只用于旧参数入口和本地默认值。
 * 最终计费必须以后端 fee preview 为准。
 */
enum class VideoModel(
    val label: VideoModelLabel,
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
) {
    SEEDANCE_2(
        label = VideoModelLabel.RuntimeName("Seedance2.0"),
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
    ),
    SEEDANCE_2_FAST(
        label = VideoModelLabel.RuntimeName("Seedance2.0-Fast"),
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
    ),
    WANXIANG_2_6(
        label = VideoModelLabel.Wanxiang26,
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
    ),
    WANXIANG_2_7(
        label = VideoModelLabel.Wanxiang27,
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
    ),
    KLING_O1(
        label = VideoModelLabel.KlingO1,
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
    ),
    KLING_O3_4K(
        label = VideoModelLabel.KlingO34k,
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
    ),
    ;

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
 * 视频本地兼容模型的稳定展示语义。
 *
 * 该类型只用于旧参数入口和本地兼容模型列表。固定中文模型名由 composeApp 资源层映射；
 * 英文商品名作为运行时文本保留，避免无意义地资源化第三方模型品牌名。
 */
sealed interface VideoModelLabel {
    /**
     * 不含本地化语义的模型商品名或英文名。
     *
     * @property value 可直接展示的运行时模型名称；空字符串不应传入。
     */
    data class RuntimeName(val value: String) : VideoModelLabel

    /** 万相 2.6 本地兼容模型。 */
    data object Wanxiang26 : VideoModelLabel

    /** 万相 2.7 本地兼容模型。 */
    data object Wanxiang27 : VideoModelLabel

    /** 可灵 O1 本地兼容模型。 */
    data object KlingO1 : VideoModelLabel

    /** 可灵 O3 4K 本地兼容模型。 */
    data object KlingO34k : VideoModelLabel
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
