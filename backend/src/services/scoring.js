function cloudRating(cloudCoverPercent) {
  if (cloudCoverPercent < 0 || cloudCoverPercent > 100) {
    return 0.1;
  }

  if (cloudCoverPercent <= 5) return 0.65;
  if (cloudCoverPercent <= 15) return 0.95;
  if (cloudCoverPercent <= 40) return 1.0;
  if (cloudCoverPercent <= 50) return 0.85;
  if (cloudCoverPercent <= 70) return 0.7;
  if (cloudCoverPercent <= 85) return 0.4;
  return 0.1;
}

function visibilityRating(visibilityKm) {
  if (visibilityKm < 0) {
    return 0.5;
  }

  if (visibilityKm < 1) return 0.0;
  if (visibilityKm < 3) return 0.3;
  if (visibilityKm < 6) return 0.7;
  if (visibilityKm < 9) return 0.9;
  return 1.0;
}

function conditionsRating(conditionStr) {
  const conditionLower = (conditionStr || '').toLowerCase();

  const ratings = {
    'clear': 1.0,
    'clear sky': 1.0,
    'sunny': 1.0,
    'mostly clear': 0.95,
    'partly cloudy': 0.85,
    'cloudy': 0.6,
    'clouds': 0.6,
    'overcast': 0.3,
    'fog': 0.1,
    'mist': 0.5,
    'haze': 0.5,
    'smoke': 0.4,
    'rain': 0.05,
    'light rain': 0.15,
    'drizzle': 0.1,
    'thunderstorm': 0.0
  };

  return ratings[conditionLower] !== undefined ? ratings[conditionLower] : 0.5;
}

function colorPotentialRating(sunAltitudeDegrees) {
  if (sunAltitudeDegrees > 0) {
    return 0.0;
  }

  if (sunAltitudeDegrees >= -3) return 1.0;
  if (sunAltitudeDegrees >= -6) return 0.95;
  if (sunAltitudeDegrees >= -12) return 0.75;
  if (sunAltitudeDegrees >= -18) return 0.5;
  return 0.2;
}

function calcScore(weatherData, astronomyData = {}) {
  const cloudRatingVal = cloudRating(weatherData.cloud_cover_percent || 50);
  const visibilityRatingVal = visibilityRating(weatherData.visibility_km || 10);
  const conditionRatingVal = conditionsRating(weatherData.conditions);

  const colorPotentialVal = colorPotentialRating(0);

  const overallScore = (
    (0.35 * cloudRatingVal) +
    (0.20 * visibilityRatingVal) +
    (0.15 * conditionRatingVal) +
    (0.30 * colorPotentialVal)
  );

  const clampedScore = Math.max(0, Math.min(1, overallScore));

  let level, label;
  if (clampedScore >= 0.8) {
    level = 1;
    label = 'Excellent';
  } else if (clampedScore >= 0.6) {
    level = 2;
    label = 'Good';
  } else if (clampedScore >= 0.4) {
    level = 3;
    label = 'Fair';
  } else {
    level = 4;
    label = 'Poor';
  }

  const conditions = weatherData.conditions || 'unknown';
  const visibility = weatherData.visibility_km || 'unknown';
  const cloudCover = weatherData.cloud_cover_percent || 'unknown';

  let reasoning = '';
  if (level === 1) {
    reasoning = `Clear skies with excellent visibility. Minimal cloud cover expected. Peak color window at sunrise/sunset.`;
  } else if (level === 2) {
    reasoning = `Good conditions with some clouds. Visibility is adequate. Colors should be visible.`;
  } else if (level === 3) {
    reasoning = `Moderate cloud cover or reduced visibility. Some color expected but potentially muted.`;
  } else {
    reasoning = `Heavy clouds or poor visibility. Limited color potential. Challenging conditions.`;
  }

  return {
    score: parseFloat(clampedScore.toFixed(2)),
    level,
    label,
    breakdown: {
      cloud_rating: parseFloat(cloudRatingVal.toFixed(2)),
      visibility_rating: parseFloat(visibilityRatingVal.toFixed(2)),
      condition_rating: parseFloat(conditionRatingVal.toFixed(2)),
      color_potential_rating: parseFloat(colorPotentialVal.toFixed(2))
    },
    reasoning
  };
}

module.exports = {
  calcScore,
  cloudRating,
  visibilityRating,
  conditionsRating,
  colorPotentialRating
};
