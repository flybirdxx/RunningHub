package com.runninghub.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

@Composable
actual fun VideoThumbnail(
    url: String,
    modifier: Modifier,
    posterUrl: String?,
    autoPlay: Boolean,
    cropToFill: Boolean,
    onPlaybackProgressChange: ((VideoPlaybackProgress) -> Unit)?,
    seekRequest: VideoPlaybackSeekRequest?,
) {
    if (posterUrl != null) {
        SmartAsyncImage(
            imageUrl = posterUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = if (cropToFill) ContentScale.Crop else ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF1E2A5E)),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶", fontSize = 32.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}
