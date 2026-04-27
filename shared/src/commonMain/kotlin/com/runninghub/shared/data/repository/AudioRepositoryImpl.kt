package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.AudioApi
import com.runninghub.shared.data.remote.dto.MiniMaxAudioRequestDto
import com.runninghub.shared.data.remote.dto.TaskQueryRequestDto
import com.runninghub.shared.domain.model.*
import com.runninghub.shared.domain.repository.AudioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AudioRepositoryImpl(
    private val audioApi: AudioApi
) : AudioRepository {

    override fun convertTextToAudio(request: AudioRequest): Flow<AudioTaskStatus> = flow {
        emit(AudioTaskStatus.Submitting)

        try {
            val dto = MiniMaxAudioRequestDto(
                text = request.text,
                voiceId = request.voiceId,
                speed = request.speed,
                volume = request.volume,
                pitch = request.pitch,
                emotion = request.emotion
            )
            val submitResponse = audioApi.textToAudio(dto)

            if (submitResponse.status == "FAILED") {
                emit(AudioTaskStatus.Error(submitResponse.errorMessage ?: "Submission failed"))
                return@flow
            }

            val taskId = submitResponse.taskId
            emit(AudioTaskStatus.Running(taskId))

            var attempts = 0
            val maxAttempts = 60

            while (attempts < maxAttempts) {
                val queryResponse = audioApi.queryTask(TaskQueryRequestDto(taskId))

                when (queryResponse.status) {
                    "SUCCESS" -> {
                        val resultUrl = queryResponse.results?.firstOrNull()?.url
                        if (resultUrl != null) {
                            emit(AudioTaskStatus.Success(resultUrl))
                        } else {
                            emit(AudioTaskStatus.Error("No result URL found"))
                        }
                        return@flow
                    }
                    "FAILED" -> {
                        emit(AudioTaskStatus.Error(queryResponse.errorMessage ?: "Task failed"))
                        return@flow
                    }
                    else -> {
                        delay(1000)
                        attempts++
                    }
                }
            }

            emit(AudioTaskStatus.Error("Task timed out"))
        } catch (e: Exception) {
            emit(AudioTaskStatus.Error(e.message ?: "Unknown error occurred"))
        }
    }

    override suspend fun queryTask(taskId: String): Result<AudioTaskResult> = runCatching {
        val response = audioApi.queryTask(TaskQueryRequestDto(taskId))
        AudioTaskResult(
            taskId = response.taskId,
            status = response.status,
            errorCode = response.errorCode,
            errorMessage = response.errorMessage,
            results = response.results?.map {
                AudioResult(url = it.url, outputType = it.outputType, text = it.text)
            }
        )
    }
}
