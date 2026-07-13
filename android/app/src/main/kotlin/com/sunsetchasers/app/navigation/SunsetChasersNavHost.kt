package com.sunsetchasers.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sunsetchasers.feature.favorites.FAVORITES_ROUTE
import com.sunsetchasers.feature.favorites.favoritesScreen
import com.sunsetchasers.feature.forecast.FORECAST_ROUTE
import com.sunsetchasers.feature.forecast.forecastScreen
import com.sunsetchasers.feature.settings.SETTINGS_ROUTE
import com.sunsetchasers.feature.settings.settingsScreen

@Composable
fun SunsetChasersNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = FORECAST_ROUTE) {
        forecastScreen(
            onNavigateToFavorites = { navController.navigate(FAVORITES_ROUTE) },
            onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) }
        )
        favoritesScreen(navController)
        settingsScreen(navController)
    }
}
