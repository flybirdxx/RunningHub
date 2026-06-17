@file:OptIn(ExperimentalMaterial3Api::class)

package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.platform.PermissionController
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.adaptive.previewQuickCreateUiState
import com.runninghub.app.ui.component.PermissionBottomSheet
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.theme.*
import com.runninghub.app.util.formatOneDecimal
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.jetbrains.compose.ui.tooling.preview.Preview

class QuickCreateVoyagerScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel: QuickCreateScreenModel = koinScreenModel()
        QuickCreateScreen(screenModel)
    }
}

@Composable
private fun QuickCreateScreen(screenModel: QuickCreateScreenModel) {
    val uiState by screenModel.uiState.collectAsState()
    val currentScreenModel by rememberUpdatedState(screenModel)
    val windowInfo = LocalRhWindowInfo.current

    val dataStore: PermissionDataStore = koinInject()
    val controller: PermissionController = rememberPermissionController(dataStore)

    var pendingPermission by remember { mutableStateOf<Permission?>(null) }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            delay(3000)
            currentScreenModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = windowInfo.detailContentMaxWidth)
            ) {
                QuickCreateTopBar(
                    selectedMode = uiState.currentMode,
                    onModeSelected = screenModel::switchMode,
                    onBack = null,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentMode) {
                        QuickCreateMode.CREATION -> CreationScrollableArea(
                            uiState = uiState,
                            onClearResults = screenModel::clearResults,
                        )
                        QuickCreateMode.INSPIRATION -> InspirationArea(uiState)
                    }
                }

                if (uiState.showCreationInput) {
                    BottomPromptPanel(
                        uiState = uiState,
                        onTabSwitch = screenModel::switchTab,
                        onPromptChange = if (uiState.currentTab == QuickCreateTab.IMAGE) screenModel::updateImagePrompt else screenModel::updateVideoPrompt,
                        onLaunchImagePicker = {
                            controller.pickMedia(
                                mediaPermission = Permission.MediaImages,
                                mediaType = MediaType.IMAGE,
                                onSuccess = { uriString -> currentScreenModel.pickImageReference(uriString) },
                                onPermissionDenied = { pendingPermission = Permission.MediaImages },
                            )
                        },
                        onLaunchVideoPicker = {
                            controller.pickMedia(
                                mediaPermission = Permission.MediaVideo,
                                mediaType = MediaType.VIDEO,
                                onSuccess = { uriString -> currentScreenModel.pickVideoReference(uriString) },
                                onPermissionDenied = { pendingPermission = Permission.MediaVideo },
                            )
                        },
                        onLaunchAudioPicker = {
                            controller.pickMedia(
                                mediaPermission = Permission.MediaAudio,
                                mediaType = MediaType.AUDIO,
                                onSuccess = { uriString -> currentScreenModel.pickAudioReference(uriString) },
                                onPermissionDenied = { pendingPermission = Permission.MediaAudio },
                            )
                        },
                        onRemoveMedia = screenModel::removeMediaReference,
                        onToggleTune = { screenModel.setTuneSheetVisible(!uiState.tuneSheetVisible) },
                        onGenerate = screenModel::generate,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
            ) {
                Surface(
                    color = ErrorDark.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    modifier = Modifier.padding(horizontal = Dimens.SpaceLG),
                ) {
                    Text(
                        uiState.error ?: "",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
                        fontSize = 13.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.tuneSheetVisible && uiState.showCreationInput,
            enter = slideInVertically { it } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically { it } + fadeOut(animationSpec = tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = Color.Black.copy(alpha = 0.5f),
                    onClick = { screenModel.setTuneSheetVisible(false) },
                ) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
                }
                TuneBottomSheet(
                    visible = true,
                    isImage = uiState.currentTab == QuickCreateTab.IMAGE,
                    uiState = uiState,
                    onDismiss = { screenModel.setTuneSheetVisible(false) },
                    onImageModelSelected = {
                        screenModel.updateImageModel(it)
                    },
                    onVideoModelSelected = {
                        screenModel.updateVideoModel(it)
                    },
                    onImageRatioChange = screenModel::updateImageAspectRatio,
                    onImageResChange = screenModel::updateImageResolution,
                    onImageQualityChange = screenModel::updateImageQuality,
                    onVideoRatioChange = screenModel::updateVideoAspectRatio,
                    onVideoResChange = screenModel::updateVideoResolution,
                    onVideoDurationChange = screenModel::updateVideoDuration,
                    onToggleRealistic = screenModel::toggleRealisticMode,
                    onToggleAudio = screenModel::toggleGenerateAudio,
                    onImageCountChange = { count -> screenModel.updateImageCount(count.count) },
                    onVideoCountChange = { count -> screenModel.updateVideoCount(count.count) },
                    onImageSeedChange = screenModel::updateImageSeed,
                    onVideoSeedChange = screenModel::updateVideoSeed,
                    onImageStyleChange = { style ->
                        val styleTag = when (style) {
                            ImageStylePreset.PHOTOREAL -> "写实摄影风格, "
                            ImageStylePreset.ARTISTIC -> "艺术插画风格, "
                            ImageStylePreset.RENDER_3D -> "3D渲染风格, "
                            ImageStylePreset.WATERCOLOR -> "水彩画风格, "
                        }
                        val currentPrompt = screenModel.uiState.value.imageConfig.prompt
                        // Remove existing style prefix if any
                        val cleanPrompt = currentPrompt
                            .removePrefix("写实摄影风格, ")
                            .removePrefix("艺术插画风格, ")
                            .removePrefix("3D渲染风格, ")
                            .removePrefix("水彩画风格, ")
                        screenModel.updateImagePrompt(styleTag + cleanPrompt)
                    },
                )
            }
        }
    }

    if (pendingPermission != null) {
        PermissionBottomSheet(
            permission = pendingPermission!!,
            onDismiss = { pendingPermission = null },
            onAuthorize = {
                val perm = pendingPermission!!
                pendingPermission = null
                controller.checkAndRequest(
                    permission = perm,
                    onGranted = {},
                    onDenied = {},
                    onPermanentlyDenied = { controller.openAppSettings() },
                )
            },
        )
    }
}

