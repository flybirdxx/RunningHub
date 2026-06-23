package com.runninghub.app.ui.feature.quickcreate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.quick_create_assistant_avatar_label
import runninghub.composeapp.generated.resources.quick_create_poster_brand_vertical
import runninghub.composeapp.generated.resources.quick_create_poster_future_vertical

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
 * 绘制设计稿中反复出现的深色玻璃面板。
 *
 * @param modifier 外层布局修饰符。
 * @param radius 面板圆角，默认匹配底部输入栏与卡片。
 * @param borderColor 面板描边色，用于区分普通面板与选中态。
 * @param content 面板内部内容。
 */
@Composable
internal fun QuickCreateGlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    borderColor: Color = QuickCreateDesignTokens.Stroke,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = QuickCreateDesignTokens.Panel,
        shape = RoundedCornerShape(radius),
        border = BorderStroke(1.dp, borderColor),
        content = {
            Box(content = content)
        },
    )
}

/**
 * 底部弹层顶部的拖拽条。
 *
 * 该元素只是视觉层级提示，不绑定拖拽手势；真实展开/收起仍由调用方的 Sheet 状态控制。
 */
@Composable
internal fun QuickCreateSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 42.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.42f)),
    )
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

/**
 * 赛博霓虹猫海报占位。
 *
 * 真实生成图片加载失败、尚未生成或预览环境没有远端图片时使用该占位。绘制内容严格服务于
 * 设计稿的结构还原：左侧中文竖排、右侧 RUNNINGHUB 竖排、城市灯柱、白猫主体与条形码。
 */
@Composable
internal fun QuickCreateCyberCatPoster(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF07142E), Color(0xFF310D45), Color(0xFF050612)),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            repeat(14) { index ->
                val x = w * (0.04f + index * 0.07f)
                val barH = h * (0.2f + (index % 5) * 0.07f)
                drawRect(
                    color = if (index % 2 == 0) QuickCreateDesignTokens.Cyan.copy(alpha = 0.55f) else QuickCreateDesignTokens.Pink.copy(alpha = 0.58f),
                    topLeft = Offset(x, 0f),
                    size = Size(w * (0.012f + (index % 3) * 0.008f), barH),
                )
            }
            drawOval(
                color = Color(0xFFF1CFC6),
                topLeft = Offset(w * 0.39f, h * 0.38f),
                size = Size(w * 0.36f, h * 0.48f),
            )
            drawCircle(
                color = Color(0xFFF4D6C8),
                radius = w * 0.2f,
                center = Offset(w * 0.57f, h * 0.34f),
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.38f, h * 0.26f)
                    lineTo(w * 0.45f, h * 0.12f)
                    lineTo(w * 0.51f, h * 0.30f)
                    close()
                },
                color = Color(0xFFF4D6C8),
            )
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.65f, h * 0.30f)
                    lineTo(w * 0.73f, h * 0.12f)
                    lineTo(w * 0.78f, h * 0.32f)
                    close()
                },
                color = Color(0xFFF4D6C8),
            )
            drawCircle(Color(0xFF081124), radius = w * 0.07f, center = Offset(w * 0.50f, h * 0.35f))
            drawCircle(Color(0xFF081124), radius = w * 0.07f, center = Offset(w * 0.65f, h * 0.35f))
            drawLine(
                color = Color.Black,
                start = Offset(w * 0.57f, h * 0.35f),
                end = Offset(w * 0.59f, h * 0.35f),
                strokeWidth = 3.dp.toPx(),
            )
            drawCircle(
                color = Color(0xFF31113D),
                radius = w * 0.045f,
                center = Offset(w * 0.57f, h * 0.55f),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            repeat(18) { index ->
                drawRect(
                    color = Color(0xFFEBD7FF),
                    topLeft = Offset(w * (0.08f + index * 0.014f), h * 0.88f),
                    size = Size(if (index % 3 == 0) 2.dp.toPx() else 1.dp.toPx(), h * 0.055f),
                )
            }
        }
        androidx.compose.material3.Text(
            text = stringResource(Res.string.quick_create_poster_future_vertical),
            color = QuickCreateDesignTokens.Pink,
            fontSize = 28.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        androidx.compose.material3.Text(
            text = stringResource(Res.string.quick_create_poster_brand_vertical),
            color = QuickCreateDesignTokens.Pink,
            fontSize = 20.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}
