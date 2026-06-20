package com.runninghub.shared.domain.model

/**
 * 跨平台领域权限。
 *
 * 该模型只描述业务层关心的权限语义、用户可见说明和稳定持久化 key，不包含 Android
 * 运行时权限名、iOS Info.plist key 或其他平台协议字符串。具体平台权限名由 androidMain /
 * iosMain 中的映射函数负责，避免 commonMain Domain 泄漏平台细节。
 *
 * @property key 跨平台稳定权限标识，用于权限状态持久化和集合互斥判断。
 * @property description 权限说明文案，用于权限引导弹层。
 * @property requiredFor 权限用途摘要，用于说明该权限为什么被请求。
 * @property icon Presentation 图标语义名，UI 层会映射为具体图标。
 */
sealed class Permission(
    val key: String,
    val description: String,
    val requiredFor: String,
    val icon: String,
) {
    /**
     * 图片读取权限。
     */
    data object MediaImages : Permission(
        key = "media_images",
        description = "需要读取您的图片，以便上传应用封面和素材",
        requiredFor = "上传图片",
        icon = "image"
    )

    /**
     * 视频读取权限。
     */
    data object MediaVideo : Permission(
        key = "media_video",
        description = "需要读取您的视频，以便上传演示内容",
        requiredFor = "上传视频",
        icon = "videocam"
    )

    /**
     * 音频读取权限。
     */
    data object MediaAudio : Permission(
        key = "media_audio",
        description = "需要读取您的音频文件，以便添加背景音乐",
        requiredFor = "添加音频",
        icon = "music_note"
    )

    /**
     * 旧版 Android 外部存储读取权限。
     *
     * 该领域权限保留是为了兼容 Android 13 之前的媒体访问策略；是否映射为真实平台权限由
     * Android 层决定。
     */
    data object StorageRead : Permission(
        key = "storage_read",
        description = "需要访问存储，以便读取媒体文件",
        requiredFor = "访问媒体",
        icon = "folder"
    )

    /**
     * 相机权限。
     */
    data object Camera : Permission(
        key = "camera",
        description = "需要使用相机，以便拍照或扫描二维码",
        requiredFor = "拍照/扫码",
        icon = "camera_alt"
    )

    /**
     * 通知权限。
     */
    data object Notifications : Permission(
        key = "notifications",
        description = "需要发送通知，以便告知您任务完成状态",
        requiredFor = "任务通知",
        icon = "notifications"
    )

    companion object {
        /**
         * 根据跨平台稳定 key 还原领域权限。
         *
         * @param key [Permission.key] 中保存的稳定标识。
         * @return 匹配的权限；未知 key 返回 null，调用方应忽略或清理旧数据。
         */
        fun fromKey(key: String): Permission? =
            listOf(
                MediaImages,
                MediaVideo,
                MediaAudio,
                StorageRead,
                Camera,
                Notifications
            ).find { it.key == key }
    }
}
