const axios = require('axios');

const WEATHER_CACHE = new Map();
const CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

function getCacheKey(lat, lon, dateStr) {
  const latRounded = Math.round(lat * 100) / 100;
  const lonRounded = Math.round(lon * 100) / 100;
  return `${latRounded}_${lonRounded}_${dateStr}`;
}

function getCachedWeather(cacheKey) {
  const cached = WEATHER_CACHE.get(cacheKey);
  if (!cached) return null;

  const age = Date.now() - cached.timestamp;
  if (age > CACHE_TTL_MS) {
    WEATHER_CACHE.delete(cacheKey);
    return null;
  }

  return cached.data;
}

function setCachedWeather(cacheKey, data) {
  WEATHER_CACHE.set(cacheKey, {
    data,
    timestamp: Date.now()
  });
}

function findClosestInterval(forecastList, targetDateStr) {
  if (!forecastList || forecastList.length === 0) return null;

  const targetDate = new Date(targetDateStr + 'T12:00:00Z');
  let closest = forecastList[0];
  let minDiff = Math.abs(new Date(forecastList[0].dt_txt) - targetDate);

  for (const item of forecastList) {
    const itemDate = new Date(item.dt_txt);
    const diff = Math.abs(itemDate - targetDate);
    if (diff < minDiff) {
      minDiff = diff;
      closest = item;
    }
  }

  return closest;
}

async function getWeather(lat, lon, dateStr) {
  const cacheKey = getCacheKey(lat, lon, dateStr);

  const cached = getCachedWeather(cacheKey);
  if (cached) {
    return cached;
  }

  try {
    const apiKey = process.env.WEATHER_API_KEY;
    if (!apiKey) {
      throw new Error('WEATHER_API_KEY not configured');
    }

    const url = 'https://api.openweathermap.org/data/2.5/forecast';
    const response = await axios.get(url, {
      params: {
        lat,
        lon,
        appid: apiKey,
        units: 'metric',
        cnt: 40
      },
      timeout: 10000
    });

    const forecastList = response.data.list;
    const interval = findClosestInterval(forecastList, dateStr);

    if (!interval) {
      throw new Error(`No forecast data found for ${dateStr}`);
    }

    const weatherData = {
      cloud_cover_percent: interval.clouds.all,
      visibility_km: (interval.visibility || 10000) / 1000,
      conditions: interval.weather && interval.weather.length > 0
        ? interval.weather[0].main.toLowerCase()
        : 'unknown',
      temperature_c: interval.main.temp,
      humidity_percent: interval.main.humidity,
      wind_speed_ms: interval.wind.speed,
      timestamp: interval.dt_txt
    };

    setCachedWeather(cacheKey, weatherData);
    return weatherData;
  } catch (error) {
    const errorMsg = error.response?.data?.message || error.message;
    throw new Error(`Weather API error: ${errorMsg}`);
  }
}

module.exports = {
  getWeather,
  getCachedWeather,
  setCachedWeather,
  getCacheKey,
  WEATHER_CACHE
};
