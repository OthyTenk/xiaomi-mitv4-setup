#!/bin/zsh
# SmartTube (YouTube), "Цаг агаар", "Kick" app-уудыг суулгана. Factory reset-ийн дараа ашиглана.
#   ./install-apps.sh            SmartTube + Цаг агаар + Kick
#   ./install-apps.sh --breezy   Breezy Weather-ийг бас суулгана
# Aerial-ийг тусад нь: ./aerial-setup.sh
set -euo pipefail
cd "${0:A:h}"
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"

adb install -r ../apks/SmartTube_stable_32.68_armeabi-v7a.apk
adb install -r ../apks/tsag-agaar-tv.apk
adb install -r ../apks/kick-tv.apk   # Android 6 дээр суулгахад ≈2 минут болно
[ "${1:-}" = --breezy ] && adb install -r ../apks/breezy-weather-armeabi-v7a-v6.2.2_standard.apk
echo "Суулгалаа. SmartTube-д нэвтрэхдээ утаснаас youtube.com/activate руу орж кодоо оруулна."
