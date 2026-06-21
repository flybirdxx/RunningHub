package com.runninghub.core.storage

/**
 * 权限状态的本地存储实现边界。
 *
 * 该接口用于约束平台 expect/actual 工厂，组合根只把实例暴露为 [PermissionStateStore]。
 * Presentation 层不应依赖本接口或 DataStore 细节，避免平台权限实现泄漏到 UI。
 */
interface PermissionDataStore : PermissionStateStore

/**
 * 创建平台对应的权限状态存储。
 *
 * Android actual 会写入 Preferences DataStore；iOS actual 当前提供系统权限弹窗驱动的轻量实现。
 * 该工厂只应在 DI 组合根中调用，上层业务代码通过 [PermissionStateStore] 使用。
 *
 * @return 平台权限状态存储实例。
 */
expect fun createPermissionDataStore(): PermissionDataStore
