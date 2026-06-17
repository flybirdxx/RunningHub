package com.runninghub.app.ui.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runninghub.app.ui.adaptive.RunningHubPreviewSurface
import com.runninghub.app.ui.adaptive.previewProfileUiState

@Preview(name = "Profile phone 320", widthDp = 320, heightDp = 568, showBackground = true)
@Composable
private fun ProfileAndroidPhone320Preview() {
    ProfileAndroidPreviewSurface(widthDp = 320, heightDp = 568)
}

@Preview(name = "Profile landscape", widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun ProfileAndroidLandscapePreview() {
    ProfileAndroidPreviewSurface(widthDp = 800, heightDp = 360)
}

@Preview(name = "Profile font 1.5", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun ProfileAndroidFontScale15Preview() {
    ProfileAndroidPreviewSurface(widthDp = 360, heightDp = 800, fontScale = 1.5f)
}

@Composable
private fun ProfileAndroidPreviewSurface(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
) {
    RunningHubPreviewSurface(
        windowWidth = widthDp.dp,
        windowHeight = heightDp.dp,
        fontScale = fontScale,
    ) {
        ProfileScreenContent(uiState = previewProfileUiState())
    }
}
