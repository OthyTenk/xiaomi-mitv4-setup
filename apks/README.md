# APKs

**English** · [Монгол](#монгол)

APK files are not stored in git. Download them into this folder under the names below; the
scripts in [`../scripts/`](../scripts/) look for exactly these names. Check them with
`shasum -a 256 -c SHA256SUMS.txt` (only for the versions listed there).

| File | What | Where to get it |
|---|---|---|
| `aerial-standard-1.75.apk` | Aerial launcher | [pengchujin/aerial-tv releases](https://github.com/pengchujin/aerial-tv/releases) |
| `SmartTube_stable_32.68_armeabi-v7a.apk` | YouTube (SmartTube) | [yuliskov/SmartTube releases](https://github.com/yuliskov/SmartTube/releases). Get the **armeabi-v7a** build; arm64 won't run on this TV |
| `tsag-agaar-tv.apk` | Weather app (this repo) | [Releases of this repo](https://github.com/OthyTenk/xiaomi-mitv4-setup/releases), or build it: `../tsag-agaar-tv/build.sh` |
| `kick-tv.apk` | Kick app (this repo) | [Releases of this repo](https://github.com/OthyTenk/xiaomi-mitv4-setup/releases), or build it: `../kick-tv/build.sh` |
| `breezy-weather-armeabi-v7a-v6.2.2_standard.apk` | Breezy Weather (optional, `install-apps.sh --breezy`) | [breezy-weather releases](https://github.com/breezy-weather/breezy-weather/releases) |

A newer Aerial can be installed with `APK=<file> ../scripts/aerial-setup.sh`. For other files,
rename the download to the name above or run `adb install -r <file>` yourself.

## Монгол

APK файлууд git-д хадгалагддаггүй. Дээрх хүснэгтэд байгаа нэрээр нь энэ хавтаст татаж хадгална.
[`../scripts/`](../scripts/) доторх script-үүд яг эдгээр нэрийг хайдаг. Татсан файлаа
`shasum -a 256 -c SHA256SUMS.txt`-ээр шалгана (зөвхөн тэнд бичигдсэн хувилбаруудад).

- **Aerial**, **SmartTube**, **Breezy Weather**: хүснэгтэд байгаа албан ёсны GitHub release-ээс татна.
  SmartTube-ийн **armeabi-v7a** хувилбарыг л ав, arm64 энэ зурагт дээр ажиллахгүй.
- **"Цаг агаар"**, **Kick**: энэ repo-ийн [Releases](https://github.com/OthyTenk/xiaomi-mitv4-setup/releases)-ээс
  татна, эсвэл `../tsag-agaar-tv/build.sh`, `../kick-tv/build.sh`-ээр өөрөө build хийнэ.

Aerial-ийн шинэ хувилбарыг `APK=<файл> ../scripts/aerial-setup.sh`-ээр суулгана. Бусад файлыг
дээрх нэрээр нь солих эсвэл `adb install -r <файл>`-ээр шууд суулгана.
