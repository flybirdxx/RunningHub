package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.runninghub.app.platform.PermissionController
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
import com.runninghub.feature.detail.presentation.creationEntry
import com.runninghub.feature.detail.presentation.inputRows
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
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
                        onPermanentlyDenied = { controller.openAppSettings() },
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
    // 长下拉字段的底部弹层选择目标；非空时弹出 AppDetailOptionPickerSheet。
    var pickerField by remember { mutableStateOf<AppDetailInputFieldUiModel?>(null) }

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
                contentPadding = PaddingValues(bottom = 168.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "creation_entry") {
                    uiState.creationEntry?.let { entry ->
                        AppDetailCreationEntry(
                            entry = entry,
                            detail = detail,
                            onBack = onBack,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }

                if (uiState.taskStep == AppDetailTaskStep.IDLE && !uiState.isRunningTask) {
                    item(key = "creation_spacing") {
                        Spacer(Modifier.height(4.dp))
                    }
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

                // 参数区紧跟创作入口，保证用户在详情首屏即可看到需要补齐的输入。
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

                item(key = "about_detail") {
                    AppDetailHero(
                        detail = detail,
                        onBack = onBack,
                        onAuthorClick = { detail.owner?.id?.let(onAuthorClick) },
                        showBackButton = false,
                    )
                }
            }

            RunTaskBottomBar(
                isRunning = uiState.isRunningTask,
                taskStep = uiState.taskStep.toComponentTaskStep(),
                hasResult = uiState.taskOutputs.isNotEmpty() || taskError != null,
                onRun = onRunTask,
                onReset = onResetTask,
                modifier = Modifier.align(Alignment.BottomCenter)
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
