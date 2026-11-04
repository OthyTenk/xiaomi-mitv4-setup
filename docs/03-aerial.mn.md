# 3. Aerial launcher (PatchWall-ийн оронд)

[English](03-aerial.md) · **Монгол**

## Яагаад Aerial вэ

- Энэ firmware PatchWall-ийг (`com.mitv.tvhome`) унтраах, нуухыг хориглодог (`pm disable-user`,
  `pm hide` → Permission denied). PatchWall HOME товчийг өндөр priority-тойгоор (100) үргэлж
  өөртөө авдаг. Тиймээс FLauncher, ATV Launcher гэх мэт энгийн launcher default болж чадахгүй.
- [Aerial](https://github.com/pengchujin/aerial-tv) нь Хятадын зурагтад зориулагдсан (MIT license,
  ~8k star). Түүний **Home Guard** accessibility service HOME дарахад PatchWall-ийг таньж, Aerial-ийг
  урд нь гаргадаг. Mi TV 4 цувралын Android 6 дээр ажилладаг гэж тэмдэглэгдсэн.

## Суулгасан хувилбар

| | |
|---|---|
| Хувилбар | 1.75 (2026-10-31), package `app.orchard.tv` |
| APK | `apks/aerial-standard-1.75.apk` ([хаанаас татах](../apks/README.md#монгол)) |
| SHA-256 | `9fa0e909052895d945791bbc9c34320bb79f59f77ea4012ac096e78db74a48e2` |
| Signature | **"Android Debug"** key (SHA-256 `e08962dc…6685`). Release key биш, community app |

## Суулгах ба дахин тохируулах

```sh
./scripts/aerial-setup.sh            # суулгах + эрх олгох + Home Guard асаах
./scripts/aerial-setup.sh repair     # HOME PatchWall дээр гацаад Aerial гарахгүй бол
```

Android 6-д `cmd` команд байхгүй тул script эдгээрийг гараар хийдэг:

| Тохиргоо | Яагаад |
|---|---|
| `pm grant … WRITE_SECURE_SETTINGS` | Home Guard унтарвал Aerial өөрөө дахин асаана |
| `appops SYSTEM_ALERT_WINDOW / WRITE_SETTINGS / GET_USAGE_STATS = allow` | Асах үед нээгдэх, гэрэлтүүлэг тохируулах, урд байгаа app-ыг мэдэх (Xiaomi Android 6 дээр PatchWall accessibility event илгээдэггүй) |
| `enabled_notification_listeners = …RevivalListener` | Xiaomi standby-д Aerial-ийг хаавал буцааж сэргээнэ |
| `enabled_accessibility_services = …HomeGuardService`, `accessibility_enabled = 1` | Home Guard |

⚠️ Aerial-ийн албан ёсны `curl … | bash` installer-ийг ашиглаагүй. Учир нь тэр installer Android 6-д
байхгүй `cmd` командуудыг ашигладаг, мөн интернетээс script татаж шууд ажиллуулах нь эрсдэлтэй.
⚠️ Энэ тохиргоо өмнө асаалттай байсан бусад accessibility service-ийг (караоке, 小爱) солино.
Тэдгээр app-ууд устгагдсан тул энэ нь асуудал үүсгэхгүй.

## Хэрэглээ

- HOME дарахад PatchWall **1–3 секунд** гараад Aerial руу буцна. Энэ firmware дээр хэвийн.
- Aerial-ийн app жагсаалтаас SmartTube болон "Цаг агаар"-ыг нүүр дэлгэцэнд pin хий.
- **Mi Box S:** HDMI оролтыг pin хийгээд нэр, icon-ыг нь өгнө. Энэ firmware-ийн (22.4) HDMI
  дугаарлалт: HDMI1=23, HDMI2=24, HDMI3=25, AV=2. Эдгээр нь
  `com.xiaomi.mitv.tvplayer/.ExternalSourceActivity` activity-ийн int extra `input`-ийн утгууд.
- Aerial нүүр дэлгэцэд цаг агаар харуулдаг. Хотыг IP хаягаар тодорхойлдог тул буруу хот
  харагдвал wallpaper цэс дэх QR кодыг утсаараа уншуулж "Ulaanbaatar" гэж тохируулна.
- **Шинэчлэх:** шинэ APK-г [GitHub release](https://github.com/pengchujin/aerial-tv/releases)-ээс
  татаад `APK=<файл> ./scripts/aerial-setup.sh` ажиллуулна.

## Буцаах

```sh
./scripts/aerial-restore.sh               # HOME → PatchWall, Aerial суусан хэвээр үлдэнэ
./scripts/aerial-restore.sh --uninstall   # Aerial-ийг бас устгана
```

## Анхаарах

- Aerial-д accessibility, system settings бичих зэрэг өргөн эрх өгсөн.
- PatchWall-ийн хүүхдийн түгжээ (儿童锁) Aerial-ээс үл хамааран ажиллана. Home Guard түүнийг
  алгасдаггүй нь зөв ажиллагаа юм.
- Xiaomi-ийн screensaver (`com.mitv.screensaver`) хэвээр байгаа тул "天津 河西" (Тяньжин)-ий цаг
  агаарыг харуулж магадгүй.
