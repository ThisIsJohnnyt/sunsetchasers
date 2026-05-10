import axios from 'axios';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:3000/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

const MAX_RETRIES = 2;

export async function fetchForecast(latitude, longitude, date, retryCount = 0) {
  if (latitude == null || longitude == null || !date) {
    return {
      success: false,
      error: 'Missing required parameters',
    };
  }

  if (latitude < -90 || latitude > 90) {
    return {
      success: false,
      error: 'Invalid latitude: must be between -90 and 90',
    };
  }

  if (longitude < -180 || longitude > 180) {
    return {
      success: false,
      error: 'Invalid longitude: must be between -180 and 180',
    };
  }

  try {
    const response = await apiClient.post('/forecast', {
      latitude,
      longitude,
      date,
      type: 'both',
    });
    return {
      success: true,
      data: response.data,
    };
  } catch (error) {
    if (retryCount < MAX_RETRIES && error.code === 'ECONNABORTED') {
      return fetchForecast(latitude, longitude, date, retryCount + 1);
    }

    let errorMessage = 'Failed to fetch forecast';
    let statusCode = null;

    if (error.response?.status === 422) {
      errorMessage = error.response.data?.message || 'Date is out of range (must be within 7 days)';
      statusCode = 422;
    } else if (error.response?.status === 400) {
      errorMessage = error.response.data?.message || 'Invalid request parameters';
      statusCode = 400;
    } else if (error.response?.status === 500) {
      errorMessage = 'Server error. Please try again later.';
      statusCode = 500;
    } else if (error.code === 'ECONNREFUSED') {
      errorMessage = 'Could not connect to server. Is the API running?';
    } else if (error.code === 'ENOTFOUND') {
      errorMessage = 'Server not found. Check your API URL.';
    } else if (error.message?.includes('timeout')) {
      errorMessage = 'Request timed out. Please try again.';
    } else if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    } else if (error.message) {
      errorMessage = error.message;
    }

    return {
      success: false,
      error: errorMessage,
      code: error.response?.data?.code,
      statusCode,
    };
  }
}

export async function reverseGeocode(latitude, longitude) {
  try {
    const response = await axios.get(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`,
      { timeout: 5000 }
    );
    return response.data.address?.city || response.data.address?.town || 'Unknown Location';
  } catch (error) {
    return 'Location';
  }
}
