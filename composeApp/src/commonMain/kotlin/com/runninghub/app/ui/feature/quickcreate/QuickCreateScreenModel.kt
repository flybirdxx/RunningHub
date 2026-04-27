package com.runninghub.app.ui.feature.quickcreate

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import coil3.Uri
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickCreateScreenModel(
    private val quickCreateRepository: QuickCreateRepository,
    private val androidContext: android.content.Context,
) : ScreenModel {

    val context: android.content.Context get() = androidContext

    private val _uiState = MutableStateFlow(QuickCreateUiState())
    val uiState: StateFlow<QuickCreateUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    private val uploadJobs = mutableMapOf<String, Job>()

    // ── Tab & Prompt ──────────────────────────────────────────────────────────

    fun switchTab(tab: QuickCreateTab) {
        val cost = when (tab) {
            QuickCreateTab.IMAGE -> _uiState.value.imageConfig.estimatedCost
            QuickCreateTab.VIDEO -> _uiState.value.videoConfig.estimatedCost
        }
        _uiState.update { it.copy(currentTab = tab, estimatedCost = cost) }
    }

    fun updateImagePrompt(prompt: String) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(prompt = prompt)) }
    }

    fun updateVideoPrompt(prompt: String) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(prompt = prompt)) }
    }

    // ── Tune Sheet ───────────────────────────────────────────────────────────

    fun setTuneSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(tuneSheetVisible = visible) }
    }

    // ── Parameters ────────────────────────────────────────────────────────────

    fun updateImageModel(model: ImageModel) {
        _uiState.update {
            it.copy(
                imageConfig = it.imageConfig.copy(
                    model = model,
                    aspectRatio = ImageAspectRatio.entries.find { r -> r.apiValue == model.defaultAspectRatio }
                        ?: ImageAspectRatio.RATIO_16_9,
                    resolution = ImageResolution.entries.find { r -> r.apiValue == model.defaultResolution }
                        ?: ImageResolution.RES_1K,
                    quality = ImageQuality.entries.find { q -> q.apiValue == model.defaultQuality }
                        ?: ImageQuality.QUALITY_MEDIUM,
                ),
                estimatedCost = model.estimateCost(
                    _uiState.value.imageConfig.resolution.apiValue,
                    _uiState.value.imageConfig.quality.apiValue
                )
            )
        }
    }

    fun updateVideoModel(model: VideoModel) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(model = model)) }
    }

    fun updateImageAspectRatio(ratio: ImageAspectRatio) {
        _uiState.update { it.copy(imageConfig = it.imageConfig.copy(aspectRatio = ratio)) }
    }

    fun updateImageResolution(res: ImageResolution) {
        _uiState.update {
            val newCost = it.imageConfig.model.estimateCost(res.apiValue, it.imageConfig.quality.apiValue)
            it.copy(imageConfig = it.imageConfig.copy(resolution = res), estimatedCost = newCost)
        }
    }

    fun updateImageQuality(quality: ImageQuality) {
        _uiState.update {
            val newCost = it.imageConfig.model.estimateCost(it.imageConfig.resolution.apiValue, quality.apiValue)
            it.copy(imageConfig = it.imageConfig.copy(quality = quality), estimatedCost = newCost)
        }
    }

    fun updateVideoAspectRatio(ratio: VideoAspectRatio) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(aspectRatio = ratio)) }
    }

    fun updateVideoResolution(res: VideoResolution) {
        _uiState.update { it.copy(videoConfig = it.videoConfig.copy(resolution = res)) }
    }

    fun updateVideoDuration(duration: VideoDuration) {
        _uiState.update {
            val newCost = VideoModel.estimateCost(
                it.videoConfig.model.apiValue,
                it.videoConfig.resolution.apiValue,
                duration.seconds,
                it.videoConfig.generateAudio
            )
            it.copy(videoConfig = it.videoConfig.copy(duration = duration), estimatedCost = newCost)
        }
    }

    fun toggleRealisticMode() {
        _uiState.update {
            it.copy(videoConfig = it.videoConfig.copy(realisticMode = !it.videoConfig.realisticMode))
        }
    }

    fun toggleGenerateAudio() {
        _uiState.update {
            val newAudio = !it.videoConfig.generateAudio
            val newCost = VideoModel.estimateCost(
                it.videoConfig.model.apiValue,
                it.videoConfig.resolution.apiValue,
                it.videoConfig.duration.seconds,
                newAudio
            )
            it.copy(videoConfig = it.videoConfig.copy(generateAudio = newAudio), estimatedCost = newCost)
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
        val id = "${type.name}_${System.currentTimeMillis()}"
        val fileName = extractFileName(uriString) ?: "${type.name.lowercase()}_${System.currentTimeMillis()}"
        val fileSize = extractFileSize(uriString)

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
            val actualFileName = "${type.name.lowercase()}_${System.currentTimeMillis()}.$ext"

            try {
                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.1f)

                val bytes = withContext(Dispatchers.IO) {
                    extractMediaBytes(androidContext, uriString)
                }

                updateReferenceStatus(id, UploadStatus.UPLOADING, 0.7f)

                val remoteUrl = quickCreateRepository.uploadMedia(bytes, actualFileName, mimeType).getOrThrow()

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

    private fun extractFileName(uriString: String): String? {
        val uri = android.net.Uri.parse(uriString)
        return try {
            androidContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFileSize(uriString: String): Long {
        val uri = android.net.Uri.parse(uriString)
        return try {
            androidContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
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
            when (_uiState.value.currentTab) {
                QuickCreateTab.IMAGE -> generateImage()
                QuickCreateTab.VIDEO -> generateVideo()
            }
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
                aspectRatio = config.aspectRatio.apiValue,
                resolution = config.resolution.apiValue,
                quality = config.quality.apiValue,
                referenceImageUri = imageRef?.remoteUrl,
            )
        ).collect { status ->
            handleTaskStatus(status)
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
                aspectRatio = config.aspectRatio.apiValue,
                duration = config.duration.seconds,
                resolution = config.resolution.apiValue,
                referenceImageUri = imageRef?.remoteUrl,
                referenceVideoUri = videoRef?.remoteUrl,
                referenceAudioUri = audioRef?.remoteUrl,
                realistic = config.realisticMode,
                generateAudio = config.generateAudio,
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
                is QuickCreateTaskStatus.Queuing -> it.copy(
                    taskStatus = QuickCreateTaskUiStatus.QUEUING, statusText = "排队中..."
                )
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
