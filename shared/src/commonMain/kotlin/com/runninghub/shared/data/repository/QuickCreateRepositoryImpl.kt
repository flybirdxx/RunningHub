package com.runninghub.shared.data.repository

import com.runninghub.shared.data.remote.api.QuickCreateApi
import com.runninghub.shared.data.remote.dto.*
import com.runninghub.shared.domain.repository.ImageGenerationRequest
import com.runninghub.shared.domain.repository.ImageModel
import com.runninghub.shared.domain.repository.QuickCreateRepository
import com.runninghub.shared.domain.repository.QuickCreateResultItem
import com.runninghub.shared.domain.repository.QuickCreateTaskStatus
import com.runninghub.shared.domain.repository.SettingsRepository
import com.runninghub.shared.domain.repository.VideoGenerationRequest
import com.runninghub.shared.domain.repository.VideoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private fun debug(tag: String, msg: String) {
    println("[$tag] $msg")
}

private fun mapResults(results: List<QuickCreateResultDto>?): List<QuickCreateResultItem> =
    results?.map {
        QuickCreateResultItem(
            url = it.url,
            type = it.outputType ?: it.type ?: "unknown",
            thumbnailUrl = it.thumbnailUrl,
            width = it.width,
            height = it.height,
            duration = it.duration,
        )
    } ?: emptyList()

private fun pollTaskStatus(
    api: QuickCreateApi,
    taskId: String,
): Flow<QuickCreateTaskStatus> = flow {
    var attempts = 0
    val maxAttempts = 120
    while (attempts < maxAttempts) {
        val queryResponse = api.queryTask(taskId)
        attempts++
        val results = mapResults(queryResponse.results)

        val taskStatus: QuickCreateTaskStatus = when (queryResponse.status) {
            QuickCreateResult.STATUS_SUCCESS -> QuickCreateTaskStatus.Success(taskId, results)
            QuickCreateResult.STATUS_FAILED -> QuickCreateTaskStatus.Failed(taskId, queryResponse.errorMessage ?: "任务失败")
            QuickCreateResult.STATUS_RUNNING -> QuickCreateTaskStatus.Running(taskId, queryResponse.progress)
            QuickCreateResult.STATUS_QUEUING -> QuickCreateTaskStatus.Queuing(taskId)
            else -> QuickCreateTaskStatus.Queuing(taskId)
        }

        emit(taskStatus)
        if (taskStatus is QuickCreateTaskStatus.Success || taskStatus is QuickCreateTaskStatus.Failed) {
            return@flow
        }
        delay(2000)
    }
    emit(QuickCreateTaskStatus.Error("任务超时"))
}

