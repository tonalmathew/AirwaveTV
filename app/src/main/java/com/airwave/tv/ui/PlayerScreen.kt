package com.airwave.tv.ui

import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.upstream.ParsingLoadable
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.airwave.tv.data.Channel
import com.airwave.tv.data.Stream
import kotlinx.coroutines.delay
import kotlin.math.max

private enum class PlayStatus { Loading, Playing, Error }

private const val DEFAULT_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 12; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
private const val LOAD_TIMEOUT_MS = 20_000L
private val progressiveExtensions = listOf(".mp4", ".ts", ".mkv", ".webm", ".flv", ".mov", ".mp3", ".aac")

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(vm: AppViewModel, session: PlayerSession) {
    val context = LocalContext.current
    val channels = session.channels

    var index by remember { mutableIntStateOf(session.startIndex) }
    var streamIndex by remember { mutableIntStateOf(0) }
    // Retry the same URL with the other container guess (HLS vs progressive) before moving on.
    var altFormat by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf(PlayStatus.Loading) }
    var errorText by remember { mutableStateOf("") }
    var videoHeight by remember { mutableIntStateOf(0) }
    var overlayVisible by remember { mutableStateOf(true) }
    var overlayPing by remember { mutableIntStateOf(0) }
    var listOpen by remember { mutableStateOf(false) }
    var centerDown by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }

    val channel = channels[index]
    val exo = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { exo.release() } }

    fun showOverlay() {
        overlayVisible = true
        overlayPing++
    }

    fun zap(to: Int) {
        index = (to + channels.size) % channels.size
        streamIndex = 0
        altFormat = false
        videoHeight = 0
        attempt++
        showOverlay()
        vm.markRecent(channels[index])
    }

    fun retry() {
        streamIndex = 0
        altFormat = false
        attempt++
        showOverlay()
    }

    fun fail(message: String) {
        val ch = channels[index]
        when {
            !altFormat -> altFormat = true
            streamIndex + 1 < ch.streams.size -> {
                streamIndex++
                altFormat = false
            }
            else -> {
                exo.stop()
                errorText = if (ch.geoBlocked) "This channel is geo-blocked and may not be available in your region." else message
                status = PlayStatus.Error
                showOverlay()
                return
            }
        }
        attempt++
    }

    DisposableEffect(exo) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> status = PlayStatus.Playing
                    Player.STATE_BUFFERING -> if (status == PlayStatus.Playing) status = PlayStatus.Loading
                    Player.STATE_ENDED -> fail("The broadcast has ended.")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exo.seekToDefaultPosition()
                    exo.prepare()
                    return
                }
                fail(describe(error))
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoHeight = videoSize.height
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener) }
    }

    // (Re)start playback whenever the channel, stream or format guess changes.
    LaunchedEffect(attempt) {
        status = PlayStatus.Loading
        if (attempt > 0) delay(250) // debounce fast channel zapping
        val stream = channels[index].streams.getOrNull(streamIndex) ?: return@LaunchedEffect
        exo.setMediaSource(buildSource(stream, altFormat))
        exo.prepare()
        exo.playWhenReady = true
    }

    LaunchedEffect(attempt, status) {
        if (status == PlayStatus.Loading) {
            delay(LOAD_TIMEOUT_MS)
            if (status == PlayStatus.Loading) fail("The stream took too long to respond.")
        }
    }

    LaunchedEffect(overlayPing, status) {
        if (status == PlayStatus.Playing) {
            delay(5_000)
            overlayVisible = false
        }
    }

    fun handleKey(ev: KeyEvent): Boolean {
        val down = ev.type == KeyEventType.KeyDown
        when (ev.key) {
            Key.DirectionUp, Key.ChannelDown -> if (down) zap(index - 1)
            Key.DirectionDown, Key.ChannelUp -> if (down) zap(index + 1)
            Key.DirectionLeft -> if (down) listOpen = true
            Key.DirectionRight, Key.Info -> if (down) showOverlay()
            Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> if (down) {
                exo.playWhenReady = !exo.playWhenReady
                showOverlay()
            }
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                if (down) {
                    if (ev.nativeKeyEvent.repeatCount == 0) {
                        centerDown = true
                        longFired = false
                    } else if (centerDown && !longFired) {
                        longFired = true
                        vm.toggleFavorite(channels[index])
                        showOverlay()
                    }
                } else {
                    if (centerDown && !longFired) {
                        if (status == PlayStatus.Error) retry() else listOpen = true
                    }
                    centerDown = false
                    longFired = false
                }
            }
            else -> return false
        }
        return true
    }

    val rootFocus = remember { FocusRequester() }
    LaunchedEffect(listOpen) {
        if (!listOpen) runCatching { rootFocus.requestFocus() }
    }

    BackHandler {
        if (listOpen) listOpen = false else vm.closePlayer()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocus)
            .onKeyEvent { ev -> if (listOpen) false else handleKey(ev) }
            .focusable(),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = exo
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    isFocusable = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!listOpen) when (status) {
            PlayStatus.Loading -> Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(52.dp))
                Text("Tuning in…", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(top = 18.dp))
            }
            PlayStatus.Error -> ErrorCard(channel, errorText, Modifier.align(Alignment.Center).offset(y = (-70).dp))
            PlayStatus.Playing -> Unit
        }

        AnimatedVisibility(
            visible = (overlayVisible || status != PlayStatus.Playing) && !listOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            InfoOverlay(
                channel = channel,
                number = index + 1,
                total = channels.size,
                listTitle = session.title,
                country = channel.country?.let { vm.library?.country(it) },
                videoHeight = videoHeight,
                favorite = channel.key in vm.favorites,
            )
        }

        AnimatedVisibility(
            visible = listOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
        ) {
            ChannelListPanel(
                title = session.title,
                channels = channels,
                current = index,
                favorites = vm.favorites,
                onPick = { i ->
                    listOpen = false
                    if (i != index) zap(i)
                },
                onClose = { listOpen = false },
            )
        }
    }
}

