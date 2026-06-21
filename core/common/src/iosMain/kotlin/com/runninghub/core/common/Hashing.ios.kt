package com.runninghub.core.common

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH

/**
 * iOS 平台 MD5 实现。
 *
 * 使用 CoreCrypto 计算摘要，仅用于兼容用户中心登录协议。调用方不得把它当作新的
 * 安全密码存储或签名方案。
 */
@OptIn(ExperimentalForeignApi::class)
actual fun md5(input: String): String {
    val data = input.encodeToByteArray()
    if (data.isEmpty()) {
        // CoreCrypto 允许空输入，但 Kotlin/Native 不能对空数组 addressOf(0)，因此显式返回 MD5 空串摘要。
        return "d41d8cd98f00b204e9800998ecf8427e"
    }
    val digest = UByteArray(CC_MD5_DIGEST_LENGTH)

    data.usePinned { pinData ->
        digest.usePinned { pinDigest ->
            CC_MD5(pinData.addressOf(0), data.size.convert(), pinDigest.addressOf(0))
        }
    }

    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
