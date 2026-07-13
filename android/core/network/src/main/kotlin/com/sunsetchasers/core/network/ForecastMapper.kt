package com.sunsetchasers.core.network

import com.sunsetchasers.core.model.ColorTimelineEntry
import com.sunsetchasers.core.model.Forecast
import com.sunsetchasers.core.model.ForecastQuality
import com.sunsetchasers.core.model.GeoLocation
import com.sunsetchasers.core.model.ScoreBreakdown
import com.sunsetchasers.core.model.SunEvent
import com.sunsetchasers.core.model.WeatherConditions
import com.sunsetchasers.core.network.dto.ForecastQualityDto
import com.sunsetchasers.core.network.dto.ForecastRangeResponseDto
import com.sunsetchasers.core.network.dto.ForecastResponseDto
import com.sunsetchasers.core.network.dto.SunriseEventDto
import com.sunsetchasers.core.network.dto.SunsetEventDto
import com.sunsetchasers.core.network.dto.WeatherDto

private fun SunriseEventDto.toDomain(): SunEvent = SunEvent(
    time = time,
    azimuthDegrees = azimuth,
    twilightCivil = twilightCivilStart,
    twilightNautical = twilightNauticalStart,
    twilightAstronomical = twilightAstronomicalStart
)

private fun SunsetEventDto.toDomain(): SunEvent = SunEvent(
    time = time,
    azimuthDegrees = azimuth,
    twilightCivil = twilightCivilEnd,
    twilightNautical = twilightNauticalEnd,
    twilightAstronomical = twilightAstronomicalEnd
)

private fun WeatherDto.toDomain(): WeatherConditions = WeatherConditions(
    cloudCoverPercent = cloudCoverPercent,
    cloudCoverLowPercent = cloudCoverLowPercent,
    cloudCoverMidPercent = cloudCoverMidPercent,
    cloudCoverHighPercent = cloudCoverHighPercent,
    visibilityKm = visibilityKm,
    conditions = conditions,
    temperatureC = temperatureC,
    humidityPercent = humidityPercent,
    windSpeedMs = windSpeedMs
)

private fun ForecastQualityDto.toDomain(): ForecastQuality = ForecastQuality(
    score = score,
    level = level,
    label = label,
    breakdown = ScoreBreakdown(
        cloudRating = breakdown.cloudRating,
        cloudLowRating = breakdown.cloudLowRating,
        cloudMidRating = breakdown.cloudMidRating,
        cloudHighRating = breakdown.cloudHighRating,
        visibilityRating = breakdown.visibilityRating,
        conditionRating = breakdown.conditionRating,
        colorPotentialRating = breakdown.colorPotentialRating
    ),
    reasoning = reasoning,
    colorTimeline = colorTimeline.map { ColorTimelineEntry(it.time, it.altitude, it.quality) }
)

fun ForecastResponseDto.toDomain(): Forecast = Forecast(
    location = GeoLocation(location.latitude, location.longitude, location.name),
    date = date,
    timezone = timezone,
    accuracyWarning = accuracyWarning,
    sunrise = sunrise?.toDomain(),
    sunset = sunset?.toDomain(),
    weather = weather.toDomain(),
    quality = forecastQuality.toDomain(),
    generatedAt = timestamp
)

fun ForecastRangeResponseDto.toDomain(): List<Forecast> = days.map { day ->
    Forecast(
        location = GeoLocation(location.latitude, location.longitude, location.name),
        date = day.date,
        timezone = timezone,
        accuracyWarning = day.accuracyWarning,
        sunrise = day.sunrise?.toDomain(),
        sunset = day.sunset?.toDomain(),
        weather = day.weather.toDomain(),
        quality = day.forecastQuality.toDomain(),
        generatedAt = timestamp
    )
}
