package com.runninghub.app.ui.feature.quickcreate

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplateDetail
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationHistoryPage
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.VideoGenerationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

data class DraftData(
    val currentTab: String = "IMAGE",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
)

private val draftJson = Json { encodeDefaults = true }
private const val HISTORY_REFRESH_INTERVAL_MS = 5_000L
private const val FEE_PREVIEW_DEBOUNCE_MS = 500L
private const val PROJECT_CREATE_MUTATION_ID = "__create_project__"
private val terminalHistoryStatuses = setOf("SUCCESS", "FAILED", "FAIL", "ERROR", "CANCELED", "CANCELLED")
private val QuickCreationHistoryItem.needsHistoryRefresh: Boolean
    get() = status.isNotBlank() && status.uppercase() !in terminalHistoryStatuses

private fun DraftData.toJsonString(): String =
    buildJsonObject {
        put("currentTab", currentTab)
        put("imagePrompt", imagePrompt)
        put("videoPrompt", videoPrompt)
    }.toString()

private fun parseDraftData(raw: String): DraftData {
    val json = draftJson.parseToJsonElement(raw).jsonObject
    return DraftData(
        currentTab = json["currentTab"]?.jsonPrimitive?.contentOrNull ?: "IMAGE",
        imagePrompt = json["imagePrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        videoPrompt = json["videoPrompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    )
}

class QuickCreateScreenModel(
    private val quickCreateRepository: QuickCreateRepository,
    private val mediaResolver: MediaResolver,
    private val settingsRepository: SettingsRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(QuickCreateUiState())
    val uiState: StateFlow<QuickCreateUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private val uploadJobs = mutableMapOf<String, Job>()
    private var draftSaveJob: Job? = null
    private var historyRefreshJob: Job? = null
    private var feePreviewJob: Job? = null

    /** Check if a draft exists and returns its content.
     *  TODO: These properties are non-reactive and currently have no UI consumers.
     *  Consider moving draft state into QuickCreateUiState for reactive UI binding. */
    var hasDraft: Boolean = false
        private set
    var draftData: DraftData? = null
        private set

    init {
        loadServiceModels()
        loadQuickCreationHistory()
        loadQuickCreationProjects()
    }

    fun loadServiceModels() {
        screenModelScope.launch {
            _uiState.update { it.copy(serviceModelsLoading = true) }

            val imageModels = quickCreateRepository.getModels("IMAGE")
            val videoModels = quickCreateRepository.getModels("VIDEO")

            _uiState.update { state ->
                val images = imageModels.getOrElse { emptyList() }
                val videos = videoModels.getOrElse { emptyList() }
                val selectedImage = state.selectedImageServiceModel
                    ?.takeIf { selected -> images.any { it.matchesServiceIdentity(selected) } }
                    ?: images.firstOrNull()
                val selectedVideo = state.selectedVideoServiceModel
                    ?.takeIf { selected -> videos.any { it.matchesServiceIdentity(selected) } }
                    ?: videos.firstOrNull()
                state.copy(
                    serviceModelsLoading = false,
                    serviceImageModels = images,
                    serviceVideoModels = videos,
                    selectedImageServiceModel = selectedImage,
                    selectedVideoServiceModel = selectedVideo,
                    imageServiceParams = if (hasSameServiceIdentity(selectedImage, state.selectedImageServiceModel)) {
                        state.imageServiceParams
                    } else {
                        selectedImage.defaultServiceParams()
                    },
                    videoServiceParams = if (hasSameServiceIdentity(selectedVideo, state.selectedVideoServiceModel)) {
                        state.videoServiceParams
                    } else {
                        selectedVideo.defaultServiceParams()
                    },
                )
            }
            scheduleFeePreview()
        }
    }

    fun loadQuickCreationHistory() {
        screenModelScope.launch {
            _uiState.update { it.copy(historyLoading = true) }
            val history = quickCreateRepository.listQuickCreationHistory(page = 1, size = 10)
            history.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        state.copy(
                            historyLoading = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = page.items.size < page.total,
                            historyItems = page.items,
                        )
                    }
                    updateHistoryRefreshJob(page.items)
                },
                onFailure = {
                    _uiState.update { state -> state.copy(historyLoading = false) }
                },
            )
        }
    }

    fun loadQuickCreationProjects() {
        screenModelScope.launch {
            _uiState.update { it.copy(projectsLoading = true) }
            val projects = quickCreateRepository.listQuickCreationProjects(page = 1, size = 20)
            projects.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        state.copy(
                            projectsLoading = false,
                            projects = page.items,
                            projectsPage = page.page,
                            projectsHasMore = page.hasNext,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { state -> state.copy(projectsLoading = false) }
                },
            )
        }
    }

    fun loadMoreQuickCreationHistory() {
        val state = _uiState.value
        if (state.historyLoading || state.historyLoadingMore || !state.historyHasMore) return

        screenModelScope.launch {
            val nextPage = _uiState.value.historyPage + 1
            _uiState.update { it.copy(historyLoadingMore = true) }
            val selectedProjectId = _uiState.value.selectedProjectId
            val history = if (selectedProjectId.isNullOrBlank()) {
                quickCreateRepository.listQuickCreationHistory(page = nextPage, size = 10)
            } else {
                quickCreateRepository.listQuickCreationProjectTasks(
                    projectId = selectedProjectId,
                    page = nextPage,
                    size = 10,
                )
            }
            history.fold(
                onSuccess = { page ->
                    var mergedItems: List<QuickCreationHistoryItem> = emptyList()
                    _uiState.update { current ->
                        val merged = (current.historyItems + page.items).distinctBy { it.taskId }
                        mergedItems = merged
                        current.copy(
                            historyLoadingMore = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = merged.size < page.total,
                            historyItems = merged,
                        )
                    }
                    updateHistoryRefreshJob(mergedItems)
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            historyLoadingMore = false,
                            error = error.message ?: "历史加载失败",
                        )
                    }
                },
            )
        }
    }

    fun selectProject(projectId: String) {
        if (projectId.isBlank() || _uiState.value.selectedProjectId == projectId) return

        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedProjectId = projectId,
                    projectTasksLoading = true,
                    historyItems = emptyList(),
                    historyPage = 0,
                    historyTotal = 0,
                    historyHasMore = false,
                    error = null,
                )
            }
            val tasks = quickCreateRepository.listQuickCreationProjectTasks(
                projectId = projectId,
                page = 1,
                size = 10,
            )
            tasks.fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        state.copy(
                            projectTasksLoading = false,
                            historyPage = page.page,
                            historyTotal = page.total,
                            historyHasMore = page.items.size < page.total,
                            historyItems = page.items,
                        )
                    }
                    updateHistoryRefreshJob(page.items)
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            projectTasksLoading = false,
                            error = error.message ?: "椤圭洰浠诲姟鍔犺浇澶辫触",
                        )
                    }
                },
            )
        }
    }

    fun clearSelectedProject() {
        if (_uiState.value.selectedProjectId == null) return
        _uiState.update {
            it.copy(
                selectedProjectId = null,
                projectTasksLoading = false,
                historyItems = emptyList(),
                historyPage = 0,
                historyTotal = 0,
                historyHasMore = false,
            )
        }
        loadQuickCreationHistory()
    }

    fun toggleProjectPin(projectId: String) {
        val project = _uiState.value.projects.firstOrNull { it.projectId == projectId } ?: return
        if (projectId in _uiState.value.projectPinningIds) return

        val targetPinned = !project.pinned
        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(projectPinningIds = state.projectPinningIds + projectId)
            }
            val result = quickCreateRepository.pinQuickCreationProject(projectId = projectId, pinned = targetPinned)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            projectPinningIds = state.projectPinningIds - projectId,
                            projects = state.projects.map { item ->
                                if (item.projectId == projectId) item.copy(pinned = targetPinned) else item
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            projectPinningIds = state.projectPinningIds - projectId,
                            error = error.message ?: "项目置顶失败",
                        )
                    }
                },
            )
        }
    }

    fun createProject(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val mutationId = PROJECT_CREATE_MUTATION_ID
        if (mutationId in _uiState.value.projectMutatingIds) return

        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + mutationId)
            }
            val result = quickCreateRepository.createQuickCreationProject(trimmedName)
            result.fold(
                onSuccess = { project ->
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - mutationId,
                            projects = (listOf(project) + state.projects)
                                .distinctBy { it.projectId },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - mutationId,
                            error = error.message ?: "项目创建失败",
                        )
                    }
                },
            )
        }
    }

    fun renameProject(projectId: String, name: String) {
        val trimmedName = name.trim()
        if (projectId.isBlank() || trimmedName.isBlank()) return
        if (projectId in _uiState.value.projectMutatingIds) return

        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + projectId)
            }
            val result = quickCreateRepository.renameQuickCreationProject(
                projectId = projectId,
                name = trimmedName,
            )
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            projects = state.projects.map { project ->
                                if (project.projectId == projectId) project.copy(name = trimmedName) else project
                            },
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            error = error.message ?: "项目重命名失败",
                        )
                    }
                },
            )
        }
    }

    fun deleteProject(projectId: String) {
        if (projectId.isBlank()) return
        if (projectId in _uiState.value.projectMutatingIds) return

        val wasSelected = _uiState.value.selectedProjectId == projectId
        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(projectMutatingIds = state.projectMutatingIds + projectId)
            }
            val result = quickCreateRepository.deleteQuickCreationProject(projectId)
            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            projects = state.projects.filterNot { it.projectId == projectId },
                            selectedProjectId = if (wasSelected) null else state.selectedProjectId,
                            projectTasksLoading = if (wasSelected) false else state.projectTasksLoading,
                            historyItems = if (wasSelected) emptyList() else state.historyItems,
                            historyPage = if (wasSelected) 0 else state.historyPage,
                            historyTotal = if (wasSelected) 0 else state.historyTotal,
                            historyHasMore = if (wasSelected) false else state.historyHasMore,
                        )
                    }
                    if (wasSelected) {
                        loadQuickCreationHistory()
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            projectMutatingIds = state.projectMutatingIds - projectId,
                            error = error.message ?: "项目删除失败",
                        )
                    }
                },
            )
        }
    }

    fun selectProjectDetail(projectId: String) {
        if (projectId.isBlank()) return

        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    projectDetailLoading = true,
                    selectedProjectDetail = null,
                    error = null,
                )
            }
            val detail = quickCreateRepository.getQuickCreationProjectDetail(projectId)
            detail.fold(
                onSuccess = { project ->
                    _uiState.update {
                        it.copy(
                            projectDetailLoading = false,
                            selectedProjectDetail = project,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            projectDetailLoading = false,
                            error = error.message ?: "项目详情加载失败",
                        )
                    }
                },
            )
        }
    }

    fun dismissProjectDetail() {
        _uiState.update {
            it.copy(
                projectDetailLoading = false,
                selectedProjectDetail = null,
            )
        }
    }

    private fun updateHistoryRefreshJob(items: List<QuickCreationHistoryItem>) {
        if (items.none { it.needsHistoryRefresh }) {
            historyRefreshJob?.cancel()
            historyRefreshJob = null
            return
        }
        if (historyRefreshJob?.isActive == true) return

        historyRefreshJob = screenModelScope.launch {
            while (_uiState.value.historyItems.any { it.needsHistoryRefresh }) {
                delay(HISTORY_REFRESH_INTERVAL_MS)
                refreshLoadedQuickCreationHistory()
            }
            historyRefreshJob = null
        }
    }

    private suspend fun refreshLoadedQuickCreationHistory() {
        val size = maxOf(10, _uiState.value.historyItems.size)
        val selectedProjectId = _uiState.value.selectedProjectId
        val history = if (selectedProjectId.isNullOrBlank()) {
            quickCreateRepository.listQuickCreationHistory(page = 1, size = size)
        } else {
            quickCreateRepository.listQuickCreationProjectTasks(
                projectId = selectedProjectId,
                page = 1,
                size = size,
            )
        }
        history.fold(
            onSuccess = { page -> applyHistoryRefreshPage(page) },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(error = error.message ?: "历史刷新失败")
                }
            },
        )
    }

    private fun applyHistoryRefreshPage(page: QuickCreationHistoryPage) {
        _uiState.update { state ->
            state.copy(
                historyTotal = page.total,
                historyHasMore = page.items.size < page.total,
                historyItems = page.items,
            )
        }
        updateHistoryRefreshJob(page.items)
    }

    fun cancelHistoryTask(taskId: String) {
        if (taskId.isBlank() || taskId in _uiState.value.historyCancellingTaskIds) return

        screenModelScope.launch {
            _uiState.update { state ->
                state.copy(historyCancellingTaskIds = state.historyCancellingTaskIds + taskId)
            }
            val result = quickCreateRepository.cancelQuickCreationTask(taskId)
            result.fold(
                onSuccess = {
                    refreshLoadedQuickCreationHistory()
                    _uiState.update { state ->
                        state.copy(historyCancellingTaskIds = state.historyCancellingTaskIds - taskId)
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            historyCancellingTaskIds = state.historyCancellingTaskIds - taskId,
                            error = error.message ?: "取消任务失败",
                        )
                    }
                },
            )
        }
    }

    fun selectHistoryOutput(outputId: String) {
        screenModelScope.launch {
            _uiState.update {
                it.copy(
                    historyDetailLoading = true,
                    selectedHistoryDetail = null,
                )
            }
            val detail = quickCreateRepository.getQuickCreationHistoryDetail(outputId)
            _uiState.update { state ->
                detail.fold(
                    onSuccess = { item ->
                        state.copy(
                            historyDetailLoading = false,
                            selectedHistoryDetail = item,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            historyDetailLoading = false,
                            error = error.message ?: "历史详情加载失败",
                        )
                    },
                )
            }
        }
    }

    fun dismissHistoryDetail() {
        _uiState.update {
            it.copy(
                historyDetailLoading = false,
                selectedHistoryDetail = null,
            )
        }
    }

    fun checkForDraft() {
        screenModelScope.launch {
            val raw = settingsRepository.getQuickCreateDraft()
            if (!raw.isNullOrEmpty()) {
                try {
                    val draft = parseDraftData(raw)
                    draftData = draft
                    hasDraft = true
                } catch (_: Exception) {
                    settingsRepository.clearQuickCreateDraft()
                }
            }
        }
    }

    fun restoreDraft() {
        val draft = draftData ?: return
        if (draft.imagePrompt.isNotEmpty()) {
            _uiState.update { it.copy(imageConfig = it.imageConfig.copy(prompt = draft.imagePrompt)) }
        }
        if (draft.videoPrompt.isNotEmpty()) {
            _uiState.update { it.copy(videoConfig = it.videoConfig.copy(prompt = draft.videoPrompt)) }
        }
        if (draft.currentTab == "VIDEO") {
            _uiState.update { it.copy(currentTab = QuickCreateTab.VIDEO) }
        }
        clearDraft()
    }

    fun discardDraft() {
        clearDraft()
    }

    private fun clearDraft() {
        hasDraft = false
        draftData = null
        screenModelScope.launch { settingsRepository.clearQuickCreateDraft() }
    }

    // ── Tab & Prompt ──────────────────────────────────────────────────────────

    fun switchMode(mode: QuickCreateMode) {
        _uiState.update {
            it.copy(
                currentMode = mode,
                tuneSheetVisible = if (mode == QuickCreateMode.CREATION) it.tuneSheetVisible else false,
            )
        }
        if (mode == QuickCreateMode.INSPIRATION && _uiState.value.inspirationTemplates.isEmpty()) {
            loadInspiration()
        }
    }

    fun loadInspiration() {
        screenModelScope.launch {
            _uiState.update { it.copy(inspirationLoading = true, error = null) }

            val tagsResult = quickCreateRepository.getInspirationTags()
            val templatesResult = quickCreateRepository.getInspirationTemplates()

            _uiState.update { state ->
                val tags = tagsResult.getOrElse { emptyList() }
                val templates = templatesResult.getOrElse { emptyList() }
                val error = tagsResult.exceptionOrNull()?.message
                    ?: templatesResult.exceptionOrNull()?.message

                state.copy(
                    inspirationLoading = false,
                    inspirationTags = tags,
                    inspirationTemplates = templates,
                    error = error,
                )
            }
        }
    }

    fun applyInspirationTemplate(templateId: String) {
        if (templateId.isBlank()) return
        screenModelScope.launch {
            _uiState.update { it.copy(inspirationLoading = true, error = null) }
            val result = quickCreateRepository.getInspirationTemplateDetail(templateId)
            result.fold(
                onSuccess = { detail ->
                    _uiState.update { state ->
                        state.applyTemplateDetail(detail)
                    }
                    scheduleFeePreview()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            inspirationLoading = false,
                            error = error.message ?: "模板详情加载失败",
                        )
                    }
                },
            )
        }
    }

    fun switchTab(tab: QuickCreateTab) {
        val cost = when (tab) {
            QuickCreateTab.IMAGE -> _uiState.value.imageConfig.estimatedCost
            QuickCreateTab.VIDEO -> _uiState.value.videoConfig.estimatedCost
        }
        _uiState.update {
            it.copy(
                currentTab = tab,
                estimatedCost = cost,
                feePreviewLoading = false,
                feePreviewError = null,
            )
        }
        scheduleFeePreview()
        autoSaveDraft()
    }

    fun updateImagePrompt(prompt: String) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(prompt = prompt)) }
        scheduleFeePreview()
        autoSaveDraft()
    }

    fun updateVideoPrompt(prompt: String) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(prompt = prompt)) }
        scheduleFeePreview()
        autoSaveDraft()
    }

    /** Debounced auto-save (500ms after last keystroke). */
    private fun autoSaveDraft() {
        draftSaveJob?.cancel()
        draftSaveJob = screenModelScope.launch {
            delay(500)
            val state = _uiState.value
            val draft = DraftData(
                currentTab = state.currentTab.name,
                imagePrompt = state.imageConfig.prompt,
                videoPrompt = state.videoConfig.prompt,
            )
            settingsRepository.saveQuickCreateDraft(draft.toJsonString())
        }
    }

    // ── Tune Sheet ───────────────────────────────────────────────────────────

    fun setTuneSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(tuneSheetVisible = visible) }
    }

    // ── Parameters ────────────────────────────────────────────────────────────

    fun updateImageModel(model: ImageModel) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(
                model = model,
                aspectRatio = model.defaultAspectRatio,
                resolution = model.defaultResolution,
                quality = model.defaultQuality,
            )
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        _uiState.update {
            it.copy(
                selectedImageServiceModel = model,
                imageServiceParams = model.defaultServiceParams(),
            )
        }
        scheduleFeePreview()
    }

    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        _uiState.update {
            it.copy(
                selectedVideoServiceModel = model,
                videoServiceParams = model.defaultServiceParams(),
            )
        }
        scheduleFeePreview()
    }

    fun updateImageServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        val selectedModel = _uiState.value.selectedImageServiceModel
        if (selectedModel?.hasFieldParam(paramKey) != true) return
        val nextParams = _uiState.value.imageServiceParams + (paramKey to value)
        _uiState.update {
            it.copy(imageServiceParams = nextParams)
        }
        if (paramKey in selectedModel.activeServiceParamKeys(nextParams)) {
            scheduleFeePreview()
        }
    }

    fun updateVideoServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        val selectedModel = _uiState.value.selectedVideoServiceModel
        if (selectedModel?.hasFieldParam(paramKey) != true) return
        val nextParams = _uiState.value.videoServiceParams + (paramKey to value)
        _uiState.update {
            it.copy(videoServiceParams = nextParams)
        }
        if (paramKey in selectedModel.activeServiceParamKeys(nextParams)) {
            scheduleFeePreview()
        }
    }

    fun updateVideoModel(model: VideoModel) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(
                model = model,
                aspectRatio = model.defaultAspectRatio,
                resolution = model.defaultResolution,
                duration = model.defaultDuration,
                generateAudio = model.supportsGenerateAudio && it.videoConfig.generateAudio,
                realisticMode = model.supportsRealistic && it.videoConfig.realisticMode,
            )
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(aspectRatio = ratio)) }
        scheduleFeePreview()
    }

    fun updateImageResolution(res: ImageResolution) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(resolution = res)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateImageQuality(quality: ImageQuality) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(quality = quality)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateImageCount(count: Int) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(count = count)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateImageSeed(seed: Int?) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(seed = seed)) }
        scheduleFeePreview()
    }

    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(aspectRatio = ratio)) }
        scheduleFeePreview()
    }

    fun updateVideoResolution(res: VideoResolution) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(resolution = res)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateVideoDuration(duration: VideoDuration) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(duration = duration)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateVideoCount(count: Int) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(count = count)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    fun updateVideoSeed(seed: Int?) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(seed = seed)) }
        scheduleFeePreview()
    }

    fun toggleRealisticMode() {
        _uiState.update {
            it.copy(videoConfig = it.videoConfig.copy(realisticMode = !it.videoConfig.realisticMode))
        }
        scheduleFeePreview()
    }

    fun toggleGenerateAudio() {
        _uiState.update {
            val newConfig = it.videoConfig.copy(generateAudio = !it.videoConfig.generateAudio)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
        scheduleFeePreview()
    }

    // ── Media References ─────────────────────────────────────────────────────

    fun pickImageReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.IMAGE, fieldParamKey = null)
    }

    fun pickVideoReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.VIDEO, fieldParamKey = null)
    }

    fun pickAudioReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.AUDIO, fieldParamKey = null)
    }

    fun pickImageReferenceForField(uriString: String, fieldParamKey: String) {
        if (uriString.isBlank() || fieldParamKey.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.IMAGE, fieldParamKey = fieldParamKey)
    }

    fun pickVideoReferenceForField(uriString: String, fieldParamKey: String) {
        if (uriString.isBlank() || fieldParamKey.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.VIDEO, fieldParamKey = fieldParamKey)
    }

    fun pickAudioReferenceForField(uriString: String, fieldParamKey: String) {
        if (uriString.isBlank() || fieldParamKey.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.AUDIO, fieldParamKey = fieldParamKey)
    }

    private fun addMediaReference(
        uriString: String,
        type: QuickCreateMediaType,
        fieldParamKey: String?,
    ) {
        val TAG = "QCScreenModel"
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "${type.name}_$now"
        val targetTab = _uiState.value.currentTab
        debug(TAG, "addMediaReference: uri=$uriString type=$type")
        val fileName = mediaResolver.getDisplayName(uriString) ?: "${type.name.lowercase()}_$now"
        val fileSize = mediaResolver.getFileSizeBytes(uriString)
        debug(TAG, "addMediaReference: displayName=$fileName size=$fileSize bytes")

        val newRef = MediaReference(
            id = id,
            type = type,
            uri = uriString,
            displayName = fileName,
            fileSizeBytes = fileSize,
            fieldParamKey = fieldParamKey,
            uploadStatus = UploadStatus.UPLOADING,
            uploadProgress = 0f,
        )
        _uiState.update { state ->
            if (targetTab == QuickCreateTab.IMAGE) {
                state.copy(
                    imageConfig = state.imageConfig.copy(
                        mediaReferences = state.imageConfig.mediaReferences + newRef
                    )
                )
            } else {
                state.copy(
                    videoConfig = state.videoConfig.copy(
                        mediaReferences = state.videoConfig.mediaReferences + newRef
                    )
                )
            }
        }

        uploadReference(id, uriString, type, fileName, targetTab)
    }

    private fun uploadReference(
        id: String,
        uriString: String,
        type: QuickCreateMediaType,
        fileName: String,
        targetTab: QuickCreateTab,
    ) {
        val TAG = "QCScreenModel"
        debug(TAG, "uploadReference: START")
        debug(TAG, "  id       = $id")
        debug(TAG, "  uri      = $uriString")
        debug(TAG, "  type     = $type")
        debug(TAG, "  fileName = $fileName")
        uploadJobs[id]?.cancel()
        uploadJobs[id] = screenModelScope.launch {
            val mimeType = when (type) {
                QuickCreateMediaType.IMAGE -> "image/jpeg"
                QuickCreateMediaType.VIDEO -> "video/mp4"
                QuickCreateMediaType.AUDIO -> "audio/mpeg"
            }
            val ext = when (type) {
                QuickCreateMediaType.IMAGE -> "jpg"
                QuickCreateMediaType.VIDEO -> "mp4"
                QuickCreateMediaType.AUDIO -> "mp3"
            }
            val actualFileName = "${type.name.lowercase()}_${Clock.System.now().toEpochMilliseconds()}.$ext"

            try {
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.1f, targetTab)
                debug(TAG, "uploadReference: reading bytes from URI...")
                val bytes = withContext(Dispatchers.IO) {
                    mediaResolver.readBytes(uriString)
                }
                debug(TAG, "uploadReference: read ${bytes.size} bytes")
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.7f, targetTab)

                debug(TAG, "uploadReference: calling repository.uploadMedia...")
                val remoteUrl = quickCreateRepository.uploadMedia(
                    fileBytes = bytes,
                    fileName = actualFileName,
                    mimeType = mimeType
                ).getOrElse { e ->
                    debug(TAG, "uploadReference: FAILED - ${e::class.simpleName}: ${e.message}")
                    throw e
                }

                debug(TAG, "uploadReference: SUCCESS, remoteUrl = $remoteUrl")
                updateReferenceStatus(id, UploadStatus.PROCESSING, 0.85f, targetTab)

                _uiState.update { state ->
                    if (targetTab == QuickCreateTab.IMAGE) {
                        state.copy(
                            imageConfig = state.imageConfig.copy(
                                mediaReferences = state.imageConfig.mediaReferences.map { ref ->
                                    if (ref.id == id) ref.copy(
                                        uploadStatus = UploadStatus.DONE,
                                        uploadProgress = 1f,
                                        remoteUrl = remoteUrl,
                                    ) else ref
                                }
                            )
                        )
                    } else {
                        state.copy(
                            videoConfig = state.videoConfig.copy(
                                mediaReferences = state.videoConfig.mediaReferences.map { ref ->
                                    if (ref.id == id) ref.copy(
                                        uploadStatus = UploadStatus.DONE,
                                        uploadProgress = 1f,
                                        remoteUrl = remoteUrl,
                                    ) else ref
                                }
                            )
                        )
                    }
                }
                scheduleFeePreviewForMediaReference(id)
            } catch (e: Exception) {
                debug(TAG, "uploadReference: CATCH - ${e::class.simpleName}: ${e.message}")
                _uiState.update { state ->
                    if (targetTab == QuickCreateTab.IMAGE) {
                        state.copy(
                            imageConfig = state.imageConfig.copy(
                                mediaReferences = state.imageConfig.mediaReferences.map { ref ->
                                    if (ref.id == id) ref.copy(uploadStatus = UploadStatus.FAILED) else ref
                                }
                            )
                        )
                    } else {
                        state.copy(
                            videoConfig = state.videoConfig.copy(
                                mediaReferences = state.videoConfig.mediaReferences.map { ref ->
                                    if (ref.id == id) ref.copy(uploadStatus = UploadStatus.FAILED) else ref
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scheduleFeePreviewForMediaReference(id: String) {
        if (_uiState.value.currentRelevantMediaReferences().any { it.id == id && it.affectsFeePreviewRequest() }) {
            scheduleFeePreview()
        }
    }

    private fun MediaReference.affectsFeePreviewRequest(): Boolean =
        uploadStatus == UploadStatus.DONE && !remoteUrl.isNullOrBlank()

    private fun updateReferenceStatus(
        id: String,
        status: UploadStatus,
        progress: Float,
        targetTab: QuickCreateTab,
    ) {
        _uiState.update { state ->
            if (targetTab == QuickCreateTab.IMAGE) {
                state.copy(
                    imageConfig = state.imageConfig.copy(
                        mediaReferences = state.imageConfig.mediaReferences.map { ref ->
                            if (ref.id == id) ref.copy(uploadStatus = status, uploadProgress = progress) else ref
                        }
                    )
                )
            } else {
                state.copy(
                    videoConfig = state.videoConfig.copy(
                        mediaReferences = state.videoConfig.mediaReferences.map { ref ->
                            if (ref.id == id) ref.copy(uploadStatus = status, uploadProgress = progress) else ref
                        }
                    )
                )
            }
        }
        scheduleFeePreviewForMediaReference(id)
    }

    fun removeMediaReference(id: String) {
        val shouldRefreshFeePreview = _uiState.value.currentRelevantMediaReferences()
            .any { it.id == id && it.affectsFeePreviewRequest() }
        uploadJobs[id]?.cancel()
        uploadJobs.remove(id)
        _uiState.update { state ->
            if (state.currentTab == QuickCreateTab.IMAGE) {
                state.copy(
                    imageConfig = state.imageConfig.copy(
                        mediaReferences = state.imageConfig.mediaReferences.filter { it.id != id }
                    )
                )
            } else {
                state.copy(
                    videoConfig = state.videoConfig.copy(
                        mediaReferences = state.videoConfig.mediaReferences.filter { it.id != id }
                    )
                )
            }
        }
        if (shouldRefreshFeePreview) {
            scheduleFeePreview()
        }
    }

    // ── Error / Results ───────────────────────────────────────────────────────

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearResults() {
        _uiState.update { it.copy(results = emptyList(), taskStatus = QuickCreateTaskUiStatus.IDLE) }
    }

    // ── Generate ──────────────────────────────────────────────────────────────

    fun generate() {
        if (_uiState.value.feePreviewLoading) {
            _uiState.update {
                it.copy(
                    taskStatus = QuickCreateTaskUiStatus.IDLE,
                    error = "价格确认中",
                )
            }
            return
        }
        if (_uiState.value.feePreviewError != null) {
            _uiState.update {
                it.copy(
                    taskStatus = QuickCreateTaskUiStatus.IDLE,
                    error = "价格待确认",
                )
            }
            return
        }
        validateCurrentServiceFields(_uiState.value)?.let { error ->
            _uiState.update {
                it.copy(
                    taskStatus = QuickCreateTaskUiStatus.IDLE,
                    error = error,
                )
            }
            return
        }
        generationJob?.cancel()
        generationJob = screenModelScope.launch {
            _uiState.update {
                it.copy(
                    taskStatus = QuickCreateTaskUiStatus.SUBMITTING,
                    statusText = "正在提交任务...",
                    error = null,
                    results = emptyList(),
                )
            }
            try {
                awaitPendingUploads()
            } catch (e: IllegalStateException) {
                _uiState.update {
                    it.copy(taskStatus = QuickCreateTaskUiStatus.IDLE, error = e.message)
                }
                return@launch
            }
            validateCurrentServiceUploads(_uiState.value)?.let { error ->
                _uiState.update {
                    it.copy(
                        taskStatus = QuickCreateTaskUiStatus.IDLE,
                        error = error,
                    )
                }
                return@launch
            }
            when (_uiState.value.currentTab) {
                QuickCreateTab.IMAGE -> generateImage()
                QuickCreateTab.VIDEO -> generateVideo()
            }
        }
    }

    private suspend fun awaitPendingUploads() {
        val mediaRefs = _uiState.value.currentRelevantMediaReferences()
        val pending = mediaRefs.filter { it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING }
        if (pending.isEmpty()) return

        _uiState.update { it.copy(statusText = "正在上传素材(${pending.size})...") }

        val pendingIds = pending.map { it.id }.toSet()
        var waited = 0
        while (waited < 120) {
            delay(500)
            waited++
            val stillPending = _uiState.value.let { state ->
                state.currentRelevantMediaReferences().filter {
                    it.id in pendingIds &&
                        (it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING)
                }
                    .map { it.id }
                    .toSet()
            }
            if (stillPending.isEmpty()) return
        }

        val failed = _uiState.value.let { state ->
            state.currentRelevantMediaReferences()
                .filter { it.id in pendingIds && it.uploadStatus == UploadStatus.FAILED }
        }
        if (failed.isNotEmpty()) {
            throw IllegalStateException("素材上传失败: ${failed.joinToString { it.displayName }}")
        }
    }

    private fun QuickCreateUiState.currentRelevantMediaReferences(): List<MediaReference> =
        when (currentTab) {
            QuickCreateTab.IMAGE -> imageConfig.mediaReferences.quickCreationRelevantMediaReferences(
                activeFieldParamKeys = selectedImageServiceModel.quickCreationActiveUploadParamKeys(imageServiceParams),
            )
            QuickCreateTab.VIDEO -> videoConfig.mediaReferences.quickCreationRelevantMediaReferences(
                activeFieldParamKeys = selectedVideoServiceModel.quickCreationActiveUploadParamKeys(videoServiceParams),
            )
        }

    private suspend fun generateImage() {
        val config = _uiState.value.imageConfig
        val prompt = config.prompt.trim()
        if (prompt.isEmpty() || config.promptOverLimit) {
            _uiState.update {
                it.copy(taskStatus = QuickCreateTaskUiStatus.IDLE, error = "请输入描述词")
            }
            return
        }

        quickCreateRepository.generateImage(
            buildImageGenerationRequest(_uiState.value, requirePrompt = true) ?: return
        ).collect { status ->
            handleTaskStatus(status)
        }
    }

    private fun scheduleFeePreview() {
        feePreviewJob?.cancel()
        if (!hasFeePreviewRequest(_uiState.value)) {
            _uiState.update {
                it.copy(
                    feePreviewLoading = false,
                    feePreviewError = null,
                    estimatedCost = if (it.currentTab == QuickCreateTab.IMAGE) {
                        it.imageConfig.estimatedCost
                    } else {
                        it.videoConfig.estimatedCost
                    },
                )
            }
            return
        }

        _uiState.update { it.copy(feePreviewLoading = true, feePreviewError = null) }
        feePreviewJob = screenModelScope.launch {
            delay(FEE_PREVIEW_DEBOUNCE_MS)
            if (_uiState.value.currentTab == QuickCreateTab.VIDEO) {
                val latestRequest = buildVideoGenerationRequest(_uiState.value, requirePrompt = true)
                if (latestRequest == null) {
                    clearFeePreviewState()
                    return@launch
                }
                quickCreateRepository.previewVideoQuickCreationFee(latestRequest).fold(
                    onSuccess = ::applyFeePreview,
                    onFailure = ::applyFeePreviewError,
                )
                return@launch
            }
            val latestRequest = buildImageGenerationRequest(_uiState.value, requirePrompt = true)
            if (latestRequest == null) {
                clearFeePreviewState()
                return@launch
            }

            quickCreateRepository.previewImageQuickCreationFee(latestRequest).fold(
                onSuccess = { preview ->
                    val previewCost = when {
                        preview.free -> 0.0
                        preview.requiredCashAmount > 0.0 -> preview.requiredCashAmount
                        else -> preview.requiredRhAmount
                    }
                    _uiState.update {
                        it.copy(
                            estimatedCost = previewCost,
                            feePreviewLoading = false,
                            feePreviewError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            feePreviewLoading = false,
                            feePreviewError = error.message ?: "价格预览失败",
                        )
                    }
                },
            )
        }
    }

    private fun hasFeePreviewRequest(state: QuickCreateUiState): Boolean =
        when (state.currentTab) {
            QuickCreateTab.IMAGE -> buildImageGenerationRequest(state, requirePrompt = true) != null
            QuickCreateTab.VIDEO -> buildVideoGenerationRequest(state, requirePrompt = true) != null
        }

    private fun clearFeePreviewState() {
        _uiState.update { it.copy(feePreviewLoading = false, feePreviewError = null) }
    }

    private fun applyFeePreview(
        preview: com.runninghub.shared.domain.repository.QuickCreationFeePreview,
    ) {
        val previewCost = when {
            preview.free -> 0.0
            preview.requiredCashAmount > 0.0 -> preview.requiredCashAmount
            else -> preview.requiredRhAmount
        }
        _uiState.update {
            it.copy(
                estimatedCost = previewCost,
                feePreviewLoading = false,
                feePreviewError = null,
            )
        }
    }

    private fun applyFeePreviewError(error: Throwable) {
        _uiState.update {
            it.copy(
                feePreviewLoading = false,
                feePreviewError = error.message ?: "价格预览失败",
            )
        }
    }

    private fun buildImageGenerationRequest(
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

    private fun buildVideoGenerationRequest(
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
            apiTier = config.model.apiTier.name,
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

    private fun imageQuickCreationParams(
        model: QuickCreationServiceModel?,
        config: ImageConfig,
        serviceParams: Map<String, String>,
    ): Map<String, String> =
        buildMap {
            putAll(model.defaultServiceParams())
            putAll(
                serviceParams
                    .filterKeys { key -> key in model.activeServiceParamKeys(serviceParams) }
                    .filterValues { it.isNotBlank() }
            )

            put("aspectRatio", config.aspectRatio.apiValue)
            put("resolution", config.resolution.apiValue)
            put("quality", config.quality.apiValue)
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
            putAll(model.defaultServiceParams())
            putAll(
                serviceParams
                    .filterKeys { key -> key in model.activeServiceParamKeys(serviceParams) }
                    .filterValues { it.isNotBlank() }
            )

            put("aspectRatio", config.aspectRatio.apiValue)
            put("resolution", config.resolution.apiValue)
            put("duration", config.duration.seconds.toString())
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
        val urlsByType = mediaReferences
            .filter { it.uploadStatus == UploadStatus.DONE }
            .filter { it.fieldParamKey.isNullOrBlank() }
            .groupBy { it.type }
            .mapValues { (_, refs) ->
                refs.mapNotNull { it.remoteUrl?.takeIf { url -> url.isNotBlank() } }
            }
        val urlsByField = mediaReferences
            .filter { it.uploadStatus == UploadStatus.DONE }
            .filter { !it.fieldParamKey.isNullOrBlank() }
            .groupBy { it.fieldParamKey.orEmpty() }
            .mapValues { (_, refs) ->
                refs.mapNotNull { it.remoteUrl?.takeIf { url -> url.isNotBlank() } }
            }

        return buildMap {
            model.uploadFields().forEach { field ->
                val mediaType = field.uploadMediaType() ?: fallbackMediaType ?: return@forEach
                val urls = urlsByField[field.paramKey].orEmpty().ifEmpty { urlsByType[mediaType].orEmpty() }
                if (urls.isNotEmpty()) {
                    val maxCount = field.maxUploadCount ?: urls.size
                    put(field.paramKey, urls.take(maxCount))
                }
            }
            model.activeChildUploadFields(serviceParams).forEach { child ->
                val mediaType = child.uploadMediaType() ?: fallbackMediaType ?: return@forEach
                val urls = urlsByField[child.paramKey].orEmpty().ifEmpty { urlsByType[mediaType].orEmpty() }
                if (urls.isNotEmpty()) {
                    val maxCount = child.maxInputCount ?: urls.size
                    put(child.paramKey, urls.take(maxCount))
                }
            }
        }
    }

    private fun QuickCreationServiceModel?.defaultServiceParams(): Map<String, String> =
        this?.fields.orEmpty()
            .filter { it.visible }
            .mapNotNull { field ->
                val value = field.defaultValue?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                field.paramKey to value
            }
            .toMap()

    private fun QuickCreationServiceModel.hasFieldParam(paramKey: String): Boolean =
        fields.any { field ->
            field.paramKey == paramKey ||
                field.inputExtra?.inputChildren.orEmpty().any { child -> child.paramKey == paramKey }
        }

    private fun QuickCreationServiceModel?.activeServiceParamKeys(
        serviceParams: Map<String, String>,
    ): Set<String> =
        this?.fields.orEmpty()
            .filter { it.visible }
            .flatMap { field ->
                listOf(field.paramKey) + field.quickCreationActiveInputChildren(serviceParams).map { it.paramKey }
            }
            .toSet()

    private fun validateCurrentServiceFields(state: QuickCreateUiState): String? =
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

    private fun validateServiceFields(
        model: QuickCreationServiceModel?,
        serviceParams: Map<String, String>,
    ): String? {
        val defaults = model.defaultServiceParams()
        return model?.fields.orEmpty()
            .filter { it.visible }
            .firstNotNullOfOrNull { field ->
                if (field.supportsQuickCreationTextEntry()) {
                    val value = serviceParams[field.paramKey] ?: defaults[field.paramKey].orEmpty()
                    field.quickCreationTextValidationError(value)?.let { return@firstNotNullOfOrNull it }
                }
                field.quickCreationActiveInputChildren(serviceParams)
                    .filter { it.supportsQuickCreationTextEntry() }
                    .firstNotNullOfOrNull { child ->
                        val value = serviceParams[child.paramKey] ?: child.defaultValue.orEmpty()
                        child.quickCreationTextValidationError(value)
                    }
            }
    }

    private fun validateCurrentServiceUploads(state: QuickCreateUiState): String? =
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

    private fun validateServiceUploads(
        model: QuickCreationServiceModel?,
        mediaReferences: List<MediaReference>,
        fallbackMediaType: QuickCreateMediaType?,
        serviceParams: Map<String, String>,
    ): String? {
        val urlsByType = mediaReferences
            .filter { it.uploadStatus == UploadStatus.DONE }
            .filter { it.fieldParamKey.isNullOrBlank() }
            .groupBy { it.type }
            .mapValues { (_, refs) ->
                refs.mapNotNull { it.remoteUrl?.takeIf { url -> url.isNotBlank() } }
            }
        val urlsByField = mediaReferences
            .filter { it.uploadStatus == UploadStatus.DONE }
            .filter { !it.fieldParamKey.isNullOrBlank() }
            .groupBy { it.fieldParamKey.orEmpty() }
            .mapValues { (_, refs) ->
                refs.mapNotNull { it.remoteUrl?.takeIf { url -> url.isNotBlank() } }
            }
        model.uploadFields()
            .firstNotNullOfOrNull { field ->
                val mediaType = field.uploadMediaType() ?: fallbackMediaType ?: return@firstNotNullOfOrNull null
                val uploadedCount = urlsByField[field.paramKey].orEmpty()
                    .ifEmpty { urlsByType[mediaType].orEmpty() }
                    .size
                field.quickCreationUploadValidationError(uploadedCount)
            }
            ?.let { return it }
        return model.activeChildUploadFields(serviceParams)
            .firstNotNullOfOrNull { child ->
                val mediaType = child.uploadMediaType() ?: fallbackMediaType ?: return@firstNotNullOfOrNull null
                val uploadedCount = urlsByField[child.paramKey].orEmpty()
                    .ifEmpty { urlsByType[mediaType].orEmpty() }
                    .size
                child.quickCreationUploadValidationError(uploadedCount)
            }
    }

    private fun QuickCreationServiceModel?.uploadFields(): List<QuickCreationServiceField> =
        this?.fields.orEmpty().filter { it.isQuickCreationServiceFieldRenderable() && it.isQuickCreationUploadField() }

    private fun QuickCreationServiceModel?.activeChildUploadFields(
        serviceParams: Map<String, String>,
    ): List<QuickCreationServiceFieldInputChild> =
        this?.fields.orEmpty()
            .filter { it.visible }
            .flatMap { field -> field.quickCreationActiveInputChildren(serviceParams) }
            .filter { it.isQuickCreationUploadField() }

    private fun QuickCreationServiceField.uploadMediaType(): QuickCreateMediaType? {
        return quickCreationUploadMediaType()
    }

    private fun QuickCreationServiceFieldInputChild.uploadMediaType(): QuickCreateMediaType? {
        return quickCreationUploadMediaType()
    }

    private fun QuickCreationServiceModel.matchesServiceIdentity(other: QuickCreationServiceModel?): Boolean =
        other != null && bindingId == other.bindingId && skuId == other.skuId

    private fun hasSameServiceIdentity(
        first: QuickCreationServiceModel?,
        second: QuickCreationServiceModel?,
    ): Boolean =
        first != null && first.matchesServiceIdentity(second)

    private fun QuickCreateUiState.applyTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val category = detail.categoryId?.uppercase()
        return when (category) {
            "VIDEO" -> applyVideoTemplateDetail(detail)
            else -> applyImageTemplateDetail(detail)
        }
    }

    private fun QuickCreateUiState.applyImageTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val selectedModel = serviceImageModels.matchTemplateModel(detail) ?: selectedImageServiceModel
        val nextConfig = imageConfig.copy(
            prompt = detail.prompt ?: imageConfig.prompt,
            aspectRatio = detail.params.templateImageAspectRatio() ?: imageConfig.aspectRatio,
            resolution = detail.params.templateImageResolution() ?: imageConfig.resolution,
            quality = detail.params.templateImageQuality() ?: imageConfig.quality,
            mediaReferences = detail.templateMediaReferences(),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.IMAGE,
            inspirationLoading = false,
            selectedImageServiceModel = selectedModel,
            imageConfig = nextConfig,
            imageServiceParams = selectedModel.defaultServiceParams() + detail.params,
            estimatedCost = nextConfig.estimatedCost,
        )
    }

    private fun QuickCreateUiState.applyVideoTemplateDetail(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreateUiState {
        val selectedModel = serviceVideoModels.matchTemplateModel(detail) ?: selectedVideoServiceModel
        val nextConfig = videoConfig.copy(
            prompt = detail.prompt ?: videoConfig.prompt,
            aspectRatio = detail.params.templateVideoAspectRatio() ?: videoConfig.aspectRatio,
            resolution = detail.params.templateVideoResolution() ?: videoConfig.resolution,
            duration = detail.params.templateVideoDuration() ?: videoConfig.duration,
            generateAudio = detail.params.templateBoolean("generateAudio") ?: videoConfig.generateAudio,
            realisticMode = detail.params.templateBoolean("realPersonMode") ?: videoConfig.realisticMode,
            mediaReferences = detail.templateMediaReferences(),
        )
        return copy(
            currentMode = QuickCreateMode.CREATION,
            currentTab = QuickCreateTab.VIDEO,
            inspirationLoading = false,
            selectedVideoServiceModel = selectedModel,
            videoConfig = nextConfig,
            videoServiceParams = selectedModel.defaultServiceParams() + detail.params,
            estimatedCost = nextConfig.estimatedCost,
        )
    }

    private fun List<QuickCreationServiceModel>.matchTemplateModel(
        detail: QuickCreateInspirationTemplateDetail,
    ): QuickCreationServiceModel? =
        firstOrNull { model ->
            (detail.bindingId != null && model.bindingId == detail.bindingId) ||
                (detail.skuId != null && model.skuId == detail.skuId)
        }

    private fun QuickCreateInspirationTemplateDetail.templateMediaReferences(): List<MediaReference> =
        listParams.flatMap { (key, values) ->
            val mediaType = key.templateMediaType()
            values.mapIndexed { index, url ->
                MediaReference(
                    id = "template_${templateId}_${mediaType.name}_$index",
                    type = mediaType,
                    uri = url,
                    displayName = url.substringAfterLast('/').ifBlank { "${mediaType.name.lowercase()}_$index" },
                    fileSizeBytes = 0L,
                    uploadStatus = UploadStatus.DONE,
                    uploadProgress = 1f,
                    remoteUrl = url,
                )
            }
        }

    private fun String.templateMediaType(): QuickCreateMediaType {
        val marker = uppercase()
        return when {
            marker.contains("AUDIO") -> QuickCreateMediaType.AUDIO
            marker.contains("VIDEO") -> QuickCreateMediaType.VIDEO
            else -> QuickCreateMediaType.IMAGE
        }
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

    private suspend fun generateVideo() {
        val config = _uiState.value.videoConfig
        val prompt = config.prompt.trim()
        if (prompt.isEmpty() || config.promptOverLimit) {
            _uiState.update {
                it.copy(taskStatus = QuickCreateTaskUiStatus.IDLE, error = "请输入描述词")
            }
            return
        }

        quickCreateRepository.generateVideo(
            buildVideoGenerationRequest(_uiState.value, requirePrompt = true) ?: return
        ).collect { status ->
            handleTaskStatus(status)
        }
    }

    private fun handleTaskStatus(status: QuickCreateTaskStatus) {
        _uiState.update {
            when (status) {
                is QuickCreateTaskStatus.Submitting -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.SUBMITTING, statusText = "正在提交..."
                )
                is QuickCreateTaskStatus.Queuing -> {
                    // AC5: draft cleared on successful submit — side effect moved outside update lambda
                    screenModelScope.launch { settingsRepository.clearQuickCreateDraft() }
                    it.copy(
                        taskStatus = QuickCreateTaskUiStatus.QUEUING, statusText = "排队中..."
                    )
                }
                is QuickCreateTaskStatus.Running -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.RUNNING,
                    statusText = "生成中... ${status.progress}%"
                )
                is QuickCreateTaskStatus.Success -> {
                    val results = status.results.map { item ->
                        QuickCreateResultUi(
                            url = item.url,
                            type = item.type,
                            thumbnailUrl = item.thumbnailUrl,
                            width = item.width,
                            height = item.height,
                            duration = item.duration,
                        )
                    }
                    screenModelScope.launch { loadQuickCreationHistory() }
                    it.copy(
                        taskStatus = QuickCreateTaskUiStatus.SUCCESS,
                        statusText = "生成完成",
                        results = results,
                    )
                }
                is QuickCreateTaskStatus.Failed -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.FAILED,
                    error = status.errorMessage,
                )
                is QuickCreateTaskStatus.Error -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.IDLE,
                    error = status.message,
                )
            }
        }
    }
}
