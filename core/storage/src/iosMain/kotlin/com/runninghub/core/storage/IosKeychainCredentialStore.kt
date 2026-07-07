package com.runninghub.core.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * 创建 iOS Keychain backed 的安全凭据存储。
 *
 * Keychain 负责持久化 API Key、Cookie、access token 和 refresh token；普通 Preferences DataStore
 * 继续只保存余额快照、草稿等非敏感数据。
 */
actual fun createSecureCredentialStore(): CredentialStore = IosKeychainCredentialStore()

/**
 * 使用 iOS Keychain 保存敏感认证凭据的 [CredentialStore] 实现。
 *
 * 本实现按固定 service 和 account 写入 Generic Password 条目，并使用
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` 限制备份迁移范围。读取不到条目时返回 `null`；
 * 写入前先删除旧条目，避免 Keychain duplicate item 影响 token 刷新后的覆盖语义。
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosKeychainCredentialStore : CredentialStore {
    override suspend fun getApiKey(): String? = readValue(KEY_API_KEY)

    override suspend fun setApiKey(key: String) {
        writeValue(KEY_API_KEY, key)
    }

    override suspend fun clearApiKey() {
        deleteValue(KEY_API_KEY)
    }

    override suspend fun getEnterpriseApiKey(): String? = readValue(KEY_ENTERPRISE_API_KEY)

    override suspend fun setEnterpriseApiKey(key: String) {
        writeValue(KEY_ENTERPRISE_API_KEY, key)
    }

    override suspend fun clearEnterpriseApiKey() {
        deleteValue(KEY_ENTERPRISE_API_KEY)
    }

    override suspend fun getCookie(): String? = readValue(KEY_COOKIE)

    override suspend fun setCookie(cookie: String) {
        writeValue(KEY_COOKIE, cookie)
    }

    override suspend fun clearCookie() {
        deleteValue(KEY_COOKIE)
    }

    override suspend fun getAuthToken(): String? = readValue(KEY_AUTH_TOKEN)

    override suspend fun setAuthToken(token: String) {
        writeValue(KEY_AUTH_TOKEN, token)
    }

    override suspend fun clearAuthToken() {
        deleteValue(KEY_AUTH_TOKEN)
    }

    override suspend fun getRefreshToken(): String? = readValue(KEY_REFRESH_TOKEN)

    override suspend fun setRefreshToken(token: String) {
        writeValue(KEY_REFRESH_TOKEN, token)
    }

    override suspend fun clearRefreshToken() {
        deleteValue(KEY_REFRESH_TOKEN)
    }

    override suspend fun isLoggedIn(): Boolean =
        !getAuthToken().isNullOrEmpty()

    override suspend fun clearAll() {
        deleteValue(KEY_API_KEY)
        deleteValue(KEY_ENTERPRISE_API_KEY)
        deleteValue(KEY_COOKIE)
        deleteValue(KEY_AUTH_TOKEN)
        deleteValue(KEY_REFRESH_TOKEN)
    }

    private fun readValue(account: String): String? = withKeychainQuery(
        account = account,
        includeReadOptions = true,
    ) { query ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status == errSecItemNotFound) return@memScoped null
            if (status != errSecSuccess) return@memScoped null

            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            data.toUtf8String()
        }
    }

    private fun writeValue(account: String, value: String) {
        // Keychain Add 不会覆盖同 account 旧值；先删后写可以保持 token 刷新后的最后写入语义。
        deleteValue(account)
        val status = withKeychainQuery(account = account, value = value) { query ->
            SecItemAdd(query, null)
        }
        if (status != errSecSuccess) {
            deleteValue(account)
            throw IllegalStateException("KEYCHAIN_WRITE_FAILED_STATUS_$status")
        }
    }

    private fun deleteValue(account: String) {
        withKeychainQuery(account = account) { query ->
            SecItemDelete(query)
        }
    }

    private fun <T> withKeychainQuery(
        account: String,
        value: String? = null,
        includeReadOptions: Boolean = false,
        block: (CFDictionaryRef?) -> T,
    ): T {
        val retainedValues = mutableListOf<CFTypeRef?>()
        fun retain(value: Any?): CFTypeRef? =
            CFBridgingRetain(value).also { retainedValues += it }

        val pairs = mutableListOf<Pair<CFTypeRef?, CFTypeRef?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to retain(KEYCHAIN_SERVICE),
            kSecAttrAccount to retain(account),
        )
        if (value != null) {
            pairs += kSecValueData to retain(value.toUtf8Data())
            pairs += kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        }
        if (includeReadOptions) {
            pairs += kSecReturnData to kCFBooleanTrue
            pairs += kSecMatchLimit to kSecMatchLimitOne
        }

        val query = CFDictionaryCreateMutable(
            allocator = null,
            capacity = pairs.size.convert(),
            keyCallBacks = null,
            valueCallBacks = null,
        )
        pairs.forEach { (key, pairValue) ->
            if (key != null && pairValue != null) {
                CFDictionaryAddValue(query, key, pairValue)
            }
        }

        return try {
            block(query)
        } finally {
            CFRelease(query)
            retainedValues.forEach { CFBridgingRelease(it) }
        }
    }
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun String.toUtf8Data(): NSData {
    return NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)
        ?: ByteArray(0).toNSData()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toUtf8String(): String {
    val length = this.length.toInt()
    if (length == 0) return ""

    val bytes = this.bytes?.reinterpret<ByteVar>()?.readBytes(length) ?: return ""
    return bytes.decodeToString()
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(
        bytes = allocArrayOf(this@toNSData),
        length = this@toNSData.size.convert(),
    )
}

private const val KEYCHAIN_SERVICE = "com.runninghub.credentials"
private const val KEY_API_KEY = "api_key"
private const val KEY_ENTERPRISE_API_KEY = "enterprise_api_key"
private const val KEY_COOKIE = "user_cookie"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
