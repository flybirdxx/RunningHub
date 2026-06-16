package com.runninghub.app.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.LocalExtendedColors

@Composable
fun SmartAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(Dimens.RadiusMD),
) {
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    // Null/empty URL: skip shimmer, show nothing (handled by caller)
    val hasValidUrl = !imageUrl.isNullOrBlank()

    Box(modifier = modifier.clip(shape)) {
        if (hasValidUrl) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            isLoading = true
                            isError = false
                        }
                        is AsyncImagePainter.State.Error -> {
                            isLoading = false
                            isError = true
                        }
                        is AsyncImagePainter.State.Success -> {
                            isLoading = false
                            isError = false
                        }
                        else -> {}
                    }
                }
            )
        }

        if (isLoading) {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        }

        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "图片加载失败",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
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
