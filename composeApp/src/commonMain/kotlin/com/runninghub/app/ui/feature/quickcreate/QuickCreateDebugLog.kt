package com.runninghub.app.ui.feature.quickcreate

/**
 * 快捷创作调试日志入口。
 *
 * commonMain 只声明脱敏日志边界，平台 actual 决定是否写出；日志只允许记录数量、类别和状态，
 * 不得写入 Token、Cookie、API Key、请求体、用户提示词或完整远程 URL。
 */
internal expect fun quickCreateDebugLog(tag: String, message: String)
