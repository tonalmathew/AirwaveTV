package com.airwave.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
            ScreenHeader("Search", eyebrow = "Find a channel")
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
                query.isBlank() -> Column(Modifier.padding(start = ScreenPadding, top = 146.dp, end = 48.dp)) {
                    Eyebrow("Popular searches")
                    Spacer(Modifier.height(14.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        suggestions.forEach { s -> TvChip(s, selected = false, onClick = { vm.searchQuery = s.uppercase() }) }
                    }
                    Text(
                        "Search across %,d channels by name.".format(lib.all.size),
                        fontSize = 16.sp,
                        color = Palette.TextDim,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                }
                results.isEmpty() -> EmptyState(Icons.Outlined.SearchOff, "No channels found", "Try a shorter or different name.")
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
                            fontSize = 15.sp,
                            color = Palette.TextDim,
                            modifier = Modifier.padding(top = 140.dp, bottom = 4.dp),
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
            .glass(Shapes.Control, Palette.GlassHi)
            .padding(horizontal = 16.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Palette.TextDim, modifier = Modifier.size(24.dp))
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
            Key("Delete", KeySize * 2 + KeyGap, onBackspace, icon = Icons.Outlined.Backspace)
            Key("Clear", KeySize, onClear, icon = Icons.Outlined.Close)
        }
    }
}

@Composable
private fun Key(label: String, width: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    TvCard(
        onClick = onClick,
        modifier = modifier
            .width(width)
            .height(KeySize),
        shape = RoundedCornerShape(12.dp),
        focusedScale = 1.1f,
        showBorder = false,
    ) { focused ->
        Box(
            Modifier
                .fillMaxSize()
                .then(if (focused) Modifier.background(Color.White) else Modifier.glass(RoundedCornerShape(12.dp))),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = if (focused) Palette.OnFocus else Palette.Text, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    label,
                    fontSize = if (label.length > 1) 15.sp else 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (focused) Palette.OnFocus else Palette.Text,
                )
            }
        }
    }
}