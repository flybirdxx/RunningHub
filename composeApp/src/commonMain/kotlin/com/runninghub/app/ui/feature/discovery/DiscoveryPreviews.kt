package com.runninghub.app.ui.feature.discovery

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewDiscoveryUiState
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * 发现页自适应窗口预览。
 *
 * 从 DiscoveryScreen.kt 拆分而来，只承载 @Preview 入口，
 * 便于主屏幕文件专注渲染结构；预览数据由 previewDiscoveryUiState 提供。
 */
@Composable
private fun DiscoveryAdaptivePreview(
    spec: RhPreviewSpec,
) {
    RhAdaptivePreview(spec = spec) {
        DiscoveryContent(
            uiState = previewDiscoveryUiState(),
            onSearchClick = {},
            onAppClick = {},
            onCategorySelected = {},
            onSortSelected = {},
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

@Preview
@Composable
private fun DiscoveryPhone320Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun DiscoveryPhone360Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun DiscoveryPhone430Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun DiscoveryMedium600Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun DiscoveryExpanded840Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun DiscoveryLandscapePreview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun DiscoveryFontScale13Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun DiscoveryFontScale15Preview() {
    DiscoveryAdaptivePreview(RhPreviewSpec.FontScale15)
}
