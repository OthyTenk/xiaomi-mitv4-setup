# Xiaomi Mi TV 4 (China edition): new launcher, YouTube, no ads

**English** · [Монгол](README.mn.md)

Scripts, notes and two small TV apps for making a **China-market Xiaomi Mi TV 4** (PatchWall /
MiTV OS, Android 6.0.1) usable outside China. No root and no firmware flashing: everything runs
over Wi‑Fi ADB from a Mac.

What you get:

- **[Aerial](https://github.com/pengchujin/aerial-tv)** as the home screen instead of PatchWall
  (HOME opens Aerial).
- **[SmartTube](https://github.com/yuliskov/SmartTube)** for YouTube: no ads, no Google services needed.
- **24 ad, telemetry and China-only apps** removed or hidden. Each one is backed up first, and
  there's a script to put any of them back.
- **Цаг агаар (Weather)**: a one-screen TV app with current weather, air quality (US AQI from the
  PM2.5 NowCast), the next 24 hours and 7 days. Data comes from Open-Meteo. It's set to Ulaanbaatar,
  and any city works.
- **Kick**: a Kick.com live stream app for the TV remote. Kick's official TV app needs Android 8+.
  Tabs: Favorites, Mongolian streams, all countries, and search.

Both apps have a Mongolian interface.

## Tested on

| | |
|---|---|
| Model | Xiaomi Mi TV 4 series 55", `MiTV4-ANSM0` (codename `xmen`), China market |
| Chip | Amlogic T962 (TXL), 4× Cortex-A53. Apps are **32-bit only** (armeabi-v7a) |
| Memory | 2 GB RAM / 8 GB eMMC |
| Software | Android 6.0.1, MiTV OS 1.3.128 (build 22.4.15.3235), PatchWall |

Other China-market Mi TVs with Android 6 and PatchWall are probably similar, but only this one has
been tested. Check `adb shell pm list packages` against the lists in
[docs/05](docs/05-app-removal.md) before you remove anything.

## ⚠️ Read this first

1. **Don't connect the TV to a computer with a USB cable.** The TV tries to enter USB flash mode
   and keeps rebooting on the Mi logo. If that happens, unplug the cable, unplug the power for
   1 minute, and turn it back on.
2. **Don't flash another firmware (Android TV, Global ROM).** No firmware fits this board, so you
   will brick the TV. [docs/01](docs/01-tv.md) explains why.
3. **Use ADB over Wi‑Fi only.** Xiaomi turns ADB debugging off on every standby and reboot. Turn
   it on when you need it, and off again when you're done.
4. **Only remove the apps on the list.** Removing the HDMI player, settings, remote or keyboard
   packages breaks the TV. The do-not-touch list is in [docs/05](docs/05-app-removal.md).
5. **Keep your signing keys.** The first build of each app creates `keystore.jks` and
   `keystore.pass`. Without them you can't update the installed app, only uninstall and reinstall
   it. Never commit them (`.gitignore` already excludes them).
6. **A factory reset** brings the removed apps back and deletes Aerial, SmartTube and the two apps.
   Run the setup below again.
7. **Child lock (儿童锁):** if PatchWall's child lock is on, HOME may ask a sum like "45 − 12 = ?".
   This repo deliberately doesn't bypass it. Turn it off in PatchWall → 家长中心.

Everything here is at your own risk.

## Setup (also after a factory reset)

You need macOS with `adb` (Android platform-tools). The scripts use zsh and macOS `nc`/`stat`, so
on Linux they need small changes.

1. Connect the TV to Wi‑Fi.
2. On the TV: Settings → About → select **Model** and press OK on the remote 7 times. This turns on
   developer mode.
3. Settings → **Account & security** → **ADB debugging = Allow**, **Install unknown apps = Allow**.
4. Find the TV's IP (Settings → Network) and pass it to the scripts through `TV`. The default is
   `192.168.1.13`.
   ```sh
   export TV=192.168.1.50
   ```
5. Download the APKs into [`apks/`](apks/README.md).
6. Run:
   ```sh
   cd scripts
   ./adb-connect.sh           # on the TV, tick "Always allow" and press OK
   ./remove-chinese-apps.sh   # asks first, backs up to backup/apk, then removes/hides 24 apps
   ./install-apps.sh          # SmartTube + Weather + Kick (Kick takes ~2 min to install)
   ./aerial-setup.sh          # Aerial + Home Guard
   adb reboot                 # the TV should start into Aerial
   ```
7. In Aerial, pin the HDMI input, Weather, SmartTube and Kick to the home screen.
8. Set **ADB debugging** back to **Disallow**.

Pressing HOME shows PatchWall for 1–3 seconds before Aerial comes back. That's expected on this
firmware: PatchWall can't be disabled, so Aerial's Home Guard brings Aerial back to the front.

## Everyday commands

```sh
./scripts/tv-info.sh                 # model, foreground app, Home Guard status, installed apps (read-only)
./scripts/screenshot.sh              # screenshot → ~/Desktop
./scripts/aerial-setup.sh repair     # HOME stays on PatchWall and Aerial doesn't come back
./scripts/aerial-restore.sh          # give HOME back to PatchWall (--uninstall also removes Aerial)
./backup/restore.sh <package>        # put a removed app back
```

## Docs

| # | Topic | Doc |
|---|---|---|
| 1 | TV specs; why Android TV / Google Play can't be installed; why the root CAs can't be updated | [docs/01](docs/01-tv.md) |
| 2 | ADB over Wi‑Fi, common errors, useful commands | [docs/02](docs/02-adb.md) |
| 3 | Aerial and Home Guard on Android 6 (no `cmd` command) | [docs/03](docs/03-aerial.md) |
| 4 | SmartTube | [docs/04](docs/04-smarttube.md) |
| 5 | Which apps were removed, which must stay, how to restore them | [docs/05](docs/05-app-removal.md) |
| 6 | Weather app: data, NowCast AQI, bundled Let's Encrypt roots, another city | [docs/06](docs/06-weather.md) |
| 7 | Troubleshooting: boot loop, ADB, HOME, child lock, voice button | [docs/07](docs/07-troubleshooting.md) |
| 8 | Kick app: why it exists, controls, how it uses Kick's API, build | [docs/08](docs/08-kick.md) |

## Building the apps

You need JDK 17 and the Android SDK (build-tools, platform 35). Gradle isn't used: `build.sh` runs
aapt2 → javac → d8 → zipalign → apksigner directly.

```sh
cd tsag-agaar-tv && ./build.sh         # → build/tsag-agaar-tv.apk
cd kick-tv && ./build.sh               # → build/kick-tv.apk (downloads Media3 + Guava, checks SHA-256)
TV=<ip> ./build.sh install             # build, install and open it on the TV
python3 art.py                         # redraw icon and banner (needs Pillow)
```

- The first build creates your own signing key. A build signed with your key won't install over
  the APK from Releases, so uninstall that one first.
- **Weather for another city:** change `COORDS` and the time zone in
  `tsag-agaar-tv/src/mn/home/tsagagaar/MainActivity.java`, and the city name in
  `res/layout/main.xml` ([docs/06](docs/06-weather.md)).
- **Kick** uses the same web endpoints kick.com itself calls, with no login. If Kick changes them,
  the lists stop loading; fix `kick-tv/src/mn/home/kick/Kick.java`. Streams Kick marks 18+ are
  hidden in the Mongolian and All tabs (`HIDE_MATURE` in `MainActivity.java`).

## Repository layout

```
├── docs/            detailed guides 01–08, English (`.md`) and Mongolian (`.mn.md`)
├── scripts/         setup and maintenance scripts
├── apks/            where downloaded APKs go, with SHA256SUMS.txt
├── backup/          restore.sh; remove-chinese-apps.sh saves APK backups here
├── info/            the TV's package list before any change
├── tsag-agaar-tv/   Weather app source (Java, no dependencies)
└── kick-tv/         Kick app source (Java + AndroidX Media3 ExoPlayer)
```

These are not in the repo: signing keys, build outputs, third-party APKs (download them from
upstream), backups of Xiaomi's apps, and the TV's serial numbers and MAC addresses.

## License

The code and docs in this repo are under the [MIT License](LICENSE). Aerial, SmartTube, Breezy
Weather, Media3 and Guava are separate projects with their own licenses. This project isn't
affiliated with Xiaomi, Kick, or the Aerial and SmartTube projects.
