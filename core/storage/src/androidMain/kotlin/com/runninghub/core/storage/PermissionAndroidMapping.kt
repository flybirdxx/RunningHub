package com.runninghub.core.storage

import android.Manifest

/**
 * Android 平台权限映射。
 *
 * commonMain 的 [Permission] 只保存业务语义和跨平台 key；Android 运行时权限请求需要的
 * Manifest 权限名只允许在 androidMain 中解析，避免平台协议泄漏到共享业务模型。
 */
val Permission.androidManifestPermission: String
    get() = when (this) {
        Permission.MediaImages -> Manifest.permission.READ_MEDIA_IMAGES
        Permission.MediaVideo -> Manifest.permission.READ_MEDIA_VIDEO
        Permission.MediaAudio -> Manifest.permission.READ_MEDIA_AUDIO
        Permission.StorageRead -> Manifest.permission.READ_EXTERNAL_STORAGE
        Permission.Camera -> Manifest.permission.CAMERA
        Permission.Notifications -> Manifest.permission.POST_NOTIFICATIONS
    }

/**
 * 根据 Android Manifest 权限名还原跨平台权限。
 *
 * 该函数只用于 Android 系统权限回调和旧本地记录兼容；业务层不应保存或传递 Manifest 字符串。
 *
 * @param manifestPermission Android 系统返回的 Manifest 权限名。
 * @return 匹配的权限；未知权限返回 null。
 */
fun Permission.Companion.fromAndroidManifestPermission(manifestPermission: String): Permission? =
    when (manifestPermission) {
        Manifest.permission.READ_MEDIA_IMAGES -> Permission.MediaImages
        Manifest.permission.READ_MEDIA_VIDEO -> Permission.MediaVideo
        Manifest.permission.READ_MEDIA_AUDIO -> Permission.MediaAudio
        Manifest.permission.READ_EXTERNAL_STORAGE -> Permission.StorageRead
        Manifest.permission.CAMERA -> Permission.Camera
        Manifest.permission.POST_NOTIFICATIONS -> Permission.Notifications
        else -> null
    }
