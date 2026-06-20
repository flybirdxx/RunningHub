package com.runninghub.feature.quickcreate.domain

/**
 * 快捷创作媒体上传的领域仓库边界。
 *
 * 本接口只暴露把本地读取到的媒体字节上传为远端 URL 的能力。平台 URI 解析、文件大小读取和 MIME
 * 推断由 Presentation 的平台边界完成；Data 层实现只负责携带凭据调用上传接口并映射错误。
 */
interface QuickCreationMediaUploadRepository {

    /**
     * 上传快捷创作所需的媒体文件。
     *
     * @param fileBytes 已由平台媒体解析器读取到内存中的文件字节；不得为空文件。
     * @param fileName 上传给服务端的文件名，需包含合理扩展名以便服务端识别媒体类型。
     * @param mimeType 标准 MIME 类型，例如 `image/jpeg`、`video/mp4` 或 `audio/mpeg`。
     * @return 上传成功时返回可提交给生成接口的远端 URL；失败时返回网络、认证或服务端异常。
     */
    suspend fun uploadMedia(fileBytes: ByteArray, fileName: String, mimeType: String): Result<String>
}
