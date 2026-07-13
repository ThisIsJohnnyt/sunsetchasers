package com.sunsetchasers.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastRequestDto(
    val latitude: Double,
    val longitude: Double,
    val date: String,
    val type: String
)

@Serializable
data class LocationDto(
    val latitude: Double,
    val longitude: Double,
    val name: String
)

@Serializable
data class AltitudesDto(
    @SerialName("at_horizon") val atHorizon: Double = 0.0,
    @SerialName("at_neg6") val atNeg6: Double = -6.0,
    @SerialName("at_neg18") val atNeg18: Double = -18.0
)

@Serializable
data class SunriseEventDto(
    val time: String,
    val azimuth: Double,
    val altitudes: AltitudesDto,
    @SerialName("twilight_civil_start") val twilightCivilStart: String,
    @SerialName("twilight_nautical_start") val twilightNauticalStart: String,
    @SerialName("twilight_astronomical_start") val twilightAstronomicalStart: String
)

@Serializable
data class SunsetEventDto(
    val time: String,
    val azimuth: Double,
    val altitudes: AltitudesDto,
    @SerialName("twilight_civil_end") val twilightCivilEnd: String,
    @SerialName("twilight_nautical_end") val twilightNauticalEnd: String,
    @SerialName("twilight_astronomical_end") val twilightAstronomicalEnd: String
)

@Serializable
data class WeatherDto(
    @SerialName("cloud_cover_percent") val cloudCoverPercent: Double,
    @SerialName("cloud_cover_low_percent") val cloudCoverLowPercent: Double = 0.0,
    @SerialName("cloud_cover_mid_percent") val cloudCoverMidPercent: Double = 0.0,
    @SerialName("cloud_cover_high_percent") val cloudCoverHighPercent: Double = 0.0,
    @SerialName("visibility_km") val visibilityKm: Double,
    val conditions: String,
    @SerialName("temperature_c") val temperatureC: Double,
    @SerialName("humidity_percent") val humidityPercent: Double,
    @SerialName("wind_speed_ms") val windSpeedMs: Double
)

@Serializable
data class ScoreBreakdownDto(
    @SerialName("cloud_rating") val cloudRating: Double,
    @SerialName("cloud_low_rating") val cloudLowRating: Double = 0.0,
    @SerialName("cloud_mid_rating") val cloudMidRating: Double = 0.0,
    @SerialName("cloud_high_rating") val cloudHighRating: Double = 0.0,
    @SerialName("visibility_rating") val visibilityRating: Double,
    @SerialName("condition_rating") val conditionRating: Double,
    @SerialName("color_potential_rating") val colorPotentialRating: Double
)

@Serializable
data class ColorTimelineEntryDto(
    val time: String,
    val altitude: String,
    val quality: String
)

@Serializable
data class ForecastQualityDto(
    val score: Double,
    val level: Int,
    val label: String,
    val breakdown: ScoreBreakdownDto,
    val reasoning: String,
    @SerialName("color_timeline") val colorTimeline: List<ColorTimelineEntryDto>
)

@Serializable
data class ForecastResponseDto(
    val status: String,
    val location: LocationDto,
    val date: String,
    val timezone: String,
    @SerialName("accuracy_warning") val accuracyWarning: Boolean,
    val sunrise: SunriseEventDto? = null,
    val sunset: SunsetEventDto? = null,
    val weather: WeatherDto,
    @SerialName("forecast_quality") val forecastQuality: ForecastQualityDto,
    val timestamp: String
)

@Serializable
data class ForecastRangeRequestDto(
    val latitude: Double,
    val longitude: Double,
    val type: String
)

@Serializable
data class ForecastDayDto(
    val date: String,
    @SerialName("accuracy_warning") val accuracyWarning: Boolean,
    val sunrise: SunriseEventDto? = null,
    val sunset: SunsetEventDto? = null,
    val weather: WeatherDto,
    @SerialName("forecast_quality") val forecastQuality: ForecastQualityDto
)

@Serializable
data class ForecastRangeResponseDto(
    val status: String,
    val location: LocationDto,
    val timezone: String,
    val days: List<ForecastDayDto>,
    val timestamp: String
)

@Serializable
data class ErrorResponseDto(
    val status: String = "error",
    val code: String,
    val message: String
)
