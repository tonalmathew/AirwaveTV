package com.airwave.tv.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
        ScreenHeader("Settings")
        Column(Modifier.widthIn(max = 760.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingRow(
                icon = Icons.Filled.Place,
                title = "Home country",
                subtitle = "Shown first on Home and in categories",
                value = "${home.flag}  ${home.name}",
                onClick = vm::openCountryPicker,
                modifier = Modifier.restorableFocus(vm, "settings/country", isFirst = true),
            )
            SettingRow(
                icon = GlobeIcon,
                title = "Home language",
                subtitle = "Channels in this language are shown first",
                value = vm.homeLanguage?.let { "🗣️  $it" } ?: "Any language",
                onClick = vm::openLanguagePicker,
                modifier = Modifier.restorableFocus(vm, "settings/language"),
            )
            SettingRow(
                icon = Icons.Filled.Warning,
                title = "Hide unreliable channels",
                subtitle = "Skip geo-blocked and part-time streams",
                onClick = { vm.updateHideUnreliable(!vm.hideUnreliable) },
                modifier = Modifier.restorableFocus(vm, "settings/hide"),
                trailing = { focused -> Toggle(vm.hideUnreliable, focused) },
            )
            SettingRow(
                icon = Icons.Filled.Refresh,
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
                icon = Icons.Filled.Info,
                title = "About Airwave TV ${BuildConfig.VERSION_NAME}",
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
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    trailing: (@Composable (focused: Boolean) -> Unit)? = null,
) {
    TvCard(onClick = onClick, modifier = modifier.fillMaxWidth(), focusedScale = 1.02f, showBorder = false) { focused ->
        val fg = if (focused) Color.Black else Palette.Text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Color.White else Palette.Surface)
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Icon(icon, contentDescription = null, tint = if (focused) Palette.Accent else Palette.TextDim, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = fg)
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = if (focused) Color.Black.copy(alpha = 0.6f) else Palette.TextDim,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (value != null) {
                Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = fg, modifier = Modifier.padding(start = 16.dp))
            }
            trailing?.invoke(focused)
        }
    }
}

@Composable
private fun Toggle(on: Boolean, focused: Boolean) {
    val offset by animateDpAsState(if (on) 22.dp else 0.dp, label = "toggle")
    Box(
        Modifier
            .padding(start = 16.dp)
            .width(52.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) Palette.Accent else if (focused) Color.Black.copy(alpha = 0.25f) else Palette.SurfaceHi)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.CenterStart),
        )
    }
}
