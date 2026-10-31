package com.airwave.tv.ui

import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Channel
import com.airwave.tv.data.Library
import kotlinx.coroutines.delay
import java.util.Date

private class HomeRow(val id: String, val title: String, val channels: List<Channel>)

private val homeCategories = listOf("News", "Sports", "Movies", "Entertainment", "Music", "Kids", "Documentary", "Series", "Comedy")

private fun buildRows(
    lib: Library,
    favorites: Set<String>,
    recents: List<String>,
    home: String,
    language: String?,
): List<HomeRow> {
    val rows = mutableListOf<HomeRow>()
    recents.mapNotNull { lib.byKey[it] }.take(20).takeIf { it.isNotEmpty() }?.let {
        rows += HomeRow("recent", "Continue watching", it)
    }
    favorites.mapNotNull { lib.byKey[it] }.sortedBy { it.name.lowercase() }.takeIf { it.isNotEmpty() }?.let {
        rows += HomeRow("favorites", "Your favorites", it)
    }
    lib.byCountry[home]?.let {
        val c = lib.country(home)
        rows += HomeRow("country", "${c.flag}  Channels in ${c.name}", Library.localFirst(Library.rank(it), home, language).take(40))
    }
    language?.let { lib.byLanguage[it] }?.let {
        rows += HomeRow("language", "🗣️  In $language", Library.localFirst(Library.rank(it), home, language).take(40))
    }
    for (cat in homeCategories) {
        val list = lib.byCategory[cat] ?: continue
        rows += HomeRow("cat-$cat", "${categoryStyle(cat).emoji}  $cat", Library.localFirst(Library.rank(list), home, language).take(30))
    }
    return rows
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: AppViewModel, lib: Library) {
    val rows = remember(lib, vm.favorites, vm.recents, vm.homeCountry, vm.homeLanguage) {
        buildRows(lib, vm.favorites, vm.recents, vm.homeCountry, vm.homeLanguage)
    }
    var hero by remember { mutableStateOf<Channel?>(null) }

    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(236.dp),
        ) {
            Crossfade(targetState = hero, label = "hero") { channel ->
                Hero(channel, lib, channel != null && channel.key in vm.favorites)
            }
            Clock(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 48.dp))
        }
        LazyColumn(
            state = vm.homeListState,
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(rows, key = { _, row -> row.id }) { rowIndex, row ->
                // Scroll the whole row (title included) into view when focus enters it.
                val rowView = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                Column(
                    Modifier
                        .bringIntoViewRequester(rowView)
                        .onFocusChanged { if (it.hasFocus) scope.launch { rowView.bringIntoView() } },
                ) {
                    SectionTitle(row.title)
                    LazyRow(
                        state = vm.rowState(row.id),
                        contentPadding = PaddingValues(start = ScreenPadding, end = 48.dp, top = 14.dp, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        itemsIndexed(row.channels, key = { _, ch -> ch.key }) { i, ch ->
                            ChannelCard(
                                channel = ch,
                                favorite = ch.key in vm.favorites,
                                onClick = { vm.play(row.title, row.channels, i) },
                                onLongClick = { vm.toggleFavorite(ch) },
                                onFocused = { hero = ch },
                                modifier = Modifier.width(212.dp),
                                cardModifier = Modifier.restorableFocus(vm, "home/${row.id}/${ch.key}", isFirst = rowIndex == 0 && i == 0),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(channel: Channel?, lib: Library, favorite: Boolean) {
    val modifier = Modifier
        .fillMaxSize()
        .padding(start = ScreenPadding, end = 150.dp, top = 28.dp, bottom = 16.dp)
    if (channel == null) {
        Column(modifier, verticalArrangement = Arrangement.Center) {
            Text("Welcome to Airwave", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(
                "%,d free live channels from %d countries".format(lib.all.size, lib.countryList.size),
                fontSize = 18.sp,
                color = Palette.TextDim,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    } else {
        HeroChannel(channel, lib, favorite, modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroChannel(channel: Channel, lib: Library, favorite: Boolean, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        ChannelLogo(
            channel,
            modifier = Modifier
                .width(272.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp)),
            padding = 22.dp,
            initialsSize = 44.sp,
        )
        Spacer(Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Text(
                channel.name,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                channel.country?.let {
                    val c = lib.country(it)
                    MetaChip("${c.flag}  ${c.name}")
                }
                if (channel.languages.isNotEmpty()) MetaChip("🗣️  ${channel.languages.take(2).joinToString(" · ")}")
                channel.categories.take(2).forEach { MetaChip(it) }
                channel.bestQuality?.let { MetaChip("${it}p") }
                if (channel.geoBlocked) MetaChip("Geo-blocked", Palette.Gold)
                if (favorite) MetaChip("★ Favorite", Palette.Gold)
            }
            Text(
                "OK  Watch     Hold OK  ${if (favorite) "Remove from" else "Add to"} favorites",
                fontSize = 15.sp,
                color = Palette.TextDim,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun Clock(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val format = remember { DateFormat.getTimeFormat(context) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(10_000)
        }
    }
    Text(format.format(Date(now)), fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Palette.TextDim, modifier = modifier)
}
