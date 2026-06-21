package com.runninghub.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * 创建 iOS Preferences DataStore。
 *
 * iOS 不需要外部 Context，直接使用应用 Documents 目录保存迁移期偏好文件。
 * 文件名与 Android 保持一致，确保跨平台代码只依赖同一个存储端口语义。
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createDataStore(): DataStore<Preferences> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )!!.path!!

    val path = "$documentDirectory/$DATASTORE_FILE_NAME"
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { path.toPath() }
    )
}
