package com.runninghub.shared.di

import com.runninghub.shared.platform.DatabaseDriverFactory
import com.runninghub.shared.platform.SETTINGS_DATA_STORE_FILE
import com.runninghub.shared.platform.createDataStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single {
        createDataStore(
            producePath = {
                androidContext().filesDir.resolve(SETTINGS_DATA_STORE_FILE).absolutePath
            }
        )
    }
}