@Composable
private fun QuickCreateTopBar(
    selectedMode: QuickCreateMode,
    onModeSelected: (QuickCreateMode) -> Unit,
    onBack: (() -> Unit)?,
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            ModeSwitch(
                selectedMode = selectedMode,
                onModeSelected = onModeSelected,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "客服",
                    tint = Color.White.copy(alpha = 0.86f),
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "静音",
                    tint = Color.White.copy(alpha = 0.86f),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
    )
}

@Composable
private fun ModeSwitch(
    selectedMode: QuickCreateMode,
    onModeSelected: (QuickCreateMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickCreateMode.entries.forEachIndexed { index, mode ->
            TextButton(
                onClick = { onModeSelected(mode) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    mode.displayName,
                    color = if (mode == selectedMode) Primary300 else Color.White.copy(alpha = 0.56f),
                    fontWeight = if (mode == selectedMode) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 18.sp,
                )
            }
            if (index == 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Primary300.copy(alpha = 0.8f)),
                )
            }
        }
    }
}

@Composable
private fun CreationScrollableArea(
    uiState: QuickCreateUiState,
    onClearResults: () -> Unit,
) {
    when {
        uiState.results.isNotEmpty() -> ResultArea(
            results = uiState.results,
            onClear = onClearResults,
        )
        uiState.taskStatus != QuickCreateTaskUiStatus.IDLE -> TaskStatusArea(
            status = uiState.taskStatus,
            statusText = uiState.statusText,
        )
        else -> EmptyArea()
    }
}

