package com.runninghub.shared.domain.repository

import com.runninghub.core.storage.CredentialStore

interface SettingsRepository : CredentialStore {
    suspend fun getLastKnownCoins(): String?
    suspend fun setLastKnownCoins(coins: String)
    suspend fun clearLastKnownCoins()

    suspend fun getQuickCreateDraft(): String?
    suspend fun saveQuickCreateDraft(json: String)
    suspend fun clearQuickCreateDraft()

    override suspend fun clearAll()
}
