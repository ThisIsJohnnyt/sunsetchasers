import React from 'react';
import { View, Text, StyleSheet, Dimensions } from 'react-native';
import MapView, { Marker } from 'react-native-maps';
import { azimuthToCompass } from '../utils/formatting';

const SCREEN_WIDTH = Dimensions.get('window').width;
const MAP_HEIGHT = 400;

export default function MapComponent({ latitude, longitude, locationName, sunrise, sunset }) {
  const initialRegion = {
    latitude,
    longitude,
    latitudeDelta: 0.05,
    longitudeDelta: 0.05,
  };

  return (
    <View style={styles.container}>
      <MapView
        style={styles.map}
        initialRegion={initialRegion}
        scrollEnabled={true}
        zoomEnabled={true}
      >
        <Marker
          coordinate={{ latitude, longitude }}
          title={locationName}
          description="Forecast location"
          pinColor="#FF8C42"
        />
      </MapView>

      <View style={styles.compass}>
        <Text style={styles.compassN}>N</Text>
        <Text style={styles.compassE}>E</Text>
        <Text style={styles.compassS}>S</Text>
        <Text style={styles.compassW}>W</Text>
      </View>

      <View style={styles.sunOverlay}>
        <SunPositionIndicator
          azimuth={sunrise.azimuth}
          type="sunrise"
          time={sunrise.time}
        />
        <SunPositionIndicator
          azimuth={sunset.azimuth}
          type="sunset"
          time={sunset.time}
        />
      </View>

      <View style={styles.legend}>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#FFA500' }]} />
          <Text style={styles.legendText}>Sunrise {sunrise.azimuth.toFixed(0)}° ({azimuthToCompass(sunrise.azimuth)})</Text>
        </View>
        <View style={styles.legendItem}>
          <View style={[styles.legendDot, { backgroundColor: '#FF6B6B' }]} />
          <Text style={styles.legendText}>Sunset {sunset.azimuth.toFixed(0)}° ({azimuthToCompass(sunset.azimuth)})</Text>
        </View>
      </View>
    </View>
  );
}

function SunPositionIndicator({ azimuth, type, time }) {
  const color = type === 'sunrise' ? '#FFA500' : '#FF6B6B';
  const centerX = SCREEN_WIDTH / 2;
  const centerY = MAP_HEIGHT / 2;
  const lineLength = 80;

  const endX = centerX + lineLength * Math.cos((azimuth * Math.PI) / 180);
  const endY = centerY + lineLength * Math.sin((azimuth * Math.PI) / 180);

  return (
    <>
      <View
        style={[
          styles.azimuthLine,
          {
            top: centerY,
            left: centerX,
            width: lineLength,
            backgroundColor: color,
            transform: [{ rotate: `${azimuth - 90}deg` }],
          },
        ]}
      />
      <View
        style={[
          styles.sunMarker,
          {
            top: endY - 12,
            left: endX - 12,
            backgroundColor: color,
          },
        ]}
      >
        <Text style={styles.sunMarkerText}>☀</Text>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'relative',
    height: MAP_HEIGHT,
    borderRadius: 8,
    overflow: 'hidden',
    marginBottom: 16,
  },
  map: {
    width: '100%',
    height: '100%',
  },
  compass: {
    position: 'absolute',
    top: 16,
    right: 16,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#FF8C42',
  },
  compassN: {
    position: 'absolute',
    top: 6,
    fontWeight: 'bold',
    color: '#FF8C42',
    fontSize: 16,
  },
  compassE: {
    position: 'absolute',
    right: 6,
    fontWeight: 'bold',
    color: '#FF8C42',
    fontSize: 14,
  },
  compassS: {
    position: 'absolute',
    bottom: 6,
    fontWeight: 'bold',
    color: '#FF8C42',
    fontSize: 14,
  },
  compassW: {
    position: 'absolute',
    left: 6,
    fontWeight: 'bold',
    color: '#FF8C42',
    fontSize: 14,
  },
  sunOverlay: {
    position: 'absolute',
    width: '100%',
    height: '100%',
    top: 0,
    left: 0,
  },
  azimuthLine: {
    position: 'absolute',
    height: 2,
    opacity: 0.7,
  },
  sunMarker: {
    position: 'absolute',
    width: 24,
    height: 24,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sunMarkerText: {
    fontSize: 14,
  },
  legend: {
    position: 'absolute',
    bottom: 12,
    left: 12,
    right: 12,
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: 8,
    padding: 10,
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  legendDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    marginRight: 8,
  },
  legendText: {
    fontSize: 12,
    color: '#333',
    fontWeight: '500',
  },
});
