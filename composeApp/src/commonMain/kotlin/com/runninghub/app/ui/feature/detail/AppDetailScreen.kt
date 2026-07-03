package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.runninghub.app.platform.PermissionController
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.ui.component.PermissionBottomSheet
import org.koin.compose.koinInject
import com.runninghub.app.ui.component.CollapsibleSection
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.ImageUploadButton
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.TaskProgressIndicator
import com.runninghub.app.ui.component.TaskStep
import com.runninghub.app.ui.adaptive.LocalRhWindowInfo
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.feature.creator.CreatorProfileScreen
import com.runninghub.app.ui.theme.DarkBackground
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Neutral500
import com.runninghub.app.ui.theme.Primary300
import com.runninghub.app.ui.theme.Primary500
import com.runninghub.app.ui.theme.SuccessDark
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import com.runninghub.core.model.InputNode
import com.runninghub.core.model.StatisticsInfo
import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.feature.detail.presentation.AppDetailErrorText
import com.runninghub.feature.detail.presentation.AppDetailCreationEntryUiModel
import com.runninghub.feature.detail.presentation.AppDetailInputControl
import com.runninghub.feature.detail.presentation.AppDetailInputFieldUiModel
import com.runninghub.feature.detail.presentation.AppDetailInputRowUiModel
import com.runninghub.feature.detail.presentation.AppDetailInputNodeValuePreview
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.AppDetailUploadingState
import com.runninghub.feature.detail.presentation.appDetailParamsLayout
import com.runninghub.feature.detail.presentation.creationEntry
import com.runninghub.feature.detail.presentation.inputRows
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_back_content_description
import runninghub.composeapp.generated.resources.app_detail_creation_cost_title
import runninghub.composeapp.generated.resources.app_detail_creation_cost_unknown
import runninghub.composeapp.generated.resources.app_detail_creation_input_media_provided
import runninghub.composeapp.generated.resources.app_detail_creation_input_missing
import runninghub.composeapp.generated.resources.app_detail_creation_description_title
import runninghub.composeapp.generated.resources.app_detail_creation_inputs_title
import runninghub.composeapp.generated.resources.app_detail_creation_technical_empty
import runninghub.composeapp.generated.resources.app_detail_creation_technical_item_format
import runninghub.composeapp.generated.resources.app_detail_creation_technical_title
import runninghub.composeapp.generated.resources.app_detail_default_app_name
import runninghub.composeapp.generated.resources.app_detail_description_title
import runninghub.composeapp.generated.resources.app_detail_error_load_failed
import runninghub.composeapp.generated.resources.app_detail_error_task_failed
import runninghub.composeapp.generated.resources.app_detail_error_task_submit_failed
import runninghub.composeapp.generated.resources.app_detail_error_task_timeout
import runninghub.composeapp.generated.resources.app_detail_fans_count_format
import runninghub.composeapp.generated.resources.app_detail_float_placeholder
import runninghub.composeapp.generated.resources.app_detail_int_placeholder
import runninghub.composeapp.generated.resources.app_detail_multi_image_upload_title
import runninghub.composeapp.generated.resources.app_detail_output_section_title
import runninghub.composeapp.generated.resources.app_detail_parameter_count_format
import runninghub.composeapp.generated.resources.app_detail_parameters_section_title
import runninghub.composeapp.generated.resources.app_detail_rerun_action
import runninghub.composeapp.generated.resources.app_detail_run_now_action
import runninghub.composeapp.generated.resources.app_detail_stat_average_duration
import runninghub.composeapp.generated.resources.app_detail_stat_success_rate
import runninghub.composeapp.generated.resources.app_detail_stat_use_count
import runninghub.composeapp.generated.resources.app_detail_status_completing
import runninghub.composeapp.generated.resources.app_detail_status_queueing
import runninghub.composeapp.generated.resources.app_detail_status_running
import runninghub.composeapp.generated.resources.app_detail_status_running_fallback
import runninghub.composeapp.generated.resources.app_detail_status_submitting
import runninghub.composeapp.generated.resources.app_detail_text_placeholder
import runninghub.composeapp.generated.resources.collapsible_section_collapse_content_description
import runninghub.composeapp.generated.resources.collapsible_section_expand_content_description

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
                .background(DarkBackground)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
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
                        SectionHeader(
                            stringResource(Res.string.app_detail_parameters_section_title),
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    item(key = "input_section") {
                        CollapsibleSection(
                            title = stringResource(
                                Res.string.app_detail_parameter_count_format,
                                detail.inputNodes.size,
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            initiallyExpanded = detail.inputNodes.size <= 5
                        ) {
                            val paramsLayout = appDetailParamsLayout(uiState.inputRows())
                            AppDetailParamsSection(
                                layout = paramsLayout,
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
                                            onRemoveFile = onRemoveFile
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
    }
}

/* ═══════════════════════════════════════════════════
   Top bar (back only)
   ═══════════════════════════════════════════════════ */

@Composable
private fun AppDetailCreationEntry(
    entry: AppDetailCreationEntryUiModel,
    detail: AppDetail,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.app_detail_back_content_description),
                    tint = Color.White,
                )
            }
            CompactDetailCover(detail = detail)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = entry.title.ifBlank { stringResource(Res.string.app_detail_default_app_name) },
                    color = Color.White,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.app_detail_creation_description_title),
                    color = Primary300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                entry.description?.let { description ->
                    Text(
                        text = description,
                        color = Neutral400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        CreationInfoRow(
            title = stringResource(Res.string.app_detail_creation_cost_title),
            value = entry.estimatedCost.amountLabel ?: stringResource(Res.string.app_detail_creation_cost_unknown),
        )

        if (entry.inputNodes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.app_detail_creation_inputs_title),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                entry.inputNodes.forEach { input ->
                    CreationInputRow(
                        title = input.title,
                        valuePreview = input.valuePreview,
                    )
                }
            }
        }

        CollapsibleSection(
            title = stringResource(Res.string.app_detail_creation_technical_title),
            initiallyExpanded = entry.technicalDetailsExpanded,
        ) {
            if (entry.technicalDetails.isEmpty()) {
                Text(
                    text = stringResource(Res.string.app_detail_creation_technical_empty),
                    color = Neutral400,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    entry.technicalDetails.forEach { detailItem ->
                        Text(
                            text = stringResource(
                                Res.string.app_detail_creation_technical_item_format,
                                detailItem.key,
                                detailItem.value,
                            ),
                            color = Neutral400,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDetailCover(detail: AppDetail) {
    val coverUrl = detail.covers.firstOrNull()?.url
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!coverUrl.isNullOrBlank()) {
            SmartAsyncImage(
                imageUrl = coverUrl,
                contentDescription = detail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Primary300,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun CreationInfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Neutral400,
            fontSize = 13.sp,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CreationInputRow(title: String, valuePreview: AppDetailInputNodeValuePreview) {
    val previewText = when (valuePreview) {
        AppDetailInputNodeValuePreview.MediaProvided ->
            stringResource(Res.string.app_detail_creation_input_media_provided)
        AppDetailInputNodeValuePreview.Missing ->
            stringResource(Res.string.app_detail_creation_input_missing)
        is AppDetailInputNodeValuePreview.Text -> valuePreview.value
    }
    val previewColor = when (valuePreview) {
        AppDetailInputNodeValuePreview.Missing -> Primary300
        else -> SuccessDark
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.74f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = previewText,
            color = previewColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.72f),
        )
    }
}

@Composable
private fun AppDetailHero(
    detail: AppDetail,
    onBack: () -> Unit,
    onAuthorClick: () -> Unit,
    showBackButton: Boolean = true,
) {
    val covers = detail.covers.mapNotNull { it.url }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (covers.isNotEmpty()) {
                CoverCarousel(
                    covers = covers,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .background(DarkSurfaceVariant)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.52f),
                                Color.Transparent,
                                DarkBackground.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.app_detail_back_content_description),
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = detail.name ?: stringResource(Res.string.app_detail_default_app_name),
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (detail.tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    detail.tags.take(6).chunked(3).forEach { rowTags ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .widthIn(max = 128.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Primary500.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = tag.name,
                                        color = Primary300,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            StatsCard(
                useCount = detail.statisticsInfo?.useCount,
                successRate = detail.runningSuccessRate,
                avgSeconds = detail.avgRunningSeconds,
                modifier = Modifier.fillMaxWidth()
            )

            AuthorRow(
                name = detail.getDisplayName(),
                avatar = detail.getDisplayAvatar(),
                owner = detail.owner,
                onClick = onAuthorClick,
                modifier = Modifier.fillMaxWidth()
            )

            val description = detail.description
            if (!description.isNullOrBlank()) {
                DescriptionSection(
                    description = description,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Cover carousel
   ═══════════════════════════════════════════════════ */

@Composable
private fun CoverCarousel(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    if (covers.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { covers.size })

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        ) { page ->
            AsyncImage(
                model = covers[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (covers.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(covers.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Stats card (standalone)
   ═══════════════════════════════════════════════════ */

@Composable
private fun StatsCard(
    useCount: String?,
    successRate: String?,
    avgSeconds: String?,
    modifier: Modifier = Modifier
) {
    val stats = buildList {
        useCount?.takeIf { it.isNotBlank() }?.let {
            add(it to Res.string.app_detail_stat_use_count)
        }
        successRate?.takeIf { it.isNotBlank() }?.let {
            add("${it}%" to Res.string.app_detail_stat_success_rate)
        }
        avgSeconds?.takeIf { it.isNotBlank() }?.let {
            add("${it}s" to Res.string.app_detail_stat_average_duration)
        }
    }
    if (stats.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEachIndexed { index, stat ->
            StatItem(value = stat.first, label = stringResource(stat.second))
            if (index < stats.lastIndex) {
                StatDivider()
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = Neutral400,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(DarkSurfaceVariant)
    )
}

/* ═══════════════════════════════════════════════════
   Author row (with follower info)
   ═══════════════════════════════════════════════════ */

@Composable
private fun AuthorRow(
    name: String,
    avatar: String?,
    owner: Author?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .then(if (owner?.id != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SmartAsyncImage(
            imageUrl = avatar,
            contentDescription = name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (owner != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.app_detail_fans_count_format, owner.fansCount),
                    color = Neutral400,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Neutral400,
            modifier = Modifier.size(18.dp)
        )
    }
}

/* ═══════════════════════════════════════════════════
   Description
   ═══════════════════════════════════════════════════ */

@Composable
private fun DescriptionSection(
    description: String,
    modifier: Modifier = Modifier
) {
    val presentation = remember(description) { appDetailDescriptionPresentation(description) }
    var expanded by remember(presentation.cleanedDescription) { mutableStateOf(false) }
    val collapsed = presentation.isCollapsible && !expanded
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "description_chevron_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = if (collapsed) presentation.collapsedBottomPadding else 16.dp
                )
        ) {
            Text(
                text = stringResource(Res.string.app_detail_description_title),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = presentation.cleanedDescription,
                color = Neutral400,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = if (collapsed) presentation.collapsedMaxLines else Int.MAX_VALUE,
                overflow = if (collapsed) TextOverflow.Ellipsis else TextOverflow.Clip
            )
        }

        if (
            collapsed &&
            presentation.collapsedDepthEffect == AppDetailDescriptionDepthEffect.BottomGradientFade
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(presentation.collapsedDepthFadeHeight)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkSurface.copy(alpha = 0.62f),
                                DarkSurface
                            )
                        )
                    )
            )
        }

        if (
            presentation.isCollapsible &&
            presentation.toggleAffordance == AppDetailDescriptionToggleAffordance.BottomBorderTriangle
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(presentation.collapsedDepthFadeHeight)
                    .clickable { expanded = !expanded }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) {
                        stringResource(Res.string.collapsible_section_collapse_content_description)
                    } else {
                        stringResource(Res.string.collapsible_section_expand_content_description)
                    },
                    tint = Neutral400,
                    modifier = Modifier
                        .size(presentation.toggleIconSize)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

/**
 * App 详情简介的展示策略。
 *
 * @property cleanedDescription 去掉 HTML 标签后的简介正文；空字符串表示没有可展示内容。
 * @property isCollapsible true 表示正文超过默认展示容量，需要提供展开/收起入口；false 表示直接完整展示。
 * @property collapsedMaxLines 折叠态最多展示的文本行数，单位为行；短简介不会使用该限制。
 * @property toggleAffordance 折叠入口的视觉位置；长简介固定使用底部边框三角，避免挤占标题行。
 * @property collapsedDepthEffect 折叠态底部的视觉过渡；用于弱化长文本被截断时的硬切边界。
 * @property toggleIconSize 折叠入口箭头图标的视觉尺寸；保持和全局折叠控件一致。
 * @property collapsedBottomPadding 折叠态正文容器的底部内边距；0 表示箭头直接覆盖在渐隐文字上，不额外制造独立底栏。
 * @property collapsedDepthFadeHeight 折叠态底部渐隐层高度；箭头覆盖在该渐隐层上方。
 */
internal data class AppDetailDescriptionPresentation(
    val cleanedDescription: String,
    val isCollapsible: Boolean,
    val collapsedMaxLines: Int,
    val toggleAffordance: AppDetailDescriptionToggleAffordance,
    val collapsedDepthEffect: AppDetailDescriptionDepthEffect,
    val toggleIconSize: Dp,
    val collapsedBottomPadding: Dp,
    val collapsedDepthFadeHeight: Dp
)

/**
 * App 详情简介折叠入口的视觉形式。
 */
internal enum class AppDetailDescriptionToggleAffordance {
    /**
     * 在简介卡片底部边框中央显示三角箭头，折叠态向下，展开态向上。
     */
    BottomBorderTriangle
}

/**
 * App 详情简介折叠态的底部视觉过渡。
 */
internal enum class AppDetailDescriptionDepthEffect {
    /**
     * 在折叠内容底部叠加从透明到卡片背景色的渐隐层，让底部三角区域呈现景深模糊感。
     */
    BottomGradientFade
}

/**
 * 计算 App 详情简介是否需要折叠。
 *
 * 服务端简介可能包含 HTML 标签或很长的规则说明，详情页默认只展示有限行数，避免说明文案挤占参数区和运行按钮。
 *
 * @param description 服务端返回的原始简介文本，可能包含 HTML 标签、换行和较长正文。
 * @return 供简介 Composable 使用的清洗文本和折叠配置。
 */
internal fun appDetailDescriptionPresentation(description: String): AppDetailDescriptionPresentation {
    val cleanedDescription = description.replace(Regex("<[^>]*>"), "").trim()
    val lineCount = cleanedDescription.lineSequence().count()
    val isCollapsible = cleanedDescription.length > APP_DETAIL_DESCRIPTION_COLLAPSE_THRESHOLD ||
        lineCount > APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES

    return AppDetailDescriptionPresentation(
        cleanedDescription = cleanedDescription,
        isCollapsible = isCollapsible,
        collapsedMaxLines = APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES,
        toggleAffordance = AppDetailDescriptionToggleAffordance.BottomBorderTriangle,
        collapsedDepthEffect = AppDetailDescriptionDepthEffect.BottomGradientFade,
        toggleIconSize = APP_DETAIL_DESCRIPTION_TOGGLE_ICON_SIZE,
        collapsedBottomPadding = APP_DETAIL_DESCRIPTION_COLLAPSED_BOTTOM_PADDING,
        collapsedDepthFadeHeight = APP_DETAIL_DESCRIPTION_DEPTH_FADE_HEIGHT
    )
}

private const val APP_DETAIL_DESCRIPTION_COLLAPSE_THRESHOLD = 220
private const val APP_DETAIL_DESCRIPTION_COLLAPSED_MAX_LINES = 6
private val APP_DETAIL_DESCRIPTION_TOGGLE_ICON_SIZE = 18.dp
private val APP_DETAIL_DESCRIPTION_COLLAPSED_BOTTOM_PADDING = 0.dp
private val APP_DETAIL_DESCRIPTION_DEPTH_FADE_HEIGHT = 24.dp

/* ═══════════════════════════════════════════════════
   Section header
   ═══════════════════════════════════════════════════ */

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/* ═══════════════════════════════════════════════════
   Input node fields
   ═══════════════════════════════════════════════════ */

@Composable
private fun RenderInputNodeField(
    field: AppDetailInputFieldUiModel,
    uiState: AppDetailUiState,
    onInputChanged: (String, String, String) -> Unit,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    InputNodeField(
        field = field,
        localUri = uiState.localUris[field.nodeId],
        uploadState = uiState.uploadingNodes[field.nodeId],
        onValueChanged = { onInputChanged(field.nodeId, field.fieldName, it) },
        onPickFile = {
            val mediaType = (field.control as? AppDetailInputControl.MediaUpload)?.mediaType ?: AppDetailMediaType.IMAGE
            onPickMedia(field.nodeId, field.fieldName, mediaType)
        },
        onRemoveFile = { onRemoveFile(field.nodeId, field.fieldName) }
    )
}

@Composable
private fun MultiImageUploadRow(
    fields: List<AppDetailInputFieldUiModel>,
    uiState: AppDetailUiState,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = stringResource(Res.string.app_detail_multi_image_upload_title),
                color = Neutral400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            RequiredFieldStar()
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(fields, key = { it.inputKey }) { field ->
                val uploadState = uiState.uploadingNodes[field.nodeId]
                ImageUploadButton(
                    localUri = uiState.localUris[field.nodeId] ?: field.currentValue.takeIf { it.startsWith("http") },
                    remoteUrl = field.currentValue.takeIf { it.startsWith("http") },
                    fileName = uiState.localUris[field.nodeId]?.substringAfterLast("/")?.substringAfterLast("%2F")
                        ?: field.currentValue.takeIf { it.isNotBlank() && !it.startsWith("http") },
                    isUploading = uploadState != null && !uploadState.isError,
                    uploadProgress = uploadState?.progress ?: 0f,
                    isError = uploadState?.isError == true,
                    mediaType = MediaType.IMAGE,
                    square = true,
                    onPickFile = { onPickMedia(field.nodeId, field.fieldName, AppDetailMediaType.IMAGE) },
                    onRemoveFile = { onRemoveFile(field.nodeId, field.fieldName) },
                    modifier = Modifier.width(112.dp)
                )
            }
        }
    }
}

/**
 * 必填字段星标：媒体上传字段缺省即无法运行任务，标题后追加醒目提示。
 */
@Composable
private fun RequiredFieldStar() {
    Text(
        text = " *",
        color = RhTheme.colors.statusFailed,
        style = RhTypography.caption,
    )
}

private fun AppDetailMediaType.permission(): Permission = when (this) {
    AppDetailMediaType.IMAGE -> Permission.MediaImages
    AppDetailMediaType.VIDEO -> Permission.MediaVideo
    AppDetailMediaType.AUDIO -> Permission.MediaAudio
}

private fun AppDetailMediaType.toComponentMediaType(): MediaType = when (this) {
    AppDetailMediaType.IMAGE -> MediaType.IMAGE
    AppDetailMediaType.VIDEO -> MediaType.VIDEO
    AppDetailMediaType.AUDIO -> MediaType.AUDIO
}

@Composable
private fun appDetailErrorMessage(error: AppDetailErrorText): String =
    when (error) {
        AppDetailErrorText.DetailLoadFailed -> stringResource(Res.string.app_detail_error_load_failed)
        AppDetailErrorText.TaskSubmitFailed -> stringResource(Res.string.app_detail_error_task_submit_failed)
        AppDetailErrorText.TaskFailed -> stringResource(Res.string.app_detail_error_task_failed)
        AppDetailErrorText.TaskTimeout -> stringResource(Res.string.app_detail_error_task_timeout)
    }

private fun AppDetailTaskStep.toComponentTaskStep(): TaskStep = when (this) {
    AppDetailTaskStep.IDLE -> TaskStep.IDLE
    AppDetailTaskStep.SUBMITTING -> TaskStep.SUBMITTING
    AppDetailTaskStep.QUEUEING -> TaskStep.QUEUEING
    AppDetailTaskStep.RUNNING -> TaskStep.RUNNING
    AppDetailTaskStep.COMPLETING -> TaskStep.COMPLETING
    AppDetailTaskStep.SUCCESS -> TaskStep.SUCCESS
    AppDetailTaskStep.FAILED -> TaskStep.FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputNodeField(
    field: AppDetailInputFieldUiModel,
    localUri: String?,
    uploadState: AppDetailUploadingState?,
    onValueChanged: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit
) {
    val isUploading = uploadState != null && !uploadState.isError
    val uploadProgress = uploadState?.progress ?: 0f
    val isUploadError = uploadState?.isError == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = field.title,
                color = Neutral400,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (field.control is AppDetailInputControl.MediaUpload) {
                RequiredFieldStar()
            }
        }

        when (val control = field.control) {
            is AppDetailInputControl.Dropdown -> {
                ListDropdown(
                    options = control.options,
                    currentValue = field.currentValue,
                    onValueChanged = onValueChanged
                )
            }
            is AppDetailInputControl.MediaUpload -> {
                ImageUploadButton(
                    localUri = localUri ?: field.currentValue.takeIf { it.startsWith("http") },
                    remoteUrl = field.currentValue.takeIf { it.startsWith("http") },
                    fileName = localUri?.substringAfterLast("/")?.substringAfterLast("%2F"),
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    isError = isUploadError,
                    mediaType = control.mediaType.toComponentMediaType(),
                    onPickFile = onPickFile,
                    onRemoveFile = onRemoveFile
                )
            }
            AppDetailInputControl.BooleanSwitch -> {
                BooleanSwitch(
                    currentValue = field.currentValue,
                    onValueChanged = onValueChanged
                )
            }
            is AppDetailInputControl.Segmented -> {
                SegmentedSelector(
                    options = control.options,
                    currentValue = field.currentValue,
                    onValueChanged = onValueChanged
                )
            }
            AppDetailInputControl.IntegerText -> {
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal.toIntOrNull() != null) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = stringResource(Res.string.app_detail_int_placeholder),
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
            }
            AppDetailInputControl.DecimalText -> {
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal == "." ||
                            newVal.toDoubleOrNull() != null || newVal.endsWith(".")
                        ) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = stringResource(Res.string.app_detail_float_placeholder),
                    keyboardType = KeyboardType.Decimal,
                    singleLine = true
                )
            }
            is AppDetailInputControl.Text -> {
                val lineLimits = appDetailTextFieldLineLimits(multiline = control.multiline)
                DarkTextField(
                    value = field.currentValue,
                    onValueChange = onValueChanged,
                    placeholder = stringResource(Res.string.app_detail_text_placeholder),
                    singleLine = !control.multiline,
                    minLines = lineLimits.minLines,
                    maxLines = lineLimits.maxLines
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Task error card
   ═══════════════════════════════════════════════════ */

/* ═══════════════════════════════════════════════════
   Bottom run button (fixed)
   ═══════════════════════════════════════════════════ */

@Composable
private fun RunTaskBottomBar(
    isRunning: Boolean,
    taskStep: TaskStep,
    hasResult: Boolean,
    onRun: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val windowInfo = LocalRhWindowInfo.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = windowInfo.detailContentMaxWidth)
            .background(DarkBackground.copy(alpha = 0.95f))
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val buttonColor = when {
            isRunning -> Primary500.copy(alpha = 0.6f)
            hasResult -> Neutral500
            else -> Primary500
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(buttonColor)
                .then(
                    if (isRunning) Modifier
                    else Modifier.clickable(onClick = if (hasResult) onReset else onRun)
                )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isRunning -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        val statusText = when (taskStep) {
                            TaskStep.SUBMITTING -> stringResource(Res.string.app_detail_status_submitting)
                            TaskStep.QUEUEING -> stringResource(Res.string.app_detail_status_queueing)
                            TaskStep.RUNNING -> stringResource(Res.string.app_detail_status_running)
                            TaskStep.COMPLETING -> stringResource(Res.string.app_detail_status_completing)
                            else -> stringResource(Res.string.app_detail_status_running_fallback)
                        }
                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    hasResult -> {
                        Text(
                            text = stringResource(Res.string.app_detail_rerun_action),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.app_detail_run_now_action),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

