package com.runninghub.feature.quickcreate.presentation.inspiration

import com.runninghub.feature.quickcreate.domain.QuickCreateInspirationTemplateDetail
import com.runninghub.feature.quickcreate.domain.QuickCreationInspirationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceSchema
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceUploadFieldAlias
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationUploadMediaKind
import com.runninghub.feature.quickcreate.presentation.QuickCreatePresentationError
import com.runninghub.feature.quickcreate.presentation.editor.ImageAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.ImageQuality
import com.runninghub.feature.quickcreate.presentation.editor.ImageResolution
import com.runninghub.feature.quickcreate.presentation.editor.MediaReference
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.editor.remoteDoneMediaReference
import com.runninghub.feature.quickcreate.presentation.editor.VideoAspectRatio
import com.runninghub.feature.quickcreate.presentation.editor.VideoDuration
import com.runninghub.feature.quickcreate.presentation.editor.VideoResolution
import com.runninghub.feature.quickcreate.presentation.modelcatalog.toQuickCreateServiceModelUiItems
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.toQuickCreateUiMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val INSPIRATION_TEMPLATE_PAGE_SIZE = 20

/**
 * 管理快捷创作灵感模板区域的状态和模板应用映射。
 *
 * 该类位于 Presentation 层，负责加载灵感标签、模板分页、模板详情，并把模板详情转换为
 * 当前编辑器状态。它不直接触发计费预览、草稿保存或生成提交；这些跨流程副作用由
 * ScreenModel 通过 [onTemplateApplied] 回调协调，避免灵感区域继续了解计费和生成细节。
 *
 * @param inspirationRepository 快捷创作灵感仓库接口，提供灵感标签、模板列表和模板详情能力。
 * @param scope ScreenModel 生命周期作用域，所有模板请求随页面释放而取消。
 * @param uiState 页面状态容器，灵感区域和模板应用后的编辑器状态通过不可变 `copy` 更新。
 * @param onTemplateApplied 模板成功应用后的回调，调用方通常用它触发计费预览。
 * @param pageSize 模板分页大小，单位为条；默认与历史接口行为保持一致。
 */
