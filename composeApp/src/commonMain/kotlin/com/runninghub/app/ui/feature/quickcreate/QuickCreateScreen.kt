@file:OptIn(ExperimentalMaterial3Api::class)

package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.runninghub.app.util.formatCashAmount
import com.runninghub.shared.data.local.PermissionDataStore
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.repository.QuickCreationHistoryItem
import com.runninghub.shared.domain.repository.QuickCreationProject
import com.runninghub.shared.domain.repository.QuickCreationServiceModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
                            onHistoryItemSelected = screenModel::selectHistoryOutput,
                            onLoadMoreHistory = screenModel::loadMoreQuickCreationHistory,
                            onCancelHistoryTask = screenModel::cancelHistoryTask,
                            onProjectSelected = screenModel::selectProject,
                            onClearSelectedProject = screenModel::clearSelectedProject,
                            onLoadMoreProjects = screenModel::loadMoreQuickCreationProjects,
                            onToggleProjectPin = screenModel::toggleProjectPin,
                            onCreateProject = screenModel::createProject,
                            onRenameProject = screenModel::renameProject,
                            onDeleteProject = screenModel::deleteProject,
                            onShowProjectDetail = screenModel::selectProjectDetail,
                        )
                        QuickCreateMode.INSPIRATION -> InspirationArea(
                            uiState = uiState,
                            onApplyTemplate = screenModel::applyInspirationTemplate,
                            onLoadMoreTemplates = screenModel::loadMoreInspirationTemplates,
                        )
                    }
                }

                if (uiState.historyDetailLoading || uiState.selectedHistoryDetail != null) {
                    HistoryDetailDialog(
                        isLoading = uiState.historyDetailLoading,
                        item = uiState.selectedHistoryDetail,
                        onDismiss = screenModel::dismissHistoryDetail,
                    )
                }

                if (uiState.projectDetailLoading || uiState.selectedProjectDetail != null) {
                    ProjectDetailDialog(
                        isLoading = uiState.projectDetailLoading,
                        project = uiState.selectedProjectDetail,
                        onDismiss = screenModel::dismissProjectDetail,
                    )
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
                        uiState.error ?: "",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
                        fontSize = 13.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.activeSheet != null && uiState.showCreationInput,
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
                    onClick = screenModel::closeActiveSheet,
                ) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
                }
                when (uiState.activeSheet) {
                    QuickCreateSheet.MODEL_PICKER -> QuickCreateModelSheet(
                        visible = true,
                        isImage = uiState.currentTab == QuickCreateTab.IMAGE,
                        uiState = uiState,
                        onDismiss = screenModel::closeActiveSheet,
                        onImageServiceModelSelected = {
                            screenModel.updateImageServiceModel(it)
                            screenModel.closeActiveSheet()
                        },
                        onVideoServiceModelSelected = {
                            screenModel.updateVideoServiceModel(it)
                            screenModel.closeActiveSheet()
                        },
                    )
                    QuickCreateSheet.PARAMS -> QuickCreateParamsSheet(
                    visible = true,
                    isImage = uiState.currentTab == QuickCreateTab.IMAGE,
                    uiState = uiState,
                    onDismiss = screenModel::closeActiveSheet,
                    onImageModelSelected = {
                        screenModel.updateImageModel(it)
                    },
                    onImageServiceParamChange = screenModel::updateImageServiceParam,
                    onVideoServiceParamChange = screenModel::updateVideoServiceParam,
                    onToggleRealistic = screenModel::toggleRealisticMode,
                    onImageSeedChange = screenModel::updateImageSeed,
                    onVideoSeedChange = screenModel::updateVideoSeed,
                    onServiceUploadFieldClick = { mediaType, fieldParamKey ->
                        when (mediaType) {
                            QuickCreateMediaType.IMAGE -> controller.pickMedia(
                                mediaPermission = Permission.MediaImages,
                                mediaType = MediaType.IMAGE,
                                onSuccess = { uriString ->
                                    currentScreenModel.pickImageReferenceForField(uriString, fieldParamKey)
                                },
                                onPermissionDenied = { pendingPermission = Permission.MediaImages },
                            )
                            QuickCreateMediaType.VIDEO -> controller.pickMedia(
                                mediaPermission = Permission.MediaVideo,
                                mediaType = MediaType.VIDEO,
                                onSuccess = { uriString ->
                                    currentScreenModel.pickVideoReferenceForField(uriString, fieldParamKey)
                                },
                                onPermissionDenied = { pendingPermission = Permission.MediaVideo },
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
    onHistoryItemSelected: (String) -> Unit,
    onLoadMoreHistory: () -> Unit,
    onCancelHistoryTask: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onClearSelectedProject: () -> Unit,
    onLoadMoreProjects: () -> Unit,
    onToggleProjectPin: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShowProjectDetail: (String) -> Unit,
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
        else -> HistoryArea(
            uiState = uiState,
            onHistoryItemSelected = onHistoryItemSelected,
            onLoadMoreHistory = onLoadMoreHistory,
            onCancelHistoryTask = onCancelHistoryTask,
            onProjectSelected = onProjectSelected,
            onClearSelectedProject = onClearSelectedProject,
            onLoadMoreProjects = onLoadMoreProjects,
            onToggleProjectPin = onToggleProjectPin,
            onCreateProject = onCreateProject,
            onRenameProject = onRenameProject,
            onDeleteProject = onDeleteProject,
            onShowProjectDetail = onShowProjectDetail,
        )
    }
}

@Composable
private fun HistoryArea(
    uiState: QuickCreateUiState,
    onHistoryItemSelected: (String) -> Unit,
    onLoadMoreHistory: () -> Unit,
    onCancelHistoryTask: (String) -> Unit,
    onProjectSelected: (String) -> Unit,
    onClearSelectedProject: () -> Unit,
    onLoadMoreProjects: () -> Unit,
    onToggleProjectPin: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShowProjectDetail: (String) -> Unit,
) {
    val selectedProject = uiState.projects.firstOrNull { it.projectId == uiState.selectedProjectId }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceMD),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        item {
            ProjectStrip(
                projects = uiState.projects,
                isLoading = uiState.projectsLoading,
                isLoadingMore = uiState.projectsLoadingMore,
                hasMore = uiState.projectsHasMore,
                selectedProjectId = uiState.selectedProjectId,
                pinningIds = uiState.projectPinningIds,
                mutatingIds = uiState.projectMutatingIds,
                onProjectSelected = onProjectSelected,
                onClearSelectedProject = onClearSelectedProject,
                onLoadMoreProjects = onLoadMoreProjects,
                onToggleProjectPin = onToggleProjectPin,
                onCreateProject = onCreateProject,
                onRenameProject = onRenameProject,
                onDeleteProject = onDeleteProject,
                onShowProjectDetail = onShowProjectDetail,
            )
            Spacer(Modifier.height(Dimens.SpaceSM))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedProject?.name ?: "最近创作",
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (uiState.historyLoading || uiState.projectTasksLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                }
            }
        }

        items(uiState.historyItems, key = { it.taskId }) { item ->
            HistoryItemRow(
                item = item,
                isCancelling = item.taskId in uiState.historyCancellingTaskIds,
                onClick = {
                    item.outputs.firstOrNull()?.outputId?.let(onHistoryItemSelected)
                },
                onCancel = {
                    onCancelHistoryTask(item.taskId)
                },
            )
        }

        if (uiState.historyHasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMoreHistory,
                    enabled = !uiState.historyLoadingMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    border = BorderStroke(1.dp, DarkOutlineVariant),
                ) {
                    if (uiState.historyLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                        Spacer(Modifier.width(Dimens.SpaceSM))
                    }
                    Text("加载更多")
                }
            }
        }
    }
}

