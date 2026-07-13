package com.sunsetchasers.routes

import com.sunsetchasers.models.ErrorResponse
import com.sunsetchasers.models.ForecastRangeResponse
import com.sunsetchasers.models.ForecastResponse
import com.sunsetchasers.plugins.installErrorHandling
import com.sunsetchasers.services.WeatherService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

class ForecastRouteTest {

    private fun weatherServiceMock(forDate: LocalDate): WeatherService {
        // A single hourly sample is enough here: WeatherService always picks
        // whichever entry is closest to the requested target instant, and with
        // only one entry available that's always the one returned.
        val body = """
            {
              "hourly": {
                "time": ["${forDate}T12:00"],
                "cloud_cover": [15], "cloud_cover_low": [5], "cloud_cover_mid": [10], "cloud_cover_high": [15],
                "visibility": [14000], "temperature_2m": [22], "relative_humidity_2m": [55],
                "wind_speed_10m": [3], "weather_code": [0]
              }
            }
        """.trimIndent()
        val engine = MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val client = HttpClient(engine) { install(ClientContentNegotiation) { json() } }
        return WeatherService(client)
    }

    private fun weatherServiceMockRange(startDate: LocalDate, days: Int): WeatherService {
        // One noon sample per day so each day's target instant resolves to a
        // distinct, verifiably-different entry.
        val times = (0 until days).joinToString(", ") { "\"${startDate.plusDays(it.toLong())}T12:00\"" }
        val perDay = { values: List<Int> -> values.take(days).joinToString(", ") }
        val body = """
            {
              "hourly": {
                "time": [$times],
                "cloud_cover": [${perDay(listOf(10, 20, 30))}],
                "cloud_cover_low": [${perDay(listOf(2, 4, 6))}],
                "cloud_cover_mid": [${perDay(listOf(5, 10, 15))}],
                "cloud_cover_high": [${perDay(listOf(10, 20, 30))}],
                "visibility": [${perDay(listOf(14000, 13000, 12000))}],
                "temperature_2m": [${perDay(listOf(22, 23, 24))}],
                "relative_humidity_2m": [${perDay(listOf(55, 56, 57))}],
                "wind_speed_10m": [${perDay(listOf(3, 4, 5))}],
                "weather_code": [${perDay(listOf(0, 1, 2))}]
              }
            }
        """.trimIndent()
        val engine = MockEngine { respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")) }
        val client = HttpClient(engine) { install(ClientContentNegotiation) { json() } }
        return WeatherService(client)
    }

    private fun requestBody(latitude: Any? = 40.7128, longitude: Any? = -74.0060, date: String?, type: String? = null): String {
        val fields = mutableListOf<String>()
        if (latitude != null) fields += "\"latitude\": $latitude"
        if (longitude != null) fields += "\"longitude\": $longitude"
        if (date != null) fields += "\"date\": \"$date\""
        if (type != null) fields += "\"type\": \"$type\""
        return "{ ${fields.joinToString(", ")} }"
    }

    private fun rangeRequestBody(latitude: Any? = 40.7128, longitude: Any? = -74.0060, type: String? = null): String {
        val fields = mutableListOf<String>()
        if (latitude != null) fields += "\"latitude\": $latitude"
        if (longitude != null) fields += "\"longitude\": $longitude"
        if (type != null) fields += "\"type\": \"$type\""
        return "{ ${fields.joinToString(", ")} }"
    }

    private fun Application.testForecastModule(weatherService: WeatherService) {
        install(ContentNegotiation) { json() }
        install(RateLimit) {
            register { rateLimiter(limit = 100, refillPeriod = 60.seconds) }
        }
        installErrorHandling()
        routing { forecastRoutes(weatherService) }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient { install(ClientContentNegotiation) { json() } }

    @Test
    fun `successful forecast request returns 200 with full payload`() = testApplication {
        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowStr = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE)

        application { testForecastModule(weatherServiceMock(tomorrow)) }
        val client = jsonClient()

        val response = client.post("/api/forecast") {
            contentType(ContentType.Application.Json)
            setBody(requestBody(date = tomorrowStr, type = "both"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload: ForecastResponse = response.body()
        assertEquals("success", payload.status)
        assertEquals(tomorrowStr, payload.date)
        assertNotNull(payload.sunrise)
        assertNotNull(payload.sunset)
        // Both sunrise (astro/nautical/civil dawn + sunrise) and sunset
        // (sunset + civil/nautical/astro dusk) legs contribute 4 points each.
        assertEquals(8, payload.forecastQuality.colorTimeline.size)
    }

    @Test
    fun `type sunrise only omits sunset`() = testApplication {
        val tomorrow = LocalDate.now().plusDays(1)
        application { testForecastModule(weatherServiceMock(tomorrow)) }
        val client = jsonClient()

        val response = client.post("/api/forecast") {
            contentType(ContentType.Application.Json)
            setBody(requestBody(date = tomorrow.format(DateTimeFormatter.ISO_LOCAL_DATE), type = "sunrise"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload: ForecastResponse = response.body()
        assertNotNull(payload.sunrise)
        assertNull(payload.sunset)
    }

    @Test
    fun `invalid latitude returns 400 with INVALID_REQUEST`() = testApplication {
        application { testForecastModule(weatherServiceMock(LocalDate.now().plusDays(1))) }
        val client = jsonClient()

        val response = client.post("/api/forecast") {
            contentType(ContentType.Application.Json)
            setBody(requestBody(latitude = 95.2, date = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertEquals("INVALID_REQUEST", error.code)
    }

    @Test
    fun `date more than 7 days ahead returns 422 with DATE_OUT_OF_RANGE`() = testApplication {
        application { testForecastModule(weatherServiceMock(LocalDate.now().plusDays(20))) }
        val client = jsonClient()

        val farDate = LocalDate.now().plusDays(20).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val response = client.post("/api/forecast") {
            contentType(ContentType.Application.Json)
            setBody(requestBody(date = farDate))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val error: ErrorResponse = response.body()
        assertEquals("DATE_OUT_OF_RANGE", error.code)
    }

    @Test
    fun `missing date returns 400`() = testApplication {
        application { testForecastModule(weatherServiceMock(LocalDate.now().plusDays(1))) }
        val client = jsonClient()

        val response = client.post("/api/forecast") {
            contentType(ContentType.Application.Json)
            setBody(requestBody(date = null))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertEquals("INVALID_REQUEST", error.code)
    }

    @Test
    fun `range request returns 3 days starting today, each with distinct weather`() = testApplication {
        val today = LocalDate.now()
        application { testForecastModule(weatherServiceMockRange(today, 3)) }
        val client = jsonClient()

        // London coordinates: local sunset stays within the same UTC calendar
        // day as local "today", so it unambiguously lands on this mock's
        // same-day noon-UTC sample rather than tipping into the next day's
        // (which a far-from-UTC timezone like New York's can do).
        val response = client.post("/api/forecast/range") {
            contentType(ContentType.Application.Json)
            setBody(rangeRequestBody(latitude = 51.5074, longitude = -0.1278, type = "both"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val payload: ForecastRangeResponse = response.body()
        assertEquals("success", payload.status)
        assertEquals(3, payload.days.size)
        assertEquals(today.toString(), payload.days[0].date)
        assertEquals(today.plusDays(1).toString(), payload.days[1].date)
        assertEquals(today.plusDays(2).toString(), payload.days[2].date)
        assertEquals(22.0, payload.days[0].weather.temperatureC)
        assertEquals(23.0, payload.days[1].weather.temperatureC)
        assertEquals(24.0, payload.days[2].weather.temperatureC)
        assertNotNull(payload.days[0].sunrise)
        assertNotNull(payload.days[0].sunset)
    }

    @Test
    fun `range request days are all within the 48-hour accuracy window, none flagged`() = testApplication {
        val today = LocalDate.now()
        application { testForecastModule(weatherServiceMockRange(today, 3)) }
        val client = jsonClient()

        val response = client.post("/api/forecast/range") {
            contentType(ContentType.Application.Json)
            setBody(rangeRequestBody(latitude = 51.5074, longitude = -0.1278, type = "both"))
        }

        val payload: ForecastRangeResponse = response.body()
        assertEquals(false, payload.days[0].accuracyWarning)
        assertEquals(false, payload.days[1].accuracyWarning)
        assertEquals(false, payload.days[2].accuracyWarning)
    }

    @Test
    fun `range request with invalid latitude returns 400`() = testApplication {
        application { testForecastModule(weatherServiceMockRange(LocalDate.now(), 3)) }
        val client = jsonClient()

        val response = client.post("/api/forecast/range") {
            contentType(ContentType.Application.Json)
            setBody(rangeRequestBody(latitude = 95.2))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error: ErrorResponse = response.body()
        assertEquals("INVALID_REQUEST", error.code)
    }
}
