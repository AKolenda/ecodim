#!/usr/bin/env bash
# Builds a signed EcoDim release APK inside a fresh Debian/Ubuntu container.
# Usage: container-build.sh <keystore.properties>
# The properties file holds ECODIMMER_STORE_FILE / _STORE_PASSWORD / _KEY_ALIAS / _KEY_PASSWORD.
set -euo pipefail

KEY_PROPS=${1:?usage: container-build.sh <keystore.properties>}
SDK=/opt/android-sdk
CMDLINE_TOOLS_ZIP=commandlinetools-linux-11076708_latest.zip

export DEBIAN_FRONTEND=noninteractive
if ! command -v javac >/dev/null || ! command -v git >/dev/null; then
    apt-get update -q
    apt-get install -yq --no-install-recommends openjdk-17-jdk-headless git unzip curl ca-certificates
fi

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
    mkdir -p "$SDK/cmdline-tools"
    curl -fsSL -o /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
    unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools
    mv /tmp/cmdline-tools/cmdline-tools "$SDK/cmdline-tools/latest"
    rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-34" "build-tools;34.0.0" >/dev/null

if [ -d /src/ecodim/.git ]; then
    git -C /src/ecodim fetch -q origin && git -C /src/ecodim reset -q --hard origin/main
else
    git clone -q https://github.com/AKolenda/ecodim.git /src/ecodim
fi

cd /src/ecodim
echo "sdk.dir=$SDK" > local.properties
./gradlew --no-daemon -q testDebugUnitTest assembleRelease $(sed 's/^/-P/' "$KEY_PROPS" | tr '\n' ' ')

"$SDK/build-tools/34.0.0/apksigner" verify app/build/outputs/apk/release/app-release.apk
echo "Built $(git rev-parse --short HEAD): /src/ecodim/app/build/outputs/apk/release/app-release.apk"
