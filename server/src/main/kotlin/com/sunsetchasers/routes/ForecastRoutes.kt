package com.sunsetchasers.routes

import com.sunsetchasers.models.Altitudes
import com.sunsetchasers.models.ForecastDay
import com.sunsetchasers.models.ForecastRangeRequest
import com.sunsetchasers.models.ForecastRangeResponse
import com.sunsetchasers.models.ForecastRequest
import com.sunsetchasers.models.ForecastResponse
import com.sunsetchasers.models.Location
import com.sunsetchasers.models.SunriseEvent
import com.sunsetchasers.models.SunsetEvent
import com.sunsetchasers.services.AstronomyResult
import com.sunsetchasers.services.AstronomyService
import com.sunsetchasers.services.ScoringService
import com.sunsetchasers.services.WeatherService
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

class InvalidRequestException(val field: String, val value: String, message: String) : Exception(message)

class DateOutOfRangeException(
    val requestedDate: String,
    val maxForecastDate: String,
    val daysAhead: Long,
    message: String
) : Exception(message)

private val VALID_TYPES = setOf("sunrise", "sunset", "both")
private const val MAX_FORECAST_DAYS = 7L
private const val ACCURACY_WARNING_DAYS = 2L // beyond 48 hours
private const val RANGE_DAYS = 3L

private suspend fun computeForecastDay(
    latitude: Double,
    longitude: Double,
    dateStr: String,
    type: String,
    accuracyWarning: Boolean,
    weatherService: WeatherService
): Pair<AstronomyResult, ForecastDay> {
    val astronomy = AstronomyService.getSunTimes(latitude, longitude, dateStr)
    // Weather is sampled once per day. For "sunrise" we sample at the sunrise
    // instant; for "sunset" and "both" we sample at sunset (documented
    // simplification — see API_SPEC.md).
    val targetInstant = if (type == "sunrise") {
        astronomy.sunrise.instant.toInstant()
    } else {
        astronomy.sunset.instant.toInstant()
    }
    val weather = weatherService.getWeather(latitude, longitude, dateStr, targetInstant)

    val includeSunrise = type == "sunrise" || type == "both"
    val includeSunset = type == "sunset" || type == "both"

    val colorTimeline = ScoringService.buildColorTimeline(
        includeSunrise = includeSunrise,
        includeSunset = includeSunset,
        astronomy = astronomy,
        formatTime = AstronomyService::formatOrEmpty
    )
    val forecastQuality = ScoringService.calcScore(weather, colorTimeline)

    val day = ForecastDay(
        date = dateStr,
        accuracyWarning = accuracyWarning,
        sunrise = if (includeSunrise) {
            SunriseEvent(
                time = astronomy.sunrise.time,
                azimuth = astronomy.sunrise.azimuthDegrees,
                altitudes = Altitudes(),
                twilightCivilStart = AstronomyService.formatOrEmpty(astronomy.civilDawn),
                twilightNauticalStart = AstronomyService.formatOrEmpty(astronomy.nauticalDawn),
                twilightAstronomicalStart = AstronomyService.formatOrEmpty(astronomy.astronomicalDawn)
            )
        } else null,
        sunset = if (includeSunset) {
            SunsetEvent(
                time = astronomy.sunset.time,
                azimuth = astronomy.sunset.azimuthDegrees,
                altitudes = Altitudes(),
                twilightCivilEnd = AstronomyService.formatOrEmpty(astronomy.civilDusk),
                twilightNauticalEnd = AstronomyService.formatOrEmpty(astronomy.nauticalDusk),
                twilightAstronomicalEnd = AstronomyService.formatOrEmpty(astronomy.astronomicalDusk)
            )
        } else null,
        weather = weather,
        forecastQuality = forecastQuality
    )
    return astronomy to day
}

