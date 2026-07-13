package com.sunsetchasers.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sunsetchasers.core.model.ThemePreference
import com.sunsetchasers.core.model.UnitSystem
import com.sunsetchasers.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val UNITS_KEY = stringPreferencesKey("units")
private val THEME_KEY = stringPreferencesKey("theme")

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<UserSettings> = dataStore.data.map { prefs ->
        UserSettings(
            units = prefs[UNITS_KEY]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() }
                ?: UnitSystem.METRIC,
            theme = prefs[THEME_KEY]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM
        )
    }

    suspend fun setUnits(units: UnitSystem) {
        dataStore.edit { it[UNITS_KEY] = units.name }
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[THEME_KEY] = theme.name }
    }
}
