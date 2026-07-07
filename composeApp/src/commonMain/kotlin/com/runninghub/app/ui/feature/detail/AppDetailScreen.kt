package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.platform.PermissionController
import com.runninghub.app.platform.SystemBackHandler
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.component.PermissionBottomSheet
import org.koin.compose.koinInject
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.TaskProgressIndicator
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.feature.creator.CreatorProfileScreen
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.feature.detail.presentation.AppDetailInputControl
import com.runninghub.feature.detail.presentation.AppDetailInputFieldUiModel
import com.runninghub.feature.detail.presentation.AppDetailInputRowUiModel
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.appDetailParamsLayout
import com.runninghub.feature.detail.presentation.inputRows
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_back_content_description
import runninghub.composeapp.generated.resources.app_detail_default_app_name
import runninghub.composeapp.generated.resources.app_detail_output_section_title
import runninghub.composeapp.generated.resources.app_detail_parameter_count_format
import runninghub.composeapp.generated.resources.app_detail_parameters_section_title

/**
 * App 详情页的 Voyager 入口与主内容装配，负责把 Presentation 状态映射为详情页的渲染结构。
 */

/* ═══════════════════════════════════════════════════
   Screen entry point
   ═══════════════════════════════════════════════════ */

data class AppDetailScreen(val appId: String) : Screen {

    override val key: ScreenKey get() = "AppDetail_$appId"

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<AppDetailScreenModel>()
        val currentScreenModel by rememberUpdatedState(screenModel)
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // 文件选择流程只需要权限领域边界，避免详情页直接依赖 data/local 存储类型。
        val permissionStateStore: PermissionStateStore = koinInject()
        val controller: PermissionController = rememberPermissionController(permissionStateStore)

        var pendingPermission by remember { mutableStateOf<Permission?>(null) }

        LaunchedEffect(appId) { currentScreenModel.loadDetail(appId) }

