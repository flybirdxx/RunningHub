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
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.smart_async_image_error

/**
 * 渲染带加载骨架和失败占位的远程图片。
 *
 * 该组件只处理远程图片的本地渲染状态，不负责补 URL、重试或业务降级；失败提示使用
 * Compose Resources，避免图片组件持有硬编码错误文案。
 *
 * @param imageUrl 远程图片地址；为 `null` 或空字符串时不发起加载，也不展示错误文案。
 * @param contentDescription 图片无障碍描述，通常来自调用方的标题或作者名；可为 `null` 表示装饰图。
 * @param modifier 外层布局修饰符，用于控制尺寸、宽高比或列表布局。
 * @param contentScale 图片裁剪方式，默认裁剪填充容器。
 * @param shape 图片和占位内容的裁剪形状。
 * @param onImageAspectRatioResolved 图片加载成功后的原始宽高比回调；`null` 或非法尺寸不会触发。
 */
@Composable
fun SmartAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RoundedCornerShape(Dimens.RadiusMD),
    onImageAspectRatioResolved: (Float) -> Unit = {},
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
                            val size = state.painter.intrinsicSize
                            if (
                                size.width.isFinite() &&
                                size.height.isFinite() &&
                                size.width > 0f &&
                                size.height > 0f
                            ) {
                                onImageAspectRatioResolved(size.width / size.height)
                            }
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
                    text = stringResource(Res.string.smart_async_image_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 渲染图片加载过程中的通用 shimmer 骨架。
 *
 * 动画颜色来自主题扩展色，不包含用户可见文案；调用方通过 [modifier] 控制骨架尺寸和裁剪边界。
 *
 * @param modifier 外层布局修饰符，通常与待加载图片尺寸保持一致。
 */
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