@OptIn(UnstableApi::class)
private fun buildSource(stream: Stream, altFormat: Boolean): MediaSource {
    val headers = buildMap {
        stream.referrer?.let { ref ->
            put("Referer", ref)
            val uri = Uri.parse(ref)
            if (uri.scheme != null && uri.host != null) put("Origin", "${uri.scheme}://${uri.host}")
        }
    }
    val http = DefaultHttpDataSource.Factory()
        .setUserAgent(stream.userAgent ?: DEFAULT_USER_AGENT)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(12_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(headers)
    val mimeType = mimeTypeFor(stream.url, altFormat)
    val item = MediaItem.Builder()
        .setUri(stream.url)
        .setMimeType(mimeType)
        .build()
    return if (mimeType == MimeTypes.APPLICATION_M3U8) {
        HlsMediaSource.Factory(http)
            .setPlaylistParserFactory(LenientHlsPlaylistParserFactory())
            .createMediaSource(item)
    } else {
        DefaultMediaSourceFactory(http).createMediaSource(item)
    }
}

private val isoDateTime = Regex("""^#EXT-X-PROGRAM-DATE-TIME:\d{4}-\d{2}-\d{2}[Tt ]\d{2}:\d{2}""")

/**
 * Some IPTV servers emit non-ISO EXT-X-PROGRAM-DATE-TIME values, which makes ExoPlayer reject the
 * whole playlist. Other players ignore them, so drop the malformed tags before parsing.
 */
@OptIn(UnstableApi::class)
private class LenientHlsPlaylistParserFactory : HlsPlaylistParserFactory {
    private val base = DefaultHlsPlaylistParserFactory()

    override fun createPlaylistParser() = lenient(base.createPlaylistParser())

    override fun createPlaylistParser(
        multivariantPlaylist: HlsMultivariantPlaylist,
        previousMediaPlaylist: HlsMediaPlaylist?,
    ) = lenient(base.createPlaylistParser(multivariantPlaylist, previousMediaPlaylist))

    private fun lenient(parser: ParsingLoadable.Parser<HlsPlaylist>) = ParsingLoadable.Parser { uri, input ->
        val text = input.readBytes().toString(Charsets.UTF_8)
        val cleaned = if ("#EXT-X-PROGRAM-DATE-TIME" !in text) text else {
            text.lineSequence()
                .filterNot { it.startsWith("#EXT-X-PROGRAM-DATE-TIME") && !isoDateTime.containsMatchIn(it) }
                .joinToString("\n")
        }
        parser.parse(uri, cleaned.byteInputStream())
    }
}

/** The second attempt ([altFormat]) retries known formats as-is and flips the guess for unknown URLs. */
private fun mimeTypeFor(url: String, altFormat: Boolean): String? {
    val path = (Uri.parse(url).path ?: "").lowercase()
    return when {
        path.endsWith(".m3u8") || path.endsWith(".m3u") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
        progressiveExtensions.any { path.endsWith(it) } -> if (altFormat) MimeTypes.APPLICATION_M3U8 else null
        // Most IPTV streams are HLS even behind .php URLs; otherwise try a raw MPEG-TS stream.
        else -> if (altFormat) MimeTypes.VIDEO_MP2T else MimeTypes.APPLICATION_M3U8
    }
}

@OptIn(UnstableApi::class)
private fun describe(e: PlaybackException): String {
    val cause = e.cause
    if (cause is HttpDataSource.InvalidResponseCodeException) {
        return when (cause.responseCode) {
            401, 403, 451 -> "This channel isn't available in your region."
            404, 410 -> "This stream is offline right now."
            else -> "The stream server returned an error (${cause.responseCode})."
        }
    }
    return when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "Couldn't reach the stream server."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        -> "This stream's format isn't supported."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> "Your TV can't decode this stream."
        else -> "This stream can't be played right now."
    }
}

