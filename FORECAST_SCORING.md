# Forecast Scoring Algorithm

The forecast quality score combines weather conditions with astronomical data to predict how visually spectacular a sunrise or sunset will be.

## Overview

**Score Levels:**
- **Level 1 - "Excellent":** Minimal clouds, excellent visibility, optimal sun angle for color
- **Level 2 - "Good":** Some clouds (beneficial for color), good visibility
- **Level 3 - "Fair":** Moderate cloud cover or reduced visibility, still worth shooting
- **Level 4 - "Poor":** Heavy clouds or poor visibility, limited color potential

## Scoring Inputs

### 1. Cloud Cover Rating (altitude-aware "cloud geometry")

Cloud cover data comes from Open-Meteo **by altitude band** — `cloud_cover_low`,
`cloud_cover_mid`, `cloud_cover_high` (each 0-100%) — not a single blended
percentage. Altitude matters enormously for color:

- **High cloud** (cirrus-type, ~20,000 ft+): thin and high enough to catch
  and scatter sunset/sunrise light into vivid color. Sweet spot ~20-70%.
- **Mid cloud** (~6,500-20,000 ft): similar effect, smaller sweet spot
  (~10-50%).
- **Low cloud** (below ~6,500 ft): sits right at the horizon and can block
  the narrow gap the colored light travels through — the "horizon block"
  effect. This isn't a sweet spot, it's a **penalty that gates the whole
  result**: even a gorgeous high/mid-cloud mix is worthless if a low cloud
  bank sits on the horizon.

**Band scoring:**

| High cloud % | Score | | Mid cloud % | Score | | Low cloud % | Gate |
|---|---|---|---|---|---|---|---|
| 0% | 0.65 | | 0% | 0.5 | | 0-10% | 1.0 |
| 1-19% | 0.9 | | 1-9% | 0.85 | | 10-25% | 0.9 |
| 20-70% | 1.0 | | 10-50% | 1.0 | | 25-40% | 0.75 |
| 71-85% | 0.6 | | 51-75% | 0.55 | | 40-60% | 0.4 |
| 86-100% | 0.3 | | 76-100% | 0.25 | | 60-80% | 0.15 |
| | | | | | | 80-100% | 0.05 |

**Combining the bands:** either the high or mid band alone hitting its sweet
spot can produce a great sky, so the color contribution is the **best** of
the two (not an average) — then the low-cloud gate multiplies it down.

```
color_contribution = max(high_band_score, mid_band_score)
cloud_geometry_rating = color_contribution × low_cloud_gate
```

**Implementation** (`server/src/main/kotlin/com/sunsetchasers/services/ScoringService.kt`):

```kotlin
fun highCloudBandScore(highPercent: Double): Double = when {
    highPercent <= 0 -> 0.65
    highPercent < 20 -> 0.9
    highPercent <= 70 -> 1.0
    highPercent <= 85 -> 0.6
    else -> 0.3
}

fun midCloudBandScore(midPercent: Double): Double = when {
    midPercent <= 0 -> 0.5
    midPercent < 10 -> 0.85
    midPercent <= 50 -> 1.0
    midPercent <= 75 -> 0.55
    else -> 0.25
}

fun lowCloudGate(lowPercent: Double): Double = when {
    lowPercent <= 10 -> 1.0
    lowPercent <= 25 -> 0.9
    lowPercent <= 40 -> 0.75
    lowPercent <= 60 -> 0.4
    lowPercent <= 80 -> 0.15
    else -> 0.05
}

fun cloudGeometryRating(lowPercent: Double, midPercent: Double, highPercent: Double): Double {
    if (lowPercent !in 0.0..100.0 || midPercent !in 0.0..100.0 || highPercent !in 0.0..100.0) return 0.1
    val colorContribution = maxOf(highCloudBandScore(highPercent), midCloudBandScore(midPercent))
    return (colorContribution * lowCloudGate(lowPercent)).coerceIn(0.0, 1.0)
}
```

**Worked examples:**
- Totally clear (0/0/0) → `max(0.65, 0.5) × 1.0 = 0.65` — matches the old flat "clear sky" value, so this is a strict upgrade, not a discontinuity.
- Sweet-spot high+mid, no low cloud (low=5, mid=30, high=50) → `max(1.0, 1.0) × 1.0 = 1.0`.
- The same sweet-spot high+mid mix, but with heavy low cloud (low=70, mid=30, high=50) → `max(1.0, 1.0) × 0.15 = 0.15` — the horizon-block gate suppresses an otherwise-great sky.

### 2. Visibility Rating

Visibility indicates atmospheric clarity (in km).

**Why it matters:** Haze, fog, or pollution reduce color saturation and contrast.

**Rating Scale:**

| Visibility (km) | Rating | Score |
|-----------------|--------|-------|
| <1 | Poor | 0.0 |
| 1-3 | Fair | 0.30 |
| 3-6 | Good | 0.70 |
| 6-9 | Excellent | 0.90 |
| 9+ | Excellent | 1.0 |

