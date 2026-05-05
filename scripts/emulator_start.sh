#!/usr/bin/env bash
# =============================================================================
# scripts/emulator_start.sh
# Starts the lightweight FitTrack_Test AVD.
# Usage: ./scripts/emulator_start.sh [--dark]
# =============================================================================

set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
EMULATOR="$SDK/emulator/emulator"
ADB="$SDK/platform-tools/adb"
AVD_NAME="FitTrack_Test"
DARK_MODE=false

for arg in "$@"; do
  [[ "$arg" == "--dark" ]] && DARK_MODE=true
done

# ── Validate AVD exists ───────────────────────────────────────────────────────
if ! "$EMULATOR" -list-avds 2>/dev/null | grep -q "^${AVD_NAME}$"; then
  echo "❌  AVD '${AVD_NAME}' not found. Run 'make emu-setup' first."
  exit 1
fi

# ── Kill any stale emulator instance ─────────────────────────────────────────
pkill -f "qemu.*${AVD_NAME}" 2>/dev/null || true
sleep 1

echo "🚀  Starting ${AVD_NAME} (lightweight)..."
nohup "$EMULATOR" \
  -avd "$AVD_NAME" \
  -no-snapshot-load \
  -no-boot-anim \
  -no-audio \
  -gpu swiftshader_indirect \
  -memory 2048 \
  -cores 2 \
  > /tmp/fittrack_emu.log 2>&1 &

EMU_PID=$!
echo "   PID: ${EMU_PID}"

# ── Wait for ADB connection ───────────────────────────────────────────────────
echo -n "   Waiting for ADB..."
"$ADB" wait-for-device
echo " connected."

# ── Wait for full boot ────────────────────────────────────────────────────────
echo -n "   Booting"
until [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; do
  echo -n "."
  sleep 2
done
echo " done."

# ── Light/dark mode ───────────────────────────────────────────────────────────
"$ADB" shell input keyevent 82   # unlock screen
if [[ "$DARK_MODE" == "true" ]]; then
  "$ADB" shell "cmd uimode night yes"
  echo "   🌙  Dark mode enabled."
else
  "$ADB" shell "cmd uimode night no"
  echo "   ☀️   Light mode enabled."
fi

echo ""
echo "✅  Emulator ready — run 'make run' to install and launch the app."
echo "   Logs: /tmp/fittrack_emu.log"
