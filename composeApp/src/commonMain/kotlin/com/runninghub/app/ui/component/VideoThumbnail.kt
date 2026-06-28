package com.runninghub.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class VideoPlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val bufferedPositionMs: Long? = null,
    val isLoading: Boolean = false,
) {
    val progressFraction: Float?
        get() = durationMs?.takeIf { it > 0L }?.let { duration ->
            (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }

    val bufferedFraction: Float?
        get() {
            val duration = durationMs?.takeIf { it > 0L } ?: return null
            val bufferedPosition = bufferedPositionMs ?: return null
            return (bufferedPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
}

data class VideoPlaybackSeekRequest(
    val requestId: Long,
    val fraction: Float,
)

internal fun videoThumbnailPlaybackVolume(playAudio: Boolean): Float =
    if (playAudio) 1f else 0f

@Composable
expect fun VideoThumbnail(
    url: String,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    autoPlay: Boolean = true,
    cropToFill: Boolean = true,
    playAudio: Boolean = false,
    onPlaybackProgressChange: ((VideoPlaybackProgress) -> Unit)? = null,
    seekRequest: VideoPlaybackSeekRequest? = null,
)
