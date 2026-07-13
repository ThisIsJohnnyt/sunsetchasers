# Backend API Specification

## Endpoints

### POST /api/forecast

Fetch a sunrise/sunset forecast for a given location and date.

#### Request

```json
{
  "latitude": 37.0,
  "longitude": -110.0,
  "date": "2024-06-15",
  "type": "both"
}
```

**Parameters:**
- `latitude` (number, required): Decimal latitude (-90 to 90)
- `longitude` (number, required): Decimal longitude (-180 to 180)
- `date` (string, required): ISO 8601 date format "YYYY-MM-DD"
- `type` (string, optional): "sunrise" | "sunset" | "both" (default: "both")

**Validation:**
- Latitude must be within ±90°
- Longitude must be within ±180°
- Date must be within 7 days of today
- Return 400 Bad Request with error details if invalid

#### Response

```json
{
  "status": "success",
  "location": {
    "latitude": 37.0,
    "longitude": -110.0,
    "name": "Monument Valley, AZ"
  },
  "date": "2024-06-15",
  "timezone": "America/Denver",
  "accuracy_warning": false,
  "sunrise": {
    "time": "05:34",
    "azimuth": 62.3,
    "altitudes": {
      "at_horizon": 0.0,
      "at_neg6": -6.0,
      "at_neg18": -18.0
    },
    "twilight_civil_start": "04:52",
    "twilight_nautical_start": "04:16",
    "twilight_astronomical_start": "03:40"
  },
  "sunset": {
    "time": "20:12",
    "azimuth": 297.5,
    "altitudes": {
      "at_horizon": 0.0,
      "at_neg6": -6.0,
      "at_neg18": -18.0
    },
    "twilight_civil_end": "20:54",
    "twilight_nautical_end": "21:30",
    "twilight_astronomical_end": "22:06"
  },
  "weather": {
    "cloud_cover_percent": 15,
    "cloud_cover_low_percent": 0,
    "cloud_cover_mid_percent": 5,
    "cloud_cover_high_percent": 15,
    "visibility_km": 10,
    "conditions": "clear",
    "temperature_c": 18,
    "humidity_percent": 65,
    "wind_speed_ms": 3.2
  },
  "forecast_quality": {
    "score": 0.94,
    "level": 1,
    "label": "Excellent",
    "breakdown": {
      "cloud_rating": 1.0,
      "cloud_low_rating": 1.0,
      "cloud_mid_rating": 0.85,
      "cloud_high_rating": 0.9,
      "visibility_rating": 1.0,
      "condition_rating": 1.0,
      "color_potential_rating": 1.0
    },
    "reasoning": "Clear skies with excellent visibility. Minimal cloud cover expected. Peak color window at sunrise/sunset.",
    "color_timeline": [
      { "time": "04:52", "altitude": "-18°", "quality": "Fair - deep purple/reds" },
      { "time": "05:34", "altitude": "0°", "quality": "Excellent - peak orange/red" }
    ]
  },
  "timestamp": "2024-05-09T15:32:00Z"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | "success" or "error" |
| `location.name` | string | Human-readable location name (falls back to `"Lat X, Lon Y"` — no reverse geocoding API is configured) |
| `date` | string | ISO 8601 date (echo of request) |
| `timezone` | string | IANA timezone for location (e.g., "America/Denver") |
| `accuracy_warning` | boolean | true if date is >48 hours in future |
| `sunrise/sunset.time` | string | HH:MM in location timezone |
| `sunrise/sunset.azimuth` | number | 0-360°, compass direction (0=N, 90=E, 180=S, 270=W) |
| `sunrise/sunset.altitudes` | object | Sun altitude angles (0° = horizon) |
| `sunrise/sunset.twilight_*` | string | Twilight event times (civil, nautical, astronomical) |
| `weather.cloud_cover_percent` | number | Blended cloud cover, 0-100% |
| `weather.cloud_cover_low/mid/high_percent` | number | Cloud cover by altitude band, 0-100% each (from Open-Meteo) — this is what actually predicts sunset/sunrise color, not the blended value |
| `weather.*` | object | Remaining weather conditions at forecast time |
| `forecast_quality.score` | number | 0-1 |
| `forecast_quality.level` | number | 1 (Excellent) – 4 (Poor) |
| `forecast_quality.label` | string | "Excellent" \| "Good" \| "Fair" \| "Poor" |
| `forecast_quality.breakdown.*` | number | Component sub-scores, 0-1 each, all "higher = better" (see FORECAST_SCORING.md) |
| `forecast_quality.color_timeline` | array | Color quality at each twilight altitude band for the requested event(s) |
| `timestamp` | string | API response generation time (ISO 8601) |

**Weather sampling note:** weather is sampled once per request, at the nearest hourly forecast to sunset time for `type=sunset` and `type=both`, and to sunrise time for `type=sunrise`. A future version may return per-event weather for `type=both` rather than sharing one sample between sunrise and sunset.

#### Error Responses

**400 Bad Request:**
```json
{
  "status": "error",
  "code": "INVALID_REQUEST",
  "message": "Latitude must be between -90 and 90",
  "details": {
    "field": "latitude",
    "value": 95.2
  }
}
```

**422 Unprocessable Entity (Date out of range):**
```json
{
  "status": "error",
  "code": "DATE_OUT_OF_RANGE",
  "message": "Forecast date must be within 7 days of today",
  "details": {
    "requested_date": "2024-06-20",
    "max_forecast_date": "2024-05-16",
    "days_ahead": 12
  }
}
```

**429 Too Many Requests:**
```json
{
  "status": "error",
  "code": "RATE_LIMITED",
  "message": "Too many requests. Please try again in 60 seconds.",
  "retry_after": 60
}
```

**500 Internal Server Error:**
```json
{
  "status": "error",
  "code": "CALCULATION_ERROR",
  "message": "Failed to calculate sunrise/sunset times",
  "request_id": "req_abc123xyz"
}
```

---

## Implementation Notes

### Astronomy Calculations

**Library:** [`commons-suncalc`](https://shredzone.org/maven/commons-suncalc/) (JVM/Kotlin, Apache 2.0). Timezone lookup is offline via [`timeshape`](https://github.com/RomanIakovlev/timeshape) (no external geocoding API call needed).

**Accuracy Requirements:**
- Sun times: ±1 minute
- Angles: ±0.1°
- Azimuth: ±1°

**Implementation** (`server/src/main/kotlin/com/sunsetchasers/services/AstronomyService.kt`):

```kotlin
val zone = timeZoneEngine.query(lat, lon).orElse(ZoneId.of("UTC"))
val referenceInstant = localDate.atTime(12, 0).atZone(zone)

