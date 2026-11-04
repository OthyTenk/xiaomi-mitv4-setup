# 6. Weather

**English** · [Монгол](06-weather.mn.md)

## Breezy Weather (tried, then removed)

- Breezy Weather 6.2.2 (open source, GitHub). For Ulaanbaatar it suggested **NAMEM** (Mongolia's
  National Agency for Meteorology and Environmental Monitoring), but that source didn't work. The
  weather.gov.mn site uses Sectigo's new "Public Server Authentication Root R46" root. Android 6
  doesn't know this root, and Breezy bundles only the Let's Encrypt roots (ISRG X1/X2).
- Switching to Open-Meteo worked. But the layout is made for phones, so on the TV the bottom of the
  7-day forecast ran off the screen. It was removed.
- APK: `apks/breezy-weather-armeabi-v7a-v6.2.2_standard.apk` ([where to get it](../apks/README.md)).
  To install it: `./scripts/install-apps.sh --breezy`.

**YoWindow TV** was also considered. It's made for TVs and supports Android 6+. But its APK is only
on RuStore, and its servers use Let's Encrypt, so it would likely fail on this TV.

## "Цаг агаар" (Weather): our own app

Code: [`tsag-agaar-tv/`](../tsag-agaar-tv/). Package `mn.home.tsagagaar`, version 1.1, ≈38KB.
It appears on the TV as **"Цаг агаар"**. The interface is in Mongolian.

### Screen

Everything fits on one screen, with no scrolling:

- **Top:** Ulaanbaatar, the ⟳ refresh button, the time, the date ("10-р сарын 31, Бямба гараг").
- **Current weather:** icon, temperature, description (cloudy, snow…), feels-like temperature,
  wind (m/s), humidity, today's day/night temperatures.
- **Air quality:** AQI number, PM2.5 (µg/m³), category. The card color follows the category: green
  (Good), yellow (Moderate), orange (Unhealthy for sensitive groups), red (Unhealthy), purple (Very
  unhealthy), maroon (Hazardous).
- **Next 24 hours:** 8 cells, one every 3 hours.
- **7 days:** Today, Tomorrow, Monday… Each day has an icon, max/min temperature and the chance of
  precipitation.
- At night the background turns dark.

### Controls

- **OK** on the remote presses the focused ⟳ button. The icon spins while it refreshes.
- It refreshes by itself every 30 minutes. After a network error it retries in 2 minutes.
- The last data is cached, so it shows right away when the app opens.

### Data source and calculations

- [Open-Meteo](https://open-meteo.com): forecast and air-quality APIs, free, no key needed.
- The **AQI** doesn't come from Open-Meteo's `us_aqi` value. `us_aqi` uses a 24-hour average, so
  hours after the morning smog it still reads "Moderate". Instead, the app calculates the
  **EPA NowCast** from the last 12 hours of PM2.5 and converts it to AQI with the EPA's 2024
  breakpoints. AirNow calculates it the same way. It reacts quickly when the evening smog builds up.
- **HTTPS:** Android 6 doesn't know the Let's Encrypt roots, so the app bundles ISRG Root X1/X2
  (`res/raw/`, downloaded from letsencrypt.org, SHA-256 `96:BC:EC:06…`, `69:72:9B:8E…`). When
  checked, all connections went over port 443. The HTTP fallback is used only if the certificate
  check fails.
- The icons are drawn in code (`WeatherIcon.java`). The TV's fonts have no weather emoji, so the
  first version showed empty boxes.

### Build and install

You need JDK 17 and the Android SDK (build-tools, platform 35). Gradle isn't used.

```sh
cd tsag-agaar-tv
./build.sh              # → build/tsag-agaar-tv.apk
./build.sh install      # build, then install and open it on the TV (ADB must be on)
python3 art.py          # redraw the icon and banner (needs Pillow)
```

For a new version, increase `versionCode` in `AndroidManifest.xml`. Copy the new build to
`apks/tsag-agaar-tv.apk` and `install-apps.sh` installs the new version.

### Another city

1. `src/mn/home/tsagagaar/MainActivity.java`: the latitude/longitude in `COORDS`, and if needed
   `Asia%2FUlaanbaatar` and `TZ`.
2. `res/layout/main.xml`: the text "Улаанбаатар".
3. `./build.sh install`

### ⚠️ Keystore

`keystore.jks` and `keystore.pass` are the app's signing key. If they're deleted or lost, a new
build won't install over the old app. Then you have to run `adb uninstall mn.home.tsagagaar` first and
install it fresh. Only the cached forecast is lost. Never put these files in git or anywhere public.

### Code layout

| File | Role |
|---|---|
| `AndroidManifest.xml` | Package, LAUNCHER + LEANBACK_LAUNCHER, banner, INTERNET |
| `src/…/MainActivity.java` | Fetching data, cache, NowCast/AQI, drawing the screen, clock, refresh button |
| `src/…/WeatherIcon.java` | Sun, moon, cloud, fog, rain, snow and thunder icons |
| `res/layout/main.xml` | Layout tuned for 960×540dp |
| `res/drawable/ic_refresh.xml`, `round_button.xml` | ⟳ icon (Material), round button with focus state |
| `res/drawable-xhdpi/icon.png`, `banner.png` | Icon and TV banner drawn by `art.py` |
| `res/raw/isrg_root_x1.pem`, `isrg_root_x2.pem` | Let's Encrypt root certificates |
| `build.sh` | aapt2 → javac → d8 → zipalign → apksigner |
