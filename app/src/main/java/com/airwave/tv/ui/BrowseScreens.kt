package com.airwave.tv.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.airwave.tv.data.OTHER_CATEGORY
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

/** How many entries the "Popular" section shows. */
private const val PopularCount = 6

/** [home] first (if present), then the entries with the most channels. */
private fun <T> popularOf(list: List<T>, count: (T) -> Int, isHome: (T) -> Boolean): List<T> =
    (list.filter(isHome) + list.sortedByDescending(count)).distinct().take(PopularCount)

@Composable
fun CountriesScreen(
    vm: AppViewModel,
    lib: Library,
    state: LazyGridState,
    pickMode: Boolean,
    onPick: (String) -> Unit,
) {
    val home = vm.homeCountry
    val countries = lib.countryList
    val popular = remember(lib, home) { popularOf(countries, { it.second }, { it.first.code == home }) }
    IndexedGrid(
        vm = vm,
        state = state,
        idPrefix = "country",
        popular = popular,
        all = countries,
        keyOf = { it.first.code },
        nameOf = { it.first.name },
        allTitle = "All countries",
        header = {
            ScreenHeader(
                title = if (pickMode) "Choose your country" else "Countries",
                eyebrow = if (pickMode) "Settings" else "Browse",
                subtitle = if (pickMode) {
                    "Your country's channels are shown first on Home"
                } else {
                    "%d countries  ·  %,d channels".format(countries.size, lib.all.size)
                },
            )
        },
    ) { (country, count), modifier ->
        val isHome = country.code == home
        BrowseTile(
            title = country.name,
            subtitle = if (isHome) "Home  ·  ${channelCount(count)}" else channelCount(count),
            highlighted = isHome,
            onClick = { onPick(country.code) },
            modifier = modifier,
        ) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text(country.flag.ifEmpty { "🏳️" }, fontSize = 30.sp)
            }
        }
    }
}

@Composable
fun CategoriesScreen(vm: AppViewModel, lib: Library) {
    val categories = remember(lib) { lib.categoryList.sortedBy { it.first } }
    val popular = remember(lib) { lib.categoryList.filter { it.first != OTHER_CATEGORY }.take(PopularCount) }
    IndexedGrid(
        vm = vm,
        state = vm.categoriesGrid,
        idPrefix = "category",
        popular = popular,
        all = categories,
        keyOf = { it.first },
        nameOf = { it.first },
        allTitle = "All categories",
        header = { ScreenHeader("Categories", eyebrow = "Browse", subtitle = "${categories.size} genres from around the world") },
    ) { (name, count), modifier ->
        BrowseTile(
            title = name,
            subtitle = channelCount(count),
            onClick = { vm.openCategory(name) },
            modifier = modifier,
        ) { focused -> TileIcon(categoryIcon(name), focused, remember(name) { gradientFor(name) }) }
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
    val languages = remember(lib) { lib.languageList.sortedBy { it.first } }
    val popular = remember(lib, home) { popularOf(lib.languageList, { it.second }, { it.first == home }) }
    IndexedGrid(
        vm = vm,
        state = state,
        idPrefix = "language",
        popular = popular,
        all = languages,
        keyOf = { it.first },
        nameOf = { it.first },
        allTitle = "All languages",
        header = {
            ScreenHeader(
                title = if (pickMode) "Choose your language" else "Languages",
                eyebrow = if (pickMode) "Settings" else "Browse",
                subtitle = if (pickMode) {
                    "Channels in your language are shown first on Home and in lists"
                } else {
                    "${languages.size} languages  ·  %,d channels".format(lib.all.size)
                },
            )
        },
        leading = if (pickMode) {
            { modifier ->
                BrowseTile(
                    title = "Any language",
                    subtitle = if (home == null) "Current choice" else "No preference",
                    highlighted = home == null,
                    onClick = { onPick(null) },
                    modifier = modifier,
                ) { focused -> TileIcon(Icons.Outlined.Language, focused) }
            }
        } else {
            null
        },
    ) { (name, count), modifier ->
        val isHome = name == home
        BrowseTile(
            title = name,
            subtitle = if (isHome) "Home  ·  ${channelCount(count)}" else channelCount(count),
            highlighted = isHome,
            onClick = { onPick(name) },
            modifier = modifier,
        ) { GradientBadge(name.take(2).uppercase(), remember(name) { gradientFor(name) }) }
    }
}
/**
 * Channel list for a country, category or language. The header and filter chips scroll away with
 * the grid, over a soft colour wash taken from the flag (countries) or the item's gradient.
 */
@Composable
fun ChannelListContent(vm: AppViewModel, screen: ChannelListScreen) {
    val selections = screen.filterRows.map { it.selected }
    val channels = remember(screen, selections) {
        screen.channels.filter { ch -> screen.filterRows.all { it.matches(ch) } }
    }
    val stats = remember(screen) { listStats(screen) }
    val activeFilters = screen.filterRows.mapNotNull { row -> row.filters[row.selected].label.takeIf { row.selected != 0 } }

    Box(Modifier.fillMaxSize()) {
        ListBackdrop(screen)
        ChannelGrid(
            vm = vm,
            title = screen.title,
            channels = channels,
            state = screen.gridState,
            idPrefix = "list/${screen.title}",
            minCellWidth = 172.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            fullWidth { ListHeader(screen, stats) }
            screen.filterRows.forEach { row -> fullWidth { FilterBar(row) } }
            fullWidth {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 18.dp, bottom = 2.dp)) {
                    Text(
                        if (activeFilters.isEmpty()) "All channels" else activeFilters.joinToString("  ·  "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "%,d".format(channels.size),
                        fontSize = 15.sp,
                        color = Palette.TextDim,
                        modifier = Modifier.padding(start = 10.dp, bottom = 1.dp),
                    )
                }
            }
            if (channels.isEmpty()) {
                fullWidth { EmptyState(Icons.Outlined.FilterAltOff, "Nothing here", "Try a different filter.", Modifier.padding(bottom = 40.dp)) }
            }
        }
    }
}

