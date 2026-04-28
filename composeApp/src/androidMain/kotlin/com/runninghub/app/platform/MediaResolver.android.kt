package com.runninghub.app.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

private lateinit var appContext: Context

fun initMediaResolver(context: Context) {
    appContext = context.applicationContext
}

actual fun createMediaResolver(): MediaResolver = AndroidMediaResolver(appContext)

private class AndroidMediaResolver(private val context: Context) : MediaResolver {
    override fun readBytes(uri: String): ByteArray =
        context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: error("Cannot open input stream for URI: $uri")

    override fun getDisplayName(uri: String): String? = try {
        context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }

    override fun getFileSizeBytes(uri: String): Long = try {
        context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx >= 0) cursor.getLong(idx) else 0L
        } ?: 0L
    } catch (_: Exception) {
        0L
    }
}
