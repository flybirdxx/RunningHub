package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valentinilk.shimmer.shimmer
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.Decoder
import coil.decode.GifDecoder
import coil.decode.VideoFrameDecoder
import coil.fetch.SourceResult
import coil.request.ImageRequest
import coil.request.Options

/**
 * 智能异步图片加载组件
 * 自动识别 .mp4 并强制使用 VideoFrameDecoder
 * 自动识别 .gif 并使用 GifDecoder
 */
@Composable
fun SmartAsyncImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    showBrokenIcon: Boolean = false,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val cleanUrl = remember(imageUrl) { imageUrl.trim() }
    val isVideo = remember(cleanUrl) { cleanUrl.contains("mp4", ignoreCase = true) }
    
    val imageRequest = remember(cleanUrl) {
        ImageRequest.Builder(context)
            .data(cleanUrl.ifEmpty { null })
            .apply {
                if (isVideo) {
                    decoderFactory(ForceVideoDecoderFactory())
                    crossfade(true)
                } else if (cleanUrl.endsWith(".gif", ignoreCase = true)) {
                     decoderFactory(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(modifier = modifier) {
        var isLoading by remember { mutableStateOf(true) }
        var isError by remember { mutableStateOf(false) }

        AsyncImage(
            model = imageRequest,
            onLoading = { isLoading = true; isError = false },
            onSuccess = { 
                isLoading = false
                isError = false
                onSuccess()
            },
            onError = { isLoading = false; isError = true },
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLoading) Modifier.shimmer() else Modifier),
            contentScale = contentScale
        )
        
        if (isError && showBrokenIcon) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Error",
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // 如果是视频，额外显示一个小的标识 (可选)
        if (isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
    }
}

/**
 * A custom factory that bypasses the contentType/applicability check
 * inside VideoFrameDecoder.Factory.
 */
class ForceVideoDecoderFactory : Decoder.Factory {

    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
        return VideoFrameDecoder(result.source, options)
    }
    
    override fun equals(other: Any?) = other is ForceVideoDecoderFactory
    override fun hashCode() = javaClass.hashCode()
}
