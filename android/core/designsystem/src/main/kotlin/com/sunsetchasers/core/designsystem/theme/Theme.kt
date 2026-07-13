package com.sunsetchasers.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SunsetOrange,
    onPrimary = Color.White,
    secondary = TwilightPurple,
    tertiary = GoldHour,
    background = NeutralLightBackground,
    surface = NeutralLightBackground
)

private val DarkColors = darkColorScheme(
    primary = SunsetOrange,
    onPrimary = Color.White,
    secondary = TwilightPurpleDark,
    tertiary = GoldHour,
    background = NeutralDarkBackground,
    surface = NeutralDarkBackground
)

@Composable
fun SunsetChasersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SunsetChasersTypography,
        content = content
    )
}
