const { getSunTimes, getTimezone } = require('../src/services/astronomy');

function parseTime(timeStr) {
  const [hours, minutes] = timeStr.split(':').map(Number);
  return hours * 60 + minutes;
}

function minutesDiff(time1Str, time2Str) {
  return Math.abs(parseTime(time1Str) - parseTime(time2Str));
}

describe('Astronomy Service', () => {
  test('New York summer solstice (2025-06-21)', () => {
    const result = getSunTimes(40.7128, -74.0060, '2025-06-21');

    expect(result).toHaveProperty('timezone');
    expect(result).toHaveProperty('sunrise');
    expect(result).toHaveProperty('sunset');

    expect(result.sunrise).toHaveProperty('time');
    expect(result.sunrise).toHaveProperty('azimuth');
    expect(result.sunrise).toHaveProperty('altitudes');

    const sunriseTime = result.sunrise.time;
    const sunsetTime = result.sunset.time;

    console.log(`New York 2025-06-21: Sunrise ${sunriseTime}, Sunset ${sunsetTime}`);

    expect(minutesDiff(sunriseTime, '05:25')).toBeLessThanOrEqual(3);
    expect(minutesDiff(sunsetTime, '20:31')).toBeLessThanOrEqual(3);
  });

  test('London spring equinox (2025-03-20)', () => {
    const result = getSunTimes(51.5074, -0.1278, '2025-03-20');

    expect(result.timezone).toBeTruthy();
    expect(result.sunrise.time).toBeTruthy();
    expect(result.sunset.time).toBeTruthy();

    const sunriseTime = result.sunrise.time;
    const sunsetTime = result.sunset.time;

    console.log(`London 2025-03-20: Sunrise ${sunriseTime}, Sunset ${sunsetTime}`);

    expect(minutesDiff(sunriseTime, '06:02')).toBeLessThanOrEqual(3);
    expect(minutesDiff(sunsetTime, '18:15')).toBeLessThanOrEqual(3);
  });

  test('Sydney winter solstice (2025-06-21 - southern hemisphere)', () => {
    const result = getSunTimes(-33.8688, 151.2093, '2025-06-21');

    expect(result.timezone).toBeTruthy();
    expect(result.sunrise.time).toBeTruthy();
    expect(result.sunset.time).toBeTruthy();

    const sunriseTime = result.sunrise.time;
    const sunsetTime = result.sunset.time;

    console.log(`Sydney 2025-06-21: Sunrise ${sunriseTime}, Sunset ${sunsetTime}`);

    expect(minutesDiff(sunriseTime, '07:02')).toBeLessThanOrEqual(5);
    expect(minutesDiff(sunsetTime, '16:55')).toBeLessThanOrEqual(5);
  });

  test('Azimuth range validation', () => {
    const result = getSunTimes(40.7128, -74.0060, '2025-06-21');

    const sunriseAz = result.sunrise.azimuth;
    const sunsetAz = result.sunset.azimuth;

    expect(sunriseAz).toBeGreaterThanOrEqual(0);
    expect(sunriseAz).toBeLessThanOrEqual(360);
    expect(sunsetAz).toBeGreaterThanOrEqual(0);
    expect(sunsetAz).toBeLessThanOrEqual(360);

    expect(sunriseAz).toBeLessThan(180);
    expect(sunsetAz).toBeGreaterThan(180);

    console.log(`Azimuths - Sunrise: ${sunriseAz}°, Sunset: ${sunsetAz}°`);
  });

  test('Twilight times ordering', () => {
    const result = getSunTimes(40.7128, -74.0060, '2025-06-21');

    const sunriseTimes = {
      astronomical: parseTime(result.sunrise.twilight_astronomical_start),
      nautical: parseTime(result.sunrise.twilight_nautical_start),
      civil: parseTime(result.sunrise.twilight_civil_start),
      sunrise: parseTime(result.sunrise.time)
    };

    const sunsetTimes = {
      sunset: parseTime(result.sunset.time),
      civil: parseTime(result.sunset.twilight_civil_end),
      nautical: parseTime(result.sunset.twilight_nautical_end),
      astronomical: parseTime(result.sunset.twilight_astronomical_end)
    };

    expect(sunriseTimes.astronomical).toBeLessThan(sunriseTimes.nautical);
    expect(sunriseTimes.nautical).toBeLessThan(sunriseTimes.civil);
    expect(sunriseTimes.civil).toBeLessThan(sunriseTimes.sunrise);

    expect(sunsetTimes.sunset).toBeLessThan(sunsetTimes.civil);
    expect(sunsetTimes.civil).toBeLessThan(sunsetTimes.nautical);
    expect(sunsetTimes.nautical).toBeLessThan(sunsetTimes.astronomical);

    console.log('Twilight times ordering verified');
  });

  test('Timezone lookup', () => {
    const nyTz = getTimezone(40.7128, -74.0060);
    const londonTz = getTimezone(51.5074, -0.1278);
    const sydneyTz = getTimezone(-33.8688, 151.2093);

    expect(nyTz).toMatch(/America\/New_York|America\/Toronto/);
    expect(londonTz).toMatch(/Europe\/London|GMT/);
    expect(sydneyTz).toMatch(/Australia\/Sydney|Australia/);

    console.log(`Timezones - NY: ${nyTz}, London: ${londonTz}, Sydney: ${sydneyTz}`);
  });
});
