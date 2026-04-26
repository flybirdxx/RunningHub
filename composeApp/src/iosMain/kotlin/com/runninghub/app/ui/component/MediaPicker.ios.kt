package com.runninghub.app.ui.component

import androidx.compose.runtime.Composable

@Composable
actual fun MediaPickerLauncher(onImagePicked: (String) -> Unit): MediaPickerLauncher {
    return object : MediaPickerLauncher {
        override fun launch() {
            // iOS: PHPickerViewController to be implemented
        }
    }
}