@Composable
private fun ProjectStrip(
    projects: List<QuickCreationProject>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    selectedProjectId: String?,
    pinningIds: Set<String>,
    mutatingIds: Set<String>,
    onProjectSelected: (String) -> Unit,
    onClearSelectedProject: () -> Unit,
    onLoadMoreProjects: () -> Unit,
    onToggleProjectPin: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onShowProjectDetail: (String) -> Unit,
) {
    var createDialogVisible by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<QuickCreationProject?>(null) }
    var deleteTarget by remember { mutableStateOf<QuickCreationProject?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "项目",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                }
                IconButton(
                    onClick = { createDialogVisible = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "新建项目",
                        tint = Primary300,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            RecentProjectChip(
                selected = selectedProjectId == null,
                onClick = onClearSelectedProject,
            )
            projects.forEach { project ->
                ProjectChip(
                    project = project,
                    selected = project.projectId == selectedProjectId,
                    isPinning = project.projectId in pinningIds,
                    isMutating = project.projectId in mutatingIds,
                    onClick = { onProjectSelected(project.projectId) },
                    onTogglePin = { onToggleProjectPin(project.projectId) },
                    onShowDetail = { onShowProjectDetail(project.projectId) },
                    onRename = { renameTarget = project },
                    onDelete = { deleteTarget = project },
                )
            }
            if (hasMore) {
                LoadMoreProjectsChip(
                    isLoading = isLoadingMore,
                    onClick = onLoadMoreProjects,
                )
            }
        }
    }

    if (createDialogVisible) {
        ProjectNameDialog(
            title = "新建项目",
            initialName = "",
            confirmText = "新建",
            onDismiss = { createDialogVisible = false },
            onConfirm = { name ->
                createDialogVisible = false
                onCreateProject(name)
            },
        )
    }

    renameTarget?.let { project ->
        ProjectNameDialog(
            title = "重命名项目",
            initialName = project.name,
            confirmText = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                onRenameProject(project.projectId, name)
            },
        )
    }

    deleteTarget?.let { project ->
        ProjectDeleteDialog(
            project = project,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                onDeleteProject(project.projectId)
            },
        )
    }
}