@Composable
private fun EmptyArea() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = Primary300.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(Dimens.SpaceMD))
            Text(
                "输入提示词开始创作",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun InspirationArea(uiState: QuickCreateUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                uiState.inspirationTags.forEachIndexed { index, tag ->
                    Surface(
                        color = if (index == 0) Primary300.copy(alpha = 0.16f) else DarkSurfaceVariant,
                        shape = RoundedCornerShape(Dimens.RadiusFull),
                        border = BorderStroke(
                            1.dp,
                            if (index == 0) Primary300.copy(alpha = 0.42f) else DarkOutlineVariant,
                        ),
                    ) {
                        Text(
                            tag.name,
                            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 7.dp),
                            color = if (index == 0) Primary300 else Neutral400,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        if (uiState.inspirationLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Primary300)
                }
            }
        }

        items(uiState.inspirationTemplates, key = { it.templateId }) { template ->
            InspirationTemplateCard(
                title = template.title,
                category = template.categoryId ?: "IMAGE",
                coverUrl = template.coverUrl,
                videoUrl = template.videoUrl,
                tagHot = template.tagHot,
                tagNew = template.tagNew,
            )
        }

        if (!uiState.inspirationLoading && uiState.inspirationTemplates.isEmpty()) {
            item { EmptyArea() }
        }
    }
}

@Composable
private fun InspirationTemplateCard(
    title: String,
    category: String,
    coverUrl: String?,
    videoUrl: String?,
    tagHot: Boolean,
    tagNew: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.RadiusLG),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkOutlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .padding(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(Primary300.copy(alpha = 0.26f), Secondary500.copy(alpha = 0.18f)),
                        ),
                        RoundedCornerShape(Dimens.RadiusMD),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !videoUrl.isNullOrBlank() -> VideoThumbnail(
                        url = videoUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                    !coverUrl.isNullOrBlank() -> SmartAsyncImage(
                        imageUrl = coverUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    else -> Icon(
                        if (category == "VIDEO") Icons.Default.Videocam else Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        category,
                        color = Neutral500,
                        fontSize = 12.sp,
                    )
                    if (tagHot) {
                        Text("HOT", color = ErrorDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (tagNew) {
                        Text("NEW", color = Primary300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Neutral500,
            )
        }
    }
}

@Composable
private fun TaskStatusArea(status: QuickCreateTaskUiStatus, statusText: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Primary300,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(Dimens.SpaceXL))
            Text(
                statusText ?: "处理中...",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ResultArea(results: List<QuickCreateResultUi>, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceMD),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "生成结果",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
            )
            TextButton(onClick = onClear, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Primary300,
                )
                Spacer(Modifier.width(4.dp))
                Text("重新创作", color = Primary300, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(Dimens.SpaceSM))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
            items(results) { result ->
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusLG),
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (result.type == "video" || result.url.contains(".mp4") || result.url.contains(".webm")) {
                        VideoThumbnail(
                            url = result.url,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )
                    } else {
                        SmartAsyncImage(
                            imageUrl = result.url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            }
        }
    }
}

// ── Bottom Prompt Panel ──────────────────────────────────────────────────────

