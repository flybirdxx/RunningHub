package com.runninghub.app.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

private lateinit var appContext: Context

fun initMediaResolver(context: Context) {
    appContext = context.applicationContext
}

actual fun createMediaResolver(): MediaResolver = AndroidMediaResolver(appContext)

private class AndroidMediaResolver(private val context: Context) : MediaResolver {
    override fun readBytes(uri: String): ByteArray {
        // Try ContentResolver first (handles content:// URIs reliably)
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "content") {
            return context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
                ?: error("Cannot open content URI: $uri")
        }
        // Fallback to direct file access for filesystem paths
        val file = File(uri)
        if (file.exists() && file.isFile && file.canRead()) {
            return file.readBytes()
        }
        // Last resort: treat as generic URI
        return context.contentResolver.openInputStream(parsed)?.use { it.readBytes() }
            ?: error("Cannot read file: $uri")
    }

    override fun getDisplayName(uri: String): String? {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "content") {
            return try {
                context.contentResolver.query(parsed, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
                }
            } catch (_: Exception) { null }
        }
        val file = File(uri)
        return if (file.exists()) file.name else uri.substringAfterLast("/").ifBlank { null }
    }

    override fun getFileSizeBytes(uri: String): Long {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "content") {
            return try {
                context.contentResolver.query(parsed, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && idx >= 0) cursor.getLong(idx) else 0L
                } ?: 0L
            } catch (_: Exception) { 0L }
        }
        val file = File(uri)
        return if (file.exists()) file.length() else 0L
    }
}
