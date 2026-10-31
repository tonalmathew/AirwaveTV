package com.airwave.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Library

private val GridPadding = PaddingValues(start = ScreenPadding, end = 48.dp, bottom = 56.dp)

@Composable
fun CountriesScreen(
    vm: AppViewModel,
    lib: Library,
    state: LazyGridState,
    pickMode: Boolean,
    onPick: (String) -> Unit,
) {
    val home = vm.homeCountry
    val countries = remember(lib, home) { lib.countryList.sortedByDescending { it.first.code == home } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(236.dp),
        state = state,
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ScreenHeader(
                title = if (pickMode) "Choose your country" else "Countries",
                eyebrow = if (pickMode) "Settings" else "Browse",
                subtitle = if (pickMode) {
                    "Your country's channels are shown first on Home"
                } else {
                    "%d countries  ·  %,d channels".format(countries.size, lib.all.size)
                },
            )
        }
        itemsIndexed(countries, key = { _, it -> it.first.code }) { i, (country, count) ->
            val isHome = country.code == home
            BrowseTile(
                title = country.name,
                subtitle = if (isHome) "Home  ·  ${channelCount(count)}" else channelCount(count),
                highlighted = isHome,
                onClick = { onPick(country.code) },
                modifier = Modifier.restorableFocus(vm, "country/${country.code}", isFirst = i == 0),
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Text(country.flag.ifEmpty { "🏳️" }, fontSize = 30.sp)
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(vm: AppViewModel, lib: Library) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(236.dp),
        state = vm.categoriesGrid,
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ScreenHeader("Categories", eyebrow = "Browse", subtitle = "${lib.categoryList.size} genres from around the world")
        }
        itemsIndexed(lib.categoryList, key = { _, it -> it.first }) { i, (name, count) ->
            BrowseTile(
                title = name,
                subtitle = channelCount(count),
                onClick = { vm.openCategory(name) },
                modifier = Modifier.restorableFocus(vm, "category/$name", isFirst = i == 0),
            ) { focused -> TileIcon(categoryIcon(name), focused, remember(name) { gradientFor(name) }) }
        }
    }
}

@Composable
fun FavoritesScreen(vm: AppViewModel, lib: Library) {
    val favorites = remember(lib, vm.favorites) {
        vm.favorites.mapNotNull { lib.byKey[it] }.sortedBy { it.name.lowercase() }
    }
    if (favorites.isEmpty()) {
        Column(Modifier.padding(start = ScreenPadding)) {
            ScreenHeader("Favorites", eyebrow = "Library")
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = "No favorites yet",
                message = "Hold OK on any channel to add it here.",
            )
        }
    } else {
        ChannelGrid(vm, "Favorites", favorites, vm.favoritesGrid, idPrefix = "fav") {
            fullWidth {
                ScreenHeader("Favorites", eyebrow = "Library", subtitle = "${channelCount(favorites.size)}  ·  Hold OK to remove")
            }
        }
    }
}

/** Languages tab, or (with [pickMode]) the home-language picker, where null means "any language". */
@Composable
fun LanguagesScreen(
    vm: AppViewModel,
    lib: Library,
    state: LazyGridState,
    pickMode: Boolean,
    onPick: (String?) -> Unit,
) {
    if (lib.languageList.isEmpty()) {
        Column(Modifier.padding(start = ScreenPadding)) {
            ScreenHeader("Languages", eyebrow = "Browse")
            EmptyState(Icons.Outlined.Language, "No language data yet", "Refresh the channel list in Settings to download it.")
        }
    } else {
        LanguageGrid(vm, lib, state, pickMode, onPick)
    }
}

@Composable
private fun LanguageGrid(
    vm: AppViewModel,
    lib: Library,
    state: LazyGridState,
    pickMode: Boolean,
    onPick: (String?) -> Unit,
) {
    val home = vm.homeLanguage
    val languages = remember(lib, home) { lib.languageList.sortedByDescending { it.first == home } }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(236.dp),
        state = state,
        contentPadding = GridPadding,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ScreenHeader(
                title = if (pickMode) "Choose your language" else "Languages",
                eyebrow = if (pickMode) "Settings" else "Browse",
                subtitle = if (pickMode) {
                    "Channels in your language are shown first on Home and in lists"
                } else {
                    "${languages.size} languages  ·  Most channels first"
                },
            )
        }
        if (pickMode) {
            item(key = "__any__") {
                BrowseTile(
                    title = "Any language",
                    subtitle = if (home == null) "Current choice" else "No preference",
                    highlighted = home == null,
                    onClick = { onPick(null) },
                    modifier = Modifier.restorableFocus(vm, "language/__any__", isFirst = true),
                ) { focused -> TileIcon(Icons.Outlined.Language, focused) }
            }
        }
        itemsIndexed(languages, key = { _, it -> it.first }) { i, (name, count) ->
            val isHome = name == home
            BrowseTile(
                title = name,
                subtitle = if (isHome) "Home  ·  ${channelCount(count)}" else channelCount(count),
                highlighted = isHome,
                onClick = { onPick(name) },
                modifier = Modifier.restorableFocus(vm, "language/$name", isFirst = !pickMode && i == 0),
            ) { GradientBadge(name.take(2).uppercase(), remember(name) { gradientFor(name) }) }
        }
    }
}

@Composable
fun ChannelListContent(vm: AppViewModel, screen: ChannelListScreen) {
    val selections = screen.filterRows.map { it.selected }
    val channels = remember(screen, selections) {
        screen.channels.filter { ch -> screen.filterRows.all { it.matches(ch) } }
    }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = ScreenPadding)) {
            ScreenHeader(
                title = screen.title,
                eyebrow = screen.kind,
                flag = screen.flag,
                subtitle = channelCount(channels.size),
            )
        }
        screen.filterRows.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, bottom = 8.dp),
            ) {
                Eyebrow(row.label, Modifier.width(104.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 48.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(row.filters) { i, f ->
                        TvChip(label = f.label, selected = i == row.selected, onClick = { row.selected = i })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (channels.isEmpty()) {
            EmptyState(Icons.Outlined.FilterAltOff, "Nothing here", "Try a different filter.")
        } else {
            ChannelGrid(
                vm = vm,
                title = screen.title,
                channels = channels,
                state = screen.gridState,
                idPrefix = "list/${screen.title}",
                modifier = Modifier.fillMaxHeight(),
            )
        }
    }
}
