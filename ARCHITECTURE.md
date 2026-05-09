# Architecture & Data Flow

## System Overview

The app consists of two primary components: a stateless backend API and a React Native frontend. The frontend is responsible for UI, local storage, and user interaction; the backend handles all ephemeris calculations and weather data aggregation.

```
┌─────────────────────────────────────────────────┐
│           React Native Frontend (Expo)          │
│  ┌────────────────┐  ┌──────────────────────┐  │
│  │  UI Screens    │  │  Local Storage       │  │
│  │  - Home        │  │  - Favorite Locs     │  │
│  │  - Forecast    │  │  - Settings          │  │
│  │  - Saved Locs  │  │  (AsyncStorage)      │  │
│  │  - Settings    │  │                      │  │
│  └────────────────┘  └──────────────────────┘  │
│           ↓                                      │
│       API Client (utils/api.js)                 │
│           ↓                                      │
└─────────────────────────────────────────────────┘
           ↓ HTTP (REST)
           ↓ {lat, lon, date}
┌─────────────────────────────────────────────────┐
│         Backend API (Node.js / Python)          │
│  ┌────────────────────────────────────────────┐ │
│  │  POST /api/forecast                        │ │
│  │  Input: {latitude, longitude, date}        │ │
│  └────────────────────────────────────────────┘ │
│    ↓                    ↓                        │
│  ┌──────────────┐  ┌──────────────────────┐   │
│  │ Astronomy    │  │ Weather Service      │   │
│  │ Service      │  │ (OpenWeatherMap API) │   │
│  │ - pymeeus    │  │ - Cloud cover        │   │
│  │ - Sun angles │  │ - Visibility         │   │
│  │ - Azimuth    │  │ - Conditions         │   │
│  └──────────────┘  └──────────────────────┘   │
│    ↓                    ↓                        │
│  ┌──────────────────────────────────────────┐  │
│  │  Forecast Scoring Service                │  │
│  │  Combines astronomy + weather → Quality  │  │
│  └──────────────────────────────────────────┘  │
│           ↓                                      │
└─────────────────────────────────────────────────┘
           ↓ HTTP Response (JSON)
           ↓ Forecast data + quality score
┌─────────────────────────────────────────────────┐
│      Frontend: Display & Local Storage          │
│  - Render map, forecast details, quality       │
│  - Cache result (optional)                      │
│  - Allow user to save location to favorites    │
└─────────────────────────────────────────────────┘
```

## Data Flow Example

**User Action:** "Search for 'Monument Valley, AZ' on 2024-06-15"

1. **Frontend**: User enters location in search box
2. **Geocoding**: LocationSearch component calls geocoding API (built into maps library or separate service) → returns {lat: 37.0, lon: -110.0}
3. **API Request**: Frontend calls backend:
   ```
   POST /api/forecast
   {
     "latitude": 37.0,
     "longitude": -110.0,
     "date": "2024-06-15",
     "type": "both"  // or "sunrise" | "sunset"
   }
   ```
4. **Backend Processing**:
   - Astronomy service calculates sun position for Monument Valley at sunrise/sunset
   - Weather service fetches 7-day forecast for that location
   - Scoring service evaluates conditions → returns quality score
5. **Response**:
   ```json
   {
     "location": "Monument Valley, AZ",
     "date": "2024-06-15",
     "sunrise": {
       "time": "05:34 MST",
       "azimuth": 62.3,
       "altitude_at_horizon": -0.8,
       "altitude_at_6deg": -6.2,
       "altitude_at_18deg": -18.1,
       "twilight_start": "04:52 MST"
     },
     "sunset": {
       "time": "20:12 MST",
       "azimuth": 297.5,
       ...
     },
     "weather": {
       "cloud_cover": 15,
       "visibility": 10,
       "conditions": "clear"
     },
     "quality_score": "excellent",
     "accuracy_warning": false  // true if >48 hours
   }
   ```
6. **Frontend Display**: ForecastScreen renders map with sun positions, displays details, shows quality badge

## Component Responsibilities

### Frontend Components