class QuickCreateRepositoryImpl(
    private val quickCreateApi: QuickCreateApi,
    private val settingsRepository: SettingsRepository,
) : QuickCreateRepository {

    // ── 图片创作 ───────────────────────────────────────

    override fun generateImage(request: ImageGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            val hasRef = !request.referenceImageUri.isNullOrBlank()
            val refImageUrl = request.referenceImageUri ?: ""  // validated non-null reference, replaces all !! usage
            val model = ImageModel.entries.find { it.modelKey == request.model }
                ?: ImageModel.ALL_POWER_IMAGE_G_2_OFFICIAL

            // 根据模型路由到对应 API 端点
            val response: TaskResponse = when (model) {

                // 全能图片 G-2.0 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_G_2_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageG2ImageToImage(
                            AllPowerImageG2ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    } else {
                        quickCreateApi.imageG2TextToImage(
                            AllPowerImageG2TextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    }
                }

                // 全能图片 G-2.0 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_G_2_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageG2CheapImageToImage(
                            AllPowerImageG2ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    } else {
                        quickCreateApi.imageG2CheapTextToImage(
                            AllPowerImageG2TextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                quality = request.quality,
                            )
                        )
                    }
                }

                // 全能图片 X 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_X_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageXImageToImage(
                            AllPowerImageXImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.imageXTextToImage(
                            AllPowerImageXTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                outputFormat = "png",
                            )
                        )
                    }
                }

                // 全能图片 X 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_X_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageXCheapImageToImage(
                            AllPowerImageXImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.imageXCheapTextToImage(
                            AllPowerImageXTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                outputFormat = "png",
                            )
                        )
                    }
                }

                // 全能图片 PRO 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_PRO_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageProImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageProTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 PRO 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_PRO_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageProCheapImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageProCheapTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 2.0 官方版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_2_OFFICIAL -> {
                    if (hasRef) {
                        quickCreateApi.imageV2ImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageV2TextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // 全能图片 2.0 低价版 (文生图 + 图生图)
                ImageModel.ALL_POWER_IMAGE_2_CHEAP -> {
                    if (hasRef) {
                        quickCreateApi.imageV2CheapImageToImage(
                            AllPowerImageV2ProImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageV2CheapTextToImage(
                            AllPowerImageV2ProTextToImageRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                            )
                        )
                    }
                }

                // Seedream 5.0 Lite (仅文生图)
                ImageModel.SEEDREAM_5_0_LITE -> {
                    quickCreateApi.imageSeedream5TextToImage(
                        SeedreamV5LiteTextToImageRequestDto(
                            prompt = request.prompt,
                            resolution = request.resolution,
                        )
                    )
                }

                // Seedream 4.0 (文生图 + 图生图)
                ImageModel.SEEDREAM_4_0 -> {
                    if (hasRef) {
                        quickCreateApi.imageSeedream4ImageToImage(
                            SeedreamV4ImageToImageRequestDto(
                                prompt = request.prompt,
                                imageUrls = listOf(refImageUrl),
                                resolution = request.resolution,
                            )
                        )
                    } else {
                        quickCreateApi.imageSeedream4TextToImage(
                            SeedreamV5LiteTextToImageRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                            )
                        )
                    }
                }
            }

            val taskId = response.taskId

            if (response.status == QuickCreateResult.STATUS_FAILED || response.errorCode?.isNotBlank() == true) {
                emit(QuickCreateTaskStatus.Failed(taskId, response.errorMessage ?: "提交失败"))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))
            pollTaskStatus(quickCreateApi, taskId).collect { emit(it) }

        } catch (e: Exception) {
            emit(QuickCreateTaskStatus.Error(e.message ?: "未知错误"))
        }
    }

    // ── 视频创作 ───────────────────────────────────────

    override fun generateVideo(request: VideoGenerationRequest): Flow<QuickCreateTaskStatus> = flow {
        emit(QuickCreateTaskStatus.Submitting)

        try {
            val hasImageRef = !request.referenceImageUri.isNullOrBlank()
            val refImageUrl = request.referenceImageUri ?: ""  // validated non-null reference, replaces all !! usage
            val hasFirstFrame = !request.firstFrameImageUri.isNullOrBlank()
            val hasLastFrame = !request.lastFrameImageUri.isNullOrBlank()

            val model = VideoModel.entries.find { it.modelKey == request.model }
                ?: VideoModel.HAPPYHORSE

            val response: TaskResponse = when (model) {

                // HappyHorse: 文生视频 + 图生视频
                VideoModel.HAPPYHORSE -> {
                    if (hasImageRef) {
                        quickCreateApi.happyHorseImageToVideo(
                            HappyHorseImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.happyHorseTextToVideo(
                            HappyHorseTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                seed = request.seed,
                            )
                        )
                    }
                }

                // Seedance 2.0: 文生视频 + 图生视频(含首尾帧)
                VideoModel.SEEDANCE_2_0 -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.seedance2ImageToVideo(
                            SeedanceImageToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                firstFrameUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastFrameUrl = request.lastFrameImageUri,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.seedance2TextToVideo(
                            SeedanceTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    }
                }

                // Seedance 2.0 Fast: 文生视频 + 图生视频(含首尾帧)
                VideoModel.SEEDANCE_2_0_FAST -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.seedance2FastImageToVideo(
                            SeedanceImageToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                firstFrameUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastFrameUrl = request.lastFrameImageUri,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    } else {
                        quickCreateApi.seedance2FastTextToVideo(
                            SeedanceTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudio = request.generateAudio,
                                ratio = request.aspectRatio,
                            )
                        )
                    }
                }

                // 可灵 3.0-4K: 仅图生视频
                VideoModel.KLING_3_0_4K -> {
                    quickCreateApi.klingO34KImageToVideo(
                        KlingO34KImageToVideoRequestDto(
                            prompt = request.prompt,
                            firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                            lastImageUrl = request.lastFrameImageUri,
                            duration = request.duration,
                            sound = request.generateAudio,
                        )
                    )
                }

                // 可灵 O3-Pro: 文生视频 + 图生视频
                VideoModel.KLING_O3_PRO -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.klingO3ProImageToVideo(
                            KlingO3ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO3ProTextToVideo(
                            KlingO3ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 可灵 O3-Std: 文生视频 + 图生视频
                VideoModel.KLING_O3_STD -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.klingO3StdImageToVideo(
                            KlingO3StdImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO3StdTextToVideo(
                            KlingO3StdTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 可灵 O1: 文生视频 + 图生视频
                VideoModel.KLING_O1 -> {
                    if (hasImageRef) {
                        quickCreateApi.klingO1ImageToVideo(
                            KlingO1ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.klingO1TextToVideo(
                            KlingO1TextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                                negativePrompt = request.negativePrompt,
                            )
                        )
                    }
                }

                // 万相 2.7: 文生视频 + 图生视频
                VideoModel.WAN_2_7 -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.wan27ImageToVideo(
                            Wan27ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: request.referenceImageUri ?: "",
                                lastImageUrl = request.lastFrameImageUri,
                                audioUrl = request.referenceAudioUri,
                                negativePrompt = request.negativePrompt,
                                resolution = request.resolution,
                                duration = request.duration.toString(),
                                promptExtend = request.promptExtend,
                                seed = request.seed,
                            )
                        )
                    } else {
                        quickCreateApi.wan27TextToVideo(
                            Wan27TextToVideoRequestDto(
                                prompt = request.prompt,
                                negativePrompt = request.negativePrompt,
                                audioUrl = request.referenceAudioUri,
                                duration = request.duration.toString(),
                                resolution = request.resolution,
                                aspectRatio = request.aspectRatio,
                                promptExtend = request.promptExtend,
                                seed = request.seed,
                            )
                        )
                    }
                }

                // 万相 2.6: 仅图生视频
                VideoModel.WAN_2_6 -> {
                    quickCreateApi.wan26ImageToVideo(
                        Wan26ImageToVideoRequestDto(
                            firstImageUrl = refImageUrl,
                            prompt = request.prompt,
                            resolution = request.resolution,
                            duration = request.duration,
                            shotType = if (hasFirstFrame) "single" else "single",
                        )
                    )
                }

                // PixVerse V6: 文生视频 + 图生视频
                VideoModel.PIXVERSE_V6 -> {
                    if (hasImageRef) {
                        quickCreateApi.pixVerseV6ImageToVideo(
                            PixVerseV6ImageToVideoRequestDto(
                                imageUrl = refImageUrl,
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudioSwitch = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.pixVerseV6TextToVideo(
                            PixVerseV6TextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                generateAudioSwitch = request.generateAudio,
                                aspectRatio = request.aspectRatio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Fast 官方版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_FAST_OFFICIAL -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31FastStartEndToVideo(
                            AllPowerVideoV31FastStartEndToVideoRequestDto(
                                prompt = request.prompt,
                                firstFrameUrl = request.firstFrameImageUri!!,
                                lastFrameUrl = request.lastFrameImageUri,
                                aspectRatio = request.aspectRatio,
                                duration = request.duration,
                                resolution = request.resolution,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31FastImageToVideo(
                            AllPowerVideoV31FastImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31FastTextToVideo(
                            AllPowerVideoV31FastTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Fast 低价版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_FAST_CHEAP -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31FastStartEndToVideo(
                            AllPowerVideoV31FastStartEndToVideoRequestDto(
                                prompt = request.prompt,
                                firstFrameUrl = request.firstFrameImageUri!!,
                                lastFrameUrl = request.lastFrameImageUri,
                                aspectRatio = request.aspectRatio,
                                duration = request.duration,
                                resolution = request.resolution,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31FastImageToVideo(
                            AllPowerVideoV31FastImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31FastTextToVideo(
                            AllPowerVideoV31FastTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                generateAudio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Pro 官方版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_PRO_OFFICIAL -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = request.firstFrameImageUri!!,
                                firstFrameUrl = request.firstFrameImageUri,
                                lastFrameUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31ProTextToVideo(
                            AllPowerVideoV31ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 V3.1 Pro 低价版: 文生视频 + 图生视频 + 首尾帧
                VideoModel.ALL_POWER_VIDEO_V_PRO_CHEAP -> {
                    if (hasFirstFrame && hasLastFrame) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = request.firstFrameImageUri!!,
                                firstFrameUrl = request.firstFrameImageUri,
                                lastFrameUrl = request.lastFrameImageUri,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else if (hasImageRef) {
                        quickCreateApi.allPowerV31ProImageToVideo(
                            AllPowerVideoV31ProImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.allPowerV31ProTextToVideo(
                            AllPowerVideoV31ProTextToVideoRequestDto(
                                prompt = request.prompt,
                                resolution = request.resolution,
                                duration = request.duration,
                                aspectRatio = request.aspectRatio,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // 全能视频 X 官方版: 文生视频 + 图生视频
                VideoModel.ALL_POWER_VIDEO_X_OFFICIAL -> {
                    if (hasImageRef) {
                        quickCreateApi.allPowerVXImageToVideo(
                            AllPowerVideoXImageToVideoRequestDto(
                                prompt = request.prompt,
                                imageUrl = refImageUrl,
                                resolution = request.resolution,
                                duration = request.duration.toString(),
                            )
                        )
                    } else {
                        quickCreateApi.allPowerVXTextToVideo(
                            AllPowerVideoXTextToVideoRequestDto(
                                prompt = request.prompt,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                            )
                        )
                    }
                }

                // 全能视频 X 低价版: 仅图生视频(多图)
                VideoModel.ALL_POWER_VIDEO_X_CHEAP -> {
                    val images = listOfNotNull(
                        request.firstFrameImageUri ?: request.referenceImageUri,
                        request.lastFrameImageUri,
                    ).filter { it.isNotBlank() }
                    quickCreateApi.allPowerVXCheapImageToVideo(
                        AllPowerVideoXCheapImageToVideoRequestDto(
                            prompt = request.prompt,
                            aspectRatio = request.aspectRatio,
                            imageUrls = images.ifEmpty { listOf(refImageUrl) },
                            resolution = request.resolution,
                            duration = request.duration,
                        )
                    )
                }

                // Vidu Q3-Pro: 文生视频 + 图生视频
                VideoModel.VIDU_Q3_PRO -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.viduQ3ProImageToVideo(
                            ViduQ3ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.viduQ3ProTextToVideo(
                            ViduQ3TextToVideoRequestDto(
                                prompt = request.prompt,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }

                // Vidu Q3-Pro-Fast: 文生视频 + 图生视频
                VideoModel.VIDU_Q3_PRO_FAST -> {
                    if (hasFirstFrame || hasImageRef) {
                        quickCreateApi.viduQ3TurboImageToVideo(
                            ViduQ3ImageToVideoRequestDto(
                                prompt = request.prompt,
                                firstImageUrl = request.firstFrameImageUri ?: refImageUrl,
                                lastImageUrl = request.lastFrameImageUri,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    } else {
                        quickCreateApi.viduQ3TurboTextToVideo(
                            ViduQ3TextToVideoRequestDto(
                                prompt = request.prompt,
                                style = request.style,
                                aspectRatio = request.aspectRatio,
                                resolution = request.resolution,
                                duration = request.duration,
                                audio = request.generateAudio,
                            )
                        )
                    }
                }
            }

            val taskId = when (val resp = response) {
                is HappyHorseTextToVideoResponseDto -> resp.taskId
                is SeedanceTextToVideoResponseDto -> resp.taskId
                is SeedanceImageToVideoResponseDto -> resp.taskId
                is KlingO34KImageToVideoResponseDto -> resp.taskId
                is KlingO3ProTextToVideoResponseDto -> resp.taskId
                is KlingO3ProImageToVideoResponseDto -> resp.taskId
                is KlingO3StdTextToVideoResponseDto -> resp.taskId
                is KlingO3StdImageToVideoResponseDto -> resp.taskId
                is KlingO1TextToVideoResponseDto -> resp.taskId
                is KlingO1ImageToVideoResponseDto -> resp.taskId
                is Wan27TextToVideoResponseDto -> resp.taskId
                is Wan27ImageToVideoResponseDto -> resp.taskId
                is Wan26ImageToVideoResponseDto -> resp.taskId
                is PixVerseV6TextToVideoResponseDto -> resp.taskId
                is PixVerseV6ImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31FastStartEndToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31ProTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoV31ProImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoXTextToVideoResponseDto -> resp.taskId
                is AllPowerVideoXImageToVideoResponseDto -> resp.taskId
                is AllPowerVideoXCheapImageToVideoResponseDto -> resp.taskId
                is ViduQ3TextToVideoResponseDto -> resp.taskId
                is ViduQ3ImageToVideoResponseDto -> resp.taskId
                else -> ""
            }

            if (response.status == QuickCreateResult.STATUS_FAILED || response.errorCode?.isNotBlank() == true) {
                emit(QuickCreateTaskStatus.Failed(taskId, response.errorMessage ?: "提交失败"))
                return@flow
            }

            emit(QuickCreateTaskStatus.Queuing(taskId))
            pollTaskStatus(quickCreateApi, taskId).collect { emit(it) }

        } catch (e: Exception) {
            emit(QuickCreateTaskStatus.Error(e.message ?: "未知错误"))
        }
    }

    // ── 媒体上传 ───────────────────────────────────────

    override suspend fun uploadMedia(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): Result<String> = runCatching {
        val TAG = "QuickCreateRepo"
        debug(TAG, "uploadMedia: START")
        debug(TAG, "  fileName  = $fileName")
        debug(TAG, "  mimeType  = $mimeType")
        debug(TAG, "  fileBytes = ${fileBytes.size} bytes")

        val apiKey = settingsRepository.getApiKey()
        debug(TAG, "  apiKey found = ${!apiKey.isNullOrBlank()}")
        if (apiKey.isNullOrBlank()) {
            throw IllegalStateException("请先登录获取 API Key")
        }

        debug(TAG, "  calling QuickCreateApi.uploadMedia...")
        val uploadResp = quickCreateApi.uploadMedia(apiKey, fileBytes, fileName, mimeType)
        debug(TAG, "  response.code    = ${uploadResp.code}")
        debug(TAG, "  response.message = ${uploadResp.message}")
        debug(TAG, "  response.url     = ${uploadResp.url}")

        if (!uploadResp.isSuccess) {
            throw IllegalStateException("Media upload failed: [${uploadResp.code}] ${uploadResp.message}")
        }

        uploadResp.url ?: throw IllegalStateException("Server returned empty url in media upload response")
    }.onFailure { e ->
        val TAG = "QuickCreateRepo"
        debug(TAG, "uploadMedia: FAILED")
        debug(TAG, "  exception = ${e::class.simpleName}: ${e.message}")
    }
}
