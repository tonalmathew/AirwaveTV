package com.airwave.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Index letter for a name: its first letter without accents ("Åland" → 'A'), or '#'. */
fun letterOf(name: String): Char {
    val c = Normalizer.normalize(name.take(1), Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * Browse grid with a "Popular" section, then every entry A–Z, plus an A–Z rail on the right that
 * jumps through the alphabetical part. [all] must be sorted by [nameOf].
 */
@Composable
fun <T> IndexedGrid(
    vm: AppViewModel,
    state: LazyGridState,
    idPrefix: String,
    popular: List<T>,
    all: List<T>,
    keyOf: (T) -> String,
    nameOf: (T) -> String,
    allTitle: String,
    header: @Composable () -> Unit,
    leading: (@Composable (Modifier) -> Unit)? = null,
    tile: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    // First index in [all] for each letter, in order.
    val sections = remember(all) {
        all.mapIndexed { i, item -> letterOf(nameOf(item)) to i }.distinctBy { it.first }
    }
    // Grid index of all[0]: header, "Popular" title, leading item, popular items, A–Z title.
    val base = 2 + (if (leading != null) 1 else 0) + popular.size + 1
    // Letter of the top visible A–Z row, taken from its last entry: a jump puts the letter's first
    // entry on that row, possibly after the tail of the previous letter.
    val current by remember(all, base) {
        derivedStateOf {
            // Rows that start on screen; a row peeking in above the top edge doesn't count.
            val visible = state.layoutInfo.visibleItemsInfo.filter { it.index >= base && it.offset.y >= 0 }
            val topRow = visible.minOfOrNull { it.row }
            val i = visible.filter { it.row == topRow }.maxOfOrNull { it.index }?.minus(base)
            if (i != null && i in all.indices) letterOf(nameOf(all[i])) else null
        }
    }
    val scope = rememberCoroutineScope()
    val topGap = with(LocalDensity.current) { FocusMargin.toPx() }

    Row(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(236.dp),
            state = state,
            contentPadding = PaddingValues(start = ScreenPadding, end = 16.dp, bottom = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .fadingEdges(),
        ) {
            fullWidth { header() }
            fullWidth { SectionHeading("Popular") }
            if (leading != null) {
                item(key = "lead") { leading(Modifier.restorableFocus(vm, "$idPrefix/__lead__", isFirst = true)) }
            }
            itemsIndexed(popular, key = { _, it -> "pop/${keyOf(it)}" }) { i, item ->
                tile(item, Modifier.restorableFocus(vm, "$idPrefix/pop/${keyOf(item)}", isFirst = leading == null && i == 0))
            }
            fullWidth { SectionHeading(allTitle) }
            itemsIndexed(all, key = { _, it -> "all/${keyOf(it)}" }) { _, item ->
                tile(item, Modifier.restorableFocus(vm, "$idPrefix/${keyOf(item)}"))
            }
        }
        AlphabetRail(
            letters = sections.map { it.first },
            current = current,
            onJump = { c ->
                sections.firstOrNull { it.first == c }?.let { (_, i) ->
                    scope.launch {
                        // Land the letter's row a little below the top edge rather than flush with it.
                        state.scrollToItem(base + i)
                        state.scrollBy(-topGap)
                    }
                }
            },
            onPick = { c ->
                sections.firstOrNull { it.first == c }?.let { (_, i) -> vm.focusTarget = "$idPrefix/${keyOf(all[i])}" }
            },
        )
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

/**
 * Vertical A–Z rail. Moving through letters jumps the list ([onJump]); OK or Left goes to the
 * first entry for that letter ([onPick]). Entering the rail lands on the [current] letter.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AlphabetRail(
    letters: List<Char>,
    current: Char?,
    onJump: (Char) -> Unit,
    onPick: (Char) -> Unit,
) {
    val requesters = remember(letters) { letters.associateWith { FocusRequester() } }
    Box(
        Modifier
            .fillMaxHeight()
            .padding(start = 4.dp, end = 20.dp, top = 32.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .glass(RoundedCornerShape(50), Palette.Glass)
                .padding(vertical = 12.dp)
                .focusProperties {
                    enter = { (current ?: letters.firstOrNull())?.let { requesters[it] } ?: FocusRequester.Default }
                }
                .focusGroup(),
        ) {
            letters.forEach { c ->
                var focused by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(if (focused) 1.35f else 1f, label = "letterScale")
                val bg by animateColorAsState(if (focused) Color.White else Color.Transparent, label = "letterBg")
                Box(
                    Modifier
                        .width(36.dp)
                        .height(17.dp)
                        .focusRequester(requesters.getValue(c))
                        .onFocusChanged {
                            focused = it.isFocused
                            if (it.isFocused) onJump(c)
                        }
                        .onPreviewKeyEvent { ev ->
                            if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionLeft) {
                                onPick(c)
                                true
                            } else {
                                false
                            }
                        }
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onPick(c) },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            c.toString(),
                            fontSize = 11.sp,
                            fontWeight = if (focused || c == current) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                focused -> Palette.OnFocus
                                c == current -> Palette.Accent2
                                else -> Palette.TextDim
                            },
                        )
                    }
                }
            }
        }
    }
}
