package com.runninghub.app.ui.feature.create

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.feature.quickcreate.domain.QuickCreationMediaUploadRepository
import com.runninghub.feature.quickcreate.domain.ImageGenerationRequest
import com.runninghub.feature.quickcreate.domain.QuickCreateResultItem
import com.runninghub.feature.quickcreate.domain.QuickCreateTaskStatus
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreview
import com.runninghub.feature.quickcreate.domain.QuickCreationFeePreviewRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationGenerationRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationHistoryItem
import com.runninghub.feature.quickcreate.domain.QuickCreationModelCatalogRepository
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceField
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceFieldInputChild
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceKind
import com.runninghub.feature.quickcreate.domain.QuickCreationServiceModel
import com.runninghub.feature.quickcreate.domain.QuickCreationTaskHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 旧创作页的顶部业务分类。
 *
 * 只有图片和视频分类可以直接加载快捷创作服务模型，因此通过 [serviceKind] 暴露领域类别；
 * 音频、LLM 和全部分类当前没有对应的新服务模型目录，保持为空并走页面降级展示。
 *
 * @property serviceKind 可用于查询服务模型目录的领域类别，空值表示该分类不支持新服务模型。
 * @property displayName 页面展示名称。
 */
enum class CreateCategory(
    val serviceKind: QuickCreationServiceKind?,
    val displayName: String,
) {
    IMAGE(QuickCreationServiceKind.IMAGE, "图片"),
    VIDEO(QuickCreationServiceKind.VIDEO, "视频"),
    AUDIO(null, "音频"),
    LLM(null, "LLM"),
    ALL(null, "全部"),
}

/**
 * 旧创作页最近历史的本地筛选条件。
 *
 * 该筛选只作用于当前已加载的最近历史列表，不会改变远端分页参数。这样可以在保留旧页面交互的同时，
 * 避免历史筛选与后续 QuickCreate 历史模块拆分互相耦合。
 *
 * @property displayName 页面筛选入口展示的中文名称。
 */
enum class CreateHistoryFilter(
    val displayName: String,
) {
    ALL("全部"),
    IMAGE("图片"),
    VIDEO("视频"),
    RUNNING("进行中"),
    SUCCESS("成功"),
}

/**
 * 旧创作页的不可变页面状态。
 *
 * 状态按模型目录、动态输入、上传、计费、提交、任务结果和历史详情分区保存。
 * ScreenModel 每次通过拷贝生成新状态，Composable 只消费快照并通过事件回调触发业务动作。
 *
 * @property isLoading 模型目录首次加载状态，用于控制页面骨架和空态。
 * @property selectedCategory 当前选中的顶部业务分类，只有图片/视频会触发快捷创作模型目录请求。
 * @property serviceModels 当前分类下可用的服务模型列表，保留服务端排序。
 * @property selectedModel 用户当前选择的服务模型；为空时禁止提交和计费预览。
 * @property fieldValues 动态字段输入值，key 为服务字段参数名或子字段参数名。
 * @property recentHistory 旧页面最近历史缓存，只保存当前已加载页的数据。
 * @property selectedHistoryFilter 最近历史的本地筛选条件。
 * @property historyLoading 最近历史列表或刷新请求是否进行中。
 * @property historyError 最近历史加载失败原因；为空表示没有可展示错误。
 * @property isSubmitting 当前是否正在提交生成任务，提交期间需要阻止重复点击。
 * @property submitMessage 提交或任务状态的临时提示文本。
 * @property catalogNotice 非图片/视频分类或目录降级时展示的说明。
 * @property searchQuery 旧页面搜索输入，当前只用于本地过滤和后续兼容。
 * @property error 模型目录或主流程的页面级错误。
 * @property feePreviewLoading 计费预览请求是否进行中。
 * @property feePreviewError 计费预览失败原因；为空表示可展示最近一次成功预览或无预览。
 * @property feePreview 最近一次成功的价格预览结果。
 * @property currentTaskStatus 当前提交任务的轮询状态。
 * @property lastResults 最近一次任务完成后返回的结果列表。
 * @property historyDetailLoading 历史详情请求是否进行中。
 * @property selectedHistoryDetail 当前打开的历史详情。
 * @property historyDetailError 历史详情加载失败原因。
 * @property uploadFieldStates 各上传字段的本地 URI、远端 URL 和错误状态，key 与字段参数名一致。
 */
