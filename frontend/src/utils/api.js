import axios from 'axios';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:3000/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

export async function fetchForecast(latitude, longitude, date) {
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
    let errorMessage = 'Failed to fetch forecast';
    if (error.response?.data?.message) {
      errorMessage = error.response.data.message;
    } else if (error.message) {
      errorMessage = error.message;
    }
    return {
      success: false,
      error: errorMessage,
      code: error.response?.data?.code,
    };
  }
}

export async function reverseGeocode(latitude, longitude) {
  try {
    const response = await axios.get(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}`
    );
    return response.data.address?.city || response.data.address?.town || 'Unknown Location';
  } catch (error) {
    return 'Location';
  }
}

export function formatCoordinates(lat, lon) {
  const latDir = lat >= 0 ? 'N' : 'S';
  const lonDir = lon >= 0 ? 'E' : 'W';
  return `${Math.abs(lat).toFixed(2)}°${latDir}, ${Math.abs(lon).toFixed(2)}°${lonDir}`;
}
