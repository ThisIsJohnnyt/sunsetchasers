package com.sunsetchasers.core.database.di

import android.content.Context
import androidx.room.Room
import com.sunsetchasers.core.database.AppDatabase
import com.sunsetchasers.core.database.FavoriteLocationDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sunsetchasers.db").build()

    @Provides
    fun provideFavoriteLocationDao(database: AppDatabase): FavoriteLocationDao =
        database.favoriteLocationDao()
}
