#!/bin/zsh
# Aerial launcher-ийг суулгаж, Home Guard-ыг асаана. Android 6 Xiaomi-д тохируулсан:
# энэ хувилбарт `cmd` команд байхгүй тул notification listener болон accessibility-г settings-ээр бичнэ.
#   ./aerial-setup.sh           суулгаад тохируулна
#   ./aerial-setup.sh repair    дахин суулгахгүй, зөвхөн эрх/тохиргоог сэргээнэ (HOME PatchWall-д үлдвэл)
#   APK=~/Downloads/aerial-standard.apk ./aerial-setup.sh   шинэ хувилбар суулгах
# АНХААР: энэ нь өмнө нь асаалттай байсан бусад accessibility service-ийг (караоке, 小爱) солино.
set -euo pipefail
cd "${0:A:h}"
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
P=app.orchard.tv
APK="${APK:-../apks/aerial-standard-1.75.apk}"

[ "${1:-}" = repair ] || adb install -r "$APK"

adb shell pm grant $P android.permission.WRITE_SECURE_SETTINGS     # Home Guard-ыг өөрөө дахин асаахад
adb shell pm grant $P android.permission.ACCESS_FINE_LOCATION || true
for op in SYSTEM_ALERT_WINDOW WRITE_SETTINGS GET_USAGE_STATS; do  # асах үед нээгдэх, гэрэлтүүлэг, урд байгаа app-ыг мэдэх
  adb shell appops set $P $op allow
done
adb shell settings put secure enabled_notification_listeners "$P/$P.guard.RevivalListener"
adb shell settings put secure enabled_accessibility_services "$P/$P.guard.HomeGuardService"
adb shell settings put secure accessibility_enabled 1
adb shell am start -n $P/.MainActivity >/dev/null

sleep 3
if adb shell dumpsys accessibility | grep -q "Aerial Home Guard"; then
  echo "Home Guard ажиллаж байна. HOME дарахад PatchWall 1–3 сек гараад Aerial руу буцна."
else
  echo "Home Guard асаагүй байна. Хэдэн секундын дараа './aerial-setup.sh repair' ажиллуулна уу." >&2
  exit 1
fi
