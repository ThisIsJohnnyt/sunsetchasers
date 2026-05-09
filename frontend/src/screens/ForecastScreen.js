import React from 'react';
import { View, Text, StyleSheet } from 'react-native';

export default function ForecastScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Forecast</Text>
      <Text style={styles.subtitle}>View forecast details and map</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
  },
});
