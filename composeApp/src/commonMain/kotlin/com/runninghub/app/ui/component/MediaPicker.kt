package com.runninghub.app.ui.component

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO
}

interface MediaPickerLauncher {
    fun launchImage()
    fun launchVideo()
    fun launchAudio()
}
