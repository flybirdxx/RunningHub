package com.runninghub.shared.data.local

import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS 平台权限状态存储占位实现。
 *
 * 当前 iOS 权限由系统弹窗和 Info.plist 文案处理，本地不持久化授权轨迹，因此所有查询默认返回已授权。
 * 后续接入真实媒体权限状态时，应按 [Permission.key] 写入跨平台稳定标识，而不是保存 iOS 平台字符串。
 */
class PermissionDataStoreImpl : PermissionDataStore {

    override val grantedPermissions: Flow<Set<String>> = flowOf(emptySet())
    override val deniedPermissions: Flow<Set<String>> = flowOf(emptySet())
    override val permanentlyDeniedPermissions: Flow<Set<String>> = flowOf(emptySet())

    override suspend fun markGranted(permissionKey: String) {}
    override suspend fun markDenied(permissionKey: String) {}
    override suspend fun markPermanentlyDenied(permissionKey: String) {}
    override suspend fun reset(permissionKey: String) {}
    override suspend fun resetAll() {}

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus =
        PermissionStatus.GRANTED
}

/**
 * 创建 iOS 平台权限状态存储。
 */
actual fun createPermissionDataStore(): PermissionDataStore = PermissionDataStoreImpl()
