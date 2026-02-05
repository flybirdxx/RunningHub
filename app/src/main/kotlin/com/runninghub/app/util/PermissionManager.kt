package com.runninghub.app.util

import android.Manifest
import android.os.Build

/**
 * [INPUT]: 依赖 Android Framework 权限常量
 * [OUTPUT]: 提供权限状态检查与契约集合
 * [POS]: 全局工具类，用于封装复杂的 Android 版本权限差异
 * [PROTOCOL]: 权限变更时同步更新 AndroidManifest.xml
 */
object PermissionManager {

    /**
     * 获取媒体访问权限列表（根据 Android 版本自动适配）
     */
    fun getMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * 获取相机权限
     */
    fun getCameraPermission(): String = Manifest.permission.CAMERA

    /**
     * 网络权限常量（Manifest 声明即可，无需运行时请求，但此处提供用于检查）
     */
    fun getNetworkPermissions(): Array<String> = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )
}
