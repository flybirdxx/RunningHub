package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.platform.PermissionController
import com.runninghub.app.platform.SystemBackHandler
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.adaptive.previewQuickCreateUiState
import com.runninghub.app.ui.component.PermissionBottomSheet
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.QuickCreateEditorPanel
import com.runninghub.app.ui.feature.quickcreate.presentation.editor.params.QuickCreateParamsSheet
import com.runninghub.app.ui.feature.quickcreate.presentation.history.QuickCreateHistoryDetailDialog
import com.runninghub.app.ui.feature.quickcreate.presentation.inspiration.QuickCreateInspirationArea
import com.runninghub.app.ui.feature.quickcreate.presentation.modelselector.QuickCreateModelSheet
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateCreateProjectAction
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateProjectDetailDialog
import com.runninghub.app.ui.theme.*
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.fields.quickCreationServiceFieldUiItems
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateMode
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.state.navigationLabel
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_top_bar_back_content_description
import runninghub.composeapp.generated.resources.quick_create_top_bar_menu_content_description
import kotlin.math.roundToInt

/**
 * 快捷创作页面在 Voyager 导航中的入口。
 *
 * 该类型只负责从 Koin 获取 [QuickCreateScreenModel] 并把它交给页面内容函数；
 * 具体业务编排由 ScreenModel/Coordinator 处理，避免导航对象直接持有创作、上传或轮询状态。
 */
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

    // 页面只依赖权限状态的领域边界，底层是否使用 DataStore 由 DI 组合根决定。
    val permissionStateStore: PermissionStateStore = koinInject()
    val controller: PermissionController = rememberPermissionController(permissionStateStore)

    var pendingPermission by remember { mutableStateOf<Permission?>(null) }
    val errorText = uiState.error?.asQuickCreateText()
    val activeBusinessSheetVisible = uiState.activeSheet != null && uiState.showCreationInput
    var lastActiveSheet by remember { mutableStateOf<QuickCreateSheet?>(null) }
    val renderedSheet = uiState.activeSheet ?: lastActiveSheet
    var availableContentHeightPx by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    // QuickCreateScreen 位于 MainScreen 的 Scaffold 内容区内；该高度已扣除手机底部 BottomBar。
    // 因此阈值按“设备屏幕高度 - BottomBar 高度”的一半计算，避免 sheet 自身高度变化影响收起手感。
    val sheetDismissThresholdPx = if (availableContentHeightPx > 0f) {
        availableContentHeightPx / 2f
    } else if (sheetHeightPx > 0f) {
        sheetHeightPx / 2f
    } else {
        Float.MAX_VALUE
    }
    var sheetDragOffsetPx by remember { mutableFloatStateOf(0f) }
    var sheetDragging by remember { mutableStateOf(false) }
    val animatedSheetDragOffsetPx by animateFloatAsState(
        targetValue = sheetDragOffsetPx,
        animationSpec = if (sheetDragging) snap() else tween(durationMillis = 180),
        label = "QuickCreateSheetDragOffset",
    )

    LaunchedEffect(uiState.activeSheet) {
        uiState.activeSheet?.let { lastActiveSheet = it }
    }

    LaunchedEffect(activeBusinessSheetVisible) {
        if (activeBusinessSheetVisible) {
            sheetDragging = false
            sheetDragOffsetPx = 0f
        } else {
            delay(180)
            sheetDragging = false
            sheetDragOffsetPx = 0f
            lastActiveSheet = null
        }
    }

    SystemBackHandler(enabled = activeBusinessSheetVisible) {
        screenModel.closeActiveSheet()
    }

    fun startSheetDrag() {
        sheetDragging = true
    }

    fun dragSheet(deltaPx: Float) {
        val maxDragOffsetPx = if (sheetHeightPx > 0f) sheetHeightPx else Float.MAX_VALUE
        sheetDragOffsetPx = (sheetDragOffsetPx + deltaPx).coerceIn(0f, maxDragOffsetPx)
    }

    fun endSheetDrag() {
        sheetDragging = false
        if (sheetDragOffsetPx >= sheetDismissThresholdPx) {
            screenModel.closeActiveSheet()
        } else {
            sheetDragOffsetPx = 0f
        }
    }

    fun cancelSheetDrag() {
        sheetDragging = false
        sheetDragOffsetPx = 0f
    }

    uiState.error?.let { error ->
        // 错误提示由页面状态驱动展示，但自动清理需要等待 3 秒后回调最新 ScreenModel。
        // rememberUpdatedState 可避免延迟期间重组导致协程持有旧实例。
        LaunchedEffect(error) {
            delay(3000)
            currentScreenModel.dismissError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                availableContentHeightPx = coordinates.size.height.toFloat()
            }
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
                    onCreateProject = screenModel::createProject,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentMode) {
                        QuickCreateMode.CREATION -> CreationScrollableArea(
                            uiState = uiState,
                        )
                        QuickCreateMode.INSPIRATION -> QuickCreateInspirationArea(
                            uiState = uiState,
                            onApplyTemplate = screenModel::applyInspirationTemplate,
                            onLoadMoreTemplates = screenModel::loadMoreInspirationTemplates,
                        )
                    }
                }

                if (uiState.historyDetailLoading || uiState.selectedHistoryDetail != null) {
                    QuickCreateHistoryDetailDialog(
                        isLoading = uiState.historyDetailLoading,
                        item = uiState.selectedHistoryDetail,
                        onDismiss = screenModel::dismissHistoryDetail,
                    )
                }

                if (uiState.projectDetailLoading || uiState.selectedProjectDetail != null) {
                    QuickCreateProjectDetailDialog(
                        isLoading = uiState.projectDetailLoading,
                        project = uiState.selectedProjectDetail,
                        onDismiss = screenModel::dismissProjectDetail,
                    )
                }

                if (uiState.showCreationInput) {
                    QuickCreateEditorPanel(
                        uiState = uiState,
                        onTabSwitch = screenModel::switchTab,
                        onPromptChange = if (uiState.currentTab == QuickCreateTab.IMAGE) screenModel::updateImagePrompt else screenModel::updateVideoPrompt,
                        onLaunchImagePicker = {
                            controller.pickMedia(
                                mediaPermission = Permission.MediaImages,
                                mediaType = MediaType.IMAGE,
                                onSuccess = { uriString -> currentScreenModel.pickImageReference(uriString) },
                                onPermissionDenied = {},
                            )
                        },
                        onLaunchVideoPicker = {
                            controller.pickMedia(
                                mediaPermission = Permission.MediaVideo,
                                mediaType = MediaType.VIDEO,
                                onSuccess = { uriString -> currentScreenModel.pickVideoReference(uriString) },
                                onPermissionDenied = {},
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
                        onOpenModelSheet = screenModel::showModelPickerSheet,
                        onOpenParamsSheet = screenModel::showParamsSheet,
                        onImageRatioChange = screenModel::updateImageAspectRatio,
                        onImageResChange = screenModel::updateImageResolution,
                        onImageQualityChange = screenModel::updateImageQuality,
                        onImageCountChange = screenModel::updateImageCount,
                        onVideoRatioChange = screenModel::updateVideoAspectRatio,
                        onVideoResChange = screenModel::updateVideoResolution,
                        onVideoDurationChange = screenModel::updateVideoDuration,
                        onToggleAudio = screenModel::toggleGenerateAudio,
                        onRestoreDraft = screenModel::restoreDraft,
                        onDiscardDraft = screenModel::discardDraft,
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
                        errorText.orEmpty(),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
                        fontSize = 13.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = activeBusinessSheetVisible,
            enter = slideInVertically { it } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically { it } + fadeOut(animationSpec = tween(160)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        sheetHeightPx = coordinates.size.height.toFloat()
                    }
                    .offset { IntOffset(x = 0, y = animatedSheetDragOffsetPx.roundToInt()) },
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = Color.Black.copy(alpha = 0.5f),
                    onClick = screenModel::closeActiveSheet,
                ) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
                }
                // 在页面边界把服务端模型字段映射为 UI 字段，参数面板只消费稳定的展示模型。
                val paramsServiceFields = if (uiState.currentTab == QuickCreateTab.IMAGE) {
                    uiState.selectedImageServiceModel.quickCreationServiceFieldUiItems(uiState.imageServiceParams)
                } else {
                    uiState.selectedVideoServiceModel.quickCreationServiceFieldUiItems(uiState.videoServiceParams)
                }
                // activeSheet 清空后仍保留上一份内容给 AnimatedVisibility 执行退出动画，
                // 否则拖拽或返回关闭时内部内容会先变成空，视觉上像是瞬间消失。
                when (renderedSheet) {
                    QuickCreateSheet.MODEL_PICKER -> QuickCreateModelSheet(
                        visible = true,
                        isImage = uiState.currentTab == QuickCreateTab.IMAGE,
                        uiState = uiState,
                        onImageServiceModelSelected = {
                            screenModel.updateImageServiceModel(it)
                        },
                        onVideoServiceModelSelected = {
                            screenModel.updateVideoServiceModel(it)
                        },
                        onTabSwitch = screenModel::switchTab,
                        onDismiss = screenModel::closeActiveSheet,
                        onSheetDragStart = ::startSheetDrag,
                        onSheetDrag = ::dragSheet,
                        onSheetDragEnd = ::endSheetDrag,
                        onSheetDragCancel = ::cancelSheetDrag,
                    )
                    QuickCreateSheet.PARAMS -> QuickCreateParamsSheet(
                        visible = true,
                        isImage = uiState.currentTab == QuickCreateTab.IMAGE,
                        uiState = uiState,
                        serviceFields = paramsServiceFields,
                        onDismiss = screenModel::closeActiveSheet,
                        onImageModelSelected = {
                            screenModel.updateImageModel(it)
                        },
                        onImageServiceParamChange = screenModel::updateImageServiceParam,
                        onVideoServiceParamChange = screenModel::updateVideoServiceParam,
                        onToggleRealistic = screenModel::toggleRealisticMode,
                        onImageSeedChange = screenModel::updateImageSeed,
                        onVideoSeedChange = screenModel::updateVideoSeed,
                        onImageRatioChange = screenModel::updateImageAspectRatio,
                        onImageResChange = screenModel::updateImageResolution,
                        onImageQualityChange = screenModel::updateImageQuality,
                        onImageCountChange = screenModel::updateImageCount,
                        onServiceUploadFieldClick = { mediaType, fieldParamKey ->
                            when (mediaType) {
                                QuickCreateMediaType.IMAGE -> controller.pickMedia(
                                    mediaPermission = Permission.MediaImages,
                                    mediaType = MediaType.IMAGE,
                                    onSuccess = { uriString ->
                                        currentScreenModel.pickImageReferenceForField(uriString, fieldParamKey)
                                    },
                                    onPermissionDenied = {},
                                )
                                QuickCreateMediaType.VIDEO -> controller.pickMedia(
                                    mediaPermission = Permission.MediaVideo,
                                    mediaType = MediaType.VIDEO,
                                    onSuccess = { uriString ->
                                        currentScreenModel.pickVideoReferenceForField(uriString, fieldParamKey)
                                    },
                                    onPermissionDenied = {},
                                )
                                QuickCreateMediaType.AUDIO -> controller.pickMedia(
                                    mediaPermission = Permission.MediaAudio,
                                    mediaType = MediaType.AUDIO,
                                    onSuccess = { uriString ->
                                        currentScreenModel.pickAudioReferenceForField(uriString, fieldParamKey)
                                    },
                                    onPermissionDenied = { pendingPermission = Permission.MediaAudio },
                                )
                            }
                        },
                        onRemoveMedia = screenModel::removeMediaReference,
                        onSheetDragStart = ::startSheetDrag,
                        onSheetDrag = ::dragSheet,
                        onSheetDragEnd = ::endSheetDrag,
                        onSheetDragCancel = ::cancelSheetDrag,
                    )
                    null -> Unit
                }
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
    onCreateProject: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(DarkBackground),
    ) {
        // 左侧菜单、中间模式切换和右侧创建入口必须彼此覆盖定位，避免 `+` 显隐改变中间 tab 的测量中心。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_back_content_description,
                        ),
                        tint = Color.White,
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_menu_content_description,
                        ),
                        tint = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
        ModeSwitch(
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
            modifier = Modifier.align(Alignment.Center),
        )
        if (selectedMode == QuickCreateMode.CREATION) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
            ) {
                QuickCreateCreateProjectAction(onCreateProject = onCreateProject)
            }
        }
    }
}

