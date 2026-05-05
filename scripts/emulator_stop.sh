#!/usr/bin/env bash
# =============================================================================
# scripts/emulator_stop.sh
# Gracefully stops all running emulator instances and cleans up.
# =============================================================================

set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

echo "🛑  Stopping emulator..."

# Try graceful shutdown via ADB first
"$ADB" -s emulator-5554 emu kill 2>/dev/null || true
sleep 2

# Force-kill QEMU if still running
pkill -f "qemu-system-x86_64.*Pixel_10_Pro" 2>/dev/null || true
pkill -f "emulator.*Pixel_10_Pro" 2>/dev/null || true

echo "✅  Emulator stopped."
