package com.runninghub.core.storage

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 创建 Android Keystore backed 的安全凭据存储。
 *
 * 明文凭据只在调用进程内短暂存在；落盘内容为 AES-GCM 密文和 IV。Keystore 密钥不可导出，
 * SharedPreferences 只用于保存密文载荷，不承载 API Key、Cookie 或 token 明文。
 */
actual fun createSecureCredentialStore(): CredentialStore =
    AndroidSecureCredentialStore(
        preferences = storageApplicationContext().getSharedPreferences(
            SECURE_CREDENTIALS_PREFS,
            android.content.Context.MODE_PRIVATE,
        ),
    )

/**
 * 使用 Android Keystore 保护敏感认证凭据的 [CredentialStore] 实现。
 *
 * 本实现位于 androidMain，只服务平台组合根注入的凭据边界；余额缓存和快捷创作草稿继续由
 * [PreferencesSettingsStore] 保存。读取失败通常意味着密钥失效、系统还原或密文损坏，此时会删除
 * 对应密文并返回 `null`，由上层会话恢复流程按未登录处理。
 *
 * @param preferences 应用私有 SharedPreferences，只保存密文和 IV，不保存明文凭据。
 */
internal class AndroidSecureCredentialStore(
    private val preferences: SharedPreferences,
) : CredentialStore {
    override suspend fun getApiKey(): String? = readEncrypted(KEY_API_KEY)

    override suspend fun setApiKey(key: String) {
        writeEncrypted(KEY_API_KEY, key)
    }

    override suspend fun clearApiKey() {
        clearValue(KEY_API_KEY)
    }

    override suspend fun getEnterpriseApiKey(): String? = readEncrypted(KEY_ENTERPRISE_API_KEY)

    override suspend fun setEnterpriseApiKey(key: String) {
        writeEncrypted(KEY_ENTERPRISE_API_KEY, key)
    }

    override suspend fun clearEnterpriseApiKey() {
        clearValue(KEY_ENTERPRISE_API_KEY)
    }

    override suspend fun getCookie(): String? = readEncrypted(KEY_COOKIE)

    override suspend fun setCookie(cookie: String) {
        writeEncrypted(KEY_COOKIE, cookie)
    }

    override suspend fun clearCookie() {
        clearValue(KEY_COOKIE)
    }

    override suspend fun getAuthToken(): String? = readEncrypted(KEY_AUTH_TOKEN)

    override suspend fun setAuthToken(token: String) {
        writeEncrypted(KEY_AUTH_TOKEN, token)
    }

    override suspend fun clearAuthToken() {
        clearValue(KEY_AUTH_TOKEN)
    }

    override suspend fun getRefreshToken(): String? = readEncrypted(KEY_REFRESH_TOKEN)

    override suspend fun setRefreshToken(token: String) {
        writeEncrypted(KEY_REFRESH_TOKEN, token)
    }

    override suspend fun clearRefreshToken() {
        clearValue(KEY_REFRESH_TOKEN)
    }

    override suspend fun isLoggedIn(): Boolean =
        !getAuthToken().isNullOrEmpty()

    override suspend fun clearAll() {
        preferences.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_ENTERPRISE_API_KEY)
            .remove(KEY_COOKIE)
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    private fun writeEncrypted(key: String, value: String) {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

        val cipherText = cipher.doFinal(value.encodeToByteArray())
        val payload = "${cipher.iv.encodeBase64()}:${cipherText.encodeBase64()}"
        preferences.edit().putString(key, payload).apply()
    }

    private fun readEncrypted(key: String): String? {
        val payload = preferences.getString(key, null) ?: return null
        val parts = payload.split(":", limit = 2)
        if (parts.size != 2) {
            clearValue(key)
            return null
        }

        return try {
            val iv = parts[0].decodeBase64()
            val cipherText = parts[1].decodeBase64()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )
            cipher.doFinal(cipherText).decodeToString()
        } catch (_: IllegalArgumentException) {
            // Base64 或 UTF-8 载荷损坏时删除密文；不能把原始密文写入日志或异常消息。
            clearValue(key)
            null
        } catch (_: GeneralSecurityException) {
            // Keystore 密钥失效时按会话不可恢复处理，避免崩溃并防止旧密文反复触发失败。
            clearValue(key)
            null
        }
    }

    private fun clearValue(key: String) {
        preferences.edit().remove(key).apply()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun ByteArray.encodeBase64(): String =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decodeBase64(): ByteArray =
        Base64.decode(this, Base64.NO_WRAP)
}

private const val SECURE_CREDENTIALS_PREFS = "runninghub_secure_credentials"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "runninghub_credentials_aes_gcm"
private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH_BITS = 128
private const val KEY_API_KEY = "api_key"
private const val KEY_ENTERPRISE_API_KEY = "enterprise_api_key"
private const val KEY_COOKIE = "user_cookie"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
