package com.runninghub.app.ui.component

import androidx.compose.runtime.Composable

@Composable
expect fun MediaPickerLauncher(onImagePicked: (String) -> Unit): MediaPickerLauncher

interface MediaPickerLauncher {
    fun launch()
}
