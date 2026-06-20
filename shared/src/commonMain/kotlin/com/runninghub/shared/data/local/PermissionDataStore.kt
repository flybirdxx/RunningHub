package com.runninghub.shared.data.local

import com.runninghub.shared.domain.permission.PermissionStateStore

/**
 * 权限状态的 DataStore 实现边界。
 *
 * 该接口保留在 data/local，用于约束 expect/actual 工厂和平台实现文件；
 * Presentation 层应依赖 Domain 层的 [PermissionStateStore]，避免直接触达
 * DataStore 命名空间。
 */
interface PermissionDataStore : PermissionStateStore

/**
 * 创建平台对应的权限状态存储。
 *
 * Android actual 会写入 DataStore；iOS actual 当前提供系统权限弹窗驱动的轻量实现。
 * 该工厂只应在 DI 组合根中调用，上层业务代码通过 [PermissionStateStore] 使用。
 */
expect fun createPermissionDataStore(): PermissionDataStore
