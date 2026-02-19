#!/bin/bash
set -e

# Configuration
AVD_NAME="Pixel_10_Pro"
SDK_ROOT="$HOME/Library/Android/sdk"
ADB="$SDK_ROOT/platform-tools/adb"

echo "=== 🛑 FitTrack Android Teardown ==="

# 1. Stop Emulator
echo "📱 Stopping Emulator..."
if pgrep -f "$AVD_NAME" > /dev/null; then
    # Try to kill nicely first via ADB
    "$ADB" -s emulator-5554 emu kill 2>/dev/null || true
    
    # Wait a moment
    sleep 2
    
    # Force kill if still running
    if pgrep -f "$AVD_NAME" > /dev/null; then
        echo "   Force killing emulator process..."
        pkill -f "$AVD_NAME"
    fi
    echo "✅ Emulator stopped."
else
    echo "ℹ️  No emulator running."
fi

# 2. Kill ADB Server (Optional but good for full reset)
# echo "🔌 Stopping ADB Server..."
# "$ADB" kill-server
# echo "✅ ADB Server stopped."

# 3. Clean Build Artifacts
echo "Cw Cleaning build artifacts..."
./gradlew clean > /dev/null 2>&1
echo "✅ Build clean."

echo "=== 😴 Teardown Complete ==="
