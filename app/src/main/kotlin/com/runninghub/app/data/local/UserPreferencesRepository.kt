package com.runninghub.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_ENTERPRISE_API_KEY = "enterprise_api_key"
        private const val KEY_COOKIE = "user_cookie"
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
    }

    fun saveEnterpriseApiKey(key: String) {
        prefs.edit().putString(KEY_ENTERPRISE_API_KEY, key).apply()
    }

    fun getEnterpriseApiKey(): String? {
        return prefs.getString(KEY_ENTERPRISE_API_KEY, null)
    }

    fun clearEnterpriseApiKey() {
        prefs.edit().remove(KEY_ENTERPRISE_API_KEY).apply()
    }

    fun saveCookie(cookie: String) {
        prefs.edit().putString(KEY_COOKIE, cookie).apply()
    }

    fun getCookie(): String? {
        return prefs.getString(KEY_COOKIE, null)
    }

    fun clearCookie() {
        prefs.edit().remove(KEY_COOKIE).apply()
    }
}