@Composable
private fun RecentProjectChip(
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Primary300.copy(alpha = 0.16f) else DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, if (selected) Primary300.copy(alpha = 0.65f) else DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = if (selected) Primary300 else Neutral400,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "最近创作",
                color = if (selected) Primary300 else Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LoadMoreProjectsChip(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !isLoading,
        color = DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Primary300,
                )
            } else {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = null,
                    tint = Primary300,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                if (isLoading) "加载中" else "加载更多",
                color = Primary300,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProjectChip(
    project: QuickCreationProject,
    selected: Boolean,
    isPinning: Boolean,
    isMutating: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onShowDetail: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        color = if (selected) Primary300.copy(alpha = 0.16f) else DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(
            1.dp,
            when {
                selected -> Primary300.copy(alpha = 0.75f)
                project.pinned -> Primary300.copy(alpha = 0.65f)
                else -> DarkOutlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onTogglePin,
                enabled = !isPinning,
                modifier = Modifier.size(28.dp),
            ) {
                if (isPinning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Primary300,
                    )
                } else {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = if (project.pinned) "取消置顶项目" else "置顶项目",
                        tint = if (project.pinned) Primary300 else Neutral400,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    project.name,
                    color = if (selected) Primary300 else Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${project.taskCount} 个任务",
                    color = Neutral400,
                    fontSize = 11.sp,
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    enabled = !isMutating,
                    modifier = Modifier.size(28.dp),
                ) {
                    if (isMutating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                    } else {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "项目操作",
                            tint = Neutral400,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("详情") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onShowDetail()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.SemiBold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("项目名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmedName) },
                enabled = trimmedName.isNotBlank(),
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ProjectDeleteDialog(
    project: QuickCreationProject,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("删除项目", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text("删除「${project.name}」后，项目入口会从当前列表移除。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("删除", color = ErrorDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ProjectDetailDialog(
    isLoading: Boolean,
    project: QuickCreationProject?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = {
            Text(
                if (isLoading) "加载项目" else "项目详情",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isLoading || project == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("正在加载项目详情")
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    if (!project.coverUrl.isNullOrBlank()) {
                        SmartAsyncImage(
                            imageUrl = project.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusMD)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Text(
                        project.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    ProjectDetailRow(label = "任务数量", value = "${project.taskCount}")
                    ProjectDetailRow(label = "置顶状态", value = if (project.pinned) "已置顶" else "未置顶")
                    project.createdAt?.let { ProjectDetailRow(label = "创建时间", value = formatProjectTime(it)) }
                    project.updatedAt?.let { ProjectDetailRow(label = "更新时间", value = formatProjectTime(it)) }
                }
            }
        },
    )
}

private fun formatProjectTime(raw: String): String {
    val millis = raw.toLongOrNull() ?: return raw
    val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(localDateTime.year.toString().padStart(4, '0'))
        append('-')
        append(localDateTime.monthNumber.toString().padStart(2, '0'))
        append('-')
        append(localDateTime.dayOfMonth.toString().padStart(2, '0'))
        append(' ')
        append(localDateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(localDateTime.minute.toString().padStart(2, '0'))
    }
}

@Composable
private fun ProjectDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Neutral400, fontSize = 13.sp)
        Text(
            value,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryItemRow(
    item: QuickCreationHistoryItem,
    isCancelling: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val output = item.outputs.firstOrNull()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = output != null) { onClick() },
        color = DarkSurface,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Dimens.SpaceSM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusSM)),
                contentAlignment = Alignment.Center,
            ) {
                val previewUrl = output?.thumbnailUrl ?: output?.url
                if (previewUrl != null) {
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.28f),
                    )
                }
                if (output?.isVideo == true) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.86f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    item.params["prompt"] ?: item.taskId,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(item.categoryId, item.status, output?.type?.uppercase()).joinToString(" · "),
                    color = Neutral400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.cashAmount > 0.0) {
                    Text(
                        "${formatCashAmount(item.cashAmount)} ${item.cashCurrency.orEmpty()}",
                        color = Primary300,
                        fontSize = 12.sp,
                    )
                }
                if (item.isCancelableQuickCreationTask) {
                    TextButton(
                        onClick = onCancel,
                        enabled = !isCancelling,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        if (isCancelling) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = Primary300,
                            )
                            Spacer(Modifier.width(Dimens.SpaceXS))
                        }
                        Text(
                            text = if (isCancelling) "取消中" else "取消任务",
                            color = Primary300,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

private val terminalQuickCreationHistoryStatuses = setOf("SUCCESS", "FAILED", "FAIL", "ERROR", "CANCELED", "CANCELLED")

private val QuickCreationHistoryItem.isCancelableQuickCreationTask: Boolean
    get() = status.isNotBlank() && status.uppercase() !in terminalQuickCreationHistoryStatuses

@Composable
private fun HistoryDetailDialog(
    isLoading: Boolean,
    item: QuickCreationHistoryItem?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        },
        title = {
            Text(
                if (isLoading) "加载详情" else "创作详情",
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            if (isLoading || item == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMD),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text("正在加载历史详情")
                }
            } else {
                val output = item.outputs.firstOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMD)) {
                    val previewUrl = output?.thumbnailUrl ?: output?.url
                    if (previewUrl != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(Dimens.RadiusMD)),
                            contentAlignment = Alignment.Center,
                        ) {
                            SmartAsyncImage(
                                imageUrl = previewUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (output?.isVideo == true) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp),
                                    tint = Color.White.copy(alpha = 0.86f),
                                )
                            }
                        }
                    }
                    Text(
                        item.params["prompt"] ?: item.taskId,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(
                            item.categoryId,
                            item.status,
                            output?.type?.uppercase(),
                            output?.let { "${it.width ?: "-"}x${it.height ?: "-"}" },
                        ).joinToString(" · "),
                        color = Neutral500,
                        fontSize = 12.sp,
                    )
                    if (item.cashAmount > 0.0) {
                        Text(
                            "${formatCashAmount(item.cashAmount)} ${item.cashCurrency.orEmpty()}",
                            color = Primary300,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        },
        containerColor = DarkSurface,
        titleContentColor = Color.White,
        textContentColor = Color.White.copy(alpha = 0.82f),
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
private fun InspirationArea(
    uiState: QuickCreateUiState,
    onApplyTemplate: (String) -> Unit,
    onLoadMoreTemplates: () -> Unit,
) {
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
                templateId = template.templateId,
                title = template.title,
                category = template.categoryId ?: "IMAGE",
                coverUrl = template.coverUrl,
                videoUrl = template.videoUrl,
                tagHot = template.tagHot,
                tagNew = template.tagNew,
                onApplyTemplate = onApplyTemplate,
            )
        }

        if (!uiState.inspirationLoading && uiState.inspirationTemplates.isEmpty()) {
            item { EmptyArea() }
        }

        if (uiState.inspirationTemplatesHasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMoreTemplates,
                    enabled = !uiState.inspirationTemplatesLoadingMore,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.RadiusMD),
                    border = BorderStroke(1.dp, DarkOutlineVariant),
                ) {
                    if (uiState.inspirationTemplatesLoadingMore) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Primary300,
                        )
                        Spacer(Modifier.width(Dimens.SpaceSM))
                    }
                    Text("加载更多模板")
                }
            }
        }
    }
}

