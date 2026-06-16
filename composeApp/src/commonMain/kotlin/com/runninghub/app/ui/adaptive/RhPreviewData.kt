package com.runninghub.app.ui.adaptive

import com.runninghub.app.ui.feature.quickcreate.ImageAspectRatio
import com.runninghub.app.ui.feature.quickcreate.ImageConfig
import com.runninghub.app.ui.feature.quickcreate.ImageModel
import com.runninghub.app.ui.feature.quickcreate.ImageQuality
import com.runninghub.app.ui.feature.quickcreate.ImageResolution
import com.runninghub.app.ui.feature.quickcreate.MediaReference
import com.runninghub.app.ui.feature.quickcreate.QuickCreateMediaType
import com.runninghub.app.ui.feature.quickcreate.QuickCreateTab
import com.runninghub.app.ui.feature.quickcreate.QuickCreateUiState
import com.runninghub.app.ui.feature.quickcreate.UploadStatus
import com.runninghub.shared.domain.model.AppDetail
import com.runninghub.shared.domain.model.Author
import com.runninghub.shared.domain.model.Cover
import com.runninghub.shared.domain.model.CoverMediaType
import com.runninghub.shared.domain.model.InputNode
import com.runninghub.shared.domain.model.MemberInfo
import com.runninghub.shared.domain.model.StatisticsInfo
import com.runninghub.shared.domain.model.Tag
import com.runninghub.shared.domain.model.TagSimple
import com.runninghub.shared.domain.model.TaskOutput
import com.runninghub.shared.domain.model.User
import com.runninghub.shared.domain.model.WalletInfo
import com.runninghub.shared.domain.model.WebApp

internal fun previewTag(
    id: String,
    name: String,
    level: Int = 2,
    childTags: List<Tag>? = null,
): Tag = Tag(
    id = id,
    name = name,
    level = level,
    parentId = null,
    rang = "1",
    enable = true,
    childTags = childTags,
)

internal fun previewTagSimple(
    id: String,
    name: String,
): TagSimple = TagSimple(
    id = id,
    name = name,
)

internal fun previewAuthor(): Author = Author(
    id = "author-1",
    name = "Astra Studio",
    avatar = "https://picsum.photos/seed/rh-author/120/120",
    intro = "Visual workflows for creators.",
    followCount = "258",
    fansCount = "12.8k",
    likeCount = "32.4k",
    collectCount = "8.1k",
    bgImage = "https://picsum.photos/seed/rh-author-bg/1200/600",
)

internal fun previewWebApp(
    id: String,
    title: String,
    tags: List<TagSimple> = listOf(
        previewTagSimple("portrait", "Portrait"),
        previewTagSimple("detail", "Detail"),
    ),
    author: Author = previewAuthor(),
): WebApp = WebApp(
    id = id,
    title = title,
    description = "Adaptive preview sample",
    thumbnailUrl = "https://picsum.photos/seed/$id/600/800",
    coverUrl = "https://picsum.photos/seed/${id}_cover/900/1200",
    coverMediaType = CoverMediaType.IMAGE,
    videoUrl = null,
    coverWidth = "900",
    coverHeight = "1200",
    author = author,
    tags = tags,
    likeCount = "1280",
    collectCount = "340",
    useCount = "2511",
    pv = "12408",
    carefullyChosen = true,
)

internal fun previewCategories(): List<Tag> = listOf(
    previewTag(
        id = "portrait-root",
        name = "Portrait",
        level = 1,
        childTags = listOf(
            previewTag(id = "portrait-1", name = "Selfie"),
            previewTag(id = "portrait-2", name = "Studio"),
        ),
    ),
    previewTag(
        id = "workflow-root",
        name = "Workflow",
        level = 1,
        childTags = listOf(
            previewTag(id = "workflow-1", name = "Image"),
            previewTag(id = "workflow-2", name = "Video"),
        ),
    ),
    previewTag(
        id = "enhance-root",
        name = "Enhance",
        level = 1,
        childTags = listOf(
            previewTag(id = "enhance-1", name = "Upscale"),
            previewTag(id = "enhance-2", name = "Restore"),
        ),
    ),
)

internal fun previewUser(): User = User(
    id = "user-1",
    nickName = "Creator with a Very Long Display Name",
    headIcon = "https://picsum.photos/seed/rh-user/200/200",
    mobile = "13800138000",
    totalCoin = "12880",
    memberInfo = MemberInfo(
        memberName = "RunningHub Pro Annual",
        memberExpiredTime = "2027-06-30",
        userType = "PRO",
        memberRemainingDays = "379",
        expired = false,
    ),
    walletInfo = WalletInfo(
        balance = 248.6,
        currency = "CNY",
        currencySymbol = "¥",
    ),
    apiKey = null,
    apiType = "runninghub",
    introduce = "Cross-platform workflow creator.",
    fanCount = "9876",
    followCount = "268",
    likeCount = "14500",
    collectCount = "3890",
)

