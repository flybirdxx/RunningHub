package com.runninghub.shared.data.repository

import com.runninghub.core.storage.BalanceCache
import com.runninghub.shared.domain.usecase.GetLastKnownBalanceUseCase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceSnapshotRepositoryImplTest {

    @Test
    fun `getLastKnownCoins returns cached balance text`() = runBlocking {
        val repository = BalanceSnapshotRepositoryImpl(FakeBalanceCache(coins = "88.6"))

        assertEquals("88.6", repository.getLastKnownCoins())
    }

    @Test
    fun `getLastKnownCoins keeps null when no cached balance exists`() = runBlocking {
        val repository = BalanceSnapshotRepositoryImpl(FakeBalanceCache(coins = null))

        assertEquals(null, repository.getLastKnownCoins())
    }

    @Test
    fun `use case returns repository value without ui placeholder`() = runBlocking {
        val repository = BalanceSnapshotRepositoryImpl(FakeBalanceCache(coins = null))
        val useCase = GetLastKnownBalanceUseCase(repository)

        assertEquals(null, useCase())
    }

    private class FakeBalanceCache(
        private var coins: String?,
    ) : BalanceCache {
        override suspend fun getLastKnownCoins(): String? =
            coins

        override suspend fun setLastKnownCoins(coins: String) {
            this.coins = coins
        }

        override suspend fun clearLastKnownCoins() {
            coins = null
        }
    }
}
