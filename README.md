# Airwave TV

A free live-TV (IPTV) player for Google TV / Android TV. Channels come from the public
[iptv-org/iptv](https://github.com/iptv-org/iptv) playlist (`https://iptv-org.github.io/iptv/index.m3u`),
which the app downloads on first launch and refreshes every 12 hours. Channel languages come from the
language-grouped playlist (`index.language.m3u`), matched by stream URL.

## Features

- **Home** – featured channel header, *Continue watching*, *Your favorites*, your country's channels, and rows per genre
- **Search** – on-screen keyboard built for the remote, instant results
- **Countries / Categories / Languages** – browse by country, genre or spoken language (200+ languages)
- **Combinable filters** – every channel list has filter chip rows (Category, Country, Language),
  e.g. *Religious → India → Hindi*
- **Favorites** – hold **OK** on any channel (or while watching) to add/remove it
- **Player** – ExoPlayer (HLS, DASH, progressive), automatic fallback to alternative streams,
  channel zapping and a slide-in channel list
- Settings: home country, home language (defaults to the TV's language; its channels come first on Home
  and in every list, plus an *In &lt;language&gt;* row on Home), hide geo-blocked/part-time channels, manual refresh

## Remote controls

| Where | Button | Action |
|---|---|---|
| Anywhere | ◀ (from content) / Back | Open the side menu |
| Channel tile | OK | Watch |
| Channel tile | Hold OK | Add/remove favorite |
| Player | ▲ / ▼ (or CH+/CH−) | Previous / next channel |
| Player | OK or ◀ | Channel list |
| Player | ▶ | Show channel info |
| Player | Hold OK | Add/remove favorite |
| Player | Back | Close list / leave player |

## Install on Google TV (sideload)

Download `AirwaveTV-1.0.apk` from the [latest release](../../releases/latest).

**Option A: ADB over Wi-Fi (most reliable)**

1. On the TV: *Settings → System → About* → click **Android TV OS build** 7 times to enable Developer options.
2. *Settings → System → Developer options* → turn on **USB debugging** (on newer Google TV: **Wireless debugging**).
3. Find the TV's IP: *Settings → Network & Internet → (your network)*.
4. On your computer:
   ```bash
   adb connect <TV_IP>:5555
   adb install -r AirwaveTV-1.0.apk
   ```
   Accept the "Allow USB debugging?" prompt on the TV the first time.
   (With *Wireless debugging* on Google TV 12+, first pair with `adb pair <ip>:<pair-port>` using the code shown on the TV,
   then `adb connect <ip>:<port>`.)

**Option B: Send Files to TV / Downloader app**

1. Install **Send Files to TV** (or **Downloader**) from the Play Store on the TV.
2. Allow it to install unknown apps: *Settings → Apps → Security & restrictions → Unknown sources*.
3. Send the APK from your phone/PC to the TV and open it to install.

Airwave TV then appears in your **Apps** row on Google TV.

## Building

Requires JDK 17 and the Android SDK (platform 34).

```bash
./gradlew assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`
(signed with the debug key so it can be sideloaded directly).

## Notes

Airwave doesn't host any streams. iptv-org is a community-maintained list of publicly available streams,
so some channels will be offline or geo-blocked at any given time; the player tries each alternative
stream automatically before showing an error.
