package com.airwave.tv.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.data.Library
import kotlinx.coroutines.delay

@Composable
fun AppRoot(vm: AppViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .onFocusChanged { vm.uiHasFocus = it.hasFocus },
    ) {
        // The intro plays to the end (and holds while loading) before the app takes over.
        var introDone by rememberSaveable { mutableStateOf(false) }
        val screen = if (introDone) vm.loadState else LoadState.Loading
        Crossfade(screen, animationSpec = tween(350), label = "root") { state ->
            when (state) {
                LoadState.Loading -> Splash(
                    ready = vm.loadState != LoadState.Loading,
                    onFinished = { introDone = true },
                )
                is LoadState.Failed -> LoadError(state.message, onRetry = vm::load)
                LoadState.Ready -> vm.library?.let { MainShell(vm, it) }
            }
        }
        vm.player?.let { session ->
            key(session) { PlayerScreen(vm, session) }
        }
    }
}

@Composable
private fun MainShell(vm: AppViewModel, lib: Library) {
    val activity = LocalContext.current as? Activity
    val navRequesters = remember { Tab.entries.associateWith { FocusRequester() } }
    var navFocused by remember { mutableStateOf(false) }

    // Back: pop a screen → jump to the menu → go Home → exit.
    BackHandler(enabled = vm.player == null) {
        when {
            vm.stack.isNotEmpty() -> vm.pop()
            !navFocused -> runCatching { navRequesters.getValue(vm.tab).requestFocus() }
            vm.tab != Tab.Home -> {
                vm.selectTab(Tab.Home)
                runCatching { navRequesters.getValue(Tab.Home).requestFocus() }
            }
            else -> activity?.finish()
        }
    }

    // If the item we want to refocus never shows up, fall back to the first item, then give up.
    val target = vm.focusTarget
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        delay(700)
        if (vm.focusTarget == target) vm.focusTarget = if (target != FIRST) FIRST else null
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = NavCollapsedWidth),
        ) {
            when (val top = vm.stack.lastOrNull()) {
                is ChannelListScreen -> ChannelListContent(vm, top)
                is CountryPickerScreen -> CountriesScreen(vm, lib, top.gridState, pickMode = true) {
                    vm.setHome(it)
                    vm.pop()
                }
                is LanguagePickerScreen -> LanguagesScreen(vm, lib, top.gridState, pickMode = true) {
                    vm.chooseHomeLanguage(it)
                    vm.pop()
                }
                null -> when (vm.tab) {
                    Tab.Home -> HomeScreen(vm, lib)
                    Tab.Search -> SearchScreen(vm, lib)
                    Tab.Countries -> CountriesScreen(vm, lib, vm.countriesGrid, pickMode = false) { vm.openCountry(it) }
                    Tab.Categories -> CategoriesScreen(vm, lib)
                    Tab.Languages -> LanguagesScreen(vm, lib, vm.languagesGrid, pickMode = false) { name ->
                        name?.let(vm::openLanguage)
                    }
                    Tab.Favorites -> FavoritesScreen(vm, lib)
                    Tab.Settings -> SettingsScreen(vm, lib)
                }
            }
        }
        SideNav(
            selected = vm.tab,
            requesters = navRequesters,
            onSelect = vm::selectTab,
            onFocusChange = { navFocused = it },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SideNav(
    selected: Tab,
    requesters: Map<Tab, FocusRequester>,
    onSelect: (Tab) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(if (expanded) NavExpandedWidth else NavCollapsedWidth, label = "navWidth")
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxHeight()
            .width(width)
            .background(
                if (expanded) {
                    Brush.horizontalGradient(0f to Palette.Bg, 0.8f to Palette.Bg.copy(alpha = 0.97f), 1f to Palette.Bg.copy(alpha = 0.6f))
                } else {
                    Brush.horizontalGradient(listOf(Palette.Bg, Palette.Bg))
                },
            )
            .onFocusChanged {
                expanded = it.hasFocus
                onFocusChange(it.hasFocus)
            }
            .focusProperties { enter = { requesters.getValue(selected) } }
            .focusGroup()
            .padding(horizontal = 14.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, bottom = 28.dp)) {
            AppMark(44.dp)
            if (expanded) {
                Text(
                    "Airwave",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        }
        Tab.entries.forEach { tab ->
            var focused by remember { mutableStateOf(false) }
            val isSelected = tab == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .focusRequester(requesters.getValue(tab))
                    .onFocusChanged {
                        focused = it.isFocused
                        if (it.isFocused) onSelect(tab)
                    }
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        when {
                            focused -> Color.White
                            isSelected -> Color.White.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        },
                    )
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        focusManager.moveFocus(FocusDirection.Right)
                    }
                    .padding(horizontal = 15.dp),
            ) {
                Icon(
                    tab.icon,
                    contentDescription = tab.label,
                    tint = when {
                        focused -> Color.Black
                        isSelected -> Palette.Text
                        else -> Palette.TextDim
                    },
                    modifier = Modifier.size(24.dp),
                )
                if (expanded) {
                    Text(
                        tab.label,
                        fontSize = 17.sp,
                        fontWeight = if (focused || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (focused) Color.Black else Palette.Text,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadError(message: String, onRetry: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📡", fontSize = 64.sp)
        Text("Couldn't load channels", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Check that your TV is connected to the internet.\n$message",
            fontSize = 17.sp,
            color = Palette.TextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
        )
        TvButton("Try again", onClick = onRetry, modifier = Modifier.focusRequester(focus))
    }
}
