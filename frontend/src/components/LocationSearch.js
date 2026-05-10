import React, { useState, useRef, useEffect } from 'react';
import { View, TextInput, StyleSheet, TouchableOpacity, Text, FlatList, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

const GEOCODING_API = 'https://nominatim.openstreetmap.org/search';
const DEBOUNCE_MS = 300;

export default function LocationSearch({ onLocationSelect }) {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const debounceTimer = useRef(null);
  const abortController = useRef(null);

  useEffect(() => {
    return () => {
      clearTimeout(debounceTimer.current);
      abortController.current?.abort();
    };
  }, []);

  const searchLocations = (text) => {
    setQuery(text);
    clearTimeout(debounceTimer.current);

    if (text.trim().length < 2) {
      setResults([]);
      setShowResults(false);
      return;
    }

    debounceTimer.current = setTimeout(() => fetchResults(text.trim()), DEBOUNCE_MS);
  };

  const fetchResults = async (searchText) => {
    abortController.current?.abort();
    abortController.current = new AbortController();

    setLoading(true);
    setShowResults(true);

    try {
      const response = await fetch(
        `${GEOCODING_API}?q=${encodeURIComponent(searchText)}&format=json&limit=8`,
        { signal: abortController.current.signal }
      );

      if (!response.ok) throw new Error(`HTTP ${response.status}`);

      const data = await response.json();

      setResults(
        Array.isArray(data)
          ? data.map(item => ({
              id: item.osm_id,
              name: item.name || item.display_name.split(',')[0],
              displayName: item.display_name,
              lat: parseFloat(item.lat),
              lon: parseFloat(item.lon),
            }))
          : []
      );
    } catch (err) {
      if (err.name !== 'AbortError') {
        console.error('Geocoding error:', err);
        setResults([]);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSelectLocation = (location) => {
    setQuery(location.displayName);
    setShowResults(false);
    setResults([]);
    onLocationSelect({
      name: location.name,
      latitude: location.lat,
      longitude: location.lon,
    });
  };

  const handleClear = () => {
    setQuery('');
    setResults([]);
    setShowResults(false);
    abortController.current?.abort();
  };

  return (
    <View style={styles.container}>
      <View style={styles.searchBox}>
        <Ionicons name="location" size={20} color="#FF8C42" style={styles.icon} />
        <TextInput
          style={styles.input}
          placeholder="Search location..."
          value={query}
          onChangeText={searchLocations}
          placeholderTextColor="#999"
        />
        {query.length > 0 && (
          <TouchableOpacity onPress={handleClear}>
            <Ionicons name="close-circle" size={20} color="#999" />
          </TouchableOpacity>
        )}
      </View>

      {loading && <ActivityIndicator size="small" color="#FF8C42" style={styles.loader} />}

      {showResults && !loading && results.length === 0 && query.trim().length >= 2 && (
        <View style={styles.noResults}>
          <Ionicons name="search" size={24} color="#999" />
          <Text style={styles.noResultsText}>No locations found</Text>
          <Text style={styles.noResultsSubtext}>Try a different search term</Text>
        </View>
      )}

      {showResults && results.length > 0 && (
        <FlatList
          data={results}
          keyExtractor={item => item.id.toString()}
          scrollEnabled={false}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.resultItem} onPress={() => handleSelectLocation(item)}>
              <Ionicons name="location-outline" size={16} color="#666" />
              <View style={styles.resultText}>
                <Text style={styles.resultName}>{item.name}</Text>
                <Text style={styles.resultDesc} numberOfLines={1}>{item.displayName}</Text>
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { width: '100%' },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    marginBottom: 8,
  },
  icon: { marginRight: 10 },
  input: { flex: 1, fontSize: 16, color: '#333' },
  loader: { marginVertical: 8 },
  noResults: { alignItems: 'center', paddingVertical: 20 },
  noResultsText: { fontSize: 14, color: '#999', marginTop: 8, fontWeight: '500' },
  noResultsSubtext: { fontSize: 12, color: '#ccc', marginTop: 4 },
  resultItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  resultText: { flex: 1, marginLeft: 10 },
  resultName: { fontSize: 14, fontWeight: '600', color: '#333' },
  resultDesc: { fontSize: 12, color: '#999', marginTop: 2 },
});