**HomeScreen**
- Location search input
- GPS auto-detect button
- Recent/favorite location quick access
- Navigation to ForecastScreen

**LocationSearch** (Component)
- Text input with autocomplete
- Calls geocoding service
- Handles error states

**DatePicker** (Component)
- Allows selection of 7 days ahead
- Disables dates beyond 7 days
- Shows current date as default

**ForecastScreen**
- Receives forecast data from API
- Orchestrates display of map, details, quality badge
- Handles save-to-favorites action

**MapView** (Component)
- Displays location on map
- Overlays sunrise/sunset azimuths as lines/arrows
- Shows cardinal directions (N, S, E, W)

**ForecastDetails** (Component)
- Tabbed view: Sunrise | Sunset
- Shows:
  - Times (with timezone)
  - Altitude angles (0°, -6°, -18°)
  - Azimuth
  - Twilight times
  - Weather data (cloud %, visibility)

**QualityScore** (Component)
- Visual badge (Excellent / Good / Fair / Poor)
- Breakdown of factors contributing to score
- Accuracy disclaimer if >48 hours

**SavedLocationsScreen**
- List of locally stored favorite locations
- Quick-access to past forecasts
- Delete location option

**SettingsScreen**
- Unit preference (12/24 hour, metric/imperial)
- Timezone override (optional)
- About, version info

### Backend Services

**Astronomy Service**
- Input: {latitude, longitude, date}
- Output: {sunrise_time, sunset_time, sun_angles, azimuths, twilight_times}
- Library: `pymeeus` (Python) or `astronomy-engine` (Node.js)
- Accuracy: ±1 minute for times, ±0.1° for angles

**Weather Service**
- Input: {latitude, longitude, date}
- Output: {cloud_cover_percent, visibility_km, conditions, ...}
- Calls OpenWeatherMap API (or alternative)
- Caches results to minimize API calls (TTL: 30 min)

**Forecast Scoring Service**
- Input: {astronomy_data, weather_data}
- Output: {quality_score, quality_level, reasoning}
- Scoring algorithm: See FORECAST_SCORING.md

**Forecast Route**
- POST /api/forecast
- Orchestrates calls to above services
- Returns combined JSON response

## Local Storage (AsyncStorage)

Frontend maintains two local data structures:

**Favorite Locations:**
```javascript
{
  "favorite_locations": [
    {
      id: "loc_1",
      name: "Monument Valley, AZ",
      latitude: 37.0,
      longitude: -110.0,
      added_date: "2024-05-01"
    },
    ...
  ]
}
```

**User Settings:**
```javascript
{
  "settings": {
    "time_format": "12h",  // or "24h"
    "temperature_unit": "F",  // or "C"
    "distance_unit": "miles",  // or "km"
    "timezone_override": null  // or "America/Denver"
  }
}
```

## Error Handling

**Frontend Error Cases:**
- Geolocation permission denied → Prompt to enable
- Location not found → Show error message, suggest alternatives
- No internet → Show offline message
- API timeout → Retry with backoff

**Backend Error Cases:**
- Invalid coordinates → Return 400 Bad Request
- Weather API rate limit → Return cached result or 429
- Astronomy calculation error → Return 500 (should be rare)

## Performance Considerations

1. **API Caching**: Backend caches weather results for 30 minutes per location
2. **Network**: Minimize request size, compress responses
3. **Battery**: Only use GPS on explicit user action, not continuous polling
4. **Storage**: Favorite locations stored locally, no sync required

## Security

- API keys (weather, maps) stored server-side only (never in frontend code)
- No user authentication required for V1
- Rate limiting on backend endpoints (prevent abuse)
- Input validation on all API endpoints

## Deployment Architecture

**Frontend:**
- Built via EAS Build (managed by Expo)
- Distributed via App Store (iOS) and Google Play (Android)
- Configuration in `app.json` and EAS build profiles

**Backend:**
- Containerized (Docker) for flexibility
- Deployed to Vercel, Railway, or Heroku
- Environment variables for API keys
- Can scale horizontally if traffic increases
