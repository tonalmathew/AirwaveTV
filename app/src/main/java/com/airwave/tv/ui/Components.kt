package com.airwave.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.airwave.tv.R
import com.airwave.tv.data.Channel
import kotlinx.coroutines.delay

fun KeyEvent.isSelectKey() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

/**
 * Lets an item grab focus when [AppViewModel.focusTarget] points at it (after popping a screen,
 * closing the player or on first launch), and records it as the last focused item.
 */
@Composable
fun Modifier.restorableFocus(vm: AppViewModel, id: String, isFirst: Boolean = false): Modifier {
    val requester = remember { FocusRequester() }
    val target = vm.focusTarget
    if (target != null && (target == id || (isFirst && target == FIRST))) {
        LaunchedEffect(target) {
            repeat(6) {
                if (runCatching { requester.requestFocus() }.isSuccess) {
                    vm.focusTarget = null
                    return@LaunchedEffect
                }
                delay(40)
            }
        }
    }
    return this
        .focusRequester(requester)
        .onFocusChanged { if (it.isFocused) vm.lastFocused = id }
}

/** Base focusable surface: grows and gets a white outline when focused. Hold OK for [onLongClick]. */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    focusedScale: Float = 1.08f,
    showBorder: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "cardScale")
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (focused && showBorder) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .clip(shape)
            .onFocusChanged {
                if (focused != it.isFocused) {
                    focused = it.isFocused
                    onFocusChange(it.isFocused)
                }
            }
            .onPreviewKeyEvent { ev ->
                if (onLongClick == null || !ev.isSelectKey()) return@onPreviewKeyEvent false
                when (ev.type) {
                    KeyEventType.KeyDown -> if (ev.nativeKeyEvent.repeatCount > 0) {
                        if (!longFired) {
                            longFired = true
                            onLongClick()
                        }
                        true
                    } else {
                        longFired = false
                        false
                    }
                    KeyEventType.KeyUp -> if (longFired) {
                        longFired = false
                        true
                    } else false
                    else -> false
                }
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        content(focused)
    }
}

@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    initialsSize: TextUnit = 28.sp,
) {
    var failed by remember(channel.logo) { mutableStateOf(channel.logo == null) }
    Box(modifier.background(Palette.LogoBrush), contentAlignment = Alignment.Center) {
        if (failed) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(remember(channel.name) { gradientFor(channel.name) })),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialsOf(channel.name), fontSize = initialsSize, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        TvCard(
            onClick = onClick,
            onLongClick = onLongClick,
            onFocusChange = {
                focused = it
                if (it) onFocused()
            },
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            ChannelLogo(channel, Modifier.fillMaxSize())
            if (favorite) {
                Icon(
                    Icons.Filled.Star, contentDescription = "Favorite", tint = Palette.Gold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp),
                )
            }
            qualityLabel(channel.bestQuality)?.let {
                Pill(it, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        Text(
            channel.name,
            color = if (focused) Palette.Text else Palette.TextDim,
            fontSize = 15.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp, start = 2.dp, end = 2.dp),
        )
    }
}

/** Grid of channel cards; playing one zaps through [channels] in the player. */
@Composable
fun ChannelGrid(
    vm: AppViewModel,
    title: String,
    channels: List<Channel>,
    state: LazyGridState,
    idPrefix: String,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 196.dp,
    header: (LazyGridScope.() -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minCellWidth),
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(start = ScreenPadding, end = 48.dp, top = 8.dp, bottom = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        header?.invoke(this)
        itemsIndexed(channels, key = { _, ch -> ch.key }) { i, ch ->
            ChannelCard(
                channel = ch,
                favorite = ch.key in vm.favorites,
                onClick = { vm.play(title, channels, i) },
                onLongClick = { vm.toggleFavorite(ch) },
                cardModifier = Modifier.restorableFocus(vm, "$idPrefix/${ch.key}", isFirst = i == 0),
            )
        }
    }
}

fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, badge: String? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 36.dp, bottom = 18.dp),
    ) {
        if (!badge.isNullOrEmpty()) {
            Text(badge, fontSize = 40.sp)
            Spacer(Modifier.size(18.dp))
        }
        Column {
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Palette.Text)
            if (subtitle != null) {
                Text(subtitle, fontSize = 16.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, background: Color = Color.Black.copy(alpha = 0.65f), color: Color = Color.White) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

@Composable
fun MetaChip(text: String, color: Color = Palette.Text) {
    Text(
        text,
        fontSize = 15.sp,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** Pill-shaped filter chip / button that turns white when focused. */
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50), focusedScale = 1.06f, showBorder = false) { focused ->
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = if (focused) Color.Black else Palette.Text,
            modifier = Modifier
                .background(
                    when {
                        focused -> Color.White
                        selected -> Palette.Accent.copy(alpha = 0.55f)
                        else -> Palette.SurfaceHi
                    },
                )
                .padding(horizontal = 18.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun TvButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50), focusedScale = 1.06f, showBorder = false) { focused ->
        Text(
            label,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (focused) Color.Black else Palette.Text,
            modifier = Modifier
                .background(if (focused) Color.White else Palette.SurfaceHi)
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

@Composable
fun AppMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(Palette.AccentBrush),
        contentAlignment = Alignment.Center,
    ) {
        // Same artwork as the launcher icon, with a little padding around the mark.
        Image(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.5f),
        )
    }
}

@Composable
fun EmptyState(emoji: String, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 56.sp)
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
        Text(message, fontSize = 16.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Palette.Text,
        modifier = modifier
            .height(28.dp)
            .padding(start = ScreenPadding),
    )
}