**Implementation:**

```python
def visibility_rating(visibility_km):
    """
    Returns visibility rating score (0-1).
    Clear air (>9km) is ideal.
    """
    if visibility_km < 0:
        return 0.5  # Unknown, assume neutral
    
    if visibility_km < 1:
        return 0.0
    elif visibility_km < 3:
        return 0.30
    elif visibility_km < 6:
        return 0.70
    elif visibility_km < 9:
        return 0.90
    else:
        return 1.0
```

### 3. Atmospheric Conditions Rating

Weather conditions like storms, precipitation, or haze affect quality.

**Rating Scale:**

| Condition | Score | Notes |
|-----------|-------|-------|
| Clear | 1.0 | Best case |
| Mostly Clear | 0.95 | |
| Partly Cloudy | 0.85 | Expected clouds already counted |
| Cloudy | 0.60 | Heavy cloud cover reduces color |
| Overcast | 0.30 | Very limited light |
| Fog | 0.10 | Extreme visibility reduction |
| Mist/Haze | 0.50 | Atmospheric particles reduce clarity |
| Rain/Drizzle | 0.05 | Clouds + precipitation blocking light |
| Thunderstorm | 0.0 | Not safe for outdoor photography |

**Implementation:**

```python
def conditions_rating(weather_condition):
    """
    Returns condition rating score (0-1).
    Converts weather API conditions to quality score.
    """
    condition_scores = {
        "clear": 1.0,
        "clear sky": 1.0,
        "sunny": 1.0,
        "mostly clear": 0.95,
        "partly cloudy": 0.85,
        "cloudy": 0.60,
        "overcast": 0.30,
        "fog": 0.10,
        "mist": 0.50,
        "haze": 0.50,
        "smoke": 0.40,
        "rain": 0.05,
        "light rain": 0.15,
        "drizzle": 0.10,
        "thunderstorm": 0.0,
    }
    
    condition_lower = weather_condition.lower()
    return condition_scores.get(condition_lower, 0.5)  # Default: neutral
```

### 4. Color Potential Rating (Astronomy-Based)

Sun angle affects color intensity. The sun must be low enough to avoid blue light scattering.

**Theory:** 
- When sun is above -6° altitude, blue light still reaches observer
- At -6° to -18° (nautical twilight), pure reds/oranges dominate
- Below -18°, light becomes very limited and colors mute

**Rating Scale:**

| Sun Altitude | Time Relative to Rise/Set | Color Rating | Score |
|--------------|---------------------------|--------------|-------|
| 0° to -3° | ±10 min from horizon | Excellent (peak orange/red) | 1.0 |
| -3° to -6° | 10-15 min before/after | Excellent (vibrant colors) | 0.95 |
| -6° to -12° | 15-30 min before/after | Good (colors present but muting) | 0.75 |
| -12° to -18° | 30-45 min before/after | Fair (deep purple/reds) | 0.50 |
| Below -18° | >45 min before/after | Poor (minimal light, colors dark) | 0.20 |

**Why this matters to photographers:**
- Peak color occurs when sun is 0° to -3° (±5-10 minutes from horizon)
- Extended twilight (past -18°) can create deep purple skies (dramatic but less colorful)
- Foreground positioning matters: sun angle determines where light hits landscape

**Implementation:**

```python
def color_potential_rating(sun_altitude_degrees):
    """
    Returns color potential score (0-1) based on sun altitude.
    Peak color when sun is near horizon (0° to -6°).
    """
    if sun_altitude_degrees > 0:
        # Sun still above horizon (before sunrise or after sunset)
        # Not useful for twilight colors
        return 0.0
    elif sun_altitude_degrees >= -3:
        return 1.0  # Peak color
    elif sun_altitude_degrees >= -6:
        return 0.95  # Excellent colors
    elif sun_altitude_degrees >= -12:
        return 0.75  # Good colors, beginning to mute
    elif sun_altitude_degrees >= -18:
        return 0.50  # Fair colors, deep purple/red
    else:
        return 0.20  # Poor colors, very dim
```

**For API Response:**
- Return color ratings at 0°, -6°, and -18° altitudes
- Frontend displays this as "Color Timeline" so photographer knows when peak occurs
- Example: "Peak colors 5:34-5:44am, good colors until 6:10am"

---

## Combined Scoring

**Weighting Formula:**

```
Overall Score = 
  (0.35 × Cloud Geometry Rating) +
  (0.20 × Visibility Rating) +
  (0.15 × Condition Rating) +
  (0.30 × Color Potential Rating at 0° altitude)
```

**Rationale:**
- Cloud geometry: Largest factor (35%) — determines if light reaches observer, now altitude-aware (see above) rather than a single blended percentage
- Color potential (30%): When sun angle aligns with good weather, colors are spectacular
- Visibility (20%): Atmospheric clarity affects color saturation
- Conditions (15%): Supplementary factor, many conditions fold into visibility

