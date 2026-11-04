# 8. Kick: our own app

**English** · [Монгол](08-kick.mn.md)

## Why build our own

Existing options, checked on 2026-10-31:

| App | Why it doesn't work |
|---|---|
| Kick's official Android TV app | Needs **Android 8.0+** (API 26), according to APKMirror. This TV has Android 6 (API 23) |
| Kick's phone app (`com.kick.mobile`) | Android 7+. Also awkward to use with a remote |
| SmartKickTV ([CxWatcher/SmartTVKick](https://github.com/CxWatcher/SmartTVKick)) | Android 7+ (minSdk 24). The source code isn't published, only the APK. It has native libraries and permission to update itself |
| Kodi + a Kick addon | No reliable addon found |

HTTPS is no problem. Kick's servers chain to roots this TV already has, so unlike the Weather app no
root certificate needs to be bundled:

- `kick.com`, `web.kick.com`, `stream.kick.com`: Google Trust Services → **GlobalSign Root CA** (cross-signed)
- Video (`*.live-video.net`, Amazon IVS) and images (`images.kick.com`, `files.kick.com`):
  Amazon → **Starfield Services Root G2**

## Installed version

| | |
|---|---|
| Package | `mn.home.kick`, version 1.0. Appears on the TV as **"Kick"** |
| APK | `apks/kick-tv.apk`, ≈2.2MB ([Releases](https://github.com/OthyTenk/xiaomi-mitv4-setup/releases)) |
| Code | [`kick-tv/`](../kick-tv/) |
| Video player | AndroidX Media3 (ExoPlayer) 1.11.1, HLS. Supports Android 6 (API 23) |

## Screen

There are 4 tabs at the top. Moving onto a tab with the remote's arrows opens it right away. The
interface is in Mongolian; the tab names are given in parentheses.

| Tab | What it shows |
|---|---|
| **Favorites** (Дуртай) | Channels you added. Live ones first (by viewer count), offline ones after them |
| **Mongolian** (Монгол) | All live streams in Mongolian (Kick's `language=mn`), by viewer count |
| **All countries** (Бүх улс) | The 48 most-watched streams worldwide |
| **Search** (Хайх) | Search by channel name. It searches as you type; "Search/Done" on the keyboard jumps to the results |

Each card shows a thumbnail, a LIVE badge, the viewer count, the title, the channel name and the
game category. Favorite channels have a ★ before their name. Favorites are stored only on this TV
(there's no Kick account login).

When the app opens, it shows **Favorites** if you have any, otherwise **Mongolian**.

### 18+ filter

Streams that Kick itself marks 18+ (`is_mature`) are hidden in the **Mongolian** and **All
countries** tabs. This was set up because the TV's child lock (儿童锁) is on. These channels still
show in Search and Favorites. To turn the filter off, set `HIDE_MATURE = false` in
`kick-tv/src/mn/home/kick/MainActivity.java` and rebuild.

## Controls

**Lists:**

| Button | Action |
|---|---|
| Arrows | Move between cards and tabs |
| **OK** | Watch the stream |
| **Menu (≡)** or **long-press OK** | Add to / remove from favorites |
| **OK** on a tab | Reload the list. A list older than 1 minute also reloads when you return to its tab or leave the player |

**Player:**

| Button | Action |
|---|---|
| **◀ ▶** | Previous / next live stream in the current tab |
| **▲ ▼** | Quality: auto → 1080p → 720p → 480p → 360p → 160p |
| **OK** | Info bar (channel, title, viewers, how long it's been live, quality) |
| **Menu (≡)** or **long-press OK** | Add to / remove from favorites |
| **Back** | Back to the list |

- Quality starts at **auto**. On this network it usually climbs to 1080p60.
- If the network drops, it reconnects by itself after 2, 5, 10 and 30 seconds.
- If the stream ends or the channel is offline, it checks every minute and starts playing when the
  stream comes back.
- While playing, the screen and Xiaomi's screensaver don't turn on.

**Not included:** chat, Kick account login ("Following" list), recordings (VODs), clips.

## How it works

- It uses Kick's web API. The kick.com site calls the same endpoints, and they need no login:
  - `web.kick.com/api/v1/livestreams?language=mn&sort=viewer_count_desc`: lists
  - `kick.com/api/v2/channels/<channel>`: channel info and `playback_url`
  - `kick.com/api/search?searched_word=<name>`: search

  The official public API (`api.kick.com`) needs an OAuth token and doesn't return video URLs, so it
  isn't used.
- The video is Amazon IVS HLS (1080p60 ≈ 9 Mbps, 720p60, 480p, 360p, 160p). The token in
  `playback_url` expires after ≈10 minutes, so a fresh one is fetched on every play and reconnect.
- If Kick changes its API, the lists stop loading (for example `HTTP 403` or `JSONException`). Then
  fix `kick-tv/src/mn/home/kick/Kick.java`.

### Tests (2026-10-31)

- Running the app's `Kick.java` on the TV with `app_process` (no UI) loaded 33 Mongolian and 48
  worldwide streams in ≈1 second. A 4.6MB segment of 1080p60 video downloaded in ≈1 second.
- The TV's Amlogic hardware decoder (`OMX.amlogic.avc.decoder.awesome`) played 1920×1080 at 60fps
  without dropping frames (`/sys/class/vdec/vdec_status`: drop count 0).
- Screenshots (`scripts/screenshot.sh`) show the video as black. Amlogic's video layer isn't captured
  in screenshots; that's normal.

## Build and install

You need JDK 17 and the Android SDK (build-tools, platform 35). Gradle isn't used.

```sh
cd kick-tv
./build.sh              # → build/kick-tv.apk
./build.sh install      # build, then install and open it on the TV (ADB must be on)
python3 art.py          # redraw the icon and banner (needs Pillow)
```

- The first build downloads Media3 and Guava from Maven into `kick-tv/libs/` and checks them against
  the SHA-256 values in `build.sh`. Later builds work offline.
- Installing on the TV takes ≈2 minutes, because Android 6 fully compiles the app at install time.
- For a new version, increase `versionCode` in `AndroidManifest.xml`. Copy the new build to
  `apks/kick-tv.apk` and update `apks/SHA256SUMS.txt`.
- Open a specific channel directly (useful for testing):
  `adb shell am start -n mn.home.kick/.PlayerActivity -e slug kesharp`

## ⚠️ Keystore

`kick-tv/keystore.jks` and `keystore.pass` are the app's signing key. If they're deleted, a new
build won't install over the old app. Then run `adb uninstall mn.home.kick` and install it fresh
(favorites are lost). Never put these files in git or anywhere public.

## Code layout

| File | Role |
|---|---|
| `AndroidManifest.xml` | Package, LAUNCHER + LEANBACK_LAUNCHER, banner, INTERNET |
| `src/…/MainActivity.java` | Tabs, cards, search, favorites, 18+ filter |
| `src/…/PlayerActivity.java` | ExoPlayer, switching channels, quality, reconnecting, info bar |
| `src/…/Kick.java` | Kick API (lists, channel, search), HTTP |
| `src/…/Favorites.java` | Favorite channels (SharedPreferences) |
| `src/…/Images.java` | Image download and memory cache |
| `res/layout/main.xml`, `card.xml`, `tab.xml`, `player.xml` | Layouts tuned for 960×540dp |
| `res/drawable/*.xml` | Card focus frame, tabs, badges, player shadow |
| `res/drawable-xhdpi/icon.png`, `banner.png` | Icon and TV banner drawn by `art.py` |
| `build.sh` | Download libraries → aapt2 → javac → d8 → zipalign → apksigner |
| `libs/` | Media3 1.11.1, Guava 33.3.1 (downloaded by build.sh) |
