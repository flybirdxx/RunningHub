package com.runninghub.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

private lateinit var appContext: Context
private var dataStoreInstance: DataStore<Preferences>? = null

/**
 * 初始化 Android 平台 DataStore 所需的 Application Context。
 *
 * DataStore 的文件路径需要 Android Context 才能解析。该函数必须在 Koin 装配前由
 * Application 调用，并保存 applicationContext，避免持有 Activity 引用导致生命周期泄漏。
 *
 * @param context Android Application 或任意 Context；内部会转换为 applicationContext。
 */
fun initDataStore(context: Context) {
    appContext = context.applicationContext
}

/**
 * 创建或复用 Android Preferences DataStore。
 *
 * 同一进程内只保留一个 DataStore 实例，避免多个 writer 同时操作同一个偏好文件造成一致性问题。
 * 调用方必须保证 [initDataStore] 已先执行，否则 Android 文件目录无法解析。
 */
actual fun createDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: run {
        val path = appContext.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { path.toPath() }
        ).also { dataStoreInstance = it }
    }
}
