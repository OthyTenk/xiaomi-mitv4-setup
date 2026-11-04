# 5. Хятад app-ууд: юуг устгасан, юуг үлдээсэн

[English](05-app-removal.md) · **Монгол**

## Арга

Энэ firmware `pm disable-user` болон `pm hide`-ийг хориглодог (Permission Denial). Тиймээс:

| App-ын төрөл | Устгах | Буцаах |
|---|---|---|
| Third-party (`/data/app`) | `pm uninstall <pkg>` бүрэн устгана | Зөвхөн backup APK-аас: `adb install <apk>` |
| System, шинэчлэлттэй | `pm uninstall -k --user 0 <pkg>` (шинэчлэлийг арилгаад нууна, `hidden=true`) | `adb install -r -d --user 0 <backup apk>` |
| System, зөвхөн `/system`-д | `pm uninstall -k --user 0 <pkg>` (`installed=false`) | `pm install -r --user 0 /system/…/<app>.apk` |

⚠️ Энгийн `pm install -r` app-ыг "Success" гэж хэлдэг ч сэргээдэггүй (`installed=false` хэвээр
үлдэнэ). Заавал `--user 0` нэмнэ. [`backup/restore.sh`](../backup/restore.sh) үүнийг зөв хийдэг:

```sh
cd backup
./restore.sh com.xiaomi.voicecontrol    # нэг app буцаах
./restore.sh                             # бүгдийг буцаах
```

Устгахаас өмнө `/data/app` дахь APK бүрийг `backup/apk/` хавтаст хадгалсан (243MB, repo-д ороогүй).
Устгалын лог: `backup/removal-log.txt`. Өмнөх төлөв:
`backup/packages-before.txt`, `backup/settings-before.txt`.

## Устгасан 24 app

### Third-party: бүрэн устгасан

| Package | Юу вэ |
|---|---|
| `com.ktcp.video` | 云视听极光, Tencent-ийн видео үйлчилгээ |
| `com.xm.webcontent` | 电视活动中心, урамшуулал болон зар |
| `com.mitv.shoplugin` | Дэлгүүрийн plugin |
| `com.xiaomi.eetv.litehome` | 办公模式 (оффис горим) |
| `com.mitv.care` | **adcare**, зар сурталчилгаа |
| `com.google.android.youtube` | 2015 оны утасны YouTube 10.16. SmartTube-ээр сольсон |

### System: хэрэглэгчээс нуусан

| Package | Юу вэ |
|---|---|
| `com.miui.systemAdSolution` | **Системийн зар** (AdServer, асах үеийн зар) |
| `com.miui.tv.analytics` | Мэдээлэл цуглуулагч (telemetry) |
| `com.xiaomi.statistic` | Статистик (telemetry) |
| `cleantools.mitv.com.tvcleantools` | Richinfo "MobileService" (telemetry SDK, system UID-ээр ажилладаг) |
| `com.xiaomi.mitv.tvpush.tvpushservice` | Push мэдэгдэл |
| `com.mitv.cloudcontrol` | Xiaomi-ийн алсын тохиргоо (cloud control) |
| `com.xiaomi.mitv.appstore` | Xiaomi app store (Хятад) |
| `com.xiaomi.mitv.shop` | Xiaomi дэлгүүр |
| `com.xiaomi.mitv.payment`, `com.xiaomi.mitv.pay` | Төлбөр |
| `com.xiaomi.gamecenter.sdk.service.mibox` | Тоглоомын төвийн SDK |
| `com.xiaomi.voicecontrol` | 小爱同学 дуут туслах. **Remote-ын дуу хоолойн товч одоо ажиллахгүй** |
| `com.xiaomi.smarthome.tv`, `com.xiaomi.smarthome.tv.service` | Mi Home (ухаалаг гэр) |
| `com.xiaomi.tweather` | Xiaomi цаг агаар |
| `com.xiaomi.mitv.calendar` | Календарь (日历) |
| `com.xiaomi.mitv.karaoke.service` | Караоке |
| `com.xiaomi.screenrecorder` | Дэлгэц бичигч |

