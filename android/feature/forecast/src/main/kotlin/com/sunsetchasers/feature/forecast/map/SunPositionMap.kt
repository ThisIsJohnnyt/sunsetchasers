package com.sunsetchasers.feature.forecast.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sunsetchasers.core.designsystem.theme.SunsetOrange
import com.sunsetchasers.core.designsystem.theme.TwilightPurple
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val AZIMUTH_LINE_METERS = 3000.0
private const val EARTH_RADIUS_METERS = 6371000.0

/**
 * A free, no-API-key map (OpenStreetMap tiles via osmdroid) centered on the
 * forecast location, with lines showing which direction the sun rises and/or
 * sets relative to the viewer.
 */
@Composable
fun SunPositionMap(
    latitude: Double,
    longitude: Double,
    sunriseAzimuthDegrees: Double?,
    sunsetAzimuthDegrees: Double?,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val sunriseColor = SunsetOrange.toArgb()
    val sunsetColor = TwilightPurple.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        factory = { context ->
            MapView(context).apply {
                setMultiTouchControls(true)
                controller.setZoom(13.0)
                mapView = this
            }
        },
        update = { view ->
            val center = GeoPoint(latitude, longitude)
            view.controller.setCenter(center)
            view.overlays.clear()

            view.overlays.add(
                Marker(view).apply {
                    position = center
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Your location"
                }
            )
            sunriseAzimuthDegrees?.let { azimuth ->
                view.overlays.add(azimuthLine(center, azimuth, sunriseColor))
            }
            sunsetAzimuthDegrees?.let { azimuth ->
                view.overlays.add(azimuthLine(center, azimuth, sunsetColor))
            }
            view.invalidate()
        }
    )

    DisposableEffect(lifecycleOwner) {
        // Reads mapView's *current* value on each event/dispose rather than
        // capturing a snapshot, so this effect only needs to be set up once
        // (keying on the mutable mapView state itself would re-fire this
        // effect the moment the factory assigns it, spuriously detaching a
        // freshly created MapView before it ever renders anything).
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }
}

private fun azimuthLine(origin: GeoPoint, azimuthDegrees: Double, colorInt: Int): Polyline {
    val bearingRad = Math.toRadians(azimuthDegrees)
    val angularDistance = AZIMUTH_LINE_METERS / EARTH_RADIUS_METERS
    val lat1 = Math.toRadians(origin.latitude)
    val lon1 = Math.toRadians(origin.longitude)

    val lat2 = asin(sin(lat1) * cos(angularDistance) + cos(lat1) * sin(angularDistance) * cos(bearingRad))
    val lon2 = lon1 + atan2(
        sin(bearingRad) * sin(angularDistance) * cos(lat1),
        cos(angularDistance) - sin(lat1) * sin(lat2)
    )

    return Polyline().apply {
        setPoints(listOf(origin, GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))))
        outlinePaint.color = colorInt
        outlinePaint.strokeWidth = 6f
    }
}
