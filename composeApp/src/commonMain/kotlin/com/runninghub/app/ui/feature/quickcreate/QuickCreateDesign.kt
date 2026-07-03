package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_assistant_avatar_label

/**
 * 快捷创作新版暗色界面的局部视觉 token。
 *
 * 这些颜色来自 Pencil 草稿中的 `rh-*` token，只服务于快捷创作重构，不改变全局主题。
 * 这样可以在不影响其他页面的前提下还原设计图的玻璃面板、霓虹强调和生成卡状态色。
 */
internal object QuickCreateDesignTokens {
    val Background = Color(0xFF020203)
    val Panel = Color(0xEE11151A)
    val PanelStrong = Color(0xF2171A20)
    val Stroke = Color(0xFF2D313A)
    val StrokeSoft = Color(0xFF232731)
    val Text = Color(0xFFF5F5F7)
    val Muted = Color(0xFF8B8C95)
    val Dim = Color(0xFF5F6068)
    val Purple = Color(0xFF8D63FF)
    val PurpleSoft = Color(0xFFB997FF)
    val Green = Color(0xFF16F4A7)
    val Cyan = Color(0xFF16D8FF)
    val Pink = Color(0xFFFF5CF4)
}
/**
 * 底部弹层顶部的拖拽条。
 *
 * 视觉上仍保持一条短横，但触控热区会放大到更容易按住的范围。组件自身不决定是否关闭 sheet，
 * 只把手势生命周期和纵向拖动距离回传给调用方，由外层根据拖动幅度实时绘制 sheet 偏移并决定回弹或收起。
 *
 * @param modifier 外层布局修饰符，通常由 sheet 内容传入居中对齐。
 * @param onDragStart 用户按住手柄开始拖动时触发。
 * @param onDrag 用户拖动手柄时触发，参数为本次纵向拖动像素；正数表示向下。
 * @param onDragEnd 用户松手结束拖动时触发。
 * @param onDragCancel 拖动被系统取消时触发。
 */
@Composable
internal fun QuickCreateSheetHandle(
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
) {
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    val latestDragCancel by rememberUpdatedState(onDragCancel)

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 16.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { latestDragStart() },
                    onDragEnd = latestDragEnd,
                    onDragCancel = latestDragCancel,
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        latestDrag(dragAmount)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.38f)),
        )
    }
}

/**
 * RH 助手头像占位。
 *
 * 截图中该头像用于标识系统生成回复；组件只绘制品牌缩写，不承载账号或凭据信息。
 */
@Composable
internal fun QuickCreateRhAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF101019)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(color = Color(0xFF4C338E))
            drawCircle(color = Color.White.copy(alpha = 0.18f), radius = size.minDimension * 0.22f, center = Offset(size.width * 0.72f, size.height * 0.28f))
        }
        androidx.compose.material3.Text(
            text = stringResource(Res.string.quick_create_assistant_avatar_label),
            color = QuickCreateDesignTokens.Text,
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
        )
    }
}

/**
 * 输入图与历史图使用的猫图缩略占位。
 *
 * 当真实图片地址不可用时，仍保持设计图中的“白猫参考图”视觉锚点；真实图片加载由外层决定。
 */
@Composable
internal fun QuickCreateCatThumbnail(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF21416F), Color(0xFFD9C9B8)),
                ),
            ),
    ) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFF2E5D6), radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.32f))
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.30f, h * 0.27f)
                lineTo(w * 0.38f, h * 0.08f)
                lineTo(w * 0.45f, h * 0.30f)
                close()
            },
            color = Color(0xFFF2E5D6),
        )
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.56f, h * 0.30f)
                lineTo(w * 0.64f, h * 0.08f)
                lineTo(w * 0.72f, h * 0.28f)
                close()
            },
            color = Color(0xFFF2E5D6),
        )
        drawOval(
            color = Color(0xFFEFE1D1),
            topLeft = Offset(w * 0.22f, h * 0.46f),
            size = Size(w * 0.56f, h * 0.42f),
        )
        drawCircle(Color(0xFF26314C), radius = w * 0.025f, center = Offset(w * 0.43f, h * 0.32f))
        drawCircle(Color(0xFF26314C), radius = w * 0.025f, center = Offset(w * 0.57f, h * 0.32f))
    }
}

/**
 * 模型选择和参数页的方形模型图标。
 *
 * @param modifier 外层布局修饰符。
 * @param accent 图标主强调色，用于区分图片、视频、音频和 3D 能力。
 * @param content 中间符号绘制内容。
 */
@Composable
internal fun QuickCreateModelGlyph(
    modifier: Modifier = Modifier,
    accent: Color = QuickCreateDesignTokens.Purple,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.56f), Color(0xFF201B3B)),
                ),
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
