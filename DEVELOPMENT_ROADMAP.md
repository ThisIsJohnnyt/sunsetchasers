# Development Roadmap

Phased breakdown of V1.0 development into logical sprints. Each sprint is sized for 1-2 weeks of part-time development.

## Phase 1: Backend Foundation (2 weeks)

### Goals
- Astronomy calculations working and tested
- Weather API integration complete
- Forecast scoring algorithm implemented
- API endpoint fully functional

### Tasks

**Sprint 1.1: Project Setup & Astronomy**
- [ ] Initialize Node.js or Python backend project
- [ ] Set up project structure (src/, tests/, config/)
- [ ] Install astronomy library (`astronomy-engine` or `pymeeus`)
- [ ] Implement `calculateSunrise()` function
  - Input: lat, lon, date
  - Output: sunrise time, azimuth, altitude angles
  - Test: Known locations (e.g., Stonehenge on solstice)
- [ ] Implement `calculateSunset()` function
- [ ] Implement timezone lookup and time conversion
- [ ] Write unit tests for ±1 minute accuracy
- [ ] Document astronomy calculations

**Sprint 1.2: Weather Integration & Scoring**
- [ ] Choose weather API (OpenWeatherMap, WeatherAPI, etc.)
- [ ] Set up API key management (.env file)
- [ ] Implement `fetchWeatherForecast()` function
  - Query 7-day forecast for location
  - Extract cloud cover, visibility, conditions
- [ ] Implement caching layer (30-min TTL)
- [ ] Implement forecast scoring algorithm (all functions)
- [ ] Implement reverse geocoding (lat/lon → city name)
- [ ] Write unit tests for scoring (see FORECAST_SCORING.md test cases)

**Sprint 1.3: API & Testing**
- [ ] Implement `/api/forecast` endpoint (POST)
- [ ] Input validation (lat range, lon range, date range)
- [ ] Error handling (400, 422, 500 responses)
- [ ] Rate limiting setup
- [ ] Integration tests (full request → response cycle)
- [ ] API documentation (in code comments)
- [ ] Manual API testing with curl/Postman
- [ ] Deploy to staging (Vercel, Railway, or local)

**Deliverables:**
- Working backend API at `http://backend-url/api/forecast`
- Passes all test cases
- Example responses documented

---

## Phase 2: Frontend Setup & Core Screens (2 weeks)

### Goals
- React Native/Expo project initialized
- Basic navigation structure
- Home, Forecast, and Settings screens scaffolded
- API integration working

### Tasks

**Sprint 2.1: Project Setup & Navigation**
- [ ] Initialize Expo project: `expo init sunrise-forecast`
- [ ] Install dependencies:
  - `@react-native-maps/maps`
  - `@react-native-community/hooks`
  - `asyncstorage`
  - axios (for API calls)
- [ ] Set up project structure (screens/, components/, utils/)
- [ ] Implement navigation (React Navigation)
  - Tab navigator: Home, Forecast, Saved, Settings
- [ ] Create dummy screens for each tab
- [ ] Test on Android emulator and iOS (Expo Go)

**Sprint 2.2: Home Screen & Location Search**
- [ ] Design Home screen layout (Figma or paper sketch)
- [ ] Implement LocationSearch component
  - Text input with autocomplete
  - Geocoding (convert text → lat/lon)
- [ ] Implement GPS auto-detect button
  - Request permission, get user location
  - Fallback to manual search
- [ ] Implement DatePicker component
  - Calendar view or date selector
  - Disable dates >7 days ahead
- [ ] Implement "Search" button
  - Call backend `/api/forecast` endpoint
  - Handle loading/error states
  - Navigate to ForecastScreen with data

**Sprint 2.3: API Client & Utilities**
- [ ] Create `utils/api.js` module
  - `fetchForecast(lat, lon, date)` function
  - Error handling, timeout, retry logic
- [ ] Create `utils/storage.js` module
  - Save/load favorite locations
  - Save user settings
- [ ] Create `utils/formatting.js` module
  - Time formatting (12/24 hour)
  - Temperature unit conversion
  - Compass directions (azimuth → "NE", etc.)
- [ ] Set backend URL as environment variable (development vs. production)

**Deliverables:**
- Working home screen with location search
- Can search location, pick date, call backend
- Data flows to next screen
- Stored in AsyncStorage

