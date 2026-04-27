package com.runninghub.shared.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

internal const val DATASTORE_FILE_NAME = "runninghub_settings.preferences_pb"

expect fun createDataStore(): DataStore<Preferences>
