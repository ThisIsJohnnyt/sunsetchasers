package com.sunsetchasers.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FavoriteLocationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteLocationDao(): FavoriteLocationDao
}
