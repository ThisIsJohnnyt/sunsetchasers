import AsyncStorage from '@react-native-async-storage/async-storage';

const FAVORITES_KEY = '@sunset_app_favorites';
const SETTINGS_KEY = '@sunset_app_settings';

const DEFAULT_SETTINGS = {
  timeFormat: '12h',
  tempUnit: 'C',
  distanceUnit: 'km',
};

export async function saveFavorite(location) {
  try {
    const favorites = await getFavorites();
    if (favorites.length >= 5) {
      return { success: false, error: 'Maximum 5 saved locations' };
    }
    const updated = [...favorites, { ...location, id: Date.now() }];
    await AsyncStorage.setItem(FAVORITES_KEY, JSON.stringify(updated));
    return { success: true };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function getFavorites() {
  try {
    const data = await AsyncStorage.getItem(FAVORITES_KEY);
    return data ? JSON.parse(data) : [];
  } catch (error) {
    return [];
  }
}

export async function removeFavorite(id) {
  try {
    const favorites = await getFavorites();
    const updated = favorites.filter(fav => fav.id !== id);
    await AsyncStorage.setItem(FAVORITES_KEY, JSON.stringify(updated));
    return { success: true };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function saveSettings(settings) {
  try {
    const merged = { ...DEFAULT_SETTINGS, ...settings };
    await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify(merged));
    return { success: true };
  } catch (error) {
    return { success: false, error: error.message };
  }
}

export async function getSettings() {
  try {
    const data = await AsyncStorage.getItem(SETTINGS_KEY);
    return data ? JSON.parse(data) : DEFAULT_SETTINGS;
  } catch (error) {
    return DEFAULT_SETTINGS;
  }
}
