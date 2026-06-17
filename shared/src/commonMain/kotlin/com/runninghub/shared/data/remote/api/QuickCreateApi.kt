package com.runninghub.shared.data.remote.api

import com.runninghub.shared.data.remote.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

class QuickCreateApi(private val client: HttpClient, private val json: Json) {

    companion object {
        const val BASE_URL = "https://www.runninghub.cn"
        private const val TAG = "QuickCreateApi"

        // 通用端点
        const val TASK_QUERY = "/openapi/v2/query"
        const val MEDIA_UPLOAD = "/openapi/v2/media/upload/binary"

        // Web 快捷创作 v2 端点
        const val QC_CATEGORIES = "/api/qc/v2/categories"
        const val QC_MODELS = "/api/qc/v2/models"
        const val QC_CREATION_MODES = "/api/qc/v2/creation-modes"
        const val QC_FEE_PREVIEW = "/task/quick-creation/fee-preview"
        const val QC_PREPARE = "/task/quick-creation/prepare"
        const val QC_COMMIT = "/task/quick-creation/commit"
        const val QC_TASK_LIST = "/task/quick-creation/list"
        const val QC_TASK_DETAIL = "/task/quick-creation/detail"
        const val QC_TASK_CANCEL = "/task/quick-creation/cancel"
        const val QC_PROJECT_LIST = "/task/quick-creation/project/list"
        const val QC_INSPIRATION_TAGS = "/task/quick-creation/inspiration/tags"
        const val QC_INSPIRATION_TEMPLATES = "/task/quick-creation/inspiration/templates"
        const val QC_INSPIRATION_TEMPLATE_DETAIL = "/task/quick-creation/inspiration/template/detail"

        // 图片模型端点 (全能图片G-2.0 官方)
        const val IMAGE_G2_TEXT = "/openapi/v2/rhart-image-g-2-official/text-to-image"
        const val IMAGE_G2_IMAGE = "/openapi/v2/rhart-image-g-2-official/image-to-image"
        // 全能图片G-2.0 低价
        const val IMAGE_G2_CHEAP_TEXT = "/openapi/v2/rhart-image-g-2/text-to-image"
        const val IMAGE_G2_CHEAP_IMAGE = "/openapi/v2/rhart-image-g-2/image-to-image"

        // 图片模型端点
        // 全能图片 X 官方
        const val IMAGE_X_TEXT = "/openapi/v2/rhart-image-x-official/text-to-image"
        const val IMAGE_X_IMAGE = "/openapi/v2/rhart-image-x-official/image-to-image"
        // 全能图片 X 低价
        const val IMAGE_X_CHEAP_TEXT = "/openapi/v2/rhart-image-x/text-to-image"
        const val IMAGE_X_CHEAP_IMAGE = "/openapi/v2/rhart-image-x/image-to-image"
        // 全能图片 PRO 官方
        const val IMAGE_PRO_TEXT = "/openapi/v2/rhart-image-pro-official/text-to-image"
        const val IMAGE_PRO_IMAGE = "/openapi/v2/rhart-image-pro-official/image-to-image"
        // 全能图片 PRO 低价
        const val IMAGE_PRO_CHEAP_TEXT = "/openapi/v2/rhart-image-n-pro/text-to-image"
        const val IMAGE_PRO_CHEAP_IMAGE = "/openapi/v2/rhart-image-n-pro/image-to-image"
        // 全能图片 2.0 官方
        const val IMAGE_V2_TEXT = "/openapi/v2/rhart-image-v2-official/text-to-image"
        const val IMAGE_V2_IMAGE = "/openapi/v2/rhart-image-v2-official/image-to-image"
        // 全能图片 2.0 低价
        const val IMAGE_V2_CHEAP_TEXT = "/openapi/v2/rhart-image-n-g31-flash/text-to-image"
        const val IMAGE_V2_CHEAP_IMAGE = "/openapi/v2/rhart-image-n-g31-flash/image-to-image"
        // Seedream 5.0 Lite / 4.0
        const val IMAGE_SEEDREAM5_TEXT = "/openapi/v2/seedream-v5-lite/text-to-image"
        const val IMAGE_SEEDREAM5_IMAGE = "/openapi/v2/seedream-v5-lite/image-to-image"
        const val IMAGE_SEEDREAM4_TEXT = "/openapi/v2/seedream-v4/text-to-image"
        const val IMAGE_SEEDREAM4_IMAGE = "/openapi/v2/seedream-v4/image-to-image"

        // 视频模型端点
        // HappyHorse
        const val VIDEO_HH_TEXT = "/openapi/v2/alibaba/happyhorse-1.0/text-to-video"
        const val VIDEO_HH_IMAGE = "/openapi/v2/alibaba/happyhorse-1.0/image-to-video"
        // Seedance 2.0
        const val VIDEO_SD2_TEXT = "/openapi/v2/rhart-video/sparkvideo-2.0/text-to-video"
        const val VIDEO_SD2_IMAGE = "/openapi/v2/rhart-video/sparkvideo-2.0/image-to-video"
        // Seedance 2.0 Fast
        const val VIDEO_SD2F_TEXT = "/openapi/v2/rhart-video/sparkvideo-2.0-fast/text-to-video"
        const val VIDEO_SD2F_IMAGE = "/openapi/v2/rhart-video/sparkvideo-2.0-fast/image-to-video"
        // 可灵 O3-4K
        const val VIDEO_KL3_IMAGE = "/openapi/v2/kling-video-o3-4k/image-to-video"
        // 可灵 O3-Pro
        const val VIDEO_KLO3P_TEXT = "/openapi/v2/kling-video-o3-pro/text-to-video"
        const val VIDEO_KLO3P_IMAGE = "/openapi/v2/kling-video-o3-pro/image-to-video"
        // 可灵 O3-Std
        const val VIDEO_KLO3S_TEXT = "/openapi/v2/kling-video-o3-std/text-to-video"
        const val VIDEO_KLO3S_IMAGE = "/openapi/v2/kling-video-o3-std/image-to-video"
        // 可灵 O1
        const val VIDEO_KLO1_TEXT = "/openapi/v2/kling-video-o1/text-to-video"
        const val VIDEO_KLO1_IMAGE = "/openapi/v2/kling-video-o1/image-to-video"
        // 万相 2.7
        const val VIDEO_WAN27_TEXT = "/openapi/v2/alibaba/wan-2.7/text-to-video"
        const val VIDEO_WAN27_IMAGE = "/openapi/v2/alibaba/wan-2.7/image-to-video"
        // 万相 2.6
        const val VIDEO_WAN26_IMAGE = "/openapi/v2/alibaba/wan-2.6/image-to-video"
        // PixVerse V6
        const val VIDEO_PX6_TEXT = "/openapi/v2/pixverse-v6/text-to-video"
        const val VIDEO_PX6_IMAGE = "/openapi/v2/pixverse-v6/image-to-video"
        // 全能视频 V3.1 Fast
        const val VIDEO_V31F_TEXT = "/openapi/v2/rhart-video-v3.1-fast/text-to-video"
        const val VIDEO_V31F_IMAGE = "/openapi/v2/rhart-video-v3.1-fast/image-to-video"
        const val VIDEO_V31F_START_END = "/openapi/v2/rhart-video-v3.1-fast/start-end-to-video"
        // 全能视频 V3.1 Pro
        const val VIDEO_V31P_TEXT = "/openapi/v2/rhart-video-v3.1-pro/text-to-video"
        const val VIDEO_V31P_IMAGE = "/openapi/v2/rhart-video-v3.1-pro/image-to-video"
        // 全能视频 X
        const val VIDEO_VX_TEXT = "/openapi/v2/rhart-video-g-official/text-to-video"
        const val VIDEO_VX_IMAGE = "/openapi/v2/rhart-video-g-official/image-to-video"
        const val VIDEO_VXC_IMAGE = "/openapi/v2/rhart-video-g/image-to-video"
        // Vidu Q3
        const val VIDEO_VIDUQ3_TEXT = "/openapi/v2/vidu/text-to-video-q3-pro"
        const val VIDEO_VIDUQ3_IMAGE = "/openapi/v2/vidu/image-to-video-q3-pro"
        const val VIDEO_VIDUQ3_FAST_TEXT = "/openapi/v2/vidu/text-to-video-q3-turbo"
        const val VIDEO_VIDUQ3_FAST_IMAGE = "/openapi/v2/vidu/image-to-video-q3-turbo"
    }

