package com.runninghub.feature.quickcreate.presentation.generation

import com.runninghub.feature.quickcreate.presentation.editor.quickCreationGlobalMediaReferences
import com.runninghub.feature.quickcreate.presentation.editor.quickCreationRelevantMediaReferences

import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState

import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceSchema
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceValidationIssue
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadedMedia
import com.runninghub.feature.quickcreate.domain.VideoGenerationRequest
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.MAX_PROMPT_CHARS
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.UploadStatus
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.ImageConfig
import com.runninghub.feature.quickcreate.presentation.editor.VideoConfig

/**
 * 当前快捷创作生成请求的构建结果。
 *
 * 该结果属于 Presentation 层的请求适配边界：它把页面状态能否进入正式生成流程表达为稳定类型，
 * 让生成 Interactor 不再重复理解当前 Tab、prompt 限制、服务字段校验和上传字段校验。
 */
sealed interface QuickCreateGenerationRequestBuildResult {
    /**
     * 图片生成请求已经构建完成，可以交给 Repository 提交。
     *
     * @property request 已合并内置参数、服务字段参数和上传列表参数的图片生成请求。
     */
    data class ImageReady(
        val request: ImageGenerationRequest,
    ) : QuickCreateGenerationRequestBuildResult

    /**
     * 视频生成请求已经构建完成，可以交给 Repository 提交。
     *
     * @property request 已合并内置参数、服务字段参数和上传列表参数的视频生成请求。
     */
    data class VideoReady(
        val request: VideoGenerationRequest,
    ) : QuickCreateGenerationRequestBuildResult

    /**
     * 当前输入不能提交生成。
     *
     * @property reason 面向快捷创作页面展示的稳定阻塞原因。
     * 该字段不直接保存中文 UI 文案；应用壳或统一文案端口负责把稳定原因映射为最终展示文本。
     */
    data class Blocked(
        val reason: QuickCreateGenerationBlockReason,
    ) : QuickCreateGenerationRequestBuildResult

    /**
     * 当前状态无法构建请求，但也没有明确可展示的业务错误。
     *
     * 该状态用于防御异常 Tab 或模型状态组合；调用方通常保持空闲状态而不发起 Repository 调用。
     */
    data object Unavailable : QuickCreateGenerationRequestBuildResult
}

/**
 * 快捷创作生成前同步校验失败的稳定原因。
 *
 * 请求构建器只负责判断当前状态为何不能生成，不直接返回最终中文文案。
 * 服务端模型字段校验由 [QuickCreationServiceSchema] 返回稳定 issue，
 * 本类型只保存业务原因和运行时约束值，不保存最终中文 UI 文案。
 */
sealed interface QuickCreateGenerationBlockReason {
    /** 描述词为空或只包含空白字符，当前请求不能提交。 */
    data object PromptRequired : QuickCreateGenerationBlockReason

    /**
     * 描述词超过当前允许上限。
     *
     * @property maxChars 允许的最大字符数，单位为 Kotlin 字符数量；当前由 [MAX_PROMPT_CHARS] 提供。
     */
    data class PromptTooLong(
        val maxChars: Int,
    ) : QuickCreateGenerationBlockReason

    /**
     * 动态服务字段或上传字段校验失败。
     *
     * @property issue Domain 层返回的稳定校验失败语义，包含服务端字段标题和约束数值。
     */
    data class ServiceValidation(
        val issue: QuickCreationServiceValidationIssue,
    ) : QuickCreateGenerationBlockReason
}

/**
 * 构建快捷创作生成请求并校验服务字段。
 *
 * 该类只处理纯数据转换：从 [QuickCreateUiState] 提取当前 Tab 的模型、参数和已上传素材，
 * 生成 Repository 所需的请求对象，并返回字段/上传校验错误。它不启动协程、不读写页面状态、
 * 不触发网络请求，便于后续把生成提交和计费预览继续拆成独立 Interactor。
 */
