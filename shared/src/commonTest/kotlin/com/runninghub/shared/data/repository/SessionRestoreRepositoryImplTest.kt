package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionRestoreRepositoryImplTest {

    @Test
    fun `hasRestorableSession returns true when auth token exists`() = runBlocking {
        val repository = SessionRestoreRepositoryImpl(FakeCredentialStore(authToken = "token-1"))

        assertTrue(repository.hasRestorableSession())
    }

    @Test
    fun `hasRestorableSession returns false when auth token is blank`() = runBlocking {
        val repository = SessionRestoreRepositoryImpl(FakeCredentialStore(authToken = " "))

        assertFalse(repository.hasRestorableSession())
    }

    @Test
    fun `hasRestorableSession returns false when auth token is missing`() = runBlocking {
        val repository = SessionRestoreRepositoryImpl(FakeCredentialStore(authToken = null))

        assertFalse(repository.hasRestorableSession())
    }

    private class FakeCredentialStore(
        private var authToken: String?,
    ) : CredentialStore {
        override suspend fun getApiKey(): String? = null
        override suspend fun setApiKey(key: String) = Unit
        override suspend fun clearApiKey() = Unit
        override suspend fun getEnterpriseApiKey(): String? = null
        override suspend fun setEnterpriseApiKey(key: String) = Unit
        override suspend fun clearEnterpriseApiKey() = Unit
        override suspend fun getCookie(): String? = null
        override suspend fun setCookie(cookie: String) = Unit
        override suspend fun clearCookie() = Unit
        override suspend fun getAuthToken(): String? = authToken
        override suspend fun setAuthToken(token: String) {
            authToken = token
        }
        override suspend fun clearAuthToken() {
            authToken = null
        }
        override suspend fun getRefreshToken(): String? = null
        override suspend fun setRefreshToken(token: String) = Unit
        override suspend fun clearRefreshToken() = Unit
        override suspend fun isLoggedIn(): Boolean = !authToken.isNullOrBlank()
        override suspend fun clearAll() {
            authToken = null
        }
    }
}
