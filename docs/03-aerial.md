# 3. Aerial launcher (instead of PatchWall)

**English** · [Монгол](03-aerial.mn.md)

## Why Aerial

- This firmware doesn't allow disabling or hiding PatchWall (`com.mitv.tvhome`): `pm disable-user`
  and `pm hide` return Permission denied. PatchWall always claims the HOME key with a high priority
  (100), so ordinary launchers like FLauncher or ATV Launcher can't become the default.
- [Aerial](https://github.com/pengchujin/aerial-tv) is made for Chinese TVs (MIT license, ~8k stars).
  Its **Home Guard** accessibility service notices PatchWall when HOME is pressed and brings Aerial
  back to the front. It's listed as working on the Android 6 Mi TV 4 series.

## Installed version

| | |
|---|---|
| Version | 1.75 (2026-10-31), package `app.orchard.tv` |
| APK | `apks/aerial-standard-1.75.apk` ([where to get it](../apks/README.md)) |
| SHA-256 | `9fa0e909052895d945791bbc9c34320bb79f59f77ea4012ac096e78db74a48e2` |
| Signature | **"Android Debug"** key (SHA-256 `e08962dc…6685`). Not a release key: it's a community app |

## Install and repair

```sh
./scripts/aerial-setup.sh            # install + grant permissions + turn on Home Guard
./scripts/aerial-setup.sh repair     # HOME stays on PatchWall and Aerial doesn't appear
```

Android 6 has no `cmd` command, so the script sets these by hand:

| Setting | Why |
|---|---|
| `pm grant … WRITE_SECURE_SETTINGS` | Aerial turns Home Guard back on by itself if it gets disabled |
| `appops SYSTEM_ALERT_WINDOW / WRITE_SETTINGS / GET_USAGE_STATS = allow` | Open at boot, set the brightness, know which app is in front (on Xiaomi's Android 6, PatchWall sends no accessibility events) |
| `enabled_notification_listeners = …RevivalListener` | Revives Aerial when Xiaomi kills it in standby |
| `enabled_accessibility_services = …HomeGuardService`, `accessibility_enabled = 1` | Home Guard |

⚠️ Aerial's official `curl … | bash` installer wasn't used. It relies on `cmd` commands that
Android 6 doesn't have, and running a script straight from the internet is risky.
⚠️ This setup replaces any accessibility services that were on before (karaoke, 小爱). Those apps
were removed, so here it causes no problem.

## Using it

- When you press HOME, PatchWall shows for **1–3 seconds** and then Aerial comes back. That's
  normal on this firmware.
- Pin SmartTube and Weather to the home screen from Aerial's app list.
- **Mi Box S:** pin the HDMI input and give it a name and icon. HDMI numbering on this firmware
  (22.4): HDMI1=23, HDMI2=24, HDMI3=25, AV=2. These are the values of the int extra `input` for the
  activity `com.xiaomi.mitv.tvplayer/.ExternalSourceActivity`.
- Aerial shows the weather on its home screen. It guesses the city from the IP address. If the city
  is wrong, scan the QR code in the wallpaper menu with your phone and set it (for example
  "Ulaanbaatar").
- **Update:** download a new APK from the [GitHub releases](https://github.com/pengchujin/aerial-tv/releases)
  and run `APK=<file> ./scripts/aerial-setup.sh`.

## Undo

```sh
./scripts/aerial-restore.sh               # HOME → PatchWall, Aerial stays installed
./scripts/aerial-restore.sh --uninstall   # also uninstall Aerial
```

## Notes

- Aerial has broad permissions, such as accessibility and writing system settings.
- PatchWall's child lock (儿童锁) works independently of Aerial. Home Guard doesn't bypass it, which
  is the correct behavior.
- Xiaomi's screensaver (`com.mitv.screensaver`) is still there, so it may show the weather for
  "天津 河西" (Tianjin).
