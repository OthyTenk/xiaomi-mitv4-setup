#!/bin/zsh
# Зурагтын үндсэн мэдээлэл ба одоогийн төлөвийг харуулна. Юу ч өөрчлөхгүй.
#   ./tv-info.sh
set -u
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
p() { adb shell getprop "$1" | tr -d '\r'; }

echo "Model:        $(p ro.product.model) ($(p ro.product.device), platform $(p ro.mitv.product.platformid))"
echo "Android:      $(p ro.build.version.release) (API $(p ro.build.version.sdk)), build $(p ro.build.display.id)"
echo "Chip:         $(p ro.boot.platform_name), panel $(p ro.boot.mi.panel_vendor) $(p ro.boot.mi.panel_size)\" $(p ro.boot.mi.panel_resolution)"
echo "Uptime:       $(adb shell cat /proc/uptime | awk '{printf "%.0f мин", $1/60}')"
echo "Нээлттэй app: $(adb shell dumpsys activity activities | grep -m1 mResumedActivity | sed 's/.* u0 //; s/ .*//' | tr -d '\r')"
echo "Home Guard:   $(adb shell dumpsys accessibility | grep -q 'Aerial Home Guard' && echo 'ажиллаж байна' || echo 'АЖИЛЛАХГҮЙ байна (scripts/aerial-setup.sh repair)')"
echo "Packages:     $(adb shell pm list packages | wc -l | tr -d ' \r') харагдах"
echo
echo "Суулгасан app-ууд:"
for pkg in app.orchard.tv org.smarttube.stable mn.home.tsagagaar mn.home.kick org.breezyweather cm.aptoidetv.pt; do
  v=$(adb shell dumpsys package "$pkg" | grep -m1 versionName | sed 's/.*=//' | tr -d '\r')
  printf "  %-24s %s\n" "$pkg" "${v:-суугаагүй}"
done
