package com.airwave.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Country
import com.airwave.tv.data.Library

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
        contentPadding = PaddingValues(start = ScreenPadding, end = 48.dp, bottom = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ScreenHeader(
                title = if (pickMode) "Choose your country" else "Countries",
                subtitle = if (pickMode) {
                    "Your country's channels are shown first on Home"
                } else {
                    "%d countries  ·  %,d channels".format(countries.size, lib.all.size)
                },
            )
        }
        itemsIndexed(countries, key = { _, it -> it.first.code }) { i, (country, count) ->
            CountryTile(
                country = country,
                count = count,
                isHome = country.code == home,
                onClick = { onPick(country.code) },
                modifier = Modifier.restorableFocus(vm, "country/${country.code}", isFirst = i == 0),
            )
        }
    }
}

@Composable
private fun CountryTile(country: Country, count: Int, isHome: Boolean, onClick: () -> Unit, modifier: Modifier) {
    TvCard(onClick = onClick, modifier = modifier.height(96.dp), showBorder = false, focusedScale = 1.06f) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) Color.White else Palette.SurfaceHi)
                .padding(horizontal = 20.dp),
        ) {
            Text(country.flag.ifEmpty { "🏳️" }, fontSize = 38.sp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    country.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (focused) Color.Black else Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (isHome) "★ ${channelCount(count)}" else channelCount(count),
                    fontSize = 14.sp,
                    color = if (focused) Color.Black.copy(alpha = 0.6f) else if (isHome) Palette.Accent2 else Palette.TextDim,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun CategoriesScreen(vm: AppViewModel, lib: Library) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(250.dp),
        state = vm.categoriesGrid,
        contentPadding = PaddingValues(start = ScreenPadding, end = 48.dp, bottom = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth { ScreenHeader("Categories", "Pick a genre to explore channels worldwide") }
        itemsIndexed(lib.categoryList, key = { _, it -> it.first }) { i, (name, count) ->
            val style = categoryStyle(name)
            TvCard(
                onClick = { vm.openCategory(name) },
                modifier = Modifier
                    .height(140.dp)
                    .restorableFocus(vm, "category/$name", isFirst = i == 0),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(style.colors))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(style.emoji, fontSize = 36.sp)
                    Column {
                        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(channelCount(count), fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }
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
            ScreenHeader("Favorites")
            EmptyState(
                emoji = "⭐",
                title = "No favorites yet",
                message = "Hold the OK button on any channel to add it here.",
            )
        }
    } else {
        ChannelGrid(vm, "Favorites", favorites, vm.favoritesGrid, idPrefix = "fav") {
            fullWidth { ScreenHeader("Favorites", "${channelCount(favorites.size)}  ·  Hold OK to remove") }
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
            ScreenHeader("Languages")
            EmptyState("🗣️", "No language data yet", "Refresh the channel list in Settings to download it.")
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
        contentPadding = PaddingValues(start = ScreenPadding, end = 48.dp, bottom = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        fullWidth {
            ScreenHeader(
                title = if (pickMode) "Choose your language" else "Languages",
                subtitle = if (pickMode) {
                    "Channels in your language are shown first on Home and in lists"
                } else {
                    "${languages.size} languages  ·  Most channels first"
                },
            )
        }
        if (pickMode) {
            item(key = "__any__") {
                LanguageTile(
                    name = "Any language",
                    badge = "★",
                    subtitle = if (home == null) "★ Current choice" else "No preference",
                    highlighted = home == null,
                    onClick = { onPick(null) },
                    modifier = Modifier.restorableFocus(vm, "language/__any__", isFirst = true),
                )
            }
        }
        itemsIndexed(languages, key = { _, it -> it.first }) { i, (name, count) ->
            LanguageTile(
                name = name,
                badge = name.take(2),
                subtitle = if (name == home) "★ ${channelCount(count)}" else channelCount(count),
                highlighted = name == home,
                onClick = { onPick(name) },
                modifier = Modifier.restorableFocus(vm, "language/$name", isFirst = !pickMode && i == 0),
            )
        }
    }
}

@Composable
private fun LanguageTile(
    name: String,
    badge: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    TvCard(onClick = onClick, modifier = modifier.height(96.dp), showBorder = false, focusedScale = 1.06f) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .background(if (focused) Color.White else Palette.SurfaceHi)
                .padding(horizontal = 20.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(remember(name) { gradientFor(name) })),
                contentAlignment = Alignment.Center,
            ) {
                Text(badge, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (focused) Color.Black else Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = when {
                        focused -> Color.Black.copy(alpha = 0.6f)
                        highlighted -> Palette.Accent2
                        else -> Palette.TextDim
                    },
                    maxLines = 1,
                )
            }
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
                subtitle = channelCount(channels.size),
                badge = screen.badge,
            )
        }
        screen.filterRows.forEach { row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ScreenPadding, bottom = 10.dp),
            ) {
                Text(row.label, fontSize = 15.sp, color = Palette.TextDim, modifier = Modifier.width(96.dp))
                LazyRow(
                    contentPadding = PaddingValues(end = 48.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(row.filters) { i, f ->
                        TvChip(label = f.label, selected = i == row.selected, onClick = { row.selected = i })
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (channels.isEmpty()) {
            EmptyState("📭", "Nothing here", "Try a different filter.")
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
