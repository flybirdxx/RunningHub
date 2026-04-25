package com.runninghub.shared.data.remote

import com.runninghub.shared.data.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AudioApiService(private val client: HttpClient) {

    suspend fun textToAudio(request: MiniMaxAudioRequest): MiniMaxAudioResponse =
        client.post("${ApiConfig.BASE_URL}../openapi/v2/rhart-audio/text-to-audio/speech-2.8-hd") {
            setBody(request)
        }.body()

    suspend fun queryTask(request: TaskQueryRequest): TaskQueryResult =
        client.post("${ApiConfig.BASE_URL}../openapi/v2/query") {
            setBody(request)
        }.body()
}
