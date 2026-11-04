#!/bin/zsh
# Aerial-ийн Home Guard-ыг унтрааж, HOME товчийг PatchWall-д буцааж өгнө.
#   ./aerial-restore.sh              Aerial суусан хэвээр үлдэнэ
#   ./aerial-restore.sh --uninstall  Aerial-ийг бас устгана
set -u
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
P=app.orchard.tv

# Эхлээд Aerial-ийн өөрийгөө дахин асаах боломжийг хаана.
adb shell am broadcast -a "$P.CONFIG" -n "$P/.guard.ConfigReceiver" --ez keep_adb false >/dev/null 2>&1
adb shell pm revoke $P android.permission.WRITE_SECURE_SETTINGS 2>/dev/null
for op in SYSTEM_ALERT_WINDOW WRITE_SETTINGS GET_USAGE_STATS; do
  adb shell appops set $P $op default
done
adb shell settings delete secure enabled_accessibility_services
adb shell settings delete secure enabled_notification_listeners
adb shell am force-stop $P

[ "${1:-}" = --uninstall ] && adb uninstall $P
adb shell input keyevent KEYCODE_HOME
echo "HOME одоо PatchWall руу очно."
