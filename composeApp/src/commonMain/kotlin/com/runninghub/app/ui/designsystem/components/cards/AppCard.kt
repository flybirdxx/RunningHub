package com.runninghub.app.ui.designsystem.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography

/** AppCard 结果预览媒体类型。 */
enum class AppCardPreviewType {
    Image,
    Video,
    Audio,
    Empty,
}

/** 叠加卡指标的语义图标类型，组件据此选择前导图标，保持 designsystem 不依赖业务枚举。 */
enum class AppCardMetricIcon {
    Use,
    Like,
    View,
    Collect,
}

/**
 * AppCard 结果预览状态。
 *
 * @property url 可展示的预览资源地址；为空时组件展示占位。
 * @property type 预览资源类型。
 */
data class AppCardPreviewState(
    val url: String?,
    val type: AppCardPreviewType,
)

/**
 * 叠加卡的单条辅助指标。
 *
 * @property icon 指标语义图标类型。
 * @property value 调用方已格式化的指标值。
 */
data class AppCardMetricState(
    val icon: AppCardMetricIcon,
    val value: String,
)

/**
 * 整图叠加式创作入口卡片状态。
 *
 * @property id WebApp ID 或卡片稳定 ID。
 * @property title 模板名。
 * @property capabilityLabel 调用方已本地化的能力类型（图像/视频/音频等）。
 * @property preview 封面预览。
 * @property metrics 真实辅助指标（0..2 条，按展示优先级排序）。
 * @property featured 是否运营精选，为 true 时右上展示精选标记。
 */
data class AppCardState(
    val id: String,
    val title: String,
    val capabilityLabel: String,
    val preview: AppCardPreviewState,
    val metrics: List<AppCardMetricState>,
    val featured: Boolean = false,
)

/** 渲染发现页和搜索结果页共用的整图叠加式创作入口 AppCard。 */
@Composable
fun AppCard(
    state: AppCardState,
    onClick: () -> Unit,
    featuredLabel: String,
    modifier: Modifier = Modifier,
    previewContent: @Composable BoxScope.(AppCardPreviewState) -> Unit = { AppCardPreviewPlaceholder(it) },
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        color = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.md),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(RhTheme.shapes.md)),
        ) {
            previewContent(state.preview)

            // 媒体叠加渐变遮罩：沿用 batch-1 Hero 的内容层语义，属于覆盖在图片之上的
            // 可读性保护层，不代表主题色板，因此保留 Color.Black 硬编码而不迁移到 RhColors。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
            )

            // 能力类型药丸：叠加在封面之上的内容层标签，同上保留 Color.Black/White 硬编码。
            Text(
                text = state.capabilityLabel,
                color = Color.White,
                style = RhTypography.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(RhSpacing.sm)
                    .clip(RoundedCornerShape(RhTheme.shapes.sm))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
            )

            if (state.featured) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(RhSpacing.sm)
                        .clip(RoundedCornerShape(RhTheme.shapes.sm))
                        .background(RhTheme.colors.brandPrimary)
                        .padding(horizontal = RhSpacing.sm, vertical = RhSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = RhTheme.colors.textInverse,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = featuredLabel,
                        color = RhTheme.colors.textInverse,
                        style = RhTypography.meta,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                Text(
                    text = state.title,
                    color = Color.White,
                    style = RhTypography.cardTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.metrics.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(RhSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.metrics.forEach { metric ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(RhSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = metric.icon.toMetricVector(),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.82f),
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = metric.value,
                                    color = Color.White.copy(alpha = 0.82f),
                                    style = RhTypography.meta,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 把叠加卡指标语义映射为具体的 Material 图标。 */
private fun AppCardMetricIcon.toMetricVector(): ImageVector = when (this) {
    AppCardMetricIcon.Use -> Icons.Rounded.LocalFireDepartment
    AppCardMetricIcon.Like -> Icons.Rounded.FavoriteBorder
    AppCardMetricIcon.View -> Icons.Rounded.Visibility
    AppCardMetricIcon.Collect -> Icons.Rounded.BookmarkBorder
}

@Composable
private fun BoxScope.AppCardPreviewPlaceholder(preview: AppCardPreviewState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center,
    ) {}
}
