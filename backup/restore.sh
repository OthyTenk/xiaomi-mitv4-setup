#!/bin/zsh
# 2026-10-31-нд зурагтаас (MiTV4-ANSM0, Android 6) устгасан Xiaomi app-уудыг буцааж суулгана.
#   ./restore.sh                  бүгдийг буцаана
#   ./restore.sh <pkg>...         зөвхөн эдгээрийг буцаана
#   TV=192.168.1.20 ./restore.sh  зурагтын IP өөр бол
# Зурагт дээр ADB debugging асаалттай байх ёстой (Settings → Account & security → ADB debugging).
# Шинэчлэлттэй system app-ууд apk/-аас яг тэр хувилбараараа, зөвхөн /system-д байгаа app-ууд
# зурагтын /system-ээс, third-party app-ууд зөвхөн apk/-аас сэргэнэ.
set -u
B=${0:A:h}
export ANDROID_SERIAL="${TV:-192.168.1.13}:5555"
adb connect "$ANDROID_SERIAL" >/dev/null

# package → original /system directory ("-" = not a system app)
typeset -A SYS=(
  com.xiaomi.mitv.appstore                 /system/vendor/app/MiTVAppStore
  com.xiaomi.mitv.shop                     /system/vendor/app/MiTVShop
  com.xiaomi.mitv.payment                  /system/vendor/app/MiTVPayment
  com.xiaomi.mitv.pay                      /system/vendor/app/MiTVPay
  com.xiaomi.gamecenter.sdk.service.mibox  /system/vendor/app/MiGameCenterSDKService
  com.xiaomi.voicecontrol                  /system/app/VoiceControl
  com.xiaomi.smarthome.tv                  /system/vendor/app/SmartHomeTV
  com.xiaomi.smarthome.tv.service          /system/vendor/app/TVSmartHomeAssistant
  com.xiaomi.tweather                      /system/app/TWeather
  com.xiaomi.mitv.calendar                 /system/app/TVCalendar
  com.xiaomi.mitv.karaoke.service          /system/app/KaraokeService
  com.xiaomi.mitv.tvmanager                /system/priv-app/TVManager
  com.xiaomi.screenrecorder                /system/app/TVScreenRecorder
  com.miui.tv.analytics                    /system/app/MiTVBiAnalytics
  com.xiaomi.statistic                     /system/vendor/app/MiTVStat
  com.miui.systemAdSolution                /system/vendor/app/AdServer
  com.xiaomi.mitv.tvpush.tvpushservice     /system/vendor/app/TVPushService
  com.mitv.cloudcontrol                    /system/app/CloudControlCenter
  cleantools.mitv.com.tvcleantools         /system/vendor/app/MobileService
  com.ktcp.video                           -
  com.xm.webcontent                        -
  com.mitv.shoplugin                       -
  com.xiaomi.eetv.litehome                 -
  com.mitv.care                            -
  com.google.android.youtube               -
)

pkgs=("$@"); (( $#pkgs )) || pkgs=(${(k)SYS})
for p in $pkgs; do
  dir=${SYS[$p]:-}
  [ -n "$dir" ] || { echo "unknown package: $p"; continue; }
  if [ -s "$B/apk/$p.apk" ]; then
    adb install -r -d --user 0 "$B/apk/$p.apk" | tail -1
  elif [ "$dir" != - ]; then
    adb shell "pm install -r --user 0 \$(ls $dir/*.apk | head -1)" | tail -1
  else
    echo "no backup for $p"; continue
  fi
  echo "  $p: $(adb shell dumpsys package $p | grep -m1 'User 0:' | sed 's/^ *//')"
done
