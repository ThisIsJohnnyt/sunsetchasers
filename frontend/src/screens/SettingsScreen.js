import React, { useState, useFocusEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Switch } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getSettings, saveSettings } from '../utils/storage';

export default function SettingsScreen() {
  const [timeFormat, setTimeFormat] = useState('12h');
  const [tempUnit, setTempUnit] = useState('C');
  const [distanceUnit, setDistanceUnit] = useState('km');

  useFocusEffect(
    React.useCallback(() => {
      loadSettings();
    }, [])
  );

  const loadSettings = async () => {
    const settings = await getSettings();
    setTimeFormat(settings.timeFormat);
    setTempUnit(settings.tempUnit);
    setDistanceUnit(settings.distanceUnit);
  };

  const updateSettings = async (updates) => {
    const settings = { timeFormat, tempUnit, distanceUnit, ...updates };
    await saveSettings(settings);
  };

  const handleTimeFormatChange = async () => {
    const newFormat = timeFormat === '12h' ? '24h' : '12h';
    setTimeFormat(newFormat);
    await updateSettings({ timeFormat: newFormat });
  };

  const handleTempUnitChange = async () => {
    const newUnit = tempUnit === 'C' ? 'F' : 'C';
    setTempUnit(newUnit);
    await updateSettings({ tempUnit: newUnit });
  };

  const handleDistanceUnitChange = async () => {
    const newUnit = distanceUnit === 'km' ? 'miles' : 'km';
    setDistanceUnit(newUnit);
    await updateSettings({ distanceUnit: newUnit });
  };

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}>
      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Display</Text>

        <SettingRow
          icon="time"
          label="Time Format"
          value={timeFormat === '12h' ? '12-hour' : '24-hour'}
          onPress={handleTimeFormatChange}
        />

        <SettingRow
          icon="thermometer"
          label="Temperature Unit"
          value={tempUnit === 'C' ? 'Celsius (°C)' : 'Fahrenheit (°F)'}
          onPress={handleTempUnitChange}
        />

        <SettingRow
          icon="location"
          label="Distance Unit"
          value={distanceUnit === 'km' ? 'Kilometers' : 'Miles'}
          onPress={handleDistanceUnitChange}
        />
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>About</Text>

        <View style={styles.infoRow}>
          <View style={styles.infoContent}>
            <Text style={styles.infoLabel}>App Version</Text>
            <Text style={styles.infoValue}>1.0.0</Text>
          </View>
        </View>

        <View style={styles.infoRow}>
          <View style={styles.infoContent}>
            <Text style={styles.infoLabel}>Forecast Data</Text>
            <Text style={styles.infoValue}>7 days ahead</Text>
          </View>
        </View>

        <View style={styles.infoRow}>
          <View style={styles.infoContent}>
            <Text style={styles.infoLabel}>Accuracy Warning</Text>
            <Text style={styles.infoValue}>Forecasts >48h may be less accurate</Text>
          </View>
        </View>
      </View>

      <View style={styles.footerText}>
        <Text style={styles.footer}>
          Sunrise/Sunset Forecast App v1.0
        </Text>
        <Text style={styles.footerSubtext}>
          Plan your perfect sunrise and sunset shots
        </Text>
      </View>
    </ScrollView>
  );
}

function SettingRow({ icon, label, value, onPress }) {
  return (
    <TouchableOpacity style={styles.settingRow} onPress={onPress}>
      <Ionicons name={icon} size={24} color="#FF8C42" style={styles.settingIcon} />
      <View style={styles.settingContent}>
        <Text style={styles.settingLabel}>{label}</Text>
        <Text style={styles.settingValue}>{value}</Text>
      </View>
      <Ionicons name="chevron-forward" size={20} color="#999" />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  section: {
    paddingHorizontal: 16,
    paddingVertical: 20,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 12,
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 12,
    backgroundColor: '#f8f8f8',
    borderRadius: 8,
    marginBottom: 10,
  },
  settingIcon: {
    marginRight: 12,
  },
  settingContent: {
    flex: 1,
  },
  settingLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  settingValue: {
    fontSize: 12,
    color: '#999',
    marginTop: 2,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 12,
    backgroundColor: '#f8f8f8',
    borderRadius: 8,
    marginBottom: 10,
  },
  infoContent: {
    flex: 1,
  },
  infoLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  infoValue: {
    fontSize: 12,
    color: '#999',
    marginTop: 2,
  },
  footerText: {
    alignItems: 'center',
    paddingVertical: 30,
    paddingHorizontal: 16,
  },
  footer: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  footerSubtext: {
    fontSize: 12,
    color: '#999',
    marginTop: 4,
  },
});
