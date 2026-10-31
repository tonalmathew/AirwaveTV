package com.airwave.tv.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Palette {
    val Bg = Color(0xFF0A0D13)
    val Surface = Color(0xFF141922)
    val SurfaceHi = Color(0xFF1E2532)
    val Accent = Color(0xFF7C5CFF)
    val Accent2 = Color(0xFF2EC5FF)
    val Text = Color(0xFFF4F5F7)
    val TextDim = Color(0xFF9BA3B4)
    val Danger = Color(0xFFFF4D5E)
    val Gold = Color(0xFFFFC94D)
    val Live = Color(0xFFFF3B4E)

    val LogoBrush = Brush.linearGradient(listOf(Color(0xFF3A4252), Color(0xFF242A35)))
    val AccentBrush = Brush.linearGradient(listOf(Accent, Accent2))
}

val NavCollapsedWidth = 84.dp
val NavExpandedWidth = 240.dp
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
