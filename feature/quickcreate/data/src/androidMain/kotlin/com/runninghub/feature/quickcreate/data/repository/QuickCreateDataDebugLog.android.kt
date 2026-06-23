package com.runninghub.feature.quickcreate.data.repository

import android.util.Log

private const val QUICK_CREATE_DATA_DEBUG_TAG_PREFIX = "RHQCD"

/**
 * Android 模型目录调试日志实现。
 */
internal actual fun quickCreateDataDebugLog(tag: String, message: String) {
    runCatching {
        Log.d("$QUICK_CREATE_DATA_DEBUG_TAG_PREFIX.$tag", message)
    }
}
