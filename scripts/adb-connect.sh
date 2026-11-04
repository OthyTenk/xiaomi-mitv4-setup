#!/bin/zsh
# Зурагттай Wi‑Fi-аар ADB холболт тогтооно.
#   ./adb-connect.sh                 (TV=192.168.1.13 гэж үзнэ)
#   TV=192.168.1.20 ./adb-connect.sh (IP өөрчлөгдсөн бол)
# Урьдчилан: зурагт дээр Settings → Account & security → ADB debugging → Allow.
# USB кабель бүү ашигла: зурагт асаж унтраад гацдаг (docs/02-adb.md).
set -u
TV="${TV:-192.168.1.13}"
SERIAL="$TV:5555"

if ! nc -z -G 3 -w 3 "$TV" 5555 >/dev/null 2>&1; then
  echo "5555 port хаалттай. Зурагт дээр ADB debugging-ийг Allow болгоно уу (standby/reboot бүрт унтардаг)." >&2
  exit 1
fi

out=$(adb connect "$SERIAL" 2>&1)
case "$out" in
  *"No route to host"*)
    # macOS: Android Studio-оос эхэлсэн хуучин adb server-т Local Network эрх байдаггүй.
    adb kill-server
    adb start-server >/dev/null 2>&1
    out=$(adb connect "$SERIAL" 2>&1)
    ;;
esac
echo "$out"

st=""
for i in $(seq 1 40); do
  st=$(adb devices | awk -v d="$SERIAL" '$1==d{print $2}')
  if [ "$st" = device ]; then
    echo "Холбогдлоо: $SERIAL"
    exit 0
  fi
  [ "$st" = unauthorized ] && [ "$i" = 1 ] && echo "Зурагт дээр 'Allow USB debugging?' цонхонд 'Always allow' чагтлаад OK дарна уу…"
  sleep 3
done
echo "Холбогдож чадсангүй (төлөв: ${st:-none})." >&2
exit 2
