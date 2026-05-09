# Sunrise/Sunset Photographer Forecast App

A mobile-first application helping photographers plan spectacular sunrise and sunset shots by combining precise astronomy calculations, weather forecasts, and a quality scoring algorithm.

## Vision

Photographers need to know not just *when* sunrise/sunset occurs, but:
- **Where** the sun will rise/set relative to foreground objects
- **What colors** will be visible (based on sun angle and atmospheric conditions)
- **How favorable** the conditions are (cloud cover, visibility, atmospheric clarity)

This app provides 7-day forecasts with detailed sun position data (angle, azimuth) combined with weather data to help photographers plan the perfect shot.

## Project Status

**Current Phase:** V1.0 Architecture & Specification

## V1.0 Feature Set

### Core Features
- **Location Search** — Search for locations by name or auto-detect via GPS
- **Date Selection** — View forecasts for today through 7 days ahead
- **Forecast Accuracy Disclaimer** — Warn users that forecasts >48 hours have lower accuracy
- **Map View** — Display sunrise/sunset positions on a map with azimuth overlay
- **Detailed Astronomy Data**:
  - Sunrise/sunset times
  - Sun altitude angles (-18°, -6°, 0°, peak)
  - Azimuth (compass direction)
  - Civil/nautical/astronomical twilight times
- **Weather Integration**:
  - Cloud cover percentage
  - Visibility
  - Atmospheric conditions
- **Forecast Quality Score** — "Good," "Fair," "Poor" rating based on combined data
- **Favorite Locations** — Save 3-5 locations locally (no sign-in required)
- **Settings** — Units (metric/imperial), timezone handling

### Out of Scope for V1
- User accounts/cloud sync
- AR overlays (future v2+)
- Camera integration
- Social sharing
- Push notifications
- Historical data analysis

## Tech Stack

### Frontend
- **React Native** with **Expo** (JavaScript)
- **@react-native-maps/maps** for map display
- **AsyncStorage** for local persistence (favorite locations)
- **EAS Build** for cloud builds (iOS/Android)
- **Expo Go** for iOS development testing

### Backend
- **Node.js** or **Python** (minimal, stateless API)
- Astronomy library: **pymeeus** (Python) or **astronomy-engine** (Node.js)
- Weather API: TBD (OpenWeatherMap, WeatherAPI, NOAA)
- Deployment: Vercel, Railway, or Heroku

## Getting Started

### Prerequisites
- Node.js 16+ or Python 3.8+
- Expo CLI: `npm install -g expo-cli`
- EAS CLI: `npm install -g eas-cli`
- iOS device with Expo Go app (for iOS testing)
- Android emulator or device for Android testing

### Frontend Setup
```bash
cd frontend
npm install
npx expo start
```

### Backend Setup
```bash
cd backend
npm install  # or pip install -r requirements.txt
cp .env.example .env
# Add your weather API key to .env
npm start
```

## Key Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — System design, data flow, component relationships
- **[API_SPEC.md](API_SPEC.md)** — Backend endpoint specifications and payloads
- **[FORECAST_SCORING.md](FORECAST_SCORING.md)** — Algorithm for quality scoring
- **[DEVELOPMENT_ROADMAP.md](DEVELOPMENT_ROADMAP.md)** — Phased breakdown of tasks
- **[GETTING_STARTED.md](GETTING_STARTED.md)** — Quick start guide for beginning development

## Development Phases

**Phase 1:** Backend foundation (astronomy, weather, scoring)  
**Phase 2:** Frontend setup (home screen, location search)  
**Phase 3:** Forecast display (map, details)  
**Phase 4:** Local storage (favorites, settings)  
**Phase 5:** Testing & deployment  

Est. timeline: ~9 weeks part-time

## Notes for Development

- All times should be timezone-aware
- Astronomy calculations must be accurate to ±1 minute for sun times
- Weather forecasts degrade significantly after 48 hours (clearly communicate this)
- Map should show both sunrise and sunset positions simultaneously
- Consider battery usage when using GPS and repeated API calls

## License

MIT License - see [LICENSE](LICENSE) for details.
