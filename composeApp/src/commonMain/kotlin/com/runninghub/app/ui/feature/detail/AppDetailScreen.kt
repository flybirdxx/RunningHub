package com.runninghub.app.ui.feature.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.runninghub.app.ui.component.MediaType
import com.runninghub.app.platform.PermissionController
import com.runninghub.app.platform.rememberPermissionController
import com.runninghub.app.ui.component.PermissionBottomSheet
import com.runninghub.shared.data.local.PermissionDataStore
import org.koin.compose.koinInject
import com.runninghub.app.ui.component.CollapsibleSection
import com.runninghub.app.ui.component.ErrorState
import com.runninghub.app.ui.component.ImageUploadButton
import com.runninghub.app.ui.component.LoadingIndicator
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.TaskProgressIndicator
import com.runninghub.app.ui.component.TaskStep
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
import com.runninghub.shared.domain.model.Author
import com.runninghub.shared.domain.model.InputNode
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.StatisticsInfo
import com.runninghub.shared.domain.model.TaskOutput

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

        val activityContext = LocalContext.current
        val dataStore: PermissionDataStore = koinInject()
        val controller: PermissionController = rememberPermissionController(dataStore, activityContext)

        var pendingPermission by remember { mutableStateOf<Permission?>(null) }

        LaunchedEffect(appId) { currentScreenModel.loadDetail(appId) }

        LaunchedEffect(uiState.pendingImagePick) {
            uiState.pendingImagePick?.let { _ ->
                controller.pickMedia(
                    mediaPermission = Permission.MediaImages,
                    mediaType = MediaType.IMAGE,
                    onSuccess = { uri -> currentScreenModel.onImageUriReceived(uri) },
                    onPermissionDenied = {
                        pendingPermission = Permission.MediaImages
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
                    onPickImage = { nodeId, fieldName ->
                        currentScreenModel.setPendingImagePick(nodeId, fieldName)
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
    onPickImage: (String, String) -> Unit,
    onRemoveFile: (String, String) -> Unit
) {
    val detail = uiState.detail ?: return

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. Top bar (no app name, only back + actions) ──
            item(key = "topbar") {
                TopBar(onBack = onBack)
            }

            // ── 2. Cover carousel ──
            if (detail.covers.isNotEmpty()) {
                item(key = "covers") {
                    CoverCarousel(
                        covers = detail.covers.mapNotNull { it.url },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── 3. App info: title + tags ──
            item(key = "info") {
                AppInfoSection(detail = detail)
            }

            // ── 4. Stats card (standalone) ──
            item(key = "stats") {
                StatsCard(
                    useCount = detail.statisticsInfo?.useCount ?: "0",
                    successRate = detail.runningSuccessRate,
                    avgSeconds = detail.avgRunningSeconds,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── 5. Author row ──
            item(key = "author") {
                AuthorRow(
                    name = detail.getDisplayName(),
                    avatar = detail.getDisplayAvatar(),
                    owner = detail.owner,
                    onClick = { detail.owner?.id?.let(onAuthorClick) }
                )
            }

            // ── 6. Description ──
            if (!detail.description.isNullOrBlank()) {
                item(key = "description") {
                    DescriptionSection(detail.description!!)
                }
            }

            // ── 7. Task progress indicator ──
            if (uiState.isRunningTask || uiState.taskStep != TaskStep.IDLE) {
                item(key = "progress") {
                    TaskProgressIndicator(
                        currentStep = uiState.taskStep,
                        elapsedSeconds = uiState.taskElapsedSeconds,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── 8. Task error ──
            if (uiState.taskError != null) {
                item(key = "task_error") {
                    TaskErrorCard(
                        error = uiState.taskError,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── 9. Task outputs ──
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

            // ── 10. Input nodes (collapsible) ──
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
                        Column {
                            detail.inputNodes.forEachIndexed { index, node ->
                                val nodeKey = AppDetailScreenModel.inputKey(node)
                                val currentValue = uiState.inputValues[nodeKey] ?: node.fieldValue ?: ""
                                InputNodeField(
                                    node = node,
                                    currentValue = currentValue,
                                    localUri = uiState.localUris[node.nodeId],
                                    uploadState = uiState.uploadingNodes[node.nodeId],
                                    onValueChanged = { onInputChanged(node.nodeId, node.fieldName, it) },
                                    onPickFile = { onPickImage(node.nodeId, node.fieldName) },
                                    onRemoveFile = { onRemoveFile(node.nodeId, node.fieldName) }
                                )
                                if (index < detail.inputNodes.lastIndex) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .height(1.dp)
                                            .background(DarkSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom run button (fixed) ──
        RunTaskBottomBar(
            isRunning = uiState.isRunningTask,
            taskStep = uiState.taskStep,
            hasResult = uiState.taskOutputs.isNotEmpty() || uiState.taskError != null,
            onRun = onRunTask,
            onReset = onResetTask,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/* ═══════════════════════════════════════════════════
   Top bar (back only)
   ═══════════════════════════════════════════════════ */

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppInfoSection(detail: com.runninghub.shared.domain.model.AppDetail) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                detail.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tag.name,
                            color = Primary300,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
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
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
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
private fun DescriptionSection(description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputNodeField(
    node: InputNode,
    currentValue: String,
    localUri: String?,
    uploadState: UploadingState?,
    onValueChanged: (String) -> Unit,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit
) {
    val options = remember(node.fieldData) { node.getOptions() }
    val isUploading = uploadState != null
    val uploadProgress = uploadState?.progress ?: 0f
    val isUploadError = uploadState?.isError == true

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListDropdown(
    options: List<String>,
    currentValue: String,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text("请选择", color = Neutral400.copy(alpha = 0.5f), fontSize = 14.sp)
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
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
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
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
    Box(
        modifier = modifier
            .fillMaxWidth()
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
