package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.BrandLime
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.video_preview_close_content_description

data class VideoPreviewItem(
    val id: String,
    val videoUrl: String?,
    val posterUrl: String?,
    val title: String? = null,
    val subtitle: String? = null,
)

@Composable
fun RhVideoPreviewOverlay(
    item: VideoPreviewItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoUrl = item.videoUrl?.trim()?.takeIf { it.isNotEmpty() }
    val posterUrl = item.posterUrl?.trim()?.takeIf { it.isNotEmpty() }
    var playbackProgress by remember(item.id, videoUrl) {
        mutableStateOf(VideoPlaybackProgress(isLoading = videoUrl != null))
    }
    var seekRequestId by remember(item.id, videoUrl) { mutableStateOf(0L) }
    var seekRequest by remember(item.id, videoUrl) { mutableStateOf<VideoPlaybackSeekRequest?>(null) }
    val onSeekFraction: (Float) -> Unit = { fraction ->
        val durationMs = playbackProgress.durationMs
        if (durationMs != null && durationMs > 0L) {
            val safeFraction = fraction.coerceIn(0f, 1f)
            seekRequestId += 1
            seekRequest = VideoPlaybackSeekRequest(requestId = seekRequestId, fraction = safeFraction)
            playbackProgress = playbackProgress.copy(positionMs = (durationMs * safeFraction).toLong())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .blockVideoPreviewClickThrough(),
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 54.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.46f)),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.video_preview_close_content_description),
                tint = Color.White,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 112.dp, end = 24.dp, bottom = 54.dp),
            contentAlignment = Alignment.Center,
        ) {
            val previewWidth = maxWidth
                .coerceAtMost(430.dp)
            val shape = RoundedCornerShape(18.dp)

            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(Color.Black),
            ) {
                if (videoUrl != null) {
                    VideoThumbnail(
                        url = videoUrl,
                        posterUrl = posterUrl,
                        autoPlay = true,
                        cropToFill = false,
                        onPlaybackProgressChange = { progress -> playbackProgress = progress },
                        seekRequest = seekRequest,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape),
                    )
                } else if (posterUrl != null) {
                    SmartAsyncImage(
                        imageUrl = posterUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        shape = shape,
                    )
                }

                if (!item.title.isNullOrBlank() || !item.subtitle.isNullOrBlank() || videoUrl != null) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.74f)),
                                ),
                            )
                            .padding(start = 14.dp, top = 44.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!item.title.isNullOrBlank()) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!item.subtitle.isNullOrBlank()) {
                                Text(
                                    text = item.subtitle,
                                    color = Color.White.copy(alpha = 0.72f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        if (videoUrl != null) {
                            VideoPreviewProgressControl(
                                progress = playbackProgress,
                                onSeekFraction = onSeekFraction,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewProgressControl(
    progress: VideoPlaybackProgress,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = videoPreviewFormatTime(progress.positionMs),
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.width(42.dp),
        )

        VideoPreviewSeekBar(
            progress = progress,
            onSeekFraction = onSeekFraction,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = videoPreviewFormatTime(progress.durationMs),
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.width(42.dp),
        )
    }
}

@Composable
private fun VideoPreviewSeekBar(
    progress: VideoPlaybackProgress,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playedFraction = progress.progressFraction ?: 0f
    val bufferedFraction = progress.bufferedFraction ?: playedFraction
    val canSeek = progress.durationMs != null && progress.durationMs > 0L

    BoxWithConstraints(
        modifier = modifier
            .height(VideoPreviewSeekTouchHeightDp.dp)
            .pointerInput(canSeek) {
                if (!canSeek) return@pointerInput
                detectTapGestures { offset ->
                    onSeekFraction(offset.x / size.width)
                }
            }
            .pointerInput(canSeek) {
                if (!canSeek) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset -> onSeekFraction(offset.x / size.width) },
                    onDrag = { change, _ ->
                        onSeekFraction(change.position.x / size.width)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val thumbSize = VideoPreviewSeekThumbSizeDp.dp
        val trackHeight = VideoPreviewSeekTrackHeightDp.dp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(bufferedFraction.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.36f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(playedFraction.coerceIn(0f, 1f))
                .height(trackHeight)
                .clip(CircleShape)
                .background(BrandLime),
        )
        Box(
            modifier = Modifier
                .offset(x = (maxWidth - thumbSize) * playedFraction.coerceIn(0f, 1f))
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (canSeek) BrandLime else Color.White.copy(alpha = 0.5f)),
        )
    }
}

internal fun videoPreviewFormatTime(timeMs: Long?): String {
    val totalSeconds = timeMs?.takeIf { it >= 0L }?.let { it / 1000L } ?: return "--:--"
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun Modifier.blockVideoPreviewClickThrough(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Final)
            event.changes.forEach { pointerInputChange ->
                pointerInputChange.consume()
            }
        }
    }
}

private const val VideoPreviewSeekTouchHeightDp = 26
private const val VideoPreviewSeekTrackHeightDp = 4
private const val VideoPreviewSeekThumbSizeDp = 12
