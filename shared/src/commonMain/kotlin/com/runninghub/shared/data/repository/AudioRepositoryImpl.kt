package com.runninghub.shared.data.repository

import com.runninghub.shared.data.model.MiniMaxAudioRequest
import com.runninghub.shared.data.model.TaskQueryRequest
import com.runninghub.shared.data.remote.AudioApiService
import com.runninghub.shared.domain.model.AppResult
import com.runninghub.shared.domain.repository.AudioRepository
import com.runninghub.shared.domain.repository.AudioTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AudioRepositoryImpl(
    private val api: AudioApiService
) : AudioRepository {

    override suspend fun generateAudio(
        text: String,
        voiceId: String,
        speed: Float,
        volume: Float,
        pitch: Int,
        emotion: String?
    ): AppResult<AudioTask> = try {
        val response = api.textToAudio(
            MiniMaxAudioRequest(
                text = text,
                voiceId = voiceId,
                speed = speed,
                volume = volume,
                pitch = pitch,
                emotion = emotion
            )
        )
        if (response.status == "FAILED") {
            AppResult.Error(response.errorMessage ?: "Submission failed")
        } else {
            AppResult.Success(
                AudioTask(
                    taskId = response.taskId ?: "",
                    status = response.status ?: "QUEUED"
                )
            )
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Unknown error")
    }

    override fun pollAudioTask(taskId: String): Flow<AppResult<AudioTask>> = flow {
        emit(AppResult.Loading)
        var attempts = 0
        val maxAttempts = 60

        while (attempts < maxAttempts) {
            try {
                val result = api.queryTask(TaskQueryRequest(taskId))
                when (result.status) {
                    "SUCCESS" -> {
                        val url = result.results?.firstOrNull()?.url
                        if (url != null) {
                            emit(AppResult.Success(AudioTask(taskId, "SUCCESS", audioUrl = url)))
                        } else {
                            emit(AppResult.Error("No result URL"))
                        }
                        return@flow
                    }
                    "FAILED" -> {
                        emit(AppResult.Error(result.errorMessage ?: "Task failed"))
                        return@flow
                    }
                    else -> {
                        emit(AppResult.Success(AudioTask(taskId, result.status ?: "RUNNING")))
                        delay(1000)
                        attempts++
                    }
                }
            } catch (e: Exception) {
                emit(AppResult.Error(e.message ?: "Polling error"))
                return@flow
            }
        }
        emit(AppResult.Error("Task timed out"))
    }
}
