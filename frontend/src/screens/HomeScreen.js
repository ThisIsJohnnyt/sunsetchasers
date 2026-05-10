import React, { useState, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from '@react-navigation/native';
import * as Location from 'expo-location';
import LocationSearch from '../components/LocationSearch';
import DatePicker from '../components/DatePicker';
import { fetchForecast, reverseGeocode } from '../utils/api';
import { saveFavorite } from '../utils/storage';
import { formatDate } from '../utils/formatting';

export default function HomeScreen({ navigation }) {
  const [location, setLocation] = useState(null);
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [showSaveButton, setShowSaveButton] = useState(false);

  useFocusEffect(
    useCallback(() => {
      return () => setShowSaveButton(false);
    }, [])
  );

  const handleLocationSelect = (loc) => {
    setLocation(loc);
    setError(null);
  };

  const handleGPSLocation = async () => {
    try {
      setLoading(true);
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        Alert.alert('Permission Denied', 'Location permission is required');
        return;
      }
      const gpsLocation = await Location.getCurrentPositionAsync({});
      const { latitude, longitude } = gpsLocation.coords;
      const city = await reverseGeocode(latitude, longitude);
      setLocation({ name: city, latitude, longitude });
      setError(null);
    } catch (err) {
      Alert.alert('Error', 'Could not get your location');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async () => {
    if (!location) {
      setError('Please select a location');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const result = await fetchForecast(location.latitude, location.longitude, date);
      if (result.success) {
        setShowSaveButton(true);
        navigation.navigate('Forecast', { forecast: result.data, location });
      } else {
        setError(result.error || 'Failed to fetch forecast');
      }
    } catch (err) {
      setError('An unexpected error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveLocation = async () => {
    if (!location) return;
    const result = await saveFavorite(location);
    if (result.success) {
      Alert.alert('Saved', `"${location.name}" has been saved to your favorites`);
      setShowSaveButton(false);
    } else {
      Alert.alert('Error', result.error);
    }
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Ionicons name="sunny" size={40} color="#FF8C42" />
          <Text style={styles.headerText}>Sunset Forecast</Text>
          <Text style={styles.headerSubtext}>Plan your perfect shot</Text>
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Location</Text>
          <LocationSearch onLocationSelect={handleLocationSelect} />
          <TouchableOpacity style={styles.gpsButton} onPress={handleGPSLocation} disabled={loading}>
            <Ionicons name="locate" size={18} color="#fff" />
            <Text style={styles.gpsButtonText}>Use My Location</Text>
          </TouchableOpacity>
          {location && (
            <View style={styles.selectedLocation}>
              <Ionicons name="checkmark-circle" size={16} color="#4CAF50" />
              <Text style={styles.selectedLocationText}>{location.name}</Text>
            </View>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.label}>Date</Text>
          <DatePicker onDateSelect={setDate} />
          <View style={styles.selectedDate}>
            <Ionicons name="calendar" size={16} color="#4CAF50" />
            <Text style={styles.selectedDateText}>{formatDate(date)}</Text>
          </View>
        </View>

        {error && (
          <View style={styles.errorBox}>
            <Ionicons name="alert-circle" size={18} color="#FF6B6B" />
            <Text style={styles.errorText}>{error}</Text>
          </View>
        )}

        <TouchableOpacity
          style={[styles.searchButton, loading && styles.searchButtonDisabled]}
          onPress={handleSearch}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <>
              <Ionicons name="search" size={20} color="#fff" />
              <Text style={styles.searchButtonText}>Get Forecast</Text>
            </>
          )}
        </TouchableOpacity>

        {showSaveButton && location && (
          <TouchableOpacity style={styles.saveButton} onPress={handleSaveLocation}>
            <Ionicons name="heart" size={20} color="#fff" />
            <Text style={styles.saveButtonText}>Save Location</Text>
          </TouchableOpacity>
        )}

        <Text style={styles.footer}>Forecasts available up to 7 days ahead</Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 16 },
  header: { alignItems: 'center', marginBottom: 32, marginTop: 16 },
  headerText: { fontSize: 28, fontWeight: 'bold', color: '#333', marginTop: 12 },
  headerSubtext: { fontSize: 14, color: '#666', marginTop: 4 },
  section: { marginBottom: 24 },
  label: { fontSize: 16, fontWeight: '600', color: '#333', marginBottom: 10 },
  gpsButton: {
    flexDirection: 'row',
    backgroundColor: '#FF8C42',
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  gpsButtonText: { color: '#fff', fontWeight: '600', fontSize: 14, marginLeft: 8 },
  selectedLocation: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#F0F9F4',
    borderRadius: 8,
    marginTop: 8,
  },
  selectedLocationText: { color: '#4CAF50', fontWeight: '500', marginLeft: 8 },
  selectedDate: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    backgroundColor: '#F0F9F4',
    borderRadius: 8,
  },
  selectedDateText: { color: '#4CAF50', fontWeight: '500', marginLeft: 8 },
  errorBox: {
    flexDirection: 'row',
    backgroundColor: '#FFE6E6',
    borderRadius: 8,
    paddingVertical: 10,
    paddingHorizontal: 12,
    marginBottom: 16,
    alignItems: 'center',
  },
  errorText: { color: '#FF6B6B', marginLeft: 8, flex: 1 },
  searchButton: {
    backgroundColor: '#FF8C42',
    borderRadius: 8,
    paddingVertical: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 24,
  },
  searchButtonDisabled: { opacity: 0.6 },
  searchButtonText: { color: '#fff', fontWeight: '600', fontSize: 16, marginLeft: 8 },
  saveButton: {
    backgroundColor: '#4CAF50',
    borderRadius: 8,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  saveButtonText: { color: '#fff', fontWeight: '600', fontSize: 16, marginLeft: 8 },
  footer: { textAlign: 'center', color: '#999', fontSize: 12, marginTop: 16 },
});
