#!/bin/zsh
# Зурагтын дэлгэцийн зургийг Mac руу хадгална (видео/HDMI давхарга хар гарч болно).
#   ./screenshot.sh                    → ~/Desktop/tv-<огноо>.png
#   ./screenshot.sh ~/tmp/zurag.png
set -eu
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
out="${1:-$HOME/Desktop/tv-$(date +%Y%m%d-%H%M%S).png}"
adb shell screencap -p /sdcard/screenshot.png >/dev/null
adb pull /sdcard/screenshot.png "$out" >/dev/null 2>&1
adb shell rm /sdcard/screenshot.png
echo "$out"
