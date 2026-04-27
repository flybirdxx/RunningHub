package com.runninghub.shared.domain.model

sealed class Permission(
    val androidManifest: String,
    val description: String,
    val requiredFor: String,
    val icon: String,
) {
    data object MediaImages : Permission(
        androidManifest = "android.permission.READ_MEDIA_IMAGES",
        description = "需要读取您的图片，以便上传应用封面和素材",
        requiredFor = "上传图片",
        icon = "image"
    )

    data object MediaVideo : Permission(
        androidManifest = "android.permission.READ_MEDIA_VIDEO",
        description = "需要读取您的视频，以便上传演示内容",
        requiredFor = "上传视频",
        icon = "videocam"
    )

    data object MediaAudio : Permission(
        androidManifest = "android.permission.READ_MEDIA_AUDIO",
        description = "需要读取您的音频文件，以便添加背景音乐",
        requiredFor = "添加音频",
        icon = "music_note"
    )

    data object StorageRead : Permission(
        androidManifest = "android.permission.READ_EXTERNAL_STORAGE",
        description = "需要访问存储，以便读取媒体文件",
        requiredFor = "访问媒体",
        icon = "folder"
    )

    data object Camera : Permission(
        androidManifest = "android.permission.CAMERA",
        description = "需要使用相机，以便拍照或扫描二维码",
        requiredFor = "拍照/扫码",
        icon = "camera_alt"
    )

    data object Notifications : Permission(
        androidManifest = "android.permission.POST_NOTIFICATIONS",
        description = "需要发送通知，以便告知您任务完成状态",
        requiredFor = "任务通知",
        icon = "notifications"
    )

    companion object {
        fun fromManifest(manifest: String): Permission? =
            listOf(
                MediaImages,
                MediaVideo,
                MediaAudio,
                StorageRead,
                Camera,
                Notifications
            ).find { it.androidManifest == manifest }
    }
}
