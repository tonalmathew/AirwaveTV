package com.airwave.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Black UI with glassy, translucent surfaces. */
object Palette {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF121214)
    val SurfaceHi = Color(0xFF1C1C1F)
    val Glass = Color.White.copy(alpha = 0.07f)
    val GlassHi = Color.White.copy(alpha = 0.12f)
    val Hairline = Color.White.copy(alpha = 0.10f)
    val Accent = Color(0xFF7C5CFF)
    val Accent2 = Color(0xFF2EC5FF)
    val Text = Color(0xFFF5F6F8)
    val TextDim = Color(0xFFA3A6B4)
    val TextFaint = Color(0xFF747889)
    val OnFocus = Color(0xFF0B0C10)
    val OnFocusDim = Color(0xFF0B0C10).copy(alpha = 0.6f)
    val Danger = Color(0xFFFF4D5E)
    val Gold = Color(0xFFFFC94D)
    val Live = Color(0xFFFF3B4E)

    val LogoBrush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f)))
    val AccentBrush = Brush.linearGradient(listOf(Accent, Accent2))

    /** Light edge of a glass panel: brighter at the top-left, fading out. */
    val GlassEdge = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f)))
}

object Shapes {
    val Card = RoundedCornerShape(14.dp)
    val Tile = RoundedCornerShape(20.dp)
    val Control = RoundedCornerShape(50)
    val Badge = RoundedCornerShape(6.dp)
}

/** Frosted-glass surface with a light edge. */
fun Modifier.glass(shape: Shape, fill: Color = Palette.Glass): Modifier = this
    .background(fill, shape)
    .border(1.dp, Palette.GlassEdge, shape)

/** The app's backdrop: plain black. */
fun Modifier.ambientBackground(): Modifier = this.background(Palette.Bg)

val NavCollapsedWidth = 84.dp
val NavExpandedWidth = 260.dp
val ScreenPadding = 40.dp

@Composable
fun AirwaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Accent,
            secondary = Palette.Accent2,
            background = Palette.Bg,
            surface = Palette.Surface,
            onBackground = Palette.Text,
            onSurface = Palette.Text,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides Palette.Text, content = content)
    }
}