@Composable
private fun ErrorCard(channel: Channel, message: String, modifier: Modifier) {
    Column(
        modifier
            .width(600.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface.copy(alpha = 0.96f))
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.Danger, modifier = Modifier.size(44.dp))
        Text(
            "Can't play ${channel.name}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            message,
            fontSize = 17.sp,
            color = Palette.TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 24.dp)) {
            KeyHint("OK", "Try again")
            KeyHint("▲ ▼", "Other channel")
            KeyHint("◀", "Channel list")
        }
    }
}

@Composable
private fun InfoOverlay(
    channel: Channel,
    number: Int,
    total: Int,
    listTitle: String,
    country: com.airwave.tv.data.Country?,
    videoHeight: Int,
    favorite: Boolean,
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(300.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f)))),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 56.dp, end = 56.dp, bottom = 44.dp),
        ) {
            ChannelLogo(
                channel,
                modifier = Modifier
                    .width(184.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
                padding = 14.dp,
                initialsSize = 32.sp,
            )
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Palette.Live)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                        )
                        Text("LIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(start = 6.dp))
                    }
                    Text(
                        "$number / $total  ·  $listTitle",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    channel.name,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                    country?.let { MetaChip("${it.flag}  ${it.name}") }
                    channel.languages.firstOrNull()?.let { MetaChip("🗣️  $it") }
                    channel.categories.take(1).forEach { MetaChip(it) }
                    if (videoHeight > 0) MetaChip("${videoHeight}p")
                    if (favorite) MetaChip("★ Favorite", Palette.Gold)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KeyHint("▲ ▼", "Switch channel")
                KeyHint("OK", "Channel list")
                KeyHint("Hold OK", if (favorite) "Remove favorite" else "Add favorite")
            }
        }
    }
}

@Composable
private fun KeyHint(key: String, action: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            key,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(action, fontSize = 15.sp, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun ChannelListPanel(
    title: String,
    channels: List<Channel>,
    current: Int,
    favorites: Set<String>,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = max(0, current - 4))
    val currentFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(10) {
            if (runCatching { currentFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(30)
        }
    }

    Box(
        Modifier
            .fillMaxHeight()
            .width(480.dp)
            .background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.95f), 0.8f to Color.Black.copy(alpha = 0.88f), 1f to Color.Transparent),
            )
            .onPreviewKeyEvent { ev ->
                if (ev.key == Key.DirectionRight && ev.type == KeyEventType.KeyDown) {
                    onClose()
                    true
                } else false
            },
    ) {
        Column(Modifier.padding(start = 36.dp, top = 40.dp, end = 56.dp)) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channelCount(channels.size), fontSize = 15.sp, color = Palette.TextDim, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp),
            ) {
                itemsIndexed(channels, key = { _, ch -> ch.key }) { i, ch ->
                    PanelRow(
                        number = i + 1,
                        channel = ch,
                        playing = i == current,
                        favorite = ch.key in favorites,
                        onClick = { onPick(i) },
                        modifier = if (i == current) Modifier.focusRequester(currentFocus) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelRow(
    number: Int,
    channel: Channel,
    playing: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val fg = if (focused) Color.Black else Palette.Text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White else if (playing) Color.White.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp),
    ) {
        Text(
            "$number",
            fontSize = 14.sp,
            color = if (focused) Color.Black.copy(alpha = 0.6f) else Palette.TextDim,
            modifier = Modifier.width(44.dp),
        )
        ChannelLogo(
            channel,
            modifier = Modifier
                .width(68.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            padding = 5.dp,
            initialsSize = 14.sp,
        )
        Text(
            channel.name,
            fontSize = 17.sp,
            fontWeight = if (focused || playing) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        )
        if (favorite) Icon(Icons.Filled.Star, contentDescription = null, tint = Palette.Gold, modifier = Modifier.size(18.dp))
        if (playing) {
            Box(
                Modifier
                    .padding(start = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Palette.Live),
            )
        }
    }
}