@Composable
private fun BottomPromptPanel(
    uiState: QuickCreateUiState,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onPromptChange: (String) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onLaunchVideoPicker: () -> Unit,
    onLaunchAudioPicker: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onToggleTune: () -> Unit,
    onGenerate: () -> Unit,
) {
    val windowInfo = LocalRhWindowInfo.current
    val isImage = uiState.currentTab == QuickCreateTab.IMAGE
    val imageConfig = uiState.imageConfig
    val videoConfig = uiState.videoConfig
    val configPrompt = if (isImage) imageConfig.prompt else videoConfig.prompt
    val configCharCount = if (isImage) imageConfig.promptCharCount else videoConfig.promptCharCount
    val configNearLimit = if (isImage) imageConfig.promptNearLimit else videoConfig.promptNearLimit
    val configOverLimit = if (isImage) imageConfig.promptOverLimit else videoConfig.promptOverLimit
    val configMediaRefs = if (isImage) imageConfig.mediaReferences else videoConfig.mediaReferences
    val isTaskActive = uiState.taskStatus in listOf(
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.97f),
        shape = RoundedCornerShape(topStart = Dimens.RadiusXL, topEnd = Dimens.RadiusXL),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = windowInfo.bottomSheetMaxWidth)
                    .heightIn(max = windowInfo.windowHeight * windowInfo.bottomPanelMaxHeightFraction)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.SpaceMD)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = Dimens.SpaceMD),
            ) {
                TabPillRow(
                    selectedTab = uiState.currentTab,
                    onTabSelected = onTabSwitch,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                MediaToolbarRow(
                    mediaReferences = configMediaRefs,
                    onLaunchImagePicker = onLaunchImagePicker,
                    onLaunchVideoPicker = onLaunchVideoPicker,
                    onLaunchAudioPicker = onLaunchAudioPicker,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                if (configMediaRefs.isNotEmpty()) {
                    configMediaRefs.forEach { ref: MediaReference ->
                        MediaChipCard(
                            reference = ref,
                            onRemove = { onRemoveMedia(ref.id) },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceSM))
                AdaptivePromptTextField(
                    prompt = configPrompt,
                    onPromptChange = onPromptChange,
                    placeholder = if (isImage) "描述你的图片..." else "描述你的视频...",
                    charCount = configCharCount,
                    nearLimit = configNearLimit,
                    overLimit = configOverLimit,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Dimens.SpaceSM))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onToggleTune,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (uiState.tuneSheetVisible) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                                RoundedCornerShape(Dimens.RadiusMD),
                            ),
                        enabled = !isTaskActive,
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "创作调优",
                            modifier = Modifier.size(18.dp),
                            tint = if (uiState.tuneSheetVisible) Primary300 else Neutral400,
                        )
                    }

                    SendButton(
                        enabled = !isTaskActive && configPrompt.isNotBlank() && !configOverLimit,
                        isLoading = isTaskActive,
                        cost = uiState.estimatedCost,
                        onClick = onGenerate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPillRow(
    selectedTab: QuickCreateTab,
    onTabSelected: (QuickCreateTab) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)) {
        QuickCreateTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Surface(
                onClick = { onTabSelected(tab) },
                color = if (selected) Primary300.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(Dimens.RadiusFull),
                border = if (selected) BorderStroke(1.dp, Primary300.copy(alpha = 0.45f)) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        if (tab == QuickCreateTab.IMAGE) Icons.Default.Image else Icons.Default.Videocam,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selected) Primary300 else Neutral500,
                    )
                    Text(
                        tab.displayName,
                        fontSize = 12.sp,
                        color = if (selected) Color.White else Neutral500,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaToolbarRow(
    mediaReferences: List<MediaReference>,
    onLaunchImagePicker: () -> Unit,
    onLaunchVideoPicker: () -> Unit,
    onLaunchAudioPicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        val imageCount = mediaReferences.count { it.type == QuickCreateMediaType.IMAGE }
        val videoCount = mediaReferences.count { it.type == QuickCreateMediaType.VIDEO }
        val audioCount = mediaReferences.count { it.type == QuickCreateMediaType.AUDIO }

        ToolbarChip(
            label = if (imageCount > 0) "图片参考 · $imageCount" else "图片参考",
            icon = Icons.Default.Image,
            hasItems = imageCount > 0,
            onClick = onLaunchImagePicker,
        )
        ToolbarChip(
            label = if (videoCount > 0) "视频参考 · $videoCount" else "视频参考",
            icon = Icons.Default.Videocam,
            hasItems = videoCount > 0,
            onClick = onLaunchVideoPicker,
        )
        ToolbarChip(
            label = if (audioCount > 0) "音频参考 · $audioCount" else "音频参考",
            icon = Icons.Default.MusicNote,
            hasItems = audioCount > 0,
            onClick = onLaunchAudioPicker,
        )
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hasItems: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (hasItems) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        border = BorderStroke(
            1.dp,
            if (hasItems) Primary300.copy(alpha = 0.4f) else DarkOutlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(14.dp),
                tint = if (hasItems) Primary300 else Neutral500,
            )
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasItems) Primary300 else Neutral400,
            )
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    isLoading: Boolean,
    cost: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                brush = if (enabled) Brush.linearGradient(listOf(Primary300, Secondary500)) else Brush.linearGradient(listOf(DarkSurfaceVariant, DarkSurfaceVariant)),
                shape = RoundedCornerShape(Dimens.RadiusMD),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = if (enabled && !isLoading) onClick else {{}},
            shape = RoundedCornerShape(Dimens.RadiusMD),
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "生成",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(6.dp))
                if (cost > 0) {
                    Text(
                        "¥${formatOneDecimal(cost)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Primary300 else Neutral500,
                    )
                } else {
                    Text(
                        "生成",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color.White else Neutral500,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCreatePreviewContent(
    uiState: QuickCreateUiState = previewQuickCreateUiState(),
) {
    val windowInfo = LocalRhWindowInfo.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = windowInfo.detailContentMaxWidth),
            ) {
                QuickCreateTopBar(
                    selectedMode = uiState.currentMode,
                    onModeSelected = {},
                    onBack = null,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentMode) {
                        QuickCreateMode.CREATION -> CreationScrollableArea(
                            uiState = uiState,
                            onClearResults = {},
                        )
                        QuickCreateMode.INSPIRATION -> InspirationArea(uiState)
                    }
                }

                if (uiState.showCreationInput) {
                    BottomPromptPanel(
                        uiState = uiState,
                        onTabSwitch = {},
                        onPromptChange = {},
                        onLaunchImagePicker = {},
                        onLaunchVideoPicker = {},
                        onLaunchAudioPicker = {},
                        onRemoveMedia = {},
                        onToggleTune = {},
                        onGenerate = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCreateAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        QuickCreatePreviewContent()
    }
}

@Preview
@Composable
private fun QuickCreatePhone320Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun QuickCreatePhone360Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun QuickCreatePhone430Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun QuickCreateMedium600Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun QuickCreateExpanded840Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun QuickCreateLandscapePreview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun QuickCreateFontScale13Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun QuickCreateFontScale15Preview() {
    QuickCreateAdaptivePreview(RhPreviewSpec.FontScale15)
}

@Composable
private fun QuickCreateBottomPanelAdaptivePreview(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
) {
    RunningHubPreviewSurface(
        windowWidth = widthDp.dp,
        windowHeight = heightDp.dp,
        fontScale = fontScale,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.BottomCenter,
        ) {
            BottomPromptPanel(
                uiState = previewQuickCreateUiState(),
                onTabSwitch = {},
                onPromptChange = {},
                onLaunchImagePicker = {},
                onLaunchVideoPicker = {},
                onLaunchAudioPicker = {},
                onRemoveMedia = {},
                onToggleTune = {},
                onGenerate = {},
            )
        }
    }
}

@Preview
@Composable
private fun QuickCreateBottomPanelPreview() {
    QuickCreateBottomPanelAdaptivePreview(widthDp = 360, heightDp = 800)
}

@Preview
@Composable
private fun QuickCreateBottomPanelLandscapePreview() {
    QuickCreateBottomPanelAdaptivePreview(widthDp = 800, heightDp = 360)
}

@Preview
@Composable
private fun QuickCreateBottomPanelFontScale13Preview() {
    QuickCreateBottomPanelAdaptivePreview(widthDp = 360, heightDp = 800, fontScale = 1.3f)
}

@Preview
@Composable
private fun QuickCreateBottomPanelFontScale15Preview() {
    QuickCreateBottomPanelAdaptivePreview(widthDp = 360, heightDp = 800, fontScale = 1.5f)
}
