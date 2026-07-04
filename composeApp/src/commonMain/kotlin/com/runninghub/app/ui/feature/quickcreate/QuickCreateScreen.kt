package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.runninghub.app.ui.feature.quickcreate.presentation.modelselector.QuickCreateModelSheet
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateCreateProjectAction
import com.runninghub.app.ui.feature.quickcreate.presentation.project.QuickCreateProjectDetailDialog
import com.runninghub.app.ui.designsystem.components.feedback.RhSnackbar
import com.runninghub.app.ui.designsystem.components.feedback.RhSnackbarSeverity
import com.runninghub.app.ui.designsystem.components.navigation.RhTopBar
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateUiState
import com.runninghub.feature.quickcreate.presentation.fields.quickCreationServiceFieldUiItems
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateNavigationLabel
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateSheet
import com.runninghub.feature.quickcreate.presentation.state.QuickCreateTab
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateConversationItemUi
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateResultAction
import com.runninghub.feature.quickcreate.presentation.result.QuickCreateTaskUiStatus
import com.runninghub.feature.quickcreate.presentation.editor.QuickCreateMediaType
import com.runninghub.feature.quickcreate.presentation.inspiration.QuickCreatePlazaReuseIntent
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_top_bar_back_content_description
import runninghub.composeapp.generated.resources.quick_create_top_bar_menu_content_description
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 快捷创作页面在 Voyager 导航中的入口。
 *
 * 该类型只负责从 Koin 获取 [QuickCreateScreenModel] 并把它交给页面内容函数；
 * 具体业务编排由 ScreenModel/Coordinator 处理，避免导航对象直接持有创作、上传或轮询状态。
 */
class QuickCreateVoyagerScreen(
    private val consumePlazaReuseIntent: () -> QuickCreatePlazaReuseIntent? = { null },
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel: QuickCreateScreenModel = koinScreenModel()
        QuickCreateScreen(
            screenModel = screenModel,
            consumePlazaReuseIntent = consumePlazaReuseIntent,
        )
    }
}

