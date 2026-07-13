package com.sunsetchasers.services

import net.iakovlev.timeshape.TimeZoneEngine
import org.shredzone.commons.suncalc.SunPosition
import org.shredzone.commons.suncalc.SunTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class AstronomyCalculationException(message: String) : Exception(message)

data class SunEventData(
    val time: String,
    val azimuthDegrees: Double,
    val instant: ZonedDateTime
)

data class AstronomyResult(
    val timezone: String,
    val sunrise: SunEventData,
    val sunset: SunEventData,
    val civilDawn: ZonedDateTime?,
    val nauticalDawn: ZonedDateTime?,
    val astronomicalDawn: ZonedDateTime?,
    val civilDusk: ZonedDateTime?,
    val nauticalDusk: ZonedDateTime?,
    val astronomicalDusk: ZonedDateTime?
)

object AstronomyService {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Loading the tz-shape dataset is expensive, so build it once and reuse it.
    private val timeZoneEngine: TimeZoneEngine by lazy { TimeZoneEngine.initialize() }

    fun getTimezone(lat: Double, lon: Double): ZoneId {
        return try {
            timeZoneEngine.query(lat, lon).orElse(ZoneId.of("UTC"))
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }
    }

    fun getSunTimes(lat: Double, lon: Double, dateStr: String): AstronomyResult {
        try {
            val localDate = LocalDate.parse(dateStr)
            val zone = getTimezone(lat, lon)
            // Anchor the calculation at local noon so we land on the requested
            // calendar day even for locations far from UTC.
            val referenceInstant = localDate.atTime(12, 0).atZone(zone)

            val visual = SunTimes.compute().on(referenceInstant).at(lat, lon).execute()
            val civil = SunTimes.compute().on(referenceInstant).at(lat, lon)
                .twilight(SunTimes.Twilight.CIVIL).execute()
            val nautical = SunTimes.compute().on(referenceInstant).at(lat, lon)
                .twilight(SunTimes.Twilight.NAUTICAL).execute()
            val astronomical = SunTimes.compute().on(referenceInstant).at(lat, lon)
                .twilight(SunTimes.Twilight.ASTRONOMICAL).execute()

            val riseInstant = visual.rise
                ?: throw AstronomyCalculationException("No sunrise for $dateStr at ($lat, $lon)")
            val setInstant = visual.set
                ?: throw AstronomyCalculationException("No sunset for $dateStr at ($lat, $lon)")

            val riseAzimuth = SunPosition.compute().on(riseInstant).at(lat, lon).execute().azimuth
            val setAzimuth = SunPosition.compute().on(setInstant).at(lat, lon).execute().azimuth

            return AstronomyResult(
                timezone = zone.id,
                sunrise = SunEventData(riseInstant.format(timeFormatter), riseAzimuth, riseInstant),
                sunset = SunEventData(setInstant.format(timeFormatter), setAzimuth, setInstant),
                civilDawn = civil.rise,
                nauticalDawn = nautical.rise,
                astronomicalDawn = astronomical.rise,
                civilDusk = civil.set,
                nauticalDusk = nautical.set,
                astronomicalDusk = astronomical.set
            )
        } catch (e: AstronomyCalculationException) {
            throw e
        } catch (e: Exception) {
            throw AstronomyCalculationException("Cannot calculate sun times for $dateStr at ($lat, $lon)")
        }
    }

    fun formatOrEmpty(instant: ZonedDateTime?): String = instant?.format(timeFormatter) ?: ""
}
