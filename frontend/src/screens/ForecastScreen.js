import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getQualityColor, getQualityLabel, formatTime, azimuthToCompass } from '../utils/formatting';
import MapComponent from '../components/MapComponent';

export default function ForecastScreen({ route }) {
  const { forecast, location } = route.params || {};
  const [activeTab, setActiveTab] = useState('sunrise');

  if (!forecast) {
    return (
      <View style={styles.container}>
        <Text>No forecast data available</Text>
      </View>
    );
  }

  const activeEvent = activeTab === 'sunrise' ? forecast.sunrise : forecast.sunset;
  const qualityColor = getQualityColor(forecast.forecast_quality.level);

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.header}>
        <Text style={styles.location}>{location?.name || 'Location'}</Text>
        <Text style={styles.date}>{forecast.date}</Text>
      </View>

      <View style={styles.mapSection}>
        <MapComponent
          latitude={location?.latitude || 0}
          longitude={location?.longitude || 0}
          locationName={location?.name || 'Location'}
          sunrise={forecast.sunrise}
          sunset={forecast.sunset}
        />
      </View>

      <View style={[styles.qualityBadge, { backgroundColor: qualityColor + '20' }]}>
        <View style={[styles.qualityDot, { backgroundColor: qualityColor }]} />
        <View style={styles.qualityInfo}>
          <Text style={styles.qualityLevel}>{getQualityLabel(forecast.forecast_quality.level)}</Text>
          <Text style={styles.qualityScore}>{(forecast.forecast_quality.score * 100).toFixed(0)}% Quality</Text>
        </View>
        <Text style={[styles.qualityValue, { color: qualityColor }]}>
          {(forecast.forecast_quality.score * 100).toFixed(0)}%
        </Text>
      </View>

      {forecast.accuracy_warning && (
        <View style={styles.warningBox}>
          <Ionicons name="information-circle" size={18} color="#FFA500" />
          <Text style={styles.warningText}>Forecast accuracy decreases for dates >48 hours ahead</Text>
        </View>
      )}

      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'sunrise' && styles.tabActive]}
          onPress={() => setActiveTab('sunrise')}
        >
          <Ionicons name="md-sunrise" size={20} color={activeTab === 'sunrise' ? '#FF8C42' : '#999'} />
          <Text style={[styles.tabText, activeTab === 'sunrise' && styles.tabTextActive]}>Sunrise</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, activeTab === 'sunset' && styles.tabActive]}
          onPress={() => setActiveTab('sunset')}
        >
          <Ionicons name="md-sunset" size={20} color={activeTab === 'sunset' ? '#FF8C42' : '#999'} />
          <Text style={[styles.tabText, activeTab === 'sunset' && styles.tabTextActive]}>Sunset</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Sun Position</Text>
        <View style={styles.row}>
          <View style={styles.item}>
            <Text style={styles.itemLabel}>Time</Text>
            <Text style={styles.itemValue}>{formatTime(activeEvent.time, '12h')}</Text>
          </View>
          <View style={styles.item}>
            <Text style={styles.itemLabel}>Azimuth</Text>
            <Text style={styles.itemValue}>{activeEvent.azimuth.toFixed(1)}°</Text>
            <Text style={styles.itemSubtext}>{azimuthToCompass(activeEvent.azimuth)}</Text>
          </View>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Altitude Angles</Text>
        <View style={styles.altitudeContainer}>
          <View style={styles.altitudeItem}>
            <Text style={styles.altitudeLabel}>Horizon (0°)</Text>
            <Text style={styles.altitudeValue}>{activeEvent.altitudes.at_horizon}°</Text>
          </View>
          <View style={styles.altitudeItem}>
            <Text style={styles.altitudeLabel}>Civil (-6°)</Text>
            <Text style={styles.altitudeValue}>{activeEvent.altitudes.at_neg6}°</Text>
          </View>
          <View style={styles.altitudeItem}>
            <Text style={styles.altitudeLabel}>Astronomical (-18°)</Text>
            <Text style={styles.altitudeValue}>{activeEvent.altitudes.at_neg18}°</Text>
          </View>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Twilight Times</Text>
        {activeTab === 'sunrise' ? (
          <>
            <View style={styles.row}>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Astronomical Start</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_astronomical_start, '12h')}</Text>
              </View>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Nautical Start</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_nautical_start, '12h')}</Text>
              </View>
            </View>
            <View style={styles.row}>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Civil Start</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_civil_start, '12h')}</Text>
              </View>
            </View>
          </>
        ) : (
          <>
            <View style={styles.row}>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Civil End</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_civil_end, '12h')}</Text>
              </View>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Nautical End</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_nautical_end, '12h')}</Text>
              </View>
            </View>
            <View style={styles.row}>
              <View style={styles.item}>
                <Text style={styles.itemLabel}>Astronomical End</Text>
                <Text style={styles.itemValue}>{formatTime(activeEvent.twilight_astronomical_end, '12h')}</Text>
              </View>
            </View>
          </>
        )}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Weather Conditions</Text>
        <View style={styles.weatherGrid}>
          <View style={styles.weatherItem}>
            <Ionicons name="cloud" size={24} color="#FF8C42" />
            <Text style={styles.weatherLabel}>Cloud Cover</Text>
            <Text style={styles.weatherValue}>{forecast.weather.cloud_cover_percent}%</Text>
          </View>
          <View style={styles.weatherItem}>
            <Ionicons name="eye" size={24} color="#FF8C42" />
            <Text style={styles.weatherLabel}>Visibility</Text>
            <Text style={styles.weatherValue}>{forecast.weather.visibility_km} km</Text>
          </View>
          <View style={styles.weatherItem}>
            <Ionicons name="thermometer" size={24} color="#FF8C42" />
            <Text style={styles.weatherLabel}>Temperature</Text>
            <Text style={styles.weatherValue}>{Math.round(forecast.weather.temperature_c)}°C</Text>
          </View>
          <View style={styles.weatherItem}>
            <Ionicons name="water" size={24} color="#FF8C42" />
            <Text style={styles.weatherLabel}>Humidity</Text>
            <Text style={styles.weatherValue}>{forecast.weather.humidity_percent}%</Text>
          </View>
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Quality Breakdown</Text>
        <View style={styles.breakdownContainer}>
          <BreakdownItem label="Cloud Rating" value={forecast.forecast_quality.breakdown.cloud_rating} />
          <BreakdownItem label="Visibility" value={forecast.forecast_quality.breakdown.visibility_rating} />
          <BreakdownItem label="Conditions" value={forecast.forecast_quality.breakdown.condition_rating} />
          <BreakdownItem label="Color Potential" value={forecast.forecast_quality.breakdown.color_potential_rating} />
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Analysis</Text>
        <Text style={styles.reasoning}>{forecast.forecast_quality.reasoning}</Text>
      </View>

      <View style={styles.footerSpacing} />
    </ScrollView>
  );
}

