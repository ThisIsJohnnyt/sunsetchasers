const axios = require('axios');
const { getWeather, WEATHER_CACHE, getCacheKey } = require('../src/services/weather');

jest.mock('axios');

describe('Weather Service', () => {
  beforeEach(() => {
    WEATHER_CACHE.clear();
    jest.clearAllMocks();
    process.env.WEATHER_API_KEY = 'test-api-key-123';
  });

  const mockForecastResponse = {
    data: {
      list: [
        {
          dt_txt: '2025-06-21 09:00:00',
          clouds: { all: 20 },
          visibility: 12000,
          weather: [{ main: 'Clear' }],
          main: { temp: 22, humidity: 65 },
          wind: { speed: 5 }
        },
        {
          dt_txt: '2025-06-21 12:00:00',
          clouds: { all: 15 },
          visibility: 13000,
          weather: [{ main: 'Clear' }],
          main: { temp: 25, humidity: 60 },
          wind: { speed: 4 }
        },
        {
          dt_txt: '2025-06-21 15:00:00',
          clouds: { all: 10 },
          visibility: 14000,
          weather: [{ main: 'Clear' }],
          main: { temp: 26, humidity: 55 },
          wind: { speed: 3 }
        }
      ]
    }
  };

  test('Successfully fetches and extracts weather data', async () => {
    axios.get.mockResolvedValue(mockForecastResponse);

    const result = await getWeather(40.7128, -74.006, '2025-06-21');

    expect(result).toHaveProperty('cloud_cover_percent');
    expect(result).toHaveProperty('visibility_km');
    expect(result).toHaveProperty('conditions');
    expect(result).toHaveProperty('temperature_c');
    expect(result).toHaveProperty('humidity_percent');
    expect(result).toHaveProperty('wind_speed_ms');

    expect(result.cloud_cover_percent).toBeGreaterThanOrEqual(0);
    expect(result.cloud_cover_percent).toBeLessThanOrEqual(100);
    expect(result.visibility_km).toBeGreaterThan(0);
    expect(result.conditions).toMatch(/clear/i);
    expect(result.temperature_c).toBeGreaterThan(0);
  });

  test('Returns cached data without API call', async () => {
    axios.get.mockResolvedValue(mockForecastResponse);

    const result1 = await getWeather(40.7128, -74.006, '2025-06-21');
    expect(axios.get).toHaveBeenCalledTimes(1);

    const result2 = await getWeather(40.7128, -74.006, '2025-06-21');
    expect(axios.get).toHaveBeenCalledTimes(1);

    expect(result2.cloud_cover_percent).toBe(result1.cloud_cover_percent);
    expect(result2.visibility_km).toBe(result1.visibility_km);
    expect(result2.conditions).toBe(result1.conditions);
  });

  test('Cache expires after TTL', async () => {
    axios.get.mockResolvedValue(mockForecastResponse);

    await getWeather(40.7128, -74.006, '2025-06-21');
    expect(axios.get).toHaveBeenCalledTimes(1);

    const cacheKey = getCacheKey(40.7128, -74.006, '2025-06-21');
    const cached = WEATHER_CACHE.get(cacheKey);
    cached.timestamp = Date.now() - 31 * 60 * 1000;

    const modifiedResponse = {
      data: {
        list: [
          {
            dt_txt: '2025-06-21 12:00:00',
            clouds: { all: 50 },
            visibility: 8000,
            weather: [{ main: 'Clouds' }],
            main: { temp: 20, humidity: 70 },
            wind: { speed: 6 }
          }
        ]
      }
    };
    axios.get.mockResolvedValue(modifiedResponse);

    const result = await getWeather(40.7128, -74.006, '2025-06-21');
    expect(axios.get).toHaveBeenCalledTimes(2);

    expect(result.cloud_cover_percent).toBe(50);
    expect(result.visibility_km).toBe(8);
  });

  test('Handles missing visibility field (defaults to 10km)', async () => {
    const responseWithoutVisibility = {
      data: {
        list: [
          {
            dt_txt: '2025-06-21 12:00:00',
            clouds: { all: 25 },
            weather: [{ main: 'Clear' }],
            main: { temp: 23, humidity: 65 },
            wind: { speed: 4 }
          }
        ]
      }
    };

    axios.get.mockResolvedValue(responseWithoutVisibility);

    const result = await getWeather(40.7128, -74.006, '2025-06-21');
    expect(result.visibility_km).toBe(10);
  });

  test('Throws error when API key is missing', async () => {
    const originalApiKey = process.env.WEATHER_API_KEY;
    delete process.env.WEATHER_API_KEY;

    await expect(getWeather(40.7128, -74.006, '2025-06-21')).rejects.toThrow('WEATHER_API_KEY not configured');

    process.env.WEATHER_API_KEY = originalApiKey;
  });

  test('Handles API errors gracefully', async () => {
    axios.get.mockRejectedValue(new Error('Network error'));

    await expect(getWeather(40.7128, -74.006, '2025-06-21')).rejects.toThrow('Weather API error');
  });

  test('Picks closest interval to solar noon', async () => {
    axios.get.mockResolvedValue(mockForecastResponse);

    const result = await getWeather(40.7128, -74.006, '2025-06-21');

    expect(result.temperature_c).toBeGreaterThan(20);
    expect(result.temperature_c).toBeLessThan(26);
    expect(result).toHaveProperty('timestamp');
  });
});
