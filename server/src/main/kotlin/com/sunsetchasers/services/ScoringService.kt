package com.sunsetchasers.services

import com.sunsetchasers.models.ColorTimelineEntry
import com.sunsetchasers.models.ForecastQuality
import com.sunsetchasers.models.ScoreBreakdown
import com.sunsetchasers.models.WeatherData
import java.time.ZonedDateTime
import kotlin.math.round

object ScoringService {

    /**
     * High-altitude (cirrus-type) cloud score. Sweet spot ~20-70%: thin high
     * cloud catches sunset/sunrise color; too little means nothing to catch
     * the color, too much blocks it.
     */
    fun highCloudBandScore(highPercent: Double): Double = when {
        highPercent <= 0 -> 0.65
        highPercent < 20 -> 0.9
        highPercent <= 70 -> 1.0
        highPercent <= 85 -> 0.6
        else -> 0.3
    }

    /** Mid-altitude cloud score. Sweet spot ~10-50%. */
    fun midCloudBandScore(midPercent: Double): Double = when {
        midPercent <= 0 -> 0.5
        midPercent < 10 -> 0.85
        midPercent <= 50 -> 1.0
        midPercent <= 75 -> 0.55
        else -> 0.25
    }

    /**
     * Low cloud sits right at the horizon and can block the narrow gap the
     * colored light travels through ("horizon block") even when the mid/high
     * layers look great. Acts as a multiplicative gate on the color
     * contribution rather than an additive term.
     */
    fun lowCloudGate(lowPercent: Double): Double = when {
        lowPercent <= 10 -> 1.0
        lowPercent <= 25 -> 0.9
        lowPercent <= 40 -> 0.75
        lowPercent <= 60 -> 0.4
        lowPercent <= 80 -> 0.15
        else -> 0.05
    }

    /**
     * Altitude-aware replacement for a single blended cloud-cover rating.
     * Either the mid or high band alone hitting its sweet spot can produce a
     * great sky (best-of, not an average), but heavy low cloud gates the
     * result down regardless of how good the upper layers look.
     */
    fun cloudGeometryRating(lowPercent: Double, midPercent: Double, highPercent: Double): Double {
        if (lowPercent !in 0.0..100.0 || midPercent !in 0.0..100.0 || highPercent !in 0.0..100.0) return 0.1
        val colorContribution = maxOf(highCloudBandScore(highPercent), midCloudBandScore(midPercent))
        return (colorContribution * lowCloudGate(lowPercent)).coerceIn(0.0, 1.0)
    }

    fun visibilityRating(visibilityKm: Double): Double {
        if (visibilityKm < 0) return 0.5
        return when {
            visibilityKm < 1 -> 0.0
            visibilityKm < 3 -> 0.3
            visibilityKm < 6 -> 0.7
            visibilityKm < 9 -> 0.9
            else -> 1.0
        }
    }

    private val CONDITION_RATINGS = mapOf(
        "clear" to 1.0,
        "clear sky" to 1.0,
        "sunny" to 1.0,
        "mostly clear" to 0.95,
        "partly cloudy" to 0.85,
        "cloudy" to 0.6,
        "clouds" to 0.6,
        "overcast" to 0.3,
        "fog" to 0.1,
        "mist" to 0.5,
        "haze" to 0.5,
        "smoke" to 0.4,
        "rain" to 0.05,
        "light rain" to 0.15,
        "drizzle" to 0.1,
        "snow" to 0.2,
        "thunderstorm" to 0.0
    )

    fun conditionsRating(condition: String?): Double =
        CONDITION_RATINGS[condition?.lowercase()] ?: 0.5

    fun colorPotentialRating(sunAltitudeDegrees: Double): Double {
        if (sunAltitudeDegrees > 0) return 0.0
        return when {
            sunAltitudeDegrees >= -3 -> 1.0
            sunAltitudeDegrees >= -6 -> 0.95
            sunAltitudeDegrees >= -12 -> 0.75
            sunAltitudeDegrees >= -18 -> 0.5
            else -> 0.2
        }
    }

    private fun colorQualityDescription(altitudeDegrees: Double): String = when {
        altitudeDegrees >= -3 -> "Excellent - peak orange/red"
        altitudeDegrees >= -6 -> "Excellent - vibrant colors"
        altitudeDegrees >= -12 -> "Good - colors present but muting"
        altitudeDegrees >= -18 -> "Fair - deep purple/reds"
        else -> "Poor - minimal light"
    }

