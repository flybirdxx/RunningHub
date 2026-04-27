package com.runninghub.shared.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

private lateinit var appContext: Context

fun initDataStore(context: Context) {
    appContext = context.applicationContext
}

private var dataStoreInstance: DataStore<Preferences>? = null

actual fun createDataStore(): DataStore<Preferences> {
    return dataStoreInstance ?: run {
        val path = appContext.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { path.toPath() }
        ).also { dataStoreInstance = it }
    }
}
