package com.sunsetchasers.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteLocationDao {

    @Query("SELECT * FROM favorite_locations ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<FavoriteLocationEntity>>

    @Query("SELECT COUNT(*) FROM favorite_locations")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: FavoriteLocationEntity): Long

    @Query("DELETE FROM favorite_locations WHERE id = :id")
    suspend fun deleteById(id: Long)
}
