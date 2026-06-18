package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.*
import com.runninghub.shared.domain.repository.QuickCreationServiceField
import com.runninghub.shared.domain.repository.QuickCreationServiceFieldInputChild
import com.runninghub.shared.domain.repository.QuickCreationServiceModel

enum class TuneTab(val label: String) {
    STYLE("风格"),
    QUALITY("质量"),
    RATIO("比例"),
    COUNT("数量"),
    ADVANCED("高级"),
}

enum class ImageStylePreset(val displayName: String, val description: String) {
    PHOTOREAL("摄影写实", "生成接近真实相机拍摄的照片效果，适合产品展示、人物写真等场景"),
    ARTISTIC("艺术插画", "将文字描述转化为富有艺术感的插画作品，适合创意概念设计"),
    RENDER_3D("3D 渲染", "三维软件渲染风格，适合建筑、产品设计、卡通角色等场景"),
    WATERCOLOR("水彩手绘", "模拟水彩画和手绘插画风格，柔和的色彩过渡和纸纹质感"),
}

enum class ImageQualityLevel(val displayName: String, val description: String) {
    FAST("快速", "快速生成，适合预览和迭代"),
    STANDARD("标准", "平衡速度与质量，日常使用推荐"),
    HD("高清", "最高质量，适合最终输出和商用"),
}

enum class ImageCount(val displayName: String, val count: Int) {
    ONE("1 张", 1),
    TWO("2 张", 2),
    FOUR("4 张", 4),
}

enum class VideoQualityLevel(val displayName: String, val description: String) {
    STANDARD("标准", "720p，适合社交媒体分享"),
    HD("高清", "1080p，推荐日常使用"),
    ULTRA("超清", "4K，适合高质量输出"),
}

enum class VideoCount(val displayName: String, val count: Int) {
    ONE("1 个", 1),
    TWO("2 个", 2),
}