`com.xiaomi.mitv.payment`-ийн `/data/app` дахь шинэчлэлийн файл устгахаас өмнө алга болсон байсан.
Тиймээс backup байхгүй, reboot-ийн дараа нэг удаа буцаж гарч ирсэн. Дахин устгасны дараа хэвийн
болсон. Сэргээх шаардлага гарвал `/system` дахь эх хувилбараас сэргээнэ.

## ⚠️ Хөндөж болохгүй app-ууд

| Package | Яагаад |
|---|---|
| `com.mitv.tvhome` | PatchWall. Aerial түүнийг шаарддаг. Устгавал power товч, систем эвдэрч болно |
| `com.xiaomi.mitv.tvplayer`, `com.droidlogic.tvinput`, `mitv.service` | HDMI/AV/TV оролт (**Mi Box S**), TV-ийн үндсэн service |
| `com.xiaomi.mitv.settings`, `com.xiaomi.mitv.providers.settings` | Тохиргоо (ADB асаах цэс энд байдаг) |
| `com.xiaomi.mitv.systemui`, `com.android.systemui` | Дууны түвшин, цэс |
| `com.xiaomi.mitv.remotecontroller.service` | Remote |
| `com.xiaomi.tvqs` | Menu товчны түргэн тохиргоо |
| `com.xiaomi.mitv.tvmanager` | System UID-ээр ажилладаг central service: идэвхтэй app-ыг системд мэдээлж, санах ойг удирддаг |
| `com.xiaomi.mitv.upgrade` | OTA update |
| `com.sohu.inputmethod.sogou.tv` | **Цорын ганц keyboard** |
| `com.android.packageinstaller` | APK суулгах |
| `com.duokan.airkan.tvbox`, `com.xiaomi.upnp`, `com.mi.miplay.mitvupnpsink`, `com.xiaomi.miplay`, `com.xiaomi.mitv.smartshare`, `com.xiaomi.mi_connect_service` | Утаснаас cast, AirPlay, DLNA, "Phone can control TV" |
| `com.xiaomi.mitv.mediaexplorer`, `com.mitv.videoplayer`, `com.mitv.mivideoplayer`, `com.mitv.gallery`, `com.mitv.codec.update` | USB-ээс кино, зураг үзэх, codec |
| `com.xiaomi.account`, `com.xiaomi.account.auth` | Xiaomi account |
| `com.mitv.screensaver`, `com.mitv.alarmcenter`, `com.xiaomi.mibox.lockscreen`, `com.xiaomi.mitv.legal.webview`, `com.xiaomi.mitv.debug`, `com.xiaomi.android.TV.audio` | Screensaver, цагийн тохиргоо, түгжээ болон бусад системийн хэсгүүд |
| `com.android.*`, `com.droidlogic`, `android` | Android-ийн цөм |

## Үлдсэн Хятад хэсгүүд (мэдээлэл болгон)

- PatchWall өөрөө хэвээр бөгөөд HOME дарахад 1–3 секунд харагдана.
- Log-оос **OneTrack** гэх Xiaomi-ийн telemetry SDK-г үлдсэн app-уудын аль нэг дотор илрүүлсэн.
  Энэ нь жишээ нь app устгасан тухай event илгээж байсан.
- Xiaomi screensaver "天津 河西" (Тяньжин)-ий цаг агаарыг харуулдаг.
- Sogou keyboard Хятадын keyboard боловч цорын ганц keyboard тул үлдээсэн.

## Дахин устгах

Жишээ нь factory reset-ийн дараа:

```sh
./scripts/remove-chinese-apps.sh    # баталгаажуулалт асуугаад, backup хийгээд устгана
adb reboot                           # дараа нь шалгана
```
