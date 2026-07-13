package com.sunsetchasers.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ForecastRequest(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val date: String? = null,
    val type: String = "both"
)

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String
)

@Serializable
data class Altitudes(
    @SerialName("at_horizon") val atHorizon: Double = 0.0,
    @SerialName("at_neg6") val atNeg6: Double = -6.0,
    @SerialName("at_neg18") val atNeg18: Double = -18.0
)

@Serializable
data class SunriseEvent(
    val time: String,
    val azimuth: Double,
    val altitudes: Altitudes,
    @SerialName("twilight_civil_start") val twilightCivilStart: String,
    @SerialName("twilight_nautical_start") val twilightNauticalStart: String,
    @SerialName("twilight_astronomical_start") val twilightAstronomicalStart: String
)

@Serializable
data class SunsetEvent(
    val time: String,
    val azimuth: Double,
    val altitudes: Altitudes,
    @SerialName("twilight_civil_end") val twilightCivilEnd: String,
    @SerialName("twilight_nautical_end") val twilightNauticalEnd: String,
    @SerialName("twilight_astronomical_end") val twilightAstronomicalEnd: String
)

@Serializable
data class WeatherData(
    @SerialName("cloud_cover_percent") val cloudCoverPercent: Double,
    @SerialName("cloud_cover_low_percent") val cloudCoverLowPercent: Double,
    @SerialName("cloud_cover_mid_percent") val cloudCoverMidPercent: Double,
    @SerialName("cloud_cover_high_percent") val cloudCoverHighPercent: Double,
    @SerialName("visibility_km") val visibilityKm: Double,
    val conditions: String,
    @SerialName("temperature_c") val temperatureC: Double,
    @SerialName("humidity_percent") val humidityPercent: Double,
    @SerialName("wind_speed_ms") val windSpeedMs: Double
)

@Serializable
data class ScoreBreakdown(
    @SerialName("cloud_rating") val cloudRating: Double,
    @SerialName("cloud_low_rating") val cloudLowRating: Double,
    @SerialName("cloud_mid_rating") val cloudMidRating: Double,
    @SerialName("cloud_high_rating") val cloudHighRating: Double,
    @SerialName("visibility_rating") val visibilityRating: Double,
    @SerialName("condition_rating") val conditionRating: Double,
    @SerialName("color_potential_rating") val colorPotentialRating: Double
)

@Serializable
data class ColorTimelineEntry(
    val time: String,
    val altitude: String,
    val quality: String
)

@Serializable
data class ForecastQuality(
    val score: Double,
    val level: Int,
    val label: String,
    val breakdown: ScoreBreakdown,
    val reasoning: String,
    @SerialName("color_timeline") val colorTimeline: List<ColorTimelineEntry>
)

@Serializable
data class ForecastResponse(
    val status: String = "success",
    val location: Location,
    val date: String,
    val timezone: String,
    @SerialName("accuracy_warning") val accuracyWarning: Boolean,
    val sunrise: SunriseEvent? = null,
    val sunset: SunsetEvent? = null,
    val weather: WeatherData,
    @SerialName("forecast_quality") val forecastQuality: ForecastQuality,
    val timestamp: String
)

@Serializable
data class ForecastRangeRequest(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val type: String = "both"
)

@Serializable
data class ForecastDay(
    val date: String,
    @SerialName("accuracy_warning") val accuracyWarning: Boolean,
    val sunrise: SunriseEvent? = null,
    val sunset: SunsetEvent? = null,
    val weather: WeatherData,
    @SerialName("forecast_quality") val forecastQuality: ForecastQuality
)

@Serializable
data class ForecastRangeResponse(
    val status: String = "success",
    val location: Location,
    val timezone: String,
    val days: List<ForecastDay>,
    val timestamp: String
)

@Serializable
data class ErrorResponse(
    val status: String = "error",
    val code: String,
    val message: String,
    val details: JsonObject? = null,
    @SerialName("retry_after") val retryAfter: Int? = null,
    @SerialName("request_id") val requestId: String? = null
)