class QuickCreateGenerationRequestFactory {
    /**
     * 构建当前 Tab 的正式生成请求。
     *
     * 本入口集中处理正式生成前的同步校验：服务字段、prompt 必填/长度，以及可选的上传字段校验。
     * 上传字段校验通常应在上传等待完成后开启，避免正在上传的有效素材被误判为空。
     *
     * @param state 当前页面状态。
     * @param validateUploads 是否同时校验当前 Tab 的动态上传字段。
     * @return 请求可提交时返回对应 Ready 结果；业务输入不合法时返回 [QuickCreateGenerationRequestBuildResult.Blocked]。
     */
    fun buildCurrentGenerationRequest(
        state: QuickCreateUiState,
        validateUploads: Boolean,
    ): QuickCreateGenerationRequestBuildResult {
        validateCurrentServiceFields(state)?.let { error ->
            return QuickCreateGenerationRequestBuildResult.Blocked(
                QuickCreateGenerationBlockReason.ServiceValidation(error),
            )
        }
        currentPromptError(state)?.let { error ->
            return QuickCreateGenerationRequestBuildResult.Blocked(error)
        }
        if (validateUploads) {
            validateCurrentServiceUploads(state)?.let { error ->
                return QuickCreateGenerationRequestBuildResult.Blocked(
                    QuickCreateGenerationBlockReason.ServiceValidation(error),
                )
            }
        }

        return when (state.currentTab) {
            QuickCreateTab.IMAGE -> buildImageGenerationRequest(state, requirePrompt = true)
                ?.let { QuickCreateGenerationRequestBuildResult.ImageReady(it) }
                ?: QuickCreateGenerationRequestBuildResult.Unavailable
            QuickCreateTab.VIDEO -> buildVideoGenerationRequest(state, requirePrompt = true)
                ?.let { QuickCreateGenerationRequestBuildResult.VideoReady(it) }
                ?: QuickCreateGenerationRequestBuildResult.Unavailable
        }
    }

    /**
     * 构建图片生成请求。
     *
     * @param state 当前页面状态。
     * @param requirePrompt 是否要求 prompt 非空；计费预览和正式提交通常需要传入 `true`。
     * @return 当前 Tab 不是图片、prompt 无效或超长时返回 null，否则返回完整领域请求。
     */
    fun buildImageGenerationRequest(
        state: QuickCreateUiState,
        requirePrompt: Boolean,
    ): ImageGenerationRequest? {
        if (state.currentTab != QuickCreateTab.IMAGE) return null
        val config = state.imageConfig
        val prompt = config.prompt.trim()
        if ((requirePrompt && prompt.isEmpty()) || config.promptOverLimit) return null
        val globalMediaReferences = config.mediaReferences.quickCreationGlobalMediaReferences()
        val imageRef = globalMediaReferences
            .filter { it.type == QuickCreateMediaType.IMAGE && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }

        return ImageGenerationRequest(
            prompt = prompt,
            model = config.model.apiValue,
            aspectRatio = config.aspectRatio.apiValue,
            resolution = config.resolution.apiValue,
            quality = config.quality.apiValue,
            referenceImageUri = imageRef?.remoteUrl,
            numImages = config.count,
            seed = config.seed,
            quickCreationCategoryId = state.selectedImageServiceModel?.categoryId,
            quickCreationBindingId = state.selectedImageServiceModel?.bindingId,
            quickCreationSkuId = state.selectedImageServiceModel?.skuId,
            quickCreationParams = imageQuickCreationParams(
                model = state.selectedImageServiceModel,
                config = config,
                serviceParams = state.imageServiceParams,
            ),
            quickCreationListParams = imageQuickCreationListParams(
                model = state.selectedImageServiceModel,
                config = config,
                serviceParams = state.imageServiceParams,
            ),
        )
    }

