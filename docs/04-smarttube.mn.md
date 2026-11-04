# 4. YouTube: SmartTube

[English](04-smarttube.md) · **Монгол**

| | |
|---|---|
| Хувилбар | 32.68 stable, armeabi-v7a (`org.smarttube.stable`) |
| Эх сурвалж | [github.com/yuliskov/SmartTube/releases](https://github.com/yuliskov/SmartTube/releases) (албан ёсны) |
| APK | `apks/SmartTube_stable_32.68_armeabi-v7a.apk` ([хаанаас татах](../apks/README.md#монгол)) |
| SHA-256 | `2130b23b17ed5cfd324baec81750a2be22059212b6ee17449f21cae2cb3a60d9` |
| Signature | `CN=Yurii L, O=SmartTube` (SHA-256 `109735aa…d8`). Хөгжүүлэгчийн албан ёсны key |

- Сурталчилгаагүй, TV-д зориулсан YouTube. Google Play Services шаарддаггүй.
- **Нэвтрэх:** SmartTube → Settings → Accounts → Add account. Дэлгэц дээр гарсан кодыг утаснаасаа
  **youtube.com/activate** хаягаар орж оруулна.
- **Шинэчлэх:** GitHub release-ээс `SmartTube_stable_<хувилбар>_armeabi-v7a.apk`-ийг татаад
  `adb install -r <файл>` ажиллуулна. App доторх updater энэ хуучин Android дээр ажиллахгүй байж
  магадгүй.
- ⚠️ Зурагтын app-ууд 32-bit тул **armeabi-v7a** хувилбарыг л суулга. arm64 хувилбар ажиллахгүй.
- 2015 оны утасны YouTube (`com.google.android.youtube` 10.16) ажиллахгүй байсан тул устгасан.
  Backup: `backup/apk/com.google.android.youtube.apk`.
