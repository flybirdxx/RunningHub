package com.runninghub.shared.di

import com.runninghub.shared.platform.DatabaseDriverFactory
import com.runninghub.shared.platform.SETTINGS_DATA_STORE_FILE
import com.runninghub.shared.platform.createDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single {
        createDataStore(
            producePath = {
                val dir = NSFileManager.defaultManager.URLForDirectory(
                    directory = NSDocumentDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = null,
                    create = false,
                    error = null
                )!!.path!!
                "$dir/$SETTINGS_DATA_STORE_FILE"
            }
        )
    }
}
