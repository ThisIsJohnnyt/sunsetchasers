package com.sunsetchasers.services

import com.sunsetchasers.models.WeatherData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScoringServiceTest {

    @Test
    fun `highCloudBandScore rewards the 20-70 percent sweet spot`() {
        assertEquals(0.65, ScoringService.highCloudBandScore(0.0))
        assertEquals(0.9, ScoringService.highCloudBandScore(10.0))
        assertEquals(1.0, ScoringService.highCloudBandScore(50.0))
        assertEquals(0.6, ScoringService.highCloudBandScore(80.0))
        assertEquals(0.3, ScoringService.highCloudBandScore(95.0))
    }

    @Test
    fun `midCloudBandScore rewards the 10-50 percent sweet spot`() {
        assertEquals(0.5, ScoringService.midCloudBandScore(0.0))
        assertEquals(0.85, ScoringService.midCloudBandScore(5.0))
        assertEquals(1.0, ScoringService.midCloudBandScore(30.0))
        assertEquals(0.55, ScoringService.midCloudBandScore(60.0))
        assertEquals(0.25, ScoringService.midCloudBandScore(90.0))
    }

    @Test
    fun `lowCloudGate penalizes heavy horizon-blocking low cloud`() {
        assertEquals(1.0, ScoringService.lowCloudGate(5.0))
        assertEquals(0.9, ScoringService.lowCloudGate(20.0))
        assertEquals(0.75, ScoringService.lowCloudGate(35.0))
        assertEquals(0.4, ScoringService.lowCloudGate(50.0))
        assertEquals(0.15, ScoringService.lowCloudGate(70.0))
        assertEquals(0.05, ScoringService.lowCloudGate(90.0))
    }

    @Test
    fun `cloudGeometryRating for clear sky matches old flat clear-sky value`() {
        assertEquals(0.65, ScoringService.cloudGeometryRating(0.0, 0.0, 0.0))
    }

    @Test
    fun `cloudGeometryRating is 1_0 when both bands hit their sweet spot with no low cloud`() {
        assertEquals(1.0, ScoringService.cloudGeometryRating(5.0, 30.0, 50.0))
    }

    @Test
    fun `cloudGeometryRating gates a great mid-high mix down when low cloud blocks the horizon`() {
        val blocked = ScoringService.cloudGeometryRating(70.0, 30.0, 50.0)
        val clear = ScoringService.cloudGeometryRating(5.0, 30.0, 50.0)

        assertEquals(0.15, blocked)
        assertTrue(blocked < clear, "heavy low cloud should suppress an otherwise-great mid/high mix")
    }

    @Test
    fun `cloudGeometryRating handles out-of-range inputs as invalid data`() {
        assertEquals(0.1, ScoringService.cloudGeometryRating(-5.0, 30.0, 50.0))
        assertEquals(0.1, ScoringService.cloudGeometryRating(10.0, 150.0, 50.0))
    }

    @Test
    fun `visibilityRating calculates correctly`() {
        assertEquals(0.0, ScoringService.visibilityRating(0.5))
        assertEquals(0.3, ScoringService.visibilityRating(2.0))
        assertEquals(0.7, ScoringService.visibilityRating(4.0))
        assertEquals(0.9, ScoringService.visibilityRating(7.0))
        assertEquals(1.0, ScoringService.visibilityRating(12.0))
    }

    @Test
    fun `visibilityRating handles negative values`() {
        assertEquals(0.5, ScoringService.visibilityRating(-5.0))
    }

    @Test
    fun `conditionsRating handles various conditions`() {
        assertEquals(1.0, ScoringService.conditionsRating("clear"))
        assertEquals(1.0, ScoringService.conditionsRating("Clear"))
        assertEquals(0.6, ScoringService.conditionsRating("clouds"))
        assertEquals(0.6, ScoringService.conditionsRating("Cloudy"))
        assertEquals(0.05, ScoringService.conditionsRating("rain"))
        assertEquals(0.2, ScoringService.conditionsRating("snow"))
        assertEquals(0.0, ScoringService.conditionsRating("thunderstorm"))
        assertEquals(0.5, ScoringService.conditionsRating("unknown_condition"))
    }

    @Test
    fun `colorPotentialRating by altitude`() {
        assertEquals(1.0, ScoringService.colorPotentialRating(0.0))
        assertEquals(0.95, ScoringService.colorPotentialRating(-4.0))
        assertEquals(0.75, ScoringService.colorPotentialRating(-8.0))
        assertEquals(0.5, ScoringService.colorPotentialRating(-15.0))
        assertEquals(0.2, ScoringService.colorPotentialRating(-20.0))
    }

    private fun weather(low: Double, mid: Double, high: Double, visibility: Double, conditions: String) = WeatherData(
        cloudCoverPercent = (low + mid + high) / 3.0,
        cloudCoverLowPercent = low,
        cloudCoverMidPercent = mid,
        cloudCoverHighPercent = high,
        visibilityKm = visibility,
        conditions = conditions,
        temperatureC = 20.0,
        humidityPercent = 50.0,
        windSpeedMs = 2.0
    )

    @Test
    fun `perfect conditions produce excellent`() {
        val result = ScoringService.calcScore(weather(low = 0.0, mid = 20.0, high = 30.0, visibility = 12.0, conditions = "clear"), emptyList())

        assertTrue(result.score >= 0.95)
        assertEquals(1, result.level)
        assertEquals("Excellent", result.label)
        assertEquals(1.0, result.breakdown.cloudRating)
        assertEquals(1.0, result.breakdown.visibilityRating)
        assertEquals(1.0, result.breakdown.conditionRating)
        assertEquals(1.0, result.breakdown.colorPotentialRating)
    }

    @Test
    fun `mediocre conditions produce good`() {
        val result = ScoringService.calcScore(weather(low = 50.0, mid = 20.0, high = 15.0, visibility = 5.0, conditions = "overcast"), emptyList())

        assertTrue(result.score >= 0.60)
        assertTrue(result.score < 0.80)
        assertEquals(2, result.level)
        assertEquals("Good", result.label)
    }

    @Test
    fun `poor conditions produce poor`() {
        val result = ScoringService.calcScore(weather(low = 90.0, mid = 90.0, high = 90.0, visibility = 0.5, conditions = "thunderstorm"), emptyList())

        assertTrue(result.score <= 0.40)
        assertEquals(4, result.level)
        assertEquals("Poor", result.label)
    }

    @Test
    fun `fair conditions produce fair`() {
        val result = ScoringService.calcScore(weather(low = 50.0, mid = 60.0, high = 70.0, visibility = 2.0, conditions = "overcast"), emptyList())

        assertTrue(result.score in 0.40..0.5999)
        assertEquals(3, result.level)
        assertEquals("Fair", result.label)
    }

    @Test
    fun `score is clamped between 0 and 1`() {
        val result = ScoringService.calcScore(weather(low = 0.0, mid = 0.0, high = 0.0, visibility = 50.0, conditions = "clear"), emptyList())

        assertTrue(result.score in 0.0..1.0)
    }

    @Test
    fun `breakdown values are all within 0 to 1`() {
        val result = ScoringService.calcScore(weather(low = 10.0, mid = 30.0, high = 40.0, visibility = 10.0, conditions = "clear"), emptyList())

        listOf(
            result.breakdown.cloudRating,
            result.breakdown.cloudLowRating,
            result.breakdown.cloudMidRating,
            result.breakdown.cloudHighRating,
            result.breakdown.visibilityRating,
            result.breakdown.conditionRating,
            result.breakdown.colorPotentialRating
        ).forEach { rating ->
            assertTrue(rating in 0.0..1.0)
        }
    }

    @Test
    fun `reasoning matches level`() {
        val excellent = ScoringService.calcScore(weather(low = 0.0, mid = 20.0, high = 30.0, visibility = 15.0, conditions = "clear"), emptyList())
        val poor = ScoringService.calcScore(weather(low = 90.0, mid = 90.0, high = 90.0, visibility = 1.0, conditions = "thunderstorm"), emptyList())

        assertTrue(excellent.reasoning.contains("excellent", ignoreCase = true))
        assertTrue(poor.reasoning.contains("Limited"))
    }

    @Test
    fun `weighted formula rewards lower low-cloud cover`() {
        val clearLow = ScoringService.calcScore(weather(low = 15.0, mid = 30.0, high = 40.0, visibility = 12.0, conditions = "clear"), emptyList())
        val heavyLow = ScoringService.calcScore(weather(low = 60.0, mid = 30.0, high = 40.0, visibility = 12.0, conditions = "clear"), emptyList())

        assertTrue(clearLow.score > heavyLow.score)
    }
}
