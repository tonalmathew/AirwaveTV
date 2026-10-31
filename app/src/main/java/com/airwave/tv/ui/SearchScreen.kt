package com.airwave.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Library

private const val KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
private const val KEYS_PER_ROW = 6
private val KeySize = 50.dp
private val KeyGap = 6.dp

private val suggestions = listOf("News", "Sport", "Music", "Movies", "Kids", "BBC", "CNN", "Al Jazeera", "France 24", "DW")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(vm: AppViewModel, lib: Library) {
    val query = vm.searchQuery
    val results = remember(lib, query) { lib.search(query) }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(ScreenPadding + KeySize * KEYS_PER_ROW + KeyGap * (KEYS_PER_ROW - 1) + 24.dp)
                .fillMaxHeight()
                .padding(start = ScreenPadding),
        ) {
            ScreenHeader("Search")
            QueryBox(query)
            Spacer(Modifier.height(18.dp))
            Keyboard(
                vm = vm,
                onKey = { vm.searchQuery = (query + it).take(40) },
                onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) vm.searchQuery = "$query " },
                onBackspace = { vm.searchQuery = query.dropLast(1) },
                onClear = { vm.searchQuery = "" },
            )
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when {
                query.isBlank() -> Column(Modifier.padding(start = ScreenPadding, top = 118.dp, end = 48.dp)) {
                    Text("Popular searches", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        suggestions.forEach { s -> TvChip(s, selected = false, onClick = { vm.searchQuery = s.uppercase() }) }
                    }
                    Text(
                        "Search across %,d channels by name.".format(lib.all.size),
                        fontSize = 16.sp,
                        color = Palette.TextDim,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
                results.isEmpty() -> EmptyState("🔍", "No channels found", "Try a shorter or different name.")
                else -> ChannelGrid(
                    vm = vm,
                    title = "Search: ${query.trim()}",
                    channels = results,
                    state = vm.searchGrid,
                    idPrefix = "search",
                    minCellWidth = 180.dp,
                ) {
                    fullWidth {
                        Text(
                            "${results.size}${if (results.size >= 300) "+" else ""} results",
                            fontSize = 18.sp,
                            color = Palette.TextDim,
                            modifier = Modifier.padding(top = 110.dp, bottom = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryBox(query: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Palette.SurfaceHi)
            .padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = Palette.TextDim, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (query.isEmpty()) "Channel name" else "$query|",
            fontSize = 20.sp,
            color = if (query.isEmpty()) Palette.TextDim else Palette.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun Keyboard(
    vm: AppViewModel,
    onKey: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        KEYS.chunked(KEYS_PER_ROW).forEachIndexed { row, chars ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                chars.forEachIndexed { col, c ->
                    Key(
                        label = c.toString(),
                        width = KeySize,
                        onClick = { onKey(c) },
                        modifier = Modifier.restorableFocus(vm, "key/$c", isFirst = row == 0 && col == 0),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            Key("Space", KeySize * 3 + KeyGap * 2, onSpace)
            Key("⌫", KeySize * 2 + KeyGap, onBackspace)
            Key("✕", KeySize, onClear)
        }
    }
}

@Composable
private fun Key(label: String, width: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvCard(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(KeySize),
        shape = RoundedCornerShape(10.dp),
        focusedScale = 1.1f,
        showBorder = false,
    ) { focused ->
        Box(
            Modifier
                .fillMaxSize()
                .background(if (focused) Color.White else Palette.SurfaceHi),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                fontSize = if (label.length > 1) 16.sp else 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) Color.Black else Palette.Text,
            )
        }
    }
}
