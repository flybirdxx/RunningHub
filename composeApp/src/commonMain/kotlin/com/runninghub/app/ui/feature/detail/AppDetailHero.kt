package com.runninghub.app.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.designsystem.components.chips.RhChip
import com.runninghub.app.ui.designsystem.components.layout.RhWrapRow
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.core.model.AppDetail
import com.runninghub.core.model.Author
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.app_detail_back_content_description
import runninghub.composeapp.generated.resources.app_detail_default_app_name
import runninghub.composeapp.generated.resources.app_detail_fans_count_format
import runninghub.composeapp.generated.resources.app_detail_stat_average_duration
import runninghub.composeapp.generated.resources.app_detail_stat_success_rate
import runninghub.composeapp.generated.resources.app_detail_stat_use_count

/**
 * App 详情页“关于本应用”英雄区。
 *
 * 从 AppDetailScreen.kt 拆分而来，承载封面轮播、标签、统计卡、作者行与简介入口等
 * 展示型 Composable；颜色统一读取 RhTheme 语义 token，标签墙由 RhWrapRow + RhChip 承载。
 * 压在封面图片上的黑色渐变与白色前景属于内容叠加语义，保留原值。
 */

@Composable
internal fun AppDetailHero(
    detail: AppDetail,
    onBack: () -> Unit,
    onAuthorClick: () -> Unit,
    showBackButton: Boolean = true,
) {
    val colors = RhTheme.colors
    val covers = detail.covers.mapNotNull { it.url }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (covers.isNotEmpty()) {
                CoverCarousel(
                    covers = covers,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .background(colors.surfaceElevated)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.52f),
                                Color.Transparent,
                                colors.backgroundPrimary.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            if (showBackButton) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = 12.dp, top = 10.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.42f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.app_detail_back_content_description),
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = detail.name ?: stringResource(Res.string.app_detail_default_app_name),
                color = colors.textPrimary,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (detail.tags.isNotEmpty()) {
                RhWrapRow(
                    spacing = RhSpacing.sm,
                    horizontalAlignment = Alignment.Start
                ) {
                    detail.tags.take(6).forEach { tag ->
                        RhChip(label = tag.name, onClick = {})
                    }
                }
            }

            StatsCard(
                useCount = detail.statisticsInfo?.useCount,
                successRate = detail.runningSuccessRate,
                avgSeconds = detail.avgRunningSeconds,
                modifier = Modifier.fillMaxWidth()
            )

            AuthorRow(
                name = detail.getDisplayName(),
                avatar = detail.getDisplayAvatar(),
                owner = detail.owner,
                onClick = onAuthorClick,
                modifier = Modifier.fillMaxWidth()
            )

        }
    }
}

/* ═══════════════════════════════════════════════════
   Cover carousel
   ═══════════════════════════════════════════════════ */

@Composable
private fun CoverCarousel(
    covers: List<String>,
    modifier: Modifier = Modifier
) {
    if (covers.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { covers.size })

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
        ) { page ->
            AsyncImage(
                model = covers[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (covers.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(covers.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

/* ═══════════════════════════════════════════════════
   Stats card (standalone)
   ═══════════════════════════════════════════════════ */

@Composable
private fun StatsCard(
    useCount: String?,
    successRate: String?,
    avgSeconds: String?,
    modifier: Modifier = Modifier
) {
    val stats = buildList {
        useCount?.takeIf { it.isNotBlank() }?.let {
            add(it to Res.string.app_detail_stat_use_count)
        }
        successRate?.takeIf { it.isNotBlank() }?.let {
            add("${it}%" to Res.string.app_detail_stat_success_rate)
        }
        avgSeconds?.takeIf { it.isNotBlank() }?.let {
            add("${it}s" to Res.string.app_detail_stat_average_duration)
        }
    }
    if (stats.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RhTheme.colors.surfaceDefault)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEachIndexed { index, stat ->
            StatItem(value = stat.first, label = stringResource(stat.second))
            if (index < stats.lastIndex) {
                StatDivider()
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = RhTheme.colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = RhTheme.colors.textSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(RhTheme.colors.surfaceElevated)
    )
}

/* ═══════════════════════════════════════════════════
   Author row (with follower info)
   ═══════════════════════════════════════════════════ */

@Composable
private fun AuthorRow(
    name: String,
    avatar: String?,
    owner: Author?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = RhTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceDefault)
            .then(if (owner?.id != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SmartAsyncImage(
            imageUrl = avatar,
            contentDescription = name,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (owner != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.app_detail_fans_count_format, owner.fansCount),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
