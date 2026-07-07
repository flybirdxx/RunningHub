package com.runninghub.app.platform

import com.runninghub.core.storage.Permission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaSaveResultRecoveryTest {
    @Test
    fun photoPermissionDeniedSuggestsMediaImagePermissionRecovery() {
        val result = MediaSaveResult.Failure(MediaSaveFailureReason.PHOTO_PERMISSION_DENIED)

        assertEquals(Permission.MediaImages, result.recoverablePermission())
    }

    @Test
    fun writeFailureDoesNotSuggestPermissionRecovery() {
        val result = MediaSaveResult.Failure(MediaSaveFailureReason.WRITE_FAILED)

        assertNull(result.recoverablePermission())
    }

    @Test
    fun successfulSaveDoesNotSuggestPermissionRecovery() {
        assertNull(MediaSaveResult.Success.recoverablePermission())
    }
}
