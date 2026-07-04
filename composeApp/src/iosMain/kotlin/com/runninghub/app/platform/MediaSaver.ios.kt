package com.runninghub.app.platform

actual fun createMediaSaver(): MediaSaver = IosMediaSaver()

/**
 * iOS 相册保存占位实现。
 *
 * 当前始终返回失败语义，页面层会提示保存失败；不抛异常、不访问平台 API，
 * 保证 iOS 侧编译与运行安全。
 */
private class IosMediaSaver : MediaSaver {
    override suspend fun saveImageToGallery(url: String, displayName: String): MediaSaveResult {
        // TODO(result-card-v2)：接入 Photos 框架保存，Windows 环境未验证。
        return MediaSaveResult.Failure(MediaSaveFailureReason.UNSUPPORTED_PLATFORM)
    }
}
