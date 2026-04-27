package com.runninghub.app.ui.feature.quickcreate

actual fun extractMediaBytes(context: Any, uri: String): ByteArray {
    // iOS implementation via NSData
    throw NotImplementedError("Media extraction not yet implemented on iOS")
}
