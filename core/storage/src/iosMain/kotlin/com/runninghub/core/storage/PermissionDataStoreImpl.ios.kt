package com.runninghub.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSUserDefaults

private const val KEY_GRANTED = "permission_granted"
private const val KEY_DENIED = "permission_denied"
private const val KEY_PERMANENTLY_DENIED = "permission_permanently_denied"
private const val PERMISSION_KEY_SEPARATOR = ","

private var permissionDataStoreInstance: PermissionDataStore? = null

/**
 * iOS 平台权限状态存储。
 *
 * 当前实现把系统权限回调映射出的跨平台状态持久化到 [NSUserDefaults]，
 * 不再把未知权限固定视为已授权，也不会在进程重启后丢失用户拒绝或永久拒绝轨迹。
 * iOS 的照片、视频和文件选择由平台系统弹窗最终裁决；本存储只记录 [Permission.key]，
 * 不保存 PHPhotoLibrary、UIDocumentPicker 或其他平台协议字符串，避免平台细节泄漏到 commonMain。
 *
 * 并发约束：所有写入通过 [Mutex] 串行化，确保同一权限 key 不会同时出现在 granted、
 * denied 和 permanentlyDenied 三个集合中。
 */
class PermissionDataStoreImpl : PermissionDataStore {

    private val mutex = Mutex()
    private val defaults = NSUserDefaults.standardUserDefaults
    private val grantedState = MutableStateFlow(readPermissionSet(KEY_GRANTED))
    private val deniedState = MutableStateFlow(readPermissionSet(KEY_DENIED))
    private val permanentlyDeniedState = MutableStateFlow(readPermissionSet(KEY_PERMANENTLY_DENIED))

    override val grantedPermissions: Flow<Set<String>> = grantedState
    override val deniedPermissions: Flow<Set<String>> = deniedState
    override val permanentlyDeniedPermissions: Flow<Set<String>> = permanentlyDeniedState

    override suspend fun markGranted(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value + permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
            persistState()
        }
    }

    override suspend fun markDenied(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value + permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
            persistState()
        }
    }

    override suspend fun markPermanentlyDenied(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value + permissionKey
            persistState()
        }
    }

    override suspend fun reset(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
            persistState()
        }
    }

    override suspend fun resetAll() {
        mutex.withLock {
            grantedState.value = emptySet()
            deniedState.value = emptySet()
            permanentlyDeniedState.value = emptySet()
            persistState()
        }
    }

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus =
        when (permission.key) {
            in grantedPermissions.first() -> PermissionStatus.GRANTED
            in deniedPermissions.first() -> PermissionStatus.DENIED
            in permanentlyDeniedPermissions.first() -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.UNKNOWN
        }

    private fun readPermissionSet(key: String): Set<String> =
        defaults.stringForKey(key)
            ?.split(PERMISSION_KEY_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()

    /**
     * 将三个互斥集合写回 NSUserDefaults。
     *
     * iOS 权限最终仍以系统状态为准；这里持久化的是应用侧最近一次回调结果，
     * 用于页面重建或进程恢复后的引导文案，不作为绕过系统权限检查的依据。
     */
    private fun persistState() {
        defaults.setObject(grantedState.value.encodePermissionSet(), forKey = KEY_GRANTED)
        defaults.setObject(deniedState.value.encodePermissionSet(), forKey = KEY_DENIED)
        defaults.setObject(
            permanentlyDeniedState.value.encodePermissionSet(),
            forKey = KEY_PERMANENTLY_DENIED,
        )
        defaults.synchronize()
    }

    private fun Set<String>.encodePermissionSet(): String =
        sorted().joinToString(PERMISSION_KEY_SEPARATOR)
}

/**
 * 创建 iOS 平台权限状态存储。
 */
actual fun createPermissionDataStore(): PermissionDataStore =
    permissionDataStoreInstance ?: PermissionDataStoreImpl().also { permissionDataStoreInstance = it }
