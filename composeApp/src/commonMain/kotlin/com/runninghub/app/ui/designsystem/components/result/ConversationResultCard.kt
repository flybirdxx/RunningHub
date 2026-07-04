package com.runninghub.app.ui.designsystem.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.runninghub.app.ui.designsystem.components.badges.RhTaskStatus
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** 卡片宽度占可用宽度的最大比例；对话流中保持聊天气泡式布局，不再全宽铺开。 */
private const val ConversationResultCardMaxWidthFraction = 0.70f

/** 卡片高度上限；避免竖版结果在手机上占满一屏。 */
private val ConversationResultCardMaxHeight = 320.dp

/** 生成比例的下限；比该值更瘦的竖图按此比例截断，防止卡片过窄不可读。 */
private const val ConversationResultCardMinAspectRatio = 0.35f

/** 生成比例的上限；比该值更宽的横图按此比例截断，防止卡片过扁。 */
private const val ConversationResultCardMaxAspectRatio = 2.4f

/** 叠加操作圆钮直径。 */
private val OverlayActionButtonSize = 30.dp

/** 叠加操作圆钮内图标尺寸。 */
private val OverlayActionIconSize = 15.dp

/** 叠加操作圆钮禁用态整体透明度。 */
private const val OverlayActionDisabledAlpha = 0.4f

/**
 * 计算对话流结果卡的目标尺寸。
 *
 * 规则（定案 v3）：
 * - 宽度上限为可用宽度的 70%，高度上限固定 320dp；
 * - [aspectRatio] 为宽高比（宽 / 高），为 null、非有限值或小于等于 0 时回退为 1（正方形）；
 *   合法值被截断到 0.35～2.4 区间，避免极端比例产生不可读的卡片；
 * - 宽度取「宽度上限」与「高度上限 × 比例」中较小者，高度由宽度按比例反推，
 *   保证结果同时满足宽高双上限且保持原始比例。
 *
 * @param availableWidth 布局可用宽度。
 * @param aspectRatio 生成比例（宽 / 高）；null 表示未知，按 1:1 回退。
 * @return 满足宽高双上限的卡片尺寸。
 */
internal fun conversationResultCardSize(availableWidth: Dp, aspectRatio: Float?): DpSize {
    val maxWidth = availableWidth * ConversationResultCardMaxWidthFraction
    val safeRatio = aspectRatio
        ?.takeIf { it.isFinite() && it > 0f }
        ?.coerceIn(ConversationResultCardMinAspectRatio, ConversationResultCardMaxAspectRatio)
        ?: 1f
    val width = min(maxWidth, ConversationResultCardMaxHeight * safeRatio)
    val height = width / safeRatio
    return DpSize(width, height)
}

/**
 * 对话流结果卡上叠加的操作圆钮描述。
 *
 * @property icon 圆钮图标。
 * @property contentDescription 无障碍描述，调用方传入已本地化文案。
 * @property emphasized true 时使用品牌色实心底与反色图标（主操作）；false 时使用媒体上的半透明白底与白图标。
 * @property enabled 是否可点击；禁用时整钮降透明度且不响应点击。
 * @property onClick 点击回调，由调用方执行实际动作。
 */