---

## Phase 3: Forecast Display & Map (2 weeks)

### Goals
- ForecastScreen fully functional
- Map showing sunrise/sunset positions
- Detailed astronomy and weather data displayed
- Quality score badge

### Tasks

**Sprint 3.1: Forecast Screen Layout**
- [ ] Design ForecastScreen mockup
- [ ] Implement screen header (location, date, quality badge)
- [ ] Implement tab view: Sunrise | Sunset
- [ ] Implement map container
- [ ] Implement detail panels below map

**Sprint 3.2: Map Integration**
- [ ] Add MapView component from `@react-native-maps/maps`
- [ ] Center map on forecast location
- [ ] Add marker for location
- [ ] Implement sun position overlay
  - Draw azimuth line (sunrise direction)
  - Draw azimuth line (sunset direction)
  - Use actual azimuth from API response
- [ ] Add compass overlay (N, E, S, W)
- [ ] Test on both Android and iOS

**Sprint 3.3: Forecast Details & Quality Badge**
- [ ] Implement ForecastDetails component
  - Display sunrise/sunset times
  - Display altitude angles (0°, -6°, -18°)
  - Display azimuths with compass labels ("NE", "SW", etc.)
  - Display twilight times
- [ ] Implement QualityScore component
  - Show score badge (Excellent/Good/Fair/Poor)
  - Color-code badge (orange/yellow/gray)
  - Show breakdown (cloud, visibility, conditions, color)
- [ ] Implement weather widget
  - Show cloud %, visibility, conditions, temp
- [ ] Add accuracy warning if date >48 hours
- [ ] Format all times in user's timezone
- [ ] Test with multiple locations/dates

**Deliverables:**
- Complete forecast display
- Map showing sun positions
- All data readable and formatted correctly
- Works on Android and iOS

---

## Phase 4: Local Storage & Saved Locations (1 week)

### Goals
- Users can save favorite locations
- Quick access to past forecasts
- Settings persistence

### Tasks

**Sprint 4.1: SavedLocationsScreen**
- [ ] Implement list view of saved locations
- [ ] Load from AsyncStorage on app open
- [ ] Add location from HomeScreen (button: "Save this location")
- [ ] Delete location (swipe-to-delete or delete button)
- [ ] Quick-access: Tap location → shows today's forecast
- [ ] Limit to 5 saved locations (V1 constraint)

**Sprint 4.2: Settings Screen**
- [ ] Time format toggle (12h / 24h)
- [ ] Temperature unit toggle (°C / °F)
- [ ] Distance unit toggle (km / miles)
- [ ] Timezone override (optional, default to device timezone)
- [ ] Save all settings to AsyncStorage
- [ ] Apply settings throughout app (times, temps, distances)

**Sprint 4.3: Persistence & Initialization**
- [ ] Load saved locations on app startup
- [ ] Load settings on app startup
- [ ] Handle empty state (no saved locations)
- [ ] Handle corrupted data (fallback to defaults)

**Deliverables:**
- Users can save and recall favorite locations
- Settings persist across app closes
- App respects user preferences throughout

---

## Phase 5: Polish, Testing & Deployment (2 weeks)

### Goals
- App is polished and user-friendly
- Comprehensive testing on devices
- Ready for App Store and Play Store

### Tasks

**Sprint 5.1: UI/UX Polish**
- [ ] Review all screens for visual consistency
- [ ] Ensure readable font sizes (accessibility)
- [ ] Add app icons (256x256, 1024x1024)
- [ ] Add splash screen
- [ ] Test dark mode compatibility
- [ ] Optimize images (reduce bundle size)
- [ ] Add loading indicators (spinners)
- [ ] Add error screens with helpful messages
- [ ] Test with photographer friends (feedback round)

**Sprint 5.2: Functionality Testing**
- [ ] Test location search (various formats, edge cases)
- [ ] Test date picker (boundary dates, formatting)
- [ ] Test forecast API with various locations
- [ ] Test network errors and timeouts
- [ ] Test GPS permission flows
- [ ] Test saving/loading locations
- [ ] Test settings persistence
- [ ] Test timezone handling (across timezones)
- [ ] Test accuracy warning display

**Sprint 5.3: Device Testing**
- [ ] Android testing (emulator and real device)
  - API 21+ compatibility
  - Sideload APK for beta testers
