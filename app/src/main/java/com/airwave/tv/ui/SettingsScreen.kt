package com.airwave.tv.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.tv.BuildConfig
import com.airwave.tv.data.Library

@Composable
fun SettingsScreen(vm: AppViewModel, lib: Library) {
    val home = lib.country(vm.homeCountry)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = ScreenPadding, end = 48.dp, bottom = 48.dp),
    ) {
        ScreenHeader("Settings", eyebrow = "Airwave TV ${BuildConfig.VERSION_NAME}")
        Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingRow(
                icon = Icons.Outlined.Place,
                colors = listOf(Color(0xFF3B82F6), Color(0xFF1E40AF)),
                title = "Home country",
                subtitle = "Shown first on Home and in categories",
                value = "${home.flag}  ${home.name}",
                onClick = vm::openCountryPicker,
                modifier = Modifier.restorableFocus(vm, "settings/country", isFirst = true),
            )
            SettingRow(
                icon = Icons.Outlined.Language,
                colors = listOf(Color(0xFFA855F7), Color(0xFF6B21A8)),
                title = "Home language",
                subtitle = "Channels in this language are shown first",
                value = vm.homeLanguage ?: "Any language",
                onClick = vm::openLanguagePicker,
                modifier = Modifier.restorableFocus(vm, "settings/language"),
            )
            SettingRow(
                icon = Icons.Outlined.VisibilityOff,
                colors = listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
                title = "Hide unreliable channels",
                subtitle = "Skip geo-blocked and part-time streams",
                onClick = { vm.updateHideUnreliable(!vm.hideUnreliable) },
                modifier = Modifier.restorableFocus(vm, "settings/hide"),
                trailing = { focused -> Toggle(vm.hideUnreliable, focused) },
            )
            SettingRow(
                icon = Icons.Outlined.Refresh,
                colors = listOf(Color(0xFF14B8A6), Color(0xFF0F766E)),
                title = "Refresh channel list",
                subtitle = if (vm.refreshing) {
                    "Updating…"
                } else {
                    "Last updated ${DateUtils.getRelativeTimeSpanString(lib.updatedAt)}  ·  %,d channels".format(lib.all.size)
                },
                onClick = vm::refresh,
                modifier = Modifier.restorableFocus(vm, "settings/refresh"),
            )
            SettingRow(
                icon = Icons.Outlined.Info,
                colors = listOf(Color(0xFF64748B), Color(0xFF334155)),
                title = "About",
                subtitle = "Channels come from the public iptv-org playlist (github.com/iptv-org/iptv). " +
                    "Airwave doesn't host any streams; availability depends on each broadcaster.",
                onClick = {},
                modifier = Modifier.restorableFocus(vm, "settings/about"),
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    colors: List<Color>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    trailing: (@Composable (focused: Boolean) -> Unit)? = null,
) {
    TvCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = Shapes.Tile, focusedScale = 1.02f, showBorder = false) { focused ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focused) Modifier.background(Color.White) else Modifier.glass(Shapes.Tile))
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            TileIcon(icon, focused, colors)
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (focused) Palette.OnFocus else Palette.Text)
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = if (focused) Palette.OnFocusDim else Palette.TextDim,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (value != null) {
                Text(
                    value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (focused) Palette.OnFocus else Palette.TextDim,
                    modifier = Modifier.padding(start = 16.dp),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (focused) Palette.OnFocus else Palette.TextFaint,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(22.dp),
                )
            }
            trailing?.invoke(focused)
        }
    }
}

@Composable
private fun Toggle(on: Boolean, focused: Boolean) {
    val offset by animateDpAsState(if (on) 20.dp else 0.dp, label = "toggle")
    Box(
        Modifier
            .padding(start = 16.dp)
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    on -> Palette.Accent
                    focused -> Palette.OnFocus.copy(alpha = 0.18f)
                    else -> Color.White.copy(alpha = 0.14f)
                },
            )
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.CenterStart),
        )
    }
}