# Getting Started Guide

Setup instructions for running the Sunrise/Sunset Photographer Forecast App locally: a Kotlin/Ktor backend and a Kotlin/Jetpack Compose Android app.

## What You're Building

A native Android app that helps photographers plan sunrise and sunset shots by showing:
- **Precise sun position** (azimuth, altitude angles) on a map
- **7-day weather forecast** with cloud cover (including by altitude) and visibility
- **Quality score** (Excellent/Good/Fair/Poor) based on combined data
- **Saved favorite locations** for quick access

**No user accounts** — all data (favorites, settings) stored locally on the device. **No API keys required anywhere** — both the weather provider (Open-Meteo) and the map tiles (osmdroid/OpenStreetMap) are free and keyless.

---

## Prerequisites

- **JDK 17** (for both the backend and Android builds)
- **Android Studio** (latest stable) with the Android SDK — needed to run the Android app on an emulator or device
  - minSdk 26 (Android 8.0), so any emulator image API 26+ works
- Git

That's it — no Node.js, no Python, no npm, no API key sign-ups.

---

## Documentation Review

Before diving in, it helps to read:
- **[ARCHITECTURE.md](ARCHITECTURE.md)** — system design and data flow
- **[API_SPEC.md](API_SPEC.md)** — what the backend returns and why
- **[FORECAST_SCORING.md](FORECAST_SCORING.md)** — the quality-scoring algorithm, including the cloud-geometry formula

---

## Code Organization

```
Sunset Chasers/
├── server/                              # Kotlin/Ktor backend
│   ├── src/main/kotlin/com/sunsetchasers/
│   │   ├── routes/ForecastRoutes.kt     # POST /api/forecast
│   │   ├── services/
│   │   │   ├── AstronomyService.kt      # commons-suncalc + timeshape
│   │   │   ├── WeatherService.kt        # Open-Meteo client + cache
│   │   │   └── ScoringService.kt        # Cloud geometry + quality scoring
│   │   ├── models/ForecastModels.kt     # Request/response DTOs
│   │   └── Application.kt               # Ktor server entry point
│   ├── src/test/kotlin/...              # JUnit 5 + MockK tests
│   ├── build.gradle.kts
│   └── .env.example
│
├── android/                             # Kotlin/Compose multi-module app
│   ├── app/                             # Application, MainActivity, NavHost
│   ├── core/
│   │   ├── model/                       # Shared domain types
│   │   ├── network/                     # Ktor client, DTOs, mapper
│   │   ├── database/                    # Room (favorites)
│   │   ├── datastore/                   # Jetpack DataStore (settings)
│   │   └── designsystem/                # Material 3 theme
│   ├── feature/
│   │   ├── forecast/                    # Main forecast screen + map
│   │   ├── favorites/                   # Favorites management screen
│   │   └── settings/                    # Units/theme screen
│   └── build.gradle.kts
│
├── backend/                             # Legacy Node.js prototype — reference only, not used
│
└── docs (this directory)
    ├── README.md
    ├── ARCHITECTURE.md
    ├── API_SPEC.md
    ├── FORECAST_SCORING.md
    ├── DEVELOPMENT_ROADMAP.md
    └── GETTING_STARTED.md (this file)
```

---

## Running the Backend

```bash
cd server
./gradlew run          # starts on http://localhost:8080
```

No `.env` setup is required — `server/.env.example` documents that `PORT` is the only optional variable. Open-Meteo needs no API key.

**Run the tests:**
```bash
cd server
./gradlew test
```

**Try it manually:**
```bash
curl -X POST http://localhost:8080/api/forecast \
  -H "Content-Type: application/json" \
  -d '{"latitude": 37.0, "longitude": -110.0, "date": "2026-07-15", "type": "both"}'
```

See [API_SPEC.md](API_SPEC.md) for the full response shape.

---

## Running the Android App

1. Open the `android/` directory as a project in Android Studio, or build from the command line:
   ```bash
   cd android
   ./gradlew assembleDebug
   ```
