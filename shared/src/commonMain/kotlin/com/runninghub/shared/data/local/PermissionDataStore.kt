package com.runninghub.shared.data.local

import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import kotlinx.coroutines.flow.Flow

interface PermissionDataStore {
    val grantedPermissions: Flow<Set<String>>
    val deniedPermissions: Flow<Set<String>>
    val permanentlyDeniedPermissions: Flow<Set<String>>

    suspend fun markGranted(manifest: String)
    suspend fun markDenied(manifest: String)
    suspend fun markPermanentlyDenied(manifest: String)
    suspend fun reset(manifest: String)
    suspend fun resetAll()

    suspend fun getCurrentStatus(permission: Permission): PermissionStatus
}

expect fun createPermissionDataStore(): PermissionDataStore
