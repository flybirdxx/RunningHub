package com.runninghub.shared.data.repository

import com.runninghub.core.storage.CredentialStore
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileCredentialRepositoryImplTest {

    @Test
    fun `bindApiKey stores api key in credential store`() = runBlocking {
        val credentialStore = FakeCredentialStore()
        val repository = ProfileCredentialRepositoryImpl(credentialStore)

        repository.bindApiKey("api-key-1")

        assertEquals("api-key-1", credentialStore.apiKey)
    }

    @Test
    fun `bindCookie stores cookie in credential store`() = runBlocking {
        val credentialStore = FakeCredentialStore()
        val repository = ProfileCredentialRepositoryImpl(credentialStore)

        repository.bindCookie("cookie-1")

        assertEquals("cookie-1", credentialStore.cookie)
    }

    @Test
    fun `clearCreationCredentials clears api keys and cookie together`() = runBlocking {
        val credentialStore = FakeCredentialStore().apply {
            apiKey = "api-key-1"
            enterpriseApiKey = "enterprise-key-1"
            cookie = "cookie-1"
        }
        val repository = ProfileCredentialRepositoryImpl(credentialStore)

        repository.clearCreationCredentials()

        assertEquals(null, credentialStore.apiKey)
        assertEquals(null, credentialStore.enterpriseApiKey)
        assertEquals(null, credentialStore.cookie)
    }

    private class FakeCredentialStore : CredentialStore {
        var apiKey: String? = null
        var enterpriseApiKey: String? = null
        var cookie: String? = null
        var authToken: String? = null
        var refreshToken: String? = null

        override suspend fun getApiKey(): String? = apiKey

        override suspend fun setApiKey(key: String) {
            apiKey = key
        }

        override suspend fun clearApiKey() {
            apiKey = null
        }

        override suspend fun getEnterpriseApiKey(): String? = enterpriseApiKey

        override suspend fun setEnterpriseApiKey(key: String) {
            enterpriseApiKey = key
        }

        override suspend fun clearEnterpriseApiKey() {
            enterpriseApiKey = null
        }

        override suspend fun getCookie(): String? = cookie

        override suspend fun setCookie(cookie: String) {
            this.cookie = cookie
        }

        override suspend fun clearCookie() {
            cookie = null
        }

        override suspend fun getAuthToken(): String? = authToken

        override suspend fun setAuthToken(token: String) {
            authToken = token
        }

        override suspend fun clearAuthToken() {
            authToken = null
        }

        override suspend fun getRefreshToken(): String? = refreshToken

        override suspend fun setRefreshToken(token: String) {
            refreshToken = token
        }

        override suspend fun clearRefreshToken() {
            refreshToken = null
        }

        override suspend fun isLoggedIn(): Boolean =
            !authToken.isNullOrBlank()

        override suspend fun clearAll() {
            apiKey = null
            enterpriseApiKey = null
            cookie = null
            authToken = null
            refreshToken = null
        }
    }
}