    private fun timelineEntry(instant: ZonedDateTime?, altitudeDegrees: Double, formatTime: (ZonedDateTime?) -> String): ColorTimelineEntry? {
        if (instant == null) return null
        return ColorTimelineEntry(
            time = formatTime(instant),
            altitude = "${altitudeDegrees.toInt()}°",
            quality = colorQualityDescription(altitudeDegrees)
        )
    }

    /**
     * Chronological color timeline around a sunrise (dark -> light) and/or
     * sunset (light -> dark), built from the twilight instants already
     * computed by [AstronomyService].
     */
    fun buildColorTimeline(
        includeSunrise: Boolean,
        includeSunset: Boolean,
        astronomy: AstronomyResult,
        formatTime: (ZonedDateTime?) -> String
    ): List<ColorTimelineEntry> {
        val entries = mutableListOf<ColorTimelineEntry?>()
        if (includeSunrise) {
            entries += timelineEntry(astronomy.astronomicalDawn, -18.0, formatTime)
            entries += timelineEntry(astronomy.nauticalDawn, -12.0, formatTime)
            entries += timelineEntry(astronomy.civilDawn, -6.0, formatTime)
            entries += timelineEntry(astronomy.sunrise.instant, 0.0, formatTime)
        }
        if (includeSunset) {
            entries += timelineEntry(astronomy.sunset.instant, 0.0, formatTime)
            entries += timelineEntry(astronomy.civilDusk, -6.0, formatTime)
            entries += timelineEntry(astronomy.nauticalDusk, -12.0, formatTime)
            entries += timelineEntry(astronomy.astronomicalDusk, -18.0, formatTime)
        }
        return entries.filterNotNull()
    }

    fun calcScore(weather: WeatherData, colorTimeline: List<ColorTimelineEntry>): ForecastQuality {
        val lowGateVal = lowCloudGate(weather.cloudCoverLowPercent)
        val midBandVal = midCloudBandScore(weather.cloudCoverMidPercent)
        val highBandVal = highCloudBandScore(weather.cloudCoverHighPercent)
        val cloudRatingVal = cloudGeometryRating(
            weather.cloudCoverLowPercent,
            weather.cloudCoverMidPercent,
            weather.cloudCoverHighPercent
        )
        val visibilityRatingVal = visibilityRating(weather.visibilityKm)
        val conditionRatingVal = conditionsRating(weather.conditions)

        // Per FORECAST_SCORING.md's combined-scoring formula, the color-potential
        // component of the *overall* score is evaluated "at 0° altitude" — i.e. it
        // assumes the peak-color moment is caught, and is always at its maximum
        // (1.0). The color_timeline field is what actually differentiates how the
        // color quality varies through the twilight window.
        val colorPotentialVal = colorPotentialRating(0.0)

        val overallScore = (0.35 * cloudRatingVal) +
            (0.20 * visibilityRatingVal) +
            (0.15 * conditionRatingVal) +
            (0.30 * colorPotentialVal)
        val clampedScore = overallScore.coerceIn(0.0, 1.0)

        val level: Int
        val label: String
        when {
            clampedScore >= 0.8 -> { level = 1; label = "Excellent" }
            clampedScore >= 0.6 -> { level = 2; label = "Good" }
            clampedScore >= 0.4 -> { level = 3; label = "Fair" }
            else -> { level = 4; label = "Poor" }
        }

        val reasoning = when (level) {
            1 -> "Clear skies with excellent visibility. Minimal cloud cover expected. Peak color window at sunrise/sunset."
            2 -> "Good conditions with some clouds. Visibility is adequate. Colors should be visible."
            3 -> "Moderate cloud cover or reduced visibility. Some color expected but potentially muted."
            else -> "Heavy clouds or poor visibility. Limited color potential. Challenging conditions."
        }

        return ForecastQuality(
            score = round2(clampedScore),
            level = level,
            label = label,
            breakdown = ScoreBreakdown(
                cloudRating = round2(cloudRatingVal),
                cloudLowRating = round2(lowGateVal),
                cloudMidRating = round2(midBandVal),
                cloudHighRating = round2(highBandVal),
                visibilityRating = round2(visibilityRatingVal),
                conditionRating = round2(conditionRatingVal),
                colorPotentialRating = round2(colorPotentialVal)
            ),
            reasoning = reasoning,
            colorTimeline = colorTimeline
        )
    }

    private fun round2(value: Double): Double = round(value * 100) / 100.0
}
