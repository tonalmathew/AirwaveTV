package com.airwave.tv.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Outline globe for the Languages tab (material-icons-core has no language icon). */
val GlobeIcon: ImageVector = ImageVector.Builder(
    name = "Globe",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    fill = null,
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2f,
    strokeLineCap = StrokeCap.Round,
) {
    // Outer circle
    moveTo(12f, 3f)
    arcTo(9f, 9f, 0f, true, true, 12f, 21f)
    arcTo(9f, 9f, 0f, true, true, 12f, 3f)
    // Meridian
    moveTo(12f, 3f)
    arcTo(4f, 9f, 0f, false, true, 12f, 21f)
    arcTo(4f, 9f, 0f, false, true, 12f, 3f)
    // Equator and parallels
    moveTo(3f, 12f)
    lineTo(21f, 12f)
    moveTo(4.6f, 7.5f)
    lineTo(19.4f, 7.5f)
    moveTo(4.6f, 16.5f)
    lineTo(19.4f, 16.5f)
}.build()
