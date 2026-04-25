package com.runninghub.app.data.remote.api

import com.runninghub.app.data.remote.model.MiniMaxAudioRequest
import com.runninghub.app.data.remote.model.MiniMaxAudioResponse
import com.runninghub.app.data.remote.model.TaskQueryRequest
import com.runninghub.app.data.remote.model.TaskQueryResult
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * [INPUT]: MiniMaxAudioRequest
 * [OUTPUT]: MiniMaxAudioResponse / TaskQueryResult
 * [POS]: Core API for MiniMax Speech-2.8-hd integration
 */
interface AudioApi {

    /**
     * Submit a text-to-speech task
     */
    @POST("/openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd")
    suspend fun textToAudio(
        @Body request: MiniMaxAudioRequest
    ): MiniMaxAudioResponse

    /**
     * Query the status and result of a task
     */
    @POST("/openapi/v2/query")
    suspend fun queryTask(
        @Body request: TaskQueryRequest
    ): TaskQueryResult
}
