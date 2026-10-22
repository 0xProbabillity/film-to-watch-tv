#!/usr/bin/env bash
# Usage: scripts/install.sh <tv-ip-address> [path/to.apk]
# Connects to the TV over Wi-Fi (network debugging must be enabled), installs the APK and opens the app.
# Without an APK path it installs the newest build: dist/*.apk, else the debug APK (building it if needed).
set -euo pipefail
cd "$(dirname "$0")/.."
TV_IP="${1:?Usage: scripts/install.sh <tv-ip-address> [path/to.apk]}"
DEVICE="$TV_IP"
[[ "$DEVICE" == *:* ]] || DEVICE="$TV_IP:5555"

APK="${2:-}"
if [ -z "$APK" ]; then
  APK="$(ls -t dist/*.apk 2>/dev/null | head -1 || true)"
  if [ -z "$APK" ]; then
    APK=app/build/outputs/apk/debug/app-debug.apk
    [ -f "$APK" ] || scripts/build.sh
  fi
fi

adb connect "$DEVICE"
adb -s "$DEVICE" wait-for-device
adb -s "$DEVICE" install -r "$APK"
adb -s "$DEVICE" shell am start -n com.filmtowatch.tv/.MainActivity
echo "Installed $APK on $DEVICE"
