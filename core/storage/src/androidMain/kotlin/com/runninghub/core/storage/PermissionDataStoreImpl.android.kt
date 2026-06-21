package com.runninghub.core.storage

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val KEY_GRANTED = "permission_granted"
private const val KEY_DENIED = "permission_denied"
private const val KEY_PERMANENTLY_DENIED = "permission_permanently_denied"

private var permissionDataStoreInstance: PermissionDataStore? = null

/**
 * Android 平台权限状态存储。
 *
 * 本实现只持久化 [Permission.key] 这样的跨平台稳定标识，不再把 Android Manifest 字符串写入
 * 领域状态集合。读取时会兼容旧版本中已经保存的 Manifest 字符串，防止升级后丢失用户授权轨迹。
 */
class PermissionDataStoreImpl : PermissionDataStore {

    private val dataStore get() = createDataStore()

    companion object {
        private val KEY_GRANTED_SET = stringSetPreferencesKey(KEY_GRANTED)
        private val KEY_DENIED_SET = stringSetPreferencesKey(KEY_DENIED)
        private val KEY_PERM_DENIED_SET = stringSetPreferencesKey(KEY_PERMANENTLY_DENIED)
    }

    override val grantedPermissions = dataStore.data.map { it[KEY_GRANTED_SET] ?: emptySet() }
    override val deniedPermissions = dataStore.data.map { it[KEY_DENIED_SET] ?: emptySet() }
    override val permanentlyDeniedPermissions = dataStore.data.map { it[KEY_PERM_DENIED_SET] ?: emptySet() }

    override suspend fun markGranted(permissionKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()) + permissionKey
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
        }
    }

    override suspend fun markDenied(permissionKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()) + permissionKey
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
        }
    }

    override suspend fun markPermanentlyDenied(permissionKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()) + permissionKey
        }
    }

    override suspend fun reset(permissionKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()).withoutPermission(permissionKey)
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus {
        val granted = grantedPermissions.first()
        val denied = deniedPermissions.first()
        val permanentlyDenied = permanentlyDeniedPermissions.first()
        return when {
            granted.containsPermission(permission) -> PermissionStatus.GRANTED
            denied.containsPermission(permission) -> PermissionStatus.DENIED
            permanentlyDenied.containsPermission(permission) -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.UNKNOWN
        }
    }

    private fun Set<String>.containsPermission(permission: Permission): Boolean =
        permission.key in this || permission.androidManifestPermission in this

    private fun Set<String>.withoutPermission(permissionKey: String): Set<String> {
        val permission = Permission.fromKey(permissionKey)
        return if (permission == null) {
            this - permissionKey
        } else {
            this - permissionKey - permission.androidManifestPermission
        }
    }
}

/**
 * 创建 Android 平台权限状态存储。
 *
 * 该工厂由 DI 组合根调用并复用单例，避免多个 DataStore 包装实例同时写入同一偏好文件。
 */
actual fun createPermissionDataStore(): PermissionDataStore =
    permissionDataStoreInstance ?: PermissionDataStoreImpl().also { permissionDataStoreInstance = it }
