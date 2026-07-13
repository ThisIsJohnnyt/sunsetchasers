package com.sunsetchasers.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

private const val MOCK_HOURLY_JSON = """
{
  "hourly": {
    "time": ["2025-06-21T09:00", "2025-06-21T12:00", "2025-06-21T15:00", "2025-06-21T20:00"],
    "cloud_cover": [20, 15, 10, 5],
    "cloud_cover_low": [5, 8, 2, 1],
    "cloud_cover_mid": [10, 5, 3, 2],
    "cloud_cover_high": [20, 15, 10, 5],
    "visibility": [12000, 13000, 14000, 16000],
    "temperature_2m": [22, 25, 26, 18],
    "relative_humidity_2m": [65, 60, 55, 70],
    "wind_speed_10m": [5, 4, 3, 6],
    "weather_code": [0, 0, 1, 3]
  }
}
"""

class WeatherServiceTest {

    private fun clientFor(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json() }
    }

    @Test
    fun `successfully fetches and extracts weather data`() = runTest {
        val engine = MockEngine { respond(MOCK_HOURLY_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val service = WeatherService(clientFor(engine))

        val result = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T12:05:00Z"))

        assertTrue(result.cloudCoverPercent in 0.0..100.0)
        assertTrue(result.cloudCoverLowPercent in 0.0..100.0)
        assertTrue(result.cloudCoverMidPercent in 0.0..100.0)
        assertTrue(result.cloudCoverHighPercent in 0.0..100.0)
        assertTrue(result.visibilityKm > 0)
        assertEquals("clear", result.conditions)
        assertEquals(25.0, result.temperatureC)
    }

    @Test
    fun `returns cached data without a second API call`() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(MOCK_HOURLY_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val service = WeatherService(clientFor(engine))
        val target = Instant.parse("2025-06-21T12:05:00Z")

        val first = service.getWeather(40.7128, -74.006, "2025-06-21", target)
        val second = service.getWeather(40.7128, -74.006, "2025-06-21", target)

        assertEquals(1, callCount)
        assertEquals(first, second)
    }

    @Test
    fun `caches raw hourly data across different target instants for the same location and date`() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(MOCK_HOURLY_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val service = WeatherService(clientFor(engine))

        val sunrise = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T09:10:00Z"))
        val sunset = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T20:03:00Z"))

        assertEquals(1, callCount, "a sunrise sample and a sunset sample for the same location/date should share one upstream fetch")
        assertNotEquals(sunrise.temperatureC, sunset.temperatureC)
        assertEquals(22.0, sunrise.temperatureC)
        assertEquals(18.0, sunset.temperatureC)
    }

    @Test
    fun `shares one upstream fetch across different dates for the same location`() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(MOCK_HOURLY_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val service = WeatherService(clientFor(engine))

        val today = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T12:05:00Z"))
        val tomorrow = service.getWeather(40.7128, -74.006, "2025-06-22", Instant.parse("2025-06-21T15:05:00Z"))

        assertEquals(1, callCount, "requesting a 3-day range for one location should share a single upstream fetch")
        assertNotEquals(today.temperatureC, tomorrow.temperatureC)
    }

    @Test
    fun `picks the hourly index closest to the given target instant, not a fixed solar-noon guess`() = runTest {
        val engine = MockEngine { respond(MOCK_HOURLY_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val service = WeatherService(clientFor(engine))

        val nearSunset = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T19:50:00Z"))

        assertEquals(18.0, nearSunset.temperatureC)
        assertEquals(5.0, nearSunset.cloudCoverPercent)
    }

    @Test
    fun `cache expires after ttl`() = runTest {
        var callCount = 0
        var mockBody = MOCK_HOURLY_JSON
        val engine = MockEngine {
            callCount++
            respond(mockBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        var clockMillis = 0L
        val service = WeatherService(clientFor(engine), nowMillis = { clockMillis })
        val target = Instant.parse("2025-06-21T12:05:00Z")

        service.getWeather(40.7128, -74.006, "2025-06-21", target)
        assertEquals(1, callCount)

        clockMillis += 31 * 60 * 1000L
        mockBody = """
            {
              "hourly": {
                "time": ["2025-06-21T12:00"],
                "cloud_cover": [50], "cloud_cover_low": [40], "cloud_cover_mid": [30], "cloud_cover_high": [20],
                "visibility": [8000], "temperature_2m": [20], "relative_humidity_2m": [70],
                "wind_speed_10m": [6], "weather_code": [3]
              }
            }
        """.trimIndent()

        val result = service.getWeather(40.7128, -74.006, "2025-06-21", target)
        assertEquals(2, callCount)
        assertEquals(50.0, result.cloudCoverPercent)
        assertEquals(8.0, result.visibilityKm)
    }

    @Test
    fun `handles missing visibility field, defaults to 10km`() = runTest {
        val json = """
            {
              "hourly": {
                "time": ["2025-06-21T12:00"],
                "cloud_cover": [25], "cloud_cover_low": [5], "cloud_cover_mid": [10], "cloud_cover_high": [15],
                "temperature_2m": [23], "relative_humidity_2m": [65],
                "wind_speed_10m": [4], "weather_code": [0]
              }
            }
        """.trimIndent()
        val engine = MockEngine { respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val service = WeatherService(clientFor(engine))

        val result = service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T12:00:00Z"))
        assertEquals(10.0, result.visibilityKm)
    }

    @Test
    fun `handles API errors gracefully`() = runTest {
        val engine = MockEngine { throw RuntimeException("Network error") }
        val service = WeatherService(clientFor(engine))

        var thrown: WeatherApiException? = null
        try {
            service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T12:00:00Z"))
        } catch (e: WeatherApiException) {
            thrown = e
        }

        assertNotNull(thrown)
        assertTrue(thrown!!.message!!.contains("Weather API error"))
    }

    @Test
    fun `handles a non-success status (e_g_ rate limited) without a raw deserialization crash`() = runTest {
        val engine = MockEngine {
            respond(
                """{"error":true,"reason":"Minutely API request limit exceeded"}""",
                HttpStatusCode.TooManyRequests,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val service = WeatherService(clientFor(engine))

        var thrown: WeatherApiException? = null
        try {
            service.getWeather(40.7128, -74.006, "2025-06-21", Instant.parse("2025-06-21T12:00:00Z"))
        } catch (e: WeatherApiException) {
            thrown = e
        }

        assertNotNull(thrown)
        assertTrue(thrown!!.message!!.contains("429") || thrown.message!!.contains("Too Many Requests"))
    }

    @Test
    fun `maps WMO weather codes to condition strings`() {
        assertEquals("clear", WeatherService.weatherCodeToCondition(0))
        assertEquals("mostly clear", WeatherService.weatherCodeToCondition(1))
        assertEquals("partly cloudy", WeatherService.weatherCodeToCondition(2))
        assertEquals("overcast", WeatherService.weatherCodeToCondition(3))
        assertEquals("fog", WeatherService.weatherCodeToCondition(45))
        assertEquals("drizzle", WeatherService.weatherCodeToCondition(51))
        assertEquals("light rain", WeatherService.weatherCodeToCondition(61))
        assertEquals("rain", WeatherService.weatherCodeToCondition(63))
        assertEquals("snow", WeatherService.weatherCodeToCondition(71))
        assertEquals("thunderstorm", WeatherService.weatherCodeToCondition(95))
        assertEquals("unknown", WeatherService.weatherCodeToCondition(-1))
    }
}
