# 5. Chinese apps: what was removed and what stays

**English** · [Монгол](05-app-removal.mn.md)

## Method

This firmware blocks `pm disable-user` and `pm hide` (Permission Denial). So:

| Kind of app | Remove | Restore |
|---|---|---|
| Third-party (`/data/app`) | `pm uninstall <pkg>` removes it completely | Only from the backup APK: `adb install <apk>` |
| System, with an update installed | `pm uninstall -k --user 0 <pkg>` (drops the update and hides it, `hidden=true`) | `adb install -r -d --user 0 <backup apk>` |
| System, only in `/system` | `pm uninstall -k --user 0 <pkg>` (`installed=false`) | `pm install -r --user 0 /system/…/<app>.apk` |

⚠️ A plain `pm install -r` reports "Success" but doesn't bring the app back (it stays
`installed=false`). You must add `--user 0`. [`backup/restore.sh`](../backup/restore.sh) does this
correctly:

```sh
cd backup
./restore.sh com.xiaomi.voicecontrol    # restore one app
./restore.sh                             # restore all of them
```

Before removal, every APK in `/data/app` was saved to the `backup/apk/` folder (243MB, not in the
repo). Removal log: `backup/removal-log.txt`. State before the changes:
`backup/packages-before.txt`, `backup/settings-before.txt`.

## The 24 removed apps

### Third-party: removed completely

| Package | What it is |
|---|---|
| `com.ktcp.video` | 云视听极光, Tencent's video service |
| `com.xm.webcontent` | 电视活动中心, promotions and ads |
| `com.mitv.shoplugin` | Shop plugin |
| `com.xiaomi.eetv.litehome` | 办公模式 (office mode) |
| `com.mitv.care` | **adcare**, advertising |
| `com.google.android.youtube` | The 2015 phone YouTube 10.16. Replaced by SmartTube |

### System: hidden from the user

| Package | What it is |
|---|---|
| `com.miui.systemAdSolution` | **System ads** (AdServer, boot ads) |
| `com.miui.tv.analytics` | Data collection (telemetry) |
| `com.xiaomi.statistic` | Statistics (telemetry) |
| `cleantools.mitv.com.tvcleantools` | Richinfo "MobileService" (telemetry SDK, runs with the system UID) |
| `com.xiaomi.mitv.tvpush.tvpushservice` | Push notifications |
| `com.mitv.cloudcontrol` | Xiaomi remote configuration (cloud control) |
| `com.xiaomi.mitv.appstore` | Xiaomi app store (China) |
| `com.xiaomi.mitv.shop` | Xiaomi shop |
| `com.xiaomi.mitv.payment`, `com.xiaomi.mitv.pay` | Payments |
| `com.xiaomi.gamecenter.sdk.service.mibox` | Game center SDK |
| `com.xiaomi.voicecontrol` | 小爱同学 voice assistant. **The voice button on the remote no longer works** |
| `com.xiaomi.smarthome.tv`, `com.xiaomi.smarthome.tv.service` | Mi Home (smart home) |
| `com.xiaomi.tweather` | Xiaomi weather |
| `com.xiaomi.mitv.calendar` | Calendar (日历) |
| `com.xiaomi.mitv.karaoke.service` | Karaoke |
| `com.xiaomi.screenrecorder` | Screen recorder |

The update file of `com.xiaomi.mitv.payment` in `/data/app` had disappeared before removal, so there's
no backup of it. It came back once after a reboot, and after removing it again it stayed gone. If it
ever needs restoring, restore it from the original in `/system`.

## ⚠️ Apps you must not touch

| Package | Why |
|---|---|
| `com.mitv.tvhome` | PatchWall. Aerial depends on it. Removing it can break the power button and the system |
| `com.xiaomi.mitv.tvplayer`, `com.droidlogic.tvinput`, `mitv.service` | HDMI/AV/TV inputs (**Mi Box S**), the TV's core service |
| `com.xiaomi.mitv.settings`, `com.xiaomi.mitv.providers.settings` | Settings (the menu that turns on ADB is here) |
| `com.xiaomi.mitv.systemui`, `com.android.systemui` | Volume overlay, menus |
| `com.xiaomi.mitv.remotecontroller.service` | Remote |
| `com.xiaomi.tvqs` | Quick settings on the Menu button |
| `com.xiaomi.mitv.tvmanager` | Central service running with the system UID: reports the active app to the system and manages memory |
| `com.xiaomi.mitv.upgrade` | OTA updates |
| `com.sohu.inputmethod.sogou.tv` | **The only keyboard** |
| `com.android.packageinstaller` | Installs APKs |
| `com.duokan.airkan.tvbox`, `com.xiaomi.upnp`, `com.mi.miplay.mitvupnpsink`, `com.xiaomi.miplay`, `com.xiaomi.mitv.smartshare`, `com.xiaomi.mi_connect_service` | Casting from phones, AirPlay, DLNA, "Phone can control TV" |
| `com.xiaomi.mitv.mediaexplorer`, `com.mitv.videoplayer`, `com.mitv.mivideoplayer`, `com.mitv.gallery`, `com.mitv.codec.update` | Movies and photos from USB, codecs |
| `com.xiaomi.account`, `com.xiaomi.account.auth` | Xiaomi account |
| `com.mitv.screensaver`, `com.mitv.alarmcenter`, `com.xiaomi.mibox.lockscreen`, `com.xiaomi.mitv.legal.webview`, `com.xiaomi.mitv.debug`, `com.xiaomi.android.TV.audio` | Screensaver, time settings, lock and other system parts |
| `com.android.*`, `com.droidlogic`, `android` | Android core |

## Chinese parts that remain (for reference)

- PatchWall itself stays and shows for 1–3 seconds when HOME is pressed.
- The logs show Xiaomi's **OneTrack** telemetry SDK inside one of the remaining apps. It sent events
  such as app removals.
- The Xiaomi screensaver shows the weather for "天津 河西" (Tianjin).
- The Sogou keyboard is a Chinese keyboard, but it's the only one, so it stays.

## Removing them again

For example, after a factory reset:

```sh
./scripts/remove-chinese-apps.sh    # asks to confirm, backs up, then removes
adb reboot                           # then check
```