    // ── 通用任务查询 ───────────────────────────────────

    suspend fun queryTask(taskId: String): QuickCreateTaskQueryResponseDto =
        client.get("$BASE_URL$TASK_QUERY") {
            parameter("taskId", taskId)
        }.body()

    // ── Web 快捷创作 v2 ───────────────────────────────

    suspend fun getQuickCreationCategories(): QuickCreationEnvelopeDto<List<QuickCreationCategoryDto>> =
        client.post("$BASE_URL$QC_CATEGORIES") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun getQuickCreationModels(categoryIds: List<String>): QuickCreationEnvelopeDto<List<QuickCreationModelDto>> =
        client.post("$BASE_URL$QC_MODELS") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationModelRequestDto(categoryIds))
        }.body()

    suspend fun getQuickCreationModes(categoryId: String): QuickCreationEnvelopeDto<List<String>> =
        client.post("$BASE_URL$QC_CREATION_MODES") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("categoryId" to categoryId))
        }.body()

    suspend fun previewQuickCreationFee(
        request: QuickCreationCreateRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationFeePreviewDto> =
        client.post("$BASE_URL$QC_FEE_PREVIEW") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun prepareQuickCreation(
        request: QuickCreationCreateRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationPrepareDataDto> =
        client.post("$BASE_URL$QC_PREPARE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun commitQuickCreation(
        request: QuickCreationCommitRequestDto,
    ): QuickCreationEnvelopeDto<QuickCreationCommitDataDto> =
        client.post("$BASE_URL$QC_COMMIT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun listQuickCreationTasks(
        page: Int = 1,
        size: Int = 10,
    ): QuickCreationEnvelopeDto<QuickCreationTaskPageDto> =
        client.post("$BASE_URL$QC_TASK_LIST") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskPageRequestDto(page, size))
        }.body()

    suspend fun getQuickCreationTaskDetail(
        outputId: String,
    ): QuickCreationEnvelopeDto<QuickCreationTaskRecordDto> =
        client.post("$BASE_URL$QC_TASK_DETAIL") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskDetailRequestDto(outputId))
        }.body()

    suspend fun cancelQuickCreationTask(
        taskId: String,
    ): QuickCreationEnvelopeDto<JsonElement> =
        client.post("$BASE_URL$QC_TASK_CANCEL") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationTaskCancelRequestDto(taskId.encodeURLParameter()))
        }.body()

    suspend fun listQuickCreationProjects(
        page: Int = 1,
        size: Int = 20,
    ): QuickCreationEnvelopeDto<QuickCreationProjectPageDto> =
        client.post("$BASE_URL$QC_PROJECT_LIST") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationProjectPageRequestDto(page, size))
        }.body()

    suspend fun getQuickCreationInspirationTags(): QuickCreationEnvelopeDto<List<QuickCreationCategoryDto>> =
        client.post("$BASE_URL$QC_INSPIRATION_TAGS") {
            contentType(ContentType.Application.Json)
            setBody(emptyMap<String, String>())
        }.body()

    suspend fun getQuickCreationInspirationTemplates(
        page: Int = 1,
        size: Int = 20,
        tagId: String? = null,
    ): QuickCreationEnvelopeDto<QuickCreationInspirationTemplatePageDto> =
        client.post("$BASE_URL$QC_INSPIRATION_TEMPLATES") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationInspirationTemplatePageRequestDto(page, size, tagId))
        }.body()

    suspend fun getQuickCreationInspirationTemplateDetail(
        templateId: String,
    ): QuickCreationEnvelopeDto<QuickCreationInspirationTemplateDetailDto> =
        client.post("$BASE_URL$QC_INSPIRATION_TEMPLATE_DETAIL") {
            contentType(ContentType.Application.Json)
            setBody(QuickCreationInspirationTemplateDetailRequestDto(templateId))
        }.body()

    // ── 媒体上传 ──────────────────────────────────────

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
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    append(HttpHeaders.ContentType, contentType)
                })
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()

        debug(TAG, "uploadMedia: code=${response.code} message=${response.message}")
        debug(TAG, "  response.url       = ${response.url}")
        debug(TAG, "  response.data.type = ${response.data?.type}")
        debug(TAG, "  response.data.fileName = ${response.data?.fileName}")
        return response
    }

    // ── 图片创作 (全能图片G-2.0) ─────────────────────

    suspend fun imageG2TextToImage(request: AllPowerImageG2TextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_G2_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageG2ImageToImage(request: AllPowerImageG2ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_G2_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片G-2.0 低价版) ────────────────

    suspend fun imageG2CheapTextToImage(request: AllPowerImageG2TextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_G2_CHEAP_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageG2CheapImageToImage(request: AllPowerImageG2ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_G2_CHEAP_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片X) ──────────────────────────

    suspend fun imageXTextToImage(request: AllPowerImageXTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_X_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageXImageToImage(request: AllPowerImageXImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_X_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片X 低价版) ───────────────────

    suspend fun imageXCheapTextToImage(request: AllPowerImageXTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_X_CHEAP_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageXCheapImageToImage(request: AllPowerImageXImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_X_CHEAP_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片PRO) ────────────────────────

    suspend fun imageProTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_PRO_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageProImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_PRO_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片PRO 低价版) ────────────────

    suspend fun imageProCheapTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_PRO_CHEAP_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageProCheapImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_PRO_CHEAP_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片V2) ─────────────────────────

    suspend fun imageV2TextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_V2_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageV2ImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_V2_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (全能图片V2 低价版) ────────────────

    suspend fun imageV2CheapTextToImage(request: AllPowerImageV2ProTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_V2_CHEAP_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageV2CheapImageToImage(request: AllPowerImageV2ProImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_V2_CHEAP_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (Seedream 5.0 Lite) ─────────────────

    suspend fun imageSeedream5TextToImage(request: SeedreamV5LiteTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_SEEDREAM5_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageSeedream5ImageToImage(request: SeedreamV5LiteImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_SEEDREAM5_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 图片创作 (Seedream 4.0) ──────────────────────

    suspend fun imageSeedream4TextToImage(request: SeedreamV5LiteTextToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_SEEDREAM4_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun imageSeedream4ImageToImage(request: SeedreamV4ImageToImageRequestDto): QuickCreateTaskQueryResponseDto =
        client.post("$BASE_URL$IMAGE_SEEDREAM4_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (HappyHorse) ────────────────────────

    suspend fun happyHorseTextToVideo(request: HappyHorseTextToVideoRequestDto): HappyHorseTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_HH_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun happyHorseImageToVideo(request: HappyHorseImageToVideoRequestDto): HappyHorseTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_HH_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Seedance 2.0) ─────────────────────

    suspend fun seedance2TextToVideo(request: SeedanceTextToVideoRequestDto): SeedanceTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_SD2_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun seedance2ImageToVideo(request: SeedanceImageToVideoRequestDto): SeedanceImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_SD2_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Seedance 2.0 Fast) ───────────────

    suspend fun seedance2FastTextToVideo(request: SeedanceTextToVideoRequestDto): SeedanceTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_SD2F_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun seedance2FastImageToVideo(request: SeedanceImageToVideoRequestDto): SeedanceImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_SD2F_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-4K) ───────────────────────

    suspend fun klingO34KImageToVideo(request: KlingO34KImageToVideoRequestDto): KlingO34KImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KL3_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-Pro) ──────────────────────

    suspend fun klingO3ProTextToVideo(request: KlingO3ProTextToVideoRequestDto): KlingO3ProTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO3P_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO3ProImageToVideo(request: KlingO3ProImageToVideoRequestDto): KlingO3ProImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO3P_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O3-Std) ──────────────────────

    suspend fun klingO3StdTextToVideo(request: KlingO3StdTextToVideoRequestDto): KlingO3StdTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO3S_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO3StdImageToVideo(request: KlingO3StdImageToVideoRequestDto): KlingO3StdImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO3S_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (可灵 O1) ───────────────────────────

    suspend fun klingO1TextToVideo(request: KlingO1TextToVideoRequestDto): KlingO1TextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO1_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun klingO1ImageToVideo(request: KlingO1ImageToVideoRequestDto): KlingO1ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_KLO1_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (万相 2.7) ─────────────────────────

    suspend fun wan27TextToVideo(request: Wan27TextToVideoRequestDto): Wan27TextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_WAN27_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun wan27ImageToVideo(request: Wan27ImageToVideoRequestDto): Wan27ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_WAN27_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (万相 2.6) ─────────────────────────

    suspend fun wan26ImageToVideo(request: Wan26ImageToVideoRequestDto): Wan26ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_WAN26_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (PixVerse V6) ─────────────────────

    suspend fun pixVerseV6TextToVideo(request: PixVerseV6TextToVideoRequestDto): PixVerseV6TextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_PX6_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun pixVerseV6ImageToVideo(request: PixVerseV6ImageToVideoRequestDto): PixVerseV6ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_PX6_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频V3.1 Fast) ───────────────

    suspend fun allPowerV31FastTextToVideo(request: AllPowerVideoV31FastTextToVideoRequestDto): AllPowerVideoV31FastTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_V31F_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31FastImageToVideo(request: AllPowerVideoV31FastImageToVideoRequestDto): AllPowerVideoV31FastImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_V31F_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31FastStartEndToVideo(request: AllPowerVideoV31FastStartEndToVideoRequestDto): AllPowerVideoV31FastStartEndToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_V31F_START_END") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频V3.1 Pro) ───────────────

    suspend fun allPowerV31ProTextToVideo(request: AllPowerVideoV31ProTextToVideoRequestDto): AllPowerVideoV31ProTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_V31P_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerV31ProImageToVideo(request: AllPowerVideoV31ProImageToVideoRequestDto): AllPowerVideoV31ProImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_V31P_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (全能视频X) ────────────────────────

    suspend fun allPowerVXTextToVideo(request: AllPowerVideoXTextToVideoRequestDto): AllPowerVideoXTextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VX_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerVXImageToVideo(request: AllPowerVideoXImageToVideoRequestDto): AllPowerVideoXImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VX_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun allPowerVXCheapImageToVideo(request: AllPowerVideoXCheapImageToVideoRequestDto): AllPowerVideoXCheapImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VXC_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    // ── 视频创作 (Vidu Q3) ─────────────────────────

    suspend fun viduQ3ProTextToVideo(request: ViduQ3TextToVideoRequestDto): ViduQ3TextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VIDUQ3_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3ProImageToVideo(request: ViduQ3ImageToVideoRequestDto): ViduQ3ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VIDUQ3_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3TurboTextToVideo(request: ViduQ3TextToVideoRequestDto): ViduQ3TextToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VIDUQ3_FAST_TEXT") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun viduQ3TurboImageToVideo(request: ViduQ3ImageToVideoRequestDto): ViduQ3ImageToVideoResponseDto =
        client.post("$BASE_URL$VIDEO_VIDUQ3_FAST_IMAGE") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
}
