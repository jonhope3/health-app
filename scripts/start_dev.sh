#!/bin/bash
set -e

# Configuration
AVD_NAME="Pixel_10_Pro"
SDK_ROOT="$HOME/Library/Android/sdk"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"
AVDMANAGER="avdmanager" # Assuming in PATH from Setup, otherwise we can find it

echo "=== 🚀 FitTrack Android Dev Start ==="

# 1. Check/Install Dependencies & Create AVD
if ! "$EMULATOR" -list-avds | grep -q "$AVD_NAME"; then
    echo "⚠️  AVD $AVD_NAME not found. Initiating auto-setup..."
    
    # Ensure system image is installed
    if [ ! -d "$SDK_ROOT/system-images/android-35/google_apis/x86_64" ]; then
        echo "⬇️  Downloading System Image (this may take a while)..."
        # Try to find sdkmanager
        SDKMANAGER="sdkmanager"
        if ! command -v sdkmanager &> /dev/null; then
             SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
        fi
        yes | "$SDKMANAGER" "system-images;android-35;google_apis;x86_64" "emulator" "platform-tools"
    fi
    
    echo "📱 Creating AVD..."
    echo "no" | "$AVDMANAGER" create avd -n $AVD_NAME -k "system-images;android-35;google_apis;x86_64" -d 47 --force
    echo "✅ AVD Created."
else
    echo "✅ AVD $AVD_NAME exists."
fi

# 2. Start Emulator if not running
if ! pgrep -f "$AVD_NAME" > /dev/null; then
    echo "🖥️  Starting Emulator..."
    "$EMULATOR" -avd $AVD_NAME -no-snapshot-load -no-audio -gpu host &
    
    echo "⏳ Waiting for emulator to come online..."
    sleep 5
else
    echo "✅ Emulator is already running."
fi

# 3. Wait for Boot
echo "⏳ Waiting for device to be fully bootted..."
"$ADB" wait-for-device
while [[ -z $("$ADB" shell getprop sys.boot_completed 2>/dev/null) ]]; do 
    sleep 2
    echo -n "."
done
echo ""
echo "✅ Device ready!"

# 4. Build & Install
echo "🔨 Building & Installing App..."
./gradlew installDebug

# 5. Launch
echo "🚀 Launching App..."
"$ADB" shell monkey -p com.fittrack.app -c android.intent.category.LAUNCHER 1

echo "=== 🎉 App is running! ==="
