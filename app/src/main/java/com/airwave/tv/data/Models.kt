package com.airwave.tv.data

const val OTHER_CATEGORY = "Other"

data class Stream(
    val url: String,
    val userAgent: String?,
    val referrer: String?,
    val quality: Int?,
    val geoBlocked: Boolean,
    val notAlways: Boolean,
)

/** One channel, possibly with several alternative streams (SD/HD feeds, mirrors). */
data class Channel(
    val key: String,
    val name: String,
    val logo: String?,
    val country: String?,
    val categories: List<String>,
    val languages: List<String>,
    val streams: List<Stream>,
) {
    val bestQuality: Int? = streams.mapNotNull { it.quality }.maxOrNull()
    val geoBlocked: Boolean = streams.all { it.geoBlocked }
    val notAlways: Boolean = streams.all { it.notAlways }
}

data class Country(val code: String, val name: String, val flag: String)

data class Catalog(
    val channels: List<Channel>,
    val countries: Map<String, Country>,
    val updatedAt: Long,
)
