package com.runninghub.app.platform

import com.runninghub.app.ui.component.MediaType
import com.runninghub.core.storage.Permission
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionControllerContractTest {
    @Test
    fun pickMediaSeparatesPickerCancellationFromPermanentPermissionDenial() {
        val controller = PermanentlyDeniedMediaPermissionController()
        var cancelledOrDeniedCount = 0
        var permanentlyDeniedCount = 0

        controller.pickMedia(
            mediaPermission = Permission.MediaImages,
            mediaType = MediaType.IMAGE,
            onSuccess = {},
            onPermissionDenied = { cancelledOrDeniedCount += 1 },
            onPermissionPermanentlyDenied = { permanentlyDeniedCount += 1 },
        )

        assertEquals(0, cancelledOrDeniedCount)
        assertEquals(1, permanentlyDeniedCount)
    }

    @Test
    fun pickMediaExposesPickerCancellationWithoutPermissionDenial() {
        val controller = CancelledMediaPermissionController()

        assertPickerCancellationIsNotPermissionDenial(controller, Permission.MediaImages, MediaType.IMAGE)
        assertPickerCancellationIsNotPermissionDenial(controller, Permission.MediaVideo, MediaType.VIDEO)
        assertPickerCancellationIsNotPermissionDenial(controller, Permission.MediaAudio, MediaType.AUDIO)
    }

    @Test
    fun openPermissionSettingsDefaultsToAppSettings() {
        val controller = SettingsRecordingPermissionController()

        controller.openPermissionSettings(Permission.MediaImages)

        assertEquals(1, controller.openAppSettingsCount)
    }

    private fun assertPickerCancellationIsNotPermissionDenial(
        controller: PermissionController,
        mediaPermission: Permission,
        mediaType: MediaType,
    ) {
        var cancelledCount = 0
        var deniedCount = 0
        var permanentlyDeniedCount = 0

        controller.pickMedia(
            mediaPermission = mediaPermission,
            mediaType = mediaType,
            onSuccess = {},
            onPermissionDenied = { deniedCount += 1 },
            onPickerCancelled = { cancelledCount += 1 },
            onPermissionPermanentlyDenied = { permanentlyDeniedCount += 1 },
        )

        assertEquals(1, cancelledCount)
        assertEquals(0, deniedCount)
        assertEquals(0, permanentlyDeniedCount)
    }

    private class PermanentlyDeniedMediaPermissionController : PermissionController {
        override fun pickMedia(
            mediaPermission: Permission,
            mediaType: MediaType,
            onSuccess: (String) -> Unit,
            onPermissionDenied: () -> Unit,
            onPickerCancelled: () -> Unit,
            onPermissionPermanentlyDenied: () -> Unit,
        ) {
            onPermissionPermanentlyDenied()
        }

        override fun checkAndRequest(
            permission: Permission,
            onGranted: () -> Unit,
            onDenied: () -> Unit,
            onPermanentlyDenied: () -> Unit,
        ) = Unit

        override fun openAppSettings() = Unit
    }

    private class CancelledMediaPermissionController : PermissionController {
        override fun pickMedia(
            mediaPermission: Permission,
            mediaType: MediaType,
            onSuccess: (String) -> Unit,
            onPermissionDenied: () -> Unit,
            onPickerCancelled: () -> Unit,
            onPermissionPermanentlyDenied: () -> Unit,
        ) {
            onPickerCancelled()
        }

        override fun checkAndRequest(
            permission: Permission,
            onGranted: () -> Unit,
            onDenied: () -> Unit,
            onPermanentlyDenied: () -> Unit,
        ) = Unit

        override fun openAppSettings() = Unit
    }

    private class SettingsRecordingPermissionController : PermissionController {
        var openAppSettingsCount = 0

        override fun pickMedia(
            mediaPermission: Permission,
            mediaType: MediaType,
            onSuccess: (String) -> Unit,
            onPermissionDenied: () -> Unit,
            onPickerCancelled: () -> Unit,
            onPermissionPermanentlyDenied: () -> Unit,
        ) = Unit

        override fun checkAndRequest(
            permission: Permission,
            onGranted: () -> Unit,
            onDenied: () -> Unit,
            onPermanentlyDenied: () -> Unit,
        ) = Unit

        override fun openAppSettings() {
            openAppSettingsCount += 1
        }
    }
}
