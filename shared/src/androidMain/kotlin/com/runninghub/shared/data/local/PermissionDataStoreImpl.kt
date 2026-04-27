package com.runninghub.shared.data.local

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val KEY_GRANTED = "permission_granted"
private const val KEY_DENIED = "permission_denied"
private const val KEY_PERMANENTLY_DENIED = "permission_permanently_denied"

private var instance: PermissionDataStore? = null

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

    override suspend fun markGranted(manifest: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()) + manifest
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()) - manifest
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()) - manifest
        }
    }

    override suspend fun markDenied(manifest: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()) - manifest
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()) + manifest
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()) - manifest
        }
    }

    override suspend fun markPermanentlyDenied(manifest: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()) - manifest
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()) - manifest
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()) + manifest
        }
    }

    override suspend fun reset(manifest: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GRANTED_SET] = (prefs[KEY_GRANTED_SET] ?: emptySet()) - manifest
            prefs[KEY_DENIED_SET] = (prefs[KEY_DENIED_SET] ?: emptySet()) - manifest
            prefs[KEY_PERM_DENIED_SET] = (prefs[KEY_PERM_DENIED_SET] ?: emptySet()) - manifest
        }
    }

    override suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus {
        val granted = grantedPermissions.first()
        val denied = deniedPermissions.first()
        val permDenied = permanentlyDeniedPermissions.first()
        return when (permission.androidManifest) {
            in granted -> PermissionStatus.GRANTED
            in denied -> PermissionStatus.DENIED
            in permDenied -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.UNKNOWN
        }
    }
}

actual fun createPermissionDataStore(): PermissionDataStore {
    return instance ?: PermissionDataStoreImpl().also { instance = it }
}
