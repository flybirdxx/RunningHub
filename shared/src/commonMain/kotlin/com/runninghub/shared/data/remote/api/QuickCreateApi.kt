package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

class QuickCreateApi(private val client: HttpClient) {

    companion object {
        const val BASE_URL = "https://www.runninghub.cn"
        private const val TAG = "QuickCreateApi"
        const val IMAGE_TEXT_TO_IMAGE = "/openapi/v2/rhart-image-g-2-official/text-to-image"
        const val IMAGE_IMAGE_TO_IMAGE = "/openapi/v2/rhart-image-g-2-official/image-to-image"
        const val VIDEO_TEXT_TO_VIDEO = "/openapi/v2/rhart-video-s-official/text-to-video-pro"
        const val VIDEO_IMAGE_TO_VIDEO = "/openapi/v2/rhart-video-s-official/image-to-video-pro"
        const val VIDEO_G_TEXT_TO_VIDEO = "/openapi/v2/rhart-video-g-official/text-to-video"
        const val VIDEO_G_IMAGE_TO_VIDEO = "/openapi/v2/rhart-video-g-official/image-to-video"
        const val TASK_QUERY = "/openapi/v2/task/query"
        const val MEDIA_UPLOAD = "/openapi/v2/media/upload/binary"
    }

    // ── 图片创作 ──────────────────────────────────────────

    suspend fun textToImage(request: ImageGenerationRequestDto): ImageGenerationResponseDto =
        client.post("$BASE_URL$IMAGE_TEXT_TO_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageToImage(request: ImageGenerationRequestDto): ImageGenerationResponseDto =
        client.post("$BASE_URL$IMAGE_IMAGE_TO_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 ──────────────────────────────────────────

    suspend fun textToVideo(request: VideoGenerationRequestDto): VideoGenerationResponseDto =
        client.post("$BASE_URL$VIDEO_TEXT_TO_VIDEO") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageToVideo(request: VideoGenerationRequestDto): VideoGenerationResponseDto =
        client.post("$BASE_URL$VIDEO_IMAGE_TO_VIDEO") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun videoGTextToVideo(request: VideoGenerationRequestDto): VideoGenerationResponseDto =
        client.post("$BASE_URL$VIDEO_G_TEXT_TO_VIDEO") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun videoGImageToVideo(request: VideoGenerationRequestDto): VideoGenerationResponseDto =
        client.post("$BASE_URL$VIDEO_G_IMAGE_TO_VIDEO") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 任务状态 ──────────────────────────────────────────

    suspend fun queryTask(taskId: String): QuickCreateTaskQueryResponseDto =
        client.get("$BASE_URL$TASK_QUERY") {
            parameter("taskId", taskId)
        }.body()

    // ── 媒体上传 ──────────────────────────────────────────

    suspend fun uploadMedia(
        apiKey: String,
        fileBytes: ByteArray,
        fileName: String,
        contentType: String = "application/octet-stream"
    ): MediaUploadResponseDto {
        val url = "$BASE_URL$MEDIA_UPLOAD"
        debug(TAG, "uploadMedia: START")
        debug(TAG, "  url        = $url")
        debug(TAG, "  apiKey     = ${apiKey.take(8)}...")
        debug(TAG, "  fileName   = $fileName")
        debug(TAG, "  contentType= $contentType")
        debug(TAG, "  fileBytes  = ${fileBytes.size} bytes")

        val response: MediaUploadResponseDto = client.submitFormWithBinaryData(
            url = url,
            formData = formData {
                append("apiKey", apiKey)
                append("fileType", contentType)
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, contentType)
                })
            }
        ).body()

        debug(TAG, "uploadMedia: SUCCESS")
        debug(TAG, "  response.url     = ${response.url}")
        debug(TAG, "  response.fileId  = ${response.fileId}")
        debug(TAG, "  response.fileName= ${response.fileName}")
        return response
    }
}