- [ ] iOS testing (Expo Go on friends' devices)
  - iOS 13+ compatibility
  - TestFlight beta build via EAS
- [ ] Test on various phone sizes (small/medium/large)
- [ ] Battery/performance testing (check for leaks)
- [ ] Network testing (on cellular, WiFi, offline)

**Sprint 5.4: Deployment Preparation**
- [ ] Create app store listings (screenshots, description)
- [ ] Set up EAS build profiles (ios and android)
- [ ] Create iOS provisioning profiles
- [ ] Submit to TestFlight for internal testing
- [ ] Submit to Google Play Console (internal testing track)
- [ ] Set up analytics (Sentry for crash reporting, optional)
- [ ] Write release notes

**Sprint 5.5: App Store Submission**
- [ ] Final review and approval from friends
- [ ] iOS App Store submission
- [ ] Google Play Store submission
- [ ] Monitor for approval (1-5 days typical)
- [ ] Announce launch!

**Deliverables:**
- Apps available on App Store and Play Store
- Clean, professional UI
- No critical bugs
- Ready for public use

---

## Timeline Summary

| Phase | Duration | Sprint | Focus |
|-------|----------|--------|-------|
| **1** | 2 weeks | 1.1-1.3 | Backend API |
| **2** | 2 weeks | 2.1-2.3 | Frontend setup, Home screen |
| **3** | 2 weeks | 3.1-3.3 | Forecast display, Map |
| **4** | 1 week | 4.1-4.3 | Local storage, Settings |
| **5** | 2 weeks | 5.1-5.5 | Testing, Polish, Deploy |
| | | | |
| **Total** | **~9 weeks** | | MVP Ready |

*Note: Timeline assumes part-time development (10-15 hrs/week). Full-time development would compress to 4-5 weeks.*

---

## Milestones & Gates

**Milestone 1: Backend Functional (end of Phase 1)**
- [ ] API endpoint responds correctly
- [ ] Passes all test cases
- [ ] Astronomy accuracy verified (±1 min)

**Milestone 2: Frontend Core Complete (end of Phase 2)**
- [ ] Home screen fully functional
- [ ] Can call API and receive data
- [ ] No crashes or major bugs

**Milestone 3: Full Feature Set (end of Phase 3)**
- [ ] All screens functional
- [ ] Map rendering correctly
- [ ] Data formatting complete

**Milestone 4: Ready for Beta (end of Phase 4)**
- [ ] Local storage working
- [ ] Settings applied throughout app
- [ ] Ready to give to photographer friends

**Milestone 5: Ready for Launch (end of Phase 5)**
- [ ] All testing complete
- [ ] App Store/Play Store approval
- [ ] Ready for public launch

---

## Risk Management

**Risk: Weather API Rate Limiting**
- *Mitigation:* Implement caching (30 min TTL), use free tier that meets needs, switch APIs if needed

**Risk: Astronomy Calculation Accuracy Issues**
- *Mitigation:* Verify against known data, use established library (pymeeus/astronomy-engine), test edge cases (poles, equinox)

**Risk: iOS Testing Constraints**
- *Mitigation:* Use Expo Go during development, get Mac-owning friends to test early

**Risk: User Location Privacy**
- *Mitigation:* Only request GPS when user explicitly initiates location search, don't track in background

**Risk: Scope Creep**
- *Mitigation:* Strictly enforce V1 feature list, move nice-to-haves to V2

---

## Success Criteria for V1.0 Launch

✓ Backend API fully functional and tested  
✓ Frontend app runs on iOS 13+ and Android 21+  
✓ Users can search locations and get 7-day forecasts  
✓ Map displays sun positions correctly  
✓ Forecast quality scoring matches expectations  
✓ Local storage for favorites works reliably  
✓ No critical bugs on real devices  
✓ App approved on both app stores  

---

## Next Steps

1. **Choose backend language:** Node.js (faster setup) or Python (more familiar)?
2. **Choose weather API:** OpenWeatherMap, WeatherAPI, NOAA, or other?
3. **Create backend repository** and start Phase 1, Sprint 1.1
4. **Set up Expo project** for frontend (can start in parallel)
5. **Schedule testing cadence** with photographer friends

Ready to start implementing?