data class ConversationResultCardAction(
    val icon: ImageVector,
    val contentDescription: String,
    val emphasized: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * 快捷创作对话流的整图叠加式结果卡。
 *
 * 卡片即媒体本身：媒体内容铺满圆角容器，状态徽与过期徽叠在图顶部，
 * 操作圆钮叠在图底部 scrim 上；长按态通过品牌色描边高亮表达。
 * 卡片尺寸由 [conversationResultCardSize] 按生成比例与宽高双上限驱动，呈聊天气泡式布局。
 *
 * 组件只负责渲染与回传动作：不持有业务状态，不执行保存、下载、导航等副作用，
 * 媒体渲染（图片或生成中蒙版）由调用方通过 [mediaContent] 槽提供。
 *
 * @param aspectRatio 生成比例（宽 / 高）；null 回退 1:1。
 * @param modifier 外部布局修饰符。
 * @param statusLabel 顶左状态徽文字（如「42%」「生成完成」）；null 不显示。
 * @param status 任务视觉状态，决定状态徽文字颜色；null 时按主文字白系展示。
 * @param expiryLabel 顶右过期徽文字（如「24h」）；null 不显示。
 * @param overlayActions 底部 scrim 上的操作圆钮列表；为空时不渲染 scrim。
 * @param highlighted 长按态：true 时外层加品牌色描边高亮。
 * @param onClick 整卡点击回调；与 [onLongPress] 均为 null 时不挂手势。
 * @param onLongPress 整卡长按回调。
 * @param mediaContent 媒体内容槽（图片或生成中蒙版），由调用方渲染，槽外层为铺满卡片的容器。
 */
@Composable
fun ConversationResultCard(
    aspectRatio: Float?,
    modifier: Modifier = Modifier,
    statusLabel: String? = null,
    status: RhTaskStatus? = null,
    expiryLabel: String? = null,
    overlayActions: List<ConversationResultCardAction> = emptyList(),
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    mediaContent: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val cardSize = conversationResultCardSize(maxWidth, aspectRatio)
        val cardShape = RoundedCornerShape(RhTheme.shapes.lg)
        val highlightModifier = if (highlighted) {
            Modifier.border(2.dp, RhTheme.colors.brandPrimary, cardShape)
        } else {
            Modifier
        }
        // 禁用 combinedClickable（实验性 API，历史上出现过运行期 NoSuchMethodError），
        // 用稳定的 pointerInput + detectTapGestures 组合承接点击与长按。
        val gestureModifier = if (onClick != null || onLongPress != null) {
            Modifier.pointerInput(onClick, onLongPress) {
                detectTapGestures(
                    onTap = onClick?.let { tap -> { _: Offset -> tap() } },
                    onLongPress = onLongPress?.let { longPress -> { _: Offset -> longPress() } },
                )
            }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .size(cardSize)
                .clip(cardShape)
                .then(highlightModifier)
                .then(gestureModifier),
        ) {
            mediaContent()
            statusLabel?.let { label ->
                ConversationResultCardBadge(
                    label = label,
                    textColor = statusBadgeTextColor(status),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(RhSpacing.sm),
                )
            }
            expiryLabel?.let { label ->
                ConversationResultCardBadge(
                    label = label,
                    textColor = RhTheme.colors.statusWarning,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(RhSpacing.sm),
                )
            }
            if (overlayActions.isNotEmpty()) {
                ConversationResultCardActionScrim(
                    actions = overlayActions,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 按任务状态映射状态徽文字颜色；null 状态按主文字白系展示。
 */
@Composable
private fun statusBadgeTextColor(status: RhTaskStatus?): Color {
    val colors = RhTheme.colors
    return when (status) {
        RhTaskStatus.Queued -> colors.statusProcessing
        RhTaskStatus.Running -> colors.statusProcessing
        RhTaskStatus.Success -> colors.statusSuccess
        RhTaskStatus.Failed -> colors.statusFailed
        RhTaskStatus.Canceled -> colors.textSecondary
        null -> colors.textPrimary
    }
}

/**
 * 叠加在媒体顶部的半透明黑胶囊徽。
 */
@Composable
private fun ConversationResultCardBadge(
    label: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(RhTheme.shapes.full))
            // 媒体叠加层色例外：半透明黑胶囊属于媒体内容层文字保护语义（批 1 Hero 封板先例），
            // 需要在任意媒体亮度上保证可读，不迁 Rh 色板。
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = textColor,
        style = RhTypography.meta,
    )
}

/**
 * 媒体底部的渐变 scrim 与右对齐操作圆钮行。
 */
@Composable
private fun ConversationResultCardActionScrim(
    actions: List<ConversationResultCardAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                // 媒体叠加层色例外：底部黑色渐变 scrim 属于媒体内容层光效保护语义
                // （批 1 Hero 封板先例），保证白系圆钮在亮色媒体上可读，不迁 Rh 色板。
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                ),
            )
            .padding(RhSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEach { action ->
            ConversationResultCardActionButton(action)
        }
    }
}

/**
 * scrim 上的单个操作圆钮。
 */
@Composable
private fun ConversationResultCardActionButton(action: ConversationResultCardAction) {
    // 媒体叠加层色例外：非强调钮使用半透明白底与白图标，属于媒体上白图标语义
    // （批 1 Hero 封板先例），不迁 Rh 色板；强调钮仍走品牌色 token。
    val backgroundColor = if (action.emphasized) {
        RhTheme.colors.brandPrimary
    } else {
        Color.White.copy(alpha = 0.16f)
    }
    val iconTint = if (action.emphasized) RhTheme.colors.textInverse else Color.White
    Box(
        modifier = Modifier
            .size(OverlayActionButtonSize)
            .alpha(if (action.enabled) 1f else OverlayActionDisabledAlpha)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                enabled = action.enabled,
                role = Role.Button,
                onClick = action.onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            tint = iconTint,
            modifier = Modifier.size(OverlayActionIconSize),
        )
    }
}
