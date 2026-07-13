# Architecture & Data Flow

## System Overview

The app consists of two components: a stateless Kotlin/Ktor backend API, and a native Android app built with Kotlin/Jetpack Compose in a multi-module structure. The Android app handles UI, local storage (favorites, settings), and user interaction; the backend handles all ephemeris calculations and weather data aggregation. No third-party API keys are required anywhere in the stack.

```
┌───────────────────────────────────────────────────────────┐
│                  Android App (Kotlin/Compose)              │
│                                                              │
│  :app — Hilt Application, MainActivity, NavHost             │
│    │                                                         │
│  ┌─┴──────────────┬──────────────────┬───────────────────┐ │
│  │ :feature:       │ :feature:        │ :feature:         │ │
│  │ forecast        │ favorites        │ settings          │ │
│  │ (form, map,     │ (list + delete,  │ (units/theme      │ │
│  │  result cards)  │  quick-picker)   │  pickers)         │ │
│  └─┬──────────────┴──────────────────┴───────────────────┘ │
│    │                                                         │
│  ┌─┴────────────┬───────────────┬────────────────┬────────┐│
│  │ :core:network │ :core:database│ :core:datastore│:core:  ││
│  │ (Ktor client, │ (Room —       │ (Jetpack       │design- ││
│  │  DTOs, mapper)│  favorites)   │  DataStore —   │system  ││
│  │               │               │  settings)     │(Material││
│  │               │               │                │ 3 theme)││
│  └──────┬────────┴───────────────┴────────────────┴────────┘│
│         │                    :core:model (shared domain types)│
└─────────┼───────────────────────────────────────────────────┘
          ↓ HTTP POST (Ktor client, no API key)
          ↓ {latitude, longitude, date, type}
┌─────────┼───────────────────────────────────────────────────┐
│         ↓        Backend API (Kotlin / Ktor, Netty)          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  POST /api/forecast  (ForecastRoutes.kt)                │ │
│  └────────────────────────────────────────────────────────┘ │
│    ↓                              ↓                          │
│  ┌──────────────────┐   ┌──────────────────────────────┐   │
│  │ AstronomyService  │   │ WeatherService                │   │
│  │ - commons-suncalc │   │ - Open-Meteo (free, no key)   │   │
│  │ - timeshape (tz)  │   │ - cloud cover by altitude      │   │
│  │ - sun angles/azimuth│ │ - 30-min in-memory cache      │   │
│  └──────────────────┘   └──────────────────────────────┘   │
│    ↓                              ↓                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  ScoringService                                          │ │
│  │  Altitude-aware cloud geometry + visibility + conditions │ │
│  │  + color potential → forecast_quality                    │ │
│  └────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
          ↓ JSON response (ForecastResponse)
          ↓ astronomy + weather + forecast_quality
┌───────────────────────────────────────────────────────────┐
│      Android: Display & Local Storage                      │
│  - Render map (osmdroid), forecast cards, quality badge     │
│  - Save location to favorites (Room, capped at 5)           │
│  - Apply units/theme from settings (DataStore)               │
└───────────────────────────────────────────────────────────┘
```

## Data Flow Example

**User Action:** Enter coordinates for Monument Valley, AZ on 2026-07-15, tap "Get Forecast"

1. **Android**: `ForecastScreen` collects lat/lon/date/type from the form, `ForecastViewModel.fetchForecast()` is called
2. **API Request**: `:core:network`'s `ForecastApi` (Ktor client) calls the backend:
   ```
   POST /api/forecast
   {
     "latitude": 37.0,
     "longitude": -110.0,
     "date": "2026-07-15",
     "type": "both"
   }
   ```
3. **Backend Processing** (`ForecastRoutes.kt`):
   - Validates input (lat/lon range, date within 7 days)
   - `AstronomyService` calculates sun position for the coordinates at sunrise/sunset
   - `WeatherService` fetches weather from Open-Meteo, sampled at the nearest hour to the actual sunset instant (or sunrise instant for `type=sunrise`)
   - `ScoringService` evaluates conditions → returns `forecast_quality` with a full breakdown
4. **Response**: see API_SPEC.md for the full JSON shape (astronomy, weather including cloud-by-altitude, forecast_quality with breakdown and color_timeline)
5. **Android Display**: `ForecastMapper` converts the network DTOs to domain types, `ForecastScreen` renders the osmdroid map with azimuth overlays, sunrise/sunset cards, weather summary (unit-aware), and the color timeline

## Module Responsibilities

### Android modules

**`:core:model`** — Plain Kotlin domain types shared across the app (`Forecast`, `WeatherConditions`, `FavoriteLocation`, `UserSettings`, etc.), no Android or serialization dependencies.

