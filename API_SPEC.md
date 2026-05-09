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
    "visibility_km": 10,
    "conditions": "clear",
    "temperature_c": 18,
    "humidity_percent": 65,
    "wind_speed_ms": 3.2
  },
  "forecast_quality": {
    "score": "excellent",
    "level": 1,
    "cloud_rating": "excellent",
    "atmospheric_rating": "excellent",
    "reasoning": "Clear skies with excellent visibility. Minimal cloud cover expected."
  },
  "timestamp": "2024-05-09T15:32:00Z"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `status` | string | "success" or "error" |
| `location.name` | string | Human-readable location name (reverse geocoded) |
| `date` | string | ISO 8601 date (echo of request) |
| `timezone` | string | IANA timezone for location (e.g., "America/Denver") |
| `accuracy_warning` | boolean | true if date is >48 hours in future |
| `sunrise/sunset.time` | string | HH:MM in location timezone |
| `sunrise/sunset.azimuth` | number | 0-360°, compass direction (0=N, 90=E, 180=S, 270=W) |
| `sunrise/sunset.altitudes` | object | Sun altitude angles (0° = horizon) |
| `sunrise/sunset.twilight_*` | string | Twilight event times (civil, nautical, astronomical) |
| `weather.*` | object | Weather conditions at forecast time |
| `forecast_quality.score` | string | "excellent" \| "good" \| "fair" \| "poor" |
| `forecast_quality.level` | number | 1-4 (for UI sorting/filtering) |
| `forecast_quality.*_rating` | string | Component breakdowns (see FORECAST_SCORING.md) |
| `timestamp` | string | API response generation time (ISO 8601) |

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

**Library Selection:**
- **Python:** `pymeeus` (pure Python, no C dependencies)
- **Node.js:** `astronomy-engine` (npm package)

**Accuracy Requirements:**
- Sun times: ±1 minute
- Angles: ±0.1°
- Azimuth: ±1°

**Implementation Pseudo-code:**

```python
from pymeeus.Epoch import Epoch
from pymeeus.Sun import Sun

def calculate_sunrise_sunset(lat, lon, date):
    # date is a datetime.date object
    epoch = Epoch.from_gregorian_date(date.year, date.month, date.day)
    
    sun = Sun(epoch)
    sunrise_epoch = sun.sunrise(Longitude(lon), Latitude(lat))
    sunset_epoch = sun.sunset(Longitude(lon), Latitude(lat))
    
    sunrise_time = sunrise_epoch.jde().to_datetime()  # UTC
    sunset_time = sunset_epoch.jde().to_datetime()    # UTC
    
    # Convert to local timezone
    local_tz = get_timezone(lat, lon)
    sunrise_local = sunrise_time.astimezone(local_tz)
    sunset_local = sunset_time.astimezone(local_tz)
    
    # Calculate sun altitude angles at specific times
    angles = {
        "at_horizon": 0,
        "at_neg6": calculate_altitude(epoch, lat, lon, -6),
        "at_neg18": calculate_altitude(epoch, lat, lon, -18)
    }
    
    # Calculate azimuths
    sunrise_azimuth = calculate_azimuth(sunrise_epoch, lat, lon)
    sunset_azimuth = calculate_azimuth(sunset_epoch, lat, lon)
    
    return {
        "sunrise_time": sunrise_local,
        "sunrise_azimuth": sunrise_azimuth,
        "sunset_time": sunset_local,
        "sunset_azimuth": sunset_azimuth,
        "angles": angles
    }
```

### Weather Data Integration

**API Choice Considerations:**
- **OpenWeatherMap**: Free tier, 60 calls/min, decent cloud data
- **WeatherAPI.com**: Free tier, 1M calls/month, good cloud cover
- **NOAA (US only)**: Free, no rate limit, very accurate for US

**Caching Strategy:**
- Cache weather results for 30 minutes per (lat, lon, date) tuple
- Use in-memory cache or Redis depending on deployment
- Invalidate cache when new forecast data is published (usually 6-hour intervals)

**Rate Limiting:**
- Implement backoff if weather API rate limits hit
- Return cached result (even if stale) rather than failing
- Log warning if falling back to cache >1 hour old

### Forecast Scoring Algorithm

See **FORECAST_SCORING.md** for detailed algorithm.

**Quick Summary:**
- Cloud cover: 0-50% = excellent, 50-70% = good, 70-85% = fair, >85% = poor
- Visibility: >9km = excellent, >6km = good, >3km = fair, <3km = poor
- Combination: Weighted average → final score

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

Request for "2024-05-18" (9 days ahead, still within 7 days but would have warning):
```json
{
  "forecast_quality": {
    "score": "good",
    "level": 2,
    "accuracy_warning": true,
    "warning_message": "Forecasts beyond 48 hours have reduced accuracy. Weather conditions may change."
  }
}
```

---

## Rate Limiting & Performance

- **Rate Limit:** 100 requests per minute per IP (configurable)
- **Timeout:** 10 second timeout per request
- **Response Time Target:** <1 second (P95) for cached results, <3 seconds for fresh weather calls
- **Caching:** 30-minute TTL on weather data, 24-hour TTL on astronomy (doesn't change)

## Security

- Input validation on all parameters (type checking, range validation)
- Rate limiting to prevent abuse
- CORS headers configured to allow frontend domain(s)
- No sensitive data in error messages (no API key leaks, etc.)
- Use HTTPS in production
