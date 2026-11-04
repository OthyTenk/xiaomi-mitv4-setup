# Xiaomi Mi TV 4 (Хятад хувилбар): шинэ launcher, YouTube, заргүй

[English](README.md) · **Монгол**

**Хятадын зах зээлд зориулсан Xiaomi Mi TV 4** (PatchWall / MiTV OS, Android 6.0.1) зурагтыг
Хятадаас гадна ашиглахад тохиромжтой болгох script, заавар, хоёр жижиг TV app. Root эрх авах,
firmware солих шаардлагагүй. Бүгдийг Mac-аас Wi‑Fi ADB-ээр хийнэ.

Үр дүн:

- PatchWall-ийн оронд **[Aerial](https://github.com/pengchujin/aerial-tv)** нүүр дэлгэц (HOME дарахад Aerial нээгдэнэ).
- YouTube-д **[SmartTube](https://github.com/yuliskov/SmartTube)**: сурталчилгаагүй, Google service шаарддаггүй.
- **Зар, мэдээлэл цуглуулагч, зөвхөн Хятадад ажилладаг 24 app**-ыг устгасан эсвэл нуусан. Бүгдийг
  устгахаас өмнө backup хийдэг, буцааж суулгах script-тэй.
- **"Цаг агаар"**: нэг дэлгэцэнд багтсан TV app. Одоогийн цаг агаар, агаарын чанар (PM2.5 NowCast-аас
  тооцсон US AQI), ойрын 24 цаг, 7 хоног. Мэдээллийг Open-Meteo-оос авна. Улаанбаатарт тохируулсан,
  өөр хот болгож болно.
- **Kick**: remote-оор удирдах Kick.com-ийн шууд дамжуулалтын app. Kick-ийн албан ёсны TV app
  Android 8+ шаарддаг. Табууд: Дуртай, Монгол, Бүх улс, Хайх.

## Туршсан зурагт

| | |
|---|---|
| Загвар | Xiaomi Mi TV 4 цуврал, 55" (`MiTV4-ANSM0`, codename `xmen`). **Хятадын зах зээлийн** |
| Chip | Amlogic T962 (TXL), 4 цөмт Cortex-A53. App-ууд **зөвхөн 32-bit** (armeabi-v7a) |
| Санах ой | 2GB RAM / 8GB eMMC |
| Систем | Android 6.0.1, MiTV OS 1.3.128 (build 22.4.15.3235), PatchWall |

Android 6, PatchWall-тэй бусад Хятад Mi TV-үүд ойролцоо байх магадлалтай ч зөвхөн энэ загвар дээр
туршсан. Юм устгахаасаа өмнө `adb shell pm list packages`-ийн гаралтыг
[docs/05](docs/05-app-removal.mn.md)-ийн жагсаалтуудтай тулгаж шалгаарай.

## ⚠️ Хамгийн чухал анхааруулгууд

1. **Зурагтыг компьютертэй USB кабелиар бүү холбо.** Зурагт Mi logo дээр гацаж, асаж унтраад
   давтдаг (USB flash горимд ордог). Холбогдсон бол кабелийг салгаад цахилгаанаас 1 минут салга.
2. **Өөр firmware (Android TV, Global ROM) бүү flash хий.** Энэ загварт тохирох firmware байхгүй,
   зурагт бүр мөсөн эвдэрнэ (brick). Шалтгааныг [docs/01](docs/01-tv.mn.md)-д тайлбарласан.
3. **ADB-г зөвхөн Wi‑Fi-аар ашигла.** Зурагт standby болон дахин асах бүрт ADB-г өөрөө унтраадаг.
   Хэрэгтэй үедээ асаа, ажлаа дуусгаад унтраа.
4. **Хятад app-уудаас зөвхөн жагсаалтад байгааг устга.** HDMI тоглуулагч, тохиргоо, remote,
   keyboard зэргийг устгавал зурагт ажиллахгүй болно ([docs/05](docs/05-app-removal.mn.md)).
5. **Signing key-ээ хадгал.** App бүрийг анх build хийхэд `keystore.jks`, `keystore.pass` үүснэ.
   Эдгээргүйгээр суусан app-ыг шинэчилж чадахгүй, устгаж байж л дахин суулгана. Git-д бүү оруул
   (`.gitignore` аль хэдийн хасдаг).
6. **Factory reset хийвэл** устгасан app-ууд буцаж гарч ирээд, Aerial / SmartTube / Цаг агаар /
   Kick арилна. Доорх "Суулгах" хэсгийн дагуу дахин тохируулна.
7. **Хүүхдийн түгжээ (儿童锁)** PatchWall дотор асаалттай бол HOME дарахад "45 − 12 = ?" гэх мэт
   асуулт гарч болно. Үүнийг тойрох арга энд зориуд байхгүй. Унтраах бол PatchWall → 家长中心.

Бүх үйлдлийг өөрийн эрсдэлээр хийнэ.

## Суулгах (factory reset-ийн дараа ч гэсэн)

ADB (Android platform-tools) суусан macOS хэрэгтэй. Script-үүд zsh болон macOS-ийн `nc`, `stat`-ийг
ашигладаг тул Linux дээр бага зэрэг засах шаардлагатай.

1. Зурагтыг Wi‑Fi-д холбоно.
2. Зурагт: Settings → About → **Model** дээр remote-ын OK-г 7 удаа дарж developer mode-ийг нээнэ.
3. Settings → **Account & security** → **ADB debugging = Allow**, **Install unknown apps = Allow**.
4. Зурагтын IP-г (Settings → Network) олоод `TV` хувьсагчаар өгнө. Default нь `192.168.1.13`.
   ```sh
   export TV=192.168.1.50
   ```
5. APK-уудыг [`apks/`](apks/README.md) хавтаст татна.
6. Ажиллуулна:
   ```sh
   cd scripts
   ./adb-connect.sh           # зурагт дээр "Always allow" чагтлаад OK дарна
   ./remove-chinese-apps.sh   # баталгаажуулалт асууж, backup/apk-д хадгалаад 24 app-ыг устгана/нууна
   ./install-apps.sh          # SmartTube + Цаг агаар + Kick (Kick суухад ≈2 минут)
   ./aerial-setup.sh          # Aerial + Home Guard
   adb reboot                 # асахад Aerial гарч ирэх ёстой
   ```
7. Aerial дээр HDMI оролт, "Цаг агаар", SmartTube, Kick-ийг нүүр дэлгэцэнд pin хийнэ.
8. **ADB debugging**-ийг **Disallow** болгоно.

HOME дарахад PatchWall 1–3 секунд гараад Aerial руу буцна. Энэ firmware дээр хэвийн: PatchWall-ийг
унтраах боломжгүй тул Aerial-ийн Home Guard Aerial-ийг урд нь гаргадаг.

## Өдөр тутмын командууд

```sh
./scripts/tv-info.sh                 # загвар, нээлттэй app, Home Guard, суусан app-ууд (юу ч өөрчлөхгүй)
./scripts/screenshot.sh              # дэлгэцийн зураг → ~/Desktop
./scripts/aerial-setup.sh repair     # HOME дарахад PatchWall дээр гацаад Aerial гарахгүй бол
./scripts/aerial-restore.sh          # HOME-ыг PatchWall-д буцаах (--uninstall бол Aerial-ийг бас устгана)
./backup/restore.sh <package>        # устгасан app-ыг буцааж суулгах
```

## Дэлгэрэнгүй заавар

| # | Сэдэв | Заавар |
|---|---|---|
| 1 | Зурагтын үзүүлэлт; Android TV + Google Play яагаад болохгүй; root CA-г яагаад шинэчилж болохгүй | [docs/01](docs/01-tv.mn.md) |
| 2 | Wi‑Fi ADB, түгээмэл алдаа, хэрэгтэй командууд | [docs/02](docs/02-adb.mn.md) |
| 3 | Android 6 дээрх Aerial ба Home Guard (`cmd` команд байхгүй) | [docs/03](docs/03-aerial.mn.md) |
| 4 | SmartTube | [docs/04](docs/04-smarttube.mn.md) |
| 5 | Юуг устгасан, юуг хөндөж болохгүй, хэрхэн буцаах | [docs/05](docs/05-app-removal.mn.md) |
| 6 | "Цаг агаар": мэдээлэл, NowCast AQI, Let's Encrypt root, өөр хот болгох | [docs/06](docs/06-weather.mn.md) |
| 7 | Асуудал шийдэх: Mi logo гацалт, ADB, HOME, түгжээ, дуу хоолойн товч | [docs/07](docs/07-troubleshooting.mn.md) |
| 8 | Kick: яагаад өөрсдөө хийсэн, удирдлага, Kick API, build | [docs/08](docs/08-kick.mn.md) |

## App-уудыг build хийх

JDK 17 болон Android SDK (build-tools, platform 35) хэрэгтэй. Gradle ашиглахгүй: `build.sh` нь
aapt2 → javac → d8 → zipalign → apksigner-ийг шууд ажиллуулна.

```sh
cd tsag-agaar-tv && ./build.sh         # → build/tsag-agaar-tv.apk
cd kick-tv && ./build.sh               # → build/kick-tv.apk (Media3, Guava-г татаж SHA-256-аар шалгана)
TV=<ip> ./build.sh install             # build хийгээд зурагт дээр суулгаж нээнэ
python3 art.py                         # icon, banner-ийг дахин зурна (Pillow хэрэгтэй)
```

- Анхны build таны өөрийн signing key-г үүсгэнэ. Өөрийн key-гээр sign хийсэн build нь Releases-ийн
  APK-ын дээр суухгүй, тиймээс эхлээд түүнийг устгана.
- **Өөр хотын цаг агаар:** `tsag-agaar-tv/src/mn/home/tsagagaar/MainActivity.java` дахь `COORDS`,
  цагийн бүс, мөн `res/layout/main.xml` дахь хотын нэрийг солино ([docs/06](docs/06-weather.mn.md)).
- **Kick** нь kick.com сайтын өөрийн дууддаг вебийн API-г нэвтрэхгүйгээр ашигладаг. Kick үүнийгээ
  өөрчилбөл жагсаалт ачаалагдахаа болино, тэгвэл `kick-tv/src/mn/home/kick/Kick.java`-г засна.
  Kick-ийн 18+ гэж тэмдэглэсэн дамжуулалтууд Монгол, Бүх улс табд харагдахгүй (`MainActivity.java`
  дахь `HIDE_MATURE`).

## Хавтасны бүтэц

```
├── docs/            дэлгэрэнгүй заавар (01–08), англи (`.md`) ба монгол (`.mn.md`)
├── scripts/         суулгах, засварлах script-үүд
├── apks/            татсан APK-ууд энд орно, SHA256SUMS.txt
├── backup/          restore.sh; remove-chinese-apps.sh APK backup-уудыг энд хадгална
├── info/            өөрчлөлт хийхээс өмнөх package жагсаалт
├── tsag-agaar-tv/   "Цаг агаар" app-ын эх код (Java, гадны сангүй)
└── kick-tv/         "Kick" app-ын эх код (Java + AndroidX Media3 ExoPlayer)
```

Repo-д ороогүй зүйлс: signing key, build-ийн файлууд, бусдын APK (эх сурвалжаас нь татна), Xiaomi-ийн
app-уудын backup, зурагтын serial дугаар болон MAC хаяг.

## License

Энэ repo-ийн код болон заавар [MIT License](LICENSE)-тэй. Aerial, SmartTube, Breezy Weather, Media3,
Guava нь тус тусын license-тэй тусдаа төслүүд. Энэ төсөл Xiaomi, Kick, Aerial болон SmartTube-тэй
холбоогүй.