@Composable
private fun InspirationTemplateCard(
    templateId: String,
    title: String,
    category: String,
    coverUrl: String?,
    videoUrl: String?,
    tagHot: Boolean,
    tagNew: Boolean,
    onApplyTemplate: (String) -> Unit,
) {
    Surface(
        onClick = { onApplyTemplate(templateId) },
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
    val display = quickCreateTaskStatusDisplay(status, statusText)
    val indicatorColor = when (display.indicator) {
        QuickCreateTaskIndicator.Progress -> Primary300
        QuickCreateTaskIndicator.Success -> SuccessDark
        QuickCreateTaskIndicator.Error -> ErrorDark
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (display.indicator) {
                QuickCreateTaskIndicator.Progress -> CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = indicatorColor,
                    strokeWidth = 3.dp,
                )
                QuickCreateTaskIndicator.Success -> Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = indicatorColor,
                )
                QuickCreateTaskIndicator.Error -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = indicatorColor,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceXL))
            Text(
                display.text,
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
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onImageRatioChange: (ImageAspectRatio) -> Unit,
    onImageResChange: (ImageResolution) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onVideoRatioChange: (VideoAspectRatio) -> Unit,
    onVideoResChange: (VideoResolution) -> Unit,
    onVideoDurationChange: (VideoDuration) -> Unit,
    onToggleAudio: () -> Unit,
    onRestoreDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
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
    val configMediaRefs = (if (isImage) imageConfig.mediaReferences else videoConfig.mediaReferences)
        .quickCreationGlobalMediaReferences()
    val selectedServiceModel = if (isImage) uiState.selectedImageServiceModel else uiState.selectedVideoServiceModel
    val isTaskActive = uiState.taskStatus in listOf(
        QuickCreateTaskUiStatus.SUBMITTING,
        QuickCreateTaskUiStatus.QUEUING,
        QuickCreateTaskUiStatus.RUNNING,
    )

    val useCompactComposer = remember { true }
    if (useCompactComposer) {
        CompactCreationComposer(
            uiState = uiState,
            isImage = isImage,
            prompt = configPrompt,
            onPromptChange = onPromptChange,
            charCount = configCharCount,
            nearLimit = configNearLimit,
            overLimit = configOverLimit,
            mediaReferences = configMediaRefs,
            selectedServiceModel = selectedServiceModel,
            serviceModelsLoading = uiState.serviceModelsLoading,
            isTaskActive = isTaskActive,
            canGenerate = !isTaskActive &&
                configPrompt.isNotBlank() &&
                !configOverLimit &&
                !uiState.feePreviewLoading,
            onTabSwitch = onTabSwitch,
            onLaunchImagePicker = onLaunchImagePicker,
            onRemoveMedia = onRemoveMedia,
            onOpenModelSheet = onOpenModelSheet,
            onOpenParamsSheet = onOpenParamsSheet,
            onGenerate = onGenerate,
        )
    } else {
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
                    .padding(horizontal = Dimens.SpaceMD)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(top = Dimens.SpaceSM, bottom = Dimens.SpaceMD),
            ) {
                if (uiState.hasDraft && !isTaskActive) {
                    DraftResumeRow(
                        draftData = uiState.draftData,
                        onRestoreDraft = onRestoreDraft,
                        onDiscardDraft = onDiscardDraft,
                    )
                    Spacer(Modifier.height(Dimens.SpaceSM))
                }

                TabPillRow(
                    selectedTab = uiState.currentTab,
                    onTabSelected = onTabSwitch,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                ServiceModelSummaryRow(
                    model = if (isImage) uiState.selectedImageServiceModel else uiState.selectedVideoServiceModel,
                    loading = uiState.serviceModelsLoading,
                    feePreviewLoading = uiState.feePreviewLoading,
                    feePreviewError = uiState.feePreviewError,
                    cost = uiState.estimatedCost,
                    onClick = onOpenModelSheet,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                MediaToolbarRow(
                    mediaReferences = configMediaRefs,
                    onLaunchImagePicker = onLaunchImagePicker,
                    onLaunchVideoPicker = onLaunchVideoPicker,
                    onLaunchAudioPicker = onLaunchAudioPicker,
                )

                Spacer(Modifier.height(Dimens.SpaceSM))

                QuickParamChipRow(
                    uiState = uiState,
                    isImage = isImage,
                    onImageRatioChange = onImageRatioChange,
                    onImageResChange = onImageResChange,
                    onImageQualityChange = onImageQualityChange,
                    onImageCountChange = onImageCountChange,
                    onVideoRatioChange = onVideoRatioChange,
                    onVideoResChange = onVideoResChange,
                    onVideoDurationChange = onVideoDurationChange,
                    onToggleAudio = onToggleAudio,
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
                        onClick = onOpenParamsSheet,
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (uiState.activeSheet == QuickCreateSheet.PARAMS) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                                RoundedCornerShape(Dimens.RadiusMD),
                            ),
                        enabled = !isTaskActive,
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "创作调优",
                            modifier = Modifier.size(18.dp),
                            tint = if (uiState.activeSheet == QuickCreateSheet.PARAMS) Primary300 else Neutral400,
                        )
                    }

                    SendButton(
                        enabled = !isTaskActive &&
                            configPrompt.isNotBlank() &&
                            !configOverLimit &&
                            !uiState.feePreviewLoading,
                        isLoading = isTaskActive,
                        cost = uiState.estimatedCost,
                        feePreviewLoading = uiState.feePreviewLoading,
                        feePreviewError = uiState.feePreviewError,
                        onClick = onGenerate,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
}

@Composable
private fun CompactCreationComposer(
    uiState: QuickCreateUiState,
    isImage: Boolean,
    prompt: String,
    onPromptChange: (String) -> Unit,
    charCount: Int,
    nearLimit: Boolean,
    overLimit: Boolean,
    mediaReferences: List<MediaReference>,
    selectedServiceModel: QuickCreationServiceModel?,
    serviceModelsLoading: Boolean,
    isTaskActive: Boolean,
    canGenerate: Boolean,
    onTabSwitch: (QuickCreateTab) -> Unit,
    onLaunchImagePicker: () -> Unit,
    onRemoveMedia: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenParamsSheet: () -> Unit,
    onGenerate: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.SpaceMD)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = Dimens.SpaceMD),
        color = DarkSurface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(Dimens.RadiusXL),
        border = BorderStroke(1.dp, if (overLimit) ErrorDark else DarkOutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = Dimens.SpaceSM),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                CompactPromptField(
                    prompt = prompt,
                    onPromptChange = onPromptChange,
                    placeholder = if (isImage) "描述你的图片..." else "描述你想生成的视频...",
                    modifier = Modifier.weight(1f),
                )
                CompactIconAction(
                    icon = if (isImage) Icons.Default.AddPhotoAlternate else Icons.Default.Image,
                    contentDescription = "添加素材",
                    highlighted = mediaReferences.isNotEmpty(),
                    enabled = !isTaskActive,
                    onClick = onLaunchImagePicker,
                )
                CompactGenerateButton(
                    enabled = canGenerate,
                    isLoading = isTaskActive,
                    cost = uiState.estimatedCost,
                    feePreviewLoading = uiState.feePreviewLoading,
                    feePreviewError = uiState.feePreviewError,
                    onClick = onGenerate,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
            ) {
                CompactControlPill(
                    text = if (isImage) "图片" else "视频",
                    icon = if (isImage) Icons.Default.Image else Icons.Default.Videocam,
                    selected = true,
                    onClick = {
                        onTabSwitch(if (isImage) QuickCreateTab.VIDEO else QuickCreateTab.IMAGE)
                    },
                )
                CompactControlPill(
                    text = compactServiceModelLabel(
                        model = selectedServiceModel,
                        fallback = if (isImage) uiState.imageConfig.model.displayName else uiState.videoConfig.model.displayName,
                        loading = serviceModelsLoading,
                    ),
                    icon = Icons.Default.AutoAwesome,
                    selected = uiState.activeSheet == QuickCreateSheet.MODEL_PICKER,
                    onClick = onOpenModelSheet,
                )
                CompactControlPill(
                    text = compactParamsSummary(uiState, isImage),
                    icon = Icons.Default.Tune,
                    selected = uiState.activeSheet == QuickCreateSheet.PARAMS,
                    onClick = onOpenParamsSheet,
                )
                if (mediaReferences.isNotEmpty()) {
                    CompactControlPill(
                        text = "素材 ${mediaReferences.size}",
                        icon = Icons.Default.AttachFile,
                        selected = true,
                        onClick = {
                            mediaReferences.firstOrNull()?.let { onRemoveMedia(it.id) }
                        },
                    )
                }
                if (nearLimit || overLimit) {
                    Text(
                        text = "$charCount/$MAX_PROMPT_CHARS",
                        color = if (overLimit) ErrorDark else WarningDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactPromptField(
    prompt: String,
    onPromptChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = Neutral100,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    )

    Box(
        modifier = modifier.heightIn(min = 36.dp, max = 72.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (prompt.isEmpty()) {
            Text(
                text = placeholder,
                color = Neutral500,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = prompt,
            onValueChange = { newValue ->
                if (newValue.length <= MAX_PROMPT_CHARS) {
                    onPromptChange(newValue)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            cursorBrush = SolidColor(Primary300),
        )
    }
}

@Composable
private fun CompactIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (highlighted) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
                shape = RoundedCornerShape(Dimens.RadiusMD),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp),
            tint = if (highlighted) Primary300 else Neutral300,
        )
    }
}

@Composable
private fun CompactGenerateButton(
    enabled: Boolean,
    isLoading: Boolean,
    cost: Double,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = if (enabled && !isLoading) onClick else {{}},
        enabled = enabled || isLoading,
        color = if (enabled) Primary300 else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusFull),
        modifier = Modifier.height(40.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    color = Neutral100,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "生成",
                    modifier = Modifier.size(17.dp),
                    tint = if (enabled) Color.White else Neutral500,
                )
            }
            Text(
                text = quickCreateSendButtonLabel(
                    cost = cost,
                    feePreviewLoading = feePreviewLoading,
                    feePreviewError = feePreviewError,
                ),
                color = if (enabled) Color.White else Neutral500,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CompactControlPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) Primary300.copy(alpha = 0.11f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusFull),
        border = BorderStroke(
            1.dp,
            if (selected) Primary300.copy(alpha = 0.35f) else DarkOutlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (selected) Primary300 else Neutral400,
            )
            Text(
                text = text,
                color = if (selected) Neutral100 else Neutral300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Neutral500,
            )
        }
    }
}

