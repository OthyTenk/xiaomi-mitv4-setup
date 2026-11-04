#!/bin/zsh
# 2026-10-31-нд устгасан 24 Хятад app-ыг дахин устгана (жишээ нь factory reset-ийн дараа).
#   ./remove-chinese-apps.sh
# Устгахаас өмнө /data/app дахь APK-г ../backup/apk руу хадгална. Буцаах: ../backup/restore.sh
# Энэ firmware `pm disable-user`, `pm hide`-ийг хориглодог тул system app-ыг `pm uninstall -k --user 0`-оор нууна.
# АНХААР: жагсаалтад өөр app бүү нэм. Хөндөж болохгүй app-ууд: docs/05-app-removal.mn.md
set -u
cd "${0:A:h}"
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
B=../backup
LOG=$B/removal-log.txt

# Бүрэн устгана: зөвхөн backup APK-аас сэргээж болно.
THIRD_PARTY=(com.ktcp.video com.xm.webcontent com.mitv.shoplugin com.xiaomi.eetv.litehome com.mitv.care
  com.google.android.youtube)
# Хэрэглэгчээс нууна: эх хувилбар нь /system-д үлдэнэ.
SYSTEM=(com.xiaomi.mitv.appstore com.xiaomi.mitv.shop com.xiaomi.mitv.payment com.xiaomi.mitv.pay
  com.miui.tv.analytics com.xiaomi.statistic com.miui.systemAdSolution com.xiaomi.mitv.tvpush.tvpushservice
  com.mitv.cloudcontrol cleantools.mitv.com.tvcleantools com.xiaomi.gamecenter.sdk.service.mibox
  com.xiaomi.voicecontrol com.xiaomi.smarthome.tv com.xiaomi.smarthome.tv.service com.xiaomi.tweather
  com.xiaomi.mitv.calendar com.xiaomi.mitv.karaoke.service com.xiaomi.screenrecorder)

# Wi‑Fi тасарвал дутуу файл үлдэхгүйн тулд хэмжээг нь шалгаж байж хадгална.
backup() {
  local want got
  want=$(adb shell ls -l "$2" | awk '{print $4}' | tr -d '\r')
  adb pull "$2" "$B/apk/$1.apk.part" >/dev/null 2>&1
  got=$(stat -f %z "$B/apk/$1.apk.part" 2>/dev/null || echo 0)
  [ -n "$want" ] && [ "$got" = "$want" ] && mv "$B/apk/$1.apk.part" "$B/apk/$1.apk"
}

echo "${#THIRD_PARTY} third-party app-ыг бүрэн устгаж, ${#SYSTEM} system app-ыг нууна."
read -q "?Үргэлжлүүлэх үү? [y/N] " || { echo; exit 1; }
echo

mkdir -p $B/apk
echo "== $(date '+%F %T') remove-chinese-apps.sh" >> $LOG
for p in $THIRD_PARTY $SYSTEM; do
  pth=$(adb shell pm path $p 2>/dev/null | head -1 | sed 's/package://' | tr -d '\r')
  if [ -z "$pth" ]; then
    echo "байхгүй  $p"
    continue
  fi
  if [[ $pth == /data/app/* ]] && [ ! -s "$B/apk/$p.apk" ] && ! backup $p "$pth"; then
    echo "backup амжилтгүй, алгаслаа: $p" >&2
    continue
  fi
  if (( ${THIRD_PARTY[(Ie)$p]} )); then
    r=$(adb shell pm uninstall $p 2>&1 | tr -d '\r')
  else
    r=$(adb shell pm uninstall -k --user 0 $p 2>&1 | tr -d '\r')
  fi
  echo "$r  $p" | tee -a $LOG
done
echo "Дууслаа. Зурагтыг нэг удаа дахин асааж шалгаарай (adb reboot)."
