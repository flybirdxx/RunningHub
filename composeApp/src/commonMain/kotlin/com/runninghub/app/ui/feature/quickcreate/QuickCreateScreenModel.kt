package com.runninghub.app.ui.feature.quickcreate

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.runninghub.app.platform.MediaResolver
import com.runninghub.shared.domain.repository.QuickCreateInspirationTemplateDetail
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import com.runninghub.shared.domain.repository.SettingsRepository
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

@Serializable
data class DraftData(
    val currentTab: String = "IMAGE",
    val imagePrompt: String = "",
    val videoPrompt: String = "",
)

private val draftJson = Json { encodeDefaults = true }

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

    /** Check if a draft exists and returns its content.
     *  TODO: These properties are non-reactive and currently have no UI consumers.
     *  Consider moving draft state into QuickCreateUiState for reactive UI binding. */
    var hasDraft: Boolean = false
        private set
    var draftData: DraftData? = null
        private set

    init {
        loadServiceModels()
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
        }
    }

    fun checkForDraft() {
        screenModelScope.launch {
            val raw = settingsRepository.getQuickCreateDraft()
            if (!raw.isNullOrEmpty()) {
                try {
                    val draft = draftJson.decodeFromString<DraftData>(raw)
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
        _uiState.update { it.copy(currentTab = tab, estimatedCost = cost) }
        autoSaveDraft()
    }

    fun updateImagePrompt(prompt: String) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(prompt = prompt)) }
        autoSaveDraft()
    }

    fun updateVideoPrompt(prompt: String) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(prompt = prompt)) }
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
            settingsRepository.saveQuickCreateDraft(draftJson.encodeToString(draft))
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
    }

    fun updateImageServiceModel(model: QuickCreationServiceModel) {
        _uiState.update {
            it.copy(
                selectedImageServiceModel = model,
                imageServiceParams = model.defaultServiceParams(),
            )
        }
    }

    fun updateVideoServiceModel(model: QuickCreationServiceModel) {
        _uiState.update {
            it.copy(
                selectedVideoServiceModel = model,
                videoServiceParams = model.defaultServiceParams(),
            )
        }
    }

    fun updateImageServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        if (_uiState.value.selectedImageServiceModel?.hasFieldParam(paramKey) != true) return
        _uiState.update {
            it.copy(imageServiceParams = it.imageServiceParams + (paramKey to value))
        }
    }

    fun updateVideoServiceParam(paramKey: String, value: String) {
        if (paramKey.isBlank()) return
        if (_uiState.value.selectedVideoServiceModel?.hasFieldParam(paramKey) != true) return
        _uiState.update {
            it.copy(videoServiceParams = it.videoServiceParams + (paramKey to value))
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
    }

    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(aspectRatio = ratio)) }
    }

    fun updateImageResolution(res: ImageResolution) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(resolution = res)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateImageQuality(quality: ImageQuality) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(quality = quality)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateImageCount(count: Int) {
        _uiState.update {
            val newConfig = it.imageConfig.copy(count = count)
            it.copy(imageConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateImageSeed(seed: Int?) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(seed = seed)) }
    }

    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(aspectRatio = ratio)) }
    }

    fun updateVideoResolution(res: VideoResolution) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(resolution = res)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateVideoDuration(duration: VideoDuration) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(duration = duration)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateVideoCount(count: Int) {
        _uiState.update {
            val newConfig = it.videoConfig.copy(count = count)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    fun updateVideoSeed(seed: Int?) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(seed = seed)) }
    }

    fun toggleRealisticMode() {
        _uiState.update {
            it.copy(videoConfig = it.videoConfig.copy(realisticMode = !it.videoConfig.realisticMode))
        }
    }

    fun toggleGenerateAudio() {
        _uiState.update {
            val newConfig = it.videoConfig.copy(generateAudio = !it.videoConfig.generateAudio)
            it.copy(videoConfig = newConfig, estimatedCost = newConfig.estimatedCost)
        }
    }

    // ── Media References ─────────────────────────────────────────────────────

    fun pickImageReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.IMAGE)
    }

    fun pickVideoReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.VIDEO)
    }

    fun pickAudioReference(uriString: String) {
        if (uriString.isBlank()) return
        addMediaReference(uriString, QuickCreateMediaType.AUDIO)
    }

    private fun addMediaReference(uriString: String, type: QuickCreateMediaType) {
        val TAG = "QCScreenModel"
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "${type.name}_$now"
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
            uploadStatus = UploadStatus.UPLOADING,
            uploadProgress = 0f,
        )
        _uiState.update { state ->
            if (state.currentTab == QuickCreateTab.IMAGE) {
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

        uploadReference(id, uriString, type, fileName)
    }

    private fun uploadReference(id: String, uriString: String, type: QuickCreateMediaType, fileName: String) {
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
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.1f)
                debug(TAG, "uploadReference: reading bytes from URI...")
                val bytes = withContext(Dispatchers.IO) {
                    mediaResolver.readBytes(uriString)
                }
                debug(TAG, "uploadReference: read ${bytes.size} bytes")
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.7f)

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
                updateReferenceStatus(id, UploadStatus.PROCESSING, 0.85f)

                _uiState.update { state ->
                    if (state.currentTab == QuickCreateTab.IMAGE) {
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
            } catch (e: Exception) {
                debug(TAG, "uploadReference: CATCH - ${e::class.simpleName}: ${e.message}")
                _uiState.update { state ->
                    if (state.currentTab == QuickCreateTab.IMAGE) {
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

    private fun updateReferenceStatus(id: String, status: UploadStatus, progress: Float) {
        _uiState.update { state ->
            if (state.currentTab == QuickCreateTab.IMAGE) {
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
    }

    fun removeMediaReference(id: String) {
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
            when (_uiState.value.currentTab) {
                QuickCreateTab.IMAGE -> generateImage()
                QuickCreateTab.VIDEO -> generateVideo()
            }
        }
    }

    private suspend fun awaitPendingUploads() {
        val mediaRefs = if (_uiState.value.currentTab == QuickCreateTab.IMAGE) {
            _uiState.value.imageConfig.mediaReferences
        } else {
            _uiState.value.videoConfig.mediaReferences
        }
        val pending = mediaRefs.filter { it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING }
        if (pending.isEmpty()) return

        _uiState.update { it.copy(statusText = "正在上传素材(${pending.size})...") }

        val pendingIds = pending.map { it.id }.toSet()
        var waited = 0
        while (waited < 120) {
            delay(500)
            waited++
            val stillPending = _uiState.value.let { state ->
                val refs = if (state.currentTab == QuickCreateTab.IMAGE) {
                    state.imageConfig.mediaReferences
                } else {
                    state.videoConfig.mediaReferences
                }
                refs.filter {
                    it.id in pendingIds &&
                        (it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING)
                }
                    .map { it.id }
                    .toSet()
            }
            if (stillPending.isEmpty()) return
        }

        val failed = _uiState.value.let { state ->
            val refs = if (state.currentTab == QuickCreateTab.IMAGE) {
                state.imageConfig.mediaReferences
            } else {
                state.videoConfig.mediaReferences
            }
            refs.filter { it.id in pendingIds && it.uploadStatus == UploadStatus.FAILED }
        }
        if (failed.isNotEmpty()) {
            throw IllegalStateException("素材上传失败: ${failed.joinToString { it.displayName }}")
        }
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

        val imageRef = config.mediaReferences
            .filter { it.type == QuickCreateMediaType.IMAGE && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }

        quickCreateRepository.generateImage(
            com.runninghub.shared.domain.repository.ImageGenerationRequest(
                prompt = prompt,
                model = config.model.apiValue,
                aspectRatio = config.aspectRatio.apiValue,
                resolution = config.resolution.apiValue,
                quality = config.quality.apiValue,
                referenceImageUri = imageRef?.remoteUrl,
                numImages = config.count,
                seed = config.seed,
                quickCreationCategoryId = _uiState.value.selectedImageServiceModel?.categoryId,
                quickCreationBindingId = _uiState.value.selectedImageServiceModel?.bindingId,
                quickCreationSkuId = _uiState.value.selectedImageServiceModel?.skuId,
                quickCreationParams = imageQuickCreationParams(
                    model = _uiState.value.selectedImageServiceModel,
                    config = config,
                    serviceParams = _uiState.value.imageServiceParams,
                ),
                quickCreationListParams = imageQuickCreationListParams(
                    model = _uiState.value.selectedImageServiceModel,
                    config = config,
                ),
            )
        ).collect { status ->
            handleTaskStatus(status)
        }
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
                    .filterKeys { key -> model?.hasFieldParam(key) == true }
                    .filterValues { it.isNotBlank() }
            )

            put("aspectRatio", config.aspectRatio.apiValue)
            put("resolution", config.resolution.apiValue)
            put("quality", config.quality.apiValue)
        }

    private fun imageQuickCreationListParams(
        model: QuickCreationServiceModel?,
        config: ImageConfig,
    ): Map<String, List<String>> =
        quickCreationListParams(
            model = model,
            mediaReferences = config.mediaReferences,
            fallbackMediaType = QuickCreateMediaType.IMAGE,
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
                    .filterKeys { key -> model?.hasFieldParam(key) == true }
                    .filterValues { it.isNotBlank() }
            )

            put("aspectRatio", config.aspectRatio.apiValue)
            put("resolution", config.resolution.apiValue)
            put("duration", config.duration.seconds.toString())
        }

    private fun videoQuickCreationListParams(
        model: QuickCreationServiceModel?,
        config: VideoConfig,
    ): Map<String, List<String>> =
        quickCreationListParams(
            model = model,
            mediaReferences = config.mediaReferences,
            fallbackMediaType = null,
        )

    private fun quickCreationListParams(
        model: QuickCreationServiceModel?,
        mediaReferences: List<MediaReference>,
        fallbackMediaType: QuickCreateMediaType?,
    ): Map<String, List<String>> {
        val urlsByType = mediaReferences
            .filter { it.uploadStatus == UploadStatus.DONE }
            .groupBy { it.type }
            .mapValues { (_, refs) ->
                refs.mapNotNull { it.remoteUrl?.takeIf { url -> url.isNotBlank() } }
            }

        return model.uploadFields()
            .mapNotNull { field ->
                val mediaType = field.uploadMediaType() ?: fallbackMediaType ?: return@mapNotNull null
                val urls = urlsByType[mediaType].orEmpty()
                if (urls.isEmpty()) {
                    null
                } else {
                    val maxCount = field.maxUploadCount ?: urls.size
                    field.paramKey to urls.take(maxCount)
                }
            }
            .toMap()
    }

    private fun QuickCreationServiceModel?.defaultServiceParams(): Map<String, String> =
        this?.fields.orEmpty()
            .mapNotNull { field ->
                val value = field.defaultValue?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                field.paramKey to value
            }
            .toMap()

    private fun QuickCreationServiceModel.hasFieldParam(paramKey: String): Boolean =
        fields.any { it.paramKey == paramKey }

    private fun QuickCreationServiceModel?.uploadFields(): List<QuickCreationServiceField> =
        this?.fields.orEmpty().filter { it.fieldType.uppercase().contains("UPLOAD") }

    private fun QuickCreationServiceField.uploadMediaType(): QuickCreateMediaType? {
        val marker = listOfNotNull(fieldType, fieldKey, paramKey, inputExtraJson)
            .joinToString(" ")
            .uppercase()
        return when {
            marker.contains("AUDIO") -> QuickCreateMediaType.AUDIO
            marker.contains("VIDEO") -> QuickCreateMediaType.VIDEO
            marker.contains("IMAGE") || marker.contains("PHOTO") || marker.contains("IMG") -> QuickCreateMediaType.IMAGE
            else -> null
        }
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

        val imageRef = config.mediaReferences
            .filter { it.type == QuickCreateMediaType.IMAGE && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }
        val videoRef = config.mediaReferences
            .filter { it.type == QuickCreateMediaType.VIDEO && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }
        val audioRef = config.mediaReferences
            .filter { it.type == QuickCreateMediaType.AUDIO && it.uploadStatus == UploadStatus.DONE }
            .firstOrNull { it.remoteUrl != null }

        quickCreateRepository.generateVideo(
            com.runninghub.shared.domain.repository.VideoGenerationRequest(
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
                quickCreationCategoryId = _uiState.value.selectedVideoServiceModel?.categoryId,
                quickCreationBindingId = _uiState.value.selectedVideoServiceModel?.bindingId,
                quickCreationSkuId = _uiState.value.selectedVideoServiceModel?.skuId,
                quickCreationParams = videoQuickCreationParams(
                    model = _uiState.value.selectedVideoServiceModel,
                    config = config,
                    serviceParams = _uiState.value.videoServiceParams,
                ),
                quickCreationListParams = videoQuickCreationListParams(
                    model = _uiState.value.selectedVideoServiceModel,
                    config = config,
                ),
            )
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
