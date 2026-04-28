package com.runninghub.app.ui.feature.detail
 
import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.valentinilk.shimmer.shimmer
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.data.remote.model.WebAppDetailDto
import com.runninghub.app.data.remote.model.InputNodeDto
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoPlayer
import com.runninghub.app.util.PermissionManager
import com.runninghub.app.util.ResourceCacheManager
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppDetailScreen(
    appId: String,
    viewModel: AppDetailViewModel,
    onBack: () -> Unit,
    onAuthorClick: (String) -> Unit
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
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
                        onMediaPick = viewModel::uploadMedia,
                        onAuthorClick = onAuthorClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppDetailContent(
    detail: WebAppDetailDto,
    inputValues: List<InputNodeDto>,
    uploadingNodes: Map<String, Boolean>,
    nodeLocalUris: Map<String, Uri>,
    taskResultUrl: String?,
    onNodeValueChange: (String, String, String) -> Unit,
    onMediaPick: (String, String, String, Uri) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    var pendingMediaNode by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var editingListNode by remember { mutableStateOf<InputNodeDto?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Bottom Sheet for LIST nodes
    if (editingListNode != null) {
        ModalBottomSheet(
            onDismissRequest = { editingListNode = null },
            sheetState = sheetState,
            containerColor = Color(0xFF1E1E1E),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.2f)) }
        ) {
            val node = editingListNode!!
            val options = remember(node.fieldData) { node.getOptions() }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "选择 ${node.description ?: node.fieldName}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNodeValueChange(node.nodeId, node.fieldName, option)
                                editingListNode = null
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = node.fieldValue == option,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = RunningHubTeal)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(option, color = Color.White)
                    }
                }
            }
        }
    }
    
    // val localUris = nodeLocalUris // Unused alias

    
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

    // 权限请求：在回调中再启动对应的媒体选择器，避免同时 launch 两个 ActivityResult
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            pendingMediaNode?.let { (_, _, fieldType) ->
                when (fieldType) {
                    "AUDIO" -> audioPickerLauncher.launch("audio/*")
                    "VIDEO" -> visualMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                    else -> visualMediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        } else {
            // 权限被拒绝，清除待处理节点
            pendingMediaNode = null
        }
    }

    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Cover Carousel
        item(key = "cover_carousel") {
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
                    val url = if (covers.isEmpty()) "" else (covers[page].url ?: "")
                    val isVideo = remember(url) { url.endsWith(".mp4", ignoreCase = true) || url.contains("video", ignoreCase = true) }
                    
                    if (isVideo && url.isNotEmpty()) {
                        VideoPlayer(
                            videoUrl = url,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SmartAsyncImage(
                            imageUrl = url,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
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
        }

        item(key = "app_meta_info") {
            Column(modifier = Modifier.padding(16.dp)) {
                // 2. Title & Tags
                Text(
                    text = detail.name ?: "未命名应用",
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
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        // Try to get author ID from owner object
                        val authorId = detail.owner?.id
                        if (!authorId.isNullOrEmpty()) {
                            onAuthorClick(authorId)
                        }
                    },
                    color = Color.White.copy(alpha = 0.03f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartAsyncImage(
                            imageUrl = detail.getDisplayAvatar() ?: "",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = detail.getDisplayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "发布于 ${detail.publishTime?.split("T")?.firstOrNull() ?: "未知"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
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
            }
        }

        // 5. Result Display (If generated)
        if (taskResultUrl != null) {
            item(key = "task_result") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("生成结果", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    ) {
                        SmartAsyncImage(
                            imageUrl = taskResultUrl,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // 6. Description
        item(key = "app_description") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("简介", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (detail.description ?: "无简介").replace(Regex("<[^>]*>"), "").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 7. Input Nodes (Interactive)
        if (inputValues.isNotEmpty()) {
            item(key = "input_nodes_header") {
                Text(
                    "配置参数", 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold, 
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            itemsIndexed(inputValues, key = { _, node -> "node_${node.nodeId}_${node.fieldName}" }) { _, node ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    InputNodeItem(
                        node = node,
                        isUploading = uploadingNodes[node.nodeId] == true,
                        localUri = nodeLocalUris[node.nodeId],
                        onValueChange = { newValue ->
                            onNodeValueChange(node.nodeId, node.fieldName, newValue)
                        },
                        onPickMediaRequest = {
                            pendingMediaNode = Triple(node.nodeId, node.fieldName, node.fieldType)
                            val permissions = PermissionManager.getMediaPermissions()
                            if (permissions.isEmpty()) {
                                // 无需权限，直接启动选择器
                                when (node.fieldType) {
                                    "AUDIO" -> audioPickerLauncher.launch("audio/*")
                                    "VIDEO" -> visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                    else -> visualMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            } else {
                                // 先请求权限，授予后在回调中启动选择器
                                permissionLauncher.launch(permissions)
                            }
                        },
                        onEditListRequest = {
                            editingListNode = it
                        }
                    )
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(120.dp))
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                )
            )
            .padding(16.dp),
        color = Color.Transparent
    ) {
        val buttonBrush = if (isRunning) {
            Brush.linearGradient(listOf(Color.Gray, Color.DarkGray))
        } else {
            Brush.linearGradient(listOf(RunningHubTeal, Color(0xFF00BFA5)))
        }

        Button(
            onClick = onRun,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(buttonBrush, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = if (isRunning) Color.White else Color.Black
            ),
            enabled = !isRunning,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.4f))
        Spacer(Modifier.height(4.dp))
        Text(count, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
    }
}
@Composable
fun BaseInputCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF25272B), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
fun ListInputNode(
    node: InputNodeDto,
    onClick: () -> Unit
) {
    BaseInputCard(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = node.description ?: node.fieldName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(node.fieldValue ?: "选择选项", fontSize = 13.sp, color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MediaInputNode(
    node: InputNodeDto,
    isUploading: Boolean,
    localUri: android.net.Uri?,
    onPickMediaRequest: () -> Unit
) {
    val context = LocalContext.current
    var lottiePath by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        delay((100L..500L).random())
        lottiePath = ResourceCacheManager.getResourcePath(
            context,
            "https://raw.githubusercontent.com/nt4f04uNd/sweyer/master/assets/animations/checkmark.json",
            "success_check.json"
        )
    }

    val lottieComposition by rememberLottieComposition(
        if (lottiePath != null) LottieCompositionSpec.File(File(lottiePath!!).absolutePath)
        else LottieCompositionSpec.Url("https://raw.githubusercontent.com/nt4f04uNd/sweyer/master/assets/animations/checkmark.json")
    )
    val lottieProgress by animateLottieCompositionAsState(composition = lottieComposition, iterations = 1)
    // A node is considered "uploaded" if it has a valid-looking URL or local URI
    val isUploaded = !node.fieldValue.isNullOrBlank() && 
            (node.fieldValue.startsWith("http") || node.fieldValue.contains("/") || node.fieldValue.length > 30)
    var isLoadSuccess by remember(node.fieldValue) { mutableStateOf(false) }

    BaseInputCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Square Preview Box
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .clickable { if (!isUploading) onPickMediaRequest() },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RunningHubTeal, strokeWidth = 2.dp)
                } else {
                    // 1. Base Layer: Placeholder Icon (Show if not loaded yet)
                    if (!isLoadSuccess && localUri == null) {
                        Icon(
                            imageVector = when(node.fieldType) {
                                "AUDIO" -> Icons.Default.Audiotrack
                                "VIDEO" -> Icons.Default.VideoLibrary
                                else -> Icons.Default.AddPhotoAlternate
                            },
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // 2. Media Layer: The actual image/icon
                    if (isUploaded || localUri != null) {
                        if (node.fieldType == "IMAGE") {
                            val imageUrl = when {
                                localUri != null -> localUri.toString()
                                node.fieldValue?.startsWith("http") == true -> node.fieldValue
                                node.fieldValue?.startsWith("content://") == true -> node.fieldValue
                                node.fieldValue?.startsWith("file://") == true -> node.fieldValue
                                node.fieldValue.isNullOrBlank() -> ""
                                else -> "https://rh-images.xiaoyaoyou.com/${node.fieldValue!!.removePrefix("api/")}"
                            }
                            if (imageUrl.isNotEmpty()) {
                                SmartAsyncImage(
                                    imageUrl = imageUrl,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    showBrokenIcon = false, // Hides the "mountain" icon on 404
                                    onSuccess = { isLoadSuccess = true }
                                )
                            }
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                        } else {
                            isLoadSuccess = true
                            val icon = if (node.fieldType == "AUDIO") Icons.Default.Audiotrack else Icons.Default.VideoLibrary
                            Icon(icon, null, modifier = Modifier.size(32.dp), tint = RunningHubTeal.copy(alpha = 0.5f))
                        }

                        // 3. Success Feedback (Only if loaded)
                        if (isLoadSuccess || localUri != null) {
                            LottieAnimation(composition = lottieComposition, progress = { lottieProgress }, modifier = Modifier.size(44.dp))
                        }
                    }
                }

                // 4. Floating Action Icon (Camera/Add) - Bottom Right
                val showSuccessIndicators = (isLoadSuccess || localUri != null) && !isUploading
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp).size(24.dp),
                    shape = CircleShape,
                    color = Color(0xFF35383F),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = if (showSuccessIndicators) Icons.Default.PhotoCamera else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (showSuccessIndicators) RunningHubTeal else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = node.description ?: node.fieldName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                val secondaryText = when(node.fieldType) {
                    "AUDIO" -> "【选填，支持音频输入控制】"
                    "VIDEO" -> "【选填，支持视频输入控制】"
                    else -> "【选填，一张图不上传默认是文生图】"
                }
                
                // Only show secondary text if it's different from the title
                if ((node.description ?: node.fieldName) != secondaryText) {
                    Text(
                        text = secondaryText,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TextInputNode(
    node: InputNodeDto,
    onValueChange: (String) -> Unit
) {
    BaseInputCard {
        Text(
            text = node.description ?: node.fieldName,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = node.fieldValue ?: "",
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Color.White),
            minLines = if (node.fieldType == "STRING") 3 else 1,
            maxLines = 10,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.02f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedBorderColor = RunningHubTeal,
                cursorColor = RunningHubTeal
            )
        )
    }
}

@Composable
fun InputNodeItem(
    node: InputNodeDto,
    isUploading: Boolean = false,
    localUri: android.net.Uri? = null,
    onValueChange: (String) -> Unit,
    onPickMediaRequest: () -> Unit = {},
    onEditListRequest: (InputNodeDto) -> Unit = {}
) {
    when (node.fieldType) {
        "LIST" -> ListInputNode(node) { onEditListRequest(node) }
        "IMAGE", "AUDIO", "VIDEO" -> MediaInputNode(node, isUploading, localUri, onPickMediaRequest)
        else -> TextInputNode(node, onValueChange)
    }
}
