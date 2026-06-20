package com.runninghub.shared.domain.model

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
}