data class CreateUiState(
    val isLoading: Boolean = true,
    val selectedCategory: CreateCategory = CreateCategory.IMAGE,
    val serviceModels: List<QuickCreationServiceModel> = emptyList(),
    val selectedModel: QuickCreationServiceModel? = null,
    val fieldValues: Map<String, String> = emptyMap(),
    val recentHistory: List<QuickCreationHistoryItem> = emptyList(),
    val selectedHistoryFilter: CreateHistoryFilter = CreateHistoryFilter.ALL,
    val historyLoading: Boolean = false,
    val historyError: String? = null,
    val isSubmitting: Boolean = false,
    val submitMessage: String? = null,
    val catalogNotice: String? = null,
    val searchQuery: String = "",
    val error: String? = null,
    val feePreviewLoading: Boolean = false,
    val feePreviewError: String? = null,
    val feePreview: QuickCreationFeePreview? = null,
    val currentTaskStatus: QuickCreateTaskStatus? = null,
    val lastResults: List<QuickCreateResultItem> = emptyList(),
    val historyDetailLoading: Boolean = false,
    val selectedHistoryDetail: QuickCreationHistoryItem? = null,
    val historyDetailError: String? = null,
    val uploadFieldStates: Map<String, CreateUploadFieldState> = emptyMap(),
) {
    val filteredHistory: List<QuickCreationHistoryItem>
        get() = recentHistory.filterByCreateHistoryFilter(selectedHistoryFilter)
}

/**
 * 旧创作页单个上传字段的状态。
 *
 * 本状态把本地媒体选择、远端上传结果和错误信息分开保存，避免上传失败时丢失用户刚选择的本地文件。
 * 计费预览和提交只使用 [remoteUrl]，因此 [isUploading] 或 [isError] 为 true 时调用方应阻止提交。
 *
 * @property localUri 用户在当前设备选择的媒体 URI，可能只在本次页面生命周期内有效。
 * @property remoteUrl 上传成功后服务端返回的媒体 URL，用于构造生成请求。
 * @property fileName 展示和 MIME 推断使用的文件名。
 * @property isUploading 当前字段是否正在读取本地文件或上传远端。
 * @property isError 当前字段最近一次上传是否失败。
 * @property errorMessage 上传失败时可展示的错误说明。
 */
