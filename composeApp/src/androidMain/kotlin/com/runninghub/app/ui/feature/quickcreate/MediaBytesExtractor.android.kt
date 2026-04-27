package com.runninghub.app.ui.feature.quickcreate

import android.net.Uri

actual fun extractMediaBytes(context: android.content.Context, uri: String): ByteArray {
    val androidUri = Uri.parse(uri)
    return context.contentResolver.openInputStream(androidUri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
}