@Composable
fun QuickCreateModelSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageServiceModelSelected: (QuickCreationServiceModel) -> Unit,
    onVideoServiceModelSelected: (QuickCreationServiceModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val models = if (isImage) uiState.serviceImageModels else uiState.serviceVideoModels
    val selected = if (isImage) uiState.selectedImageServiceModel else uiState.selectedVideoServiceModel
    val onSelect = if (isImage) onImageServiceModelSelected else onVideoServiceModelSelected

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
            color = DarkSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceLG),
            ) {
                SheetHeader(title = "选择模型", onDismiss = onDismiss)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = Dimens.SpaceLG),
                ) {
                    when {
                        uiState.serviceModelsLoading -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.SpaceLG),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Primary300,
                                strokeWidth = 2.dp,
                            )
                            Text("正在加载模型", fontSize = 13.sp, color = Neutral400)
                        }
                        models.isEmpty() -> Text(
                            "暂未获取到服务端模型，将继续使用本地兼容参数。",
                            fontSize = 13.sp,
                            color = Neutral500,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = Dimens.SpaceLG),
                        )
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                            models
                                .groupBy { it.groupName?.takeIf { name -> name.isNotBlank() } ?: "其他模型" }
                                .forEach { (groupName, groupModels) ->
                                    item {
                                        Text(
                                            text = groupName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Primary300,
                                            modifier = Modifier.padding(top = Dimens.SpaceSM),
                                        )
                                    }
                                    items(groupModels) { model ->
                                        ServiceModelListRow(
                                            model = model,
                                            selected = model.isSameQuickCreationServiceModel(selected),
                                            onClick = { onSelect(model) },
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCreateParamsSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageModelSelected: (ImageModel) -> Unit,
    onImageServiceModelSelected: (QuickCreationServiceModel) -> Unit = {},
    onImageServiceParamChange: (String, String) -> Unit,
    onVideoModelSelected: (VideoModel) -> Unit = {},
    onVideoServiceModelSelected: (QuickCreationServiceModel) -> Unit = {},
    onVideoServiceParamChange: (String, String) -> Unit,
    onImageRatioChange: (ImageAspectRatio) -> Unit = {},
    onImageResChange: (ImageResolution) -> Unit = {},
    onImageQualityChange: (ImageQuality) -> Unit = {},
    onVideoRatioChange: (VideoAspectRatio) -> Unit = {},
    onVideoResChange: (VideoResolution) -> Unit = {},
    onVideoDurationChange: (VideoDuration) -> Unit = {},
    onToggleRealistic: () -> Unit,
    onToggleAudio: () -> Unit = {},
    onImageCountChange: (ImageCount) -> Unit = {},
    onVideoCountChange: (VideoCount) -> Unit = {},
    onImageStyleChange: (ImageStylePreset) -> Unit = {},
    onImageSeedChange: (Int?) -> Unit = {},
    onVideoSeedChange: (Int?) -> Unit = {},
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit = { _, _ -> },
    onRemoveMedia: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
            color = DarkSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceLG),
            ) {
                SheetHeader(title = "更多参数", onDismiss = onDismiss)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = Dimens.SpaceLG),
                ) {
                    if (isImage) {
                        ImageParamsContent(
                            models = ImageModel.entries,
                            selected = uiState.imageConfig.model,
                            selectedServiceModel = uiState.selectedImageServiceModel,
                            serviceParams = uiState.imageServiceParams,
                            mediaReferences = uiState.imageConfig.mediaReferences,
                            seed = uiState.imageConfig.seed,
                            onSelect = onImageModelSelected,
                            onServiceParamChange = onImageServiceParamChange,
                            onServiceUploadFieldClick = onServiceUploadFieldClick,
                            onRemoveMedia = onRemoveMedia,
                            onSeedChange = onImageSeedChange,
                        )
                    } else {
                        VideoParamsContent(
                            selectedServiceModel = uiState.selectedVideoServiceModel,
                            serviceParams = uiState.videoServiceParams,
                            mediaReferences = uiState.videoConfig.mediaReferences,
                            realistic = uiState.videoConfig.realisticMode,
                            seed = uiState.videoConfig.seed,
                            onServiceParamChange = onVideoServiceParamChange,
                            onServiceUploadFieldClick = onServiceUploadFieldClick,
                            onRemoveMedia = onRemoveMedia,
                            onRealisticToggle = onToggleRealistic,
                            onSeedChange = onVideoSeedChange,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceSM, start = Dimens.SpaceLG, end = Dimens.SpaceLG, bottom = Dimens.SpaceSM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Neutral100,
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭",
                tint = Neutral500,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ServiceModelListRow(
    model: QuickCreationServiceModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = if (selected) Primary300.copy(alpha = 0.10f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) Primary300 else DarkOutlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Primary300 else Neutral100,
                    maxLines = 1,
                )
                model.quickCreationServiceModelSubtitle().takeIf { it.isNotBlank() }?.let { subtitle ->
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Neutral500,
                        maxLines = 1,
                    )
                }
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Primary300,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ImageParamsContent(
    models: List<ImageModel>,
    selected: ImageModel,
    selectedServiceModel: QuickCreationServiceModel?,
    serviceParams: Map<String, String>,
    mediaReferences: List<MediaReference>,
    seed: Int?,
    onSelect: (ImageModel) -> Unit,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        selectedServiceModel
            ?.fields
            .orEmpty()
            .filter { it.isQuickCreationServiceFieldRenderable() }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    params = selectedServiceModel.quickCreationParamsWithFieldAliases(serviceParams),
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        Text(
            text = "本地兼容模型",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            models.forEach { model ->
                val isSelected = model == selected
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onSelect(model) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = model.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Primary300 else Neutral200,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Primary300,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceXL))
        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

@Composable
private fun VideoParamsContent(
    selectedServiceModel: QuickCreationServiceModel?,
    serviceParams: Map<String, String>,
    mediaReferences: List<MediaReference>,
    realistic: Boolean,
    seed: Int?,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onRealisticToggle: () -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        selectedServiceModel
            ?.fields
            .orEmpty()
            .filter { it.isQuickCreationServiceFieldRenderable() }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    params = selectedServiceModel.quickCreationParamsWithFieldAliases(serviceParams),
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        ToggleChip(
            label = "真人模式",
            enabled = realistic,
            onClick = onRealisticToggle,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.SpaceLG))
        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

@Composable
fun TuneBottomSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageModelSelected: (ImageModel) -> Unit,
    onImageServiceModelSelected: (QuickCreationServiceModel) -> Unit,
    onImageServiceParamChange: (String, String) -> Unit,
    onVideoModelSelected: (VideoModel) -> Unit,
    onVideoServiceModelSelected: (QuickCreationServiceModel) -> Unit,
    onVideoServiceParamChange: (String, String) -> Unit,
    onImageRatioChange: (ImageAspectRatio) -> Unit,
    onImageResChange: (ImageResolution) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onVideoRatioChange: (VideoAspectRatio) -> Unit,
    onVideoResChange: (VideoResolution) -> Unit,
    onVideoDurationChange: (VideoDuration) -> Unit,
    onToggleRealistic: () -> Unit,
    onToggleAudio: () -> Unit,
    onImageCountChange: (ImageCount) -> Unit,
    onVideoCountChange: (VideoCount) -> Unit,
    onImageStyleChange: (ImageStylePreset) -> Unit,
    onImageSeedChange: (Int?) -> Unit = {},
    onVideoSeedChange: (Int?) -> Unit = {},
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit = { _, _ -> },
    onRemoveMedia: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(TuneTab.STYLE) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
            color = DarkSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceLG),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.SpaceSM, start = Dimens.SpaceLG, end = Dimens.SpaceLG, bottom = Dimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "创作调优",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Neutral100,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Neutral500,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = TuneTab.entries.indexOf(selectedTab),
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = Neutral100,
                    edgePadding = Dimens.SpaceLG,
                    divider = {},
                    indicator = {},
                ) {
                    TuneTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = tab.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Medium else FontWeight.Normal,
                                    color = if (selectedTab == tab) Primary300 else Neutral500,
                                )
                            },
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceMD))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(horizontal = Dimens.SpaceLG),
                ) {
                    when (selectedTab) {
                        TuneTab.STYLE -> {
                            if (isImage) {
                                StyleContent(
                                    presets = ImageStylePreset.entries,
                                    onSelect = onImageStyleChange,
                                )
                            } else {
                                VideoModelContent(
                                    models = VideoModel.entries,
                                    selected = uiState.videoConfig.model,
                                    onSelect = onVideoModelSelected,
                                )
                            }
                        }
                        TuneTab.QUALITY -> {
                            if (isImage) {
                                val model = uiState.imageConfig.model
                                ImageQualityContent(
                                    quality = uiState.imageConfig.quality,
                                    resolution = uiState.imageConfig.resolution,
                                    supportedResolutions = model.supportedResolutions.toList(),
                                    supportedQualities = model.supportedQualities.toList(),
                                    onQualityChange = onImageQualityChange,
                                    onResChange = onImageResChange,
                                )
                            } else {
                                val model = uiState.videoConfig.model
                                VideoQualityContent(
                                    quality = uiState.videoConfig.resolution,
                                    supportedResolutions = model.supportedResolutions.toList(),
                                    onQualityChange = onVideoResChange,
                                )
                            }
                        }
                        TuneTab.RATIO -> {
                            if (isImage) {
                                RatioContent(
                                    ratios = uiState.imageConfig.model.supportedRatios.toList(),
                                    selected = uiState.imageConfig.aspectRatio,
                                    onSelect = onImageRatioChange,
                                )
                            } else {
                                VideoRatioContent(
                                    ratios = uiState.videoConfig.model.supportedRatios.toList(),
                                    selected = uiState.videoConfig.aspectRatio,
                                    onSelect = onVideoRatioChange,
                                )
                            }
                        }
                        TuneTab.COUNT -> {
                            if (isImage) {
                                val curCount = ImageCount.entries.find { it.count == uiState.imageConfig.count } ?: ImageCount.ONE
                                CountContent(
                                    counts = ImageCount.entries,
                                    selected = curCount,
                                    onSelect = onImageCountChange,
                                )
                            } else {
                                val curCount = VideoCount.entries.find { it.count == uiState.videoConfig.count } ?: VideoCount.ONE
                                CountContent(
                                    counts = VideoCount.entries,
                                    selected = curCount,
                                    onSelect = onVideoCountChange,
                                )
                            }
                        }
                        TuneTab.ADVANCED -> {
                            if (isImage) {
                                ImageAdvancedContent(
                                    models = ImageModel.entries,
                                    selected = uiState.imageConfig.model,
                                    serviceModels = uiState.serviceImageModels,
                                    selectedServiceModel = uiState.selectedImageServiceModel,
                                    serviceModelsLoading = uiState.serviceModelsLoading,
                                    serviceParams = uiState.imageServiceParams,
                                    mediaReferences = uiState.imageConfig.mediaReferences,
                                    seed = uiState.imageConfig.seed,
                                    onSelect = onImageModelSelected,
                                    onServiceModelSelect = onImageServiceModelSelected,
                                    onServiceParamChange = onImageServiceParamChange,
                                    onServiceUploadFieldClick = onServiceUploadFieldClick,
                                    onRemoveMedia = onRemoveMedia,
                                    onSeedChange = onImageSeedChange,
                                )
                            } else {
                                VideoAdvancedContent(
                                    serviceModels = uiState.serviceVideoModels,
                                    selectedServiceModel = uiState.selectedVideoServiceModel,
                                    serviceModelsLoading = uiState.serviceModelsLoading,
                                    serviceParams = uiState.videoServiceParams,
                                    mediaReferences = uiState.videoConfig.mediaReferences,
                                    realistic = uiState.videoConfig.realisticMode,
                                    generateAudio = uiState.videoConfig.generateAudio,
                                    duration = uiState.videoConfig.duration,
                                    seed = uiState.videoConfig.seed,
                                    onServiceModelSelect = onVideoServiceModelSelected,
                                    onServiceParamChange = onVideoServiceParamChange,
                                    onServiceUploadFieldClick = onServiceUploadFieldClick,
                                    onRemoveMedia = onRemoveMedia,
                                    onRealisticToggle = onToggleRealistic,
                                    onAudioToggle = onToggleAudio,
                                    onDurationChange = onVideoDurationChange,
                                    onSeedChange = onVideoSeedChange,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleContent(
    presets: List<ImageStylePreset>,
    onSelect: (ImageStylePreset) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        items(presets) { preset ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, DarkOutlineVariant),
                onClick = { onSelect(preset) },
            ) {
                Column(modifier = Modifier.padding(Dimens.SpaceMD)) {
                    Text(
                        text = preset.displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Neutral100,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = preset.description,
                        fontSize = 11.sp,
                        color = Neutral500,
                        lineHeight = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoModelContent(
    models: List<VideoModel>,
    selected: VideoModel,
    onSelect: (VideoModel) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        items(models) { model ->
            val isSelected = model == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) Primary300.copy(alpha = 0.4f) else DarkOutlineVariant),
                onClick = { onSelect(model) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = model.iconChar, fontSize = 18.sp)
                    Spacer(Modifier.width(Dimens.SpaceSM))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = model.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Primary300 else Neutral100,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Primary300,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageQualityContent(
    quality: ImageQuality,
    resolution: ImageResolution,
    supportedResolutions: List<ImageResolution>,
    supportedQualities: List<ImageQuality>,
    onQualityChange: (ImageQuality) -> Unit,
    onResChange: (ImageResolution) -> Unit,
) {
    Column {
        Text(
            text = "质量",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            supportedQualities.forEach { q ->
                val isSelected = q == quality
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onQualityChange(q) },
                ) {
                    Text(
                        text = q.displayName,
                        modifier = Modifier.padding(vertical = Dimens.SpaceSM, horizontal = Dimens.SpaceMD),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Primary300 else Neutral200,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceLG))

        Text(
            text = "分辨率",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            supportedResolutions.forEach { r ->
                val isSelected = r == resolution
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onResChange(r) },
                ) {
                    Text(
                        text = r.displayName,
                        modifier = Modifier.padding(vertical = Dimens.SpaceSM, horizontal = Dimens.SpaceMD),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Primary300 else Neutral200,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoQualityContent(
    quality: VideoResolution,
    supportedResolutions: List<VideoResolution>,
    onQualityChange: (VideoResolution) -> Unit,
) {
    Column {
        Text(
            text = "分辨率",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            supportedResolutions.forEach { res ->
                val isSelected = res == quality
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onQualityChange(res) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = res.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Primary300 else Neutral200,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Primary300,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RatioContent(
    ratios: List<ImageAspectRatio>,
    selected: ImageAspectRatio,
    onSelect: (ImageAspectRatio) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        ratios.forEach { ratio ->
            val isSelected = ratio == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                onClick = { onSelect(ratio) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ratio.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Primary300 else Neutral200,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Primary300,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRatioContent(
    ratios: List<VideoAspectRatio>,
    selected: VideoAspectRatio,
    onSelect: (VideoAspectRatio) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        ratios.forEach { ratio ->
            val isSelected = ratio == selected
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                onClick = { onSelect(ratio) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ratio.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Primary300 else Neutral200,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Primary300,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> CountContent(
    counts: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        counts.forEach { count ->
            val displayName = when (count) {
                is ImageCount -> count.displayName
                is VideoCount -> count.displayName
                else -> count.toString()
            }
            val isSelected = count == selected
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                onClick = { onSelect(count) },
            ) {
                Text(
                    text = displayName,
                    modifier = Modifier.padding(vertical = Dimens.SpaceMD),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) Primary300 else Neutral200,
                )
            }
        }
    }
}

@Composable
private fun ImageAdvancedContent(
    models: List<ImageModel>,
    selected: ImageModel,
    serviceModels: List<QuickCreationServiceModel>,
    selectedServiceModel: QuickCreationServiceModel?,
    serviceModelsLoading: Boolean,
    serviceParams: Map<String, String>,
    mediaReferences: List<MediaReference>,
    seed: Int?,
    onSelect: (ImageModel) -> Unit,
    onServiceModelSelect: (QuickCreationServiceModel) -> Unit,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        ServiceModelPickerContent(
            serviceModels = serviceModels,
            selectedServiceModel = selectedServiceModel,
            serviceModelsLoading = serviceModelsLoading,
            onServiceModelSelect = onServiceModelSelect,
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        selectedServiceModel
            ?.fields
            .orEmpty()
            .filter { it.isQuickCreationServiceFieldRenderable() }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    params = selectedServiceModel.quickCreationParamsWithFieldAliases(serviceParams),
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        Text(
            text = "本地兼容模型",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            models.forEach { model ->
                val isSelected = model == selected
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onSelect(model) },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = model.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Primary300 else Neutral200,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Primary300,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceXL))

        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

@Composable
private fun ServiceModelPickerContent(
    serviceModels: List<QuickCreationServiceModel>,
    selectedServiceModel: QuickCreationServiceModel?,
    serviceModelsLoading: Boolean,
    onServiceModelSelect: (QuickCreationServiceModel) -> Unit,
) {
    Text(
        text = "服务端模型",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary300,
        modifier = Modifier.padding(bottom = Dimens.SpaceSM),
    )
    when {
        serviceModelsLoading -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpaceSM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Primary300,
                    strokeWidth = 2.dp,
                )
                Text("正在加载模型", fontSize = 12.sp, color = Neutral400)
            }
        }
        serviceModels.isEmpty() -> {
            Text(
                text = "暂未获取到服务端模型，继续使用本地默认配置",
                fontSize = 12.sp,
                color = Neutral500,
                modifier = Modifier.padding(bottom = Dimens.SpaceSM),
            )
        }
        else -> {
            var expanded by remember(serviceModels, selectedServiceModel) { mutableStateOf(false) }
            val selected = selectedServiceModel
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, Primary300),
                    onClick = { expanded = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpaceMD),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selected?.name ?: "请选择服务端模型",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary300,
                            )
                            selected?.quickCreationServiceModelSubtitle()?.takeIf { it.isNotBlank() }?.let { subtitle ->
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = Neutral500,
                                )
                            }
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Neutral300,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(DarkSurface),
                ) {
                    serviceModels.forEach { model ->
                        val isSelected = model.isSameQuickCreationServiceModel(selectedServiceModel)
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = model.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isSelected) Primary300 else Neutral200,
                                    )
                                    model.quickCreationServiceModelSubtitle().takeIf { it.isNotBlank() }?.let { subtitle ->
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.sp,
                                            color = Neutral500,
                                        )
                                    }
                                }
                            },
                            trailingIcon = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary300,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                onServiceModelSelect(model)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun QuickCreationServiceModel.isSameQuickCreationServiceModel(
    other: QuickCreationServiceModel?,
): Boolean = other != null && bindingId == other.bindingId && skuId == other.skuId

private fun QuickCreationServiceModel.quickCreationServiceModelSubtitle(): String =
    listOfNotNull(
        groupName?.takeIf { it.isNotBlank() },
        "${fields.size} 个参数",
    ).joinToString(" · ")

@Composable
private fun ServiceFieldOptionsContent(
    fields: List<QuickCreationServiceField>,
    params: Map<String, String>,
    mediaReferences: List<MediaReference>,
    onParamChange: (String, String) -> Unit,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
        Text(
            text = "服务端参数",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
        )
        fields.forEach { field ->
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                Text(
                    text = field.quickCreationFieldTitle(),
                    fontSize = 12.sp,
                    color = Neutral300,
                )
                field.inputExtra?.paramDescription?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Neutral500,
                    )
                }
                if (field.options.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                    ) {
                        field.options.forEach { option ->
                            val selectedValue = params[field.paramKey] ?: field.defaultValue
                            val isSelected = selectedValue == option.value
                            Surface(
                                shape = RoundedCornerShape(Dimens.RadiusSM),
                                color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                                onClick = { onParamChange(field.paramKey, option.value) },
                            ) {
                                Text(
                                    text = option.label,
                                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) Primary300 else Neutral200,
                                )
                            }
                        }
                    }
                } else if (field.supportsQuickCreationTextEntry()) {
                    var text by remember(field.paramKey, params[field.paramKey]) {
                        mutableStateOf(params[field.paramKey] ?: field.defaultValue.orEmpty())
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { value ->
                            val constrained = field.constrainQuickCreationTextInput(value)
                            text = constrained
                            onParamChange(field.paramKey, constrained)
                        },
                        singleLine = true,
                        placeholder = { Text(field.quickCreationInputPlaceholder(), color = Neutral500, fontSize = 12.sp) },
                        shape = RoundedCornerShape(Dimens.RadiusSM),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Primary300,
                            focusedBorderColor = Primary300,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    field.quickCreationTextLimitCounter(text)?.let { counter ->
                        Text(
                            text = counter,
                            fontSize = 10.sp,
                            color = Neutral500,
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                } else if (field.isQuickCreationUploadField()) {
                    ServiceUploadFieldPicker(
                        paramKey = field.paramKey,
                        mediaType = field.quickCreationUploadMediaType(),
                        hint = field.quickCreationUploadHintParts().joinToString(" · "),
                        mediaReferences = mediaReferences,
                        onUploadFieldClick = onUploadFieldClick,
                        onRemoveMedia = onRemoveMedia,
                    )
                }
                field.quickCreationActiveInputChildren(params).forEach { child ->
                    ServiceChildFieldInput(
                        child = child,
                        params = params,
                        mediaReferences = mediaReferences,
                        onParamChange = onParamChange,
                        onUploadFieldClick = onUploadFieldClick,
                        onRemoveMedia = onRemoveMedia,
                    )
                }
            }
        }
    }
}

@Composable
private fun ServiceUploadFieldPicker(
    paramKey: String,
    mediaType: QuickCreateMediaType?,
    hint: String,
    mediaReferences: List<MediaReference>,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    val fieldRefs = mediaReferences.quickCreationFieldMediaReferences(paramKey)
    val doneCount = fieldRefs.count { it.uploadStatus == UploadStatus.DONE }
    val uploadingCount = fieldRefs.count {
        it.uploadStatus == UploadStatus.UPLOADING || it.uploadStatus == UploadStatus.PROCESSING
    }
    val label = when (mediaType) {
        QuickCreateMediaType.IMAGE -> "选择图片"
        QuickCreateMediaType.VIDEO -> "选择视频"
        QuickCreateMediaType.AUDIO -> "选择音频"
        null -> "选择素材"
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(
                        hint.takeIf { it.isNotBlank() },
                        doneCount.takeIf { it > 0 }?.let { "已上传 $it" },
                        uploadingCount.takeIf { it > 0 }?.let { "上传中 $it" },
                    ).joinToString(" · ").ifBlank { "为该字段选择专属素材" },
                    fontSize = 11.sp,
                    color = Neutral500,
                )
            }
            Surface(
                enabled = mediaType != null,
                shape = RoundedCornerShape(Dimens.RadiusSM),
                color = if (doneCount > 0) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (doneCount > 0) Primary300.copy(alpha = 0.4f) else DarkOutlineVariant,
                ),
                onClick = {
                    val resolvedType = mediaType ?: return@Surface
                    onUploadFieldClick(resolvedType, paramKey)
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = when (mediaType) {
                            QuickCreateMediaType.IMAGE -> Icons.Default.Image
                            QuickCreateMediaType.VIDEO -> Icons.Default.Videocam
                            QuickCreateMediaType.AUDIO -> Icons.Default.MusicNote
                            null -> Icons.Default.Upload
                        },
                        contentDescription = null,
                        tint = if (doneCount > 0) Primary300 else Neutral400,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (doneCount > 0) Primary300 else Neutral300,
                        fontWeight = if (doneCount > 0) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
        fieldRefs.forEach { ref ->
            MediaChipCard(
                reference = ref,
                onRemove = { onRemoveMedia(ref.id) },
            )
        }
    }
}

@Composable
private fun ServiceChildFieldInput(
    child: QuickCreationServiceFieldInputChild,
    params: Map<String, String>,
    mediaReferences: List<MediaReference>,
    onParamChange: (String, String) -> Unit,
    onUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = Dimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        Text(
            text = child.quickCreationFieldTitle(),
            fontSize = 12.sp,
            color = Neutral300,
        )
        child.paramDescription?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                fontSize = 11.sp,
                color = Neutral500,
            )
        }
        if (child.options.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                child.options.forEach { option ->
                    val selectedValue = params[child.paramKey] ?: child.defaultValue
                    val isSelected = selectedValue == option.value
                    Surface(
                        shape = RoundedCornerShape(Dimens.RadiusSM),
                        color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                        onClick = { onParamChange(child.paramKey, option.value) },
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Primary300 else Neutral200,
                        )
                    }
                }
            }
        } else if (child.supportsQuickCreationTextEntry()) {
            var text by remember(child.paramKey, params[child.paramKey]) {
                mutableStateOf(params[child.paramKey] ?: child.defaultValue.orEmpty())
            }
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    val constrained = child.constrainQuickCreationTextInput(value)
                    text = constrained
                    onParamChange(child.paramKey, constrained)
                },
                singleLine = true,
                placeholder = { Text(child.quickCreationInputPlaceholder(), color = Neutral500, fontSize = 12.sp) },
                shape = RoundedCornerShape(Dimens.RadiusSM),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Primary300,
                    focusedBorderColor = Primary300,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            child.quickCreationTextLimitCounter(text)?.let { counter ->
                Text(
                    text = counter,
                    fontSize = 10.sp,
                    color = Neutral500,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        } else if (child.isQuickCreationUploadField()) {
            ServiceUploadFieldPicker(
                paramKey = child.paramKey,
                mediaType = child.quickCreationUploadMediaType(),
                hint = "",
                mediaReferences = mediaReferences,
                onUploadFieldClick = onUploadFieldClick,
                onRemoveMedia = onRemoveMedia,
            )
        }
    }
}

@Composable
private fun SeedInput(seed: Int?, onSeedChange: (Int?) -> Unit) {
    var text by remember(seed) { mutableStateOf(seed?.toString() ?: "") }

    Text(
        text = "Seed（留空为随机）",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary300,
        modifier = Modifier.padding(bottom = Dimens.SpaceSM),
    )
    OutlinedTextField(
        value = text,
        onValueChange = { newVal ->
            text = newVal
            val num = newVal.toIntOrNull()
            onSeedChange(num)
        },
        placeholder = { Text("随机", color = Neutral500, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary300,
            focusedBorderColor = Primary300,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun VideoAdvancedContent(
    serviceModels: List<QuickCreationServiceModel>,
    selectedServiceModel: QuickCreationServiceModel?,
    serviceModelsLoading: Boolean,
    serviceParams: Map<String, String>,
    mediaReferences: List<MediaReference>,
    realistic: Boolean,
    generateAudio: Boolean,
    duration: VideoDuration,
    seed: Int?,
    onServiceModelSelect: (QuickCreationServiceModel) -> Unit,
    onServiceParamChange: (String, String) -> Unit,
    onServiceUploadFieldClick: (QuickCreateMediaType, String) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onRealisticToggle: () -> Unit,
    onAudioToggle: () -> Unit,
    onDurationChange: (VideoDuration) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        ServiceModelPickerContent(
            serviceModels = serviceModels,
            selectedServiceModel = selectedServiceModel,
            serviceModelsLoading = serviceModelsLoading,
            onServiceModelSelect = onServiceModelSelect,
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        selectedServiceModel
            ?.fields
            .orEmpty()
            .filter { it.isQuickCreationServiceFieldRenderable() }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    params = selectedServiceModel.quickCreationParamsWithFieldAliases(serviceParams),
                    mediaReferences = mediaReferences,
                    onParamChange = onServiceParamChange,
                    onUploadFieldClick = onServiceUploadFieldClick,
                    onRemoveMedia = onRemoveMedia,
                )
                Spacer(Modifier.height(Dimens.SpaceXL))
            }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            ToggleChip(
                label = "真人模式",
                enabled = realistic,
                onClick = onRealisticToggle,
                modifier = Modifier.weight(1f),
            )
            ToggleChip(
                label = "生成音频",
                enabled = generateAudio,
                onClick = onAudioToggle,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Dimens.SpaceLG))

        Text(
            text = "时长",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Primary300,
            modifier = Modifier.padding(bottom = Dimens.SpaceSM),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
            VideoDuration.entries.forEach { dur ->
                val isSelected = dur == duration
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.RadiusSM),
                    color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                    onClick = { onDurationChange(dur) },
                ) {
                    Text(
                        text = dur.displayName,
                        modifier = Modifier.padding(vertical = Dimens.SpaceSM, horizontal = Dimens.SpaceMD),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        color = if (isSelected) Primary300 else Neutral200,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.SpaceLG))

        SeedInput(seed = seed, onSeedChange = onSeedChange)
    }
}

@Composable
private fun ToggleChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = if (enabled) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
        border = BorderStroke(1.dp, if (enabled) Primary300 else DarkOutlineVariant),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                if (enabled) Icons.Default.Check else Icons.Default.Add,
                contentDescription = null,
                tint = if (enabled) Primary300 else Neutral500,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (enabled) FontWeight.Medium else FontWeight.Normal,
                color = if (enabled) Primary300 else Neutral200,
            )
        }
    }
}
