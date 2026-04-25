package com.runninghub.app.ui.feature.community.tools

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import android.media.MediaPlayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.runninghub.app.R // Assuming Lottie Res logic
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.theme.RunningHubTeal
import com.runninghub.app.util.ResourceCacheManager
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretDecodeScreen(
    navController: NavHostController,
    viewModel: SecretDecodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        }
    }
    
    // Password Dialog
    if (uiState.needsPassword) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { /* Force input or back */ },
            title = { Text("加密文件") },
            text = {
                Column {
                    Text("该文件受密码保护，请输入密码以解码。")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.onPasswordEntered(password) }) {
                    Text("解密")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.reset() 
                    navController.popBackStack()
                }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐写解码", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            if (uiState.decodeSuccess && uiState.decodedFilename != null) {
                // SUCCESS STATE
                SuccessView(
                    filename = uiState.decodedFilename!!,
                    data = uiState.decodedData,
                    onSave = {
                        uiState.decodedData?.let { data ->
                             saveFile(context, uiState.decodedFilename!!, data)
                        }
                    },
                    onReset = viewModel::reset
                )
            } else if (uiState.isLoading) {
                // LOADING STATE
                CircularProgressIndicator(color = RunningHubTeal)
                Spacer(Modifier.height(16.dp))
                Text("正在深入像素寻找秘密...", color = Color.Gray)
            } else {
                // IDLE STATE
                UploadArea(
                    imageUri = uiState.imageUri,
                    onClick = {
                         mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
                
                if (uiState.error != null) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "解码失败: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun UploadArea(imageUri: Uri?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E1E))
                .border(2.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Upload",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("点击上传“鸭子图”", color = Color.Gray)
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text(
            "支持 SS_tools 算法 (2/6/8 bit)",
            color = Color.DarkGray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SuccessView(filename: String, data: ByteArray?, onSave: () -> Unit, onReset: () -> Unit) {
    val context = LocalContext.current
    var isPreviewVisible by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<File?>(null) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Lottie Localization
    var lottiePath by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
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
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition,
        iterations = 1
    )
    
    val isImage = remember(filename) { 
        filename.endsWith(".png", true) || filename.endsWith(".jpg", true) || filename.endsWith(".jpeg", true) || filename.endsWith(".webp", true)
    }
    val isVideo = remember(filename) { filename.endsWith(".mp4", true) || filename.endsWith(".mkv", true) }
    val isAudio = remember(filename) { filename.endsWith(".mp3", true) || filename.endsWith(".wav", true) || filename.endsWith(".m4a", true) }

    // Save to temp file for preview
    LaunchedEffect(data, filename) {
        if (data != null) {
            val tempFile = File(context.cacheDir, "preview_$filename")
            withContext(Dispatchers.IO) {
                tempFile.writeBytes(data)
            }
            previewFile = tempFile
        }
    }

    if (isFullScreen && previewFile != null) {
        FullScreenImageViewer(
            file = previewFile!!,
            onClose = { isFullScreen = false }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isAudio) 3f else 1f) 
                .heightIn(max = 400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E1E))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
        ) {
            if (previewFile != null) {
                val fileUri = Uri.fromFile(previewFile!!).toString()
                
                // Content Layer
                if (isPreviewVisible) {
                    when {
                        isImage -> {
                            SmartAsyncImage(
                                imageUrl = fileUri,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isFullScreen = true },
                                contentScale = ContentScale.Fit
                            )
                        }
                        isVideo -> {
                            VideoPreviewPlayer(previewFile!!)
                        }
                        isAudio -> {
                            AudioPreviewPlayer(previewFile!!)
                        }
                        else -> {
                            UnknownFilePreview(filename)
                        }
                    }
                }

                // Mask Layer (Overlay)
                if (!isPreviewVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Hidden",
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("内容已隐藏", color = Color.Gray)
                        }
                    }
                }
                
                // Toggle Button (Floating)
                if (!isAudio || !isPreviewVisible) { // Keep clean for audio
                    IconButton(
                        onClick = { isPreviewVisible = !isPreviewVisible },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPreviewVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Preview",
                            tint = Color.White
                        )
                    }
                }
                
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = RunningHubTeal
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        // Metadata & Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LockOpen, null, tint = RunningHubTeal, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "解码成功！",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.width(8.dp))
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(32.dp)
            )
        }
        
        Text(
            "文件: $filename\n大小: ${formatSize(data?.size ?: 0)}",
            color = Color.Gray,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RunningHubTeal)
        ) {
            Icon(Icons.Default.FileDownload, null, tint = Color.Black)
            Spacer(Modifier.width(8.dp))
            Text("保存到系统相册", color = Color.Black)
        }
        
        Spacer(Modifier.height(16.dp))
        
        TextButton(onClick = onReset) {
            Text("解码另一张", color = Color.Gray)
        }
    }
}

// --- Specialized Media Components ---

@Composable
fun VideoPreviewPlayer(file: File) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoPath(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun AudioPreviewPlayer(file: File) {
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(file) {
        val player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener { isPlaying = false }
        }
        mediaPlayer = player
        onDispose {
            player.release()
            mediaPlayer = null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                        isPlaying = false
                    } else {
                        it.start()
                        isPlaying = true
                    }
                }
            },
            modifier = Modifier
                .size(64.dp)
                .background(RunningHubTeal, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text("音频文件已解析", color = Color.White, fontWeight = FontWeight.Bold)
            Text("点击播放按钮试听", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun FullScreenImageViewer(file: File, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            SmartAsyncImage(
                imageUrl = Uri.fromFile(file).toString(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun UnknownFilePreview(filename: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.InsertDriveFile, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("预览暂不可用", color = Color.Gray)
            Text(filename, color = Color.DarkGray, fontSize = 10.sp)
        }
    }
}

fun formatSize(size: Int): String {
    val mb = size / 1024.0 / 1024.0
    return if (mb < 1) "${size / 1024} KB" else String.format("%.2f MB", mb)
}

fun saveFile(context: android.content.Context, filename: String, data: ByteArray) {
    // Save to public directory
    try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            // Determine mime type roughly
            val mimeType = when {
                filename.endsWith(".mp4") -> "video/mp4"
                filename.endsWith(".png") -> "image/png"
                filename.endsWith(".jpg") -> "image/jpeg"
                filename.endsWith(".mp3") -> "audio/mpeg"
                filename.endsWith(".wav") -> "audio/wav"
                filename.endsWith(".m4a") -> "audio/mp4"
                else -> "application/octet-stream"
            }
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            
            when {
                filename.endsWith(".mp4") -> put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/RunningHub")
                filename.endsWith(".mp3") || filename.endsWith(".wav") || filename.endsWith(".m4a") -> put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/RunningHub")
                else -> put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/RunningHub")
            }
        }
        
        val uri = when {
            filename.endsWith(".mp4") -> resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            filename.endsWith(".mp3") || filename.endsWith(".wav") || filename.endsWith(".m4a") -> resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            else -> resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
        
        uri?.let {
            resolver.openOutputStream(it)?.use { os ->
                os.write(data)
            }
            Toast.makeText(context, "已保存到相册: RunningHub", Toast.LENGTH_LONG).show()
        } ?: run {
             Toast.makeText(context, "保存失败", Toast.LENGTH_SHORT).show()
        }
        
    } catch (e: Exception) {
        Toast.makeText(context, "保存出错: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
