package com.sunsetchasers.feature.forecast

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val FORECAST_ROUTE = "forecast"

fun NavGraphBuilder.forecastScreen(
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    composable(FORECAST_ROUTE) {
        ForecastScreen(
            onNavigateToFavorites = onNavigateToFavorites,
            onNavigateToSettings = onNavigateToSettings
        )
    }
}
