package com.runninghub.app.ui.feature.detail
 
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.InputNodeDto
import com.runninghub.app.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDetailScreen(
    appId: String,
    viewModel: AppDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(appId) {
        viewModel.fetchAppDetail(appId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.appDetail?.name ?: "应用详情", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            if (uiState.appDetail != null) {
                RunActionButton(
                    isRunning = uiState.isRunning,
                    statusText = uiState.statusText,
                    onRun = viewModel::runTask
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RunningHubTeal
                )
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "错误: ${uiState.error}", color = Color.Red, modifier = Modifier.padding(16.dp))
                    Button(onClick = { viewModel.fetchAppDetail(appId) }) {
                        Text("重试")
                    }
                }
            } else {
                uiState.appDetail?.let { detail ->
                    AppDetailContent(
                        detail = detail,
                        inputValues = uiState.inputValues,
                        uploadingNodes = uiState.uploadingNodes,
                        nodeLocalUris = uiState.nodeLocalUris,
                        taskResultUrl = uiState.taskResultUrl,
                        onNodeValueChange = viewModel::updateNodeValue,
                        onMediaPick = viewModel::uploadMedia
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDetailContent(
    detail: WebAppDetailDto,
    inputValues: List<InputNodeDto>,
    uploadingNodes: Map<String, Boolean>,
    nodeLocalUris: Map<String, Uri>,
    taskResultUrl: String?,
    onNodeValueChange: (String, String, String) -> Unit,
    onMediaPick: (String, String, String, Uri) -> Unit
) {
    var pendingMediaNode by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    
    // val localUris = nodeLocalUris // Unused alias

    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Re-trigger the picker if permissions are granted
            pendingMediaNode?.let { (nodeId, fieldName, fieldType) ->
                when (fieldType) {
                    "AUDIO" -> {} // Handled separately if needed
                    "VIDEO" -> {} // Handled separately if needed
                    else -> {}
                }
            }
        }
    }

    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            pendingMediaNode?.let { (nodeId, fieldName, fieldType) ->
                onMediaPick(nodeId, fieldName, fieldType, it)
            }
        }
        pendingMediaNode = null
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingMediaNode?.let { (nodeId, fieldName, fieldType) ->
                onMediaPick(nodeId, fieldName, fieldType, it)
            }
        }
        pendingMediaNode = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Cover Carousel
        val covers = detail.covers ?: emptyList()
        val pagerState = rememberPagerState(pageCount = { if (covers.isEmpty()) 1 else covers.size })
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val imageUrl = if (covers.isEmpty()) "" else covers[page].url
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Pager Indicator
            if (covers.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(covers.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) RunningHubTeal else Color.White.copy(alpha = 0.5f)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(6.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // 2. Title & Tags
            Text(
                text = detail.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                detail.tags?.forEach { tag ->
                    Surface(
                        color = RunningHubTeal.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, RunningHubTeal.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = tag.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = RunningHubTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Author Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = detail.owner?.avatar ?: "https://www.runninghub.cn/favicon.ico",
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = detail.owner?.name ?: detail.owner?.nickname ?: "Anonymous",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "发布于 ${detail.publishTime?.take(10) ?: "未知"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(Icons.Default.Favorite, detail.statisticsInfo?.likeCount ?: "0", "点赞")
                StatItem(Icons.Default.Star, detail.statisticsInfo?.collectCount ?: "0", "收藏")
                StatItem(Icons.Default.PlayArrow, detail.statisticsInfo?.useCount ?: "0", "运行")
                StatItem(Icons.Default.Visibility, detail.statisticsInfo?.pv ?: "0", "浏览")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Result Display (If generated)
            if (taskResultUrl != null) {
                Text("生成结果", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, RunningHubTeal, RoundedCornerShape(12.dp)),
                ) {
                    AsyncImage(
                        model = taskResultUrl,
                        contentDescription = "Result",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. Description
            Text("简介", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (detail.description ?: "无简介").replace(Regex("<[^>]*>"), "").trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 7. Input Nodes (Interactive)
            if (inputValues.isNotEmpty()) {
                Text("配置参数", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                inputValues.forEach { node ->
                    InputNodeItem(
                        node = node,
                        isUploading = uploadingNodes[node.nodeId] == true,
                        localUri = nodeLocalUris[node.nodeId],
                        onValueChange = { newValue ->
                            onNodeValueChange(node.nodeId, node.fieldName, newValue)
                        },
                        onPickMediaRequest = {
                            pendingMediaNode = Triple(node.nodeId, node.fieldName, node.fieldType)
                            
                            // Check for permissions (demo of PermissionManager usage)
                            permissionLauncher.launch(PermissionManager.getMediaPermissions())

                            when (node.fieldType) {
                                "AUDIO" -> audioPickerLauncher.launch("audio/*")
                                "VIDEO" -> visualMediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                                else -> visualMediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun RunActionButton(
    isRunning: Boolean,
    statusText: String?,
    onRun: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            )
            .padding(16.dp),
        color = Color.Transparent
    ) {
        Button(
            onClick = onRun,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RunningHubTeal,
                contentColor = Color.Black
            ),
            enabled = !isRunning
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text(statusText ?: "运行中...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("立即运行", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        Text(count, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun InputNodeItem(
    node: InputNodeDto,
    isUploading: Boolean = false,
    localUri: android.net.Uri? = null,
    onValueChange: (String) -> Unit,
    onPickMediaRequest: () -> Unit = {}
) {
    var showListDialog by remember { mutableStateOf(false) }
    val options = remember(node.fieldData) { node.getOptions() }

    if (showListDialog) {
        AlertDialog(
            onDismissRequest = { showListDialog = false },
            title = { Text("选择 ${node.description ?: node.fieldName}") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(option)
                                    showListDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = node.fieldValue == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showListDialog = false }) { Text("取消") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(node.description ?: node.fieldName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        
        when (node.fieldType) {
            "LIST" -> {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showListDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(node.fieldValue ?: "选择选项", fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
            }
            "IMAGE", "AUDIO", "VIDEO" -> {
                val isUploaded = !node.fieldValue.isNullOrBlank() && node.fieldValue.contains(".")
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { 
                            if (!isUploading) onPickMediaRequest()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RunningHubTeal)
                            Spacer(Modifier.height(8.dp))
                            Text("正在上传...", fontSize = 12.sp, color = RunningHubTeal)
                        }
                    } else if (isUploaded || localUri != null) {
                        if (node.fieldType == "IMAGE") {
                            // Display the uploaded image or placeholder
                            val imageUrl = when {
                                localUri != null -> localUri
                                node.fieldValue?.startsWith("http") == true -> node.fieldValue
                                node.fieldValue?.startsWith("content://") == true -> node.fieldValue
                                node.fieldValue?.startsWith("file://") == true -> node.fieldValue
                                else -> "https://rh-images.xiaoyaoyou.com/${node.fieldValue?.removePrefix("api/")}"
                            }
                            
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Uploaded Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = 0.8f
                            )
                        } else {
                            // Audio or Video icon
                            val icon = if (node.fieldType == "AUDIO") Icons.Default.Audiotrack else Icons.Default.VideoLibrary
                            Icon(icon, null, modifier = Modifier.size(48.dp), tint = RunningHubTeal.copy(alpha = 0.5f))
                        }
                        
                        // Overlay with status
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text("点击替换", fontSize = 10.sp, color = Color.White)
                            }
                        }

                        // Checkmark indicator
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center),
                            tint = RunningHubTeal.copy(alpha = 0.9f)
                        )
                    } else {
                        val (icon, label, format) = when(node.fieldType) {
                            "AUDIO" -> Triple(Icons.Default.Audiotrack, "上传音频", "支持 MP3, WAV 格式")
                            "VIDEO" -> Triple(Icons.Default.VideoLibrary, "上传视频", "支持 MP4, MOV 格式")
                            else -> Triple(Icons.Default.AddPhotoAlternate, "上传图片", "支持 JPG, PNG 格式")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                color = Color.White.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.padding(12.dp),
                                    tint = Color.Gray
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(label, fontSize = 13.sp, color = Color.Gray)
                            Text(format, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            else -> {
                OutlinedTextField(
                    value = node.fieldValue ?: "",
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    minLines = if (node.fieldType == "STRING") 3 else 1,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RunningHubTeal,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}
