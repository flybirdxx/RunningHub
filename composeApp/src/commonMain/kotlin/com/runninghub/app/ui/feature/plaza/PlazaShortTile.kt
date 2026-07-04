package com.runninghub.app.ui.feature.plaza

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.theme.RhSpacing
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.theme.RhTypography
import com.runninghub.feature.community.domain.PlazaCreationCard
import com.runninghub.feature.community.domain.PlazaShortCard
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.plaza_default_short_owner
import runninghub.composeapp.generated.resources.plaza_image_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_short_media_type_fallback
import runninghub.composeapp.generated.resources.plaza_untitled_short

/**
 * 广场短片 16:9 缩略图卡片。
 *
 * 仅做 token 换肤：封面之上的渐变遮罩、居中播放按钮与时长标签属于覆盖在媒体上的内容层，保留
 * Color.Black/White 硬编码并加注释；占位背景改为 [RhTheme] 语义色。
 */
@Composable
internal fun PlazaShortTile(
    card: PlazaShortCard,
    onPreviewClick: (() -> Unit)? = null,
) {
    val previewClickModifier = if (onPreviewClick != null) {
        Modifier.clickable(onClick = onPreviewClick)
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(previewClickModifier),
        verticalArrangement = Arrangement.spacedBy(RhSpacing.xs),
    ) {
        val videoUrl = card.videoUrl?.takeIf { it.isNotBlank() }
        val posterUrl = card.thumbnailUrl?.takeIf { it.isNotBlank() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(plazaShortThumbnailAspectRatio())
                .clip(RoundedCornerShape(RhTheme.shapes.md))
                .background(RhTheme.colors.surfaceSunken),
        ) {
            when {
                posterUrl != null -> {
                    SmartAsyncImage(
                        imageUrl = posterUrl,
                        contentDescription = card.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                videoUrl != null -> {
                    VideoThumbnail(
                        url = videoUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                    )
                }
                else -> {
                    PlazaMissingMediaVisual(
                        PlazaCreationCard(
                            id = card.id,
                            intro = card.name,
                        ),
                        mediaType = stringResource(Res.string.plaza_short_media_type_fallback),
                    )
                }
            }
            // 媒体叠加渐变遮罩：覆盖在封面之上的可读性保护层，保留 Color.Black 硬编码。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        ),
                    ),
            )
            if (onPreviewClick != null) {
                // 居中播放按钮：媒体内容层控件，保留 Color.Black/White 硬编码。
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            // 白色叠加文字：媒体内容层作者名，保留 Color.White 硬编码。
            Text(
                text = card.authorName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: stringResource(Res.string.plaza_default_short_owner),
                color = Color.White,
                style = RhTypography.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = RhSpacing.sm, end = 52.dp, bottom = RhSpacing.xs),
            )
            plazaShortDurationLabel(card.durationSeconds)?.let { duration ->
                // 白色叠加时长：媒体内容层标签，保留 Color.White 硬编码。
                Text(
                    text = duration,
                    color = Color.White,
                    style = RhTypography.meta,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = RhSpacing.sm, bottom = RhSpacing.xs),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = card.name.ifBlank { stringResource(Res.string.plaza_untitled_short) },
                color = RhTheme.colors.textPrimary,
                style = RhTypography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(card.authorName, card.categoryName)
                    .joinToString(" / ")
                    .ifBlank { stringResource(Res.string.plaza_default_short_owner) },
                color = RhTheme.colors.textTertiary,
                style = RhTypography.meta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 无媒体作品的兜底视觉。
 *
 * 渐变底色属于占位内容层；为满足全面迁移 Rh token 的要求，改用语义色 [RhTheme] 而非旧硬编码色值。
 */
@Composable
internal fun PlazaMissingMediaVisual(card: PlazaCreationCard, mediaType: String? = card.mediaType) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RhTheme.colors.surfaceSunken),
    ) {
        Text(
            text = mediaType ?: stringResource(Res.string.plaza_image_media_type_fallback),
            color = RhTheme.colors.textTertiary,
            style = RhTypography.caption,
            modifier = Modifier.align(Alignment.Center).padding(RhSpacing.sm),
        )
    }
}
