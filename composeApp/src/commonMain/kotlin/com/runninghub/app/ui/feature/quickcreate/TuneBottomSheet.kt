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
fun TuneBottomSheet(
    visible: Boolean,
    isImage: Boolean,
    uiState: QuickCreateUiState,
    onDismiss: () -> Unit,
    onImageModelSelected: (ImageModel) -> Unit,
    onImageServiceModelSelected: (QuickCreationServiceModel) -> Unit,
    onImageServiceParamChange: (String, String) -> Unit,
    onVideoModelSelected: (VideoModel) -> Unit,
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
                                    seed = uiState.imageConfig.seed,
                                    onSelect = onImageModelSelected,
                                    onServiceModelSelect = onImageServiceModelSelected,
                                    onServiceParamChange = onImageServiceParamChange,
                                    onSeedChange = onImageSeedChange,
                                )
                            } else {
                                VideoAdvancedContent(
                                    realistic = uiState.videoConfig.realisticMode,
                                    generateAudio = uiState.videoConfig.generateAudio,
                                    duration = uiState.videoConfig.duration,
                                    seed = uiState.videoConfig.seed,
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
    seed: Int?,
    onSelect: (ImageModel) -> Unit,
    onServiceModelSelect: (QuickCreationServiceModel) -> Unit,
    onServiceParamChange: (String, String) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
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
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
                    serviceModels.forEach { model ->
                        val isSelected = selectedServiceModel != null &&
                            model.bindingId == selectedServiceModel.bindingId &&
                            model.skuId == selectedServiceModel.skuId
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.RadiusSM),
                            color = if (isSelected) Primary300.copy(alpha = 0.1f) else DarkSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) Primary300 else DarkOutlineVariant),
                            onClick = { onServiceModelSelect(model) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.SpaceMD),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = if (isSelected) Primary300 else Neutral200,
                                    )
                                    if (!model.groupName.isNullOrBlank() || model.fields.isNotEmpty()) {
                                        Text(
                                            text = listOfNotNull(
                                                model.groupName,
                                                "${model.fields.size} 个参数",
                                            ).joinToString(" · "),
                                            fontSize = 11.sp,
                                            color = Neutral500,
                                        )
                                    }
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
        }

        Spacer(Modifier.height(Dimens.SpaceXL))

        selectedServiceModel
            ?.fields
            .orEmpty()
            .filter { it.options.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.let { fields ->
                ServiceFieldOptionsContent(
                    fields = fields,
                    params = serviceParams,
                    onParamChange = onServiceParamChange,
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
private fun ServiceFieldOptionsContent(
    fields: List<QuickCreationServiceField>,
    params: Map<String, String>,
    onParamChange: (String, String) -> Unit,
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
                    text = field.fieldKey,
                    fontSize = 12.sp,
                    color = Neutral300,
                )
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
            }
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
    realistic: Boolean,
    generateAudio: Boolean,
    duration: VideoDuration,
    seed: Int?,
    onRealisticToggle: () -> Unit,
    onAudioToggle: () -> Unit,
    onDurationChange: (VideoDuration) -> Unit,
    onSeedChange: (Int?) -> Unit,
) {
    Column {
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
