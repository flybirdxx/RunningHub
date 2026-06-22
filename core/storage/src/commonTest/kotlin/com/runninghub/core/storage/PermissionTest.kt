package com.runninghub.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionTest {
    @Test
    fun `fromKey restores known permission`() {
        assertEquals(Permission.MediaImages, Permission.fromKey("media_images"))
        assertEquals(Permission.MediaVideo, Permission.fromKey("media_video"))
        assertEquals(Permission.MediaAudio, Permission.fromKey("media_audio"))
        assertEquals(Permission.StorageRead, Permission.fromKey("storage_read"))
        assertEquals(Permission.Camera, Permission.fromKey("camera"))
        assertEquals(Permission.Notifications, Permission.fromKey("notifications"))
    }

    @Test
    fun `fromKey ignores unknown permission key`() {
        assertEquals(null, Permission.fromKey("android.permission.CAMERA"))
        assertEquals(null, Permission.fromKey("unknown"))
    }

    @Test
    fun `permissions expose stable text keys instead of display copy`() {
        assertEquals(PermissionTextKey.MediaImagesDescription, Permission.MediaImages.descriptionKey)
        assertEquals(PermissionTextKey.MediaImagesRequiredFor, Permission.MediaImages.requiredForKey)
        assertEquals(PermissionTextKey.MediaVideoDescription, Permission.MediaVideo.descriptionKey)
        assertEquals(PermissionTextKey.MediaVideoRequiredFor, Permission.MediaVideo.requiredForKey)
        assertEquals(PermissionTextKey.MediaAudioDescription, Permission.MediaAudio.descriptionKey)
        assertEquals(PermissionTextKey.MediaAudioRequiredFor, Permission.MediaAudio.requiredForKey)
        assertEquals(PermissionTextKey.StorageReadDescription, Permission.StorageRead.descriptionKey)
        assertEquals(PermissionTextKey.StorageReadRequiredFor, Permission.StorageRead.requiredForKey)
        assertEquals(PermissionTextKey.CameraDescription, Permission.Camera.descriptionKey)
        assertEquals(PermissionTextKey.CameraRequiredFor, Permission.Camera.requiredForKey)
        assertEquals(PermissionTextKey.NotificationsDescription, Permission.Notifications.descriptionKey)
        assertEquals(PermissionTextKey.NotificationsRequiredFor, Permission.Notifications.requiredForKey)
    }
}
