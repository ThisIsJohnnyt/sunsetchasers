package com.sunsetchasers.feature.forecast.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lightweight compass showing where on the horizon the sun rises/sets,
 * standing in for a full map view until a Google Maps API key is configured.
 */
@Composable
fun AzimuthCompass(
    azimuthDegrees: Double,
    label: String,
    modifier: Modifier = Modifier
) {
    val labelColor = MaterialTheme.colorScheme.onSurface
    val ringColor = MaterialTheme.colorScheme.outline
    val needleColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.size(140.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 16.dp.toPx()

        drawCircle(color = ringColor, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))

        val directions = listOf("N" to 0.0, "E" to 90.0, "S" to 180.0, "W" to 270.0)
        directions.forEach { (text, angle) ->
            val rad = Math.toRadians(angle - 90.0)
            val labelRadius = radius + 14.dp.toPx()
            val x = center.x + (labelRadius * cos(rad)).toFloat()
            val y = center.y + (labelRadius * sin(rad)).toFloat()
            drawContext.canvas.nativeCanvas.drawText(
                text,
                x,
                y,
                android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        val needleRad = Math.toRadians(azimuthDegrees - 90.0)
        val tip = Offset(
            center.x + (radius * cos(needleRad)).toFloat(),
            center.y + (radius * sin(needleRad)).toFloat()
        )
        drawLine(color = needleColor, start = center, end = tip, strokeWidth = 4.dp.toPx())
        drawCircle(color = needleColor, radius = 5.dp.toPx(), center = center)
    }
    Text(
        text = label,
        style = TextStyle(fontSize = 12.sp, color = labelColor),
        modifier = Modifier
    )
}
