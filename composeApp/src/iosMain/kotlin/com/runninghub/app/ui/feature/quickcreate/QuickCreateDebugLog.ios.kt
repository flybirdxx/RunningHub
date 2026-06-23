package com.runninghub.app.ui.feature.quickcreate

/**
 * iOS 调试日志实现。
 *
 * 当前问题只在 Android 调试链路排查，iOS 侧保持无输出，避免引入额外平台日志噪声。
 */
internal actual fun quickCreateDebugLog(tag: String, message: String) = Unit
