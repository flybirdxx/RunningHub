package com.runninghub.app.data.repository

import com.runninghub.app.data.remote.api.AudioApi
import com.runninghub.app.data.remote.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [INPUT]: AudioApi
 * [OUTPUT]: Audio Task Flows & Status handling
 * [POS]: Repository for Audio processing business logic
 */
@Singleton
class AudioRepository @Inject constructor(
    private val audioApi: AudioApi
) {

    /**
     * Higher-level flow that submits a task and polls for result
     */
    fun convertTextToAudio(request: MiniMaxAudioRequest): Flow<AudioTaskStatus> = flow {
        emit(AudioTaskStatus.Submitting)
        
        try {
            val submitResponse = audioApi.textToAudio(request)
            
            if (submitResponse.status == "FAILED") {
                emit(AudioTaskStatus.Error(submitResponse.errorMessage ?: "Submission failed"))
                return@flow
            }

            val taskId = submitResponse.taskId
            emit(AudioTaskStatus.Running(taskId))

            // Polling Loop
            var attempts = 0
            val maxAttempts = 60 // 1 minute with 1s delay
            
            while (attempts < maxAttempts) {
                val queryResponse = audioApi.queryTask(TaskQueryRequest(taskId))
                
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
                        // Still QUEUED or RUNNING
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
}

sealed class AudioTaskStatus {
    object Submitting : AudioTaskStatus()
    data class Running(val taskId: String) : AudioTaskStatus()
    data class Success(val url: String) : AudioTaskStatus()
    data class Error(val message: String) : AudioTaskStatus()
}
