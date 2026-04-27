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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.runninghub.app.ui.component.PermissionBottomSheet
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.theme.*
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

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

    val activityContext = LocalContext.current
    val dataStore: PermissionDataStore = koinInject()
        val controller: PermissionController = rememberPermissionController(dataStore, activityContext)

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
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = "快捷创作", onBack = null)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.results.isNotEmpty() -> ResultArea(
                        results = uiState.results,
                        onClear = screenModel::clearResults,
                    )
                    uiState.taskStatus != QuickCreateTaskUiStatus.IDLE -> TaskStatusArea(
                        status = uiState.taskStatus,
                        statusText = uiState.statusText,
                    )
                    else -> EmptyArea()
                }
            }

            BottomPromptPanel(
                uiState = uiState,
                onTabSwitch = screenModel::switchTab,
                onPromptChange = if (uiState.currentTab == QuickCreateTab.IMAGE) screenModel::updateImagePrompt else screenModel::updateVideoPrompt,
                onLaunchImagePicker = {
                    controller.pickMedia(
                        mediaPermission = Permission.MediaImages,
                        mediaType = MediaType.IMAGE,
                        onSuccess = { uriString ->
                            val uri = android.net.Uri.parse(uriString)
                            try {
                                activityContext.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } catch (_: SecurityException) { }
                            currentScreenModel.pickImageReference(uriString)
                        },
                        onPermissionDenied = { pendingPermission = Permission.MediaImages },
                    )
                },
                onLaunchVideoPicker = {
                    controller.pickMedia(
                        mediaPermission = Permission.MediaVideo,
                        mediaType = MediaType.VIDEO,
                        onSuccess = { uriString ->
                            val uri = android.net.Uri.parse(uriString)
                            try {
                                activityContext.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } catch (_: SecurityException) { }
                            currentScreenModel.pickVideoReference(uriString)
                        },
                        onPermissionDenied = { pendingPermission = Permission.MediaVideo },
                    )
                },
                onLaunchAudioPicker = {
                    controller.pickMedia(
                        mediaPermission = Permission.MediaAudio,
                        mediaType = MediaType.AUDIO,
                        onSuccess = { uriString ->
                            val uri = android.net.Uri.parse(uriString)
                            try {
                                activityContext.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } catch (_: SecurityException) { }
                            currentScreenModel.pickAudioReference(uriString)
                        },
                        onPermissionDenied = { pendingPermission = Permission.MediaAudio },
                    )
                },
                onRemoveMedia = screenModel::removeMediaReference,
                onToggleTune = { screenModel.setTuneSheetVisible(!uiState.tuneSheetVisible) },
                onGenerate = screenModel::generate,
            )
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
            visible = uiState.tuneSheetVisible,
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
                    onImageCountChange = {},
                    onVideoCountChange = {},
                    onImageStyleChange = {},
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(title: String, onBack: (() -> Unit)?) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
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
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
    )
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
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.SpaceMD)
                .navigationBarsPadding()
                .padding(bottom = Dimens.SpaceMD),
        ) {
            // 1. Tab Pills
            TabPillRow(
                selectedTab = uiState.currentTab,
                onTabSelected = onTabSwitch,
            )

            Spacer(Modifier.height(Dimens.SpaceSM))

            // 2. Horizontal Toolbar
            MediaToolbarRow(
                mediaReferences = configMediaRefs,
                onLaunchImagePicker = onLaunchImagePicker,
                onLaunchVideoPicker = onLaunchVideoPicker,
                onLaunchAudioPicker = onLaunchAudioPicker,
            )

            // 3. Media Chip Cards (when references exist)
            if (configMediaRefs.isNotEmpty()) {
                Spacer(Modifier.height(Dimens.SpaceSM))
                configMediaRefs.forEach { ref: MediaReference ->
                    MediaChipCard(
                        reference = ref,
                        onRemove = { onRemoveMedia(ref.id) },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            // 4. Adaptive Input
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

            // 5. Bottom Row: Tune + Send
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
                        "¥${"%.1f".format(cost)}",
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