private fun compactServiceModelLabel(
    model: QuickCreationServiceModel?,
    fallback: String,
    loading: Boolean,
): String {
    if (loading) return "模型加载中"
    val raw = model?.name?.takeIf { it.isNotBlank() } ?: fallback
    return raw
        .replace("全能图片G-2.0", "G-2.0")
        .replace("全能图片 G-2.0", "G-2.0")
        .replace("官方版", "")
        .trim(' ', '-', '·')
        .take(18)
}

private fun compactParamsSummary(uiState: QuickCreateUiState, isImage: Boolean): String =
    if (isImage) {
        uiState.imageConfig.aspectRatio.displayName
    } else {
        "视频生成"
    }

@Composable
private fun DraftResumeRow(
    draftData: DraftData?,
    onRestoreDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
) {
    if (draftData == null) return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Primary300.copy(alpha = 0.10f),
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, Primary300.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Primary300,
            )
            Text(
                text = draftData.resumeSummaryText(),
                modifier = Modifier.weight(1f),
                color = Neutral100,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = onDiscardDraft,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("丢弃", color = Neutral400, fontSize = 12.sp)
            }
            TextButton(
                onClick = onRestoreDraft,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("恢复", color = Primary300, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
private fun ServiceModelSummaryRow(
    model: QuickCreationServiceModel?,
    loading: Boolean,
    feePreviewLoading: Boolean,
    feePreviewError: String?,
    cost: Double,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusMD),
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.SpaceMD, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = Primary300,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        loading -> "模型加载中"
                        model != null -> model.name
                        else -> "默认创作模型"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Neutral100,
                    maxLines = 1,
                )
                val subtitle = when {
                    loading -> "正在同步服务端模型参数"
                    model != null -> listOfNotNull(
                        model.groupName,
                        "${model.fields.size} 个参数",
                    ).joinToString(" · ")
                    else -> "未获取到服务端模型，使用本地兼容参数"
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Neutral500,
                        maxLines = 1,
                    )
                }
            }
            val priceText = when {
                feePreviewLoading -> "价格刷新中"
                feePreviewError != null -> "价格待确认"
                cost > 0.0 -> "${formatCashAmount(cost)} CNY"
                else -> null
            }
            priceText?.let {
                Surface(
                    color = Primary300.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    border = BorderStroke(1.dp, Primary300.copy(alpha = 0.28f)),
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Primary300,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Neutral500,
            )
        }
    }
}

