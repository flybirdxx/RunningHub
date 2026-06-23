package com.runninghub.app.ui.feature.quickcreate

import android.util.Log

private const val QUICK_CREATE_DEBUG_TAG_PREFIX = "RHQC"

/**
 * Android 调试日志实现。
 *
 * 仅写入调用方传入的脱敏统计信息，便于通过 Android Studio Logcat 或 `adb logcat` 追踪模型目录流转。
 */
internal actual fun quickCreateDebugLog(tag: String, message: String) {
    runCatching {
        Log.d("$QUICK_CREATE_DEBUG_TAG_PREFIX.$tag", message)
    }
}
