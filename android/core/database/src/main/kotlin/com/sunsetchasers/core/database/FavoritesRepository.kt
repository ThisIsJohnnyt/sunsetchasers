package com.sunsetchasers.core.database

import com.sunsetchasers.core.model.FavoriteLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

const val MAX_FAVORITES = 5

class FavoritesRepository @Inject constructor(
    private val dao: FavoriteLocationDao
) {
    fun observeFavorites(): Flow<List<FavoriteLocation>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /** Returns false if the [MAX_FAVORITES] cap has already been reached. */
    suspend fun addFavorite(name: String, latitude: Double, longitude: Double): Boolean {
        if (dao.count() >= MAX_FAVORITES) return false
        dao.insert(
            FavoriteLocationEntity(
                name = name,
                latitude = latitude,
                longitude = longitude,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun removeFavorite(id: Long) {
        dao.deleteById(id)
    }
}

private fun FavoriteLocationEntity.toDomain() = FavoriteLocation(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude
)
