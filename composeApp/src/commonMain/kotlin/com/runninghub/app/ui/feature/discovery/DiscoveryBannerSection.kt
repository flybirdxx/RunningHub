package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.app.ui.theme.WindowSizeClass
import com.runninghub.app.ui.theme.adaptiveGridSpacing
import com.runninghub.core.model.WebApp
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_metric_use_count
import runninghub.composeapp.generated.resources.discovery_metric_view_count

private const val DISCOVERY_BANNER_MAX_ITEMS = 6

/**
 * 发现页运营精选横滑区。从 DiscoveryScreen.kt 拆分而来，
 * 表面色与圆角走 Rh 语义 token；封面上的渐变遮罩与白色前景为
 * 媒体内容叠加语义（保证任意封面上的文字可读），沿批 1 Hero 先例保留。
 */
@Composable
internal fun DiscoveryBannerSection(
    banners: List<WebApp>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
    onAppClick: (String) -> Unit = {},
) {
    val spacing = adaptiveGridSpacing(windowSizeClass)
    val cardWidth = if (windowSizeClass.isWide) 360.dp else 286.dp
    val cardHeight = if (windowSizeClass.isWide) 156.dp else 124.dp
    val visibleBanners = banners.take(DISCOVERY_BANNER_MAX_ITEMS)

    LazyRow(
        modifier = modifier.fillMaxWidth().padding(bottom = spacing),
        contentPadding = PaddingValues(horizontal = spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
    ) {
        itemsIndexed(
            items = visibleBanners,
            key = { index, app -> app.id.ifBlank { "banner-$index" } },
        ) { _, app ->
            DiscoveryBannerCard(
                app = app,
                modifier = Modifier.width(cardWidth).height(cardHeight),
                onClick = { onAppClick(app.id) },
            )
        }
    }
}

@Composable
private fun DiscoveryBannerCard(
    app: WebApp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val previewUrl = app.coverUrl ?: app.thumbnailUrl ?: app.videoUrl
    val showVideo = app.videoUrl != null && previewUrl == app.videoUrl
    val metricText = app.useCount.takeIf { it.isNotBlank() }?.let { value ->
        "${stringResource(Res.string.discovery_metric_use_count)} ${formatCount(value)}"
    } ?: app.pv.takeIf { it.isNotBlank() }?.let { value ->
        "${stringResource(Res.string.discovery_metric_view_count)} ${formatCount(value)}"
    }

    Surface(
        modifier = modifier,
        onClick = onClick,
        color = RhTheme.colors.surfaceElevated,
        shape = RoundedCornerShape(RhTheme.shapes.md),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!previewUrl.isNullOrBlank()) {
                if (showVideo) {
                    VideoThumbnail(url = previewUrl, modifier = Modifier.fillMaxSize())
                } else {
                    SmartAsyncImage(
                        imageUrl = previewUrl,
                        contentDescription = app.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }

            // 媒体内容叠加：压暗渐变保证封面上的标题可读，属于内容层语义而非主题色，
            // 与批 1 Hero 封板裁定一致，不迁移到 RhColors token。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f),
                            ),
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = RhSpacing.md, vertical = RhSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
            ) {
                Text(
                    text = app.title,
                    style = RhTypography.cardTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                metricText?.let { value ->
                    Text(
                        text = value,
                        style = RhTypography.caption,
                        color = Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
