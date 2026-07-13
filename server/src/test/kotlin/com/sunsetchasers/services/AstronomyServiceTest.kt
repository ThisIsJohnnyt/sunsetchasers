package com.sunsetchasers.services

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AstronomyServiceTest {

    private fun parseTimeToMinutes(time: String): Int {
        val (hours, minutes) = time.split(":").map { it.toInt() }
        return hours * 60 + minutes
    }

    private fun minutesDiff(a: String, b: String): Int = abs(parseTimeToMinutes(a) - parseTimeToMinutes(b))

    @Test
    fun `New York summer solstice 2025-06-21`() {
        val result = AstronomyService.getSunTimes(40.7128, -74.0060, "2025-06-21")

        assertTrue(minutesDiff(result.sunrise.time, "05:25") <= 3, "sunrise was ${result.sunrise.time}")
        assertTrue(minutesDiff(result.sunset.time, "20:31") <= 3, "sunset was ${result.sunset.time}")
    }

    @Test
    fun `London spring equinox 2025-03-20`() {
        val result = AstronomyService.getSunTimes(51.5074, -0.1278, "2025-03-20")

        assertTrue(minutesDiff(result.sunrise.time, "06:02") <= 3, "sunrise was ${result.sunrise.time}")
        assertTrue(minutesDiff(result.sunset.time, "18:15") <= 3, "sunset was ${result.sunset.time}")
    }

    @Test
    fun `Sydney winter solstice 2025-06-21 southern hemisphere`() {
        val result = AstronomyService.getSunTimes(-33.8688, 151.2093, "2025-06-21")

        assertTrue(minutesDiff(result.sunrise.time, "07:02") <= 5, "sunrise was ${result.sunrise.time}")
        assertTrue(minutesDiff(result.sunset.time, "16:55") <= 5, "sunset was ${result.sunset.time}")
    }

    @Test
    fun `azimuth range validation`() {
        val result = AstronomyService.getSunTimes(40.7128, -74.0060, "2025-06-21")

        assertTrue(result.sunrise.azimuthDegrees in 0.0..360.0)
        assertTrue(result.sunset.azimuthDegrees in 0.0..360.0)
        assertTrue(result.sunrise.azimuthDegrees < 180.0, "expected NE sunrise, got ${result.sunrise.azimuthDegrees}")
        assertTrue(result.sunset.azimuthDegrees > 180.0, "expected NW sunset, got ${result.sunset.azimuthDegrees}")
    }

    @Test
    fun `twilight times ordering`() {
        val result = AstronomyService.getSunTimes(40.7128, -74.0060, "2025-06-21")

        val astronomicalDawn = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.astronomicalDawn))
        val nauticalDawn = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.nauticalDawn))
        val civilDawn = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.civilDawn))
        val sunrise = parseTimeToMinutes(result.sunrise.time)

        assertTrue(astronomicalDawn < nauticalDawn)
        assertTrue(nauticalDawn < civilDawn)
        assertTrue(civilDawn < sunrise)

        val sunset = parseTimeToMinutes(result.sunset.time)
        val civilDusk = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.civilDusk))
        val nauticalDusk = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.nauticalDusk))
        val astronomicalDusk = parseTimeToMinutes(AstronomyService.formatOrEmpty(result.astronomicalDusk))

        assertTrue(sunset < civilDusk)
        assertTrue(civilDusk < nauticalDusk)
        assertTrue(nauticalDusk < astronomicalDusk)
    }

    @Test
    fun `timezone lookup`() {
        val nyTz = AstronomyService.getTimezone(40.7128, -74.0060).id
        val londonTz = AstronomyService.getTimezone(51.5074, -0.1278).id
        val sydneyTz = AstronomyService.getTimezone(-33.8688, 151.2093).id

        assertTrue(nyTz == "America/New_York" || nyTz == "America/Toronto", "was $nyTz")
        assertTrue(londonTz.startsWith("Europe/London") || londonTz.contains("GMT"), "was $londonTz")
        assertTrue(sydneyTz.startsWith("Australia"), "was $sydneyTz")
    }
}