@Composable
private fun ModeSwitch(
    selectedMode: QuickCreateMode,
    onModeSelected: (QuickCreateMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuickCreateMode.entries.forEachIndexed { index, mode ->
            TextButton(
                onClick = { onModeSelected(mode) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    quickCreateNavigationText(mode.navigationLabel),
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
) {
    val hasConversation = uiState.conversationItems.isNotEmpty() ||
        uiState.submittedPrompt.isNotBlank() ||
        uiState.results.isNotEmpty() ||
        uiState.taskStatus != QuickCreateTaskUiStatus.IDLE

    if (hasConversation) {
        QuickCreateConversationArea(
            uiState = uiState,
        )
    } else {
        Spacer(modifier = Modifier.fillMaxSize())
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
                    onCreateProject = {},
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentMode) {
                        QuickCreateMode.CREATION -> CreationScrollableArea(
                            uiState = uiState,
                        )
                        QuickCreateMode.INSPIRATION -> QuickCreateInspirationArea(
                            uiState = uiState,
                            onApplyTemplate = {},
                            onLoadMoreTemplates = {},
                        )
                    }
                }

                if (uiState.showCreationInput) {
                    QuickCreateEditorPanel(
                        uiState = uiState,
                        onTabSwitch = {},
                        onPromptChange = {},
                        onLaunchImagePicker = {},
                        onLaunchVideoPicker = {},
                        onLaunchAudioPicker = {},
                        onRemoveMedia = {},
                        onOpenModelSheet = {},
                        onOpenParamsSheet = {},
                        onImageRatioChange = {},
                        onImageResChange = {},
                        onImageQualityChange = {},
                        onImageCountChange = {},
                        onVideoRatioChange = {},
                        onVideoResChange = {},
                        onVideoDurationChange = {},
                        onToggleAudio = {},
                        onRestoreDraft = {},
                        onDiscardDraft = {},
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
            QuickCreateEditorPanel(
                uiState = previewQuickCreateUiState(),
                onTabSwitch = {},
                onPromptChange = {},
                onLaunchImagePicker = {},
                onLaunchVideoPicker = {},
                onLaunchAudioPicker = {},
                onRemoveMedia = {},
                onOpenModelSheet = {},
                onOpenParamsSheet = {},
                onImageRatioChange = {},
                onImageResChange = {},
                onImageQualityChange = {},
                onImageCountChange = {},
                onVideoRatioChange = {},
                onVideoResChange = {},
                onVideoDurationChange = {},
                onToggleAudio = {},
                onRestoreDraft = {},
                onDiscardDraft = {},
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
