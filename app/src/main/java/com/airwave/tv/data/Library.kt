package com.airwave.tv.data

import java.util.Locale

/** Indexed view over the catalog that the UI browses. Built off the main thread. */
class Library(catalog: Catalog, hideUnreliable: Boolean) {
    private val countries = catalog.countries
    val updatedAt = catalog.updatedAt

    val all: List<Channel> = if (!hideUnreliable) catalog.channels else catalog.channels.mapNotNull { ch ->
        val reliable = ch.streams.filter { !it.geoBlocked && !it.notAlways }
        when {
            reliable.isEmpty() -> null
            reliable.size == ch.streams.size -> ch
            else -> ch.copy(streams = reliable)
        }
    }

    val byKey: Map<String, Channel> = all.associateBy { it.key }

    val byCountry: Map<String, List<Channel>> = all.filter { it.country != null }.groupBy { it.country!! }

    val byCategory: Map<String, List<Channel>> = HashMap<String, MutableList<Channel>>().also { map ->
        all.forEach { ch -> ch.categories.forEach { map.getOrPut(it) { mutableListOf() }.add(ch) } }
    }

    val byLanguage: Map<String, List<Channel>> = HashMap<String, MutableList<Channel>>().also { map ->
        all.forEach { ch -> ch.languages.forEach { map.getOrPut(it) { mutableListOf() }.add(ch) } }
    }

    val languageList: List<Pair<String, Int>> =
        byLanguage.map { it.key to it.value.size }.sortedWith(compareBy({ -it.second }, { it.first }))

    val countryList: List<Pair<Country, Int>> =
        byCountry.map { (code, list) -> country(code) to list.size }.sortedBy { it.first.name }

    val categoryList: List<Pair<String, Int>> =
        byCategory.map { it.key to it.value.size }
            .sortedWith(compareBy({ it.first == OTHER_CATEGORY }, { -it.second }))

    private val searchNames = all.map { normalize(it.name) }

    fun country(code: String): Country = countries[code] ?: Country(
        code = code,
        name = Locale("", code).displayCountry.ifBlank { code },
        flag = flagOf(code),
    )

    fun search(query: String, limit: Int = 300): List<Channel> {
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val prefix = ArrayList<Channel>()
        val contains = ArrayList<Channel>()
        for (i in all.indices) {
            val name = searchNames[i]
            when {
                name.startsWith(q) -> prefix += all[i]
                q in name -> contains += all[i]
            }
        }
        return (rank(prefix) + rank(contains)).take(limit)
    }

    companion object {
        fun normalize(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

        /** Puts channels with logos and reliable streams first; keeps alphabetical order otherwise. */
        fun rank(list: List<Channel>): List<Channel> =
            list.sortedWith(compareBy({ it.logo == null }, { it.geoBlocked }, { it.notAlways }))

        /**
         * Stable re-order: home country + home language first, then home country, then home
         * language, then everything else.
         */
        fun localFirst(list: List<Channel>, country: String, language: String?): List<Channel> =
            list.sortedBy { ch ->
                (if (ch.country == country) 0 else 2) + (if (language != null && language in ch.languages) 0 else 1)
            }

        fun flagOf(code: String): String {
            val iso = if (code == "UK") "GB" else code
            if (iso.length != 2 || !iso.all { it in 'A'..'Z' }) return ""
            return iso.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
        }
    }
}
