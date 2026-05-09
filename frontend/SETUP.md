# Sunset Forecast Frontend - Setup & Deployment

## Local Development

### Prerequisites
- Node.js 16+
- Expo CLI: `npm install -g expo-cli`
- EAS CLI: `npm install -g eas-cli`
- Android emulator or physical device

### Installation
```bash
cd frontend
npm install
```

### Environment Setup
```bash
cp .env.example .env
# Edit .env with your API URL (default is localhost:3000)
```

### Running Locally
```bash
# Start Expo dev server
npm start

# Press 'a' for Android emulator
# Press 'i' for iOS simulator
# Scan QR code with Expo Go app on physical device
```

## Android Test Build

### One-Time Setup
1. Install EAS CLI:
   ```bash
   npm install -g eas-cli
   ```

2. Create Expo account at https://expo.dev (if you don't have one)

3. Login to EAS:
   ```bash
   eas login
   ```

### Building APK for Testing
```bash
cd frontend

# Build APK (internal preview)
eas build --platform android --profile preview

# This generates an APK you can download and install directly on your device
```

The build will take 5-10 minutes. Once complete:
1. Download the APK from the EAS dashboard
2. Transfer to your Android device
3. Open file manager and tap the APK to install
4. Grant permissions when prompted

### Key Features to Test
- [ ] Search for a location (try: "Paris", "Tokyo", "New York")
- [ ] Pick a date (ensure 7-day limit works)
- [ ] GPS auto-detect (grant location permission)
- [ ] View forecast with map and sun positions
- [ ] Save a location to favorites
- [ ] View saved locations
- [ ] Change settings (time format, temperature unit)
- [ ] Test error cases (invalid dates, network errors)

## Configuration Files

- **app.json** — App metadata, permissions, build config
- **eas.json** — EAS build profiles
- **.env** — Environment variables (API URL)

## Project Structure
```
frontend/
├── src/
│   ├── screens/        # Home, Forecast, Saved, Settings
│   ├── components/     # LocationSearch, DatePicker, MapComponent
│   └── utils/          # API, storage, formatting helpers
├── assets/             # Icons, images
├── App.js              # Main app with navigation
└── app.json            # Expo configuration
```

## Troubleshooting

### API Connection Issues
- Ensure backend is running: `cd ../backend && npm start`
- Check API URL in `.env` matches your server
- On physical device, use your computer's actual IP (not localhost)

### Map Not Showing
- Ensure expo-location permissions are granted
- Android requires Google Play Services
- On emulator, check Google Play is installed

### Build Fails
- Run `expo prebuild --clean` to regenerate native code
- Clear cache: `npm install && rm -rf node_modules/.cache`

## Deployment to App Store

See Phase 5 documentation for TestFlight and Google Play Store submission.
