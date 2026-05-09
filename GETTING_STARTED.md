# Getting Started Guide

Quick reference for beginning V1.0 development of the Sunrise/Sunset Photographer Forecast App.

## What You're Building

A smartphone app (React Native/Expo) that helps photographers plan sunrise and sunset shots by showing:
- **Precise sun position** (azimuth, altitude angles) on a map
- **7-day weather forecast** with cloud cover and visibility
- **Quality score** (Excellent/Good/Fair/Poor) based on combined data
- **Saved favorite locations** for quick access

**No user accounts for V1** — all data stored locally on the phone.

---

## Key Decisions You Need to Make

Before starting Phase 1, decide:

### 1. Backend Language

**Option A: Node.js (Recommended for speed)**
- Faster setup (same language as frontend)
- `astronomy-engine` npm package is well-maintained
- Easy deployment (Vercel, Railway)
- Con: Less familiar if your background is Python

**Option B: Python**
- Familiar language for you
- `pymeeus` library is excellent for astronomy
- Easy deployment (Flask + Heroku/Railway)
- Con: Separate language stack from frontend

**Recommendation:** Start with **Node.js** for fastest MVP, switch to Python later if preferred.

### 2. Weather API

**Option A: OpenWeatherMap (Recommended)**
- Free tier: 60 calls/min, 1000/day
- Includes: cloud %, visibility, conditions, temperature
- Well-documented, reliable
- Cost: $0 for V1 usage

**Option B: WeatherAPI.com**
- Free tier: 1M calls/month
- Slightly better UI/docs
- Same data quality as OpenWeatherMap

**Option C: NOAA (US Only)**
- Free, no rate limits
- Most accurate for USA
- Limited international coverage

**Recommendation:** Start with **OpenWeatherMap** (most flexible, good free tier).

### 3. Map Library

**React Native Maps:**
- `@react-native-maps/maps` — pre-selected
- Supports both iOS and Android
- Can render custom overlays (azimuth lines, compass)
- Good documentation

**No alternatives needed** — this is the standard choice.

---

## Pre-Development Checklist

Before coding:

- [ ] **Accounts/Keys Created:**
  - OpenWeatherMap API key (free account)
  - EAS account (already have, confirmed)
  - GitHub account (for version control, optional but recommended)

- [ ] **Development Environment:**
  - Node.js 16+ installed (`node --version`)
  - Expo CLI installed (`npm install -g expo-cli`)
  - EAS CLI installed (`npm install -g eas-cli`)
  - Code editor (VS Code recommended)

- [ ] **Testing Devices Ready:**
  - Android: Emulator set up OR device for sideloading
  - iOS: Expo Go installed on friend's iPhone (for Phase 2+)

- [ ] **Documentation Review:**
  - Read ARCHITECTURE.md (understand data flow)
  - Read API_SPEC.md (understand what backend needs to do)
  - Read FORECAST_SCORING.md (understand scoring logic)

---

## Development Process

### Code Organization

```
sunrise-forecast-app/
├── backend/                    # Node.js API
│   ├── src/
│   │   ├── routes/forecast.js # Main /api/forecast endpoint
│   │   ├── services/
│   │   │   ├── astronomy.js   # Sun calculations
│   │   │   ├── weather.js     # Weather API calls
│   │   │   └── scoring.js     # Quality scoring
│   │   └── index.js           # Express app entry
│   ├── tests/                 # Unit tests
│   ├── package.json
│   └── .env                   # API keys (never commit!)
│
├── frontend/                   # React Native (Expo)
│   ├── app/
│   │   ├── screens/
│   │   │   ├── HomeScreen.js
│   │   │   ├── ForecastScreen.js
│   │   │   ├── SavedLocationsScreen.js
│   │   │   └── SettingsScreen.js
│   │   ├── components/
│   │   │   ├── LocationSearch.js
│   │   │   ├── DatePicker.js
│   │   │   ├── MapView.js
│   │   │   ├── ForecastDetails.js
│   │   │   └── QualityScore.js
│   │   ├── utils/
│   │   │   ├── api.js         # Backend calls
│   │   │   ├── storage.js     # AsyncStorage helpers
│   │   │   └── formatting.js  # Time/units formatting
│   │   └── App.js             # Entry point
│   ├── app.json               # Expo config
│   └── package.json
│
└── docs/                       # All markdown files
    ├── README.md
    ├── ARCHITECTURE.md
    ├── API_SPEC.md
    ├── FORECAST_SCORING.md
    ├── DEVELOPMENT_ROADMAP.md
    └── GETTING_STARTED.md (this file)
```

