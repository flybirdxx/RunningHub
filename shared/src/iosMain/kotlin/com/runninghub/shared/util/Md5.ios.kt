package com.runninghub.shared.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun md5(input: String): String {
    val data = input.encodeToByteArray()
    val digest = UByteArray(CC_MD5_DIGEST_LENGTH)

    data.usePinned { pinData ->
        digest.usePinned { pinDigest ->
            CC_MD5(pinData.addressOf(0), data.size.convert(), pinDigest.addressOf(0))
        }
    }

    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
