# 2. ADB: controlling the TV from a computer

**English** · [Монгол](02-adb.mn.md)

## Turn on ADB (on the TV)

1. Settings → About → on the **Model** row, press OK on the remote 7 times. A "Developer mode"
   message appears.
2. Settings → **Account & security** → **ADB debugging** → **Allow**. This menu appears only after
   developer mode is on.
3. In the same menu, set **Install unknown apps** → **Allow**.

> ⚠️ Xiaomi turns ADB **off on every standby and reboot**. If you can't connect, check this setting
> first. After a reboot you sometimes have to switch it Disallow → Allow again.

## Connect from a Mac

```sh
./scripts/adb-connect.sh
# or by hand:
adb connect 192.168.1.13:5555
adb devices -l        # "device" means it's ready
```

The first time you connect, the TV shows **"Allow USB debugging?"**. Tick **Always allow from this
computer** and press OK.

## ⚠️ Don't use a USB cable

The TV's USB ports work in host mode, so a MacBook connected by cable doesn't see any device. Worse,
the TV tries to enter USB flash mode when it boots and **keeps rebooting on the Mi logo**. If that
happens, unplug the cable, unplug the power for 1 minute, and turn the TV on again.

## Common problems

| Symptom | Cause | Fix |
|---|---|---|
| `Connection refused` | ADB is off | Set ADB debugging to Allow on the TV |
| `No route to host`, but `nc -z 192.168.1.13 5555` works | An old adb server started by Android Studio doesn't have macOS Local Network permission | `adb kill-server && adb start-server` (`adb-connect.sh` does this automatically) |
| `unauthorized` | OK wasn't pressed on the TV's prompt | Press OK on the TV. If no prompt appeared, `adb disconnect` and connect again |
| TV not found | The IP changed (DHCP) | On the TV, Settings → Network shows the IP. `TV=<new IP> ./adb-connect.sh` |

## Useful commands

```sh
export ANDROID_SERIAL=192.168.1.13:5555
adb shell getprop ro.product.model            # model
adb shell pm list packages                     # installed apps
adb install -r app.apk                         # install or update an app
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png   # screenshot
adb shell input keyevent KEYCODE_HOME          # remote keys: HOME, BACK, DPAD_CENTER (OK)…
adb shell input tap 960 540                    # tap the screen (1920×1080 coordinates)
adb shell uiautomator dump /sdcard/ui.xml      # text and buttons on the screen
adb reboot                                      # reboot
```

Xiaomi's screensaver starts after only about 1 minute of inactivity. If it starts while you're
controlling the screen over ADB, bring your app back to the front with
`adb shell am start -n <package>/<activity>`.

## Security

- The TV has `ro.adb.secure=1`, so it asks for approval on screen whenever a new computer connects.
- Even so, while ADB is open anyone on the local network can try to connect. When you're done, set
  **ADB debugging = Disallow**.
- Don't scan all 65,535 ports. It puts load on the TV's fragile services.
