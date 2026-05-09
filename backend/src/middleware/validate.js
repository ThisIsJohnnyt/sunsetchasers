function validateForecastRequest(body) {
  if (!body) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Request body is required',
      details: {}
    };
  }

  const { latitude, longitude, date, type } = body;

  if (latitude === undefined || latitude === null) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Latitude is required',
      details: { field: 'latitude' }
    };
  }

  if (typeof latitude !== 'number') {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Latitude must be a number',
      details: { field: 'latitude', value: latitude }
    };
  }

  if (latitude < -90 || latitude > 90) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Latitude must be between -90 and 90',
      details: { field: 'latitude', value: latitude }
    };
  }

  if (longitude === undefined || longitude === null) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Longitude is required',
      details: { field: 'longitude' }
    };
  }

  if (typeof longitude !== 'number') {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Longitude must be a number',
      details: { field: 'longitude', value: longitude }
    };
  }

  if (longitude < -180 || longitude > 180) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Longitude must be between -180 and 180',
      details: { field: 'longitude', value: longitude }
    };
  }

  if (!date) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Date is required',
      details: { field: 'date' }
    };
  }

  const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
  if (!dateRegex.test(date)) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Date must be in YYYY-MM-DD format',
      details: { field: 'date', value: date }
    };
  }

  const requestDate = new Date(date + 'T00:00:00Z');
  if (isNaN(requestDate.getTime())) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Date is not a valid date',
      details: { field: 'date', value: date }
    };
  }

  const today = new Date();
  today.setUTCHours(0, 0, 0, 0);
  const maxDate = new Date(today);
  maxDate.setUTCDate(maxDate.getUTCDate() + 7);

  if (requestDate < today || requestDate > maxDate) {
    const maxDateStr = maxDate.toISOString().split('T')[0];
    return {
      valid: false,
      code: 'DATE_OUT_OF_RANGE',
      message: 'Forecast date must be within 7 days of today',
      details: {
        requested_date: date,
        max_forecast_date: maxDateStr,
        days_ahead: Math.floor((requestDate - today) / (1000 * 60 * 60 * 24))
      }
    };
  }

  if (type && !['sunrise', 'sunset', 'both'].includes(type)) {
    return {
      valid: false,
      code: 'INVALID_REQUEST',
      message: 'Type must be "sunrise", "sunset", or "both"',
      details: { field: 'type', value: type }
    };
  }

  return { valid: true };
}

module.exports = { validateForecastRequest };
