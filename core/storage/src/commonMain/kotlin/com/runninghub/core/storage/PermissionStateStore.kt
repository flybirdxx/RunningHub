package com.runninghub.core.storage

import kotlinx.coroutines.flow.Flow

/**
 * 记录跨平台权限申请结果的存储边界。
 *
 * Presentation 层只需要知道权限是否已授权、已拒绝或永久拒绝，不应依赖底层
 * DataStore、平台存储或具体持久化实现。本接口位于 core:storage，用于隔离
 * Compose 页面、平台权限控制器与临时 shared DataStore 实现。
 *
 * 并发约束：
 * - 所有写入函数必须保证同一权限 key 在三类集合中互斥。
 * - 调用方通过 [Flow] 观察状态变化，不直接读取底层偏好存储。
 * - Android 会持久化真实授权轨迹；iOS 当前实现以系统权限弹窗为准，可能返回降级状态。
 */
interface PermissionStateStore {
    /**
     * 已授权权限的跨平台 key 集合。
     *
     * @return 权限集合流，调用方应按只读状态处理。
     */
    val grantedPermissions: Flow<Set<String>>

    /** 已拒绝但仍可再次申请的权限 key 集合。 */
    val deniedPermissions: Flow<Set<String>>

    /** 已永久拒绝、需要引导用户到系统设置修改的权限 key 集合。 */
    val permanentlyDeniedPermissions: Flow<Set<String>>

    /**
     * 标记权限已授权。
     *
     * @param permissionKey [Permission.key] 中定义的跨平台稳定权限标识。
     */
    suspend fun markGranted(permissionKey: String)

    /**
     * 标记权限已拒绝但尚未永久拒绝。
     *
     * @param permissionKey [Permission.key] 中定义的跨平台稳定权限标识。
     */
    suspend fun markDenied(permissionKey: String)

    /**
     * 标记权限已永久拒绝。
     *
     * @param permissionKey [Permission.key] 中定义的跨平台稳定权限标识。
     */
    suspend fun markPermanentlyDenied(permissionKey: String)

    /**
     * 清除单个权限的本地状态。
     *
     * @param permissionKey 需要重置的跨平台权限标识。
     */
    suspend fun reset(permissionKey: String)

    /**
     * 清除全部权限状态。
     *
     * 该函数主要用于测试、调试或用户显式重置权限记录，生产流程不应在普通页面切换时调用。
     */
    suspend fun resetAll()

    /**
     * 读取当前权限状态。
     *
     * @param permission 权限模型，包含跨平台稳定 key。
     * @return 当前权限状态；当本地没有记录时返回 [PermissionStatus.UNKNOWN]。
     */
    suspend fun getCurrentStatus(permission: Permission): PermissionStatus
}