        LaunchedEffect(uiState.pendingMediaPick) {
            uiState.pendingMediaPick?.let { pending ->
                val permission = pending.mediaType.permission()
                controller.pickMedia(
                    mediaPermission = permission,
                    mediaType = pending.mediaType.toComponentMediaType(),
                    onSuccess = { uri -> currentScreenModel.onMediaUriReceived(uri) },
                    onPermissionDenied = {
                        currentScreenModel.clearPendingMediaPick()
                        // Android 图片/视频优先使用系统相册，不需要运行时媒体库权限；
                        // 用户取消系统选择器时只清理挂起请求，避免误弹授权说明。音频仍需要权限兜底。
                        if (permission == Permission.MediaAudio) {
                            pendingPermission = permission
                        }
                    },
                    onPickerCancelled = {
                        currentScreenModel.clearPendingMediaPick()
                    },
                    onPermissionPermanentlyDenied = {
                        currentScreenModel.clearPendingMediaPick()
                        pendingPermission = permission
                    },
                )
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
                        onPermanentlyDenied = { controller.openPermissionSettings(activePermission) },
                    )
                },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RhTheme.colors.backgroundPrimary)
        ) {
            val detailError = uiState.error
            when {
                uiState.isLoading -> LoadingIndicator()
                detailError != null -> ErrorState(
                    message = appDetailErrorMessage(detailError),
                    onRetry = { currentScreenModel.loadDetail(appId) }
                )
                uiState.detail != null -> DetailContent(
                    uiState = uiState,
                    onBack = { navigator.pop() },
                    onAuthorClick = { userId -> navigator.push(CreatorProfileScreen(userId)) },
                    onInputChanged = currentScreenModel::updateInputValue,
                    onRunTask = currentScreenModel::runTask,
                    onResetTask = currentScreenModel::resetTask,
                    onPickMedia = { nodeId, fieldName, mediaType ->
                        currentScreenModel.setPendingMediaPick(nodeId, fieldName, mediaType)
                    },
                    onRemoveFile = currentScreenModel::removeLocalFile
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Main content
   ═══════════════════════════════════════════════════ */

@Composable
internal fun DetailContent(
    uiState: AppDetailUiState,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onInputChanged: (String, String, String) -> Unit,
    onRunTask: () -> Unit,
    onResetTask: () -> Unit,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    val detail = uiState.detail ?: return
    val windowInfo = LocalRhWindowInfo.current
    val taskError = uiState.taskError?.let { appDetailErrorMessage(it) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val toolbarFadeStartPx = remember(density) { with(density) { 112.dp.toPx() } }
    val toolbarFadeRangePx = remember(density) { with(density) { 88.dp.toPx() } }
    val toolbarCollapseProgress by remember(toolbarFadeStartPx, toolbarFadeRangePx) {
        derivedStateOf {
            detailToolbarCollapseProgress(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
                fadeStartPx = toolbarFadeStartPx,
                fadeRangePx = toolbarFadeRangePx,
            )
        }
    }
    // 长下拉字段的底部弹层选择目标；非空时弹出 AppDetailOptionPickerSheet。
    var pickerField by remember { mutableStateOf<AppDetailInputFieldUiModel?>(null) }

    // 弹层打开时拦截系统返回，仅关闭选择弹层而不是 pop 整个详情页。
    SystemBackHandler(enabled = pickerField != null) {
        pickerField = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.backgroundPrimary),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = windowInfo.detailContentMaxWidth),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = AppDetailContentBottomPadding),
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "about_detail") {
                    AppDetailHero(
                        detail = detail,
                        onBack = onBack,
                        onAuthorClick = { detail.owner?.id?.let(onAuthorClick) },
                        showBackButton = false,
                    )
                }

                // 任务提交后保留阶段进度，用户能区分“已提交”和“正在等待结果”的状态。
                if (uiState.isRunningTask || uiState.taskStep != AppDetailTaskStep.IDLE) {
                    item(key = "progress") {
                        TaskProgressIndicator(
                            currentStep = uiState.taskStep.toComponentTaskStep(),
                            elapsedSeconds = uiState.taskElapsedSeconds,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // 任务失败属于生成流程结果，需要在输入区上方稳定展示，避免被底部操作栏遮挡。
                if (taskError != null) {
                    item(key = "task_error") {
                        TaskErrorCard(
                            error = taskError,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // 生成结果按服务端返回顺序展示；该顺序可能包含后端输出节点的业务顺序。
                if (uiState.taskOutputs.isNotEmpty()) {
                    item(key = "output_header") {
                        SectionHeader(stringResource(Res.string.app_detail_output_section_title))
                    }
                    items(
                        uiState.taskOutputs.filter { !it.fileUrl.isNullOrBlank() },
                        key = { it.fileUrl ?: it.hashCode().toString() }
                    ) { output ->
                        TaskOutputCard(
                            output = output,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // 参数区优先于长简介展示，避免短详情页的单个媒体上传入口被底部区域挤出首屏。
                if (detail.inputNodes.isNotEmpty()) {
                    item(key = "input_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            SectionHeader(stringResource(Res.string.app_detail_parameters_section_title))
                            Text(
                                text = stringResource(
                                    Res.string.app_detail_parameter_count_format,
                                    detail.inputNodes.size,
                                ),
                                color = RhTheme.colors.textTertiary,
                                style = RhTypography.caption,
                            )
                        }
                    }
                    item(key = "input_section") {
                        val paramsLayout = remember(uiState.detail, uiState.inputValues) {
                            appDetailParamsLayout(uiState.inputRows())
                        }
                        AppDetailParamsSection(
                            layout = paramsLayout,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            renderRow = { row ->
                                when (row) {
                                    is AppDetailInputRowUiModel.ImageUploadGroup -> MultiImageUploadRow(
                                        fields = row.fields,
                                        uiState = uiState,
                                        onPickMedia = onPickMedia,
                                        onRemoveFile = onRemoveFile
                                    )
                                    is AppDetailInputRowUiModel.Single -> RenderInputNodeField(
                                        field = row.field,
                                        uiState = uiState,
                                        onInputChanged = onInputChanged,
                                        onPickMedia = onPickMedia,
                                        onRemoveFile = onRemoveFile,
                                        onOpenPicker = { field -> pickerField = field }
                                    )
                                }
                            },
                            onResetGroup = { group ->
                                group.rows.forEach { row ->
                                    when (row) {
                                        is AppDetailInputRowUiModel.Single -> onInputChanged(
                                            row.field.nodeId,
                                            row.field.fieldName,
                                            row.field.defaultValue,
                                        )
                                        is AppDetailInputRowUiModel.ImageUploadGroup ->
                                            row.fields.forEach { field ->
                                                onInputChanged(field.nodeId, field.fieldName, field.defaultValue)
                                            }
                                    }
                                }
                            },
                        )
                    }
                }

                detail.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item(key = "description_detail") {
                        DescriptionSection(
                            description = description,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                item(key = "run_bar") {
                    RunTaskBottomBar(
                        isRunning = uiState.isRunningTask,
                        taskStep = uiState.taskStep.toComponentTaskStep(),
                        hasResult = uiState.taskOutputs.isNotEmpty() || taskError != null,
                        onRun = onRunTask,
                        onReset = onResetTask,
                    )
                }
            }

            // Compose 中模拟 CoordinatorLayout/CollapsingToolbar：内容滚动，Toolbar 固定并随折叠进度显示背景和标题。
            DetailCollapsingTopBar(
                title = detail.name ?: stringResource(Res.string.app_detail_default_app_name),
                collapseProgress = toolbarCollapseProgress,
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        // 长下拉字段的底部弹层选择器；选择后走既有 onInputChanged 并关闭。
        pickerField?.let { field ->
            AppDetailOptionPickerSheet(
                title = field.title,
                options = (field.control as? AppDetailInputControl.Dropdown)?.options.orEmpty(),
                selected = field.currentValue,
                onSelect = { option ->
                    onInputChanged(field.nodeId, field.fieldName, option)
                    pickerField = null
                },
                onDismiss = { pickerField = null },
            )
        }
    }
}

@Composable
private fun DetailCollapsingTopBar(
    title: String,
    collapseProgress: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RhTheme.colors
    val progress = collapseProgress.coerceIn(0f, 1f)
    val expandedButtonAlpha = 0.42f * (1f - progress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundPrimary.copy(alpha = progress))
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(56.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = expandedButtonAlpha))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.app_detail_back_content_description),
                    tint = if (progress < 0.55f) Color.White else colors.textPrimary,
                )
            }

            Text(
                text = title.ifBlank { stringResource(Res.string.app_detail_default_app_name) },
                color = colors.textPrimary.copy(alpha = progress),
                style = RhTypography.cardTitle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 12.dp),
            )
        }
    }
}

private fun detailToolbarCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    fadeStartPx: Float,
    fadeRangePx: Float,
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    // 先让 Hero 保持沉浸展示，滚过视觉安全距离后再让 pinned toolbar 渐显，贴近 CollapsingToolbar 的 scrim 触发节奏。
    return ((firstVisibleItemScrollOffset - fadeStartPx) / fadeRangePx).coerceIn(0f, 1f)
}

/* ═══════════════════════════════════════════════════
   Section header
   ═══════════════════════════════════════════════════ */

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = RhTheme.colors.textPrimary,
        style = RhTypography.cardTitle,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
