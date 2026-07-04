package com.runninghub.app.ui.feature.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.runninghub.app.ui.component.SmartAsyncImage
import com.runninghub.app.ui.component.VideoThumbnail
import com.runninghub.app.ui.designsystem.components.cards.AppCard
import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricIcon
import com.runninghub.app.ui.designsystem.components.cards.AppCardMetricState
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewState
import com.runninghub.app.ui.designsystem.components.cards.AppCardState
import com.runninghub.app.ui.designsystem.theme.RhTheme
import com.runninghub.app.ui.designsystem.components.cards.AppCardPreviewType as DsAppCardPreviewType
import com.runninghub.app.util.formatOneDecimal
import com.runninghub.feature.discovery.presentation.DiscoveryAppCapability
import com.runninghub.feature.discovery.presentation.DiscoveryAppCardMetricKind
import com.runninghub.feature.discovery.presentation.DiscoveryAppCardUiModel
import com.runninghub.feature.discovery.presentation.DiscoveryAppPreviewType
import org.jetbrains.compose.resources.stringResource
import runninghub.composeapp.generated.resources.Res
import runninghub.composeapp.generated.resources.discovery_capability_audio
import runninghub.composeapp.generated.resources.discovery_capability_general
import runninghub.composeapp.generated.resources.discovery_capability_image
import runninghub.composeapp.generated.resources.discovery_capability_video
import runninghub.composeapp.generated.resources.discovery_featured_label

/**
 * 发现页应用卡片渲染与展示映射。
 *
 * 从 DiscoveryScreen.kt 拆分而来，只承载 Compose UI 叶子组件和
 * Presentation 枚举到 Compose Resources 文案的最终映射；
 * 卡片语义仍由 feature:discovery:presentation 的 UiModel 表达。
 */
@Composable
internal fun DiscoveryAppCard(
    card: DiscoveryAppCardUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AppCard(
        state = card.toAppCardState(),
        onClick = onClick,
        featuredLabel = stringResource(Res.string.discovery_featured_label),
        modifier = modifier,
        previewContent = { preview ->
            DiscoveryAppCardPreview(preview)
        },
    )
}

@Composable
private fun DiscoveryAppCardPreview(preview: AppCardPreviewState) {
    val url = preview.url
    if (url.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RhTheme.colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {}
        return
    }

    when (preview.type) {
        DsAppCardPreviewType.Video -> VideoThumbnail(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
        else -> SmartAsyncImage(
            imageUrl = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun DiscoveryAppCardUiModel.toAppCardState(): AppCardState = AppCardState(
    id = id,
    title = templateName,
    capabilityLabel = capability.toCapabilityLabel(),
    preview = AppCardPreviewState(
        url = preview.url,
        type = preview.type.toDesignSystemPreviewType(),
    ),
    metrics = metrics.map { metric ->
        AppCardMetricState(
            icon = metric.kind.toMetricIcon(),
            value = formatCount(metric.value),
        )
    },
    featured = featured,
)

@Composable
private fun DiscoveryAppCapability.toCapabilityLabel(): String = stringResource(
    when (this) {
        DiscoveryAppCapability.IMAGE -> Res.string.discovery_capability_image
        DiscoveryAppCapability.VIDEO -> Res.string.discovery_capability_video
        DiscoveryAppCapability.AUDIO -> Res.string.discovery_capability_audio
        DiscoveryAppCapability.GENERAL -> Res.string.discovery_capability_general
    },
)

private fun DiscoveryAppCardMetricKind.toMetricIcon(): AppCardMetricIcon = when (this) {
    DiscoveryAppCardMetricKind.USE_COUNT -> AppCardMetricIcon.Use
    DiscoveryAppCardMetricKind.VIEW_COUNT -> AppCardMetricIcon.View
    DiscoveryAppCardMetricKind.LIKE_COUNT -> AppCardMetricIcon.Like
}

private fun DiscoveryAppPreviewType.toDesignSystemPreviewType(): DsAppCardPreviewType = when (this) {
    DiscoveryAppPreviewType.IMAGE -> DsAppCardPreviewType.Image
    DiscoveryAppPreviewType.VIDEO -> DsAppCardPreviewType.Video
    DiscoveryAppPreviewType.AUDIO -> DsAppCardPreviewType.Audio
    DiscoveryAppPreviewType.EMPTY -> DsAppCardPreviewType.Empty
}

internal fun formatCount(raw: String): String {
    val num = raw.toLongOrNull() ?: return raw
    return when {
        // Kotlin/Native 不支持 JVM 的 String.format；使用项目内跨平台格式化保持 iOS 编译稳定。
        num >= 10_000 -> "${formatOneDecimal(num / 10_000.0)}w"
        num >= 1_000 -> "${formatOneDecimal(num / 1_000.0)}k"
        else -> raw
    }
}
