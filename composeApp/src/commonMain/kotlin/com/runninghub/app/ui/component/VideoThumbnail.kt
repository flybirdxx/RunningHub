package com.runninghub.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoThumbnail(
    url: String,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    autoPlay: Boolean = true,
)