    /**
     * 构建视频生成请求。
     *
     * 视频请求同时收集全局图片、视频和音频素材；字段绑定素材会进入
     * quickCreationListParams，不会作为全局参考素材提交。
     */
    fun buildVideoGenerationRequest(
        state: QuickCreateUiState,
        requirePrompt: Boolean,
    ): VideoGenerationRequest? {
        if (state.currentTab != QuickCreateTab.VIDEO) return null
        val config = state.videoConfig
        val prompt = config.prompt.trim()
        if ((requirePrompt && prompt.isEmpty()) || config.promptOverLimit) return null
        val globalMediaReferences = config.mediaReferences.quickCreationGlobalMediaReferences()
        val imageRef = globalMediaReferences
            .filter { it.type == QuickCreateMediaType.IMAGE && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }
        val videoRef = globalMediaReferences
            .filter { it.type == QuickCreateMediaType.VIDEO && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }
        val audioRef = globalMediaReferences
            .filter { it.type == QuickCreateMediaType.AUDIO && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }

        return VideoGenerationRequest(
            prompt = prompt,
            model = config.model.apiValue,
            aspectRatio = config.aspectRatio.apiValue,
            duration = config.duration.seconds,
            resolution = config.resolution.apiValue,
            referenceImageUri = imageRef?.remoteUrl,
            referenceVideoUri = videoRef?.remoteUrl,
            referenceAudioUri = audioRef?.remoteUrl,
            realistic = config.realisticMode,
            generateAudio = config.generateAudio,
            numVideos = config.count,
            seed = config.seed,
            quickCreationCategoryId = state.selectedVideoServiceModel?.categoryId,
            quickCreationBindingId = state.selectedVideoServiceModel?.bindingId,
            quickCreationSkuId = state.selectedVideoServiceModel?.skuId,
            quickCreationParams = videoQuickCreationParams(
                model = state.selectedVideoServiceModel,
                config = config,
                serviceParams = state.videoServiceParams,
            ),
            quickCreationListParams = videoQuickCreationListParams(
                model = state.selectedVideoServiceModel,
                config = config,
                serviceParams = state.videoServiceParams,
            ),
        )
    }

    /**
     * 校验当前 Tab 的服务字段。
     *
     * @return 校验失败时返回稳定 issue；全部通过时返回 null。
     */
    fun validateCurrentServiceFields(state: QuickCreateUiState): QuickCreationServiceValidationIssue? =
        when (state.currentTab) {
            QuickCreateTab.IMAGE -> validateServiceFields(
                model = state.selectedImageServiceModel,
                serviceParams = state.imageServiceParams,
            )
            QuickCreateTab.VIDEO -> validateServiceFields(
                model = state.selectedVideoServiceModel,
                serviceParams = state.videoServiceParams,
            )
        }

    /**
     * 校验当前 Tab 的服务上传字段。
     *
     * 隐藏字段和未激活 child 字段不会参与校验，避免不可见参数阻塞生成。
     */
    fun validateCurrentServiceUploads(state: QuickCreateUiState): QuickCreationServiceValidationIssue? =
        when (state.currentTab) {
            QuickCreateTab.IMAGE -> validateServiceUploads(
                model = state.selectedImageServiceModel,
                mediaReferences = state.imageConfig.mediaReferences,
                fallbackMediaType = QuickCreateMediaType.IMAGE,
                serviceParams = state.imageServiceParams,
            )
            QuickCreateTab.VIDEO -> validateServiceUploads(
                model = state.selectedVideoServiceModel,
                mediaReferences = state.videoConfig.mediaReferences,
                fallbackMediaType = null,
                serviceParams = state.videoServiceParams,
            )
        }

    /**
     * 返回当前 Tab 会影响请求或计费预览的素材引用。
     *
     * 全局素材始终相关；绑定到隐藏字段或未激活 child 字段的素材会被排除。
     */
    fun currentRelevantMediaReferences(state: QuickCreateUiState): List<MediaReference> =
        when (state.currentTab) {
            QuickCreateTab.IMAGE -> state.imageConfig.mediaReferences.quickCreationRelevantMediaReferences(
                activeFieldParamKeys = QuickCreationServiceSchema.activeUploadParamKeys(
                    model = state.selectedImageServiceModel,
                    serviceParams = state.imageServiceParams,
                ),
            )
            QuickCreateTab.VIDEO -> state.videoConfig.mediaReferences.quickCreationRelevantMediaReferences(
                activeFieldParamKeys = QuickCreationServiceSchema.activeUploadParamKeys(
                    model = state.selectedVideoServiceModel,
                    serviceParams = state.videoServiceParams,
                ),
            )
        }

    private fun currentPromptError(state: QuickCreateUiState): QuickCreateGenerationBlockReason? {
        val prompt = when (state.currentTab) {
            QuickCreateTab.IMAGE -> state.imageConfig.prompt.trim()
            QuickCreateTab.VIDEO -> state.videoConfig.prompt.trim()
        }
        val overLimit = when (state.currentTab) {
            QuickCreateTab.IMAGE -> state.imageConfig.promptOverLimit
            QuickCreateTab.VIDEO -> state.videoConfig.promptOverLimit
        }
        return when {
            prompt.isEmpty() -> QuickCreateGenerationBlockReason.PromptRequired
            overLimit -> QuickCreateGenerationBlockReason.PromptTooLong(MAX_PROMPT_CHARS)
            else -> null
        }
    }
}

