package com.sunsetchasers.core.model

enum class ForecastType {
    SUNRISE,
    SUNSET,
    BOTH
}

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String
)

data class SunEvent(
    val time: String,
    val azimuthDegrees: Double,
    val twilightCivil: String,
    val twilightNautical: String,
    val twilightAstronomical: String
)

data class WeatherConditions(
    val cloudCoverPercent: Double,
    val cloudCoverLowPercent: Double,
    val cloudCoverMidPercent: Double,
    val cloudCoverHighPercent: Double,
    val visibilityKm: Double,
    val conditions: String,
    val temperatureC: Double,
    val humidityPercent: Double,
    val windSpeedMs: Double
)

data class ScoreBreakdown(
    val cloudRating: Double,
    val cloudLowRating: Double,
    val cloudMidRating: Double,
    val cloudHighRating: Double,
    val visibilityRating: Double,
    val conditionRating: Double,
    val colorPotentialRating: Double
)

data class ColorTimelineEntry(
    val time: String,
    val altitude: String,
    val quality: String
)

data class ForecastQuality(
    val score: Double,
    val level: Int,
    val label: String,
    val breakdown: ScoreBreakdown,
    val reasoning: String,
    val colorTimeline: List<ColorTimelineEntry>
)

data class Forecast(
    val location: GeoLocation,
    val date: String,
    val timezone: String,
    val accuracyWarning: Boolean,
    val sunrise: SunEvent?,
    val sunset: SunEvent?,
    val weather: WeatherConditions,
    val quality: ForecastQuality,
    val generatedAt: String
)

/**
 * Typed outcome of a forecast request, mirroring the success/error contract in API_SPEC.md
 * so the UI layer can react to specific backend error codes (e.g. DATE_OUT_OF_RANGE) rather
 * than parsing exception messages.
 */
sealed interface ForecastResult {
    data class Success(val forecast: Forecast) : ForecastResult
    data class Error(val code: String, val message: String) : ForecastResult
}

/** Same success/error contract as [ForecastResult], for the multi-day /api/forecast/range endpoint. */
sealed interface ForecastRangeResult {
    data class Success(val days: List<Forecast>) : ForecastRangeResult
    data class Error(val code: String, val message: String) : ForecastRangeResult
}
