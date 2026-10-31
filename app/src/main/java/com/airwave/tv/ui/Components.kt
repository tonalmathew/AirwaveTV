package com.airwave.tv.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
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

/** Base focusable surface: lifts slightly and gets a crisp white outline when focused. Hold OK for [onLongClick]. */
@Composable
fun TvCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.Card,
    focusedScale: Float = 1.06f,
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
            // Focused cards glow softly in the brand colour.
            .then(
                if (focused && showBorder) {
                    Modifier
                        .shadow(28.dp, shape, ambientColor = Palette.Accent, spotColor = Palette.Accent)
                        .border(2.5.dp, Color.White, shape)
                } else {
                    Modifier
                },
            )
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
    padding: Dp = 18.dp,
    initialsSize: TextUnit = 26.sp,
) {
    var failed by remember(channel.logo) { mutableStateOf(channel.logo == null) }
    Box(modifier.background(Palette.LogoBrush), contentAlignment = Alignment.Center) {
        if (failed) {
            Text(
                initialsOf(channel.name),
                fontSize = initialsSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = Palette.TextFaint,
            )
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
        ) { isFocused ->
            ChannelLogo(
                channel,
                Modifier
                    .fillMaxSize()
                    // Solid while focused so the glow behind doesn't show through the glass.
                    .then(if (isFocused) Modifier.background(Palette.SurfaceHi) else Modifier)
                    .border(1.dp, Palette.GlassEdge, Shapes.Card),
            )
            if (favorite) {
                Icon(
                    Icons.Filled.Favorite, contentDescription = "Favorite", tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(16.dp),
                )
            }
            qualityLabel(channel.bestQuality)?.let {
                Badge(it, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
        }
        Text(
            channel.name,
            color = if (focused) Palette.Text else Palette.TextDim,
            fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
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
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
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

/** Widens a full-width backdrop to reach under the collapsed side menu, so it has no edge there. */
fun Modifier.underSideMenu(): Modifier = layout { measurable, constraints ->
    val extra = NavCollapsedWidth.roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}

fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

/** Small spaced-out caps label, used above titles and for filter rows. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = Palette.TextFaint) {
    Text(
        text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp,
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, eyebrow: String? = null, flag: String? = null) {
    Column(Modifier.padding(top = 36.dp, bottom = 20.dp)) {
        if (eyebrow != null) Eyebrow(eyebrow, Modifier.padding(bottom = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (flag != null) {
                Text(flag, fontSize = 30.sp)
                Spacer(Modifier.width(14.dp))
            }
            Text(title, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp, color = Palette.Text)
        }
        if (subtitle != null) {
            Text(subtitle, fontSize = 15.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

/** Tiny label on top of artwork, e.g. "FHD". */
@Composable
fun Badge(text: String, modifier: Modifier = Modifier, background: Color = Color.Black.copy(alpha = 0.72f), color: Color = Color.White) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = color,
        modifier = modifier
            .clip(Shapes.Badge)
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Frosted metadata pill ("India", "1080p"). */
@Composable
fun MetaChip(text: String, color: Color = Palette.Text) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .glass(Shapes.Control, Palette.GlassHi)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    )
}

/** Filter chip: frosted pill, brand gradient when selected, white when focused. */
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    TvCard(onClick = onClick, modifier = modifier, shape = Shapes.Control, focusedScale = 1.06f, showBorder = false) { focused ->
        val fg = when {
            focused -> Palette.OnFocus
            selected -> Color.White
            else -> Palette.Text
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(
                    when {
                        focused -> Modifier.background(Color.White)
                        selected -> Modifier.background(Palette.AccentBrush)
                        else -> Modifier.glass(Shapes.Control)
                    },
                )
                .padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(
                label,
                fontSize = 15.sp,
                fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                color = fg,
            )
            if (count != null) {
                Text(
                    "%,d".format(count),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = fg.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun TvButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TvCard(onClick = onClick, modifier = modifier, shape = Shapes.Control, focusedScale = 1.06f, showBorder = false) { focused ->
        Text(
            label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (focused) Palette.OnFocus else Palette.Text,
            modifier = Modifier
                .then(if (focused) Modifier.background(Color.White) else Modifier.glass(Shapes.Control, Palette.GlassHi))
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

/**
 * Browse tile used for countries, categories and languages: [leading] art, a title and a
 * subtitle on a quiet surface that turns white when focused.
 */
@Composable
fun BrowseTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    leading: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    TvCard(onClick = onClick, modifier = modifier.height(88.dp), shape = Shapes.Tile, showBorder = false, focusedScale = 1.05f) { focused ->
        val bg by animateColorAsState(if (focused) Color.White else Palette.Glass, label = "tileBg")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .then(if (focused) Modifier else Modifier.border(1.dp, Palette.GlassEdge, Shapes.Tile))
                .padding(horizontal = 18.dp),
        ) {
            leading(focused)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (focused) Palette.OnFocus else Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = when {
                        focused -> Palette.OnFocusDim
                        highlighted -> Palette.Accent2
                        else -> Palette.TextDim
                    },
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Rounded-square icon badge for [BrowseTile]: a colour gradient with a white icon, or frosted glass. */
@Composable
fun TileIcon(icon: ImageVector, focused: Boolean, colors: List<Color>? = null) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(48.dp)
            .clip(shape)
            .then(
                when {
                    colors != null -> Modifier.background(Brush.linearGradient(colors))
                    focused -> Modifier.background(Palette.OnFocus.copy(alpha = 0.08f))
                    else -> Modifier.glass(shape, Palette.GlassHi)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (colors == null && focused) Palette.OnFocus else Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Gradient circle with a short label, e.g. a language code. */
@Composable
fun GradientBadge(text: String, colors: List<Color>) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun AppMark(size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
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
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .glass(Shapes.Tile, Palette.GlassHi),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.TextDim, modifier = Modifier.size(32.dp))
        }
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text(message, fontSize = 15.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = Palette.Text,
        modifier = modifier
            .height(26.dp)
            .padding(start = ScreenPadding),
    )
}
