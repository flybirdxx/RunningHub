package com.runninghub.core.common

import java.security.MessageDigest

/**
 * Android/JVM 平台 MD5 实现。
 *
 * 使用 JDK MessageDigest 计算摘要，仅用于兼容登录接口的传输格式，不用于本地密码存储。
 */
actual fun md5(input: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}