function BreakdownItem({ label, value }) {
  const percentage = Math.round(value * 100);
  return (
    <View style={styles.breakdownItem}>
      <Text style={styles.breakdownLabel}>{label}</Text>
      <View style={styles.progressBar}>
        <View style={[styles.progressFill, { width: `${percentage}%` }]} />
      </View>
      <Text style={styles.breakdownValue}>{percentage}%</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    padding: 16,
    paddingTop: 12,
    backgroundColor: '#FFF5E6',
    borderBottomWidth: 1,
    borderBottomColor: '#FFE8CC',
  },
  mapSection: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  location: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  date: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  qualityBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    margin: 16,
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderRadius: 8,
  },
  qualityDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 10,
  },
  qualityInfo: {
    flex: 1,
  },
  qualityLevel: {
    fontWeight: 'bold',
    color: '#333',
    fontSize: 14,
  },
  qualityScore: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  qualityValue: {
    fontWeight: 'bold',
    fontSize: 18,
  },
  warningBox: {
    flexDirection: 'row',
    backgroundColor: '#FFF5E6',
    marginHorizontal: 16,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 16,
  },
  warningText: {
    color: '#FFA500',
    marginLeft: 8,
    flex: 1,
    fontSize: 12,
  },
  tabContainer: {
    flexDirection: 'row',
    marginHorizontal: 16,
    marginBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  tab: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  tabActive: {
    borderBottomColor: '#FF8C42',
  },
  tabText: {
    color: '#999',
    fontWeight: '500',
    marginLeft: 6,
  },
  tabTextActive: {
    color: '#FF8C42',
  },
  section: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  item: {
    flex: 1,
    backgroundColor: '#f8f8f8',
    padding: 12,
    borderRadius: 8,
    marginRight: 8,
  },
  itemLabel: {
    fontSize: 12,
    color: '#999',
    marginBottom: 6,
  },
  itemValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  itemSubtext: {
    fontSize: 11,
    color: '#FF8C42',
    marginTop: 2,
  },
  altitudeContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  altitudeItem: {
    flex: 1,
    backgroundColor: '#f8f8f8',
    padding: 10,
    borderRadius: 8,
    marginRight: 8,
  },
  altitudeLabel: {
    fontSize: 11,
    color: '#999',
    marginBottom: 4,
  },
  altitudeValue: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
  },
  weatherGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  weatherItem: {
    width: '48%',
    backgroundColor: '#f8f8f8',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
    marginBottom: 10,
  },
  weatherLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 6,
  },
  weatherValue: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginTop: 2,
  },
  breakdownContainer: {
    backgroundColor: '#f8f8f8',
    padding: 12,
    borderRadius: 8,
  },
  breakdownItem: {
    marginBottom: 12,
  },
  breakdownLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: '#333',
    marginBottom: 4,
  },
  progressBar: {
    height: 6,
    backgroundColor: '#eee',
    borderRadius: 3,
    overflow: 'hidden',
    marginBottom: 4,
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#FF8C42',
  },
  breakdownValue: {
    fontSize: 12,
    color: '#666',
  },
  reasoning: {
    fontSize: 14,
    color: '#333',
    lineHeight: 20,
  },
  footerSpacing: {
    height: 30,
  },
});