data class CreateUploadFieldState(
    val localUri: String? = null,
    val remoteUrl: String? = null,
    val fileName: String? = null,
    val isUploading: Boolean = false,
    val isError: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * 旧创作页的 ScreenModel。
 *
 * 本类维护旧创作页的模型目录、字段输入、上传、价格预览、提交和最近历史状态。模型目录读取通过
 * [modelCatalogRepository] 进入窄边界；上传、计费和生成分别通过 [mediaUploadRepository]、
 * [feePreviewRepository] 与 [generationRepository] 进入窄边界；历史通过 [historyRepository] 进入窄边界。
 *
 * @param historyRepository 快捷创作历史仓库，负责最近历史、详情和轮询刷新。
 * @param mediaResolver 平台媒体读取边界，用于把本地 URI 转换为上传所需的文件名、大小和字节内容。
 * @param ioDispatcher 媒体字节读取使用的调度器；commonMain 默认使用跨平台可用的 Default，
 * Android/iOS 如需专用 IO 调度器可在组合根或测试中显式注入。
 * @param modelCatalogRepository 快捷创作模型目录仓库，只用于加载图片/视频服务模型列表。
 * @param generationRepository 快捷创作生成仓库，只用于提交旧创作页图片生成任务。
 * @param feePreviewRepository 快捷创作计费预览仓库，只用于远端价格确认。
 * @param mediaUploadRepository 快捷创作媒体上传仓库，只用于把本地媒体上传为远端 URL。
 */
class CreateScreenModel(
    private val historyRepository: QuickCreationTaskHistoryRepository,
    private val mediaResolver: MediaResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val modelCatalogRepository: QuickCreationModelCatalogRepository,
    private val generationRepository: QuickCreationGenerationRepository,
    private val feePreviewRepository: QuickCreationFeePreviewRepository,
    private val mediaUploadRepository: QuickCreationMediaUploadRepository,
) : ScreenModel {
    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var feePreviewJob: Job? = null
    private var generationJob: Job? = null
    private var historyRefreshJob: Job? = null
    private val uploadJobs = mutableMapOf<String, Job>()

    fun loadModels() {
        loadJob?.cancel()
        loadJob = screenModelScope.launch {
            val category = _uiState.value.selectedCategory
            val serviceKind = category.serviceKind
            if (serviceKind == null) {
                _uiState.update {
                    it.copy(
                    isLoading = false,
                    serviceModels = emptyList(),
                    selectedModel = null,
                    fieldValues = emptyMap(),
                        feePreview = null,
                        feePreviewError = null,
                        catalogNotice = "${category.displayName}暂未接入快捷创作模型",
                        error = null,
                    )
                }
                loadRecentHistory()
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    catalogNotice = null,
                    feePreview = null,
                    feePreviewError = null,
                    feePreviewLoading = false,
                )
            }

            val result = withTimeoutOrNull(6_000) {
                modelCatalogRepository.getModels(serviceKind)
            } ?: Result.failure(IllegalStateException("模型目录请求超时"))

            result
                .onSuccess { models ->
                    val filteredModels = models.filteredByQuery(_uiState.value.searchQuery)
                    val selected = _uiState.value.selectedModel
                        ?.let { current -> filteredModels.firstOrNull { it.sameIdentity(current) } }
                        ?: filteredModels.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            serviceModels = filteredModels,
                            selectedModel = selected,
                            fieldValues = selected?.defaultValues().orEmpty(),
                            catalogNotice = if (category == CreateCategory.IMAGE) null else "${category.displayName}模型已加载，首期仅开放图片生成",
                            error = null,
                        )
                    }
                    loadRecentHistory()
                    scheduleFeePreview()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            serviceModels = emptyList(),
                            selectedModel = null,
                            fieldValues = emptyMap(),
                            catalogNotice = null,
                            error = error.toCatalogErrorMessage(),
                        )
                    }
                    loadRecentHistory()
                }
        }
    }

    fun updateCategory(category: CreateCategory) {
        if (_uiState.value.selectedCategory == category) return
        feePreviewJob?.cancel()
        generationJob?.cancel()
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
        _uiState.update {
            it.copy(
                selectedCategory = category,
                serviceModels = emptyList(),
                selectedModel = null,
                fieldValues = emptyMap(),
                isSubmitting = false,
                submitMessage = null,
                feePreviewLoading = false,
                feePreviewError = null,
                feePreview = null,
                currentTaskStatus = null,
                lastResults = emptyList(),
                uploadFieldStates = emptyMap(),
            )
        }
        loadModels()
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectModel(modelKey: String) {
        val selected = _uiState.value.serviceModels.firstOrNull { it.identityKey == modelKey } ?: return
        feePreviewJob?.cancel()
        uploadJobs.values.forEach { it.cancel() }
        uploadJobs.clear()
        _uiState.update {
            it.copy(
                selectedModel = selected,
                fieldValues = selected.defaultValues(),
                submitMessage = null,
                error = null,
                feePreview = null,
                feePreviewError = null,
                feePreviewLoading = false,
                uploadFieldStates = emptyMap(),
            )
        }
        scheduleFeePreview()
    }

    fun updateFieldValue(fieldKey: String, value: String) {
        _uiState.update {
            it.copy(
                fieldValues = it.fieldValues + (fieldKey to value),
                submitMessage = null,
                feePreview = null,
                feePreviewError = null,
            )
        }
        scheduleFeePreview()
    }

    fun pickUploadField(fieldKey: String, uriString: String) {
        if (fieldKey.isBlank() || uriString.isBlank()) return
        val model = _uiState.value.selectedModel ?: return
        val field = model.fields.firstOrNull {
            it.visible && it.isUploadField && (it.fieldKey == fieldKey || it.paramKey == fieldKey)
        } ?: return
        val stateKey = field.fieldKey
        val fileName = mediaResolver.getDisplayName(uriString)
            ?.takeIf { it.isNotBlank() }
            ?: defaultUploadFileName(field)
        val maxBytes = field.maxUploadBytes
        val fileSize = runCatching { mediaResolver.getFileSizeBytes(uriString) }.getOrDefault(0L)
        if (maxBytes != null && fileSize > maxBytes) {
            _uiState.update {
                it.copy(
                    uploadFieldStates = it.uploadFieldStates + (
                        stateKey to CreateUploadFieldState(
                            localUri = uriString,
                            fileName = fileName,
                            isError = true,
                            errorMessage = "文件超过服务端限制",
                        )
                    ),
                    submitMessage = "文件超过服务端限制",
                )
            }
            return
        }

        uploadJobs[stateKey]?.cancel()
        _uiState.update {
            it.copy(
                uploadFieldStates = it.uploadFieldStates + (
                    stateKey to CreateUploadFieldState(
                        localUri = uriString,
                        fileName = fileName,
                        isUploading = true,
                    )
                ),
                submitMessage = null,
                feePreview = null,
                feePreviewError = null,
            )
        }
        uploadJobs[stateKey] = screenModelScope.launch {
            try {
                val bytes = withContext(ioDispatcher) { mediaResolver.readBytes(uriString) }
                val remoteUrl = mediaUploadRepository.uploadMedia(
                    fileBytes = bytes,
                    fileName = fileName,
                    mimeType = inferMimeType(fileName, field),
                ).getOrThrow()

                _uiState.update { state ->
                    val previousValues = field.inputValues(state.fieldValues.rawValueFor(field))
                    val nextValues = if (field.multipleInputs || (field.maxUploadCount ?: 1) > 1) {
                        (previousValues + remoteUrl).take(field.inputExtra?.maxInputCount ?: field.maxUploadCount ?: Int.MAX_VALUE)
                    } else {
                        listOf(remoteUrl)
                    }
                    state.copy(
                        fieldValues = state.fieldValues + (stateKey to nextValues.joinToString("\n")),
                        uploadFieldStates = state.uploadFieldStates + (
                            stateKey to CreateUploadFieldState(
                                localUri = uriString,
                                remoteUrl = remoteUrl,
                                fileName = fileName,
                            )
                        ),
                    )
                }
                scheduleFeePreview()
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        uploadFieldStates = it.uploadFieldStates + (
                            stateKey to CreateUploadFieldState(
                                localUri = uriString,
                                fileName = fileName,
                                isError = true,
                                errorMessage = error.message ?: "上传失败",
                            )
                        ),
                        feePreview = null,
                        feePreviewError = error.message ?: "上传失败",
                    )
                }
            }
        }
    }

    fun removeUploadField(fieldKey: String) {
        val model = _uiState.value.selectedModel ?: return
        val field = model.fields.firstOrNull { it.fieldKey == fieldKey || it.paramKey == fieldKey } ?: return
        val stateKey = field.fieldKey
        uploadJobs[stateKey]?.cancel()
        uploadJobs.remove(stateKey)
        _uiState.update {
            it.copy(
                fieldValues = it.fieldValues - stateKey,
                uploadFieldStates = it.uploadFieldStates - stateKey,
                feePreview = null,
                feePreviewError = null,
            )
        }
        scheduleFeePreview()
    }

    fun submitSelectedModel() {
        val state = _uiState.value
        val model = state.selectedModel ?: return

        if (state.selectedCategory != CreateCategory.IMAGE) {
            _uiState.update { it.copy(submitMessage = "首期仅支持图片生成") }
            return
        }

        val validationError = model.validateFieldValues(state.fieldValues)
        if (validationError != null) {
            _uiState.update { it.copy(submitMessage = validationError) }
            return
        }

        if (state.feePreviewLoading) {
            _uiState.update { it.copy(submitMessage = "价格确认中，请稍后") }
            return
        }

        val uploadIssue = state.uploadFieldStates.values.firstOrNull { it.isUploading || it.isError }
        if (uploadIssue?.isUploading == true) {
            _uiState.update { it.copy(submitMessage = "文件上传中，请稍后") }
            return
        }
        if (uploadIssue?.isError == true) {
            _uiState.update { it.copy(submitMessage = uploadIssue.errorMessage ?: "文件上传失败") }
            return
        }

        val preview = state.feePreview
        if (preview != null && (!preview.passed || preview.insufficientType != null)) {
            _uiState.update { it.copy(submitMessage = state.feePreviewError ?: "余额不足或价格预览未通过") }
            return
        }
        if (state.feePreviewError != null || preview == null) {
            _uiState.update { it.copy(submitMessage = "价格待确认，暂不能生成") }
            return
        }

        val request = model.buildImageRequest(state.fieldValues)
        generationJob?.cancel()
        generationJob = screenModelScope.launch {
            generationRepository.generateImage(request).collect { status ->
                when (status) {
                    QuickCreateTaskStatus.Submitting -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = true,
                                submitMessage = "任务提交中",
                                currentTaskStatus = status,
                            )
                        }
                    }
                    is QuickCreateTaskStatus.Queuing -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = true,
                                submitMessage = "任务已排队 ${status.taskId}",
                                currentTaskStatus = status,
                            )
                        }
                        loadRecentHistory(showLoading = false)
                    }
                    is QuickCreateTaskStatus.Running -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = true,
                                submitMessage = "生成中 ${status.progress}%",
                                currentTaskStatus = status,
                            )
                        }
                    }
                    is QuickCreateTaskStatus.Success -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitMessage = "生成成功 ${status.taskId}",
                                currentTaskStatus = status,
                                lastResults = status.results,
                            )
                        }
                        loadRecentHistory(showLoading = false)
                    }
                    is QuickCreateTaskStatus.Failed -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitMessage = status.errorMessage,
                                currentTaskStatus = status,
                            )
                        }
                        loadRecentHistory(showLoading = false)
                    }
                    is QuickCreateTaskStatus.Cancelled -> {
                        _uiState.update {
                            // 旧 Create 页面仅作为迁移期对照保留；这里跟随 Domain 取消终态停止提交态，
                            // 避免服务端取消被误判为仍在生成。
                            it.copy(
                                isSubmitting = false,
                                submitMessage = "任务已取消",
                                currentTaskStatus = status,
                            )
                        }
                        loadRecentHistory(showLoading = false)
                    }
                    is QuickCreateTaskStatus.Error -> {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                submitMessage = status.message,
                                currentTaskStatus = status,
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectHistoryOutput(outputId: String) {
        if (outputId.isBlank()) return
        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    historyDetailLoading = true,
                    selectedHistoryDetail = null,
                    historyDetailError = null,
                )
            }
            historyRepository.getQuickCreationHistoryDetail(outputId)
                .onSuccess { item ->
                    _uiState.update {
                        it.copy(
                            historyDetailLoading = false,
                            selectedHistoryDetail = item,
                            historyDetailError = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            historyDetailLoading = false,
                            selectedHistoryDetail = null,
                            historyDetailError = error.message ?: "历史详情加载失败",
                        )
                    }
                }
        }
    }

    fun dismissHistoryDetail() {
        _uiState.update {
            it.copy(
                historyDetailLoading = false,
                selectedHistoryDetail = null,
                historyDetailError = null,
            )
        }
    }

    fun refreshRecentHistory() {
        screenModelScope.launch {
            loadRecentHistory(showLoading = true)
        }
    }

    fun updateHistoryFilter(filter: CreateHistoryFilter) {
        _uiState.update { it.copy(selectedHistoryFilter = filter) }
    }

    private fun scheduleFeePreview() {
        feePreviewJob?.cancel()
        val state = _uiState.value
        val model = state.selectedModel ?: return
        if (state.selectedCategory != CreateCategory.IMAGE) return
        if (model.promptValue(state.fieldValues).isBlank()) {
            _uiState.update { it.copy(feePreviewLoading = false, feePreview = null, feePreviewError = null) }
            return
        }
        feePreviewJob = screenModelScope.launch {
            delay(500)
            previewFeeNow()
        }
    }

    private suspend fun previewFeeNow() {
        val state = _uiState.value
        val model = state.selectedModel ?: return
        if (state.selectedCategory != CreateCategory.IMAGE) return
        val validationError = model.validateFieldValues(state.fieldValues)
        if (validationError != null) {
            _uiState.update {
                it.copy(
                    feePreviewLoading = false,
                    feePreview = null,
                    feePreviewError = validationError,
                )
            }
            return
        }

        _uiState.update { it.copy(feePreviewLoading = true, feePreviewError = null) }
        feePreviewRepository.previewImageQuickCreationFee(model.buildImageRequest(state.fieldValues))
            .onSuccess { preview ->
                _uiState.update {
                    it.copy(
                        feePreviewLoading = false,
                        feePreview = preview,
                        feePreviewError = if (!preview.passed || preview.insufficientType != null) {
                            "余额不足或价格预览未通过"
                        } else {
                            null
                        },
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        feePreviewLoading = false,
                        feePreview = null,
                        feePreviewError = error.toFeePreviewErrorMessage(),
                    )
                }
            }
    }

    private suspend fun loadRecentHistory(showLoading: Boolean = true) {
        if (showLoading) {
            _uiState.update { it.copy(historyLoading = true, historyError = null) }
        }
        historyRepository.listQuickCreationHistory(page = 1, size = 12)
            .onSuccess { page ->
                _uiState.update {
                    it.copy(
                        recentHistory = page.items,
                        historyLoading = false,
                        historyError = null,
                    )
                }
                updateHistoryRefreshJob(page.items)
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        historyLoading = false,
                        historyError = error.toHistoryErrorMessage(),
                    )
                }
            }
    }

    private fun updateHistoryRefreshJob(items: List<QuickCreationHistoryItem>) {
        val needsRefresh = items.any { it.needsHistoryRefresh }
        if (!needsRefresh) {
            historyRefreshJob?.cancel()
            historyRefreshJob = null
            return
        }
        if (historyRefreshJob?.isActive == true) return
        historyRefreshJob = screenModelScope.launch {
            while (_uiState.value.recentHistory.any { it.needsHistoryRefresh }) {
                delay(HISTORY_REFRESH_INTERVAL_MS)
                historyRepository.listQuickCreationHistory(page = 1, size = 12)
                    .onSuccess { page ->
                        _uiState.update { it.copy(recentHistory = page.items, historyError = null) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(historyError = error.toHistoryErrorMessage()) }
                    }
            }
            historyRefreshJob = null
        }
    }

    private fun Throwable.toSafeLogMessage(): String =
        message
            ?.replace(Regex("(?i)bearer\\s+[A-Za-z0-9._-]+"), "Bearer <redacted>")
            ?.replace(Regex("(?i)(token|cookie)=([^\\s,;]+)"), "$1=<redacted>")
            ?.take(240)
            ?: this::class.simpleName.orEmpty()

    private fun List<QuickCreationServiceModel>.filteredByQuery(query: String): List<QuickCreationServiceModel> {
        val normalized = query.trim()
        if (normalized.isBlank()) return this
        return filter { model ->
            listOfNotNull(model.name, model.groupName, model.description)
                .any { it.contains(normalized, ignoreCase = true) }
        }
    }

    private fun QuickCreationServiceModel.defaultValues(): Map<String, String> =
        fields
            .filter { it.visible }
            .associate { field ->
                field.fieldKey to field.initialValue()
            }

    private fun QuickCreationServiceField.initialValue(): String {
        if (isPromptField) return ""
        defaultValue?.takeIf { it.isNotBlank() }?.let { return it }
        if (required && options.isNotEmpty() && !isUploadField) {
            return options.firstNotNullOfOrNull { option ->
                option.value.ifBlank { option.label }.takeIf { it.isNotBlank() }
            }.orEmpty()
        }
        return ""
    }

    private fun QuickCreationServiceModel.validateFieldValues(values: Map<String, String>): String? {
        fields.filter { it.visible }.forEach { field ->
            val raw = values.rawValueFor(field)
            val title = field.displayTitle
            if (field.required && raw.isBlank()) {
                return "$title 不能为空"
            }
            if (raw.isBlank()) return@forEach

            val minLength = field.inputExtra?.minLength
            if (minLength != null && raw.length < minLength) {
                return "$title 至少需要 $minLength 个字符"
            }
            val maxLength = field.inputExtra?.maxLength
            if (maxLength != null && raw.length > maxLength) {
                return "$title 最多 $maxLength 个字符"
            }
            if (field.options.isNotEmpty() && !field.isUploadField) {
                val allowed = field.options.flatMap { option -> listOf(option.value, option.label) }
                    .filter { it.isNotBlank() }
                val ignoreCase = field.inputExtra?.ignoreListValueCaseSensitive != true
                val matches = allowed.any { allowedValue ->
                    if (ignoreCase) allowedValue.equals(raw, ignoreCase = true) else allowedValue == raw
                }
                if (!matches) {
                    val display = field.options.joinToString(", ") { option -> option.value.ifBlank { option.label } }
                    return "$title 必须是 $display 之一"
                }
            }
            val maxCount = field.inputExtra?.maxInputCount ?: field.maxUploadCount
            if (field.isUploadField && maxCount != null && field.inputValues(raw).size > maxCount) {
                return "$title 最多支持 $maxCount 个文件"
            }
        }
        activeInputChildren(values).forEach { child ->
            val raw = values.rawValueFor(child)
            val title = child.displayTitle
            if (child.required && raw.isBlank()) {
                return "$title 不能为空"
            }
            if (raw.isBlank()) return@forEach

            val minLength = child.minLength
            if (minLength != null && raw.length < minLength) {
                return "$title 至少需要 $minLength 个字符"
            }
            val maxLength = child.maxLength
            if (maxLength != null && raw.length > maxLength) {
                return "$title 最多 $maxLength 个字符"
            }
            if (child.options.isNotEmpty() && !child.isUploadField) {
                val allowed = child.options.flatMap { option -> listOf(option.value, option.label) }
                    .filter { it.isNotBlank() }
                val matches = allowed.any { allowedValue -> allowedValue.equals(raw, ignoreCase = true) }
                if (!matches) {
                    val display = child.options.joinToString(", ") { option -> option.value.ifBlank { option.label } }
                    return "$title 必须是 $display 之一"
                }
            }
            val maxCount = child.maxInputCount
            if (child.isUploadField && maxCount != null && child.inputValues(raw).size > maxCount) {
                return "$title 最多支持 $maxCount 个文件"
            }
        }
        return null
    }

    private fun QuickCreationServiceModel.buildImageRequest(values: Map<String, String>): ImageGenerationRequest {
        val scalarParams = mutableMapOf<String, String>()
        val listParams = mutableMapOf<String, List<String>>()
        fields.filter { it.visible }.forEach { field ->
            val raw = values.rawValueFor(field).takeIf { it.isNotBlank() } ?: return@forEach
            if (field.isPromptField) return@forEach
            if (field.isUploadField || field.multipleInputs) {
                val inputValues = field.inputValues(raw)
                if (inputValues.isNotEmpty()) {
                    listParams[field.paramKey] = inputValues
                }
            } else {
                scalarParams[field.paramKey] = raw
            }
        }
        activeInputChildren(values).forEach { child ->
            val raw = values.rawValueFor(child).takeIf { it.isNotBlank() } ?: return@forEach
            if (child.isUploadField) {
                val inputValues = child.inputValues(raw)
                if (inputValues.isNotEmpty()) {
                    listParams[child.paramKey] = inputValues
                }
            } else {
                scalarParams[child.paramKey] = raw
            }
        }

        val aspectRatio = scalarParams["aspectRatio"] ?: scalarParams["ratio"] ?: "1:1"
        val resolution = scalarParams["resolution"] ?: "2k"
        val quality = scalarParams["quality"] ?: "medium"
        val imageUrls = listParams["imageUrls"] ?: listParams["imageUrl"].orEmpty()

        return ImageGenerationRequest(
            prompt = promptValue(values),
            model = identityKey,
            aspectRatio = aspectRatio,
            resolution = resolution,
            quality = quality,
            referenceImageUri = imageUrls.firstOrNull(),
            quickCreationCategoryId = categoryId,
            quickCreationBindingId = bindingId,
            quickCreationSkuId = skuId,
            quickCreationParams = scalarParams,
            quickCreationListParams = listParams,
        )
    }

    private fun QuickCreationServiceModel.promptValue(values: Map<String, String>): String {
        val promptField = fields.firstOrNull { it.visible && it.isPromptField }
        return if (promptField != null) {
            values.rawValueFor(promptField)
        } else {
            values["prompt"].orEmpty()
        }
    }

    private val QuickCreationServiceModel.identityKey: String
        get() = "$bindingId:$skuId"

    private fun QuickCreationServiceModel.sameIdentity(other: QuickCreationServiceModel): Boolean =
        bindingId == other.bindingId && skuId == other.skuId

    private fun QuickCreationServiceModel.activeInputChildren(values: Map<String, String>): List<QuickCreationServiceFieldInputChild> =
        fields
            .filter { it.visible }
            .flatMap { field ->
                field.inputExtra?.inputChildren.orEmpty()
                    .filter { child -> child.visible && child.isActive(values) }
            }

    private fun QuickCreationServiceFieldInputChild.isActive(values: Map<String, String>): Boolean {
        val condition = visibleWhen ?: return true
        val actual = values[condition.fieldKey].orEmpty()
        return condition.values.any { expected -> expected.equals(actual, ignoreCase = true) }
    }

    private val QuickCreationServiceField.isPromptField: Boolean
        get() {
            val text = listOf(fieldKey, paramKey, inputExtra?.title, inputExtra?.titleEn)
                .filterNotNull()
                .joinToString(" ")
                .lowercase()
            return text.contains("prompt") || text.contains("提示词")
        }

    private val QuickCreationServiceField.isUploadField: Boolean
        get() {
            if (isPromptField) return false
            val normalizedParam = paramKey.ifBlank { fieldKey }.lowercase()
            val type = fieldType.lowercase()
            val key = fieldKey.lowercase()
            return type.contains("upload") ||
                type.contains("file") ||
                key.contains("upload") ||
                normalizedParam in uploadParamKeys ||
                uploadParamKeys.any { normalizedParam.endsWith(it) }
        }

    private val QuickCreationServiceField.displayTitle: String
        get() = commonUploadTitle
            ?: inputExtra?.title?.takeIf { it.isNotBlank() }
            ?: inputExtra?.titleEn?.takeIf { it.isNotBlank() }
            ?: fieldKey

    private val QuickCreationServiceField.commonUploadTitle: String?
        get() = when (paramKey.ifBlank { fieldKey }.lowercase()) {
            "imageurls", "imageurl" -> "参考图"
            "videourls", "videourl" -> "参考视频"
            "audiourls", "audiourl" -> "参考音频"
            else -> null
        }

    private fun Map<String, String>.rawValueFor(field: QuickCreationServiceField): String =
        this[field.fieldKey] ?: this[field.paramKey] ?: field.defaultValue.orEmpty()

    private fun Map<String, String>.rawValueFor(child: QuickCreationServiceFieldInputChild): String =
        this[child.fieldKey] ?: this[child.paramKey] ?: child.defaultValue.orEmpty()

    private fun QuickCreationServiceField.inputValues(raw: String): List<String> =
        raw.split(Regex("""[\n,]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private val QuickCreationServiceFieldInputChild.isUploadField: Boolean
        get() {
            val text = listOf(fieldType, fieldKey, paramKey).joinToString(" ").lowercase()
            if (text.contains("prompt") || text.contains("提示词")) return false
            val normalizedParam = paramKey.ifBlank { fieldKey }.lowercase()
            val type = fieldType.lowercase()
            val key = fieldKey.lowercase()
            return type.contains("upload") ||
                type.contains("file") ||
                key.contains("upload") ||
                normalizedParam in uploadParamKeys ||
                uploadParamKeys.any { normalizedParam.endsWith(it) }
        }

    private val QuickCreationServiceFieldInputChild.displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: fieldKey

    private fun QuickCreationServiceFieldInputChild.inputValues(raw: String): List<String> =
        raw.split(Regex("""[\n,]"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun defaultUploadFileName(field: QuickCreationServiceField): String =
        when {
            field.uploadMediaMarker.contains("video") -> "upload.mp4"
            field.uploadMediaMarker.contains("audio") -> "upload.mp3"
            else -> "upload.png"
        }

    private fun inferMimeType(fileName: String, field: QuickCreationServiceField): String {
        val lowerName = fileName.lowercase()
        return when {
            lowerName.endsWith(".png") -> "image/png"
            lowerName.endsWith(".webp") -> "image/webp"
            lowerName.endsWith(".gif") -> "image/gif"
            lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") -> "image/jpeg"
            lowerName.endsWith(".mp4") -> "video/mp4"
            lowerName.endsWith(".mov") -> "video/quicktime"
            lowerName.endsWith(".webm") -> "video/webm"
            lowerName.endsWith(".mp3") -> "audio/mpeg"
            lowerName.endsWith(".m4a") -> "audio/mp4"
            lowerName.endsWith(".wav") -> "audio/wav"
            lowerName.endsWith(".aac") -> "audio/aac"
            lowerName.endsWith(".ogg") -> "audio/ogg"
            field.uploadMediaMarker.contains("video") -> "video/mp4"
            field.uploadMediaMarker.contains("audio") -> "audio/mpeg"
            else -> "image/jpeg"
        }
    }

    private val QuickCreationServiceField.maxUploadBytes: Long?
        get() = maxUploadSize?.let { size ->
            if (size in 1L until 1024L) size * 1024L * 1024L else size
        }

    private val QuickCreationServiceField.uploadMediaMarker: String
        get() = listOf(fieldType, fieldKey, paramKey, inputExtra?.title, inputExtra?.titleEn)
            .filterNotNull()
            .joinToString(" ")
            .lowercase()

    private fun Throwable.toCatalogErrorMessage(): String {
        val text = message.orEmpty()
        return if (
            text.looksLikeAuthError()
        ) {
            "登录后可同步真实快捷创作模型目录"
        } else {
            message?.takeIf { it.isNotBlank() } ?: "模型目录加载失败"
        }
    }

    private fun Throwable.toFeePreviewErrorMessage(): String {
        val text = message.orEmpty()
        return if (text.looksLikeAuthError()) {
            "登录后可确认价格"
        } else {
            text.toSafeUiMessage("价格预览失败")
        }
    }

    private fun Throwable.toHistoryErrorMessage(): String {
        val text = message.orEmpty()
        return if (text.looksLikeAuthError()) {
            "登录后可同步历史记录"
        } else {
            text.toSafeUiMessage("历史记录加载失败")
        }
    }

    private fun String.looksLikeAuthError(): Boolean =
        contains("TOKEN", ignoreCase = true) ||
            contains("UNAUTHORIZED", ignoreCase = true) ||
            contains("FORBIDDEN", ignoreCase = true) ||
            Regex("(?i)(HTTP|CODE|STATUS)\\s*[:=]?\\s*(401|403|412)").containsMatchIn(this)

    private fun String.toSafeUiMessage(fallback: String): String =
        takeIf { it.isNotBlank() }
            ?.replace(Regex("(?i)bearer\\s+[A-Za-z0-9._-]+"), "Bearer <redacted>")
            ?.replace(Regex("(?i)(token|cookie)=([^\\s,;]+)"), "$1=<redacted>")
            ?.take(160)
            ?: fallback
}

private const val HISTORY_REFRESH_INTERVAL_MS = 5_000L

private val terminalQuickCreationHistoryStatuses = setOf(
    "SUCCESS",
    "COMPLETED",
    "DONE",
    "FAILED",
    "FAIL",
    "FAILURE",
    "ERROR",
    "CANCELED",
    "CANCELLED",
)

private val QuickCreationHistoryItem.needsHistoryRefresh: Boolean
    get() = status.isNotBlank() && status.uppercase() !in terminalQuickCreationHistoryStatuses

internal fun List<QuickCreationHistoryItem>.filterByCreateHistoryFilter(
    filter: CreateHistoryFilter,
): List<QuickCreationHistoryItem> =
    this.filter { item ->
        when (filter) {
        CreateHistoryFilter.ALL -> true
        CreateHistoryFilter.IMAGE -> item.isImageHistory
        CreateHistoryFilter.VIDEO -> item.isVideoHistory
        CreateHistoryFilter.RUNNING -> item.status.uppercase() in runningQuickCreationHistoryStatuses
        CreateHistoryFilter.SUCCESS -> item.status.uppercase() in successQuickCreationHistoryStatuses
    }
    }

private val runningQuickCreationHistoryStatuses = setOf(
    "SUBMITTING",
    "QUEUED",
    "QUEUEING",
    "QUEUING",
    "RUNNING",
    "PROCESSING",
    "IN_PROGRESS",
)

private val successQuickCreationHistoryStatuses = setOf(
    "SUCCESS",
    "COMPLETED",
    "DONE",
)

private val QuickCreationHistoryItem.isImageHistory: Boolean
    get() = categoryId.equals("IMAGE", ignoreCase = true) || outputs.any { it.isImage }

private val QuickCreationHistoryItem.isVideoHistory: Boolean
    get() = categoryId.equals("VIDEO", ignoreCase = true) || outputs.any { it.isVideo }

private val uploadParamKeys = setOf(
    "imageurl",
    "imageurls",
    "videourl",
    "videourls",
    "audiourl",
    "audiourls",
    "fileurl",
    "fileurls",
)
