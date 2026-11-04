# 4. YouTube: SmartTube

**English** · [Монгол](04-smarttube.mn.md)

| | |
|---|---|
| Version | 32.68 stable, armeabi-v7a (`org.smarttube.stable`) |
| Source | [github.com/yuliskov/SmartTube/releases](https://github.com/yuliskov/SmartTube/releases) (official) |
| APK | `apks/SmartTube_stable_32.68_armeabi-v7a.apk` ([where to get it](../apks/README.md)) |
| SHA-256 | `2130b23b17ed5cfd324baec81750a2be22059212b6ee17449f21cae2cb3a60d9` |
| Signature | `CN=Yurii L, O=SmartTube` (SHA-256 `109735aa…d8`). The developer's official key |

- YouTube for TVs, without ads. It doesn't need Google Play Services.
- **Sign in:** SmartTube → Settings → Accounts → Add account. Enter the code shown on the TV at
  **youtube.com/activate** on your phone.
- **Update:** download `SmartTube_stable_<version>_armeabi-v7a.apk` from the GitHub releases and run
  `adb install -r <file>`. The in-app updater may not work on this old Android.
- ⚠️ The TV's apps are 32-bit, so install only the **armeabi-v7a** build. The arm64 build won't run.
- The 2015 phone YouTube app (`com.google.android.youtube` 10.16) didn't work, so it was removed.
  Backup: `backup/apk/com.google.android.youtube.apk`.
