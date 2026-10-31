package com.airwave.tv.data

/**
 * Parses the iptv-org M3U playlist. Entries look like:
 *
 *   #EXTINF:-1 tvg-id="BBCNews.uk@SD" tvg-logo="..." group-title="News",BBC News (720p) [Geo-blocked]
 *   #EXTVLCOPT:http-referrer=https://example.com/
 *   https://example.com/stream.m3u8
 *
 * Feeds of the same channel (same tvg-id before '@') are merged into one [Channel].
 */
object M3uParser {
    private val attrRegex = Regex("""([A-Za-z0-9-]+)="([^"]*)"""")
    private val qualityRegex = Regex("""\((\d{3,4})p\)""")
    private val labelRegex = Regex("""\[([^\]]*)]""")
    private val spaces = Regex("""\s+""")

    private class Entry(
        val id: String,
        val name: String,
        val logo: String?,
        val groups: List<String>,
        var userAgent: String?,
        var referrer: String?,
        val quality: Int?,
        val geoBlocked: Boolean,
        val notAlways: Boolean,
    )

    /**
     * Reads the language-grouped playlist (index.language.m3u), where each entry's group-title is a
     * language name. Returns stream URL -> languages.
     */
    fun parseLanguages(text: String): Map<String, List<String>> {
        val result = HashMap<String, MutableList<String>>()
        var language: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> language = attrRegex.findAll(line)
                    .firstOrNull { it.groupValues[1].equals("group-title", ignoreCase = true) }
                    ?.groupValues?.get(2)?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("Undefined", ignoreCase = true) }
                line.isEmpty() || line.startsWith("#") -> Unit
                else -> {
                    language?.let { lang ->
                        val list = result.getOrPut(line) { mutableListOf() }
                        if (lang !in list) list += lang
                    }
                    language = null
                }
            }
        }
        return result
    }

    fun parse(text: String, languagesByUrl: Map<String, List<String>> = emptyMap()): List<Channel> {
        val grouped = LinkedHashMap<String, MutableList<Pair<Entry, String>>>()
        var current: Entry? = null

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTINF", ignoreCase = true) -> current = parseInfo(line)
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = line.substringAfter(':')
                    val value = option.substringAfter('=', "").trim().ifEmpty { null }
                    when (option.substringBefore('=').lowercase()) {
                        "http-user-agent" -> current?.userAgent = value
                        "http-referrer" -> current?.referrer = value
                    }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val entry = current ?: return@forEach
                    current = null
                    if (entry.groups.any { it.equals("XXX", ignoreCase = true) }) return@forEach
                    val key = if (entry.id.isNotEmpty()) {
                        entry.id.substringBefore('@').lowercase()
                    } else {
                        "name:" + entry.name.lowercase()
                    }
                    grouped.getOrPut(key) { mutableListOf() }.add(entry to line)
                }
            }
        }

        return grouped.map { (key, items) ->
            val entries = items.map { it.first }
            val streams = items
                .map { (e, url) -> Stream(url, e.userAgent, e.referrer, e.quality, e.geoBlocked, e.notAlways) }
                .distinctBy { it.url }
                .sortedWith(compareBy({ it.geoBlocked }, { it.notAlways }, { -minOf(it.quality ?: 0, 1080) }))
            Channel(
                key = key,
                name = entries.minBy { it.name.length }.name,
                logo = entries.firstNotNullOfOrNull { it.logo },
                country = entries.first().id.substringBefore('@').substringAfterLast('.', "")
                    .uppercase().takeIf { it.length == 2 },
                categories = entries.flatMap { it.groups }.distinct().ifEmpty { listOf(OTHER_CATEGORY) },
                languages = streams.flatMap { languagesByUrl[it.url].orEmpty() }.distinct(),
                streams = streams,
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun parseInfo(line: String): Entry {
        val attrs = HashMap<String, String>()
        attrRegex.findAll(line).forEach { attrs[it.groupValues[1].lowercase()] = it.groupValues[2] }

        // The title follows the first comma after the attribute list (attributes may contain commas).
        val lastQuote = line.lastIndexOf('"')
        val comma = line.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
        val title = if (comma >= 0) line.substring(comma + 1).trim() else ""

        val labels = labelRegex.findAll(title).map { it.groupValues[1].lowercase() }.toList()
        val clean = title.replace(labelRegex, "").replace(qualityRegex, "").replace(spaces, " ").trim()
        val groups = attrs["group-title"].orEmpty().split(';')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("Undefined", ignoreCase = true) }

        return Entry(
            id = attrs["tvg-id"].orEmpty().trim(),
            name = clean.ifEmpty { title.ifEmpty { "Unknown channel" } },
            logo = attrs["tvg-logo"]?.trim()?.takeIf { it.startsWith("http") },
            groups = groups,
            userAgent = attrs["http-user-agent"]?.takeIf { it.isNotBlank() },
            referrer = attrs["http-referrer"]?.takeIf { it.isNotBlank() },
            quality = qualityRegex.find(title)?.groupValues?.get(1)?.toIntOrNull(),
            geoBlocked = labels.any { "geo" in it },
            notAlways = labels.any { "24/7" in it },
        )
    }
}
