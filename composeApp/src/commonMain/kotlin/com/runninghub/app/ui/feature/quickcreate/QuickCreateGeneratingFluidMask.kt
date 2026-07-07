package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.runninghub.app.ui.designsystem.theme.RhTheme

/**
 * 生成中的流体占位蒙版。
 *
 * 动画对卡片实际尺寸感知：漂移的径向光斑与斜向掠过的高光带都按测量后的宽高计算坐标,
 * 保证任意尺寸的占位卡上动效都覆盖全卡、肉眼可见。旧实现使用固定像素坐标,
 * 在大卡片上只有左上角一小块在动,视觉上近似静止。
 * `Color.White`/`Color.Transparent` 属占位内容层的光效语义(与媒体叠加层同类),不迁 Rh 色板。
 */
@Composable
internal fun QuickCreateGeneratingFluidMask(modifier: Modifier = Modifier) {
    var maskSize by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "quick-create-fluid-mask")
    val drift = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quick-create-fluid-drift",
    )
    val sweep = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "quick-create-fluid-sweep",
    )
    val pulse = transition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "quick-create-fluid-pulse",
    )

    val processingColor = RhTheme.colors.statusProcessing
    val accentColor = RhTheme.colors.brandSecondary
    val width = maskSize.width.toFloat()
    val height = maskSize.height.toFloat()
    val sized = width > 0f && height > 0f

    var maskModifier = modifier
        .onSizeChanged { maskSize = it }
        .background(processingColor)
    if (sized) {
        // 漂移光斑：中心在卡片范围内缓慢往返,半径随卡片尺寸缩放。
        maskModifier = maskModifier
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = pulse.value),
                        processingColor.copy(alpha = 0.42f),
                        Color.Transparent,
                    ),
                    center = Offset(
                        x = width * (0.2f + 0.6f * drift.value),
                        y = height * (0.7f - 0.4f * drift.value),
                    ),
                    radius = maxOf(width, height) * 0.55f,
                ),
            )
        // 高光带：从左上角外侧斜向掠到右下角外侧,Restart 循环形成持续流动感。
        val band = maxOf(width, height) * 0.45f
        val bandStart = Offset(
            x = -band + (width + 2f * band) * sweep.value,
            y = -band + (height + 2f * band) * sweep.value,
        )
        maskModifier = maskModifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.16f),
                    accentColor.copy(alpha = 0.22f),
                    Color.Transparent,
                ),
                start = bandStart,
                end = Offset(bandStart.x + band, bandStart.y + band),
            ),
        )
    }
    Box(modifier = maskModifier)
}
