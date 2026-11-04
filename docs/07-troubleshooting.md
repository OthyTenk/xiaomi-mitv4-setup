# 7. Troubleshooting

**English** · [Монгол](07-troubleshooting.mn.md)

## The TV is stuck on the Mi logo or keeps rebooting

1. **If a USB cable is connected, unplug it.** This is the most common cause ([docs/02](02-adb.md)).
2. Pull the plug from the wall (turning it off with the remote isn't enough), wait 1 minute and plug
   it back in. Then wait 5 minutes without pressing anything.
3. If that doesn't help, use **Recovery mode**:
   - Unplug the TV and wait 10 seconds.
   - Hold **OK + Back** (Bluetooth remote) or **Home + Menu** (IR remote) and plug the TV in.
   - Keep holding the buttons until the recovery menu appears.
   - Choose **Reboot** first.
   - Use **Wipe data** (factory reset) only as a last resort. It deletes all apps and settings; then
     redo the setup from the "Setup" section of the [README](../README.md).

## ADB won't connect

See the "Common problems" table in [docs/02](02-adb.md). Usually ADB was turned off after the TV went
into standby.

## HOME stays on PatchWall and Aerial doesn't appear

```sh
./scripts/adb-connect.sh
./scripts/tv-info.sh              # does it say "Home Guard: АЖИЛЛАХГҮЙ" (not running)?
./scripts/aerial-setup.sh repair
```

Xiaomi kills Aerial in standby and turns accessibility off. Aerial usually recovers by itself, but
sometimes it needs a repair.

## HOME shows "请验证您的家长身份" (45 − 12 = ?)

This is PatchWall's **child lock (儿童锁)**. Enter the answer with the remote. If you don't need the
lock, turn it off in PatchWall → 家长中心 (Parent Center).

## The voice button on the remote doesn't work

That's expected, because 小爱同学 (`com.xiaomi.voicecontrol`) was removed. To bring it back:
`./backup/restore.sh com.xiaomi.voicecontrol`.

## I need an app that was removed

```sh
./backup/restore.sh <package>
```

The list of apps is in [docs/05](05-app-removal.md).

## The Weather app shows "Сүлжээний алдаа" (network error)

- Check the TV's Wi‑Fi.
- It retries by itself every 2 minutes. The ⟳ button retries right away.
- The footer shows the error type (for example `SSLHandshakeException`) and the time of the last
  successful update. If it lasts a long time, Open-Meteo's certificate may have changed: see the
  HTTPS part of [docs/06](06-weather.md).

## The Kick app won't load lists or play video

- The error type shows in the top right corner. Press OK on a tab to retry.
- If `HTTP 403`, `HTTP 404` or `JSONException` keeps appearing, Kick may have changed its API. Then
  fix `kick-tv/src/mn/home/kick/Kick.java` ([docs/08](08-kick.md)).
- If the player says "Холболт тасарлаа" (connection lost), it reconnects by itself after 2–30
  seconds. If it says "…одоо шууд дамжуулалт хийхгүй байна" (not live right now), the channel is
  offline. The app checks again every minute.
- If the app crashed, see why with:
  `adb logcat -d | grep -A15 'FATAL EXCEPTION' | grep -A15 mn.home.kick`

## The TV's IP changed

On the TV, Settings → Network shows the new IP. All scripts accept `TV=<new IP>`. To always get the
same IP, add a DHCP reservation for the TV's Wi‑Fi MAC address on your router.

## See the TV's screen on the Mac

```sh
./scripts/screenshot.sh     # PNG → ~/Desktop
```
