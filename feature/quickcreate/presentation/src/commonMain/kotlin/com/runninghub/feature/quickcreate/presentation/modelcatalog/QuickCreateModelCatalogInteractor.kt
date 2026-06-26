package com.runninghub.feature.quickcreate.presentation.modelcatalog

import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceSchema
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreateModelSelectionRepository
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 管理快捷创作的服务模型目录与服务参数状态。
 *
 * 本类位于 Presentation 层，因为它直接维护 `QuickCreateUiState` 中面向页面的模型列表、
 * 当前选中模型和服务字段参数。Repository 只负责返回 Domain 模型；模型重新加载后的规范化、
 * 默认参数回填以及是否触发价格预览，属于页面状态编排职责。
 * 本类不依赖 composeApp、Composable 或平台 API，可在 feature presentation 模块内独立测试。
 *
 * @param modelCatalogRepository 快捷创作模型目录仓库，只负责分别加载图片与视频服务模型目录。
 * @param scope 页面生命周期协程作用域，加载任务会随 ScreenModel 销毁而取消。
 * @param uiState 页面状态容器；本类只修改服务模型和服务参数相关字段。
 * @param onFeePreviewRequired 模型或有效服务参数变化后的回调，由 ScreenModel 统一调度价格预览。
 */
class QuickCreateModelCatalogInteractor(
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
    private val modelSelectionRepository: QuickCreateModelSelectionRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onFeePreviewRequired: () -> Unit,
) {
    /**
     * 当前页面生命周期内用户主动选择的图片模型身份键。
     *
     * 加载模型目录时会先读取持久化的上次选择；若用户在缓存展示和远端刷新之间重新选择模型，
     * 这里的值必须优先于旧持久化值，避免刷新回调把用户刚刚点选的模型改回旧选择。
     */
    private var sessionImageSelectionIdentityKey: String? = null

    /**
     * 当前页面生命周期内用户主动选择的视频模型身份键。
     *
     * 语义同 [sessionImageSelectionIdentityKey]，仅用于视频快捷创作模型列表。
     */
    private var sessionVideoSelectionIdentityKey: String? = null

    /**
     * 加载图片与视频服务模型目录，并在刷新后修正当前选中模型。
     *
     * 流程分两段执行：先读取本地缓存并尽快回写 UI，避免打开模型 sheet 时等待远端接口；
     * 随后后台强制刷新远端目录，成功后再用最新模型覆盖缓存展示。若本地保存过用户最后一次选择，
     * 优先恢复该模型；否则保留当前列表中的同一 `bindingId + skuId` 模型，或名称、分组和接口类型一致的
     * 同一服务模型。仍无法命中时回退到列表首项并使用新模型默认参数，避免生成请求继续携带过期服务字段。
     */
    fun loadServiceModels() {
        scope.launch {
            uiState.update { it.copy(serviceModelsLoading = true) }

            val lastImageIdentityKey = modelSelectionRepository.getLastImageServiceModelIdentityKey()
            val lastVideoIdentityKey = modelSelectionRepository.getLastVideoServiceModelIdentityKey()
            val shouldRefreshAfterCachedDisplay =
                modelCatalogRepository.hasCachedModels(QuickCreationServiceKind.IMAGE) ||
                    modelCatalogRepository.hasCachedModels(QuickCreationServiceKind.VIDEO)
            val imageModels = modelCatalogRepository.getModels(QuickCreationServiceKind.IMAGE)
            val videoModels = modelCatalogRepository.getModels(QuickCreationServiceKind.VIDEO)

            applyLoadedModels(
                imageModels = imageModels.getOrElse { emptyList() },
                videoModels = videoModels.getOrElse { emptyList() },
                loading = false,
                preferPersistedSelection = true,
                lastImageIdentityKey = lastImageIdentityKey,
                lastVideoIdentityKey = lastVideoIdentityKey,
            )
            onFeePreviewRequired()

            if (!shouldRefreshAfterCachedDisplay) return@launch
            val refreshedImageModels = modelCatalogRepository.refreshModels(QuickCreationServiceKind.IMAGE)
            val refreshedVideoModels = modelCatalogRepository.refreshModels(QuickCreationServiceKind.VIDEO)
            if (refreshedImageModels.isSuccess || refreshedVideoModels.isSuccess) {
                applyLoadedModels(
                    imageModels = refreshedImageModels.getOrElse { uiState.value.serviceImageModels },
                    videoModels = refreshedVideoModels.getOrElse { uiState.value.serviceVideoModels },
                    loading = false,
                    preferPersistedSelection = false,
                    lastImageIdentityKey = lastImageIdentityKey,
                    lastVideoIdentityKey = lastVideoIdentityKey,
                )
                onFeePreviewRequired()
            }
        }
    }

    private fun applyLoadedModels(
        imageModels: List<QuickCreationServiceModel>,
        videoModels: List<QuickCreationServiceModel>,
        loading: Boolean,
        preferPersistedSelection: Boolean,
        lastImageIdentityKey: String?,
        lastVideoIdentityKey: String?,
    ) {
        uiState.update { state ->
            val effectiveImageIdentityKey = sessionImageSelectionIdentityKey ?: lastImageIdentityKey
            val effectiveVideoIdentityKey = sessionVideoSelectionIdentityKey ?: lastVideoIdentityKey
            // 缓存快照用于即时可用，远端刷新只在能映射到当前选择时修正为最新对象；
            // 若刷新结果已改变默认首项，则保持当前已展示模型，避免网络慢或排序变动造成标签跳动。
            val selectedImage = imageModels.resolveServiceSelection(
                identityKey = effectiveImageIdentityKey,
                selected = state.selectedImageServiceModel,
                preferPersistedSelection = preferPersistedSelection,
            )
            val selectedVideo = videoModels.resolveServiceSelection(
                identityKey = effectiveVideoIdentityKey,
                selected = state.selectedVideoServiceModel,
                preferPersistedSelection = preferPersistedSelection,
            )
            val imageItems = imageModels.toQuickCreateServiceModelUiItems(selectedImage)
            val videoItems = videoModels.toQuickCreateServiceModelUiItems(selectedVideo)

            state.copy(
                serviceModelsLoading = loading,
                serviceImageModels = imageModels,
                serviceVideoModels = videoModels,
                selectedImageServiceModel = selectedImage,
                selectedVideoServiceModel = selectedVideo,
                serviceImageModelItems = imageItems,
                serviceVideoModelItems = videoItems,
                selectedImageServiceModelUi = imageItems.firstOrNull { it.selected },
                selectedVideoServiceModelUi = videoItems.firstOrNull { it.selected },
                imageServiceParams = if (hasSameServiceSelection(selectedImage, state.selectedImageServiceModel)) {
                    state.imageServiceParams
                } else {
                    QuickCreationServiceSchema.defaultParams(selectedImage)
                },
                videoServiceParams = if (hasSameServiceSelection(selectedVideo, state.selectedVideoServiceModel)) {
                    state.videoServiceParams
                } else {
                    QuickCreationServiceSchema.defaultParams(selectedVideo)
                },
            )
        }
    }

    /**
     * 切换当前图片服务模型并重置为该模型的默认服务字段。
     *
     * 只允许选择当前目录中能按服务身份匹配到的模型；外部传入的视频模型或过期对象会被忽略，
     * 防止跨类别模型污染图片生成请求。
     */
    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        val selectedModel = uiState.value.serviceImageModels.firstOrNull { it.matchesServiceIdentity(model) } ?: return
        updateImageServiceModelSelection(selectedModel)
    }

    /**
     * 按 UI 模型身份键切换当前图片服务模型。
     *
     * 模型选择面板只回传 [identityKey]，本类在内部解析为当前目录中的 Domain 模型，
     * 避免 Composable 直接持有或回传完整服务端模型对象。
     */
    fun updateImageServiceModel(identityKey: String) {
        val selectedModel = uiState.value.serviceImageModels
            .firstOrNull { it.quickCreateServiceModelIdentityKey() == identityKey }
            ?: return
        updateImageServiceModelSelection(selectedModel)
    }

    private fun updateImageServiceModelSelection(selectedModel: QuickCreationServiceModel) {
        sessionImageSelectionIdentityKey = selectedModel.quickCreateServiceModelIdentityKey()
        uiState.update {
            val imageItems = it.serviceImageModels.toQuickCreateServiceModelUiItems(selectedModel)
            it.copy(
                selectedImageServiceModel = selectedModel,
                serviceImageModelItems = imageItems,
                selectedImageServiceModelUi = imageItems.firstOrNull { item -> item.selected },
                imageServiceParams = QuickCreationServiceSchema.defaultParams(selectedModel),
            )
        }
        scope.launch {
            modelSelectionRepository.saveLastImageServiceModelIdentityKey(selectedModel.quickCreateServiceModelIdentityKey())
        }
        onFeePreviewRequired()
    }

    /**
     * 切换当前视频服务模型并重置为该模型的默认服务字段。
     *
     * 只允许选择当前视频目录中的模型；如果调用方传入图片模型或过期对象，本方法会直接忽略，
     * 保持视频生成请求继续使用上一份有效配置。
     */
    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        val selectedModel = uiState.value.serviceVideoModels.firstOrNull { it.matchesServiceIdentity(model) } ?: return
        updateVideoServiceModelSelection(selectedModel)
    }

    /**
     * 按 UI 模型身份键切换当前视频服务模型。
     *
     * 空值、过期 key 或跨类别 key 都会被忽略，保持当前视频生成配置不变。
     */
    fun updateVideoServiceModel(identityKey: String) {
        val selectedModel = uiState.value.serviceVideoModels
            .firstOrNull { it.quickCreateServiceModelIdentityKey() == identityKey }
            ?: return
        updateVideoServiceModelSelection(selectedModel)
    }

    private fun updateVideoServiceModelSelection(selectedModel: QuickCreationServiceModel) {
        sessionVideoSelectionIdentityKey = selectedModel.quickCreateServiceModelIdentityKey()
        uiState.update {
            val videoItems = it.serviceVideoModels.toQuickCreateServiceModelUiItems(selectedModel)
            it.copy(
                selectedVideoServiceModel = selectedModel,
                serviceVideoModelItems = videoItems,
                selectedVideoServiceModelUi = videoItems.firstOrNull { item -> item.selected },
                videoServiceParams = QuickCreationServiceSchema.defaultParams(selectedModel),
            )
        }
        scope.launch {
            modelSelectionRepository.saveLastVideoServiceModelIdentityKey(selectedModel.quickCreateServiceModelIdentityKey())
        }
        onFeePreviewRequired()
    }

    /**
     * 更新图片服务字段参数。
     *
     * 只有当前模型真实声明的字段才会被写入状态；隐藏字段或非激活 child 字段虽然可以暂存参数，
     * 但不会触发价格预览，避免无效字段变化造成额外计费请求。
     */
    fun updateImageServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        val selectedModel = uiState.value.selectedImageServiceModel
        if (selectedModel == null || !QuickCreationServiceSchema.hasFieldParam(selectedModel, paramKey)) return
        val nextParams = uiState.value.imageServiceParams + (paramKey to value)
        uiState.update {
            it.copy(imageServiceParams = nextParams)
        }
        if (paramKey in QuickCreationServiceSchema.activeParamKeys(selectedModel, nextParams)) {
            onFeePreviewRequired()
        }
    }

    /**
     * 更新视频服务字段参数。
     *
     * 本方法与图片参数更新保持同一套字段校验规则；只有影响当前可见字段的参数变化才重新预览价格，
     * 以免用户调整非激活分支时覆盖当前有效报价。
     */
    fun updateVideoServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        val selectedModel = uiState.value.selectedVideoServiceModel
        if (selectedModel == null || !QuickCreationServiceSchema.hasFieldParam(selectedModel, paramKey)) return
        val nextParams = uiState.value.videoServiceParams + (paramKey to value)
        uiState.update {
            it.copy(videoServiceParams = nextParams)
        }
        if (paramKey in QuickCreationServiceSchema.activeParamKeys(selectedModel, nextParams)) {
            onFeePreviewRequired()
        }
    }

    private fun QuickCreationServiceModel.matchesServiceIdentity(other: QuickCreationServiceModel?): Boolean =
        other != null && bindingId == other.bindingId && skuId == other.skuId

    private fun List<QuickCreationServiceModel>.matchingServiceSelection(
        selected: QuickCreationServiceModel?,
    ): QuickCreationServiceModel? {
        if (selected == null) return null
        return firstOrNull { it.matchesServiceIdentity(selected) }
            ?: firstOrNull { it.matchesServiceSignature(selected) }
    }

    private fun List<QuickCreationServiceModel>.matchingPersistedSelection(
        identityKey: String?,
    ): QuickCreationServiceModel? =
        identityKey
            ?.takeIf { it.isNotBlank() }
            ?.let { key -> firstOrNull { it.quickCreateServiceModelIdentityKey() == key } }

    private fun List<QuickCreationServiceModel>.resolveServiceSelection(
        identityKey: String?,
        selected: QuickCreationServiceModel?,
        preferPersistedSelection: Boolean,
    ): QuickCreationServiceModel? {
        val currentSelection = matchingServiceSelection(selected)
        val persistedSelection = matchingPersistedSelection(identityKey)
        return when {
            preferPersistedSelection && persistedSelection != null -> persistedSelection
            currentSelection != null -> currentSelection
            persistedSelection != null -> persistedSelection
            else -> preferredQuickCreateDefaultSelection() ?: firstOrNull()
        }
    }

    private fun List<QuickCreationServiceModel>.preferredQuickCreateDefaultSelection(): QuickCreationServiceModel? =
        firstOrNull { it.isPreferredImageDefaultModel() }
            ?: firstOrNull { it.isPreferredVideoDefaultModel() }

    private fun QuickCreationServiceModel.isPreferredImageDefaultModel(): Boolean {
        val signature = listOf(categoryId, name, groupName, apiType, apiSource)
            .joinToString("|")
            .quickCreateServiceSignatureToken()
        return ("g20" in signature || "g2.0" in signature) &&
            ("image" in signature || "picture" in signature)
    }

    private fun QuickCreationServiceModel.isPreferredVideoDefaultModel(): Boolean {
        val signature = listOf(categoryId, name, groupName, apiType, apiSource)
            .joinToString("|")
            .quickCreateServiceSignatureToken()
        return ("seedance20" in signature || "seedance2.0" in signature) &&
            "video" in signature
    }

    private fun QuickCreationServiceModel.matchesServiceSignature(other: QuickCreationServiceModel?): Boolean =
        other != null &&
            categoryId.quickCreateServiceSignatureToken() == other.categoryId.quickCreateServiceSignatureToken() &&
            groupName.quickCreateServiceSignatureToken() == other.groupName.quickCreateServiceSignatureToken() &&
            name.quickCreateServiceSignatureToken() == other.name.quickCreateServiceSignatureToken() &&
            apiType.quickCreateServiceSignatureToken() == other.apiType.quickCreateServiceSignatureToken() &&
            apiSource.quickCreateServiceSignatureToken() == other.apiSource.quickCreateServiceSignatureToken()

    private fun String?.quickCreateServiceSignatureToken(): String =
        orEmpty()
            .lowercase()
            .filterNot { it.isWhitespace() || it == '-' || it == '_' }

    private fun hasSameServiceSelection(
        first: QuickCreationServiceModel?,
        second: QuickCreationServiceModel?,
    ): Boolean =
        first != null && (first.matchesServiceIdentity(second) || first.matchesServiceSignature(second))
}
