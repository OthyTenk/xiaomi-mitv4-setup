# 8. Kick: өөрсдийн app

[English](08-kick.md) · **Монгол**

## Яагаад өөрсдөө хийсэн бэ

2026-10-31-нд шалгасан бэлэн хувилбарууд:

| App | Яагаад болохгүй |
|---|---|
| Kick-ийн албан ёсны Android TV app | **Android 8.0+** (API 26) шаарддаг (APKMirror-ийн мэдээлэл). Энэ зурагт Android 6 (API 23) |
| Kick-ийн утасны app (`com.kick.mobile`) | Android 7+. Мөн remote-оор удирдахад тохиромжгүй |
| SmartKickTV ([CxWatcher/SmartTVKick](https://github.com/CxWatcher/SmartTVKick)) | Android 7+ (minSdk 24). Эх код нь нийтлэгдээгүй, зөвхөн APK байдаг. Native library болон өөрийгөө шинэчлэх эрхтэй |
| Kodi + Kick addon | Найдвартай addon олдсонгүй |

HTTPS-ийн хувьд асуудал байхгүй. Kick-ийн серверүүд энэ зурагтын хуучин root CA-уудтай таардаг тул
"Цаг агаар"-тай адил root certificate багтаах шаардлагагүй:

- `kick.com`, `web.kick.com`, `stream.kick.com`: Google Trust Services → **GlobalSign Root CA** (cross-sign)
- Видео (`*.live-video.net`, Amazon IVS), зураг (`images.kick.com`, `files.kick.com`):
  Amazon → **Starfield Services Root G2**

## Суулгасан хувилбар

| | |
|---|---|
| Package | `mn.home.kick`, хувилбар 1.0. Зурагт дээр **"Kick"** нэрээр харагдана |
| APK | `apks/kick-tv.apk`, ≈2.2MB ([Releases](https://github.com/OthyTenk/xiaomi-mitv4-setup/releases)) |
| Код | [`kick-tv/`](../kick-tv/) |
| Видео тоглуулагч | AndroidX Media3 (ExoPlayer) 1.11.1, HLS. Android 6 (API 23)-г дэмждэг |

## Дэлгэц

Дээд талд 4 таб байна. Remote-ын сумаар таб дээр очиход тэр таб шууд нээгдэнэ.

| Таб | Юу харуулах вэ |
|---|---|
| **Дуртай** | Нэмсэн сувгууд. Шууд дамжуулж буй нь эхэнд (үзэгчдийн тоогоор), offline нь араас нь |
| **Монгол** | Монгол хэл дээрх бүх шууд дамжуулалт (Kick-ийн `language=mn`), үзэгчдийн тоогоор |
| **Бүх улс** | Дэлхийн хамгийн их үзэгчтэй 48 дамжуулалт |
| **Хайх** | Сувгийн нэрээр хайна. Бичиж байхад өөрөө хайна, keyboard дээрх "Search/Done" дарахад илэрц рүү шилжинэ |

Карт бүр дээр зураг, LIVE тэмдэг, үзэгчдийн тоо, гарчиг, сувгийн нэр, тоглоомын ангилал харагдана.
Дуртай сувгийн нэрний өмнө ★ тэмдэг гарна. Дуртай сувгууд энэ зурагт дээр л хадгалагдана
(Kick account-д нэвтрэхгүй).

App асахад дуртай суваг байвал **Дуртай**, үгүй бол **Монгол** таб нээгдэнэ.

### 18+ шүүлтүүр

Kick өөрөө 18+ (`is_mature`) гэж тэмдэглэсэн дамжуулалтыг **Монгол** болон **Бүх улс** табд
харуулдаггүй. Зурагтад хүүхдийн түгжээ (儿童锁) асаалттай байгаа тул ингэж тохируулсан. Хайлт болон
Дуртай табд эдгээр суваг харагдана. Шүүлтүүрийг унтраах бол
`kick-tv/src/mn/home/kick/MainActivity.java` дотор `HIDE_MATURE = false` болгоод дахин build хийнэ.

## Удирдлага

**Жагсаалт:**

| Товч | Үйлдэл |
|---|---|
| Сумнууд | Карт, таб хооронд шилжинэ |
| **OK** | Дамжуулалтыг үзэх |
| **Menu (≡)** эсвэл **OK-г удаан дарах** | Дуртайд нэмэх / хасах |
| Таб дээр **OK** | Жагсаалтыг шинэчлэх. Мөн таб руу буцаж ороход эсвэл тоглуулагчаас гарахад жагсаалт 1 минутаас хуучин бол өөрөө шинэчлэгдэнэ |

**Тоглуулагч:**

| Товч | Үйлдэл |
|---|---|
| **◀ ▶** | Тухайн табын өмнөх / дараагийн шууд дамжуулалт |
| **▲ ▼** | Чанар: автомат → 1080p → 720p → 480p → 360p → 160p |
| **OK** | Мэдээллийн мөр (суваг, гарчиг, үзэгч, хэдэн цаг дамжуулж буй, чанар) |
| **Menu (≡)** эсвэл **OK-г удаан дарах** | Дуртайд нэмэх / хасах |
| **Back** | Жагсаалт руу буцах |

- Чанар анхнаасаа **автомат**. Энэ сүлжээнд ихэвчлэн 1080p60 хүртэл өсдөг.
- Сүлжээ тасарвал 2, 5, 10, 30 секундийн дараа өөрөө дахин холбогдоно.
- Дамжуулалт дууссан эсвэл суваг offline бол минут тутам шалгаж, эхлэхэд нь өөрөө тоглуулна.
- Тоглуулж байхад дэлгэц болон Xiaomi-ийн screensaver асахгүй.

**Байхгүй зүйлс:** чат, Kick account-д нэвтрэх ("Following" жагсаалт), бичлэг (VOD), клип.

## Хэрхэн ажилладаг вэ

- Kick-ийн вебийн API-г ашигладаг. Kick.com сайт өөрөө эдгээрийг дууддаг, нэвтрэх шаардлагагүй:
  - `web.kick.com/api/v1/livestreams?language=mn&sort=viewer_count_desc`: жагсаалт
  - `kick.com/api/v2/channels/<суваг>`: сувгийн мэдээлэл ба `playback_url`
  - `kick.com/api/search?searched_word=<нэр>`: хайлт

  Албан ёсны public API (`api.kick.com`) OAuth token шаарддаг бөгөөд видеоны хаяг өгдөггүй тул
  ашиглаагүй.
- Видео нь Amazon IVS-ийн HLS (1080p60 ≈ 9 Mbps, 720p60, 480p, 360p, 160p). `playback_url`-ийн
  token ≈10 минутын хугацаатай. Тиймээс тоглуулах болон дахин холбогдох бүрт шинээр авдаг.
- Kick API-гаа өөрчилбөл жагсаалт ачаалагдахаа болино (жишээ нь `HTTP 403`, `JSONException`).
  Тэгвэл `kick-tv/src/mn/home/kick/Kick.java`-г засна.

### Шалгалт (2026-10-31)

- Зурагт дээр `app_process`-оор (UI-гүйгээр) app-ын `Kick.java`-г ажиллуулахад Монгол 33, дэлхийн
  48 дамжуулалт ≈1 секундэд ачаалагдсан. 1080p60 видеоны 4.6MB segment ≈1 секундэд татагдсан.
- Зурагтын Amlogic hardware decoder (`OMX.amlogic.avc.decoder.awesome`) 1920×1080 60fps-ийг
  алдагдалгүй тоглуулсан (`/sys/class/vdec/vdec_status`: drop count 0).
- Дэлгэцийн зураг (`scripts/screenshot.sh`) авахад видео хар харагдана. Энэ нь Amlogic-ийн видео
  давхарга screenshot-д ордоггүйтэй холбоотой, хэвийн.

## Build хийж суулгах

JDK 17 болон Android SDK (build-tools, platform 35) хэрэгтэй. Gradle ашиглахгүй.

```sh
cd kick-tv
./build.sh              # → build/kick-tv.apk
./build.sh install      # build хийгээд зурагт дээр суулгаж нээнэ (ADB асаалттай байх ёстой)
python3 art.py          # icon, banner-ийг дахин зурна (Pillow хэрэгтэй)
```

- Анх build хийхэд Media3 ба Guava-г Maven-аас `kick-tv/libs/`-д татаж, `build.sh` дахь SHA-256-аар
  шалгана. Дараагийн build-үүд интернетгүй ажиллана.
- Зурагт дээр суулгахад ≈2 минут болно. Android 6 app-ыг суулгах үедээ бүрэн compile хийдэг.
- Шинэ хувилбар гаргахдаа `AndroidManifest.xml`-ийн `versionCode`-ийг нэмэгдүүлнэ. Шинэ build-ийг
  `apks/kick-tv.apk` руу хуулж, `apks/SHA256SUMS.txt`-ийг шинэчилнэ.
- Тодорхой сувгийг шууд нээх (шалгахад хэрэгтэй):
  `adb shell am start -n mn.home.kick/.PlayerActivity -e slug kesharp`

## ⚠️ Keystore

`kick-tv/keystore.jks` болон `keystore.pass` нь app-ын signing key. Устгавал шинэ build хуучин app-ын
дээр суухгүй. Тэгвэл `adb uninstall mn.home.kick` хийгээд шинээр суулгана (дуртай сувгууд арилна).
Эдгээр файлыг git эсвэл олон нийтийн газар бүү байршуул.

## Кодын бүтэц

| Файл | Үүрэг |
|---|---|
| `AndroidManifest.xml` | Package, LAUNCHER + LEANBACK_LAUNCHER, banner, INTERNET |
| `src/…/MainActivity.java` | Табууд, картууд, хайлт, дуртай, 18+ шүүлтүүр |
| `src/…/PlayerActivity.java` | ExoPlayer, суваг солих, чанар, дахин холбогдох, мэдээллийн мөр |
| `src/…/Kick.java` | Kick API (жагсаалт, суваг, хайлт), HTTP |
| `src/…/Favorites.java` | Дуртай сувгууд (SharedPreferences) |
| `src/…/Images.java` | Зураг татах, санах ойд хадгалах |
| `res/layout/main.xml`, `card.xml`, `tab.xml`, `player.xml` | 960×540dp-д тааруулсан layout |
| `res/drawable/*.xml` | Картын focus хүрээ, таб, badge, тоглуулагчийн сүүдэр |
| `res/drawable-xhdpi/icon.png`, `banner.png` | `art.py`-ийн зурсан icon, TV banner |
| `build.sh` | Сан татах → aapt2 → javac → d8 → zipalign → apksigner |
| `libs/` | Media3 1.11.1, Guava 33.3.1 (build.sh татна) |
