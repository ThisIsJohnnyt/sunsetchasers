package com.sunsetchasers.feature.favorites

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val FAVORITES_ROUTE = "favorites"

fun NavGraphBuilder.favoritesScreen(navController: NavController) {
    composable(FAVORITES_ROUTE) {
        FavoritesScreen(onBack = { navController.popBackStack() })
    }
}
