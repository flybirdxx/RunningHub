package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.theme.*
import com.runninghub.app.util.formatMinutesSeconds
import com.runninghub.app.util.formatOneDecimal

@Composable
fun MediaChipCard(
    reference: MediaReference,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = DarkSurfaceVariant,
        border = BorderStroke(1.dp, DarkOutlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceSM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaThumb(
                reference = reference,
                modifier = Modifier.size(40.dp),
            )

            Spacer(Modifier.width(Dimens.SpaceSM))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reference.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Neutral200,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(2.dp))

                val typeLabel = buildString {
                    append(
                        when (reference.type) {
                            QuickCreateMediaType.IMAGE -> "图片参考"
                            QuickCreateMediaType.VIDEO -> "视频参考"
                            QuickCreateMediaType.AUDIO -> "音频参考"
                        }
                    )
                    if (reference.fileSizeBytes > 0) {
                        append(" · ")
                        append(formatFileSize(reference.fileSizeBytes))
                    }
                    if (reference.type == QuickCreateMediaType.VIDEO && reference.durationSeconds != null) {
                        append(" · ")
                        append(formatDuration(reference.durationSeconds))
                    }
                }
                Text(
                    text = typeLabel,
                    fontSize = 10.sp,
                    color = Neutral500,
                )

                Spacer(Modifier.height(4.dp))

                val (barColor, statusText) = when (reference.uploadStatus) {
                    UploadStatus.UPLOADING -> Pair(Primary300, "上传中 ${(reference.uploadProgress * 100).toInt()}%")
                    UploadStatus.PROCESSING -> Pair(WarningDark, "AI 分析中...")
                    UploadStatus.DONE -> Pair(SuccessDark, "就绪")
                    UploadStatus.FAILED -> Pair(ErrorDark, "上传失败")
                }

                val alpha = if (reference.uploadStatus == UploadStatus.UPLOADING) pulseAlpha else 1f

                LinearProgressIndicator(
                    progress = { reference.uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = barColor.copy(alpha = alpha),
                    trackColor = DarkSurface,
                )

                if (reference.uploadStatus != UploadStatus.DONE) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = barColor.copy(alpha = alpha),
                    )
                }
            }

            Spacer(Modifier.width(Dimens.SpaceSM))

            if (reference.uploadStatus == UploadStatus.DONE) {
                Surface(
                    shape = RoundedCornerShape(Dimens.RadiusFull),
                    color = SuccessDark,
                    modifier = Modifier.size(18.dp),
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(14.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    tint = Neutral500,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun MediaThumb(
    reference: MediaReference,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.RadiusSM),
        color = DarkSurface,
    ) {
        when (reference.type) {
            QuickCreateMediaType.IMAGE -> {
                if (reference.uri.toString().startsWith("content://")) {
                    AsyncImage(
                        model = reference.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = Neutral500,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            QuickCreateMediaType.VIDEO -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            QuickCreateMediaType.AUDIO -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${formatOneDecimal(bytes / (1024.0 * 1024 * 1024))} GB"
    }
}

private fun formatDuration(seconds: Int): String = formatMinutesSeconds(seconds)
