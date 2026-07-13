# Development Roadmap

Status of V1.0 development: a Kotlin/Ktor backend and a Kotlin/Jetpack Compose Android app.

## Phase 1: Backend Foundation — ✅ Done

### Goals
- Astronomy calculations working and tested
- Weather API integration complete, with altitude-aware cloud data
- Forecast scoring algorithm implemented
- API endpoint fully functional

### Completed

**Sprint 1.1: Project Setup & Astronomy**
- [x] Kotlin/Ktor backend project (`server/`), Netty engine
- [x] `AstronomyService` using `commons-suncalc` + `timeshape` (offline timezone lookup)
- [x] Sunrise/sunset time, azimuth, altitude angles, civil/nautical/astronomical twilight
- [x] Unit tests verifying ±1 minute / ±0.1° / ±1° accuracy

**Sprint 1.2: Weather Integration & Scoring**
- [x] Weather provider: **Open-Meteo** (free, keyless, only option exposing cloud cover by altitude — see FORECAST_SCORING.md and API_SPEC.md for the rationale)
- [x] `WeatherService` — 30-minute cache per `(lat, lon, date)`, shared across sunrise/sunset requests; weather is sampled at the actual computed sunrise/sunset instant (fixed a prior solar-noon sampling bug)
- [x] Altitude-aware "cloud geometry" scoring in `ScoringService` (high/mid band color-contribution scores, low-cloud horizon-block gate)
- [x] Full unit test coverage for both services

**Sprint 1.3: API & Testing**
- [x] `POST /api/forecast` endpoint, input validation (lat/lon range, 7-day window)
- [x] Error handling (400 `INVALID_REQUEST`, 422 `DATE_OUT_OF_RANGE`, 429 `RATE_LIMITED`, 500 `CALCULATION_ERROR`)
- [x] Rate limiting (Ktor `RateLimit` plugin)
- [x] Route-level integration tests (Ktor test host)
- [x] Manually verified with curl against live Open-Meteo data

**Not done / deferred:**
- [ ] Reverse geocoding (`location.name` currently falls back to `"Lat X, Lon Y"` — no free keyless geocoder has been picked yet)
- [ ] Per-event weather sampling (currently `type=both` shares one sample taken at sunset time)

---

## Phase 2: Android App Skeleton — ✅ Done

### Goals
- Multi-module Gradle project
- Navigation between forecast/favorites/settings
- API integration working

### Completed
- [x] Multi-module structure: `:app`, `:core:model`, `:core:network`, `:core:database`, `:core:datastore`, `:core:designsystem`, `:feature:forecast`, `:feature:favorites`, `:feature:settings`
- [x] Hilt DI wired across modules
- [x] `:core:network`'s Ktor client + DTOs + mapper calling the real backend
- [x] Compose navigation (`NavHost` in `:app`) between the three feature screens
- [x] Verified against a live backend on an emulator (not just compiled)

---

## Phase 3: Forecast Display & Map — ✅ Done

### Goals
- Forecast screen fully functional
- Map showing sunrise/sunset positions
- Detailed astronomy and weather data displayed
- Quality score badge

### Completed
- [x] `:feature:forecast` screen: location/date/type form, result cards
- [x] Map via **osmdroid** (OpenStreetMap tiles, free, no API key) with sunrise/sunset azimuth line overlays
- [x] Sunrise/sunset times, altitude angles, azimuths, twilight times displayed
- [x] Quality score badge (Excellent/Good/Fair/Poor) with breakdown, including the new cloud-by-altitude sub-scores
- [x] Weather summary card, including "Clouds by altitude — Low/Mid/High %"
- [x] Accuracy warning shown when the forecast date is >48 hours out
- [x] Verified live on an emulator against real backend data, in both metric and imperial units

---

## Phase 4: Local Storage & Saved Locations — ✅ Done

### Goals
- Users can save favorite locations
- Quick access to past forecasts
- Settings persistence

### Completed
- [x] `:feature:favorites` screen: list + delete, backed by Room (`:core:database`)
- [x] Favorites capped at 5, exposed as a reactive `Flow`
- [x] Quick-picker on the forecast screen for saved locations
- [x] `:feature:settings` screen: units (metric/imperial) and theme (system/light/dark), persisted via Jetpack DataStore (`:core:datastore`)
- [x] Settings applied live throughout the app (unit-aware formatting, reactive theme in `MainActivity`)

---

## Phase 5: Real-Device Testing, Polish & Play Store Release — 🚧 In Progress

### Goals
- App is polished and reliable
- Tested beyond the emulator
- Ready for a Google Play release

### Remaining Tasks

**Device testing**
- [ ] Test on physical Android devices (not just emulator) across a few screen sizes/API levels
- [ ] Battery/performance check on-device
- [ ] Network testing (cellular, spotty wifi, fully offline)

**UI/UX polish**
- [ ] Visual consistency pass across all three feature screens
- [ ] App icon and splash screen
- [ ] Loading and error states reviewed for clarity
- [ ] Accessibility check (font scaling, contrast)
- [ ] Feedback round with photographer testers

**Release prep**
- [ ] Play Store listing (screenshots, description)
- [ ] Signing config and release build
- [ ] Internal testing track on Google Play Console
- [ ] Release notes

**Stretch goals (not required for V1 launch)**
- [ ] Location search/geocoding (replace raw lat/lon entry — needs a free/keyless geocoder, not yet selected)
- [ ] Per-event weather sampling for `type=both` (see Phase 1 deferred item)

---

## Timeline Summary

| Phase | Status | Focus |
|-------|--------|-------|
| **1** | ✅ Done | Backend API (astronomy, Open-Meteo weather, cloud-geometry scoring) |
| **2** | ✅ Done | Android multi-module skeleton, navigation, live API integration |
| **3** | ✅ Done | Forecast screen, osmdroid map, quality badge |
| **4** | ✅ Done | Favorites (Room), Settings (DataStore) |
| **5** | 🚧 In Progress | Real-device testing, polish, Play Store release |

---

## Risk Management

**Risk: Weather API accuracy beyond 48 hours**
- *Mitigation:* `accuracy_warning: true` surfaced in the API and shown in the UI; scoring doesn't hide the uncertainty

**Risk: No geocoding yet**
- *Mitigation:* lat/lon entry works today; `location.name` gracefully falls back to coordinates rather than failing

**Risk: Real-device quirks not caught by the emulator**
- *Mitigation:* Phase 5 explicitly budgets time for physical-device testing before release

**Risk: Scope creep**
- *Mitigation:* location search, iOS, and per-event weather sampling are explicitly out of scope for V1 (see README.md)

---

## Next Steps

1. Get the app onto a physical device and run through the full golden path (forecast → save favorite → change settings → revisit favorite)
2. Start the Play Store release checklist (signing, listing, internal testing track)
3. Revisit location search/geocoding as a V1.1 candidate once a free/keyless provider is identified
