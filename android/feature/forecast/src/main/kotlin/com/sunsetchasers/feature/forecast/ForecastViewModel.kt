package com.sunsetchasers.feature.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunsetchasers.core.database.FavoritesRepository
import com.sunsetchasers.core.datastore.SettingsRepository
import com.sunsetchasers.core.model.FavoriteLocation
import com.sunsetchasers.core.model.ForecastRangeResult
import com.sunsetchasers.core.model.ForecastType
import com.sunsetchasers.core.model.UserSettings
import com.sunsetchasers.core.network.ForecastApi
import com.sunsetchasers.feature.forecast.location.DeviceLocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class ForecastViewModel @Inject constructor(
    private val forecastApi: ForecastApi,
    private val favoritesRepository: FavoritesRepository,
    private val locationProvider: DeviceLocationProvider,
    settingsRepository: SettingsRepository
) : ViewModel() {

    companion object {
        private const val LOCATION_TIMEOUT_MILLIS = 15_000L
    }

    private val _uiState = MutableStateFlow(ForecastUiState())
    val uiState: StateFlow<ForecastUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<List<FavoriteLocation>> = favoritesRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun onLatitudeChange(value: String) {
        _uiState.update { it.copy(latitudeInput = value) }
    }

    fun onLongitudeChange(value: String) {
        _uiState.update { it.copy(longitudeInput = value) }
    }

    fun onTypeChange(type: ForecastType) {
        _uiState.update { it.copy(type = type) }
    }

    fun toggleDayExpanded(index: Int) {
        _uiState.update { it.copy(expandedDayIndex = if (it.expandedDayIndex == index) null else index) }
    }

    fun selectFavorite(favorite: FavoriteLocation) {
        _uiState.update {
            it.copy(
                latitudeInput = favorite.latitude.toString(),
                longitudeInput = favorite.longitude.toString()
            )
        }
    }

    private fun validatedLatLon(): Pair<Double, Double>? {
        val state = _uiState.value
        val latitude = state.latitudeInput.toDoubleOrNull()
        val longitude = state.longitudeInput.toDoubleOrNull()

        if (latitude == null || latitude !in -90.0..90.0) {
            _uiState.update { it.copy(errorMessage = "Enter a latitude between -90 and 90") }
            return null
        }
        if (longitude == null || longitude !in -180.0..180.0) {
            _uiState.update { it.copy(errorMessage = "Enter a longitude between -180 and 180") }
            return null
        }
        return latitude to longitude
    }

    fun fetchForecastRange() {
        val (latitude, longitude) = validatedLatLon() ?: return
        val type = _uiState.value.type

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = forecastApi.getForecastRange(latitude, longitude, type)) {
                is ForecastRangeResult.Success -> _uiState.update {
                    it.copy(isLoading = false, days = result.days, expandedDayIndex = 0, errorMessage = null)
                }
                is ForecastRangeResult.Error -> _uiState.update {
                    it.copy(isLoading = false, days = emptyList(), expandedDayIndex = null, errorMessage = result.message)
                }
            }
        }
    }

    fun useDeviceLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, errorMessage = null) }
            val location = locationProvider.lastKnownLocation()
                ?: withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) { locationProvider.requestFreshLocation() }

            if (location != null) {
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        latitudeInput = location.latitude.toString(),
                        longitudeInput = location.longitude.toString()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        errorMessage = "Couldn't determine your location. Check that location services are enabled and try again."
                    )
                }
            }
        }
    }

    fun saveCurrentAsFavorite(name: String) {
        val (latitude, longitude) = validatedLatLon() ?: return
        viewModelScope.launch {
            val saved = favoritesRepository.addFavorite(name, latitude, longitude)
            _messages.send(
                if (saved) "Saved \"$name\" to favorites"
                else "You can only save up to 5 favorites — remove one first"
            )
        }
    }
}
