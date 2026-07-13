package com.sunsetchasers.feature.forecast.map

import android.content.Context
import org.osmdroid.config.Configuration
import java.io.File

/**
 * OpenStreetMap's tile usage policy requires a User-Agent identifying the
 * app (osmdroid refuses tile requests without one), and osmdroid otherwise
 * defaults to external storage for its cache, which needs scoped-storage-safe
 * app-private directories instead. Call once from Application.onCreate().
 */
fun initializeOsmdroid(context: Context) {
    val configuration = Configuration.getInstance()
    configuration.userAgentValue = context.packageName
    configuration.osmdroidBasePath = File(context.filesDir, "osmdroid")
    configuration.osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
}
