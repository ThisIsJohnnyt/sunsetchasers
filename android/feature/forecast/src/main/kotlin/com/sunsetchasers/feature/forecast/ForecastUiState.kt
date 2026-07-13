package com.sunsetchasers.feature.forecast

import com.sunsetchasers.core.model.Forecast
import com.sunsetchasers.core.model.ForecastType

data class ForecastUiState(
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    val type: ForecastType = ForecastType.BOTH,
    val isLoading: Boolean = false,
    val isLocating: Boolean = false,
    val days: List<Forecast> = emptyList(),
    val expandedDayIndex: Int? = null,
    val errorMessage: String? = null
)
