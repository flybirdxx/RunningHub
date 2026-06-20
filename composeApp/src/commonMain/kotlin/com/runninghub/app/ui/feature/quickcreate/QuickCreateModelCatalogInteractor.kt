package com.runninghub.app.ui.feature.quickcreate

import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreationServiceSchema
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
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
 *
 * @param quickCreateRepository 快捷创作仓库，用于分别加载图片与视频服务模型目录。
 * @param scope 页面生命周期协程作用域，加载任务会随 ScreenModel 销毁而取消。
 * @param uiState 页面状态容器；本类只修改服务模型和服务参数相关字段。
 * @param onFeePreviewRequired 模型或有效服务参数变化后的回调，由 ScreenModel 统一调度价格预览。
 */
internal class QuickCreateModelCatalogInteractor(
    private val quickCreateRepository: QuickCreateRepository,
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<QuickCreateUiState>,
    private val onFeePreviewRequired: () -> Unit,
) {

    /**
     * 加载图片与视频服务模型目录，并在刷新后修正当前选中模型。
     *
     * 若新目录中仍存在同一 `bindingId + skuId` 的模型，会切换到最新返回的规范对象并保留用户已填参数；
     * 若原模型不存在，则回退到列表首项并使用新模型默认参数，避免生成请求继续携带过期服务字段。
     */
    fun loadServiceModels() {
        scope.launch {
            uiState.update { it.copy(serviceModelsLoading = true) }

            val imageModels = quickCreateRepository.getModels("IMAGE")
            val videoModels = quickCreateRepository.getModels("VIDEO")

            uiState.update { state ->
                val images = imageModels.getOrElse { emptyList() }
                val videos = videoModels.getOrElse { emptyList() }
                val selectedImage = state.selectedImageServiceModel
                    ?.let { selected -> images.firstOrNull { it.matchesServiceIdentity(selected) } }
                    ?: images.firstOrNull()
                val selectedVideo = state.selectedVideoServiceModel
                    ?.let { selected -> videos.firstOrNull { it.matchesServiceIdentity(selected) } }
                    ?: videos.firstOrNull()
                val imageItems = images.toQuickCreateServiceModelUiItems(selectedImage)
                val videoItems = videos.toQuickCreateServiceModelUiItems(selectedVideo)

                state.copy(
                    serviceModelsLoading = false,
                    serviceImageModels = images,
                    serviceVideoModels = videos,
                    selectedImageServiceModel = selectedImage,
                    selectedVideoServiceModel = selectedVideo,
                    serviceImageModelItems = imageItems,
                    serviceVideoModelItems = videoItems,
                    selectedImageServiceModelUi = imageItems.firstOrNull { it.selected },
                    selectedVideoServiceModelUi = videoItems.firstOrNull { it.selected },
                    imageServiceParams = if (hasSameServiceIdentity(selectedImage, state.selectedImageServiceModel)) {
                        state.imageServiceParams
                    } else {
                        QuickCreationServiceSchema.defaultParams(selectedImage)
                    },
                    videoServiceParams = if (hasSameServiceIdentity(selectedVideo, state.selectedVideoServiceModel)) {
                        state.videoServiceParams
                    } else {
                        QuickCreationServiceSchema.defaultParams(selectedVideo)
                    },
                )
            }
            onFeePreviewRequired()
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
        uiState.update {
            val imageItems = it.serviceImageModels.toQuickCreateServiceModelUiItems(selectedModel)
            it.copy(
                selectedImageServiceModel = selectedModel,
                serviceImageModelItems = imageItems,
                selectedImageServiceModelUi = imageItems.firstOrNull { item -> item.selected },
                imageServiceParams = QuickCreationServiceSchema.defaultParams(selectedModel),
            )
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
        uiState.update {
            val videoItems = it.serviceVideoModels.toQuickCreateServiceModelUiItems(selectedModel)
            it.copy(
                selectedVideoServiceModel = selectedModel,
                serviceVideoModelItems = videoItems,
                selectedVideoServiceModelUi = videoItems.firstOrNull { item -> item.selected },
                videoServiceParams = QuickCreationServiceSchema.defaultParams(selectedModel),
            )
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

    private fun hasSameServiceIdentity(
        first: QuickCreationServiceModel?,
        second: QuickCreationServiceModel?,
    ): Boolean =
        first != null && first.matchesServiceIdentity(second)
}
