#!/bin/zsh
# APK-г Gradle-гүйгээр build хийж sign хийнэ: aapt2 → javac → d8 → zipalign → apksigner.
#   ./build.sh            build/tsag-agaar-tv.apk үүсгэнэ
#   ./build.sh install    build хийгээд зурагт дээр суулгаж нээнэ (TV=<ip>, default 192.168.1.13)
# Анх ажиллахдаа keystore.jks үүсгэнэ. Түүнийг бүү устга, үгүй бол дараагийн build суусан app-ыг шинэчилж чадахгүй.
set -euo pipefail
cd "${0:A:h}"

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JAR="$SDK/platforms/android-35/android.jar"

rm -rf build
mkdir -p build/gen build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/app.unsigned.apk -I "$JAR" --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 21 --target-sdk-version 30 build/res.zip

javac --release 8 -encoding UTF-8 -classpath "$JAR" -d build/classes $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 21 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

cp build/app.unsigned.apk build/app.dex.apk
(cd build/dex && zip -q ../app.dex.apk classes.dex)
"$BT/zipalign" -p -f 4 build/app.dex.apk build/app.aligned.apk

if [ ! -f keystore.jks ]; then
  (umask 077; head -c 24 /dev/urandom | base64 > keystore.pass)
  keytool -genkeypair -keystore keystore.jks -storepass:file keystore.pass -alias tsagagaar \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Tsag Agaar TV"
fi
"$BT/apksigner" sign --ks keystore.jks --ks-pass file:keystore.pass --ks-key-alias tsagagaar \
  --out build/tsag-agaar-tv.apk build/app.aligned.apk
echo "built build/tsag-agaar-tv.apk ($(stat -f %z build/tsag-agaar-tv.apk) bytes)"

if [ "${1:-}" = install ]; then
  serial="${TV:-192.168.1.13}:5555"
  adb connect "$serial" >/dev/null
  adb -s "$serial" install -r build/tsag-agaar-tv.apk
  adb -s "$serial" shell am start -n mn.home.tsagagaar/.MainActivity
fi