internal fun previewAppDetail(): AppDetail = AppDetail(
    id = "detail-1",
    name = "Cinema Portrait Generator with Long Title",
    workflowId = "wf-1001",
    description = "Generate portrait images with reference images, ratio controls, and long prompts without breaking the layout.",
    tags = listOf(
        previewTagSimple("portrait", "Portrait"),
        previewTagSimple("cinema", "Cinema"),
        previewTagSimple("kmp", "Adaptive"),
    ),
    owner = previewAuthor(),
    publishTime = "2026-06-16 10:00",
    inputNodes = listOf(
        InputNode(
            nodeId = "prompt",
            nodeName = "Prompt",
            fieldName = "prompt",
            fieldValue = "A cinematic portrait with shallow depth of field.",
            fieldData = "multiline",
            fieldType = "STRING",
            description = "Prompt",
        ),
        InputNode(
            nodeId = "ratio",
            nodeName = "Aspect Ratio",
            fieldName = "ratio",
            fieldValue = "3:4",
            fieldData = "[\"1:1\",\"3:4\",\"9:16\"]",
            fieldType = "LIST",
            description = "Aspect ratio",
        ),
        InputNode(
            nodeId = "reference",
            nodeName = "Reference Image",
            fieldName = "reference_image",
            fieldValue = null,
            fieldData = null,
            fieldType = "IMAGE",
            description = "Reference image",
        ),
    ),
    covers = listOf(
        Cover(
            url = "https://picsum.photos/seed/rh-detail-1/1200/800",
            imageWidth = "1200",
            imageHeight = "800",
        ),
        Cover(
            url = "https://picsum.photos/seed/rh-detail-2/1200/800",
            imageWidth = "1200",
            imageHeight = "800",
        ),
    ),
    statisticsInfo = StatisticsInfo(
        likeCount = "1450",
        collectCount = "240",
        useCount = "3620",
        pv = "26990",
    ),
    authorName = "Astra Studio",
    authorAvatar = "https://picsum.photos/seed/rh-author/120/120",
    runningSuccessRate = "96%",
    avgRunningSeconds = "28",
    instanceType = "GPU",
)

internal fun previewTaskOutputs(): List<TaskOutput> = listOf(
    TaskOutput(
        fileUrl = "https://picsum.photos/seed/rh-output-1/1200/800",
        fileName = "result-1.png",
        fileType = "image",
        failedReason = null,
    ),
    TaskOutput(
        fileUrl = "https://picsum.photos/seed/rh-output-2/1200/800",
        fileName = "result-2.png",
        fileType = "image",
        failedReason = null,
    ),
)

internal fun previewQuickCreateUiState(
    tuneSheetVisible: Boolean = false,
): QuickCreateUiState = QuickCreateUiState(
    currentTab = QuickCreateTab.IMAGE,
    imageConfig = ImageConfig(
        prompt = "Build a cinematic portrait with clean skin texture, glass reflections, and a restrained studio palette.",
        model = ImageModel.SEEDREAM_5,
        aspectRatio = ImageAspectRatio.RATIO_3_4,
        resolution = ImageResolution.RES_2K,
        quality = ImageQuality.QUALITY_HIGH,
        mediaReferences = listOf(
            MediaReference(
                id = "image-ref-1",
                type = QuickCreateMediaType.IMAGE,
                uri = "https://picsum.photos/seed/rh-ref-image/600/800",
                displayName = "portrait-reference.png",
                fileSizeBytes = 2_450_000,
                uploadStatus = UploadStatus.DONE,
                uploadProgress = 1f,
                remoteUrl = "https://picsum.photos/seed/rh-ref-image/600/800",
            ),
            MediaReference(
                id = "video-ref-1",
                type = QuickCreateMediaType.VIDEO,
                uri = "https://example.com/reference.mp4",
                displayName = "camera-motion-reference.mp4",
                fileSizeBytes = 12_800_000,
                uploadStatus = UploadStatus.PROCESSING,
                uploadProgress = 0.8f,
                remoteUrl = "https://example.com/reference.mp4",
            ),
        ),
    ),
    estimatedCost = 2.25,
    tuneSheetVisible = tuneSheetVisible,
)