**Example Calculation:**

```python
def overall_score(cloud_geometry_rating, visibility_rating, condition_rating, color_potential):
    """
    Returns combined forecast quality score (0-1).
    """
    score = (
        0.35 * cloud_geometry_rating +
        0.20 * visibility_rating +
        0.15 * condition_rating +
        0.30 * color_potential
    )
    return min(max(score, 0), 1)  # Clamp to 0-1 range

# Example:
cloud_geometry = 1.0   # low=5%, mid=30%, high=50% — sweet spot, clear horizon
visibility = 0.90      # 8km visibility
condition = 1.0        # Clear
color = 1.0            # Sun at 0° altitude (peak color)

overall = overall_score(cloud_geometry, visibility, condition, color)
# = (0.35*1.0) + (0.20*0.90) + (0.15*1.0) + (0.30*1.0)
# = 0.35 + 0.18 + 0.15 + 0.30
# = 0.98 → "Excellent"
```

---

## Score to Level Mapping

```python
def score_to_level(score):
    """
    Maps numeric score (0-1) to level and label.
    """
    if score >= 0.80:
        return {
            "level": 1,
            "label": "Excellent",
            "emoji": "🌅",
            "color": "#FF6B35"  # Vibrant orange
        }
    elif score >= 0.60:
        return {
            "level": 2,
            "label": "Good",
            "emoji": "🌤️",
            "color": "#FFA500"  # Orange
        }
    elif score >= 0.40:
        return {
            "level": 3,
            "label": "Fair",
            "emoji": "⛅",
            "color": "#FFD700"  # Gold
        }
    else:
        return {
            "level": 4,
            "label": "Poor",
            "emoji": "☁️",
            "color": "#B0C4DE"  # Light gray
        }
```

---

## API Response Fields

The `/api/forecast` endpoint includes:

```json
{
  "forecast_quality": {
    "score": 0.87,
    "level": 1,
    "label": "Excellent",
    "breakdown": {
      "cloud_rating": 0.95,
      "cloud_low_rating": 1.0,
      "cloud_mid_rating": 1.0,
      "cloud_high_rating": 0.9,
      "visibility_rating": 0.90,
      "condition_rating": 1.0,
      "color_potential_rating": 1.0
    },
    "reasoning": "Clear skies with excellent visibility. Peak color window 5:34-5:44am.",
    "color_timeline": [
      { "time": "04:52", "altitude": "-18°", "quality": "Fair - deep purple" },
      { "time": "05:34", "altitude": "0°", "quality": "Excellent - peak orange/red" },
      { "time": "06:10", "altitude": "-6°", "quality": "Good - colors muting" }
    ]
  }
}
```

---

## Edge Cases & Fallbacks

**Missing Data:**
- If cloud cover unavailable: Use 50% (neutral) and reduce confidence
- If visibility unavailable: Assume clear (use max visibility score)
- If conditions unavailable: Use 0.7 (neutral/slightly favorable)

**Extreme Values:**
- Cloud cover > 100% or < 0%: Treat as error, return Poor rating
- Visibility < 0: Treat as error, use fallback value
- Sun altitude validation: If calculation fails, use zero altitude

**Accuracy Warning (>48 hours):**
- Display quality score but add prominent disclaimer
- Reduce confidence visually (fade colors, add warning icon)
- Suggest user re-check forecast closer to date

---

## Testing the Algorithm

**Test Cases:**

1. **Perfect Conditions:**
   - Cloud: 25% (1.0)
   - Visibility: 12km (1.0)
   - Condition: Clear (1.0)
   - Color: Peak altitude (1.0)
   - Expected: ≥0.95 → "Excellent" ✓

2. **Mediocre Conditions:**
   - Cloud: 65% (0.70)
   - Visibility: 5km (0.70)
   - Condition: Partly cloudy (0.85)
   - Color: -12° altitude (0.75)
   - Expected: ~0.73 → "Good" ✓

3. **Poor Conditions:**
   - Cloud: 90% (0.10)
   - Visibility: 2km (0.30)
   - Condition: Overcast (0.30)
   - Color: -20° altitude (0.20)
   - Expected: ~0.23 → "Poor" ✓

---

## Future Enhancements (V2+)

- ~~Cloud altitude from satellite data (high clouds > low clouds)~~ — **done**: Open-Meteo's per-altitude cloud cover now drives the Cloud Geometry Rating above.
- Incorporate wind (affects cloud movement, dust, etc.)
- Humidity levels (affects haze, atmospheric scattering)
- Pressure systems (high pressure = clearer skies)
- Per-event weather sampling (currently `type=both` shares one weather sample taken at sunset time — see API_SPEC.md)
- Historical "golden" dates (patterns over years)
- User feedback loop (photographers rate actual results vs. forecast)