### Git Workflow (Recommended)

```bash
# Initial setup
git init
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/sunrise-forecast.git

# Per feature/sprint
git checkout -b feature/backend-astronomy
# ... make changes ...
git add .
git commit -m "Implement astronomy calculations with ±1 min accuracy"
git push origin feature/backend-astronomy
# Open PR, review, merge

# Always keep main deployable
```

### Testing Strategy

**Backend:**
- Unit tests for each service (astronomy, weather, scoring)
- Integration tests for /api/forecast endpoint
- Manual testing with curl/Postman before frontend integration

**Frontend:**
- Manual testing on device (start simple, iterate)
- Test with friends' iPhones early (Phase 2+)
- Automated testing optional for V1 (React Native Testing Library if motivated)

---

## Phase 1 Quickstart: Backend Setup

### Step 1: Initialize Backend Project

```bash
# Create project directory
mkdir sunrise-forecast-app
cd sunrise-forecast-app

# Initialize backend (Node.js)
mkdir backend
cd backend
npm init -y
npm install express axios dotenv

# For astronomy
npm install astronomy-engine

# For testing
npm install --save-dev jest

# Create folder structure
mkdir src tests
mkdir src/routes src/services
```

### Step 2: Create .env File

```bash
# backend/.env
WEATHER_API_KEY=your_openweathermap_key_here
PORT=3000
NODE_ENV=development
```

### Step 3: Start with Astronomy Service

Create `src/services/astronomy.js`:

```javascript
// Import astronomy-engine or implement using pymeeus
// See ARCHITECTURE.md and API_SPEC.md for implementation details

// Function: calculateSunrise(latitude, longitude, date)
// Returns: { time, azimuth, altitudes }

// Function: calculateSunset(latitude, longitude, date)
// Returns: { time, azimuth, altitudes }

// Test with known values (e.g., Stonehenge solstice)
```

### Step 4: Implement Weather Service

Create `src/services/weather.js`:

```javascript
// Calls OpenWeatherMap API
// Caches results for 30 minutes
// Extracts: cloud_cover%, visibility_km, conditions

// Function: fetchWeatherForecast(latitude, longitude)
// Returns: 7-day forecast array
```

### Step 5: Implement Scoring Service

Create `src/services/scoring.js`:

```javascript
// Implements algorithm from FORECAST_SCORING.md
// Combines astronomy + weather into quality score

// Function: calculateForecastQuality(astronomyData, weatherData)
// Returns: { score, level, reasoning }
```

### Step 6: Create API Endpoint

Create `src/routes/forecast.js`:

```javascript
// POST /api/forecast
// Input validation
// Calls astronomy + weather + scoring services
// Returns JSON response (see API_SPEC.md)
```

### Step 7: Test & Deploy

```bash
# Local testing
npm test

# Start server locally
npm start
# Accessible at http://localhost:3000

# Deploy (Vercel or Railway)
# Follow their Node.js deployment guide
```

---

## Frontend Phase 2 Quickstart

```bash
# From project root
cd ..

# Initialize Expo
npx create-expo-app frontend
cd frontend
npm install axios @react-native-maps/maps react-navigation @react-native-bottom-tabs

# Start development
npx expo start

# Scan QR code with Expo Go on iPhone (friend) or Android phone
```

