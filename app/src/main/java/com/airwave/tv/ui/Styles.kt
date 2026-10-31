package com.airwave.tv.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

class CategoryStyle(val emoji: String, start: Long, end: Long) {
    val colors = listOf(Color(start), Color(end))
}

private val categoryStyles = mapOf(
    "News" to CategoryStyle("📰", 0xFF2563EB, 0xFF1E3A8A),
    "Sports" to CategoryStyle("⚽", 0xFF16A34A, 0xFF14532D),
    "Movies" to CategoryStyle("🎬", 0xFFDC2626, 0xFF7F1D1D),
    "Entertainment" to CategoryStyle("🎭", 0xFFDB2777, 0xFF831843),
    "Music" to CategoryStyle("🎵", 0xFF9333EA, 0xFF4C1D95),
    "Kids" to CategoryStyle("🧸", 0xFFF59E0B, 0xFF92400E),
    "Documentary" to CategoryStyle("🌍", 0xFF0D9488, 0xFF134E4A),
    "Series" to CategoryStyle("📺", 0xFF7C3AED, 0xFF3B0764),
    "General" to CategoryStyle("📡", 0xFF475569, 0xFF1E293B),
    "Religious" to CategoryStyle("🕊️", 0xFF0EA5E9, 0xFF0C4A6E),
    "Education" to CategoryStyle("🎓", 0xFF4F46E5, 0xFF312E81),
    "Comedy" to CategoryStyle("😂", 0xFFEAB308, 0xFF854D0E),
    "Culture" to CategoryStyle("🏛️", 0xFFB45309, 0xFF78350F),
    "Lifestyle" to CategoryStyle("🌿", 0xFF65A30D, 0xFF365314),
    "Shop" to CategoryStyle("🛍️", 0xFFE11D48, 0xFF881337),
    "Business" to CategoryStyle("💼", 0xFF0891B2, 0xFF164E63),
    "Outdoor" to CategoryStyle("🏕️", 0xFF15803D, 0xFF052E16),
    "Classic" to CategoryStyle("🎞️", 0xFF78716C, 0xFF292524),
    "Travel" to CategoryStyle("✈️", 0xFF0284C7, 0xFF082F49),
    "Cooking" to CategoryStyle("🍳", 0xFFEA580C, 0xFF7C2D12),
    "Family" to CategoryStyle("🏠", 0xFFF97316, 0xFF7C2D12),
    "Auto" to CategoryStyle("🚗", 0xFF64748B, 0xFF0F172A),
    "Weather" to CategoryStyle("⛅", 0xFF38BDF8, 0xFF0C4A6E),
    "Science" to CategoryStyle("🔬", 0xFF06B6D4, 0xFF164E63),
    "Animation" to CategoryStyle("✨", 0xFFC026D3, 0xFF701A75),
    "Legislative" to CategoryStyle("⚖️", 0xFF57534E, 0xFF1C1917),
    "Public" to CategoryStyle("🏢", 0xFF3B82F6, 0xFF1E3A8A),
    "Relax" to CategoryStyle("🧘", 0xFF14B8A6, 0xFF134E4A),
    "Other" to CategoryStyle("📺", 0xFF374151, 0xFF111827),
)

private val fallbackGradients = listOf(
    0xFF7C5CFF to 0xFF3B1FA8,
    0xFF2EC5FF to 0xFF0B5E8A,
    0xFFFF6B6B to 0xFF9B1C31,
    0xFF22C55E to 0xFF14532D,
    0xFFF59E0B to 0xFF92400E,
    0xFFEC4899 to 0xFF831843,
    0xFF14B8A6 to 0xFF134E4A,
    0xFF6366F1 to 0xFF312E81,
)

fun categoryStyle(name: String): CategoryStyle = categoryStyles[name] ?: run {
    val (a, b) = fallbackGradients[abs(name.hashCode()) % fallbackGradients.size]
    CategoryStyle("📺", a, b)
}

fun gradientFor(seed: String): List<Color> {
    val (a, b) = fallbackGradients[abs(seed.hashCode()) % fallbackGradients.size]
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
