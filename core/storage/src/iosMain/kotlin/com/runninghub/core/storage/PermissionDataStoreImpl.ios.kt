package com.runninghub.core.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * iOS 平台权限状态存储。
 *
 * 当前实现保存本进程内由系统权限回调映射出的跨平台状态，不再把未知权限固定视为已授权。
 * iOS 的照片、视频和文件选择由平台系统弹窗最终裁决；本存储只记录 [Permission.key]，
 * 不保存 PHPhotoLibrary、UIDocumentPicker 或其他平台协议字符串，避免平台细节泄漏到 commonMain。
 *
 * 并发约束：所有写入通过 [Mutex] 串行化，确保同一权限 key 不会同时出现在 granted、
 * denied 和 permanentlyDenied 三个集合中。
 */
class PermissionDataStoreImpl : PermissionDataStore {

    private val mutex = Mutex()
    private val grantedState = MutableStateFlow<Set<String>>(emptySet())
    private val deniedState = MutableStateFlow<Set<String>>(emptySet())
    private val permanentlyDeniedState = MutableStateFlow<Set<String>>(emptySet())

    override val grantedPermissions: Flow<Set<String>> = grantedState
    override val deniedPermissions: Flow<Set<String>> = deniedState
    override val permanentlyDeniedPermissions: Flow<Set<String>> = permanentlyDeniedState

    override suspend fun markGranted(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value + permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
        }
    }

    override suspend fun markDenied(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value + permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
        }
    }

    override suspend fun markPermanentlyDenied(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value + permissionKey
        }
    }

    override suspend fun reset(permissionKey: String) {
        mutex.withLock {
            grantedState.value = grantedState.value - permissionKey
            deniedState.value = deniedState.value - permissionKey
            permanentlyDeniedState.value = permanentlyDeniedState.value - permissionKey
        }
    }

    override suspend fun resetAll() {
        mutex.withLock {
            grantedState.value = emptySet()
            deniedState.value = emptySet()
            permanentlyDeniedState.value = emptySet()
        }
    }

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus =
        when (permission.key) {
            in grantedPermissions.first() -> PermissionStatus.GRANTED
            in deniedPermissions.first() -> PermissionStatus.DENIED
            in permanentlyDeniedPermissions.first() -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.UNKNOWN
        }
}

/**
 * 创建 iOS 平台权限状态存储。
 */
actual fun createPermissionDataStore(): PermissionDataStore = PermissionDataStoreImpl()
