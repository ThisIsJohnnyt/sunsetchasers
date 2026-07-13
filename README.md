# Sunrise/Sunset Photographer Forecast App

A mobile-first application helping photographers plan spectacular sunrise and sunset shots by combining precise astronomy calculations, weather forecasts, and a quality scoring algorithm.

## Vision

Photographers need to know not just *when* sunrise/sunset occurs, but:
- **Where** the sun will rise/set relative to foreground objects
- **What colors** will be visible (based on sun angle and atmospheric conditions)
- **How favorable** the conditions are (cloud cover, visibility, atmospheric clarity)

This app provides 7-day forecasts with detailed sun position data (angle, azimuth) combined with weather data to help photographers plan the perfect shot.

## Project Status

**Current Phase:** Android app functional end-to-end against a live backend. Core V1 feature set (forecast, map, favorites, settings) is implemented and verified; remaining work is real-device testing and a Play Store release.

## V1.0 Feature Set

### Core Features
- **Location Entry** — Enter coordinates directly (no geocoding/search API configured yet — see "Notes for Development")
- **Date Selection** — View forecasts for today through 7 days ahead
- **Forecast Accuracy Disclaimer** — Warns when forecasts are >48 hours out
- **Map View** — OpenStreetMap tiles (via osmdroid, free, no API key) with sunrise/sunset azimuth lines overlaid
- **Detailed Astronomy Data**:
  - Sunrise/sunset times
  - Sun altitude angles (-18°, -6°, 0°, peak) and a color timeline across them
  - Azimuth (compass direction)
  - Civil/nautical/astronomical twilight times
- **Weather Integration** (via [Open-Meteo](https://open-meteo.com), free and keyless):
  - Cloud cover — blended **and** by altitude (low/mid/high), which is what actually predicts sunset/sunrise color
  - Visibility, temperature, humidity, wind
- **Forecast Quality Score** — "Excellent/Good/Fair/Poor" rating, with an altitude-aware cloud-geometry component (see FORECAST_SCORING.md)
- **Favorite Locations** — Save up to 5 locations locally (Room database, no sign-in required)
- **Settings** — Units (metric/imperial) and theme (system/light/dark), persisted via Jetpack DataStore

### Out of Scope for V1
- User accounts/cloud sync
- Location search/geocoding (currently lat/lon entry only)
- AR overlays (future v2+)
- Camera integration
- Social sharing
- Push notifications
- Historical data analysis
- iOS (Android-only for now)

## Tech Stack

### Backend (`server/`)
- **Kotlin** + **Ktor** (Netty engine)
- Astronomy: [`commons-suncalc`](https://shredzone.org/maven/commons-suncalc/) + [`timeshape`](https://github.com/RomanIakovlev/timeshape) for offline timezone lookup
- Weather: **Open-Meteo** — free, no API key
- Testing: JUnit 5, Ktor test host, MockK
- Deployment: any JVM host (Docker-friendly); no external API keys required

### Android app (`android/`)
- **Kotlin** + **Jetpack Compose**, Material 3
- Multi-module: `:core:model`, `:core:network` (Ktor client), `:core:database` (Room), `:core:datastore` (Jetpack DataStore), `:core:designsystem`, `:feature:forecast`, `:feature:favorites`, `:feature:settings`, `:app`
- DI: **Hilt**
- Map: **osmdroid** (OpenStreetMap tiles, free, no API key)
- Build: Gradle (Kotlin DSL), Android Gradle Plugin, targeting minSdk 26

There's also a legacy `backend/` (Node.js) directory kept only as a historical reference from an earlier prototype — it is not used by the app; `server/` is the real backend.

## Getting Started

See **[GETTING_STARTED.md](GETTING_STARTED.md)** for full setup instructions (JDK, Android Studio/SDK, running the server, running the Android app).

Quick version — no API keys needed anywhere:

```bash
# Backend
cd server
./gradlew run          # starts on http://localhost:8080

# Android app
cd android
./gradlew assembleDebug   # or open in Android Studio and Run
```

## Key Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — System design, data flow, module responsibilities
- **[API_SPEC.md](API_SPEC.md)** — Backend endpoint specification and payloads
- **[FORECAST_SCORING.md](FORECAST_SCORING.md)** — Algorithm for quality scoring, including the cloud-geometry formula
- **[DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md)** — What's done and what's left
- **[GETTING_STARTED.md](GETTING_STARTED.md)** — Environment setup and how to run everything

## Notes for Development

- All times are timezone-aware (IANA timezone looked up offline per coordinate)
- Astronomy calculations are accurate to ±1 minute for sun times
- Weather forecasts degrade significantly after 48 hours (the API returns `accuracy_warning: true`, and the UI should surface it)
- The map shows both sunrise and sunset azimuth lines simultaneously
- No location search/geocoding is wired up yet — the UI takes raw latitude/longitude. `location.name` in API responses currently falls back to `"Lat X, Lon Y"`.

## License

MIT License - see [LICENSE](LICENSE) for details.
