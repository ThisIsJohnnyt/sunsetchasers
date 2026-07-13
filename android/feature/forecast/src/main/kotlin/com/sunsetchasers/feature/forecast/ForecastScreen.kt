package com.sunsetchasers.feature.forecast

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.sunsetchasers.core.model.ColorTimelineEntry
import com.sunsetchasers.core.model.Forecast
import com.sunsetchasers.core.model.ForecastType
import com.sunsetchasers.core.model.SunEvent
import com.sunsetchasers.core.model.UnitSystem
import com.sunsetchasers.core.model.WeatherConditions
import com.sunsetchasers.feature.forecast.components.AzimuthCompass
import com.sunsetchasers.feature.forecast.map.SunPositionMap
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ForecastViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.useDeviceLocation()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission denied — enter coordinates manually")
            }
        }
    }

    fun requestDeviceLocation() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.useDeviceLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sunset Chasers") },
                actions = {
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorites")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (favorites.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(favorites, key = { it.id }) { favorite ->
                            AssistChip(
                                onClick = { viewModel.selectFavorite(favorite) },
                                label = { Text(favorite.name) }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { requestDeviceLocation() },
                    enabled = !uiState.isLocating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(text = "  Use My Location")
                }
            }
            item {
                OutlinedTextField(
                    value = uiState.latitudeInput,
                    onValueChange = viewModel::onLatitudeChange,
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.longitudeInput,
                    onValueChange = viewModel::onLongitudeChange,
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ForecastType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = uiState.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ForecastType.entries.size)
                        ) {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = viewModel::fetchForecastRange,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Get Forecast")
                    }
                    OutlinedButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Filled.Star, contentDescription = "Save as favorite")
                    }
                }
            }
            if (uiState.isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            uiState.errorMessage?.let { message ->
                item {
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
            itemsIndexed(uiState.days) { index, forecast ->
                DayForecastCard(
                    forecast = forecast,
                    expanded = uiState.expandedDayIndex == index,
                    onToggle = { viewModel.toggleDayExpanded(index) },
                    units = settings.units
                )
            }
        }
    }

    if (showSaveDialog) {
        SaveFavoriteDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCurrentAsFavorite(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun SaveFavoriteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Favorite") },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun dayLabel(dateStr: String): String {
    val date = LocalDate.parse(dateStr)
    val today = LocalDate.now()
    val relative = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }
    return "$relative — $dateStr"
}

@Composable
private fun DayForecastCard(forecast: Forecast, expanded: Boolean, onToggle: () -> Unit, units: UnitSystem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = dayLabel(forecast.date), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${forecast.quality.label} — ${(forecast.quality.score * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            if (expanded) {
                ForecastResultSection(forecast, units)
            }
        }
    }
}

@Composable
private fun ForecastResultSection(forecast: Forecast, units: UnitSystem) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = forecast.location.name, style = MaterialTheme.typography.titleLarge)
        Text(text = "${forecast.date} • ${forecast.timezone}", style = MaterialTheme.typography.bodyMedium)

        SunPositionMap(
            latitude = forecast.location.latitude,
            longitude = forecast.location.longitude,
            sunriseAzimuthDegrees = forecast.sunrise?.azimuthDegrees,
            sunsetAzimuthDegrees = forecast.sunset?.azimuthDegrees,
            modifier = Modifier.fillMaxWidth()
        )

        if (forecast.accuracyWarning) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    text = "This forecast is more than 48 hours out — weather conditions may change.",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        QualityBadge(score = forecast.quality.score, level = forecast.quality.level, label = forecast.quality.label, reasoning = forecast.quality.reasoning)

        forecast.sunrise?.let { SunEventCard(title = "Sunrise", event = it) }
        forecast.sunset?.let { SunEventCard(title = "Sunset", event = it) }

        WeatherSummaryCard(forecast.weather, units)

        if (forecast.quality.colorTimeline.isNotEmpty()) {
            Text(text = "Color Timeline", style = MaterialTheme.typography.titleMedium)
            ColorTimelineList(forecast.quality.colorTimeline)
        }
    }
}

@Composable
private fun QualityBadge(score: Double, level: Int, label: String, reasoning: String) {
    val color = when (level) {
        1 -> Color(0xFFFF6B35)
        2 -> Color(0xFFFFA500)
        3 -> Color(0xFFFFD700)
        else -> Color(0xFFB0C4DE)
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.25f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "$label — ${(score * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            Text(text = reasoning, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SunEventCard(title: String, event: SunEvent) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = event.time, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Civil: ${event.twilightCivil}  Nautical: ${event.twilightNautical}  Astro: ${event.twilightAstronomical}", style = MaterialTheme.typography.bodyMedium)
            }
            AzimuthCompass(
                azimuthDegrees = event.azimuthDegrees,
                label = "${event.azimuthDegrees.toInt()}°",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
private fun WeatherSummaryCard(weather: WeatherConditions, units: UnitSystem) {
    Card {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Weather", style = MaterialTheme.typography.titleMedium)
            Text(text = "${weather.conditions.replaceFirstChar { it.uppercase() }} • ${formatTemperature(weather.temperatureC, units)}")
            Text(text = "Cloud cover: ${weather.cloudCoverPercent.toInt()}%  Visibility: ${formatDistance(weather.visibilityKm, units)}")
            Text(text = "Humidity: ${weather.humidityPercent.toInt()}%  Wind: ${formatSpeed(weather.windSpeedMs, units)}")
            Text(
                text = "Clouds by altitude — Low: ${weather.cloudCoverLowPercent.toInt()}%  " +
                    "Mid: ${weather.cloudCoverMidPercent.toInt()}%  High: ${weather.cloudCoverHighPercent.toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatTemperature(celsius: Double, units: UnitSystem): String = when (units) {
    UnitSystem.METRIC -> "%.1f°C".format(celsius)
    UnitSystem.IMPERIAL -> "%.1f°F".format(celsius * 9.0 / 5.0 + 32.0)
}

private fun formatDistance(km: Double, units: UnitSystem): String = when (units) {
    UnitSystem.METRIC -> "%.1f km".format(km)
    UnitSystem.IMPERIAL -> "%.1f mi".format(km * 0.621371)
}

private fun formatSpeed(metersPerSecond: Double, units: UnitSystem): String = when (units) {
    UnitSystem.METRIC -> "%.1f m/s".format(metersPerSecond)
    UnitSystem.IMPERIAL -> "%.1f mph".format(metersPerSecond * 2.23694)
}

@Composable
private fun ColorTimelineList(entries: List<ColorTimelineEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        entries.forEach { entry ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = entry.time)
                Text(text = entry.altitude)
                Text(text = entry.quality, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}
