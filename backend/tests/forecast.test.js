const request = require('supertest');
const express = require('express');
const forecastRouter = require('../src/routes/forecast');
const axios = require('axios');
const { WEATHER_CACHE } = require('../src/services/weather');

jest.mock('axios');

const app = express();
app.use(express.json());
app.use('/api', forecastRouter);

describe('Forecast API Integration', () => {
  let testDate;

  beforeEach(() => {
    jest.clearAllMocks();
    WEATHER_CACHE.clear();
    process.env.WEATHER_API_KEY = 'test-key';

    testDate = new Date();
    testDate.setDate(testDate.getDate() + 1);
    testDate = testDate.toISOString().split('T')[0];
  });

  const mockAstronomyResponse = {
    timezone: 'America/New_York',
    sunrise: {
      time: '05:26',
      azimuth: 57.7,
      altitudes: { at_horizon: 0.0, at_neg6: -6.0, at_neg18: -18.0 },
      twilight_civil_start: '04:58',
      twilight_nautical_start: '04:22',
      twilight_astronomical_start: '03:37'
    },
    sunset: {
      time: '20:31',
      azimuth: 302.7,
      altitudes: { at_horizon: 0.0, at_neg6: -6.0, at_neg18: -18.0 },
      twilight_civil_end: '20:59',
      twilight_nautical_end: '21:35',
      twilight_astronomical_end: '22:20'
    }
  };

  const mockWeatherResponse = {
    cloud_cover_percent: 20,
    visibility_km: 10,
    conditions: 'clear',
    temperature_c: 22,
    humidity_percent: 60,
    wind_speed_ms: 3.5,
    timestamp: '2025-06-21 12:00:00'
  };

  test('Valid request returns 200 with full response structure', async () => {
    axios.get.mockResolvedValue({
      data: {
        list: [
          {
            dt_txt: testDate + ' 12:00:00',
            clouds: { all: 20 },
            visibility: 10000,
            weather: [{ main: 'Clear' }],
            main: { temp: 22, humidity: 60 },
            wind: { speed: 3.5 }
          }
        ]
      }
    });

    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: testDate
      });

    expect(res.status).toBe(200);
    expect(res.body.status).toBe('success');
    expect(res.body).toHaveProperty('location');
    expect(res.body).toHaveProperty('date', testDate);
    expect(res.body).toHaveProperty('timezone');
    expect(res.body).toHaveProperty('accuracy_warning');
    expect(res.body).toHaveProperty('sunrise');
    expect(res.body).toHaveProperty('sunset');
    expect(res.body).toHaveProperty('weather');
    expect(res.body).toHaveProperty('forecast_quality');
    expect(res.body).toHaveProperty('timestamp');

    expect(res.body.location).toHaveProperty('latitude', 40.7128);
    expect(res.body.location).toHaveProperty('longitude', -74.006);
    expect(res.body.location).toHaveProperty('name');
  });

  test('Missing latitude returns 400 INVALID_REQUEST', async () => {
    const res = await request(app)
      .post('/api/forecast')
      .send({
        longitude: -74.006,
        date: '2025-06-21'
      });

    expect(res.status).toBe(400);
    expect(res.body.code).toBe('INVALID_REQUEST');
    expect(res.body.message).toContain('Latitude');
  });

  test('Latitude out of range (>90) returns 400 INVALID_REQUEST', async () => {
    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 95,
        longitude: -74.006,
        date: '2025-06-21'
      });

    expect(res.status).toBe(400);
    expect(res.body.code).toBe('INVALID_REQUEST');
    expect(res.body.details.value).toBe(95);
  });

  test('Missing date returns 400 INVALID_REQUEST', async () => {
    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006
      });

    expect(res.status).toBe(400);
    expect(res.body.code).toBe('INVALID_REQUEST');
    expect(res.body.message).toContain('Date');
  });

  test('Date beyond 7 days returns 422 DATE_OUT_OF_RANGE', async () => {
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 10);
    const futureDateStr = futureDate.toISOString().split('T')[0];

    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: futureDateStr
      });

    expect(res.status).toBe(422);
    expect(res.body.code).toBe('DATE_OUT_OF_RANGE');
    expect(res.body.details.requested_date).toBe(futureDateStr);
  });

  test('Date >48 hours in future has accuracy_warning: true', async () => {
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 5);
    const futureDateStr = futureDate.toISOString().split('T')[0];

    axios.get.mockResolvedValue({
      data: {
        list: [
          {
            dt_txt: futureDateStr + ' 12:00:00',
            clouds: { all: 20 },
            visibility: 10000,
            weather: [{ main: 'Clear' }],
            main: { temp: 22, humidity: 60 },
            wind: { speed: 3.5 }
          }
        ]
      }
    });

    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: futureDateStr
      });

    expect(res.status).toBe(200);
    expect(res.body.accuracy_warning).toBe(true);
  });

  test('Invalid type value returns 400 INVALID_REQUEST', async () => {
    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: testDate,
        type: 'invalid_type'
      });

    expect(res.status).toBe(400);
    expect(res.body.code).toBe('INVALID_REQUEST');
    expect(res.body.message).toContain('Type');
  });

  test('Weather service failure returns 500 CALCULATION_ERROR', async () => {
    axios.get.mockRejectedValue(new Error('API Error'));

    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: testDate
      });

    expect(res.status).toBe(500);
    expect(res.body.code).toBe('CALCULATION_ERROR');
  });

  test('Valid type values are accepted', async () => {
    axios.get.mockResolvedValue({
      data: {
        list: [
          {
            dt_txt: testDate + ' 12:00:00',
            clouds: { all: 20 },
            visibility: 10000,
            weather: [{ main: 'Clear' }],
            main: { temp: 22, humidity: 60 },
            wind: { speed: 3.5 }
          }
        ]
      }
    });

    for (const type of ['sunrise', 'sunset', 'both']) {
      const res = await request(app)
        .post('/api/forecast')
        .send({
          latitude: 40.7128,
          longitude: -74.006,
          date: testDate,
          type
        });

      expect(res.status).toBe(200);
      expect(res.body.status).toBe('success');
    }
  });

  test('Response includes quality score with correct structure', async () => {
    axios.get.mockResolvedValue({
      data: {
        list: [
          {
            dt_txt: testDate + ' 12:00:00',
            clouds: { all: 20 },
            visibility: 10000,
            weather: [{ main: 'Clear' }],
            main: { temp: 22, humidity: 60 },
            wind: { speed: 3.5 }
          }
        ]
      }
    });

    const res = await request(app)
      .post('/api/forecast')
      .send({
        latitude: 40.7128,
        longitude: -74.006,
        date: testDate
      });

    expect(res.status).toBe(200);
    expect(res.body.forecast_quality).toHaveProperty('score');
    expect(res.body.forecast_quality).toHaveProperty('level');
    expect(res.body.forecast_quality).toHaveProperty('label');
    expect(res.body.forecast_quality).toHaveProperty('breakdown');
    expect(res.body.forecast_quality).toHaveProperty('reasoning');

    expect(res.body.forecast_quality.score).toBeGreaterThanOrEqual(0);
    expect(res.body.forecast_quality.score).toBeLessThanOrEqual(1);
    expect([1, 2, 3, 4]).toContain(res.body.forecast_quality.level);
  });
});
