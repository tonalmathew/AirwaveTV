package com.airwave.tv.ui

import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.airwave.tv.data.OTHER_CATEGORY
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Channel
import com.airwave.tv.data.Library
import kotlinx.coroutines.delay
import java.util.Date

/** Gap above a pinned row: just the blank space between rows, so none of the row above shows. */
private val RowEdge = 12.dp

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
        rows += HomeRow("country", "Popular in ${c.name}", Library.localFirst(Library.rank(it), home, language).take(40))
    }
    language?.let { lib.byLanguage[it] }?.let {
        rows += HomeRow("language", "In $language", Library.localFirst(Library.rank(it), home, language).take(40))
    }
    for (cat in homeCategories) {
        val list = lib.byCategory[cat] ?: continue
        rows += HomeRow("cat-$cat", cat, Library.localFirst(Library.rank(list), home, language).take(30))
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

    // The focused channel's colours wash over the top of the screen, behind everything.
    Crossfade(targetState = hero, animationSpec = tween(600), label = "backdrop") { channel ->
        if (channel != null) HeroBackdrop(channel)
    }
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(224.dp),
        ) {
            Crossfade(targetState = hero, label = "hero") { channel ->
                Hero(channel, lib, channel != null && channel.key in vm.favorites)
            }
            Clock(Modifier.align(Alignment.TopEnd).padding(top = 30.dp, end = 48.dp))
        }
        LazyColumn(
            state = vm.homeListState,
            contentPadding = PaddingValues(top = RowEdge, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .fadingEdges(top = RowEdge),
        ) {
            itemsIndexed(rows, key = { _, row -> row.id }) { rowIndex, row ->
                // Scroll the whole row (title included) into view when focus enters it.
                // Scroll the whole row (title included) into view when focus enters it, clear of the faded top edge.
                val rowView = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                var rowSize by remember { mutableStateOf(IntSize.Zero) }
                val margin = with(LocalDensity.current) { RowEdge.toPx() }
                Column(
                    Modifier
                        .onSizeChanged { rowSize = it }
                        .bringIntoViewRequester(rowView)
                        .onFocusChanged {
                            if (it.hasFocus) {
                                scope.launch {
                                    // Pin the focused row just below the header so the previous row
                                    // scrolls away rather than peeking out half-cut. Wait for the list's
                                    // own focus scrolling to finish first, or it cancels this animation.
                                    delay(60)
                                    runCatching { vm.homeListState.animateScrollToItem(rowIndex, -margin.toInt()) }
                                        .onFailure {
                                            rowView.bringIntoView(Rect(0f, -margin, rowSize.width.toFloat(), rowSize.height.toFloat()))
                                        }
                                }
                            }
                        },
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
            Eyebrow("Airwave TV")
            Text(
                "Free live TV from around the world",
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "%,d channels  ·  %d countries".format(lib.all.size, lib.countryList.size),
                fontSize = 16.sp,
                color = Palette.TextDim,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    } else {
        HeroChannel(channel, lib, favorite, modifier)
    }
}

/**
 * Large blurred colour wash from the channel's logo, fading out down the screen so the rows sit
 * on the ambient background (blur needs Android 12+).
 */
@Composable
private fun HeroBackdrop(channel: Channel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || channel.logo == null) return
    AsyncImage(
        model = channel.logo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .underSideMenu()
            .height(520.dp)
            .graphicsLayer {
                alpha = 0.35f
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(listOf(Color.Black, Color.Transparent)), blendMode = BlendMode.DstIn)
            }
            .blur(110.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroChannel(channel: Channel, lib: Library, favorite: Boolean, modifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        ChannelLogo(
            channel,
            modifier = Modifier
                .width(256.dp)
                .aspectRatio(16f / 9f)
                .clip(Shapes.Tile)
                .border(1.dp, Palette.GlassEdge, Shapes.Tile),
            padding = 22.dp,
            initialsSize = 40.sp,
        )
        Spacer(Modifier.width(32.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow(channel.categories.firstOrNull()?.takeIf { it != OTHER_CATEGORY } ?: "Live channel")
            Text(
                channel.name,
                fontSize = 38.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            val details = listOfNotNull(
                channel.country?.let { lib.country(it).let { c -> "${c.flag}  ${c.name}" } },
                channel.languages.take(2).joinToString(", ").ifEmpty { null },
            ).joinToString("   ·   ")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                if (details.isNotEmpty()) {
                    Text(details, fontSize = 16.sp, color = Palette.TextDim, modifier = Modifier.padding(end = 6.dp))
                }
                channel.bestQuality?.let { MetaChip("${it}p") }
                if (channel.geoBlocked) MetaChip("Geo-blocked", Palette.Gold)
                if (favorite) MetaChip("Favorite")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                KeyHint("OK", "Watch")
                Spacer(Modifier.width(22.dp))
                KeyHint("Hold OK", if (favorite) "Remove from favorites" else "Add to favorites")
            }
        }
    }
}

@Composable
private fun KeyHint(key: String, action: String) {
    Text(
        key,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Palette.Text,
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.3f), Shapes.Badge)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
    Text(action, fontSize = 14.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 8.dp))
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
    Text(format.format(Date(now)), fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Palette.TextDim, modifier = modifier)
}