**Key first goal:** Get home screen working with location search that calls your backend.

---

## Daily Development Workflow

**Each session:**

1. Pick a task from DEVELOPMENT_ROADMAP.md
2. Create a git branch: `git checkout -b feature/task-name`
3. Code and test (run on real device frequently)
4. Commit: `git commit -m "Clear message about what you did"`
5. Push and create PR (or just push to main if solo)

**Progress tracking:**

- Use the checkbox lists in DEVELOPMENT_ROADMAP.md
- Update as you complete tasks
- Screenshot progress (nice for motivation)

---

## Common Gotchas & Solutions

### Astronomy Library Accuracy
- **Problem:** Sun times off by >1 minute
- **Solution:** Verify with known data (NASA, timeanddate.com)
- **Check:** Test at multiple latitudes/longitudes, especially polar regions

### Weather API Rate Limiting
- **Problem:** Too many requests, hitting rate limit
- **Solution:** Implement 30-min cache, user can't search same location twice in quick succession
- **Fallback:** Return cached result if fresh API call fails

### iOS Testing Friction
- **Problem:** No Mac, hard to test iPhone version
- **Solution:** Use Expo Go (runs app inside Expo's container) on friend's iPhone
- **For beta:** Use EAS internal distribution → TestFlight

### Map Overlay Performance
- **Problem:** Drawing many azimuth lines causes lag
- **Solution:** Use native map polylines, not custom Canvas overlays
- **Optimize:** Test with multiple locations, profile with React DevTools

### Timezone Bugs
- **Problem:** Times showing in wrong timezone
- **Solution:** Always work with UTC internally, convert to local only for display
- **Test:** Check forecast for locations across multiple time zones

### AsyncStorage Data Loss
- **Problem:** Saved locations disappear after app close
- **Solution:** Ensure AsyncStorage is properly initialized, handle async/await correctly
- **Debug:** Log data to console, verify it persists

---

## Resources & Documentation

**Astronomy:**
- [PyMeeus Documentation](https://github.com/monadius/pymeeus) (Python)
- [Astronomy Engine](https://github.com/cosinekitty/astronomy) (JavaScript/Python)
- [Timeanddate.com](https://timeanddate.com) (verify calculations)

**React Native:**
- [React Native Docs](https://reactnative.dev)
- [Expo Docs](https://docs.expo.dev)
- [React Navigation](https://reactnavigation.org)

**Weather APIs:**
- [OpenWeatherMap API](https://openweathermap.org/api)
- [WeatherAPI](https://www.weatherapi.com)
- [NOAA Weather Data](https://www.weather.gov/documentation/services-web-api)

**Deployment:**
- [Vercel Docs](https://vercel.com/docs) (Node.js)
- [Railway Docs](https://docs.railway.app)
- [Heroku Docs](https://devcenter.heroku.com) (legacy but still works)

**App Store Submission:**
- [Apple App Store Guidelines](https://developer.apple.com/app-store/guidelines/)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)

---

## Success Metrics for V1.0

✓ Users can search any location on Earth  
✓ Forecasts show sun angles to ±1 minute and ±1° accuracy  
✓ Quality scores match actual conditions (photographer feedback)  
✓ App doesn't crash, runs smoothly on phones 5+ years old  
✓ Available on both iOS App Store and Google Play Store  

---

## Questions Before Starting?

**Unclear on architecture?** → Re-read ARCHITECTURE.md with focus on data flow diagram  
**Unclear on API?** → Check API_SPEC.md example requests/responses  
**Unclear on scoring?** → Work through FORECAST_SCORING.md test cases  
**Unclear on timeline?** → Start Phase 1, adjust as needed  

**Ready?** Start Phase 1, Sprint 1.1: Backend project initialization!

---

## Support & Iteration

This document and the roadmap are **living documents** — update them as you learn:
- If a task takes 2x longer than estimated, note it
- If a library doesn't work, document the switch
- If you discover a simpler approach, update the roadmap

Good luck! 🌅
