package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewAppDetail
import com.runninghub.app.ui.adaptive.previewTaskOutputs
import com.runninghub.app.ui.feature.creator.CreatorProfileScreen
import com.runninghub.app.ui.theme.DarkBackground
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.ErrorDark
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
import com.runninghub.core.model.TaskOutput
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.feature.detail.presentation.AppDetailMediaType
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.AppDetailUploadingState
import com.runninghub.feature.detail.presentation.appDetailInputKey
import org.jetbrains.compose.ui.tooling.preview.Preview

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
                        pendingPermission = permission
                        currentScreenModel.clearPendingMediaPick()
                    },
                )
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
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
private fun DetailContent(
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
    val taskError = uiState.taskError

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
                item(key = "hero") {
                    AppDetailHero(
                        detail = detail,
                        onBack = onBack,
                        onAuthorClick = { detail.owner?.id?.let(onAuthorClick) }
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
                        SectionHeader("生成结果")
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

                // 参数较多时默认折叠，减少详情内容和生成结果之间的滚动成本。
                if (detail.inputNodes.isNotEmpty()) {
                    item(key = "input_header") {
                        SectionHeader("配置参数", modifier = Modifier.padding(top = 16.dp))
                    }
                    item(key = "input_section") {
                        CollapsibleSection(
                            title = "${detail.inputNodes.size} 个参数",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            initiallyExpanded = detail.inputNodes.size <= 5
                        ) {
                            InputNodesContent(
                                inputNodes = detail.inputNodes,
                                uiState = uiState,
                                onInputChanged = onInputChanged,
                                onPickMedia = onPickMedia,
                                onRemoveFile = onRemoveFile
                            )
                        }
                    }
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
private fun AppDetailHero(
    detail: AppDetail,
    onBack: () -> Unit,
    onAuthorClick: () -> Unit
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
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = detail.name ?: "未命名应用",
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
                useCount = detail.statisticsInfo?.useCount ?: "0",
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

            if (!detail.description.isNullOrBlank()) {
                DescriptionSection(
                    description = detail.description!!,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White
            )
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
   App info section (title + tags)
   ═══════════════════════════════════════════════════ */

@Composable
private fun AppInfoSection(detail: AppDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = detail.name ?: "未命名应用",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        if (detail.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                detail.tags.chunked(3).forEach { rowTags ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .widthIn(max = 120.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tag.name,
                                    color = Primary300,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
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
    useCount: String,
    successRate: String?,
    avgSeconds: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(value = useCount, label = "使用次数")
        StatDivider()
        StatItem(value = successRate?.let { "${it}%" } ?: "--", label = "成功率")
        StatDivider()
        StatItem(value = avgSeconds?.let { "${it}s" } ?: "--", label = "平均用时")
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
                    text = "${owner.fansCount} 粉丝",
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(16.dp)
    ) {
        Text(
            text = "简介",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description.replace(Regex("<[^>]*>"), "").trim(),
            color = Neutral400,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

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
private fun InputNodesContent(
    inputNodes: List<InputNode>,
    uiState: AppDetailUiState,
    onInputChanged: (String, String, String) -> Unit,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    Column {
        var index = 0
        while (index < inputNodes.size) {
            val node = inputNodes[index]
            val mediaType = node.uploadMediaType()
            if (mediaType == AppDetailMediaType.IMAGE) {
                val imageNodes = mutableListOf<InputNode>()
                var cursor = index
                while (cursor < inputNodes.size && inputNodes[cursor].uploadMediaType() == AppDetailMediaType.IMAGE) {
                    imageNodes += inputNodes[cursor]
                    cursor++
                }

                if (imageNodes.size > 1) {
                    MultiImageUploadRow(
                        nodes = imageNodes,
                        uiState = uiState,
                        onPickMedia = onPickMedia,
                        onRemoveFile = onRemoveFile
                    )
                    index = cursor
                } else {
                    RenderInputNodeField(
                        node = node,
                        uiState = uiState,
                        onInputChanged = onInputChanged,
                        onPickMedia = onPickMedia,
                        onRemoveFile = onRemoveFile
                    )
                    index++
                }
            } else {
                RenderInputNodeField(
                    node = node,
                    uiState = uiState,
                    onInputChanged = onInputChanged,
                    onPickMedia = onPickMedia,
                    onRemoveFile = onRemoveFile
                )
                index++
            }

            if (index < inputNodes.size) {
                InputDivider()
            }
        }
    }
}

@Composable
private fun RenderInputNodeField(
    node: InputNode,
    uiState: AppDetailUiState,
    onInputChanged: (String, String, String) -> Unit,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    val nodeKey = appDetailInputKey(node)
    val currentValue = uiState.inputValues[nodeKey] ?: node.fieldValue ?: ""
    InputNodeField(
        node = node,
        currentValue = currentValue,
        localUri = uiState.localUris[node.nodeId],
        uploadState = uiState.uploadingNodes[node.nodeId],
        onValueChanged = { onInputChanged(node.nodeId, node.fieldName, it) },
        onPickFile = { onPickMedia(node.nodeId, node.fieldName, node.uploadMediaType() ?: AppDetailMediaType.IMAGE) },
        onRemoveFile = { onRemoveFile(node.nodeId, node.fieldName) }
    )
}

@Composable
private fun MultiImageUploadRow(
    nodes: List<InputNode>,
    uiState: AppDetailUiState,
    onPickMedia: (String, String, AppDetailMediaType) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "上传图片",
            color = Neutral400,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(nodes, key = { it.nodeId + it.fieldName }) { node ->
                val nodeKey = appDetailInputKey(node)
                val currentValue = uiState.inputValues[nodeKey] ?: node.fieldValue ?: ""
                val uploadState = uiState.uploadingNodes[node.nodeId]
                Column(modifier = Modifier.width(112.dp)) {
                    Text(
                        text = node.description ?: node.fieldName,
                        color = Neutral400,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ImageUploadButton(
                        localUri = uiState.localUris[node.nodeId] ?: currentValue.takeIf { it.startsWith("http") },
                        remoteUrl = currentValue.takeIf { it.startsWith("http") },
                        fileName = uiState.localUris[node.nodeId]?.substringAfterLast("/")?.substringAfterLast("%2F")
                            ?: currentValue.takeIf { it.isNotBlank() && !it.startsWith("http") },
                        isUploading = uploadState != null && !uploadState.isError,
                        uploadProgress = uploadState?.progress ?: 0f,
                        isError = uploadState?.isError == true,
                        mediaType = MediaType.IMAGE,
                        square = true,
                        onPickFile = { onPickMedia(node.nodeId, node.fieldName, AppDetailMediaType.IMAGE) },
                        onRemoveFile = { onRemoveFile(node.nodeId, node.fieldName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InputDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(DarkSurfaceVariant)
    )
}

private fun InputNode.uploadMediaType(): AppDetailMediaType? {
    val type = fieldType.uppercase()

    val label = listOfNotNull(fieldName, nodeName, description, descriptionEn)
        .joinToString(" ")
        .lowercase()
    return when {
        type == "IMAGE" || type == "IMAGE_UPLOAD" -> AppDetailMediaType.IMAGE
        label.contains("上传视频") || label.contains("上传录像") ||
            label.contains("upload video") || label.contains("video upload") ||
            label.contains("video file") -> AppDetailMediaType.VIDEO
        label.contains("上传音频") || label.contains("上传音乐") ||
            label.contains("upload audio") || label.contains("audio upload") ||
            label.contains("audio file") -> AppDetailMediaType.AUDIO
        label.contains("上传图片") || label.contains("上传图像") ||
            label.contains("upload image") || label.contains("image upload") -> AppDetailMediaType.IMAGE
        else -> null
    }
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
    node: InputNode,
    currentValue: String,
    localUri: String?,
    uploadState: AppDetailUploadingState?,
    onValueChanged: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit
) {
    val options = remember(node.fieldData) { node.getOptions() }
    val isUploading = uploadState != null && !uploadState.isError
    val uploadProgress = uploadState?.progress ?: 0f
    val isUploadError = uploadState?.isError == true
    val mediaType = node.uploadMediaType()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = node.description ?: node.fieldName,
            color = Neutral400,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (mediaType != null) {
            ImageUploadButton(
                localUri = localUri ?: currentValue.takeIf { it.startsWith("http") },
                remoteUrl = currentValue.takeIf { it.startsWith("http") },
                fileName = localUri?.substringAfterLast("/")?.substringAfterLast("%2F")
                    ?: currentValue.takeIf { it.isNotBlank() && !it.startsWith("http") },
                isUploading = isUploading,
                uploadProgress = uploadProgress,
                isError = isUploadError,
                mediaType = mediaType.toComponentMediaType(),
                onPickFile = onPickFile,
                onRemoveFile = onRemoveFile
            )
            return@Column
        }

        when (node.fieldType.uppercase()) {
            "LIST" -> {
                ListDropdown(
                    options = options,
                    currentValue = currentValue,
                    onValueChanged = onValueChanged
                )
            }

            "IMAGE" -> {
                ImageUploadButton(
                    localUri = localUri ?: currentValue.takeIf { it.startsWith("http") },
                    remoteUrl = currentValue.takeIf { it.startsWith("http") },
                    fileName = localUri?.substringAfterLast("/")?.substringAfterLast("%2F"),
                    isUploading = isUploading,
                    uploadProgress = uploadProgress,
                    isError = isUploadError,
                    onPickFile = onPickFile,
                    onRemoveFile = onRemoveFile
                )
            }

            "BOOLEAN" -> {
                BooleanSwitch(
                    currentValue = currentValue,
                    onValueChanged = onValueChanged
                )
            }

            "SWITCH" -> {
                SegmentedSelector(
                    options = options.ifEmpty { listOf("选项A", "选项B") },
                    currentValue = currentValue,
                    onValueChanged = onValueChanged
                )
            }

            "INT" -> {
                DarkTextField(
                    value = currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal.toIntOrNull() != null) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = "请输入整数",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )
            }

            "FLOAT" -> {
                DarkTextField(
                    value = currentValue,
                    onValueChange = { newVal ->
                        if (newVal.isEmpty() || newVal == "-" || newVal == "." ||
                            newVal.toDoubleOrNull() != null || newVal.endsWith(".")
                        ) {
                            onValueChanged(newVal)
                        }
                    },
                    placeholder = "请输入数值",
                    keyboardType = KeyboardType.Decimal,
                    singleLine = true
                )
            }

            "STRING" -> {
                val isMultiline = node.fieldData?.contains("multiline", ignoreCase = true) == true
                if (options.isNotEmpty()) {
                    ListDropdown(
                        options = options,
                        currentValue = currentValue,
                        onValueChanged = onValueChanged
                    )
                } else {
                    DarkTextField(
                        value = currentValue,
                        onValueChange = onValueChanged,
                        placeholder = "请输入内容",
                        singleLine = !isMultiline,
                        minLines = if (isMultiline) 4 else 1
                    )
                }
            }

            else -> {
                if (options.isNotEmpty()) {
                    ListDropdown(
                        options = options,
                        currentValue = currentValue,
                        onValueChanged = onValueChanged
                    )
                } else {
                    DarkTextField(
                        value = currentValue,
                        onValueChange = onValueChanged,
                        placeholder = "请输入内容",
                        singleLine = true
                    )
                }
            }
        }
    }
}

/* ── Dark-styled text field ── */

@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = placeholder, color = Neutral400.copy(alpha = 0.5f), fontSize = 14.sp)
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Primary300,
            focusedBorderColor = Primary300,
            unfocusedBorderColor = DarkSurfaceVariant,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/* ── LIST dropdown ── */

@Composable
private fun ListDropdown(
    options: List<String>,
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text("请选择", color = Neutral400.copy(alpha = 0.5f), fontSize = 14.sp)
            },
            trailingIcon = {
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Neutral400,
                    fontSize = 12.sp,
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Primary300,
                unfocusedBorderColor = DarkSurfaceVariant,
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedTrailingIconColor = Neutral400,
                unfocusedTrailingIconColor = Neutral400
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = DarkSurface
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == currentValue) Primary300 else Color.White,
                            fontSize = 14.sp
                        )
                    },
                    onClick = {
                        onValueChanged(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ── BOOLEAN switch ── */

@Composable
private fun BooleanSwitch(
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    val checked = currentValue.equals("true", ignoreCase = true)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = if (checked) "开启" else "关闭",
            color = if (checked) SuccessDark else Neutral400,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { onValueChanged(it.toString()) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary500,
                uncheckedThumbColor = Neutral400,
                uncheckedTrackColor = DarkSurface,
                uncheckedBorderColor = DarkSurfaceVariant
            )
        )
    }
}

/* ── SWITCH segmented selector ── */

@Composable
private fun SegmentedSelector(
    options: List<String>,
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEach { option ->
            val selected = option == currentValue
            val bgColor by animateColorAsState(
                targetValue = if (selected) Primary500 else Color.Transparent,
                animationSpec = tween(200)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onValueChanged(option) }
            ) {
                Text(
                    text = option,
                    color = if (selected) Color.White else Neutral400,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Task error card
   ═══════════════════════════════════════════════════ */

@Composable
private fun TaskErrorCard(error: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorDark.copy(alpha = 0.12f))
            .border(1.dp, ErrorDark.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = error,
            color = ErrorDark,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

/* ═══════════════════════════════════════════════════
   Task output card
   ═══════════════════════════════════════════════════ */

@Composable
private fun TaskOutputCard(output: TaskOutput, modifier: Modifier = Modifier) {
    val url = output.fileUrl.orEmpty()
    val isImage = output.fileType?.startsWith("image") == true ||
        url.endsWith(".png") || url.endsWith(".jpg") ||
        url.endsWith(".jpeg") || url.endsWith(".webp")
    val isVideo = output.fileType?.startsWith("video") == true ||
        url.endsWith(".mp4") || url.endsWith(".mov") || url.endsWith(".webm")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
    ) {
        if (isImage && url.isNotBlank()) {
            SmartAsyncImage(
                imageUrl = url,
                contentDescription = output.fileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.FillWidth
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isVideo && url.isNotBlank()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Primary300,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "视频文件",
                        color = Neutral400,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = output.fileName ?: url.substringAfterLast("/"),
            color = Neutral400,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

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
                            TaskStep.SUBMITTING -> "提交中..."
                            TaskStep.QUEUEING -> "排队中..."
                            TaskStep.RUNNING -> "生成中..."
                            TaskStep.COMPLETING -> "完成中..."
                            else -> "运行中..."
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
                            text = "重新运行",
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
                            text = "立即运行",
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

private fun detailPreviewState(): AppDetailUiState {
    val detail = previewAppDetail()
    return AppDetailUiState(
        isLoading = false,
        detail = detail,
        inputValues = detail.inputNodes.associate { node ->
            appDetailInputKey(node) to (node.fieldValue ?: "")
        },
        taskStep = AppDetailTaskStep.SUCCESS,
        taskOutputs = previewTaskOutputs(),
    )
}

@Composable
private fun DetailAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        DetailContent(
            uiState = detailPreviewState(),
            onBack = {},
            onAuthorClick = {},
            onInputChanged = { _, _, _ -> },
            onRunTask = {},
            onResetTask = {},
            onPickMedia = { _, _, _ -> },
            onRemoveFile = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun DetailPhone320Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun DetailPhone360Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun DetailPhone430Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun DetailMediumPreview() {
    DetailAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun DetailExpandedPreview() {
    DetailAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun DetailLandscapePreview() {
    DetailAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun DetailFontScale13Preview() {
    DetailAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun DetailFontScale15Preview() {
    DetailAdaptivePreview(RhPreviewSpec.FontScale15)
}
