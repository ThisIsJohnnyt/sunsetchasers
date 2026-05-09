export function formatTime(timeStr, format = '12h') {
  if (!timeStr) return '';
  const [hours, minutes] = timeStr.split(':');
  let h = parseInt(hours, 10);
  const m = minutes;

  if (format === '12h') {
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    return `${h}:${m} ${ampm}`;
  }
  return `${hours}:${m}`;
}

export function convertTemperature(celsius, unit = 'C') {
  if (unit === 'F') {
    return Math.round((celsius * 9) / 5 + 32);
  }
  return Math.round(celsius);
}

export function convertDistance(km, unit = 'km') {
  if (unit === 'miles') {
    return (km * 0.621371).toFixed(1);
  }
  return km;
}

export function azimuthToCompass(azimuth) {
  const directions = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
  const index = Math.round(azimuth / 22.5) % 16;
  return directions[index];
}

export function formatDate(dateStr) {
  const date = new Date(dateStr + 'T00:00:00');
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}

export function getQualityColor(level) {
  switch (level) {
    case 1:
      return '#FF8C42';
    case 2:
      return '#FFD700';
    case 3:
      return '#A9A9A9';
    case 4:
      return '#696969';
    default:
      return '#999';
  }
}

export function getQualityLabel(level) {
  switch (level) {
    case 1:
      return 'Excellent';
    case 2:
      return 'Good';
    case 3:
      return 'Fair';
    case 4:
      return 'Poor';
    default:
      return 'Unknown';
  }
}
