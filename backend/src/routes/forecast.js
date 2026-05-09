const express = require('express');
const { validateForecastRequest } = require('../middleware/validate');
const { getSunTimes } = require('../services/astronomy');
const { getWeather } = require('../services/weather');
const { calcScore } = require('../services/scoring');

const router = express.Router();

function formatCoords(lat, lon) {
  const latDir = lat >= 0 ? 'N' : 'S';
  const lonDir = lon >= 0 ? 'E' : 'W';
  return `${Math.abs(lat).toFixed(2)}°${latDir}, ${Math.abs(lon).toFixed(2)}°${lonDir}`;
}

function isDateInPast48Hours(dateStr) {
  const requestDate = new Date(dateStr + 'T12:00:00Z');
  const now = new Date();
  const diffHours = (requestDate - now) / (1000 * 60 * 60);
  return diffHours > 48;
}

router.post('/forecast', async (req, res) => {
  const validation = validateForecastRequest(req.body);
  if (!validation.valid) {
    const statusCode = validation.code === 'DATE_OUT_OF_RANGE' ? 422 : 400;
    return res.status(statusCode).json({
      status: 'error',
      code: validation.code,
      message: validation.message,
      details: validation.details
    });
  }

  const { latitude, longitude, date } = req.body;

  try {
    const [astronomyData, weatherData] = await Promise.all([
      getSunTimes(latitude, longitude, date),
      getWeather(latitude, longitude, date)
    ]);

    const qualityScore = calcScore(weatherData, astronomyData);
    const accuracyWarning = isDateInPast48Hours(date);

    const response = {
      status: 'success',
      location: {
        latitude,
        longitude,
        name: formatCoords(latitude, longitude)
      },
      date,
      timezone: astronomyData.timezone,
      accuracy_warning: accuracyWarning,
      sunrise: astronomyData.sunrise,
      sunset: astronomyData.sunset,
      weather: {
        cloud_cover_percent: weatherData.cloud_cover_percent,
        visibility_km: weatherData.visibility_km,
        conditions: weatherData.conditions,
        temperature_c: weatherData.temperature_c,
        humidity_percent: weatherData.humidity_percent,
        wind_speed_ms: weatherData.wind_speed_ms
      },
      forecast_quality: qualityScore,
      timestamp: new Date().toISOString()
    };

    res.json(response);
  } catch (error) {
    console.error('Forecast calculation error:', error);
    res.status(500).json({
      status: 'error',
      code: 'CALCULATION_ERROR',
      message: 'Failed to calculate forecast: ' + error.message
    });
  }
});

module.exports = router;