val visual = SunTimes.compute().on(referenceInstant).at(lat, lon).execute()
val civil = SunTimes.compute().on(referenceInstant).at(lat, lon).twilight(SunTimes.Twilight.CIVIL).execute()
val nautical = SunTimes.compute().on(referenceInstant).at(lat, lon).twilight(SunTimes.Twilight.NAUTICAL).execute()
val astronomical = SunTimes.compute().on(referenceInstant).at(lat, lon).twilight(SunTimes.Twilight.ASTRONOMICAL).execute()

val riseAzimuth = SunPosition.compute().on(visual.rise).at(lat, lon).execute().azimuth
```

### Weather Data Integration

**Provider:** [Open-Meteo](https://open-meteo.com) — free, no API key or account required (10,000 requests/day, 600/min for non-commercial use). Chosen specifically because it exposes cloud cover **by altitude** (`cloud_cover_low` / `_mid` / `_high`), which OpenWeatherMap, WeatherAPI.com, and NOAA/NWS do not — and altitude is what actually predicts sunrise/sunset color quality (see FORECAST_SCORING.md).

**Request:** `GET https://api.open-meteo.com/v1/forecast` with `hourly=cloud_cover,cloud_cover_low,cloud_cover_mid,cloud_cover_high,visibility,temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code`, `wind_speed_unit=ms`, `timezone=UTC`, `past_days=1`, `forecast_days=9` (the padding avoids missing the target local-date hour at extreme UTC offsets). Weather codes (WMO) are mapped to condition strings in `WeatherService.weatherCodeToCondition()`.

**Caching Strategy** (`server/.../services/WeatherService.kt`):
- The raw hourly arrays are cached per `(lat, lon, dateStr)` for 30 minutes — not a single picked sample, so a sunrise request and a sunset request for the same location/date share one upstream fetch.
- The nearest-hour index is picked fresh on every call, using the actual computed sunrise/sunset instant from `AstronomyService` (not a fixed "solar noon" guess).

### Forecast Scoring Algorithm

See **FORECAST_SCORING.md** for the full algorithm, including the cloud-geometry (altitude-aware) formula.

### Reverse Geocoding

Convert lat/lon to human-readable location name:
- Use Maps API geocoding (Google Maps, Mapbox, etc.)
- Cache results (location names don't change often)
- Fall back to "Lat {lat}, Lon {lon}" if lookup fails

### Timezone Handling

- Use `pytz` (Python) or `moment-timezone` (Node.js)
- Look up timezone by lat/lon using a library like `timezonefinder`
- Return IANA timezone string in response
- All times in response are in location's local timezone

---

## Example Requests & Responses

### Example 1: Perfect Photography Conditions

**Request:**
```bash
curl -X POST http://localhost:3000/api/forecast \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 40.7128,
    "longitude": -74.0060,
    "date": "2024-06-21",
    "type": "both"
  }'
```

**Response:** (simplified)
```json
{
  "status": "success",
  "location": {
    "name": "New York, NY",
    "latitude": 40.7128,
    "longitude": -74.0060
  },
  "sunrise": {
    "time": "05:27",
    "azimuth": 62.5,
    "twilight_civil_start": "04:52"
  },
  "sunset": {
    "time": "20:32",
    "azimuth": 297.8
  },
  "weather": {
    "cloud_cover_percent": 10,
    "visibility_km": 15,
    "conditions": "clear"
  },
  "forecast_quality": {
    "score": "excellent",
    "level": 1
  }
}
```

### Example 2: Date >48 Hours (Accuracy Warning)

Same request, but date is "2024-06-25" (16 days ahead):
```json
{
  "status": "error",
  "code": "DATE_OUT_OF_RANGE",
  "message": "Forecast date must be within 7 days of today"
}
```

Request for "2024-05-18" (more than 48 hours ahead, still within the 7-day window):
```json
{
  "accuracy_warning": true,
  "forecast_quality": {
    "score": 0.72,
    "level": 2,
    "label": "Good"
  }
}
```
The client is expected to show a disclaimer when `accuracy_warning` is `true` — see "Notes for Development" in README.md.

---

## Rate Limiting & Performance

- **Rate Limit:** 100 requests per minute per IP (configurable)
- **Timeout:** 10 second timeout per request
- **Response Time Target:** <1 second (P95) for cached results, <3 seconds for fresh weather calls
- **Caching:** 30-minute TTL on weather data, 24-hour TTL on astronomy (doesn't change)

## Security

- Input validation on all parameters (type checking, range validation)
- Rate limiting to prevent abuse
- No API keys to leak — Open-Meteo requires none, and no other third-party keys are used server-side
- Use HTTPS in production (the native Android client talks to this API directly; no CORS applies since there's no browser involved)