@Composable
private fun QuickParamChipRow(
    uiState: QuickCreateUiState,
    isImage: Boolean,
    onImageRatioChange: (ImageAspectRatio) -> Unit,
    onImageResChange: (ImageResolution) -> Unit,
    onImageQualityChange: (ImageQuality) -> Unit,
    onImageCountChange: (Int) -> Unit,
    onVideoRatioChange: (VideoAspectRatio) -> Unit,
    onVideoResChange: (VideoResolution) -> Unit,
    onVideoDurationChange: (VideoDuration) -> Unit,
    onToggleAudio: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSM),
    ) {
        if (isImage) {
            val config = uiState.imageConfig
            val ratios = ImageAspectRatio.entries.filter { it in config.model.supportedRatios }
            val resolutions = ImageResolution.entries.filter { it in config.model.supportedResolutions }
            val qualities = ImageQuality.entries.filter { it in config.model.supportedQualities }
            CompactParamChip(
                label = "比例",
                value = config.aspectRatio.displayName,
                icon = Icons.Default.Tune,
                onClick = { onImageRatioChange(nextQuickCreateValue(ratios, config.aspectRatio)) },
            )
            CompactParamChip(
                label = "尺寸",
                value = config.resolution.displayName,
                icon = Icons.Default.Tune,
                onClick = { onImageResChange(nextQuickCreateValue(resolutions, config.resolution)) },
            )
            CompactParamChip(
                label = "质量",
                value = config.quality.displayName,
                icon = Icons.Default.Tune,
                onClick = { onImageQualityChange(nextQuickCreateValue(qualities, config.quality)) },
            )
            CompactParamChip(
                label = "数量",
                value = config.count.toString(),
                icon = Icons.Default.Add,
                onClick = { onImageCountChange(nextQuickCreateValue(listOf(1, 2, 4), config.count)) },
            )
        } else {
            val config = uiState.videoConfig
            val ratios = VideoAspectRatio.entries.filter { it in config.model.supportedRatios }
            val resolutions = VideoResolution.entries.filter { it in config.model.supportedResolutions }
            val durations = VideoDuration.entries.filter { it in config.model.supportedDurations }
            CompactParamChip(
                label = "比例",
                value = config.aspectRatio.displayName,
                icon = Icons.Default.Tune,
                onClick = { onVideoRatioChange(nextQuickCreateValue(ratios, config.aspectRatio)) },
            )
            CompactParamChip(
                label = "尺寸",
                value = config.resolution.displayName,
                icon = Icons.Default.Tune,
                onClick = { onVideoResChange(nextQuickCreateValue(resolutions, config.resolution)) },
            )
            CompactParamChip(
                label = "时长",
                value = config.duration.displayName,
                icon = Icons.Default.Tune,
                onClick = { onVideoDurationChange(nextQuickCreateValue(durations, config.duration)) },
            )
            CompactParamChip(
                label = "音频",
                value = if (config.generateAudio) "开" else "关",
                icon = Icons.Default.MusicNote,
                highlighted = config.generateAudio,
                onClick = onToggleAudio,
            )
        }
    }
    }