private fun imageQuickCreationParams(
    model: QuickCreationServiceModel?,
    config: ImageConfig,
    serviceParams: Map<String, String>,
): Map<String, String> =
    buildMap {
        put("aspectRatio", config.aspectRatio.apiValue)
        put("resolution", config.resolution.apiValue)
        put("quality", config.quality.apiValue)

        putAll(QuickCreationServiceSchema.defaultParams(model, serviceParams))
        putAll(
            serviceParams
                .filterKeys { key -> key in QuickCreationServiceSchema.activeParamKeys(model, serviceParams) }
                .filterValues { it.isNotBlank() }
        )
    }

private fun imageQuickCreationListParams(
    model: QuickCreationServiceModel?,
    config: ImageConfig,
    serviceParams: Map<String, String>,
): Map<String, List<String>> =
    quickCreationListParams(
        model = model,
        mediaReferences = config.mediaReferences,
        fallbackMediaType = QuickCreateMediaType.IMAGE,
        serviceParams = serviceParams,
    )

private fun videoQuickCreationParams(
    model: QuickCreationServiceModel?,
    config: VideoConfig,
    serviceParams: Map<String, String>,
): Map<String, String> =
    buildMap {
        put("aspectRatio", config.aspectRatio.apiValue)
        put("resolution", config.resolution.apiValue)
        put("duration", config.duration.seconds.toString())

        putAll(QuickCreationServiceSchema.defaultParams(model, serviceParams))
        putAll(
            serviceParams
                .filterKeys { key -> key in QuickCreationServiceSchema.activeParamKeys(model, serviceParams) }
                .filterValues { it.isNotBlank() }
        )
    }

private fun videoQuickCreationListParams(
    model: QuickCreationServiceModel?,
    config: VideoConfig,
    serviceParams: Map<String, String>,
): Map<String, List<String>> =
    quickCreationListParams(
        model = model,
        mediaReferences = config.mediaReferences,
        fallbackMediaType = null,
        serviceParams = serviceParams,
    )

private fun quickCreationListParams(
    model: QuickCreationServiceModel?,
    mediaReferences: List<MediaReference>,
    fallbackMediaType: QuickCreateMediaType?,
    serviceParams: Map<String, String>,
): Map<String, List<String>> {
    return QuickCreationServiceSchema.listParams(
        model = model,
        uploadedMedia = mediaReferences.toQuickCreationUploadedMedia(),
        fallbackMediaKind = fallbackMediaType?.toQuickCreationUploadMediaKind(),
        serviceParams = serviceParams,
    )
}

private fun validateServiceFields(
    model: QuickCreationServiceModel?,
    serviceParams: Map<String, String>,
): QuickCreationServiceValidationIssue? =
    QuickCreationServiceSchema.validateFields(model = model, serviceParams = serviceParams)

private fun validateServiceUploads(
    model: QuickCreationServiceModel?,
    mediaReferences: List<MediaReference>,
    fallbackMediaType: QuickCreateMediaType?,
    serviceParams: Map<String, String>,
): QuickCreationServiceValidationIssue? =
    QuickCreationServiceSchema.validateUploads(
        model = model,
        uploadedMedia = mediaReferences.toQuickCreationUploadedMedia(),
        fallbackMediaKind = fallbackMediaType?.toQuickCreationUploadMediaKind(),
        serviceParams = serviceParams,
    )

private fun List<MediaReference>.toQuickCreationUploadedMedia(): List<QuickCreationUploadedMedia> =
    filter { it.uploadStatus == UploadStatus.DONE }
        .mapNotNull { reference ->
            val remoteUrl = reference.remoteUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            QuickCreationUploadedMedia(
                mediaKind = reference.type.toQuickCreationUploadMediaKind(),
                remoteUrl = remoteUrl,
                fieldParamKey = reference.fieldParamKey,
            )
        }

private fun QuickCreateMediaType.toQuickCreationUploadMediaKind(): QuickCreationUploadMediaKind =
    when (this) {
        QuickCreateMediaType.IMAGE -> QuickCreationUploadMediaKind.IMAGE
        QuickCreateMediaType.VIDEO -> QuickCreationUploadMediaKind.VIDEO
        QuickCreateMediaType.AUDIO -> QuickCreationUploadMediaKind.AUDIO
    }
