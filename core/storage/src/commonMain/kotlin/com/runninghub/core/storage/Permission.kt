package com.runninghub.core.storage

/**
 * 跨平台应用权限。
 *
 * 该模型只描述业务层关心的权限语义、资源化文案 key 和稳定持久化 key，不包含 Android
 * 运行时权限名、iOS Info.plist key 或其他平台协议字符串。具体平台权限名由 androidMain /
 * iosMain 中的映射函数负责，避免 commonMain 泄漏平台细节。
 *
 * @property key 跨平台稳定权限标识，用于权限状态持久化和集合互斥判断。
 * @property descriptionKey 权限说明的稳定文案 key，应用壳负责映射为最终展示文案。
 * @property requiredForKey 权限用途摘要的稳定文案 key，应用壳负责映射为最终展示文案。
 * @property icon Presentation 图标语义名，UI 层会映射为具体图标。
 */
sealed class Permission(
    val key: String,
    val descriptionKey: PermissionTextKey,
    val requiredForKey: PermissionTextKey,
    val icon: String,
) {
    /** 图片读取权限。 */
    data object MediaImages : Permission(
        key = "media_images",
        descriptionKey = PermissionTextKey.MediaImagesDescription,
        requiredForKey = PermissionTextKey.MediaImagesRequiredFor,
        icon = "image",
    )

    /** 视频读取权限。 */
    data object MediaVideo : Permission(
        key = "media_video",
        descriptionKey = PermissionTextKey.MediaVideoDescription,
        requiredForKey = PermissionTextKey.MediaVideoRequiredFor,
        icon = "videocam",
    )

    /** 音频读取权限。 */
    data object MediaAudio : Permission(
        key = "media_audio",
        descriptionKey = PermissionTextKey.MediaAudioDescription,
        requiredForKey = PermissionTextKey.MediaAudioRequiredFor,
        icon = "music_note",
    )

    /**
     * 旧版 Android 外部存储读取权限。
     *
     * 该权限保留是为了兼容 Android 13 之前的媒体访问策略；是否映射为真实平台权限由
     * Android 层决定。
     */
    data object StorageRead : Permission(
        key = "storage_read",
        descriptionKey = PermissionTextKey.StorageReadDescription,
        requiredForKey = PermissionTextKey.StorageReadRequiredFor,
        icon = "folder",
    )

    /** 相机权限。 */
    data object Camera : Permission(
        key = "camera",
        descriptionKey = PermissionTextKey.CameraDescription,
        requiredForKey = PermissionTextKey.CameraRequiredFor,
        icon = "camera_alt",
    )

    /** 通知权限。 */
    data object Notifications : Permission(
        key = "notifications",
        descriptionKey = PermissionTextKey.NotificationsDescription,
        requiredForKey = PermissionTextKey.NotificationsRequiredFor,
        icon = "notifications",
    )

    companion object {
        /**
         * 根据跨平台稳定 key 还原权限。
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
                Notifications,
            ).find { it.key == key }
    }
}

/**
 * 跨平台权限模型的稳定文案 key。
 *
 * 该枚举只用于在 Core 层标记权限说明和用途摘要的语义，不保存最终中文文案。
 * composeApp 负责把这些 key 映射到 Compose Resources。
 */
enum class PermissionTextKey {
    /** 图片读取权限说明。 */
    MediaImagesDescription,

    /** 图片读取权限用途摘要。 */
    MediaImagesRequiredFor,

    /** 视频读取权限说明。 */
    MediaVideoDescription,

    /** 视频读取权限用途摘要。 */
    MediaVideoRequiredFor,

    /** 音频读取权限说明。 */
    MediaAudioDescription,

    /** 音频读取权限用途摘要。 */
    MediaAudioRequiredFor,

    /** 旧版存储读取权限说明。 */
    StorageReadDescription,

    /** 旧版存储读取权限用途摘要。 */
    StorageReadRequiredFor,

    /** 相机权限说明。 */
    CameraDescription,

    /** 相机权限用途摘要。 */
    CameraRequiredFor,

    /** 通知权限说明。 */
    NotificationsDescription,

    /** 通知权限用途摘要。 */
    NotificationsRequiredFor,
}
