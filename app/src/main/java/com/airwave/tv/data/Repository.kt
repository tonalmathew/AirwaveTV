package com.airwave.tv.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Downloads and caches the iptv-org playlist, and stores user preferences. */
class Repository(context: Context) {
    private val prefs = context.getSharedPreferences("airwave", Context.MODE_PRIVATE)
    private val playlistFile = File(context.filesDir, "playlist.m3u")
    private val countriesFile = File(context.filesDir, "countries.json")
    private val languagesFile = File(context.filesDir, "languages.m3u")

    private val hasCache get() = playlistFile.exists() && playlistFile.length() > 0
    val isStale
        get() = !hasCache || !languagesFile.exists() ||
            System.currentTimeMillis() - playlistFile.lastModified() > STALE_AFTER_MS

    suspend fun loadCached(): Catalog? = withContext(Dispatchers.IO) {
        if (hasCache) runCatching { read() }.getOrNull() else null
    }

    suspend fun refresh(): Catalog = withContext(Dispatchers.IO) {
        download(PLAYLIST_URL, playlistFile)
        runCatching { download(COUNTRIES_URL, countriesFile) }
        runCatching { download(LANGUAGES_URL, languagesFile) }
        read()
    }

    private fun read(): Catalog {
        val languages = if (languagesFile.exists()) M3uParser.parseLanguages(languagesFile.readText()) else emptyMap()
        val channels = M3uParser.parse(playlistFile.readText(), languages)
        val countries = if (countriesFile.exists()) parseCountries(countriesFile.readText()) else emptyMap()
        return Catalog(channels, countries, playlistFile.lastModified())
    }

    private fun parseCountries(json: String): Map<String, Country> = runCatching {
        val array = JSONArray(json)
        buildMap {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val code = o.optString("code").uppercase()
                if (code.isNotEmpty()) put(code, Country(code, o.optString("name", code), o.optString("flag", "")))
            }
        }
    }.getOrDefault(emptyMap())

    private fun download(url: String, target: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "AirwaveTV/1.0 (Android TV)")
        }
        try {
            if (conn.responseCode !in 200..299) throw IOException("Server returned HTTP ${conn.responseCode}")
            val tmp = File(target.parentFile, target.name + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (tmp.length() == 0L) throw IOException("Empty response from server")
            if (!tmp.renameTo(target)) {
                target.delete()
                tmp.renameTo(target)
            }
        } finally {
            conn.disconnect()
        }
    }

    var favorites: Set<String>
        get() = prefs.getStringSet(KEY_FAVORITES, emptySet())!!.toSet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORITES, value).apply()

    var recents: List<String>
        get() = prefs.getString(KEY_RECENTS, "")!!.split('\n').filter { it.isNotEmpty() }
        set(value) = prefs.edit().putString(KEY_RECENTS, value.joinToString("\n")).apply()

    var homeCountry: String?
        get() = prefs.getString(KEY_HOME_COUNTRY, null)
        set(value) = prefs.edit().putString(KEY_HOME_COUNTRY, value).apply()

    /** null = never chosen; "" = "any language". */
    var homeLanguage: String?
        get() = prefs.getString(KEY_HOME_LANGUAGE, null)
        set(value) = prefs.edit().putString(KEY_HOME_LANGUAGE, value).apply()

    var hideUnreliable: Boolean
        get() = prefs.getBoolean(KEY_HIDE_UNRELIABLE, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_UNRELIABLE, value).apply()

    companion object {
        const val PLAYLIST_URL = "https://iptv-org.github.io/iptv/index.m3u"
        const val COUNTRIES_URL = "https://iptv-org.github.io/api/countries.json"
        const val LANGUAGES_URL = "https://iptv-org.github.io/iptv/index.language.m3u"
        private const val STALE_AFTER_MS = 12L * 60 * 60 * 1000

        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENTS = "recents"
        private const val KEY_HOME_COUNTRY = "home_country"
        private const val KEY_HOME_LANGUAGE = "home_language"
        private const val KEY_HIDE_UNRELIABLE = "hide_unreliable"
    }
}
