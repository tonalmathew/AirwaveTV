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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val DIGITS = "1234567890"
private const val KEYS_PER_ROW = 7
private val KeySize = 48.dp
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
                .width(ScreenPadding + keyWidth(KEYS_PER_ROW) + 24.dp)
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
                    minCellWidth = 160.dp,
                ) {
                    fullWidth {
                        Text(
                            when {
                                results.size == 1 -> "1 result"
                                results.size >= 300 -> "300+ results"
                                else -> "${results.size} results"
                            },
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

/** Letters by default; the "123" key swaps the grid for digits (search ignores symbols). */
@Composable
private fun Keyboard(
    vm: AppViewModel,
    onKey: (Char) -> Unit,
    onSpace: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
) {
    var numbers by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
        (if (numbers) DIGITS else LETTERS).chunked(KEYS_PER_ROW).forEachIndexed { row, chars ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                chars.forEachIndexed { col, c ->
                    Key(
                        label = c.toString(),
                        width = keyWidth(1),
                        onClick = { onKey(c) },
                        modifier = Modifier.restorableFocus(vm, "key/$c", isFirst = row == 0 && col == 0),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            Key(
                label = if (numbers) "ABC" else "123",
                width = keyWidth(2),
                onClick = {
                    numbers = !numbers
                    vm.focusTarget = "key/mode"
                },
                modifier = Modifier.restorableFocus(vm, "key/mode"),
                accent = true,
            )
            Key("Space", keyWidth(3), onSpace, modifier = Modifier.restorableFocus(vm, "key/space"))
            Key("Delete", keyWidth(1), onBackspace, modifier = Modifier.restorableFocus(vm, "key/delete"), icon = Icons.Outlined.Backspace)
            Key("Clear", keyWidth(1), onClear, modifier = Modifier.restorableFocus(vm, "key/clear"), icon = Icons.Outlined.Close)
        }
    }
}

private fun keyWidth(units: Int): Dp = KeySize * units + KeyGap * (units - 1)

@Composable
private fun Key(
    label: String,
    width: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
) {
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
                .then(
                    when {
                        focused -> Modifier.background(Color.White)
                        accent -> Modifier.glass(RoundedCornerShape(12.dp), Palette.GlassHi)
                        else -> Modifier.glass(RoundedCornerShape(12.dp))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = if (focused) Palette.OnFocus else Palette.Text, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    label,
                    fontSize = if (label.length > 1) 15.sp else 20.sp,
                    fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (focused) Palette.OnFocus else Palette.Text,
                )
            }
        }
    }
}