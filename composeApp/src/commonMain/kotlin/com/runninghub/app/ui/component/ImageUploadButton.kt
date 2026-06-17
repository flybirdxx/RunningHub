package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runninghub.app.ui.theme.DarkSurface
import com.runninghub.app.ui.theme.DarkSurfaceVariant
import com.runninghub.app.ui.theme.ErrorDark
import com.runninghub.app.ui.theme.Neutral400
import com.runninghub.app.ui.theme.Primary300

@Composable
fun ImageUploadButton(
    localUri: String?,
    remoteUrl: String?,
    fileName: String?,
    isUploading: Boolean,
    uploadProgress: Float,
    isError: Boolean = false,
    mediaType: MediaType = MediaType.IMAGE,
    square: Boolean = false,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayUrl = remoteUrl ?: localUri
    val hasFile = !displayUrl.isNullOrBlank()
    val uploadLabel = when (mediaType) {
        MediaType.IMAGE -> "上传图片"
        MediaType.VIDEO -> "上传视频"
        MediaType.AUDIO -> "上传音频"
    }
    val clickLabel = "点击$uploadLabel"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (square) Modifier.aspectRatio(1f) else Modifier.height(120.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.72f))
            .border(
                width = 1.5.dp,
                color = when {
                    isError -> ErrorDark.copy(alpha = 0.6f)
                    hasFile -> Primary300.copy(alpha = 0.5f)
                    else -> Neutral400.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isUploading) { onPickFile() },
        contentAlignment = Alignment.Center
    ) {
        when {
            isUploading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = Primary300,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "上传中... ${(uploadProgress * 100).toInt()}%",
                        color = Neutral400,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Primary300,
                        trackColor = DarkSurface
                    )
                }
            }

            isError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "上传失败",
                        tint = ErrorDark,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "上传失败，点击重新选择",
                        color = ErrorDark,
                        fontSize = 13.sp
                    )
                }
            }

            hasFile -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (mediaType == MediaType.IMAGE) {
                        SmartAsyncImage(
                            imageUrl = displayUrl,
                            contentDescription = fileName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (mediaType == MediaType.VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                                contentDescription = uploadLabel,
                                tint = Primary300,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = fileName ?: displayUrl.substringAfterLast("/").substringAfterLast("%2F"),
                                color = Color.White.copy(alpha = 0.86f),
                                fontSize = 12.sp,
                                maxLines = if (square) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (remoteUrl != null) "已上传" else "本地预览",
                                color = Primary300,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        // Touch target: 44dp via default IconButton sizing (no size restriction)
                        IconButton(onClick = onRemoveFile) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "移除",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (fileName != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = fileName,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    Icon(
                        imageVector = when (mediaType) {
                            MediaType.IMAGE -> Icons.Default.Add
                            MediaType.VIDEO -> Icons.Default.Videocam
                            MediaType.AUDIO -> Icons.Default.MusicNote
                        },
                        contentDescription = uploadLabel,
                        tint = Neutral400,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = clickLabel,
                        color = Neutral400,
                        fontSize = if (square) 12.sp else 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
