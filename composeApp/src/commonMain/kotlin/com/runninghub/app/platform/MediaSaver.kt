package com.runninghub.app.platform

/**
 * 媒体保存结果的稳定语义。
 *
 * 平台实现必须把原始异常归一化为该模型，禁止把异常文本或远端诊断信息透传给 UI；
 * 页面层只根据成功/失败选择本地化文案。
 */
sealed interface MediaSaveResult {
    /** 媒体已成功写入系统相册并对用户可见。 */
    data object Success : MediaSaveResult

    /**
     * 保存失败。
     *
     * @property reason 稳定失败原因，仅用于内部诊断与后续差异化提示；当前 UI 统一展示重试文案。
     */
    data class Failure(val reason: MediaSaveFailureReason) : MediaSaveResult
}

/**
 * 保存失败的稳定原因枚举。
 *
 * 不携带异常消息或 URL，避免敏感信息经由 UI 或日志泄漏。
 */
enum class MediaSaveFailureReason {
    /** 当前系统版本低于平台实现支持的最低版本（Android 侧要求 API 29+ 的 scoped storage 路径）。 */
    UNSUPPORTED_OS_VERSION,

    /** 当前平台尚未接入保存能力（iOS 占位实现）。 */
    UNSUPPORTED_PLATFORM,

    /** 结果媒体字节获取失败（网络或缓存均不可用）。 */
    FETCH_FAILED,

    /** 系统媒体库写入失败。 */
    WRITE_FAILED,
}

/**
 * 把远端结果媒体保存到系统相册的平台能力边界。
 *
 * commonMain 只依赖该接口；Android 通过 MediaStore scoped storage 实现，
 * iOS 本批为占位实现（后续接入 Photos 框架）。由 Koin 组合根装配为单例。
 */
interface MediaSaver {
    /**
     * 下载 [url] 指向的结果图并写入系统相册。
     *
     * 实现必须运行在 IO 上下文并支持协程取消；取消时尽力清理未完成的媒体条目。
     *
     * @param url 结果图的远程地址；实现不得把该地址写入日志。
     * @param displayName 保存文件的展示名前缀，实现会追加时间戳与扩展名防止重名。
     * @return 归一化后的保存结果，见 [MediaSaveResult]。
     */
    suspend fun saveImageToGallery(url: String, displayName: String): MediaSaveResult
}

expect fun createMediaSaver(): MediaSaver
