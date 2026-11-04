#!/bin/zsh
# APK-г Gradle-гүйгээр build хийж sign хийнэ: aapt2 → javac → d8 → zipalign → apksigner.
#   ./build.sh            build/kick-tv.apk үүсгэнэ
#   ./build.sh install    build хийгээд зурагт дээр суулгаж нээнэ (TV=<ip>, default 192.168.1.13)
# Видео тоглуулагч (Media3 ExoPlayer) болон Guava-г анх удаа libs/-д татаж, SHA-256-аар шалгана.
# Анх ажиллахдаа keystore.jks үүсгэнэ. Түүнийг бүү устга, үгүй бол дараагийн build суусан app-ыг шинэчилж чадахгүй.
set -euo pipefail
cd "${0:A:h}"

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
JAR="$SDK/platforms/android-35/android.jar"

# Хувилбар солихдоо URL болон SHA-256-ийг хамт солино. Media3 1.11 нь Android 6 (API 23)-г дэмждэг.
G=https://dl.google.com/dl/android/maven2
C=https://repo1.maven.org/maven2
LIBS=(
  $G/androidx/media3/media3-common/1.11.1/media3-common-1.11.1.aar 50813cf219c7c93ed8a32a750891ca6d1280729ecf7a90c49567adacd67ef31b
  $G/androidx/media3/media3-container/1.11.1/media3-container-1.11.1.aar 930fca2f018f06f1d08c12599f8c97f31eddbd728edee5de06eb53a882f8fc54
  $G/androidx/media3/media3-database/1.11.1/media3-database-1.11.1.aar 520e6f814e42a7d428db187ac073b453c0fcba0f4f5f4edd1cf33fafd2cfb740
  $G/androidx/media3/media3-datasource/1.11.1/media3-datasource-1.11.1.aar 490fd23c7562a6a1183d63bf854b291ae3a77dce5a151450aa1937148c80c60b
  $G/androidx/media3/media3-decoder/1.11.1/media3-decoder-1.11.1.aar c3bcd704959026e0d644b0260f9e682a2e26e7357c2cf82d990bcb323f800d49
  $G/androidx/media3/media3-exoplayer/1.11.1/media3-exoplayer-1.11.1.aar 588592baec110837c33ce0c82bb76464203e6b0621177bbe5d88779001a6f1ed
  $G/androidx/media3/media3-exoplayer-hls/1.11.1/media3-exoplayer-hls-1.11.1.aar bd7aeac8dc60b3ee926eddbe05e29f27dc53c81514cae3271b2edab693f34262
  $G/androidx/media3/media3-extractor/1.11.1/media3-extractor-1.11.1.aar 6b2d58d23a0c53425249d8b7c945600019d61a8b404c8fb8cd9ca860c5738508
  $C/com/google/guava/guava/33.3.1-android/guava-33.3.1-android.jar 2c3e41d1b380f2044d257947a3aa82dabf3ae4b978622745254aa18b6cf89ab0
  $C/com/google/guava/failureaccess/1.0.2/failureaccess-1.0.2.jar 8a8f81cf9b359e3f6dfa691a1e776985c061ef2f223c9b2c80753e1b458e8064
)
# Зөвхөн javac-д хэрэгтэй (annotation-ууд), APK-д орохгүй.
COMPILE_ONLY=(
  $G/androidx/annotation/annotation/1.6.0/annotation-1.6.0.jar fbc64f5c44a7added8b6eab517cf7d70555e25153bf5d44a6ed9b0e5312f7de9
)

fetch() {  # <url> <sha256>: libs/<файл>-ыг татаж шалгаад замыг нь хэвлэнэ
  local f="libs/${1:t}"
  [ -f "$f" ] || curl -fsSL -o "$f" "$1"
  if ! echo "$2  $f" | shasum -a 256 -c --status; then
    echo "SHA-256 таарахгүй байна: $f" >&2
    rm -f "$f"
    exit 1
  fi
  echo "$f"
}

rm -rf build
mkdir -p libs build/gen build/classes build/dex build/jars

runtime=()
for url sum in $LIBS; do
  f=$(fetch $url $sum)
  if [[ $f == *.aar ]]; then
    unzip -p "$f" classes.jar > "build/jars/${f:t:r}.jar"
    runtime+=("build/jars/${f:t:r}.jar")
  else
    runtime+=("$f")
  fi
done
compile=()
for url sum in $COMPILE_ONLY; do compile+=("$(fetch $url $sum)"); done

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/app.unsigned.apk -I "$JAR" --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 23 --target-sdk-version 30 build/res.zip

javac --release 8 -encoding UTF-8 -classpath "$JAR:${(j.:.)runtime}:${(j.:.)compile}" -d build/classes \
  $(find src build/gen -name '*.java')
"$BT/d8" --release --min-api 23 --lib "$JAR" --output build/dex $(find build/classes -name '*.class') $runtime

cp build/app.unsigned.apk build/app.dex.apk
(cd build/dex && zip -q ../app.dex.apk classes*.dex)
"$BT/zipalign" -p -f 4 build/app.dex.apk build/app.aligned.apk

if [ ! -f keystore.jks ]; then
  (umask 077; head -c 24 /dev/urandom | base64 > keystore.pass)
  keytool -genkeypair -keystore keystore.jks -storepass:file keystore.pass -alias kicktv \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Kick TV"
fi
"$BT/apksigner" sign --ks keystore.jks --ks-pass file:keystore.pass --ks-key-alias kicktv \
  --out build/kick-tv.apk build/app.aligned.apk
echo "built build/kick-tv.apk ($(stat -f %z build/kick-tv.apk) bytes)"

if [ "${1:-}" = install ]; then
  serial="${TV:-192.168.1.13}:5555"
  adb connect "$serial" >/dev/null
  adb -s "$serial" install -r build/kick-tv.apk
  adb -s "$serial" shell am start -n mn.home.kick/.MainActivity
fi
