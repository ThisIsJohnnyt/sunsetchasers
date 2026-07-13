package com.sunsetchasers.services

import com.sunsetchasers.models.WeatherData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

class WeatherApiException(message: String) : Exception(message)

@Serializable
private data class OpenMeteoResponse(val hourly: OpenMeteoHourly)

@Serializable
private data class OpenMeteoHourly(
    val time: List<String>,
    @SerialName("cloud_cover") val cloudCover: List<Double>,
    @SerialName("cloud_cover_low") val cloudCoverLow: List<Double>,
    @SerialName("cloud_cover_mid") val cloudCoverMid: List<Double>,
    @SerialName("cloud_cover_high") val cloudCoverHigh: List<Double>,
    val visibility: List<Double?> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: List<Double>,
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>
)

/**
 * Weather via Open-Meteo (https://open-meteo.com) — free, no API key required.
 * Chosen over OpenWeatherMap/WeatherAPI.com/NOAA specifically because it
 * exposes cloud cover *by altitude* (low/mid/high), which is what actually
 * predicts sunrise/sunset color quality, rather than a single blended value.
 */
class WeatherService(
    private val httpClient: HttpClient,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    companion object {
        private const val CACHE_TTL_MILLIS = 30 * 60 * 1000L

        private val WEATHER_CODE_CONDITIONS: Map<Int, String> = mapOf(
            0 to "clear",
            1 to "mostly clear",
            2 to "partly cloudy",
            3 to "overcast",
            45 to "fog",
            48 to "fog",
            51 to "drizzle",
            53 to "drizzle",
            55 to "drizzle",
            56 to "drizzle",
            57 to "drizzle",
            61 to "light rain",
            80 to "light rain",
            63 to "rain",
            65 to "rain",
            66 to "rain",
            67 to "rain",
            81 to "rain",
            82 to "rain",
            71 to "snow",
            73 to "snow",
            75 to "snow",
            77 to "snow",
            85 to "snow",
            86 to "snow",
            95 to "thunderstorm",
            96 to "thunderstorm",
            99 to "thunderstorm"
        )

        /** WMO weather code (https://open-meteo.com/en/docs#weathervariables) -> our condition string. */
        internal fun weatherCodeToCondition(code: Int): String = WEATHER_CODE_CONDITIONS[code] ?: "unknown"
    }

    private data class CacheEntry(val hourly: OpenMeteoHourly, val cachedAtMillis: Long)

    // Cached per (lat, lon) only: a single fetch already spans past_days=1
    // plus forecast_days=9, so it covers every date within the 7-day forecast
    // window. Keying on lat/lon alone (not date) means a 3-day range request
    // shares one upstream fetch instead of three, same as sunrise/sunset
    // sharing one fetch for a single date.
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private fun cacheKey(lat: Double, lon: Double): String {
        val latRounded = (lat * 100).roundToInt() / 100.0
        val lonRounded = (lon * 100).roundToInt() / 100.0
        return "${latRounded}_${lonRounded}"
    }

    private fun getCachedHourly(key: String): OpenMeteoHourly? {
        val entry = cache[key] ?: return null
        val age = nowMillis() - entry.cachedAtMillis
        if (age > CACHE_TTL_MILLIS) {
            cache.remove(key)
            return null
        }
        return entry.hourly
    }

    suspend fun getWeather(lat: Double, lon: Double, dateStr: String, targetInstant: Instant): WeatherData {
        val key = cacheKey(lat, lon)
        val hourly = getCachedHourly(key) ?: fetchHourly(lat, lon, key)

        val index = findClosestIndex(hourly.time, targetInstant)
            ?: throw WeatherApiException("No forecast data found for $dateStr")

        return WeatherData(
            cloudCoverPercent = hourly.cloudCover[index],
            cloudCoverLowPercent = hourly.cloudCoverLow[index],
            cloudCoverMidPercent = hourly.cloudCoverMid[index],
            cloudCoverHighPercent = hourly.cloudCoverHigh[index],
            visibilityKm = (hourly.visibility.getOrNull(index) ?: 10000.0) / 1000.0,
            conditions = weatherCodeToCondition(hourly.weatherCode[index]),
            temperatureC = hourly.temperature2m[index],
            humidityPercent = hourly.relativeHumidity2m[index],
            windSpeedMs = hourly.windSpeed10m[index]
        )
    }

    private suspend fun fetchHourly(lat: Double, lon: Double, cacheKey: String): OpenMeteoHourly {
        try {
            val response: OpenMeteoResponse = httpClient.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("timezone", "UTC")
                parameter("wind_speed_unit", "ms")
                // Pad a day on each side of "today" so extreme UTC-offset
                // locations don't miss the target local-date hour.
                parameter("past_days", 1)
                parameter("forecast_days", 9)
                parameter(
                    "hourly",
                    "cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility," +
                        "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code"
                )
            }.body()

            cache[cacheKey] = CacheEntry(response.hourly, nowMillis())
            return response.hourly
        } catch (e: Exception) {
            throw WeatherApiException("Weather API error: ${e.message}")
        }
    }

    private fun findClosestIndex(times: List<String>, target: Instant): Int? {
        if (times.isEmpty()) return null
        return times.indices.minByOrNull { i ->
            val entryInstant = LocalDateTime.parse(times[i]).toInstant(ZoneOffset.UTC)
            abs(Duration.between(target, entryInstant).toMillis())
        }
    }
}
