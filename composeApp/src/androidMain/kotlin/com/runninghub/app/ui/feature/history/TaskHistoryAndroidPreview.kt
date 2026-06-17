package com.runninghub.app.ui.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.adaptive.previewTaskHistoryUiState

@Preview(name = "History phone 320", widthDp = 320, heightDp = 568, showBackground = true)
@Composable
private fun TaskHistoryAndroidPhone320Preview() {
    TaskHistoryAndroidPreviewSurface(widthDp = 320, heightDp = 568)
}

@Preview(name = "History landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun TaskHistoryAndroidLandscapePreview() {
    TaskHistoryAndroidPreviewSurface(widthDp = 800, heightDp = 360)
}

@Preview(name = "History font 1.5", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun TaskHistoryAndroidFontScale15Preview() {
    TaskHistoryAndroidPreviewSurface(widthDp = 360, heightDp = 800, fontScale = 1.5f)
}

@Composable
private fun TaskHistoryAndroidPreviewSurface(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
) {
    RunningHubPreviewSurface(
        windowWidth = widthDp.dp,
        windowHeight = heightDp.dp,
        fontScale = fontScale,
    ) {
        TaskHistoryContent(uiState = previewTaskHistoryUiState())
    }
}
