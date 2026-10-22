#!/usr/bin/env bash
# Builds Film to Watch.
#
#   scripts/build.sh            debug APK  -> app/build/outputs/apk/debug/app-debug.apk
#   scripts/build.sh release    release APK, signed with the key in .signing/ (created on first run)
#                               -> dist/film-to-watch-tv-<version>.apk
#
# Optional API keys come from local.properties or the TMDB_API_KEY / OMDB_API_KEY env variables.
# Set NO_KEYS=1 to build without any keys (that's how the public release is built).
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q '"17'; then
  for candidate in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-17-openjdk; do
    if [ -x "$candidate/bin/java" ]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
echo "Using JDK: ${JAVA_HOME:-system default}"

key_args=()
if [ "${NO_KEYS:-0}" = "1" ]; then
  key_args=(-PTMDB_API_KEY= -POMDB_API_KEY=)
  echo "Building without API keys (Cinemeta only)"
fi

if [ "${1:-debug}" = "release" ]; then
  if [ ! -f .signing/keystore.properties ]; then
    echo "Creating a release signing key in .signing/ (keep it: later builds need it to update the app)"
    mkdir -p .signing
    pass="$(head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n')"
    "${JAVA_HOME:+$JAVA_HOME/bin/}keytool" -genkeypair -keystore .signing/release.jks -alias filmtowatch \
      -keyalg RSA -keysize 4096 -validity 36500 -storepass "$pass" -keypass "$pass" \
      -dname "CN=Film to Watch" >/dev/null
    printf 'storeFile=.signing/release.jks\nstorePassword=%s\nkeyAlias=filmtowatch\nkeyPassword=%s\n' "$pass" "$pass" > .signing/keystore.properties
  fi
  ./gradlew clean assembleRelease "${key_args[@]}"
  version="$(sed -n 's/.*versionName = "\(.*\)"/\1/p' app/build.gradle.kts)"
  mkdir -p dist
  cp app/build/outputs/apk/release/app-release.apk "dist/film-to-watch-tv-$version.apk"
  echo "APK: $(pwd)/dist/film-to-watch-tv-$version.apk"
else
  ./gradlew assembleDebug "${key_args[@]}"
  echo "APK: $(pwd)/app/build/outputs/apk/debug/app-debug.apk"
fi
