package com.sunsetchasers.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val SETTINGS_ROUTE = "settings"

fun NavGraphBuilder.settingsScreen(navController: NavController) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(onBack = { navController.popBackStack() })
    }
}
