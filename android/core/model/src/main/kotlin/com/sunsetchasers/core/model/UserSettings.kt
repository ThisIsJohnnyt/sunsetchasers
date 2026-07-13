package com.sunsetchasers.core.model

enum class UnitSystem {
    METRIC,
    IMPERIAL
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val units: UnitSystem = UnitSystem.METRIC,
    val theme: ThemePreference = ThemePreference.SYSTEM
)