/** "788 channels · 22 languages · 25 categories", leaving out the screen's own dimension. */
private fun listStats(screen: ChannelListScreen): String {
    val ch = screen.channels
    val parts = mutableListOf(channelCount(ch.size))
    if (screen.kind != "Country") {
        ch.mapNotNull { it.country }.distinct().size.takeIf { it > 1 }?.let { parts += "$it countries" }
    }
    if (screen.kind != "Language") {
        ch.flatMap { it.languages }.distinct().size.takeIf { it > 1 }?.let { parts += "$it languages" }
    }
    if (screen.kind != "Category") {
        ch.flatMap { it.categories }.filter { it != OTHER_CATEGORY }.distinct().size.takeIf { it > 1 }?.let { parts += "$it categories" }
    }
    return parts.joinToString("  ·  ")
}

@Composable
private fun ListHeader(screen: ChannelListScreen, stats: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 40.dp, bottom = 24.dp)) {
        ListBadge(screen)
        Spacer(Modifier.width(24.dp))
        Column {
            Eyebrow(screen.kind)
            Text(
                screen.title,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(stats, fontSize = 16.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Big flag tile, category icon or language code next to the title. */
@Composable
private fun ListBadge(screen: ChannelListScreen) {
    val size = 96.dp
    when {
        screen.flag != null -> Box(Modifier.size(size).glass(Shapes.Tile, Palette.GlassHi), contentAlignment = Alignment.Center) {
            Text(screen.flag, fontSize = 54.sp)
        }
        screen.kind == "Category" -> Box(
            Modifier
                .size(size)
                .clip(Shapes.Tile)
                .background(Brush.linearGradient(remember(screen.title) { gradientFor(screen.title) })),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryIcon(screen.title), contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
        }
        else -> Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(remember(screen.title) { gradientFor(screen.title) })),
            contentAlignment = Alignment.Center,
        ) {
            Text(screen.title.take(2).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun FilterBar(row: FilterRow) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Eyebrow(row.label, Modifier.width(104.dp))
        LazyRow(
            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(row.filters) { i, f ->
                TvChip(label = f.label, count = f.count, selected = i == row.selected, onClick = { row.selected = i })
            }
        }
    }
}

/** Blurred wash of the flag's (or the item's) colours across the top of the page; needs Android 12+. */
@Composable
private fun ListBackdrop(screen: ChannelListScreen) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    Box(
        Modifier
            .underSideMenu()
            .height(460.dp)
            .graphicsLayer {
                alpha = 0.4f
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)), blendMode = BlendMode.DstIn)
            }
            .blur(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (screen.flag != null) {
            // Stretch the flag emoji across the width so its colours fill the wash.
            Text(
                screen.flag,
                fontSize = 260.sp,
                modifier = Modifier.graphicsLayer {
                    scaleX = 4f
                    scaleY = 1.6f
                },
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(remember(screen.title) { gradientFor(screen.title) })),
            )
        }
    }
}