# Kick (TV)

Зочны өрөөний Xiaomi зурагтад (Android 6, 1080p, Google service-гүй) зориулсан Kick-ийн app.
Kick-ийн албан ёсны TV app Android 8+ шаарддаг тул өөрсдөө хийсэн. Табууд: **Дуртай**, **Монгол**
(Монгол хэл дээрх бүх шууд дамжуулалт), **Бүх улс**, **Хайх**. Remote-ын OK-оор үзэж, ◀ ▶-оор суваг
сольж, ▲ ▼-оор чанар сонгоно. Menu (≡) эсвэл OK-г удаан дарвал дуртайд нэмнэ / хасна.

Дэлгэрэнгүй тайлбар, удирдлага, кодын бүтэц: [../docs/08-kick.mn.md](../docs/08-kick.mn.md) ([English](../docs/08-kick.md)).

- Kick-ийн вебийн API-г (нэвтрэхгүйгээр) ашигладаг. Kick үүнийгээ өөрчилбөл `src/mn/home/kick/Kick.java`-г засна.
- Видеог AndroidX Media3 (ExoPlayer) 1.11.1 тоглуулдаг. Энэ нь Android 6 (API 23)-г дэмждэг.
- Kick-ийн 18+ гэж тэмдэглэсэн дамжуулалтууд Монгол, Бүх улс табд харагдахгүй (`HIDE_MATURE`).
- Kick-ийн серверүүд зурагтын хуучин root CA-тай таардаг тул root certificate багтаагаагүй.

## Build хийх ба суулгах

JDK 17 болон Android SDK (build-tools, platform 35) хэрэгтэй. Gradle ашиглахгүй.

```sh
./build.sh            # → build/kick-tv.apk (анх удаа Media3, Guava-г libs/-д татаж SHA-256-аар шалгана)
./build.sh install    # ADB-ээр зурагт дээр суулгаж нээнэ (TV=<ip>, default 192.168.1.13)
python3 art.py        # icon, banner-ийг дахин зурна (Pillow хэрэгтэй)
```

⚠️ `keystore.jks`, `keystore.pass`-ийг бүү устга. Шинэчлэлийг заавал ижил key-гээр sign хийх ёстой.
