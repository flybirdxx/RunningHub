package com.runninghub.shared.domain.repository

import kotlinx.coroutines.flow.Flow

data class ImageGenerationRequest(
    val prompt: String,
    val aspectRatio: String = "16:9",
    val resolution: String = "1K",
    val quality: String = "medium",
    val referenceImageUri: String? = null,
    val seed: Int? = null,
)

data class VideoGenerationRequest(
    val prompt: String,
    val model: String,
    val aspectRatio: String = "16:9",
    val duration: Int = 5,
    val resolution: String = "720p",
    val referenceImageUri: String? = null,
    val referenceVideoUri: String? = null,
    val referenceAudioUri: String? = null,
    val realistic: Boolean = false,
    val generateAudio: Boolean = false,
)

sealed class QuickCreateTaskStatus {
    data object Submitting : QuickCreateTaskStatus()
    data class Queuing(val taskId: String) : QuickCreateTaskStatus()
    data class Running(val taskId: String, val progress: Int) : QuickCreateTaskStatus()
    data class Success(val taskId: String, val results: List<QuickCreateResultItem>) : QuickCreateTaskStatus()
    data class Failed(val taskId: String, val errorMessage: String) : QuickCreateTaskStatus()
    data class Error(val message: String) : QuickCreateTaskStatus()
}

data class QuickCreateResultItem(
    val url: String,
    val type: String,
    val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Int? = null,
)

interface QuickCreateRepository {
    fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus>
    fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus>
    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String>
}