@Composable
private fun QuickCreateScreen(
    screenModel: QuickCreateScreenModel,
    consumePlazaReuseIntent: () -> QuickCreatePlazaReuseIntent?,
) {
    val uiState by screenModel.uiState.collectAsState()
    val currentScreenModel by rememberUpdatedState(screenModel)
    val windowInfo = LocalRhWindowInfo.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val rootWindowHeightPx = rememberRootWindowHeightPx().toFloat()

    // 页面只依赖权限状态的领域边界，底层是否使用 DataStore 由 DI 组合根决定。
    val permissionStateStore: PermissionStateStore = koinInject()
    val controller: PermissionController = rememberPermissionController(permissionStateStore)

    var pendingPermission by remember { mutableStateOf<Permission?>(null) }
    val errorText = uiState.error?.asQuickCreateText()
    val activeBusinessSheetVisible = uiState.activeSheet != null && uiState.showCreationInput
    var lastActiveSheet by remember { mutableStateOf<QuickCreateSheet?>(null) }
    val renderedSheet = uiState.activeSheet ?: lastActiveSheet
    var availableContentHeightPx by remember { mutableFloatStateOf(0f) }
    var rootBottomWindowPx by remember { mutableFloatStateOf(0f) }
    var editorBottomPx by remember { mutableFloatStateOf(0f) }
    var editorHeightPx by remember { mutableFloatStateOf(0f) }
    var editorImeOffsetPx by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()
    val navigationBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val keyboardVisible = imeBottomPx > 0f
    val editorKeyboardGapPx = with(density) { 8.dp.toPx() }
    // 输入栏按窗口坐标闭环贴近键盘顶部，避免把局部 root 高度或整段 IME inset 误当作最终偏移。
    val targetEditorBottomPx =
        (rootWindowHeightPx.takeIf { it > 0f } ?: rootBottomWindowPx) -
            imeBottomPx +
            navigationBottomPx -
            editorKeyboardGapPx
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
        // 任一业务 sheet 弹出时收起软键盘,避免确认弹窗与键盘叠在一起互相遮挡。
        if (uiState.activeSheet != null) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(screenModel) {
        consumePlazaReuseIntent()?.let(screenModel::applyPlazaReuseIntent)
    }

    LaunchedEffect(keyboardVisible, editorBottomPx, targetEditorBottomPx) {
        if (!keyboardVisible) {
            editorImeOffsetPx = 0f
            return@LaunchedEffect
        }
        if (editorBottomPx <= 0f) return@LaunchedEffect

        val deltaPx = editorBottomPx - targetEditorBottomPx
        if (abs(deltaPx) > 1f) {
            editorImeOffsetPx -= deltaPx
        }
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

    fun handleResultAction(action: QuickCreateResultAction, item: QuickCreateConversationItemUi) {
        when (action) {
            QuickCreateResultAction.TryAgain,
            QuickCreateResultAction.Retry -> {
                screenModel.restoreConversationPrompt(item.prompt)
                screenModel.generate()
            }
            QuickCreateResultAction.ReuseParameters -> screenModel.restoreConversationPrompt(item.prompt)
            QuickCreateResultAction.CopyPrompt -> clipboardManager.setText(AnnotatedString(item.prompt))
            QuickCreateResultAction.ViewTask,
            QuickCreateResultAction.ViewResult,
            QuickCreateResultAction.Save,
            // TODO(result-card-v2 T5)：接线下载到本地能力。
            QuickCreateResultAction.Download,
            // TODO(result-card-v2 T4)：接线复制到素材区能力。
            QuickCreateResultAction.CopyToComposer,
            QuickCreateResultAction.ViewDetail -> Unit
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
                rootBottomWindowPx = coordinates.positionInWindow().y + coordinates.size.height
            }
            .background(RhTheme.colors.backgroundPrimary)
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
                    onBack = null,
                    onCreateProject = screenModel::createProject,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CreationScrollableArea(
                        uiState = uiState,
                        // 悬浮输入面板覆盖会话区底部;按面板实测高度收缩会话视口,
                        // 让最后一张结果卡的操作按钮可以完整滚出。
                        bottomInset = if (uiState.showCreationInput) {
                            with(density) { editorHeightPx.toDp() }
                        } else {
                            0.dp
                        },
                        onResultAction = ::handleResultAction,
                        onSampleClick = screenModel::restoreConversationPrompt,
                    )
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
            }
        }

        if (uiState.showCreationInput) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .widthIn(max = windowInfo.detailContentMaxWidth)
                    .offset { IntOffset(x = 0, y = editorImeOffsetPx.roundToInt()) }
                    .onGloballyPositioned { coordinates ->
                        editorBottomPx = coordinates.positionInWindow().y + coordinates.size.height
                        editorHeightPx = coordinates.size.height.toFloat()
                    },
            ) {
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
                    onRestoreDraft = screenModel::restoreDraft,
                    onDiscardDraft = screenModel::discardDraft,
                    onGenerate = screenModel::generate,
                )
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
                RhSnackbar(
                    message = errorText.orEmpty(),
                    severity = RhSnackbarSeverity.Error,
                    modifier = Modifier.padding(horizontal = RhSpacing.lg),
                )
            }
        }

        AnimatedVisibility(
            visible = activeBusinessSheetVisible,
            enter = slideInVertically { it } + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically { it } + fadeOut(animationSpec = tween(160)),
            // imePadding 保证键盘尚未收起时,确认/参数等 sheet 整体抬升到键盘之上,不被遮挡。
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding(),
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
                        onModelPickerQueryChange = screenModel::updateModelPickerQuery,
                        onModelPickerFilterSelected = screenModel::selectModelPickerFilter,
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
                        onImageServiceParamChange = screenModel::updateImageServiceParam,
                        onVideoServiceParamChange = screenModel::updateVideoServiceParam,
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
                    QuickCreateSheet.GENERATION_CONFIRM -> QuickCreateGenerationConfirmSheet(
                        uiState = uiState,
                        onConfirm = screenModel::confirmGeneration,
                        onDismiss = screenModel::closeActiveSheet,
                    )
                    null -> Unit
                }
            }
        }
    }

    val activePermission = pendingPermission
    if (activePermission != null) {
        PermissionBottomSheet(
            permission = activePermission,
            onDismiss = { pendingPermission = null },
            onAuthorize = {
                pendingPermission = null
                controller.checkAndRequest(
                    permission = activePermission,
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
    onBack: (() -> Unit)?,
    onCreateProject: (String) -> Unit,
) {
    RhTopBar(
        title = quickCreateNavigationText(QuickCreateNavigationLabel.CreationMode),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_back_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            } else {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(
                            Res.string.quick_create_top_bar_menu_content_description,
                        ),
                        tint = RhTheme.colors.textSecondary,
                    )
                }
            }
        },
        actions = { QuickCreateCreateProjectAction(onCreateProject = onCreateProject) },
    )
}

@Composable
private fun CreationScrollableArea(
    uiState: QuickCreateUiState,
    onResultAction: (QuickCreateResultAction, QuickCreateConversationItemUi) -> Unit,
    onSampleClick: (String) -> Unit,
    bottomInset: Dp = 0.dp,
) {
    val hasConversation = uiState.conversationItems.isNotEmpty() ||
        uiState.submittedPrompt.isNotBlank() ||
        uiState.results.isNotEmpty() ||
        uiState.taskStatus != QuickCreateTaskUiStatus.IDLE

    if (hasConversation) {
        QuickCreateConversationArea(
            uiState = uiState,
            bottomInset = bottomInset,
            onResultAction = onResultAction,
        )
    } else {
        // 空引导态同样被悬浮输入面板覆盖:按面板高度收缩后再居中,
        // 避免矮屏设备上示例词条沉到面板后面。
        QuickCreateEmptyGuide(
            onSampleClick = onSampleClick,
            modifier = Modifier.padding(bottom = bottomInset),
        )
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
            .background(RhTheme.colors.backgroundPrimary),
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
                    onBack = null,
                    onCreateProject = {},
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CreationScrollableArea(
                        uiState = uiState,
                        onResultAction = { _, _ -> },
                        onSampleClick = {},
                    )
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
                .background(RhTheme.colors.backgroundPrimary),
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