fun Route.forecastRoutes(weatherService: WeatherService) {
    route("/api") {
        rateLimit {
            post("/forecast") {
                val request = call.receive<ForecastRequest>()

                val latitude = request.latitude
                    ?: throw InvalidRequestException("latitude", "null", "Latitude is required")
                if (latitude < -90.0 || latitude > 90.0) {
                    throw InvalidRequestException("latitude", latitude.toString(), "Latitude must be between -90 and 90")
                }

                val longitude = request.longitude
                    ?: throw InvalidRequestException("longitude", "null", "Longitude is required")
                if (longitude < -180.0 || longitude > 180.0) {
                    throw InvalidRequestException("longitude", longitude.toString(), "Longitude must be between -180 and 180")
                }

                val dateStr = request.date
                    ?: throw InvalidRequestException("date", "null", "Date is required")
                val requestedDate = try {
                    LocalDate.parse(dateStr)
                } catch (e: DateTimeParseException) {
                    throw InvalidRequestException("date", dateStr, "Date must be in YYYY-MM-DD format")
                }

                if (request.type !in VALID_TYPES) {
                    throw InvalidRequestException("type", request.type, "Type must be one of: sunrise, sunset, both")
                }

                val today = LocalDate.now(ZoneOffset.UTC)
                val daysAhead = ChronoUnit.DAYS.between(today, requestedDate)
                if (daysAhead < 0 || daysAhead > MAX_FORECAST_DAYS) {
                    throw DateOutOfRangeException(
                        requestedDate = dateStr,
                        maxForecastDate = today.plusDays(MAX_FORECAST_DAYS).toString(),
                        daysAhead = daysAhead,
                        message = "Forecast date must be within 7 days of today"
                    )
                }
                val accuracyWarning = daysAhead > ACCURACY_WARNING_DAYS

                val (astronomy, day) = computeForecastDay(
                    latitude = latitude,
                    longitude = longitude,
                    dateStr = dateStr,
                    type = request.type,
                    accuracyWarning = accuracyWarning,
                    weatherService = weatherService
                )

                val response = ForecastResponse(
                    location = Location(
                        latitude = latitude,
                        longitude = longitude,
                        name = "Lat $latitude, Lon $longitude"
                    ),
                    date = day.date,
                    timezone = astronomy.timezone,
                    accuracyWarning = day.accuracyWarning,
                    sunrise = day.sunrise,
                    sunset = day.sunset,
                    weather = day.weather,
                    forecastQuality = day.forecastQuality,
                    timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
                )

                call.respond(response)
            }

            post("/forecast/range") {
                val request = call.receive<ForecastRangeRequest>()

                val latitude = request.latitude
                    ?: throw InvalidRequestException("latitude", "null", "Latitude is required")
                if (latitude < -90.0 || latitude > 90.0) {
                    throw InvalidRequestException("latitude", latitude.toString(), "Latitude must be between -90 and 90")
                }

                val longitude = request.longitude
                    ?: throw InvalidRequestException("longitude", "null", "Longitude is required")
                if (longitude < -180.0 || longitude > 180.0) {
                    throw InvalidRequestException("longitude", longitude.toString(), "Longitude must be between -180 and 180")
                }

                if (request.type !in VALID_TYPES) {
                    throw InvalidRequestException("type", request.type, "Type must be one of: sunrise, sunset, both")
                }

                val today = LocalDate.now(ZoneOffset.UTC)

                var timezone = "UTC"
                val days = (0 until RANGE_DAYS).map { offset ->
                    val dateStr = today.plusDays(offset).toString()
                    val (astronomy, day) = computeForecastDay(
                        latitude = latitude,
                        longitude = longitude,
                        dateStr = dateStr,
                        type = request.type,
                        accuracyWarning = offset > ACCURACY_WARNING_DAYS,
                        weatherService = weatherService
                    )
                    timezone = astronomy.timezone
                    day
                }

                call.respond(
                    ForecastRangeResponse(
                        location = Location(
                            latitude = latitude,
                            longitude = longitude,
                            name = "Lat $latitude, Lon $longitude"
                        ),
                        timezone = timezone,
                        days = days,
                        timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
                    )
                )
            }
        }
    }
}
