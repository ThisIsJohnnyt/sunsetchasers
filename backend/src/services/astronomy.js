const SunCalc = require('suncalc');
const geotimezone = require('geo-tz');

function getTimezone(lat, lon) {
  try {
    const timezone = geotimezone.find(lat, lon);
    return timezone ? timezone[0] : 'UTC';
  } catch (e) {
    return 'UTC';
  }
}

function formatTime(jsDate, timezone) {
  const timeStr = jsDate.toLocaleString('en-US', {
    timeZone: timezone,
    hour12: false,
    hour: '2-digit',
    minute: '2-digit'
  });
  return timeStr;
}

function calculateAzimuth(lat, lon, date, isSunrise) {
  const times = SunCalc.getTimes(date, lat, lon);
  const targetTime = isSunrise ? times.sunrise : times.sunset;

  const position = SunCalc.getPosition(targetTime, lat, lon);
  const azimuth = (position.azimuth * 180 / Math.PI + 180) % 360;
  return azimuth;
}

function getSunTimes(lat, lon, dateStr) {
  try {
    const date = new Date(dateStr + 'T12:00:00Z');
    const timezone = getTimezone(lat, lon);

    const sunTimes = SunCalc.getTimes(date, lat, lon);

    if (!sunTimes.sunrise || !sunTimes.sunset) {
      throw new Error(`Cannot calculate sun times for date ${dateStr}`);
    }

    const riseTime = sunTimes.sunrise;
    const setTime = sunTimes.sunset;

    // Get twilight times (suncalc provides civil dawn/dusk, nautical, astronomical)
    const civilDawn = sunTimes.dawn || '';
    const nauticalDawn = sunTimes.nauticalDawn || '';
    const astronomicalDawn = sunTimes.nightEnd || '';

    const civilDusk = sunTimes.dusk || '';
    const nauticalDusk = sunTimes.nauticalDusk || '';
    const astronomicalDusk = sunTimes.night || '';

    // Calculate azimuths
    const riseAzimuth = calculateAzimuth(lat, lon, date, true);
    const setAzimuth = calculateAzimuth(lat, lon, date, false);

    // Format times in local timezone
    const riseTimeLocal = formatTime(riseTime, timezone);
    const setTimeLocal = formatTime(setTime, timezone);
    const civilDawnLocal = civilDawn ? formatTime(civilDawn, timezone) : '';
    const nauticalDawnLocal = nauticalDawn ? formatTime(nauticalDawn, timezone) : '';
    const astronomicalDawnLocal = astronomicalDawn ? formatTime(astronomicalDawn, timezone) : '';
    const civilDuskLocal = civilDusk ? formatTime(civilDusk, timezone) : '';
    const nauticalDuskLocal = nauticalDusk ? formatTime(nauticalDusk, timezone) : '';
    const astronomicalDuskLocal = astronomicalDusk ? formatTime(astronomicalDusk, timezone) : '';

    return {
      timezone,
      sunrise: {
        time: riseTimeLocal,
        azimuth: parseFloat(riseAzimuth.toFixed(1)),
        altitudes: {
          at_horizon: 0.0,
          at_neg6: -6.0,
          at_neg18: -18.0
        },
        twilight_civil_start: civilDawnLocal,
        twilight_nautical_start: nauticalDawnLocal,
        twilight_astronomical_start: astronomicalDawnLocal
      },
      sunset: {
        time: setTimeLocal,
        azimuth: parseFloat(setAzimuth.toFixed(1)),
        altitudes: {
          at_horizon: 0.0,
          at_neg6: -6.0,
          at_neg18: -18.0
        },
        twilight_civil_end: civilDuskLocal,
        twilight_nautical_end: nauticalDuskLocal,
        twilight_astronomical_end: astronomicalDuskLocal
      }
    };
  } catch (error) {
    console.error(`Astronomy calculation error for ${dateStr} at (${lat}, ${lon}):`, error.message);
    throw new Error(`Cannot calculate sun times for ${dateStr} at (${lat}, ${lon})`);
  }
}

module.exports = {
  getSunTimes,
  getTimezone
};