class QuickCreateInspirationStateHolder(
    private val inspirationRepository: QuickCreationInspirationRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onTemplateApplied: () -> Unit,
    private val pageSize: Int = INSPIRATION_TEMPLATE_PAGE_SIZE,
) {

    /**
     * 加载灵感标签和模板第一页。
     *
     * 切换到灵感模式且本地还没有模板时调用。标签和模板相互独立，任一请求失败都不会清空
     * 另一侧已成功返回的数据；错误信息保留在页面状态中，由 UI 统一展示。
     */
    fun loadInspiration() {
        scope.launch {
            uiState.update { it.copy(inspirationLoading = true, error = null) }

            val tagsResult = inspirationRepository.getInspirationTags()
            val templatesResult = inspirationRepository.getInspirationTemplates(
                page = 1,
                size = pageSize,
            )

            uiState.update { state ->
                val tags = tagsResult.getOrElse { emptyList() }.toQuickCreateInspirationTagUiItems()
                val templatePage = templatesResult.getOrNull()
                val templates = templatePage?.items.orEmpty().map { it.toQuickCreateInspirationTemplateUi() }
                val error = tagsResult.exceptionOrNull()?.toQuickCreateUiMessage(
                    QuickCreatePresentationError.InspirationTagsLoadFailed,
                ) ?: templatesResult.exceptionOrNull()?.toQuickCreateUiMessage(
                    QuickCreatePresentationError.InspirationTemplatesLoadFailed,
                )

                state.copy(
                    inspirationLoading = false,
                    inspirationTags = tags,
                    inspirationTemplates = templates,
                    inspirationTemplatesLoadingMore = false,
                    inspirationTemplatesPage = templatePage?.page ?: 0,
                    inspirationTemplatesHasMore = templatePage?.hasNext ?: false,
                    error = error,
                )
            }
        }
    }

    /**
     * 追加加载下一页灵感模板。
     *
     * 通过 `templateId` 去重，避免用户重复点击加载更多或服务端分页边界变化时出现重复模板卡片。
     */
    fun loadMoreTemplates() {
        val current = uiState.value
        if (
            current.inspirationLoading ||
            current.inspirationTemplatesLoadingMore ||
            !current.inspirationTemplatesHasMore
        ) {
            return
        }

        val nextPage = current.inspirationTemplatesPage + 1
        // 加载更多可能被按钮连点或同一事件帧重复触发，必须先同步占用加载状态。
        uiState.update { it.copy(inspirationTemplatesLoadingMore = true, error = null) }
        scope.launch {
            val result = inspirationRepository.getInspirationTemplates(
                page = nextPage,
                size = pageSize,
            )

            uiState.update { state ->
                result.fold(
                    onSuccess = { nextPageResult ->
                        val loadedTemplates = nextPageResult.items.map { it.toQuickCreateInspirationTemplateUi() }
                        state.copy(
                            inspirationTemplates = (state.inspirationTemplates + loadedTemplates)
                                .distinctBy { it.id },
                            inspirationTemplatesLoadingMore = false,
                            inspirationTemplatesPage = nextPageResult.page,
                            inspirationTemplatesHasMore = nextPageResult.hasNext,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            inspirationTemplatesLoadingMore = false,
                            error = error.toQuickCreateUiMessage(
                                QuickCreatePresentationError.InspirationTemplatesLoadFailed,
                            ),
                        )
                    },
                )
            }
        }
    }

    /**
     * 获取模板详情并应用到当前编辑器。
     *
     * 模板详情会切回创作模式，并根据模板类型写入图片或视频编辑状态。只有成功应用后才触发
     * [onTemplateApplied]，避免失败请求导致计费预览使用旧编辑参数重新计算。
     */
    fun applyTemplate(templateId: String) {
        if (templateId.isBlank()) return
        scope.launch {
            uiState.update { it.copy(inspirationLoading = true, error = null) }
            val result = inspirationRepository.getInspirationTemplateDetail(templateId)
            result.fold(
                onSuccess = { detail ->
                    uiState.update { state ->
                        state.applyTemplateDetail(detail)
                    }
                    onTemplateApplied()
                },
                onFailure = { error ->
                    uiState.update {
                        it.copy(
                            inspirationLoading = false,
                            error = error.toQuickCreateUiMessage(
                                QuickCreatePresentationError.InspirationTemplateDetailLoadFailed,
                            ),
                        )
                    }
                },
            )
        }
    }

    /**
     * 将 Plaza 使用同款意图应用到当前编辑器。
     *
     * 该入口只写入已知可编辑参数和参考素材，不提交生成任务；成功应用后通过 [onTemplateApplied]
     * 触发既有计费预览链路，确保后续生成仍经过价格确认。
     *
     * @param intent Plaza 作品详情转换出的复用意图。
     */
    fun applyPlazaReuseIntent(intent: QuickCreatePlazaReuseIntent) {
        if (intent.sourceWorkId.isBlank()) return
        uiState.update { state -> state.applyPlazaReuseIntent(intent) }
        onTemplateApplied()
    }

    private fun QuickCreateUiState.applyTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val category = detail.categoryId?.uppercase()
        return when (category) {
            "VIDEO" -> applyVideoTemplateDetail(detail)
            else -> applyImageTemplateDetail(detail)
        }
    }

    private fun QuickCreateUiState.applyPlazaReuseIntent(
        intent: QuickCreatePlazaReuseIntent,
    ): QuickCreateUiState =
        when (intent.referenceMediaKind) {
            QuickCreatePlazaReuseMediaKind.VIDEO -> applyVideoPlazaReuseIntent(intent)
            else -> applyImagePlazaReuseIntent(intent)
        }

    private fun QuickCreateUiState.applyImagePlazaReuseIntent(
        intent: QuickCreatePlazaReuseIntent,
    ): QuickCreateUiState {
        val selectedModel = serviceImageModels.matchPlazaReuseIntent(intent) ?: selectedImageServiceModel
        val plazaParams = intent.plazaServiceParams(selectedModel)
        val serviceParams = plazaParams
        val imageModel = imageConfig.model
        val imageItems = serviceImageModels.toQuickCreateServiceModelUiItems(selectedModel)
        val nextConfig = imageConfig.copy(
            prompt = intent.prompt?.takeIf { it.isNotBlank() } ?: imageConfig.prompt,
            aspectRatio = plazaParams["aspectRatio"]?.toImageAspectRatio()
                ?.takeIf { it in imageModel.supportedRatios }
                ?: imageConfig.aspectRatio,
            resolution = plazaParams["resolution"]?.toImageResolution()
                ?.takeIf { it in imageModel.supportedResolutions }
                ?: imageConfig.resolution,
            mediaReferences = intent.toMediaReferences(
                activeUploadAliases = selectedModel?.let { model ->
                    QuickCreationServiceSchema.activeUploadParamAliases(
                        model = model,
                        serviceParams = serviceParams,
                    )
                }.orEmpty(),
            ),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.IMAGE,
            inspirationLoading = false,
            selectedImageServiceModel = selectedModel,
            serviceImageModelItems = imageItems,
            selectedImageServiceModelUi = imageItems.firstOrNull { it.selected },
            imageConfig = nextConfig,
            imageServiceParams = serviceParams,
            estimatedCost = 0.0,
        ).withoutStaleFeePreview()
    }

    private fun QuickCreateUiState.applyVideoPlazaReuseIntent(
        intent: QuickCreatePlazaReuseIntent,
    ): QuickCreateUiState {
        val selectedModel = serviceVideoModels.matchPlazaReuseIntent(intent) ?: selectedVideoServiceModel
        val plazaParams = intent.plazaServiceParams(selectedModel)
        val serviceParams = plazaParams
        val videoModel = videoConfig.model
        val videoItems = serviceVideoModels.toQuickCreateServiceModelUiItems(selectedModel)
        val nextConfig = videoConfig.copy(
            prompt = intent.prompt?.takeIf { it.isNotBlank() } ?: videoConfig.prompt,
            aspectRatio = plazaParams["aspectRatio"]?.toVideoAspectRatio()
                ?.takeIf { it in videoModel.supportedRatios }
                ?: videoConfig.aspectRatio,
            resolution = plazaParams["resolution"]?.toVideoResolution()
                ?.takeIf { it in videoModel.supportedResolutions }
                ?: videoConfig.resolution,
            mediaReferences = intent.toMediaReferences(
                activeUploadAliases = selectedModel?.let { model ->
                    QuickCreationServiceSchema.activeUploadParamAliases(
                        model = model,
                        serviceParams = serviceParams,
                    )
                }.orEmpty(),
            ),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.VIDEO,
            inspirationLoading = false,
            selectedVideoServiceModel = selectedModel,
            serviceVideoModelItems = videoItems,
            selectedVideoServiceModelUi = videoItems.firstOrNull { it.selected },
            videoConfig = nextConfig,
            videoServiceParams = serviceParams,
            estimatedCost = 0.0,
        ).withoutStaleFeePreview()
    }

    private fun QuickCreateUiState.applyImageTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val selectedModel = serviceImageModels.matchTemplateModel(detail) ?: selectedImageServiceModel
        val templateParams = QuickCreationServiceSchema.canonicalParams(selectedModel, detail.params)
        val serviceParams = QuickCreationServiceSchema.defaultParams(selectedModel, templateParams) + templateParams
        val imageModel = imageConfig.model
        val templateAspectRatio = detail.params.templateImageAspectRatio()
            ?.takeIf { it in imageModel.supportedRatios }
        val templateResolution = detail.params.templateImageResolution()
            ?.takeIf { it in imageModel.supportedResolutions }
        val templateQuality = detail.params.templateImageQuality()
            ?.takeIf { it in imageModel.supportedQualities }
        val imageItems = serviceImageModels.toQuickCreateServiceModelUiItems(selectedModel)
        val nextConfig = imageConfig.copy(
            prompt = detail.prompt ?: imageConfig.prompt,
            aspectRatio = templateAspectRatio ?: imageConfig.aspectRatio,
            resolution = templateResolution ?: imageConfig.resolution,
            quality = templateQuality ?: imageConfig.quality,
            mediaReferences = detail.templateMediaReferences(
                activeUploadAliases = QuickCreationServiceSchema.activeUploadParamAliases(
                    model = selectedModel,
                    serviceParams = serviceParams,
                ),
                declaredUploadAliases = QuickCreationServiceSchema.declaredUploadParamAliases(selectedModel),
            ),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.IMAGE,
            inspirationLoading = false,
            selectedImageServiceModel = selectedModel,
            serviceImageModelItems = imageItems,
            selectedImageServiceModelUi = imageItems.firstOrNull { it.selected },
            imageConfig = nextConfig,
            imageServiceParams = serviceParams,
            estimatedCost = 0.0,
        ).withoutStaleFeePreview()
    }

    private fun QuickCreateUiState.applyVideoTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val selectedModel = serviceVideoModels.matchTemplateModel(detail) ?: selectedVideoServiceModel
        val templateParams = QuickCreationServiceSchema.canonicalParams(selectedModel, detail.params)
        val serviceParams = QuickCreationServiceSchema.defaultParams(selectedModel, templateParams) + templateParams
        val videoModel = videoConfig.model
        val templateAspectRatio = detail.params.templateVideoAspectRatio()
            ?.takeIf { it in videoModel.supportedRatios }
        val templateResolution = detail.params.templateVideoResolution()
            ?.takeIf { it in videoModel.supportedResolutions }
        val templateDuration = detail.params.templateVideoDuration()
            ?.takeIf { it in videoModel.supportedDurations }
        val templateGenerateAudio = detail.params.templateBoolean("generateAudio")
            ?: videoConfig.generateAudio
        val templateRealisticMode = detail.params.templateBoolean("realPersonMode")
            ?: videoConfig.realisticMode
        val videoItems = serviceVideoModels.toQuickCreateServiceModelUiItems(selectedModel)
        val nextConfig = videoConfig.copy(
            prompt = detail.prompt ?: videoConfig.prompt,
            aspectRatio = templateAspectRatio ?: videoConfig.aspectRatio,
            resolution = templateResolution ?: videoConfig.resolution,
            duration = templateDuration ?: videoConfig.duration,
            generateAudio = videoModel.supportsGenerateAudio && templateGenerateAudio,
            realisticMode = videoModel.supportsRealistic && templateRealisticMode,
            mediaReferences = detail.templateMediaReferences(
                activeUploadAliases = QuickCreationServiceSchema.activeUploadParamAliases(
                    model = selectedModel,
                    serviceParams = serviceParams,
                ),
                declaredUploadAliases = QuickCreationServiceSchema.declaredUploadParamAliases(selectedModel),
            ),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.VIDEO,
            inspirationLoading = false,
            selectedVideoServiceModel = selectedModel,
            serviceVideoModelItems = videoItems,
            selectedVideoServiceModelUi = videoItems.firstOrNull { it.selected },
            videoConfig = nextConfig,
            videoServiceParams = serviceParams,
            estimatedCost = 0.0,
        ).withoutStaleFeePreview()
    }

    private fun QuickCreateUiState.withoutStaleFeePreview(): QuickCreateUiState =
        copy(
            feePreviewLoading = false,
            feePreviewError = null,
            feePreviewRequestKey = null,
            billingPreview = null,
            estimatedCost = 0.0,
        )

    private fun List<QuickCreationServiceModel>.matchTemplateModel(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreationServiceModel? =
        firstOrNull { model ->
            (detail.bindingId != null && model.bindingId == detail.bindingId) ||
                (detail.skuId != null && model.skuId == detail.skuId)
        }

    private fun List<QuickCreationServiceModel>.matchPlazaReuseIntent(
        intent: QuickCreatePlazaReuseIntent,
    ): QuickCreationServiceModel? =
        firstOrNull { model ->
            (intent.templateId != null && model.bindingId == intent.templateId) ||
                (intent.skuId != null && model.skuId == intent.skuId)
        }

    private fun QuickCreatePlazaReuseIntent.plazaServiceParams(
        model: QuickCreationServiceModel?,
    ): Map<String, String> {
        val rawParams = buildMap {
            aspectRatio?.takeIf { it.isNotBlank() }?.let { put("aspectRatio", it) }
            resolution?.takeIf { it.isNotBlank() }?.let { put("resolution", it) }
        }
        val canonicalParams = QuickCreationServiceSchema.canonicalParams(model, rawParams)
        val activeKeys = QuickCreationServiceSchema.activeParamKeys(model, canonicalParams)
        return canonicalParams.filterKeys { key -> key in activeKeys }
    }

    private fun String.toImageAspectRatio(): ImageAspectRatio? =
        ImageAspectRatio.entries.firstOrNull { it.apiValue.equals(this, ignoreCase = true) }

    private fun String.toVideoAspectRatio(): VideoAspectRatio? =
        VideoAspectRatio.entries.firstOrNull { it.apiValue.equals(this, ignoreCase = true) }

    private fun String.toImageResolution(): ImageResolution? =
        ImageResolution.entries.firstOrNull { it.apiValue.equals(this, ignoreCase = true) }

    private fun String.toVideoResolution(): VideoResolution? =
        VideoResolution.entries.firstOrNull { it.apiValue.equals(this, ignoreCase = true) }

    private fun QuickCreatePlazaReuseIntent.toMediaReferences(
        activeUploadAliases: Map<String, QuickCreationServiceUploadFieldAlias>,
    ): List<MediaReference> {
        val url = referenceMediaUrl?.takeIf { it.isNotBlank() } ?: return emptyList()
        val mediaType = referenceMediaKind?.toQuickCreateMediaType() ?: return emptyList()
        val fieldParamKey = activeUploadAliases.values
            .firstOrNull { alias ->
                alias.mediaKind?.toQuickCreateMediaType() == mediaType
            }
            ?.paramKey
        return listOf(
            remoteDoneMediaReference(
                url = url,
                type = mediaType,
                id = "plaza_${sourceWorkId.plazaReuseReferenceIdPart()}_${mediaType.name}",
                fallbackDisplayName = sourceWorkId,
                fieldParamKey = fieldParamKey,
            ),
        )
    }

    private fun String.plazaReuseReferenceIdPart(): String =
        map { char ->
            when (char) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9' -> char
                else -> '_'
            }
        }.joinToString("").ifBlank { "work" }

    private fun QuickCreatePlazaReuseMediaKind.toQuickCreateMediaType(): QuickCreateMediaType =
        when (this) {
            QuickCreatePlazaReuseMediaKind.IMAGE -> QuickCreateMediaType.IMAGE
            QuickCreatePlazaReuseMediaKind.VIDEO -> QuickCreateMediaType.VIDEO
            QuickCreatePlazaReuseMediaKind.AUDIO -> QuickCreateMediaType.AUDIO
        }

    private fun QuickCreateInspirationTemplateDetail.templateMediaReferences(
        activeUploadAliases: Map<String, QuickCreationServiceUploadFieldAlias>,
        declaredUploadAliases: Map<String, String>,
    ): List<MediaReference> =
        listParams.entries.flatMapIndexed { fieldIndex, (key, values) ->
            val activeFieldAlias = activeUploadAliases[key]
            val mediaType = activeFieldAlias?.mediaKind?.toQuickCreateMediaType() ?: key.templateMediaType()
            // 模板素材只有命中当前活跃上传字段时才绑定 fieldParamKey；
            // 已声明但未激活的字段素材必须丢弃，避免变成全局素材污染当前参数组合。
            val fieldParamKey = when {
                activeFieldAlias != null -> activeFieldAlias.paramKey
                key in declaredUploadAliases -> return@flatMapIndexed emptyList()
                else -> null
            }
            values.mapIndexed { index, url ->
                remoteDoneMediaReference(
                    url = url,
                    type = mediaType,
                    id = "template_${categoryId.templateCategoryIdPart()}_${templateId}_${fieldIndex}_${key.templateReferenceIdPart()}_${mediaType.name}_$index",
                    fallbackDisplayName = "${mediaType.name.lowercase()}_$index",
                    fieldParamKey = fieldParamKey,
                )
            }
        }

    private fun String.templateReferenceIdPart(): String =
        map { char ->
            when (char) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9' -> char
                else -> '_'
            }
        }.joinToString("").ifBlank { "field" }

    private fun String?.templateCategoryIdPart(): String =
        this?.templateReferenceIdPart() ?: "unknown"

    private fun String.templateMediaType(): QuickCreateMediaType {
        val marker = uppercase()
        return when {
            marker.contains("AUDIO") -> QuickCreateMediaType.AUDIO
            marker.contains("VIDEO") -> QuickCreateMediaType.VIDEO
            else -> QuickCreateMediaType.IMAGE
        }
    }

    private fun QuickCreationUploadMediaKind.toQuickCreateMediaType(): QuickCreateMediaType =
        when (this) {
            QuickCreationUploadMediaKind.IMAGE -> QuickCreateMediaType.IMAGE
            QuickCreationUploadMediaKind.VIDEO -> QuickCreateMediaType.VIDEO
            QuickCreationUploadMediaKind.AUDIO -> QuickCreateMediaType.AUDIO
        }

    private fun Map<String, String>.templateImageAspectRatio(): ImageAspectRatio? =
        (this["aspectRatio"] ?: this["ratio"])?.let { value ->
            ImageAspectRatio.entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }

    private fun Map<String, String>.templateVideoAspectRatio(): VideoAspectRatio? =
        (this["aspectRatio"] ?: this["ratio"])?.let { value ->
            VideoAspectRatio.entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }

    private fun Map<String, String>.templateImageResolution(): ImageResolution? =
        this["resolution"]?.let { value ->
            ImageResolution.entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }

    private fun Map<String, String>.templateVideoResolution(): VideoResolution? =
        this["resolution"]?.let { value ->
            VideoResolution.entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }

    private fun Map<String, String>.templateImageQuality(): ImageQuality? =
        this["quality"]?.let { value ->
            ImageQuality.entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }

    private fun Map<String, String>.templateVideoDuration(): VideoDuration? =
        (this["duration"] ?: this["videoDuration"])?.toIntOrNull()?.let { seconds ->
            VideoDuration.entries.minByOrNull { duration ->
                kotlin.math.abs(duration.seconds - seconds)
            }
        }

    private fun Map<String, String>.templateBoolean(key: String): Boolean? =
        this[key]?.let { value ->
            when (value.lowercase()) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
}
