package com.sunsetchasers.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sunsetchasers.app.navigation.SunsetChasersNavHost
import com.sunsetchasers.core.datastore.SettingsRepository
import com.sunsetchasers.core.designsystem.theme.SunsetChasersTheme
import com.sunsetchasers.core.model.ThemePreference
import com.sunsetchasers.core.model.UserSettings
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepository.settings.collectAsState(initial = UserSettings())
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.theme) {
                ThemePreference.SYSTEM -> systemInDarkTheme
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            SunsetChasersTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SunsetChasersNavHost()
                }
            }
        }
    }
}
