# Цаг агаар (TV)

Зочны өрөөний Xiaomi зурагтад (Android 6, 1080p, Google service-гүй) зориулсан Улаанбаатарын цаг
агаарын app. Бүх мэдээлэл нэг дэлгэцэнд багтана: одоогийн цаг агаар, агаарын чанар (PM2.5 / US AQI),
ойрын 24 цаг, 7 хоног. Мэдээллийг [Open-Meteo](https://open-meteo.com)-оос авна. 30 минут тутамд
өөрөө шинэчлэгдэх ба ⟳ товч (remote-ын OK) дарахад шууд шинэчлэгдэнэ.

Дэлгэрэнгүй тайлбар, кодын бүтэц, өөр хот болгох заавар: [../docs/06-tsag-agaar.md](../docs/06-tsag-agaar.md).

- Агаарын чанар нь сүүлийн 12 цагийн PM2.5-ийн EPA NowCast-аас (2024 оны хязгаараар) тооцсон US AQI
  юм. Open-Meteo-ийн `us_aqi` 24 цагийн дунджаар тооцогддог тул оройн утааг хэдэн цаг хоцорч
  харуулдаг.
- Зурагтын фонтод цаг агаарын emoji байхгүй тул icon-уудыг `WeatherIcon` кодоор зурдаг.
- Android 6 Open-Meteo-ийн ашигладаг Let's Encrypt root-ыг таньдаггүй. Тиймээс `res/raw/`-д
  ISRG Root X1/X2 багтсан (letsencrypt.org-оос татсан, SHA-256 `96:BC:EC:06…`, `69:72:9B:8E…`).

## Build хийх ба суулгах

JDK 17 болон Android SDK (build-tools, platform 35) хэрэгтэй. Gradle ашиглахгүй.

```sh
./build.sh            # → build/tsag-agaar-tv.apk
./build.sh install    # ADB-ээр зурагт дээр суулгаж нээнэ (TV=<ip>, default 192.168.1.13)
python3 art.py        # icon, banner-ийг дахин зурна (Pillow хэрэгтэй)
```

⚠️ `keystore.jks`, `keystore.pass`-ийг бүү устга. Шинэчлэлийг заавал ижил key-гээр sign хийх ёстой.
