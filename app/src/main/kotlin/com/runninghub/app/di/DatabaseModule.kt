package com.runninghub.app.di

import android.content.Context
import androidx.room.Room
import com.runninghub.app.data.local.AppDatabase
import com.runninghub.app.data.local.dao.DiscoveryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "runninghub_db"
        ).build()
    }

    @Provides
    fun provideDiscoveryDao(database: AppDatabase): DiscoveryDao {
        return database.discoveryDao()
    }
}
