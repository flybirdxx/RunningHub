package com.runninghub.app.platform

import com.runninghub.core.storage.Permission
import com.runninghub.core.storage.PermissionStateStore
import com.runninghub.core.storage.PermissionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusRestricted
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusProvisional
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class IosPermissionAuthorizationMappingTest {
    @Test
    fun photoLimitedAndAuthorizedAreGranted() {
        assertEquals(
            IosPermissionAuthorizationDecision.GRANTED,
            iosPhotoAuthorizationDecision(PHAuthorizationStatusAuthorized),
        )
        assertEquals(
            IosPermissionAuthorizationDecision.GRANTED,
            iosPhotoAuthorizationDecision(PHAuthorizationStatusLimited),
        )
    }

    @Test
    fun photoDeniedAndRestrictedRequireSettings() {
        assertEquals(
            IosPermissionAuthorizationDecision.PERMANENTLY_DENIED,
            iosPhotoAuthorizationDecision(PHAuthorizationStatusDenied),
        )
        assertEquals(
            IosPermissionAuthorizationDecision.PERMANENTLY_DENIED,
            iosPhotoAuthorizationDecision(PHAuthorizationStatusRestricted),
        )
    }

    @Test
    fun limitedPhotoManagementUsesLimitedPickerOnlyForPhotoPermissions() {
        assertEquals(
            IosPhotoLibraryManagementTarget.LIMITED_LIBRARY_PICKER,
            iosPhotoLibraryManagementTarget(Permission.MediaImages, PHAuthorizationStatusLimited),
        )
        assertEquals(
            IosPhotoLibraryManagementTarget.LIMITED_LIBRARY_PICKER,
            iosPhotoLibraryManagementTarget(Permission.MediaVideo, PHAuthorizationStatusLimited),
        )
        assertEquals(
            IosPhotoLibraryManagementTarget.APP_SETTINGS,
            iosPhotoLibraryManagementTarget(Permission.Camera, PHAuthorizationStatusLimited),
        )
        assertEquals(
            IosPhotoLibraryManagementTarget.APP_SETTINGS,
            iosPhotoLibraryManagementTarget(Permission.MediaImages, PHAuthorizationStatusAuthorized),
        )
    }

    @Test
    fun cameraDeniedAndRestrictedRequireSettings() {
        assertEquals(
            IosPermissionAuthorizationDecision.PERMANENTLY_DENIED,
            iosCameraAuthorizationDecision(AVAuthorizationStatusDenied),
        )
        assertEquals(
            IosPermissionAuthorizationDecision.PERMANENTLY_DENIED,
            iosCameraAuthorizationDecision(AVAuthorizationStatusRestricted),
        )
    }

    @Test
    fun notificationProvisionalAndAuthorizedAreGranted() {
        assertEquals(
            IosPermissionAuthorizationDecision.GRANTED,
            iosNotificationAuthorizationDecision(UNAuthorizationStatusAuthorized),
        )
        assertEquals(
            IosPermissionAuthorizationDecision.GRANTED,
            iosNotificationAuthorizationDecision(UNAuthorizationStatusProvisional),
        )
    }

    @Test
    fun notificationDeniedRequiresSettings() {
        assertEquals(
            IosPermissionAuthorizationDecision.PERMANENTLY_DENIED,
            iosNotificationAuthorizationDecision(UNAuthorizationStatusDenied),
        )
    }

    @Test
    fun photoAuthorizationResultsAreRecordedInPermissionStateStore() = runTest {
        val permissionStateStore = RecordingPermissionStateStore()
        val callbacks = mutableListOf<String>()

        recordIosPermissionAuthorizationDecision(
            permissionStateStore = permissionStateStore,
            scope = this,
            permission = Permission.MediaImages,
            decision = iosPhotoAuthorizationDecision(PHAuthorizationStatusAuthorized),
            onGranted = { callbacks += "authorized-granted" },
            onDenied = { callbacks += "authorized-denied" },
            onPermanentlyDenied = { callbacks += "authorized-permanently-denied" },
        )
        recordIosPermissionAuthorizationDecision(
            permissionStateStore = permissionStateStore,
            scope = this,
            permission = Permission.MediaImages,
            decision = iosPhotoAuthorizationDecision(PHAuthorizationStatusLimited),
            onGranted = { callbacks += "limited-granted" },
            onDenied = { callbacks += "limited-denied" },
            onPermanentlyDenied = { callbacks += "limited-permanently-denied" },
        )
        recordIosPermissionAuthorizationDecision(
            permissionStateStore = permissionStateStore,
            scope = this,
            permission = Permission.MediaImages,
            decision = iosPhotoAuthorizationDecision(PHAuthorizationStatusDenied),
            onGranted = { callbacks += "denied-granted" },
            onDenied = { callbacks += "denied-denied" },
            onPermanentlyDenied = { callbacks += "denied-permanently-denied" },
        )

        advanceUntilIdle()

        assertEquals(
            listOf(
                "authorized-granted",
                "limited-granted",
                "denied-permanently-denied",
            ),
            callbacks,
        )
        assertEquals(
            listOf(
                "granted:${Permission.MediaImages.key}",
                "granted:${Permission.MediaImages.key}",
                "permanentlyDenied:${Permission.MediaImages.key}",
            ),
            permissionStateStore.events,
        )
        assertEquals(
            PermissionStatus.PERMANENTLY_DENIED,
            permissionStateStore.getCurrentStatus(Permission.MediaImages),
        )
    }
}

private class RecordingPermissionStateStore : PermissionStateStore {
    override val grantedPermissions = MutableStateFlow<Set<String>>(emptySet())
    override val deniedPermissions = MutableStateFlow<Set<String>>(emptySet())
    override val permanentlyDeniedPermissions = MutableStateFlow<Set<String>>(emptySet())
    val events = mutableListOf<String>()

    override suspend fun markGranted(permissionKey: String) {
        events += "granted:$permissionKey"
        grantedPermissions.value = setOf(permissionKey)
        deniedPermissions.value = emptySet()
        permanentlyDeniedPermissions.value = emptySet()
    }

    override suspend fun markDenied(permissionKey: String) {
        events += "denied:$permissionKey"
        grantedPermissions.value = emptySet()
        deniedPermissions.value = setOf(permissionKey)
        permanentlyDeniedPermissions.value = emptySet()
    }

    override suspend fun markPermanentlyDenied(permissionKey: String) {
        events += "permanentlyDenied:$permissionKey"
        grantedPermissions.value = emptySet()
        deniedPermissions.value = emptySet()
        permanentlyDeniedPermissions.value = setOf(permissionKey)
    }

    override suspend fun reset(permissionKey: String) {
        grantedPermissions.value = grantedPermissions.value - permissionKey
        deniedPermissions.value = deniedPermissions.value - permissionKey
        permanentlyDeniedPermissions.value = permanentlyDeniedPermissions.value - permissionKey
    }

    override suspend fun resetAll() {
        grantedPermissions.value = emptySet()
        deniedPermissions.value = emptySet()
        permanentlyDeniedPermissions.value = emptySet()
    }

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus =
        when (permission.key) {
            in grantedPermissions.value -> PermissionStatus.GRANTED
            in deniedPermissions.value -> PermissionStatus.DENIED
            in permanentlyDeniedPermissions.value -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.UNKNOWN
        }
}