@Composable
private fun CompactParamChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (highlighted) Primary300.copy(alpha = 0.12f) else DarkSurfaceVariant,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        border = BorderStroke(1.dp, if (highlighted) Primary300.copy(alpha = 0.42f) else DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = if (highlighted) Primary300 else Neutral500,
            )
            Text(
                "$label $value",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (highlighted) Primary300 else Neutral300,
                maxLines = 1,
            )
        }
    }
}

private fun <T> nextQuickCreateValue(options: List<T>, current: T): T {
    if (options.isEmpty()) return current
    val currentIndex = options.indexOf(current)
    return options[(if (currentIndex >= 0) currentIndex + 1 else 0) % options.size]
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
    feePreviewLoading: Boolean,
    feePreviewError: String?,
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
                val sendLabel = quickCreateSendButtonLabel(
                    cost = cost,
                    feePreviewLoading = feePreviewLoading,
                    feePreviewError = feePreviewError,
                )
                if (sendLabel.isNotBlank()) {
                    Text(
                        sendLabel,
                        fontSize = if (cost > 0 || feePreviewLoading || feePreviewError != null) 12.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) Color.White else Neutral500,
                        maxLines = 1,
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
                            onHistoryItemSelected = {},
                            onLoadMoreHistory = {},
                            onCancelHistoryTask = {},
                            onProjectSelected = {},
                            onClearSelectedProject = {},
                            onLoadMoreProjects = {},
                            onToggleProjectPin = {},
                            onCreateProject = {},
                            onRenameProject = { _, _ -> },
                            onDeleteProject = {},
                            onShowProjectDetail = {},
                        )
                        QuickCreateMode.INSPIRATION -> InspirationArea(
                            uiState = uiState,
                            onApplyTemplate = {},
                            onLoadMoreTemplates = {},
                        )
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
            BottomPromptPanel(
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
