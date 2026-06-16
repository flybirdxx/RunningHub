package com.runninghub.app.util

private val VolatileImageUrlParams = setOf(
    "access_token",
    "auth",
    "auth_key",
    "authorization",
    "expires",
    "expiresat",
    "expiration",
    "policy",
    "security-token",
    "sign",
    "signature",
    "token",
    "x-amz-algorithm",
    "x-amz-credential",
    "x-amz-date",
    "x-amz-expires",
    "x-amz-security-token",
    "x-amz-signature",
    "x-oss-credential",
    "x-oss-date",
    "x-oss-expires",
    "x-oss-security-token",
    "x-oss-signature",
)

internal fun stableImageCacheKey(url: String): String {
    val withoutFragment = url.substringBefore('#')
    val queryStart = withoutFragment.indexOf('?')
    if (queryStart < 0) return withoutFragment

    val baseUrl = withoutFragment.substring(0, queryStart)
    val stableParams = withoutFragment.substring(queryStart + 1)
        .split('&')
        .filter { param ->
            val name = param.substringBefore('=').lowercase()
            name !in VolatileImageUrlParams
        }

    return if (stableParams.isEmpty()) {
        baseUrl
    } else {
        "$baseUrl?${stableParams.joinToString("&")}"
    }
}