**`:core:network`** — Ktor HTTP client, `@Serializable` DTOs matching the backend's JSON contract, and a mapper from DTOs to domain types. Exposes `ForecastApi`.

**`:core:database`** — Room database and DAO for favorite locations, plus a `FavoritesRepository` that maps entities to domain types and enforces the 5-favorite cap.

**`:core:datastore`** — Jetpack DataStore (Preferences) wrapped in a `SettingsRepository` exposing a `Flow<UserSettings>` (units, theme).

**`:core:designsystem`** — Material 3 theme (light/dark color schemes, typography).

**`:feature:forecast`** — The main screen: location/date/type form, osmdroid map with azimuth lines, sunrise/sunset cards, weather summary, color timeline, favorites quick-picker, and the "save as favorite" dialog.

**`:feature:favorites`** — Full favorites management screen (list + delete).

**`:feature:settings`** — Units and theme picker screen.

**`:app`** — Hilt `Application`, `MainActivity` (applies the theme reactively from `SettingsRepository`), and the `NavHost` wiring the three feature screens together.

### Backend services

**`AstronomyService`** (`server/.../services/AstronomyService.kt`)
- Input: `{latitude, longitude, date}`
- Output: sunrise/sunset times, azimuths, twilight times (civil/nautical/astronomical), all as precise instants
- Library: `commons-suncalc`; timezone lookup via `timeshape` (fully offline, no geocoding API)
- Accuracy: ±1 minute for times, ±0.1° for angles, ±1° for azimuth

**`WeatherService`** (`server/.../services/WeatherService.kt`)
- Input: `{latitude, longitude, dateStr, targetInstant}`
- Output: cloud cover (blended + low/mid/high), visibility, conditions, temperature, humidity, wind
- Calls Open-Meteo (free, no API key)
- Caches the raw hourly forecast per `(lat, lon, date)` for 30 minutes; the nearest-hour sample is picked fresh on every call from the actual sunrise/sunset instant, not a fixed time

**`ScoringService`** (`server/.../services/ScoringService.kt`)
- Input: weather data + color timeline
- Output: `forecast_quality` — score, level, label, full breakdown, reasoning
- Algorithm: see FORECAST_SCORING.md, including the altitude-aware cloud geometry rating

**`ForecastRoutes`** (`server/.../routes/ForecastRoutes.kt`)
- `POST /api/forecast`
- Orchestrates calls to the above services, validates input, returns the combined JSON response

## Local Storage

**Favorites** (Room, `:core:database`): a `favorite_locations` table (id, name, latitude, longitude, createdAt), capped at 5 entries, exposed as a reactive `Flow` so the forecast screen's quick-picker and the favorites management screen stay in sync automatically.

**Settings** (Jetpack DataStore, `:core:datastore`): a `Preferences` DataStore with `units` and `theme` keys, exposed as a `Flow<UserSettings>` that `MainActivity` collects to apply the theme live, and `ForecastViewModel` collects to drive unit-aware formatting.

## Error Handling

**Android:**
- Network/API errors surface as a typed `ForecastResult.Error(code, message)` (not raw exceptions), letting the UI react to specific backend error codes
- Form validation (lat/lon range) happens client-side before a request is even sent

**Backend:**
- Invalid coordinates/date/type → 400 `INVALID_REQUEST` or 422 `DATE_OUT_OF_RANGE`
- Weather API failure → 500 `CALCULATION_ERROR` with a `request_id`
- Rate limiting → 429 `RATE_LIMITED` with `retry_after`

## Performance Considerations

1. **API Caching**: backend caches raw weather data for 30 minutes per `(lat, lon, date)` — shared across sunrise/sunset/both requests for the same location and date
2. **Network**: no CORS concerns (native client, not a browser); HTTPS expected in production
3. **Battery**: no background location polling — the app takes explicit lat/lon input, not continuous GPS
4. **Storage**: favorites and settings stored locally, no sync required

## Security

- No third-party API keys anywhere in the stack (Open-Meteo and osmdroid are both keyless)
- No user authentication for V1
- Rate limiting on the backend (100 req/min/IP)
- Input validation on all API parameters

## Deployment Architecture

**Android app:**
- Built via Gradle (`android/`), signed and distributed through Google Play
- No API keys/secrets to configure at build time

**Backend:**
- Plain JVM application (`server/`), runs via `./gradlew run` or a packaged jar
- Containerizable (Docker) for deployment to any host
- No environment variables required beyond an optional `PORT`
- Can scale horizontally if traffic increases (stateless aside from the in-memory weather cache)
