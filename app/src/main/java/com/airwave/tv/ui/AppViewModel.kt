package com.airwave.tv.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airwave.tv.data.Catalog
import com.airwave.tv.data.Channel
import com.airwave.tv.data.Library
import com.airwave.tv.data.OTHER_CATEGORY
import com.airwave.tv.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Focus target meaning "the first item of whatever screen is showing". */
const val FIRST = "__first__"

/** Side-menu tabs: outlined icon normally, filled when it's the current tab. */
enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Filled.Home),
    Search("Search", Icons.Outlined.Search, Icons.Filled.Search),
    Countries("Countries", Icons.Outlined.Place, Icons.Filled.Place),
    Categories("Categories", Icons.Outlined.GridView, Icons.Filled.GridView),
    Languages("Languages", Icons.Outlined.Language, Icons.Filled.Language),
    Favorites("Favorites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Settings("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

sealed interface LoadState {
    data object Loading : LoadState
    data class Failed(val message: String) : LoadState
    data object Ready : LoadState
}

/** A filter chip; [count] is how many of the screen's channels it matches. */
class ChannelFilter(val label: String, val count: Int, val test: (Channel) -> Boolean)

/** One row of filter chips ("All" first). Rows on a screen combine. */
class FilterRow(val label: String, val filters: List<ChannelFilter>) {
    var selected by mutableIntStateOf(0)
    fun matches(channel: Channel) = filters[selected].test(channel)
}

/** Screens pushed on top of a tab. [returnFocus] is the item to refocus when popping back. */
sealed interface Screen {
    val returnFocus: String?
}

/** [kind] is the small label above the title ("Country"…); [flag] is shown for countries. */
class ChannelListScreen(
    val title: String,
    val kind: String,
    val flag: String?,
    val channels: List<Channel>,
    val filterRows: List<FilterRow>,
    override val returnFocus: String?,
) : Screen {
    val gridState = LazyGridState()
}

class CountryPickerScreen(override val returnFocus: String?) : Screen {
    val gridState = LazyGridState()
}

class LanguagePickerScreen(override val returnFocus: String?) : Screen {
    val gridState = LazyGridState()
}

class PlayerSession(val title: String, val channels: List<Channel>, val startIndex: Int)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private var catalog: Catalog? = null

    var loadState by mutableStateOf<LoadState>(LoadState.Loading)
        private set
    var library by mutableStateOf<Library?>(null)
        private set
    var refreshing by mutableStateOf(false)
        private set

    var favorites by mutableStateOf(repo.favorites)
        private set
    var recents by mutableStateOf(repo.recents)
        private set
    var homeCountry by mutableStateOf(repo.homeCountry ?: defaultCountry())
        private set
    var homeLanguage by mutableStateOf(repo.homeLanguage.let { if (it == null) defaultLanguage() else it.ifEmpty { null } })
        private set
    var hideUnreliable by mutableStateOf(repo.hideUnreliable)
        private set

    var tab by mutableStateOf(Tab.Home)
        private set
    val stack = mutableStateListOf<Screen>()
    var player by mutableStateOf<PlayerSession?>(null)
        private set

    // Focus restoration: the item id that should grab focus when it is next composed.
    var focusTarget by mutableStateOf<String?>(FIRST)
    var lastFocused: String? = null
    var uiHasFocus = false

    // Scroll positions survive tab switches and the player.
    val homeListState = LazyListState()
    private val rowStates = HashMap<String, LazyListState>()
    fun rowState(id: String) = rowStates.getOrPut(id) { LazyListState() }
    val countriesGrid = LazyGridState()
    val categoriesGrid = LazyGridState()
    val languagesGrid = LazyGridState()
    val favoritesGrid = LazyGridState()
    val searchGrid = LazyGridState()
    var searchQuery by mutableStateOf("")

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            if (library == null) loadState = LoadState.Loading
            val cached = repo.loadCached()
            if (cached != null) apply(cached)
            if (cached == null || repo.isStale) {
                refreshing = true
                try {
                    apply(repo.refresh())
                } catch (e: Exception) {
                    if (library == null) loadState = LoadState.Failed(e.message ?: "Network error")
                } finally {
                    refreshing = false
                }
            }
        }
    }

    fun refresh() {
        if (refreshing) return
        viewModelScope.launch {
            refreshing = true
            try {
                apply(repo.refresh())
                toast("Channel list updated")
            } catch (e: Exception) {
                toast("Couldn't update the channel list: ${e.message ?: "network error"}")
            } finally {
                refreshing = false
            }
        }
    }

    private suspend fun apply(c: Catalog) {
        catalog = c
        library = withContext(Dispatchers.Default) { Library(c, hideUnreliable) }
        loadState = LoadState.Ready
    }

    // Navigation

    fun selectTab(t: Tab) {
        if (t != tab) {
            tab = t
            stack.clear()
        }
    }

    private fun push(screen: Screen) {
        stack.add(screen)
        focusTarget = FIRST
    }

    fun pop() {
        val screen = stack.removeLastOrNull() ?: return
        focusTarget = screen.returnFocus ?: FIRST
    }

    fun openCountry(code: String) {
        val lib = library ?: return
        val country = lib.country(code)
        val channels = Library.rank(lib.byCountry[code].orEmpty())
        val rows = listOfNotNull(categoryRow(channels), languageRow(channels))
        push(ChannelListScreen(country.name, "Country", country.flag.ifEmpty { null }, channels, rows, lastFocused))
    }

    fun openCategory(name: String) {
        val lib = library ?: return
        val channels = localFirst(Library.rank(lib.byCategory[name].orEmpty()))
        val rows = listOfNotNull(countryRow(lib, channels), languageRow(channels))
        push(ChannelListScreen(name, "Category", null, channels, rows, lastFocused))
    }

    fun openLanguage(name: String) {
        val lib = library ?: return
        val channels = localFirst(Library.rank(lib.byLanguage[name].orEmpty()))
        val rows = listOfNotNull(categoryRow(channels), countryRow(lib, channels))
        push(ChannelListScreen(name, "Language", null, channels, rows, lastFocused))
    }

    private fun localFirst(ranked: List<Channel>) = Library.localFirst(ranked, homeCountry, homeLanguage)

    /** Builds a chip row from the most common [values] in [channels]; null if there's nothing to choose. */
    private fun filterRow(
        label: String,
        channels: List<Channel>,
        values: (Channel) -> List<String>,
        pinned: String? = null,
        chipLabel: (String) -> String = { it },
    ): FilterRow? {
        val counts = channels.flatMap(values).groupingBy { it }.eachCount()
        if (counts.size < 2) return null
        val top = counts.entries.sortedByDescending { it.value }.map { it.key }.take(12)
            .let { if (pinned != null && pinned in counts) listOf(pinned) + (it - pinned) else it }
        val filters = listOf(ChannelFilter("All", channels.size) { true }) +
            top.map { value -> ChannelFilter(chipLabel(value), counts.getValue(value)) { value in values(it) } }
        return FilterRow(label, filters)
    }

    private fun categoryRow(channels: List<Channel>) = filterRow(
        "Category", channels, { ch -> ch.categories.filter { it != OTHER_CATEGORY } },
    )

    private fun languageRow(channels: List<Channel>) =
        filterRow("Language", channels, { it.languages }, pinned = homeLanguage)

    private fun countryRow(lib: Library, channels: List<Channel>) = filterRow(
        "Country", channels, { listOfNotNull(it.country) }, pinned = homeCountry,
    ) { code -> lib.country(code).let { "${it.flag}  ${it.name}" } }

    fun openCountryPicker() = push(CountryPickerScreen(lastFocused))

    fun openLanguagePicker() = push(LanguagePickerScreen(lastFocused))

    fun play(title: String, channels: List<Channel>, index: Int) {
        if (channels.isEmpty()) return
        player = PlayerSession(title, channels, index)
        markRecent(channels[index])
    }

    fun closePlayer() {
        player = null
        focusTarget = lastFocused ?: FIRST
    }

    // Preferences

    fun markRecent(channel: Channel) {
        recents = (listOf(channel.key) + recents.filter { it != channel.key }).take(30)
        repo.recents = recents
    }

    fun toggleFavorite(channel: Channel) {
        val adding = channel.key !in favorites
        favorites = if (adding) favorites + channel.key else favorites - channel.key
        repo.favorites = favorites
        toast(if (adding) "★  Added ${channel.name} to favorites" else "Removed ${channel.name} from favorites")
    }

    fun setHome(code: String) {
        homeCountry = code
        repo.homeCountry = code
    }

    fun chooseHomeLanguage(name: String?) {
        homeLanguage = name
        repo.homeLanguage = name ?: ""
    }

    fun updateHideUnreliable(value: Boolean) {
        hideUnreliable = value
        repo.hideUnreliable = value
        val c = catalog ?: return
        viewModelScope.launch {
            library = withContext(Dispatchers.Default) { Library(c, value) }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    /** The TV's UI language as an English name ("Hindi"), matching iptv-org's language names. */
    private fun defaultLanguage(): String? =
        Locale.getDefault().getDisplayLanguage(Locale.ENGLISH).takeIf { it.isNotBlank() }

    private fun defaultCountry(): String {
        val code = Locale.getDefault().country.uppercase()
        return when {
            code == "GB" -> "UK"
            code.length == 2 -> code
            else -> "US"
        }
    }
}
