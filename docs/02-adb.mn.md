# 2. ADB: зурагтыг компьютерээс удирдах

[English](02-adb.md) · **Монгол**

## ADB асаах (зурагт дээр)

1. Settings → About → **Model** мөрөн дээр remote-ын OK-г 7 удаа дарна. "Developer mode" гэсэн
   мэдэгдэл гарна.
2. Settings → **Account & security** → **ADB debugging** → **Allow**. Энэ цэс зөвхөн developer
   mode нээгдсэний дараа гарч ирнэ.
3. Мөн тэнд **Install unknown apps** → **Allow**.

> ⚠️ Xiaomi ADB-г **standby болон дахин асах бүрт унтраадаг**. Холбогдохгүй бол эхлээд энэ
> тохиргоог шалга. Reboot-ийн дараа заримдаа Disallow → Allow гэж дахин сэлгэх шаардлагатай.

## Mac-аас холбогдох

```sh
./scripts/adb-connect.sh
# эсвэл гараар:
adb connect 192.168.1.13:5555
adb devices -l        # "device" гэж гарвал бэлэн
```

Анх холбогдоход зурагт дээр **"Allow USB debugging?"** цонх гарна. **Always allow from this
computer**-ийг чагтлаад OK дарна.

## ⚠️ USB кабель бүү ашигла

Зурагтын USB port-ууд host горимд ажилладаг тул MacBook-тэй холбоход Mac ямар ч төхөөрөмж
харахгүй. Харин зурагт асахдаа USB flash горим руу орох гэж оролдоод **Mi logo дээр асаж унтраад
давтана**. Ийм зүйл болвол кабелийг салгаад, цахилгаанаас 1 минут салгаж, дахин асаана.

## Түгээмэл асуудал

| Шинж | Шалтгаан | Шийдэл |
|---|---|---|
| `Connection refused` | ADB унтарсан | Зурагт дээр ADB debugging-ийг Allow болгоно |
| `No route to host`, гэхдээ `nc -z 192.168.1.13 5555` ажилладаг | Android Studio-оос эхэлсэн хуучин adb server-т macOS-ийн Local Network эрх байхгүй | `adb kill-server && adb start-server` (`adb-connect.sh` үүнийг автоматаар хийдэг) |
| `unauthorized` | Зурагт дээрх цонхонд OK дараагүй | Зурагт дээр OK дарна. Цонх гараагүй бол `adb disconnect`-ийн дараа дахин connect |
| Зурагт олдохгүй | IP өөрчлөгдсөн (DHCP) | Зурагт: Settings → Network-аас IP-г харна. `TV=<шинэ IP> ./adb-connect.sh` |

## Хэрэгтэй командууд

```sh
export ANDROID_SERIAL=192.168.1.13:5555
adb shell getprop ro.product.model            # загвар
adb shell pm list packages                     # суусан app-ууд
adb install -r app.apk                         # app суулгах эсвэл шинэчлэх
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png   # дэлгэцийн зураг
adb shell input keyevent KEYCODE_HOME          # remote-ын товч: HOME, BACK, DPAD_CENTER (OK)…
adb shell input tap 960 540                    # дэлгэцэд дарах (1920×1080 координат)
adb shell uiautomator dump /sdcard/ui.xml      # дэлгэц дээрх текст, товчнуудын бүтэц
adb reboot                                      # дахин асаах
```

Xiaomi-ийн screensaver ердөө 1 минут орчим идэвхгүй байхад асдаг. ADB-ээр дэлгэц удирдаж байхад
screensaver асвал `adb shell am start -n <package>/<activity>`-ээр app-аа урд нь гаргана.

## Аюулгүй байдал

- Зурагт `ro.adb.secure=1` тул шинэ компьютер холбогдох бүрт зурагт дээр зөвшөөрөл асууна.
- Гэсэн ч ADB нээлттэй үед дотоод сүлжээнээс холбогдох оролдлого хийх боломжтой. Тиймээс ажлаа
  дуусгаад **ADB debugging = Disallow** болго.
- Бүх 65,535 port-ыг скан хийх хэрэггүй. Зурагтын сул service-үүдэд ачаалал өгнө.
