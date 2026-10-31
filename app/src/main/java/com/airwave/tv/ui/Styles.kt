package com.airwave.tv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs

private val categoryIcons = mapOf(
    "News" to Icons.Outlined.Newspaper,
    "Sports" to Icons.Outlined.SportsSoccer,
    "Movies" to Icons.Outlined.Movie,
    "Entertainment" to Icons.Outlined.TheaterComedy,
    "Music" to Icons.Outlined.MusicNote,
    "Kids" to Icons.Outlined.ChildCare,
    "Documentary" to Icons.Outlined.Public,
    "Series" to Icons.Outlined.Tv,
    "General" to Icons.Outlined.LiveTv,
    "Religious" to Icons.Outlined.AutoStories,
    "Education" to Icons.Outlined.School,
    "Comedy" to Icons.Outlined.SentimentVerySatisfied,
    "Culture" to Icons.Outlined.AccountBalance,
    "Lifestyle" to Icons.Outlined.Spa,
    "Shop" to Icons.Outlined.ShoppingBag,
    "Business" to Icons.Outlined.BusinessCenter,
    "Outdoor" to Icons.Outlined.Landscape,
    "Classic" to Icons.Outlined.MovieFilter,
    "Travel" to Icons.Outlined.Flight,
    "Cooking" to Icons.Outlined.Restaurant,
    "Family" to Icons.Outlined.FamilyRestroom,
    "Auto" to Icons.Outlined.DirectionsCar,
    "Weather" to Icons.Outlined.WbSunny,
    "Science" to Icons.Outlined.Science,
    "Animation" to Icons.Outlined.AutoAwesome,
    "Legislative" to Icons.Outlined.Gavel,
    "Public" to Icons.Outlined.Groups,
    "Relax" to Icons.Outlined.SelfImprovement,
)

fun categoryIcon(name: String): ImageVector = categoryIcons[name] ?: Icons.Outlined.LiveTv

private val categoryColors = mapOf(
    "News" to (0xFF3B82F6 to 0xFF1E40AF),
    "Sports" to (0xFF22C55E to 0xFF15803D),
    "Movies" to (0xFFEF4444 to 0xFF991B1B),
    "Entertainment" to (0xFFEC4899 to 0xFF9D174D),
    "Music" to (0xFFA855F7 to 0xFF6B21A8),
    "Kids" to (0xFFF59E0B to 0xFFB45309),
    "Documentary" to (0xFF14B8A6 to 0xFF0F766E),
    "Series" to (0xFF8B5CF6 to 0xFF5B21B6),
    "General" to (0xFF64748B to 0xFF334155),
    "Religious" to (0xFF38BDF8 to 0xFF0369A1),
    "Education" to (0xFF6366F1 to 0xFF3730A3),
    "Comedy" to (0xFFEAB308 to 0xFFA16207),
    "Culture" to (0xFFD97706 to 0xFF92400E),
    "Lifestyle" to (0xFF84CC16 to 0xFF4D7C0F),
    "Shop" to (0xFFF43F5E to 0xFF9F1239),
    "Business" to (0xFF06B6D4 to 0xFF0E7490),
)

private val fallbackColors = listOf(
    0xFF7C5CFF to 0xFF4C1DCC,
    0xFF2EC5FF to 0xFF0B6E9E,
    0xFFFF6B8B to 0xFFB0244A,
    0xFF34D399 to 0xFF0F7A55,
    0xFFFBBF24 to 0xFFB86E0B,
    0xFFF472B6 to 0xFFA3245F,
    0xFF2DD4BF to 0xFF0F766E,
    0xFF818CF8 to 0xFF3F45C9,
)

/** Two-stop gradient for a category, or a stable one picked from [seed] (languages, initials). */
fun gradientFor(seed: String): List<Color> {
    val (a, b) = categoryColors[seed] ?: fallbackColors[abs(seed.hashCode()) % fallbackColors.size]
    return listOf(Color(a), Color(b))
}

fun initialsOf(name: String): String {
    val words = name.split(' ', '-', '.').filter { it.isNotBlank() && it[0].isLetterOrDigit() }
    return when {
        words.isEmpty() -> "TV"
        words.size == 1 -> words[0].take(2)
        else -> "${words[0][0]}${words[1][0]}"
    }.uppercase()
}

fun channelCount(n: Int): String = if (n == 1) "1 channel" else "%,d channels".format(n)

fun qualityLabel(q: Int?): String? = when {
    q == null -> null
    q >= 2160 -> "4K"
    q >= 1080 -> "FHD"
    q >= 720 -> "HD"
    else -> null
}
