package com.runninghub.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.LocalExtendedColors

@Composable
fun SmartAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.clip(RoundedCornerShape(Dimens.RadiusMD))) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            onState = { state ->
                isLoading = state is AsyncImagePainter.State.Loading
            }
        )

        if (isLoading) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
    val extendedColors = LocalExtendedColors.current
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            extendedColors.shimmerBase,
            extendedColors.shimmerHighlight,
            extendedColors.shimmerBase
        ),
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Box(modifier = modifier.background(shimmerBrush))
}
