package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.data.remote.dto.ImageGenerationRequestDto
import com.runninghub.shared.data.remote.dto.QuickCreateResult
import com.runninghub.shared.data.remote.dto.VideoGenerationRequestDto
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateResultItem
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.VideoGenerationRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QuickCreateRepositoryImpl(
    private val quickCreateApi: QuickCreateApi
) : QuickCreateRepository {

    override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            val dto = ImageGenerationRequestDto(
                prompt = request.prompt,
                aspectRatio = request.aspectRatio,
                resolution = request.resolution,
                quality = request.quality,
                imageUrl = request.referenceImageUri,
                seed = request.seed,
            )

            val response = if (request.referenceImageUri != null) {
                quickCreateApi.imageToImage(dto)
            } else {
                quickCreateApi.textToImage(dto)
            }

            val taskId = response.taskId

            if (response.status == QuickCreateResult.STATUS_FAILED) {
                emit(QuickCreateTaskStatus.Failed(taskId, "提交失败"))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))

            var attempts = 0
            val maxAttempts = 120
            while (attempts < maxAttempts) {
                delay(2000)
                attempts++

                val queryResponse = quickCreateApi.queryTask(taskId)
                val results = queryResponse.results?.map {
                    QuickCreateResultItem(
                        url = it.url,
                        type = it.type,
                        thumbnailUrl = it.thumbnailUrl,
                        width = it.width,
                        height = it.height,
                        duration = it.duration,
                    )
                }

                val taskStatus = when (queryResponse.status) {
                    QuickCreateResult.STATUS_SUCCESS -> QuickCreateTaskStatus.Success(taskId, results ?: emptyList())
                    QuickCreateResult.STATUS_FAILED -> QuickCreateTaskStatus.Failed(taskId, queryResponse.errorMessage ?: "任务失败")
                    QuickCreateResult.STATUS_RUNNING -> QuickCreateTaskStatus.Running(taskId, queryResponse.progress)
                    else -> QuickCreateTaskStatus.Queuing(taskId)
                }

                emit(taskStatus)
                if (taskStatus is QuickCreateTaskStatus.Success) return@flow
            }

            emit(QuickCreateTaskStatus.Error("任务超时"))
        } catch (e: Exception) {
            emit(QuickCreateTaskStatus.Error(e.message ?: "未知错误"))
        }
    }

    override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            val dto = VideoGenerationRequestDto(
                prompt = request.prompt,
                model = request.model,
                aspectRatio = request.aspectRatio,
                duration = request.duration,
                resolution = request.resolution,
                imageUrl = request.referenceImageUri,
                videoUrl = request.referenceVideoUri,
                audioUrl = request.referenceAudioUri,
                realistic = request.realistic,
                generateAudio = request.generateAudio,
            )

            val response = when {
                request.referenceImageUri != null -> quickCreateApi.imageToVideo(dto)
                request.referenceVideoUri != null -> quickCreateApi.imageToVideo(dto)
                else -> quickCreateApi.textToVideo(dto)
            }

            val taskId = response.taskId

            if (response.status == QuickCreateResult.STATUS_FAILED) {
                emit(QuickCreateTaskStatus.Failed(taskId, "提交失败"))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))

            var attempts = 0
            val maxAttempts = 120
            while (attempts < maxAttempts) {
                delay(2000)
                attempts++

                val queryResponse = quickCreateApi.queryTask(taskId)
                val results = queryResponse.results?.map {
                    QuickCreateResultItem(
                        url = it.url,
                        type = it.type,
                        thumbnailUrl = it.thumbnailUrl,
                        width = it.width,
                        height = it.height,
                        duration = it.duration,
                    )
                }

                val taskStatus = when (queryResponse.status) {
                    QuickCreateResult.STATUS_SUCCESS -> QuickCreateTaskStatus.Success(taskId, results ?: emptyList())
                    QuickCreateResult.STATUS_FAILED -> QuickCreateTaskStatus.Failed(taskId, queryResponse.errorMessage ?: "任务失败")
                    QuickCreateResult.STATUS_RUNNING -> QuickCreateTaskStatus.Running(taskId, queryResponse.progress)
                    else -> QuickCreateTaskStatus.Queuing(taskId)
                }

                emit(taskStatus)
                if (taskStatus is QuickCreateTaskStatus.Success) return@flow
            }

            emit(QuickCreateTaskStatus.Error("任务超时"))
        } catch (e: Exception) {
            emit(QuickCreateTaskStatus.Error(e.message ?: "未知错误"))
        }
    }

    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> = runCatching {
        val response = quickCreateApi.uploadMedia(fileBytes, fileName, mimeType)
        response.url
    }
}
