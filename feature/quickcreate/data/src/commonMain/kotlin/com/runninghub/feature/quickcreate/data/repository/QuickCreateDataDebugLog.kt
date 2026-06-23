package com.runninghub.feature.quickcreate.data.repository

/**
 * 快捷创作 Data 层调试日志入口。
 *
 * 仅用于输出模型目录页码、数量和分类分布等脱敏统计；不得记录凭据、请求体、用户提示词或完整 URL。
 */
internal expect fun quickCreateDataDebugLog(tag: String, message: String)
