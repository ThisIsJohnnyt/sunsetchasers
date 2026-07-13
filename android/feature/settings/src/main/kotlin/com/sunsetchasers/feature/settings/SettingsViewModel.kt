package com.sunsetchasers.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunsetchasers.core.datastore.SettingsRepository
import com.sunsetchasers.core.model.ThemePreference
import com.sunsetchasers.core.model.UnitSystem
import com.sunsetchasers.core.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setUnits(units: UnitSystem) {
        viewModelScope.launch { repository.setUnits(units) }
    }

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch { repository.setTheme(theme) }
    }
}
