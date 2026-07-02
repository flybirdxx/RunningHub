package com.runninghub.app.ui.feature.detail

import androidx.compose.runtime.Composable
import com.runninghub.app.ui.adaptive.RhAdaptivePreview
import com.runninghub.app.ui.adaptive.RhPreviewSpec
import com.runninghub.app.ui.adaptive.previewAppDetail
import com.runninghub.app.ui.adaptive.previewTaskOutputs
import com.runninghub.feature.detail.presentation.AppDetailTaskStep
import com.runninghub.feature.detail.presentation.AppDetailUiState
import com.runninghub.feature.detail.presentation.appDetailInputKey
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * App 详情页自适应窗口预览。
 *
 * 从 AppDetailScreen.kt 拆分而来，只承载预览样例状态和 @Preview 入口，
 * 让主屏幕文件专注渲染结构；预览数据由 previewAppDetail/previewTaskOutputs 提供。
 */
private fun detailPreviewState(): AppDetailUiState {
    val detail = previewAppDetail()
    return AppDetailUiState(
        isLoading = false,
        detail = detail,
        inputValues = detail.inputNodes.associate { node ->
            appDetailInputKey(node) to (node.fieldValue ?: "")
        },
        taskStep = AppDetailTaskStep.SUCCESS,
        taskOutputs = previewTaskOutputs(),
    )
}

@Composable
private fun DetailAdaptivePreview(spec: RhPreviewSpec) {
    RhAdaptivePreview(spec = spec) {
        DetailContent(
            uiState = detailPreviewState(),
            onBack = {},
            onAuthorClick = {},
            onInputChanged = { _, _, _ -> },
            onRunTask = {},
            onResetTask = {},
            onPickMedia = { _, _, _ -> },
            onRemoveFile = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun DetailPhone320Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone320)
}

@Preview
@Composable
private fun DetailPhone360Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone360)
}

@Preview
@Composable
private fun DetailPhone430Preview() {
    DetailAdaptivePreview(RhPreviewSpec.Phone430)
}

@Preview
@Composable
private fun DetailMediumPreview() {
    DetailAdaptivePreview(RhPreviewSpec.Medium600)
}

@Preview
@Composable
private fun DetailExpandedPreview() {
    DetailAdaptivePreview(RhPreviewSpec.Expanded840)
}

@Preview
@Composable
private fun DetailLandscapePreview() {
    DetailAdaptivePreview(RhPreviewSpec.Landscape800)
}

@Preview
@Composable
private fun DetailFontScale13Preview() {
    DetailAdaptivePreview(RhPreviewSpec.FontScale13)
}

@Preview
@Composable
private fun DetailFontScale15Preview() {
    DetailAdaptivePreview(RhPreviewSpec.FontScale15)
}
