package com.runninghub.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.theme.Dimens
import com.runninghub.app.ui.theme.LocalExtendedColors

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium
) {
    val extendedColors = LocalExtendedColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            extendedColors.shimmerBase,
            extendedColors.shimmerHighlight,
            extendedColors.shimmerBase
        ),
        start = Offset(translateX, 0f),
        end = Offset(translateX + 300f, 0f)
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
fun AppCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSM)
    ) {
        ShimmerPlaceholder(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.8f)
        )
        ShimmerPlaceholder(
            modifier = Modifier.fillMaxWidth(0.7f).height(14.dp),
            shape = MaterialTheme.shapes.small
        )
        ShimmerPlaceholder(
            modifier = Modifier.fillMaxWidth(0.4f).height(12.dp),
            shape = MaterialTheme.shapes.small
        )
    }
}
