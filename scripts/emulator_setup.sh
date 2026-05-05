#!/usr/bin/env bash
# =============================================================================
# scripts/emulator_setup.sh
# One-time setup: creates a lightweight test AVD (Pixel 6, 2GB RAM).
# Requires ANDROID_HOME or SDK at ~/Library/Android/sdk.
# =============================================================================

set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
AVD_NAME="FitTrack_Test"

# Primary install path used on this machine
if [[ ! -f "$SDKMANAGER" ]]; then
  SDK="/usr/local/share/android-commandlinetools"
  SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
  AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
fi

# Also check ~/Library/Android/sdk (Android Studio default)
if [[ ! -f "$SDKMANAGER" ]]; then
  SDK="$HOME/Library/Android/sdk"
  SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"
  AVDMANAGER="$SDK/cmdline-tools/latest/bin/avdmanager"
fi

if [[ ! -f "$AVDMANAGER" ]]; then
  echo "❌  avdmanager not found. Install Android command-line tools first."
  exit 1
fi

# Use google_apis_playstore only if needed; google_apis is lighter and sufficient for testing
SYSTEM_IMAGE="system-images;android-35;google_apis;x86_64"

# Install system image only if not already present
if ! "$SDKMANAGER" --list_installed 2>/dev/null | grep -q "$SYSTEM_IMAGE"; then
  echo "📦  Installing system image..."
  "$SDKMANAGER" --install "$SYSTEM_IMAGE"
else
  echo "✅  System image already installed."
fi

echo "📱  Creating lightweight test AVD '${AVD_NAME}'..."
"$AVDMANAGER" delete avd -n "$AVD_NAME" 2>/dev/null || true
echo "no" | "$AVDMANAGER" create avd \
  --name "$AVD_NAME" \
  --package "$SYSTEM_IMAGE" \
  --device "pixel_6" \
  --force

# ── Apply lightweight hardware config ────────────────────────────────────────
AVD_DIR="$HOME/.android/avd/${AVD_NAME}.avd"
CONFIG="$AVD_DIR/config.ini"

apply() {
  if grep -q "^${1}=" "$CONFIG" 2>/dev/null; then
    sed -i '' "s|^${1}=.*|${1}=${2}|" "$CONFIG"
  else
    echo "${1}=${2}" >> "$CONFIG"
  fi
}

apply hw.lcd.width    1080
apply hw.lcd.height   2410     # matches Pixel 10 Pro exactly
apply hw.lcd.density  420
apply hw.cpu.ncore    2        # 2 cores is enough for UI testing
apply hw.ramSize      2048     # 2 GB — sufficient, not greedy
apply vm.heapSize     256      # 256 MB heap
apply hw.gpu.enabled  yes
apply hw.gpu.mode     auto
apply showDeviceFrame no       # no device chrome = faster render
apply fastboot.forceFastBoot yes

echo ""
echo "✅  AVD '${AVD_NAME}' created (1080×2410 @ 420dpi, 2GB RAM, 2 cores)"
echo "   Run: make emu-start"