2. Start an emulator (API 26+) via Android Studio's Device Manager, or connect a physical device with USB debugging enabled.
3. Run the app (Android Studio's Run button, or `./gradlew installDebug`).

**Backend URL:** defaults to a LAN IP for testing on a physical device (see `android/core/network/build.gradle.kts`). For the emulator, override it to `10.0.2.2:8080` (the emulator's alias for host localhost) via one of the methods below. To point the app at a different backend (LAN IP, emulator alias, or a hosted URL — see "Hosting for Remote Testing" below) without editing source, set `backendBaseUrl` either:
- in `android/local.properties` (gitignored, per-machine): `backendBaseUrl=http://10.0.2.2:8080/`, or
- as a one-off Gradle property: `./gradlew assembleDebug -PbackendBaseUrl=http://10.0.2.2:8080/`

---

## Hosting for Remote Testing

If you want to test the app without your dev machine running the backend (e.g. while traveling), `server/` deploys as-is to [Render](https://render.com)'s free tier — no credit card required:

1. **Deploy**: create a free Render account, connect this GitHub repo, and create a new **Web Service** with root directory `server/`. Render auto-detects the `server/Dockerfile` and builds from it — no other configuration needed. The build produces a runnable JVM distribution (`./gradlew installDist`) in a slim JRE runtime image, with the heap tuned (`-Xmx400m`) to fit Render's free-tier 512MB RAM.
2. **Get the URL**: Render assigns a public `https://<name>.onrender.com` address. Confirm it works: `curl https://<name>.onrender.com/health` should return `{"status":"ok"}`.
3. **Point a build at it**: rebuild the Android app with `backendBaseUrl` set to that URL (via `local.properties` or `-PbackendBaseUrl=`, as above) — no `network_security_config.xml` change needed since Render serves HTTPS by default.
4. **Avoid the cold-start delay (optional)**: Render's free web services sleep after 15 minutes of inactivity and take 30-60s to wake on the next request. To keep it warm the whole time you're away, sign up for a free [UptimeRobot](https://uptimerobot.com) (or similar) account and add an HTTP(S) monitor hitting `https://<name>.onrender.com/health` every 5 minutes. This fits comfortably inside Render's 750 free hours/month (a full month is ~720 hours) for a single always-on service.

The `/health` endpoint (`server/.../routes/HealthRoutes.kt`) is intentionally outside the rate limiter so keepalive pings never compete with real forecast traffic.

---

## Testing Strategy

**Backend:**
- Unit tests per service (`AstronomyServiceTest`, `WeatherServiceTest`, `ScoringServiceTest`)
- Route-level tests using Ktor's test host (`ForecastRouteTest`)
- Manual testing with curl before wiring up the Android app

**Android:**
- Compile and run on an emulator or device against a live backend for any UI-affecting change — type checks and unit tests verify correctness of logic, not that a screen actually renders and behaves as expected
- Test both metric and imperial unit settings, light and dark theme

---

## Common Gotchas & Solutions

### Android emulator can't reach the backend
- **Problem:** requests time out or fail to connect
- **Solution:** the emulator's `localhost` is not your machine's `localhost` — use `10.0.2.2` from the emulator to reach a backend running on your host machine

### AGP / Kotlin plugin conflicts
- **Problem:** `Cannot add extension with name 'kotlin'` during a Gradle sync
- **Solution:** AGP 9.x auto-applies Kotlin Android support for Android modules — don't also explicitly apply `org.jetbrains.kotlin.android` in those modules' `build.gradle.kts` (the pure-JVM `:core:model` module is the one exception, using `kotlin.jvm` instead)

### Dependency version lookups
- **Problem:** `search.maven.org`'s search index can lag behind what's actually published
- **Solution:** check `maven-metadata.xml` directly — `https://repo1.maven.org/maven2/<group-path>/<artifact>/maven-metadata.xml` (or `dl.google.com/dl/android/maven2/...` for AndroidX/Google artifacts) — for the authoritative latest version

### Timezone bugs
- **Problem:** times showing in the wrong timezone
- **Solution:** the backend looks up IANA timezone offline via `timeshape` from the request's lat/lon and returns all times already localized — no client-side timezone math should be needed

### Weather sampled at the wrong time
- Note that `type=both` currently samples weather once, at the sunset instant (see the "Weather sampling note" in [API_SPEC.md](API_SPEC.md)) — this is a known simplification, not a bug, tracked in [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md).

---

## Daily Development Workflow

1. Pick a task from [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md)
2. Create a branch: `git checkout -b feature/task-name`
3. Make changes; run `./gradlew test` (backend) and/or `./gradlew assembleDebug` + manual verification on an emulator (Android)
4. Commit with a clear message
5. Push and open a PR (or push to main if solo)

---

## Resources

**Astronomy:** [commons-suncalc](https://shredzone.org/maven/commons-suncalc/), [timeshape](https://github.com/RomanIakovlev/timeshape)

**Weather:** [Open-Meteo API docs](https://open-meteo.com/en/docs)

**Android:** [Jetpack Compose docs](https://developer.android.com/jetpack/compose), [Ktor client docs](https://ktor.io/docs/client-create-new-application.html), [osmdroid](https://github.com/osmdroid/osmdroid), [Room](https://developer.android.com/training/data-storage/room), [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore), [Hilt](https://dagger.dev/hilt/)

**Deployment:** [Google Play Console Help](https://support.google.com/googleplay/android-developer)

---

## Questions Before Starting?

**Unclear on architecture?** → Re-read [ARCHITECTURE.md](ARCHITECTURE.md), focused on the data flow diagram.
**Unclear on the API?** → Check [API_SPEC.md](API_SPEC.md)'s example requests/responses.
**Unclear on scoring?** → Work through [FORECAST_SCORING.md](FORECAST_SCORING.md)'s worked examples.
**Unclear on what's left?** → Check [DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md).
