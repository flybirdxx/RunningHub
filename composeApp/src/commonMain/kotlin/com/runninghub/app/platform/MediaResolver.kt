package com.runninghub.app.platform

interface MediaResolver {
    fun readBytes(uri: String): ByteArray
    fun getDisplayName(uri: String): String?
    fun getFileSizeBytes(uri: String): Long
}

expect fun createMediaResolver(): MediaResolver
