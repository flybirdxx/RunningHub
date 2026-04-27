package com.runninghub.shared.data.local

import com.runninghub.shared.domain.model.Permission
import com.runninghub.shared.domain.model.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class PermissionDataStoreImpl : PermissionDataStore {

    override val grantedPermissions: Flow<Set<String>> = flowOf(emptySet())
    override val deniedPermissions: Flow<Set<String>> = flowOf(emptySet())
    override val permanentlyDeniedPermissions: Flow<Set<String>> = flowOf(emptySet())

    override suspend fun markGranted(manifest: String) {}
    override suspend fun markDenied(manifest: String) {}
    override suspend fun markPermanentlyDenied(manifest: String) {}
    override suspend fun reset(manifest: String) {}
    override suspend fun resetAll() {}

    override suspend fun getCurrentStatus(permission: Permission): PermissionStatus =
        PermissionStatus.GRANTED
}

fun createPermissionDataStore(): PermissionDataStore = PermissionDataStoreImpl()
