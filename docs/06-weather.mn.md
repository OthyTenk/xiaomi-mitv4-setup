# 6. Цаг агаар

[English](06-weather.md) · **Монгол**

## Breezy Weather (туршаад устгасан)

- Breezy Weather 6.2.2 (open source, GitHub). Улаанбаатарт **NAMEM** (Цаг уур, орчны шинжилгээний
  газар)-ийг санал болгосон ч ажиллаагүй. Учир нь weather.gov.mn сайт Sectigo-гийн шинэ
  "Public Server Authentication Root R46" root-ыг ашигладаг. Android 6 энэ root-ыг таньдаггүй,
  Breezy ч зөвхөн Let's Encrypt-ийн root-уудыг (ISRG X1/X2) өөртөө агуулдаг.
- Open-Meteo руу шилжүүлэхэд ажилласан. Гэхдээ утсанд зориулсан layout учир TV дээр 7 хоногийн
  мэдээний доод хэсэг дэлгэцнээс хальсан тул устгасан.
- APK: `apks/breezy-weather-armeabi-v7a-v6.2.2_standard.apk` ([хаанаас татах](../apks/README.md#монгол)).
  Суулгах бол `./scripts/install-apps.sh --breezy`.

**YoWindow TV**-г бас судалсан. TV-д зориулсан, Android 6+ дэмждэг. Гэхдээ APK нь зөвхөн RuStore дээр
байдаг бөгөөд серверүүд нь Let's Encrypt ашигладаг тул энэ зурагт дээр ажиллахгүй байх эрсдэлтэй.

## "Цаг агаар": өөрсдийн app

Код: [`tsag-agaar-tv/`](../tsag-agaar-tv/). Package `mn.home.tsagagaar`, хувилбар 1.1, ≈38KB.
Зурагт дээр **"Цаг агаар"** нэрээр харагдана.

### Дэлгэц

Бүх мэдээлэл нэг дэлгэцэнд багтана, гүйлгэх шаардлагагүй:

- **Дээд хэсэг:** Улаанбаатар, ⟳ шинэчлэх товч, цаг, огноо ("10-р сарын 31, Бямба гараг").
- **Одоогийн цаг агаар:** icon, температур, тайлбар (Үүлэрхэг, Цас…), мэдрэгдэх температур,
  салхи (м/с), чийгшил, өнөөдрийн өдөр/шөнийн температур.
- **Агаарын чанар:** AQI тоо, PM2.5 (мкг/м³), ангилал. Картын өнгө ангиллаараа солигдоно: ногоон
  (Сайн), шар (Дунд зэрэг), улбар шар (Мэдрэг хүмүүст эрүүл бус), улаан (Эрүүл бус), ягаан
  (Маш эрүүл бус), хүрэн (Аюултай).
- **Ойрын 24 цаг:** 3 цаг тутмын 8 нүд.
- **7 хоног:** Өнөөдөр, Маргааш, Даваа… өдөр бүрд icon, хамгийн их/бага температур, тунадасны
  магадлал.
- Шөнө арын өнгө харанхуй болно.

### Удирдлага

- Remote-ын **OK** нь focus-тай байгаа ⟳ товчийг дарна. Шинэчлэгдэх үед icon эргэлдэнэ.
- 30 минут тутамд өөрөө шинэчлэгдэнэ. Сүлжээний алдаа гарвал 2 минутын дараа дахин оролдоно.
- Сүүлийн мэдээг хадгалдаг тул app асмагц шууд харагдана.

### Мэдээллийн эх сурвалж ба тооцоо

- [Open-Meteo](https://open-meteo.com): forecast болон air-quality API, үнэгүй, key шаардахгүй.
- **AQI**-г Open-Meteo-ийн `us_aqi` утгаас авдаггүй. `us_aqi` 24 цагийн дунджаар тооцогддог тул
  өглөөний утаа хэдэн цагийн дараа ч "Дунд зэрэг" гэж харагдсаар байдаг. Үүний оронд сүүлийн
  12 цагийн PM2.5-аас **EPA NowCast**-ыг тооцоод, EPA-ийн 2024 оны хязгаараар AQI болгодог.
  AirNow ч ийм аргаар тооцдог. Оройн утаа нэмэгдэхэд хурдан хариу үзүүлнэ.
- **HTTPS:** Android 6 Let's Encrypt-ийн root-ыг таньдаггүй тул app-д ISRG Root X1/X2 багтсан
  (`res/raw/`, letsencrypt.org-оос татсан, SHA-256 `96:BC:EC:06…`, `69:72:9B:8E…`).
  Шалгахад бүх холболт port 443-аар явж байсан. HTTP fallback зөвхөн certificate-ийн алдаа
  гарсан тохиолдолд ажиллана.
- Icon-ууд кодоор зурагддаг (`WeatherIcon.java`). Зурагтын фонтод цаг агаарын emoji байхгүй тул
  анхны хувилбарт дөрвөлжин харагдаж байсан.

### Build хийж суулгах

JDK 17 болон Android SDK (build-tools, platform 35) хэрэгтэй. Gradle ашиглахгүй.

```sh
cd tsag-agaar-tv
./build.sh              # → build/tsag-agaar-tv.apk
./build.sh install      # build хийгээд зурагт дээр суулгаж нээнэ (ADB асаалттай байх ёстой)
python3 art.py          # icon, banner-ийг дахин зурна (Pillow хэрэгтэй)
```

Шинэ хувилбар гаргахдаа `AndroidManifest.xml`-ийн `versionCode`-ийг нэмэгдүүлнэ. Шинэ build-ийг
`apks/tsag-agaar-tv.apk` руу хуулбал `install-apps.sh` шинэ хувилбарыг суулгана.

### Өөр хот болгох

1. `src/mn/home/tsagagaar/MainActivity.java`: `COORDS`-ийн өргөрөг/уртраг, шаардлагатай бол
   `Asia%2FUlaanbaatar` болон `TZ`.
2. `res/layout/main.xml`: "Улаанбаатар" гэсэн бичиг.
3. `./build.sh install`

### ⚠️ Keystore

`keystore.jks` болон `keystore.pass` нь app-ын signing key. Устгасан эсвэл гээсэн тохиолдолд шинэ
build нь хуучин app-ын дээр суухгүй. Тэгвэл эхлээд `adb uninstall mn.home.tsagagaar`, дараа нь шинээр
суулгах хэрэгтэй. Хадгалсан мэдээ л арилна. Эдгээр файлыг git эсвэл олон нийтийн газар бүү байршуул.

### Кодын бүтэц

| Файл | Үүрэг |
|---|---|
| `AndroidManifest.xml` | Package, LAUNCHER + LEANBACK_LAUNCHER, banner, INTERNET |
| `src/…/MainActivity.java` | Мэдээ татах, cache, NowCast/AQI, дэлгэц зурах, цаг, refresh товч |
| `src/…/WeatherIcon.java` | Нар, сар, үүл, манан, бороо, цас, аянгын icon-ууд |
| `res/layout/main.xml` | 960×540dp-д тааруулсан layout |
| `res/drawable/ic_refresh.xml`, `round_button.xml` | ⟳ icon (Material), focus-той дугуй товч |
| `res/drawable-xhdpi/icon.png`, `banner.png` | `art.py`-ийн зурсан icon, TV banner |
| `res/raw/isrg_root_x1.pem`, `isrg_root_x2.pem` | Let's Encrypt-ийн root certificate-ууд |
| `build.sh` | aapt2 → javac → d8 → zipalign → apksigner |
